package com.bana.dotori.chore.mapper;

import org.springframework.stereotype.Component;

import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.entity.ChoreCompletionRequest;

@Component
public class ChoreCompletionMapper {

	public ChoreCompletionResponse toResponse(ChoreCompletionRequest request) {
		return new ChoreCompletionResponse(request.getId(), request.getChoreId(), request.getStatus(),
			request.getRequestedAt());
	}
}
