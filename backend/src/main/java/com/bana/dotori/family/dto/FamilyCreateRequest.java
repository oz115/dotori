package com.bana.dotori.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FamilyCreateRequest(
	@NotBlank @Size(max = 100) String name
) {
}
