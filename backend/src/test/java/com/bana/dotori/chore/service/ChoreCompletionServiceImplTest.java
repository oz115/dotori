package com.bana.dotori.chore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.chore.dto.ChoreCompletionCreateRequest;
import com.bana.dotori.chore.dto.ChoreCompletionListResponse;
import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.dto.ChoreCompletionRow;
import com.bana.dotori.chore.dto.ChoreRejectionResponse;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreCompletionRequest;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;
import com.bana.dotori.chore.entity.ChoreDifficulty;
import com.bana.dotori.chore.entity.ChoreRepeatType;
import com.bana.dotori.chore.mapper.ChoreCompletionMapper;
import com.bana.dotori.chore.repository.ChoreCompletionRequestRepository;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.storage.PhotoStorage;

/**
 * 2계층 - 완료 요청·취소·거절·대기함 조회의 규칙만 검증한다 (CHORE-08·10·11·12).
 *
 * <p>가족·아이 경계 검증은 {@link ChoreAccessValidator} 몫이라 여기서는 모킹한다.
 * 이 테스트가 실패하면 원인은 완료 요청 규칙 쪽이다.
 *
 * <p>대기함 조회의 JPQL 조인 자체는 여기서 검증하지 않는다 - 리포지토리를 모킹하기 때문이다.
 * 조인이 맞는지는 DB 가 있어야 알 수 있다.
 */
