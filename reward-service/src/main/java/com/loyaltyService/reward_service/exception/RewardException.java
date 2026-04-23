package com.loyaltyService.reward_service.exception;
import lombok.Getter;
import org.springframework.http.HttpStatus;
@Getter
public class RewardException extends RuntimeException {
    private final HttpStatus status;
    public RewardException(String message) { super(message); this.status = HttpStatus.BAD_REQUEST; }
    public RewardException(String message, HttpStatus status) { super(message); this.status = status; }
}
