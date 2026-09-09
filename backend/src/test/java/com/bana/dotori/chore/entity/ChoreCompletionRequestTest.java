package com.bana.dotori.chore.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

/**
 * 1계층 - 완료 요청의 상태 전이만 검증한다.
 *
 * <p>스프링도 DB 도 없다. 이 테스트가 실패하면 원인은 무조건
 * {@link ChoreCompletionRequest} 안에 있다.
 */
@DisplayName("ChoreCompletionRequest 상태 전이")
class ChoreCompletionRequestTest {

	private static final OffsetDateTime PROCESSED_AT = OffsetDateTime.parse("2026-09-10T21:30+09:00");

	private ChoreCompletionRequest pendingRequest() {
		return ChoreCompletionRequest.builder()
			.choreId(1)
			.childId(2)
			.description("다 했어요")
			.build();
	}

	@Nested
	@DisplayName("요청을 올릴 때")
	class Create {

		@Test
		@DisplayName("대기 상태로 시작한다")
		void 대기_상태로_시작한다() {
			ChoreCompletionRequest request = pendingRequest();

			assertThat(request.getStatus()).isEqualTo(ChoreCompletionStatus.PENDING);
			assertThat(request.isPending()).isTrue();
		}

		@Test
		@DisplayName("처리 시각은 비어 있다")
		void 처리_시각은_비어_있다() {
			assertThat(pendingRequest().getProcessedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("대기 중인 요청을 처리할 때")
	class Transition {

		@Test
		@DisplayName("승인하면 SUCCEED 가 되고 처리 시각이 남는다")
		void 승인하면_SUCCEED() {
			ChoreCompletionRequest request = pendingRequest();

			request.approve(PROCESSED_AT);

			assertThat(request.getStatus()).isEqualTo(ChoreCompletionStatus.SUCCEED);
			assertThat(request.getProcessedAt()).isEqualTo(PROCESSED_AT);
		}

		@Test
		@DisplayName("거절하면 REJECTED 가 된다")
		void 거절하면_REJECTED() {
			ChoreCompletionRequest request = pendingRequest();

			request.reject(PROCESSED_AT);

			assertThat(request.getStatus()).isEqualTo(ChoreCompletionStatus.REJECTED);
			assertThat(request.getProcessedAt()).isEqualTo(PROCESSED_AT);
		}

		@Test
		@DisplayName("아이가 물리면 CANCELLED 가 된다")
		void 취소하면_CANCELLED() {
			ChoreCompletionRequest request = pendingRequest();

			request.cancel(PROCESSED_AT);

			assertThat(request.getStatus()).isEqualTo(ChoreCompletionStatus.CANCELLED);
		}
	}

	@Nested
	@DisplayName("이미 처리된 요청을 다시 건드리면")
	class AlreadyProcessed {

		@Test
		@DisplayName("두 번째 승인은 409 로 막는다")
		void 두_번_승인할_수_없다() {
			ChoreCompletionRequest request = pendingRequest();
			request.approve(PROCESSED_AT);

			// 이 검사가 없으면 부모가 승인 버튼을 두 번 눌렀을 때 보상이 두 번 나간다.
			assertThatThrownBy(() -> request.approve(PROCESSED_AT))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_REQUEST_NOT_PENDING);
		}

		@Test
		@DisplayName("거절한 요청을 승인할 수 없다")
		void 거절_뒤_승인할_수_없다() {
			ChoreCompletionRequest request = pendingRequest();
			request.reject(PROCESSED_AT);

			assertThatThrownBy(() -> request.approve(PROCESSED_AT))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_REQUEST_NOT_PENDING);
		}

		@Test
		@DisplayName("취소한 요청은 부모도 처리할 수 없다")
		void 취소_뒤_처리할_수_없다() {
			ChoreCompletionRequest request = pendingRequest();
			request.cancel(PROCESSED_AT);

			assertThatThrownBy(() -> request.reject(PROCESSED_AT))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_REQUEST_NOT_PENDING);
		}

		@Test
		@DisplayName("처리 시각을 덮어쓰지 않는다")
		void 처리_시각을_덮어쓰지_않는다() {
			ChoreCompletionRequest request = pendingRequest();
			request.approve(PROCESSED_AT);

			OffsetDateTime later = PROCESSED_AT.plusHours(1);
			assertThatThrownBy(() -> request.reject(later)).isInstanceOf(BusinessException.class);

			// 예외를 던지고도 필드를 건드렸으면 승인 시각이 거절 시도 시각으로 밀린다.
			assertThat(request.getProcessedAt()).isEqualTo(PROCESSED_AT);
		}
	}
}
