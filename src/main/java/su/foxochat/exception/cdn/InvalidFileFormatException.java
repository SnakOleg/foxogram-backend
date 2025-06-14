package su.foxochat.exception.cdn;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import su.foxochat.constant.ExceptionConstant;
import su.foxochat.exception.BaseException;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class InvalidFileFormatException extends BaseException {

	public InvalidFileFormatException() {
		super(ExceptionConstant.Messages.INVALID_FILE_FORMAT.getValue(), InvalidFileFormatException.class.getAnnotation(ResponseStatus.class).value(), ExceptionConstant.CDN.INVALID_FILE_FORMAT.getValue());
	}
}
