package com.loyaltyService.auth_service.exception;

import org.springframework.http.HttpStatus;

public class UserBlockedException extends AuthException {
    public UserBlockedException() {
        super("User is blocked by admin", HttpStatus.FORBIDDEN);
    }
}
