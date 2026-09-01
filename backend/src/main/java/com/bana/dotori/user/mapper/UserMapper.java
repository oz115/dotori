package com.bana.dotori.user.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.user.dto.SignupRequest;
import com.bana.dotori.user.dto.SignupResponse;
import com.bana.dotori.user.entity.User;

@Component
public class UserMapper {

	public User toEntity(SignupRequest request, String encodedPassword) {
		return User.builder()
			.loginId(request.loginId())
			.password(encodedPassword)
			.name(request.name())
			.email(request.email())
			.role(request.role())
			.build();
	}

	public SignupResponse toSignupResponse(User user) {
		return new SignupResponse(user.getId(), user.getLoginId(), user.getName(), user.getEmail(), user.getRole(),
			user.getCreatedAt());
	}
}
