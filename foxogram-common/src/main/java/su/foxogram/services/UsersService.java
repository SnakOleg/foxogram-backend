package su.foxogram.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import su.foxogram.constants.EmailConstants;
import su.foxogram.constants.OTPConstants;
import su.foxogram.constants.StorageConstants;
import su.foxogram.constants.UserConstants;
import su.foxogram.dtos.api.request.UserEditDTO;
import su.foxogram.dtos.api.response.ChannelDTO;
import su.foxogram.exceptions.cdn.UploadFailedException;
import su.foxogram.exceptions.otp.OTPExpiredException;
import su.foxogram.exceptions.otp.OTPsInvalidException;
import su.foxogram.exceptions.user.UserCredentialsDuplicateException;
import su.foxogram.exceptions.user.UserCredentialsIsInvalidException;
import su.foxogram.exceptions.user.UserNotFoundException;
import su.foxogram.models.Member;
import su.foxogram.models.Message;
import su.foxogram.models.OTP;
import su.foxogram.models.User;
import su.foxogram.repositories.MemberRepository;
import su.foxogram.repositories.MessageRepository;
import su.foxogram.repositories.UserRepository;
import su.foxogram.util.OTPGenerator;
import su.foxogram.util.PasswordHasher;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UsersService {
	private final UserRepository userRepository;

	private final EmailService emailService;

	private final OTPService OTPService;

	private final StorageService storageService;

	private final MemberRepository memberRepository;

	private final MessageRepository messageRepository;

	@Autowired
	public UsersService(UserRepository userRepository, EmailService emailService, OTPService OTPService, StorageService storageService, MemberRepository memberRepository, MessageRepository messageRepository) {
		this.userRepository = userRepository;
		this.emailService = emailService;
		this.OTPService = OTPService;
		this.storageService = storageService;
		this.memberRepository = memberRepository;
		this.messageRepository = messageRepository;
	}

	public User getUserById(long id) throws UserNotFoundException {
		return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
	}

	public User getUserByUsername(String username) throws UserNotFoundException {
		return userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
	}

	public List<ChannelDTO> getChannels(User user) {
		return memberRepository.findAllByUserId(user.getId())
				.stream()
				.map(Member::getChannel).map(channel -> {
					Message lastMessage = messageRepository.getLastMessageByChannel(channel);
					return new ChannelDTO(channel, lastMessage);
				})
				.collect(Collectors.toList());
	}

	public User editUser(User user, UserEditDTO body) throws UserCredentialsDuplicateException, UploadFailedException {
		if (body.getDisplayName() != null) user.setDisplayName(body.getDisplayName());
		if (body.getAvatar() != null) changeAvatar(user, body.getAvatar());
		if (body.getUsername() != null) user.setUsername(body.getUsername());
		if (body.getEmail() != null) changeEmail(user, body);
		if (body.getPassword() != null) changePassword(user, body);

		try {
			userRepository.save(user);
		} catch (DataIntegrityViolationException e) {
			throw new UserCredentialsDuplicateException();
		}

		log.info("User ({}, {}) edited successfully", user.getUsername(), user.getEmail());

		return user;
	}

	public void requestUserDelete(User user, String password) throws UserCredentialsIsInvalidException {
		if (!PasswordHasher.verifyPassword(password, user.getPassword()))
			throw new UserCredentialsIsInvalidException();

		sendEmail(user, EmailConstants.Type.ACCOUNT_DELETE);
		log.info("User ({}, {}) delete requested successfully", user.getUsername(), user.getEmail());
	}

	public void confirmUserDelete(User user, String pathCode) throws OTPsInvalidException, OTPExpiredException {
		OTP OTP = OTPService.validateCode(pathCode);

		userRepository.delete(user);

		log.info("User ({}, {}) deleted successfully", user.getUsername(), user.getEmail());

		if (OTP == null) return; // is dev

		OTPService.delete(OTP);
	}

	private void changeAvatar(User user, MultipartFile avatar) throws UploadFailedException {
		String hash;

		try {
			hash = storageService.uploadIdentityImage(avatar, StorageConstants.AVATARS_BUCKET);
		} catch (Exception e) {
			throw new UploadFailedException();
		}

		user.setAvatar(hash);
	}

	private void changeEmail(User user, UserEditDTO body) {
		user.setEmail(body.getEmail());
		user.addFlag(UserConstants.Flags.AWAITING_CONFIRMATION);

		sendEmail(user, EmailConstants.Type.EMAIL_VERIFY);
	}

	private void changePassword(User user, UserEditDTO body) {
		user.setPassword(PasswordHasher.hashPassword(body.getPassword()));
		user.addFlag(UserConstants.Flags.AWAITING_CONFIRMATION);

		sendEmail(user, EmailConstants.Type.RESET_PASSWORD);
	}

	private void sendEmail(User user, EmailConstants.Type type) {
		String emailType = type.getValue();
		String code = OTPGenerator.generateDigitCode();
		long issuedAt = System.currentTimeMillis();
		long expiresAt = issuedAt + OTPConstants.Lifetime.BASE.getValue();

		emailService.sendEmail(user.getEmail(), user.getId(), emailType, user.getUsername(), code, issuedAt, expiresAt, null);
	}
}
