package com.bana.dotori.global.security;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.bana.dotori.global.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException {
		ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("""
			{"code":"%s","message":"%s","timestamp":"%s"}"""
			.formatted(errorCode.name(), errorCode.getMessage(), OffsetDateTime.now()));
		response.getWriter().flush();
	}
}
