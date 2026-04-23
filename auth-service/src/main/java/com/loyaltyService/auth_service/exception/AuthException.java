package com.loyaltyService.auth_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class AuthException extends RuntimeException {
    private final HttpStatus status;

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

@ResponseStatus(HttpStatus.CONFLICT)
class UserAlreadyExistsException extends AuthException {
    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class UserNotFoundException extends AuthException {
    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class InvalidCredentialsException extends AuthException {
    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidOtpException extends AuthException {
    public InvalidOtpException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}

@ResponseStatus(HttpStatus.GONE)
class OtpExpiredException extends AuthException {
    public OtpExpiredException(String message) {
        super(message, HttpStatus.GONE);
    }
}

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
class OtpRateLimitException extends AuthException {
    public OtpRateLimitException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class InvalidTokenException extends AuthException {
    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class TokenExpiredException extends AuthException {
    public TokenExpiredException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
