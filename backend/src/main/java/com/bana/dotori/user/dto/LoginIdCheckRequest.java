package com.bana.dotori.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginIdCheckRequest(
	@NotBlank @Size(max = 50) String loginId
) {
}
