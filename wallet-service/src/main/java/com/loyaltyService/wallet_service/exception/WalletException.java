package com.loyaltyService.wallet_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
@Getter
public class WalletException extends RuntimeException {
    private final HttpStatus status;
    public WalletException(String message) { super(message); this.status = HttpStatus.BAD_REQUEST; }
    public WalletException(String message, HttpStatus status) { super(message); this.status = status; }
}
