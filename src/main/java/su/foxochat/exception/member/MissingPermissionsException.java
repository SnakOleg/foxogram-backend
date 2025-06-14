package su.foxochat.exception.member;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import su.foxochat.constant.ExceptionConstant;
import su.foxochat.exception.BaseException;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class MissingPermissionsException extends BaseException {

	public MissingPermissionsException() {
		super(ExceptionConstant.Messages.MISSING_PERMISSIONS.getValue(), MissingPermissionsException.class.getAnnotation(ResponseStatus.class).value(), ExceptionConstant.Member.MISSING_PERMISSIONS.getValue());
	}
}
