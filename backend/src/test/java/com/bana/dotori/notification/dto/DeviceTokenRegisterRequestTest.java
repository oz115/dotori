package com.bana.dotori.notification.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bana.dotori.notification.entity.DeviceTokenPlatform;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class DeviceTokenRegisterRequestTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	private Set<String> violatedFields(DeviceTokenRegisterRequest request) {
		return validator.validate(request).stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	@Test
	@DisplayName("모든 필드가 유효하면 위반이 없다")
	void validate_success() {
		DeviceTokenRegisterRequest request = new DeviceTokenRegisterRequest("fcm-token-abc",
			DeviceTokenPlatform.ANDROID);

		Set<ConstraintViolation<DeviceTokenRegisterRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("fcmToken이 비어 있으면 위반이 발생한다")
	void validate_blankFcmToken() {
		DeviceTokenRegisterRequest request = new DeviceTokenRegisterRequest(" ", DeviceTokenPlatform.ANDROID);

		assertThat(violatedFields(request)).contains("fcmToken");
	}

	@Test
	@DisplayName("fcmToken이 255자를 초과하면 위반이 발생한다")
	void validate_tooLongFcmToken() {
		DeviceTokenRegisterRequest request = new DeviceTokenRegisterRequest("a".repeat(256),
			DeviceTokenPlatform.ANDROID);

		assertThat(violatedFields(request)).contains("fcmToken");
	}

	@Test
	@DisplayName("platform이 null이면 위반이 발생한다")
	void validate_nullPlatform() {
		DeviceTokenRegisterRequest request = new DeviceTokenRegisterRequest("fcm-token-abc", null);

		assertThat(violatedFields(request)).contains("platform");
	}
}