@DisplayName("ChoreCompletionServiceImpl 완료 요청·취소·거절·대기함")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChoreCompletionServiceImplTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private static final Integer FAMILY_ID = 10;
	private static final Integer PARENT_ID = 1;
	private static final Integer CHILD_ID = 2;
	private static final Integer SIBLING_ID = 3;
	private static final Integer CHORE_ID = 7;
	private static final Integer REQUEST_ID = 100;
	/** 서버가 만든 형식의 유효한 키. FAMILY_ID·CHILD_ID 와 일치해야 통과한다. */
	private static final String PHOTO_KEY = "chores/10/2/550e8400-e29b-41d4-a716-446655440000.jpg";

	@Mock
	private ChoreRepository choreRepository;

	@Mock
	private ChoreCompletionRequestRepository completionRequestRepository;

	@Mock
	private ChoreAccessValidator accessValidator;

	@Mock
	private PhotoStorage photoStorage;

	@Spy
	private ChoreCompletionMapper completionMapper;

	@InjectMocks
	private ChoreCompletionServiceImpl choreCompletionService;

	private Chore chore(LocalDate startDate, LocalDate endDate, Integer rewardPoints) {
		Chore chore = Chore.builder()
			.familyId(FAMILY_ID)
			.parentId(1)
			.childId(CHILD_ID)
			.title("설거지")
			.difficulty(ChoreDifficulty.NORMAL)
			.rewardPoints(rewardPoints)
			.repeatType(ChoreRepeatType.DAY)
			.startDate(startDate)
			.endDate(endDate)
			.build();
		ReflectionTestUtils.setField(chore, "id", CHORE_ID);
		return chore;
	}

	private Chore todayChore() {
		LocalDate today = LocalDate.now(KST);
		return chore(today, today, 500);
	}

	private void givenChore(Chore chore) {
		given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.of(chore));
		given(completionRequestRepository.saveAndFlush(any())).willAnswer(call -> call.getArgument(0));
	}

	private ChoreCompletionRequest pendingRequest(Integer ownerId) {
		ChoreCompletionRequest request = ChoreCompletionRequest.builder()
			.choreId(CHORE_ID)
			.childId(ownerId)
			.build();
		ReflectionTestUtils.setField(request, "id", REQUEST_ID);
		return request;
	}

	private ChoreCompletionResponse request() {
		return choreCompletionService.requestCompletion(CHILD_ID, FAMILY_ID, CHILD_ID, CHORE_ID,
			new ChoreCompletionCreateRequest(PHOTO_KEY, "다 했어요"));
	}

	@Nested
	@DisplayName("완료를 요청할 때")
	class RequestCompletion {

		@Test
		@DisplayName("기간 안의 할 일이면 대기 상태로 저장한다")
		void 정상_요청() {
			givenChore(todayChore());

			ChoreCompletionResponse response = request();

			assertThat(response.choreId()).isEqualTo(CHORE_ID);
			assertThat(response.status()).isEqualTo(ChoreCompletionStatus.PENDING);
			verify(completionRequestRepository).saveAndFlush(any());
		}

		@Test
		@DisplayName("형제의 사진 키를 실어 보내면 400이고 저장하지 않는다")
		void 남의_사진_키() {
			givenChore(todayChore());
			String siblingKey = "chores/10/3/550e8400-e29b-41d4-a716-446655440000.jpg";

			// 키는 클라이언트가 보내는 문자열이다. 그대로 믿으면 형제가 올린 사진으로
			// 자기 인증을 할 수 있다.
			assertThatThrownBy(() -> choreCompletionService.requestCompletion(CHILD_ID, FAMILY_ID, CHILD_ID,
					CHORE_ID, new ChoreCompletionCreateRequest(siblingKey, "다 했어요")))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FILE);

			verify(completionRequestRepository, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("우리 버킷 밖을 가리키는 키도 400이다")
		void 형식이_아닌_사진_키() {
			givenChore(todayChore());

			assertThatThrownBy(() -> choreCompletionService.requestCompletion(CHILD_ID, FAMILY_ID, CHILD_ID,
					CHORE_ID, new ChoreCompletionCreateRequest("https://evil.example.com/a.jpg", null)))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_FILE);

			verify(completionRequestRepository, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("아이가 아니면 403으로 막고 저장하지 않는다")
		void 아이만_요청할_수_있다() {
			willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(accessValidator).requireChild(CHILD_ID);

			assertThatThrownBy(() -> request())
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

			verify(completionRequestRepository, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("없는 할 일이면 404다")
		void 없는_할_일() {
			given(choreRepository.findByIdAndFamilyId(CHORE_ID, FAMILY_ID)).willReturn(Optional.empty());

			assertThatThrownBy(() -> request())
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_NOT_FOUND);
		}

		@Test
		@DisplayName("형제에게 배정된 할 일이면 404다")
		void 남의_할_일() {
			Chore siblings = todayChore();
			ReflectionTestUtils.setField(siblings, "childId", SIBLING_ID);
			givenChore(siblings);

			// 같은 가족이라도 경로와 어긋나면 없는 것으로 본다.
			assertThatThrownBy(() -> request())
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_NOT_FOUND);
		}

		@Test
		@DisplayName("지워진 할 일에는 새 요청을 받지 않는다")
		void 지워진_할_일() {
			Chore deleted = todayChore();
			deleted.softDelete();
			givenChore(deleted);

			assertThatThrownBy(() -> request())
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);

			verify(completionRequestRepository, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("기간이 지났으면 400이다")
		void 기간_밖() {
			LocalDate yesterday = LocalDate.now(KST).minusDays(1);
			givenChore(chore(yesterday, yesterday, 500));

			assertThatThrownBy(() -> request())
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
		}

		@Test
		@DisplayName("보상이 아직 산정되지 않았으면 막는다")
		void 보상_미산정() {
			LocalDate today = LocalDate.now(KST);
			givenChore(chore(today, today, null));

			// 승인해도 줄 금액이 없다. 상점의 PRICE_NOT_EVALUATED 와 같은 자리다.
			assertThatThrownBy(() -> request())
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REWARD_NOT_EVALUATED);

			verify(completionRequestRepository, never()).saveAndFlush(any());
		}

		@Test
		@DisplayName("이미 대기 중인 요청이 있으면 409다")
		void 대기_중_요청_존재() {
			givenChore(todayChore());
			given(completionRequestRepository.existsByChoreIdAndStatus(CHORE_ID, ChoreCompletionStatus.PENDING))
				.willReturn(true);

			assertThatThrownBy(() -> request())
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PENDING_REQUEST_EXISTS);
		}

		@Test
		@DisplayName("이번 주기에 이미 받았으면 409다")
		void 이번_주기에_이미_받음() {
			givenChore(todayChore());
			given(completionRequestRepository.hasApprovedSince(eq(CHORE_ID), any())).willReturn(true);

			// 받아 봐야 승인 단계에서 막히므로 부모 대기함에 쌓지 않는다.
			assertThatThrownBy(() -> request())
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_REWARDED);

			verify(completionRequestRepository, never()).saveAndFlush(any());
		}
	}

	@Nested
	@DisplayName("부모가 대기함을 볼 때")
	class GetCompletionRequests {

		private ChoreCompletionRow row() {
			return new ChoreCompletionRow(REQUEST_ID, CHORE_ID, "설거지", "김아이", PHOTO_KEY, "다 했어요",
				ChoreCompletionStatus.PENDING, OffsetDateTime.now(KST));
		}

		@Test
		@DisplayName("조회 결과를 그대로 담아 준다")
		void 목록을_담아_준다() {
			given(completionRequestRepository.findSummaries(FAMILY_ID, CHILD_ID, ChoreCompletionStatus.PENDING))
				.willReturn(List.of(row()));

			ChoreCompletionListResponse response = choreCompletionService.getCompletionRequests(
				PARENT_ID, FAMILY_ID, CHILD_ID, ChoreCompletionStatus.PENDING);

			assertThat(response.requests()).hasSize(1);
			assertThat(response.requests().getFirst().choreTitle()).isEqualTo("설거지");
			assertThat(response.requests().getFirst().childName()).isEqualTo("김아이");
		}

		@Test
		@DisplayName("저장된 키로 볼 수 있는 링크를 발급해 담는다")
		void 링크를_발급한다() {
			given(completionRequestRepository.findSummaries(FAMILY_ID, CHILD_ID, ChoreCompletionStatus.PENDING))
				.willReturn(List.of(row()));
			given(photoStorage.presignedUrl(PHOTO_KEY)).willReturn("https://bucket/photo?X-Amz-Signature=x");

			ChoreCompletionListResponse response = choreCompletionService.getCompletionRequests(
				PARENT_ID, FAMILY_ID, CHILD_ID, ChoreCompletionStatus.PENDING);

			// 버킷이 비공개라 키를 그대로 내려주면 부모 화면에 사진이 뜨지 않는다.
			assertThat(response.requests().getFirst().photoUrl())
				.isEqualTo("https://bucket/photo?X-Amz-Signature=x");
		}

		@Test
		@DisplayName("사진 키가 없는 행은 링크를 발급하지 않는다")
		void 키가_없으면_발급하지_않는다() {
			ChoreCompletionRow noPhoto = new ChoreCompletionRow(REQUEST_ID, CHORE_ID, "설거지", "김아이", null,
				"다 했어요", ChoreCompletionStatus.PENDING, OffsetDateTime.now(KST));
			given(completionRequestRepository.findSummaries(FAMILY_ID, CHILD_ID, ChoreCompletionStatus.PENDING))
				.willReturn(List.of(noPhoto));

			ChoreCompletionListResponse response = choreCompletionService.getCompletionRequests(
				PARENT_ID, FAMILY_ID, CHILD_ID, ChoreCompletionStatus.PENDING);

			// photo_key 가 NULL 인 행이 DDL 상 가능하다. 그대로 발급하면 키가 "null" 인
			// 링크가 나가고 부모 화면이 깨진다.
			assertThat(response.requests().getFirst().photoUrl()).isNull();
			verify(photoStorage, never()).presignedUrl(any());
		}

		@Test
		@DisplayName("비어 있어도 빈 목록을 준다")
		void 빈_목록() {
			given(completionRequestRepository.findSummaries(FAMILY_ID, CHILD_ID, ChoreCompletionStatus.PENDING))
				.willReturn(List.of());

			ChoreCompletionListResponse response = choreCompletionService.getCompletionRequests(
				PARENT_ID, FAMILY_ID, CHILD_ID, ChoreCompletionStatus.PENDING);

			assertThat(response.requests()).isEmpty();
		}

		@Test
		@DisplayName("상태 필터를 그대로 넘긴다")
		void 상태_필터를_넘긴다() {
			choreCompletionService.getCompletionRequests(PARENT_ID, FAMILY_ID, CHILD_ID,
				ChoreCompletionStatus.SUCCEED);

			verify(completionRequestRepository).findSummaries(FAMILY_ID, CHILD_ID, ChoreCompletionStatus.SUCCEED);
		}

		@Test
		@DisplayName("아이가 조회하면 403으로 막는다")
		void 아이는_볼_수_없다() {
			willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(accessValidator).requireParent(CHILD_ID);

			// 아이가 볼 목록은 완료 내역(CHORE-12)으로 따로 있다.
			assertThatThrownBy(() -> choreCompletionService.getCompletionRequests(
					CHILD_ID, FAMILY_ID, CHILD_ID, ChoreCompletionStatus.PENDING))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

			verify(completionRequestRepository, never()).findSummaries(any(), any(), any());
		}
	}

	@Nested
	@DisplayName("부모가 거절할 때")
	class RejectCompletion {

		private ChoreRejectionResponse reject() {
			return choreCompletionService.rejectCompletion(PARENT_ID, FAMILY_ID, CHILD_ID, REQUEST_ID);
		}

		@Test
		@DisplayName("REJECTED 로 바뀌고 처리 시각을 응답한다")
		void 정상_거절() {
			ChoreCompletionRequest pending = pendingRequest(CHILD_ID);
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.of(pending));

			ChoreRejectionResponse response = reject();

			assertThat(response.choreCompletionRequestId()).isEqualTo(REQUEST_ID);
			assertThat(response.status()).isEqualTo(ChoreCompletionStatus.REJECTED);
			assertThat(pending.getStatus()).isEqualTo(ChoreCompletionStatus.REJECTED);
			// 응답의 처리 시각은 엔티티에 기록된 값이어야 한다. 서비스가 따로 계산한 값을
			// 실어 보내면 DB 에 저장된 시각과 응답이 어긋날 수 있다.
			assertThat(response.processedAt()).isNotNull().isEqualTo(pending.getProcessedAt());
		}

		@Test
		@DisplayName("거절한 뒤에도 같은 주기에 다시 요청할 수 있다")
		void 거절_뒤_재요청_가능() {
			ChoreCompletionRequest rejected = pendingRequest(CHILD_ID);
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.of(rejected));
			reject();

			// 대기 중 중복 검사는 PENDING 만, 주기당 지급 판정은 SUCCEED 만 본다.
			// REJECTED 로 남은 행은 둘 중 어느 것에도 걸리지 않는다.
			givenChore(todayChore());
			given(completionRequestRepository.existsByChoreIdAndStatus(CHORE_ID, ChoreCompletionStatus.PENDING))
				.willReturn(false);
			given(completionRequestRepository.hasApprovedSince(eq(CHORE_ID), any())).willReturn(false);

			assertThat(request().status()).isEqualTo(ChoreCompletionStatus.PENDING);
		}

		@Test
		@DisplayName("부모가 아니면 403으로 막는다")
		void 부모만_거절할_수_있다() {
			willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(accessValidator).requireParent(PARENT_ID);

			assertThatThrownBy(this::reject)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
		}

		@Test
		@DisplayName("없는 요청이면 404다")
		void 없는_요청() {
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.empty());

			assertThatThrownBy(this::reject)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND);
		}

		@Test
		@DisplayName("경로의 아이와 요청의 주인이 다르면 404다")
		void 남의_요청() {
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID))
				.willReturn(Optional.of(pendingRequest(SIBLING_ID)));

			// 취소(아이)는 403 이지만 거절(부모)은 404 다. 부모는 남의 집 요청 ID 를 알 이유가 없다.
			assertThatThrownBy(this::reject)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND);
		}

		@Test
		@DisplayName("이미 처리된 요청이면 409다")
		void 이미_처리됨() {
			ChoreCompletionRequest approved = pendingRequest(CHILD_ID);
			approved.approve(OffsetDateTime.now(KST));
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.of(approved));

			// 승인된 것을 거절로 뒤집을 수는 없다. 이미 지급한 포인트를 상태 변경만으로 회수할 수 없다.
			assertThatThrownBy(this::reject)
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS);
		}
	}

	@Nested
	@DisplayName("요청을 취소할 때")
	class CancelCompletion {

		@Test
		@DisplayName("자기 대기 요청이면 CANCELLED 로 바꾼다")
		void 정상_취소() {
			ChoreCompletionRequest pending = pendingRequest(CHILD_ID);
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.of(pending));

			choreCompletionService.cancelCompletion(CHILD_ID, FAMILY_ID, CHILD_ID, REQUEST_ID);

			assertThat(pending.getStatus()).isEqualTo(ChoreCompletionStatus.CANCELLED);
			assertThat(pending.getProcessedAt()).isNotNull();
		}

		@Test
		@DisplayName("행을 지우지 않는다")
		void 물리_삭제하지_않는다() {
			ChoreCompletionRequest pending = pendingRequest(CHILD_ID);
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.of(pending));

			choreCompletionService.cancelCompletion(CHILD_ID, FAMILY_ID, CHILD_ID, REQUEST_ID);

			// 취소 이력이 남아야 부모가 사라진 요청을 되짚을 수 있다.
			verify(completionRequestRepository, never()).delete(any());
		}

		@Test
		@DisplayName("없는 요청이면 404다")
		void 없는_요청() {
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.empty());

			assertThatThrownBy(() -> choreCompletionService.cancelCompletion(CHILD_ID, FAMILY_ID, CHILD_ID, REQUEST_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND);
		}

		@Test
		@DisplayName("남의 요청이면 403이다")
		void 남의_요청() {
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID))
				.willReturn(Optional.of(pendingRequest(SIBLING_ID)));

			assertThatThrownBy(() -> choreCompletionService.cancelCompletion(CHILD_ID, FAMILY_ID, CHILD_ID, REQUEST_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
		}

		@Test
		@DisplayName("이미 처리된 요청이면 409다")
		void 이미_처리됨() {
			ChoreCompletionRequest approved = pendingRequest(CHILD_ID);
			approved.approve(OffsetDateTime.now(KST));
			given(completionRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.of(approved));

			assertThatThrownBy(() -> choreCompletionService.cancelCompletion(CHILD_ID, FAMILY_ID, CHILD_ID, REQUEST_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS);
		}
	}
}
