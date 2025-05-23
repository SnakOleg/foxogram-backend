package su.foxogram.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import su.foxogram.constants.GatewayConstants;
import su.foxogram.constants.MemberConstants;
import su.foxogram.constants.StorageConstants;
import su.foxogram.dtos.api.request.MessageCreateDTO;
import su.foxogram.dtos.api.response.MessageDTO;
import su.foxogram.exceptions.cdn.UploadFailedException;
import su.foxogram.exceptions.member.MissingPermissionsException;
import su.foxogram.exceptions.message.MessageNotFoundException;
import su.foxogram.models.*;
import su.foxogram.repositories.AttachmentRepository;
import su.foxogram.repositories.ChannelRepository;
import su.foxogram.repositories.MemberRepository;
import su.foxogram.repositories.MessageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessagesService {

	private final MessageRepository messageRepository;

	private final StorageService storageService;

	private final RabbitService rabbitService;

	private final ChannelRepository channelRepository;

	private final MemberRepository memberRepository;

	private final AttachmentRepository attachmentRepository;

	@Autowired
	public MessagesService(MessageRepository messageRepository, StorageService storageService, RabbitService rabbitService, ChannelRepository channelRepository, MemberRepository memberRepository, AttachmentRepository attachmentRepository) {
		this.messageRepository = messageRepository;
		this.storageService = storageService;
		this.rabbitService = rabbitService;
		this.channelRepository = channelRepository;
		this.memberRepository = memberRepository;
		this.attachmentRepository = attachmentRepository;
	}

	public List<MessageDTO> getMessages(long before, int limit, Channel channel) {
		List<Message> messagesArray = messageRepository.findAllByChannel(channel, before, limit);

		log.info("Messages ({}, {}) in channel ({}) found successfully", limit, before, channel.getId());

		return messagesArray.reversed().stream()
				.map(message -> {
					List<Attachment> attachments = new ArrayList<>();
					if (message.getAttachments() != null) {
						message.getAttachments().forEach(attachment -> attachments.add(attachmentRepository.findById(attachment)));
					}
					return new MessageDTO(message, attachments, true);
				})
				.collect(Collectors.toList());
	}

	public MessageDTO getMessage(long id, Channel channel) throws MessageNotFoundException {
		Message message = messageRepository.findByChannelAndId(channel, id);

		if (message == null) throw new MessageNotFoundException();

		List<Attachment> attachments = new ArrayList<>();
		message.getAttachments().forEach(attachment -> attachments.add(attachmentRepository.findById(attachment)));

		log.info("Message ({}) in channel ({}) found successfully", id, channel.getId());

		return new MessageDTO(message, attachments, true);
	}

	public Message addMessage(Channel channel, User user, MessageCreateDTO body) throws UploadFailedException, JsonProcessingException, MissingPermissionsException {
		List<String> uploadedAttachments = new ArrayList<>();
		Member member = memberRepository.findByChannelAndUser(channel, user);

		if (!member.hasAnyPermission(MemberConstants.Permissions.ADMIN, MemberConstants.Permissions.SEND_MESSAGES))
			throw new MissingPermissionsException();

		if (body.getAttachments() != null && !body.getAttachments().isEmpty()) {

			if (!member.hasAnyPermission(MemberConstants.Permissions.ADMIN, MemberConstants.Permissions.ATTACH_FILES))
				throw new MissingPermissionsException();

			try {
				uploadedAttachments = body.getAttachments().stream()
						.map(attachment -> {
							try {
								return uploadAttachment(attachment).getId();
							} catch (UploadFailedException e) {
								throw new RuntimeException(e);
							}
						})
						.collect(Collectors.toList());
			} catch (Exception e) {
				throw new UploadFailedException();
			}
		}

		Message message = new Message(channel, body.getContent(), member, uploadedAttachments);
		messageRepository.save(message);

		rabbitService.send(getRecipients(channel), new MessageDTO(message, null, true), GatewayConstants.Event.MESSAGE_CREATE.getValue());
		log.info("Message ({}) to channel ({}) created successfully", message.getId(), channel.getId());

		return message;
	}

	public void deleteMessage(long id, Member member, Channel channel) throws MessageNotFoundException, MissingPermissionsException, JsonProcessingException {
		Message message = messageRepository.findByChannelAndId(channel, id);

		if (message == null) throw new MessageNotFoundException();
		if (!message.isAuthor(member) && !member.hasAnyPermission(MemberConstants.Permissions.ADMIN, MemberConstants.Permissions.MANAGE_MESSAGES))
			throw new MissingPermissionsException();

		messageRepository.delete(message);
		rabbitService.send(getRecipients(channel), Map.of("id", id), GatewayConstants.Event.MESSAGE_DELETE.getValue());
		log.info("Message ({}) in channel ({}) deleted successfully", id, channel.getId());
	}

	public Message editMessage(long id, Channel channel, Member member, MessageCreateDTO body) throws MessageNotFoundException, MissingPermissionsException, JsonProcessingException {
		Message message = messageRepository.findByChannelAndId(channel, id);
		String content = body.getContent();

		if (message == null) throw new MessageNotFoundException();
		if (!message.isAuthor(member)) throw new MissingPermissionsException();

		message.setContent(content);
		messageRepository.save(message);

		rabbitService.send(getRecipients(channel), new MessageDTO(message, null, true), GatewayConstants.Event.MESSAGE_UPDATE.getValue());
		log.info("Message ({}) in channel ({}) edited successfully", id, channel.getId());

		return message;
	}

	private Attachment uploadAttachment(MultipartFile attachment) throws UploadFailedException {
		try {
			return storageService.uploadToMinio(attachment, StorageConstants.ATTACHMENTS_BUCKET);
		} catch (Exception e) {
			throw new UploadFailedException();
		}
	}

	private List<Long> getRecipients(Channel channel) {
		channel = channelRepository.findById(channel.getId()).get();
		return channel.getMembers().stream()
				.map(Member::getUser)
				.map(User::getId)
				.collect(Collectors.toList());
	}
}
