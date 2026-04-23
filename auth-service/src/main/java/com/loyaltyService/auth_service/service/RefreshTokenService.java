package com.loyaltyService.auth_service.service;

import com.loyaltyService.auth_service.model.RefreshToken;
import com.loyaltyService.auth_service.model.User;

public interface RefreshTokenService {
	RefreshToken createRefreshToken(User user);
	void revokeRefreshToken(String token);
	void revokeAllUserTokens(User user);
	void cleanupExpiredTokens();
	void revokeToken(String token);
	RefreshToken validateRefreshToken(String token) ;
}
