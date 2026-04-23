package com.loyaltyService.user_service.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateKycException extends RuntimeException {
    public DuplicateKycException(String msg) { super(msg); }
}
