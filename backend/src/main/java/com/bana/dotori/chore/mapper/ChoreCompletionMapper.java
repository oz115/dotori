package com.bana.dotori.chore.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.chore.dto.ChoreApprovalResponse;
import com.bana.dotori.chore.dto.ChoreCompletionListResponse;
import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.dto.ChoreCompletionRow;
import com.bana.dotori.chore.dto.ChoreCompletionSummary;
import com.bana.dotori.chore.dto.ChoreRejectionResponse;
import com.bana.dotori.chore.entity.ChoreCompletionRequest;

/**
 * 완료 요청 계열(CHORE-08~12)의 응답 생성.
 *
 * <p>여기 있는 메서드는 모두 <b>필드를 옮기기만</b> 한다. 리포지토리도 저장소도 시각 계산도
 * 없다. 그래서 서비스 테스트가 {@code @Spy} 로 실물을 그대로 쓸 수 있고, 응답 필드가 늘 때
 * 서비스를 건드리지 않는다.
 *
 * <p>{@code processedAt} 을 인자로 받지 않는 이유는 상태 전이가 이미 엔티티에 기록해 두기
 * 때문이다({@code ChoreCompletionRequest#approve}·{@code reject}). 서비스가 계산한 값을
 * 다시 넘기면 엔티티에 든 값과 어긋날 여지가 생긴다.
 *
 * <p><b>여기 넣지 않은 것</b>
 * <ul>
 *   <li>완료 내역({@code ChoreCompletionHistoryItem}) - 지갑 원장·집안일 제목·아이 이름
 *       세 소스를 짝지어 만든다. 한 소스를 옮기는 일이 아니라 조립이라 서비스에 둔다.</li>
 *   <li>사진 업로드 응답({@code ChorePhotoUploadResponse}) - 칸이 하나뿐이라 옮길 것이 없다.</li>
 * </ul>
 */
@Component
public class ChoreCompletionMapper {

	/** 완료 요청 생성 결과 (CHORE-11). */
	public ChoreCompletionResponse toResponse(ChoreCompletionRequest request) {
		return new ChoreCompletionResponse(request.getId(), request.getChoreId(), request.getStatus(),
			request.getRequestedAt());
	}

	/** 거절 결과 (CHORE-10). */
	public ChoreRejectionResponse toRejectionResponse(ChoreCompletionRequest request) {
		return new ChoreRejectionResponse(request.getId(), request.getStatus(), request.getProcessedAt());
	}

	/**
	 * 승인 결과 (CHORE-09).
	 *
	 * <p>{@code rewardPoints} 만 인자로 받는다. 지급액은 지갑이 돌려주는 값이라 요청 행에 없다.
	 */
	public ChoreApprovalResponse toApprovalResponse(ChoreCompletionRequest request, Integer rewardPoints) {
		return new ChoreApprovalResponse(request.getId(), request.getStatus(), rewardPoints,
			request.getProcessedAt());
	}

	public ChoreCompletionListResponse toListResponse(List<ChoreCompletionSummary> summaries) {
		return new ChoreCompletionListResponse(summaries);
	}

	/**
	 * 완료 요청 목록의 한 줄 (CHORE-08).
	 *
	 * <p>{@code photoUrl} 을 인자로 받는다. DB 에 있는 것은 S3 키라 링크를 발급해야 하는데,
	 * 그 호출을 여기 두면 매퍼가 저장소에 의존하게 되고 서비스 테스트가 매퍼까지 모킹해야 한다.
	 * 발급은 서비스가 하고 결과 문자열만 넘긴다.
	 */
	public ChoreCompletionSummary toSummary(ChoreCompletionRow row, String photoUrl) {
		return new ChoreCompletionSummary(row.choreCompletionRequestId(), row.choreId(), row.choreTitle(),
			row.childName(), photoUrl, row.description(), row.status(), row.requestedAt());
	}
}
