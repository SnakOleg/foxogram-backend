package su.foxochat.dto.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import su.foxochat.constant.ValidationConstant;

@Setter
@Getter
@Schema(name = "ChannelCreate")
public class ChannelCreateDTO {

	@NotNull(message = "Display name" + ValidationConstant.Messages.MUST_NOT_BE_NULL)
	@Size(min = 1, max = ValidationConstant.Lengths.CHANNEL_NAME, message = ValidationConstant.Messages.CHANNEL_NAME_WRONG_LENGTH)
	private String displayName;

	@NotNull(message = "Name" + ValidationConstant.Messages.MUST_NOT_BE_NULL)
	@Pattern(regexp = ValidationConstant.Regex.NAME_REGEX, message = ValidationConstant.Messages.CHANNEL_NAME_INCORRECT)
	@Size(min = 1, max = ValidationConstant.Lengths.CHANNEL_NAME, message = ValidationConstant.Messages.CHANNEL_NAME_WRONG_LENGTH)
	private String name;

	@NotNull(message = "Type" + ValidationConstant.Messages.MUST_NOT_BE_NULL)
	@Min(value = 1, message = ValidationConstant.Messages.CHANNEL_TYPE_INCORRECT)
	@Max(value = 3, message = ValidationConstant.Messages.CHANNEL_TYPE_INCORRECT)
	private int type;

	@NotNull(message = "Public" + ValidationConstant.Messages.MUST_NOT_BE_NULL)
	private boolean isPublic;
}
