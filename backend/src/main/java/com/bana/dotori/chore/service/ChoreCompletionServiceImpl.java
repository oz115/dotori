package com.bana.dotori.chore.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.chore.dto.ChoreCompletionCreateRequest;
import com.bana.dotori.chore.dto.ChoreCompletionListResponse;
import com.bana.dotori.chore.dto.ChoreCompletionResponse;
import com.bana.dotori.chore.dto.ChoreCompletionRow;
import com.bana.dotori.chore.dto.ChoreCompletionSummary;
import com.bana.dotori.chore.dto.ChoreRejectionResponse;
import com.bana.dotori.chore.entity.Chore;
import com.bana.dotori.chore.entity.ChoreCompletionRequest;
import com.bana.dotori.chore.entity.ChoreCompletionStatus;
import com.bana.dotori.chore.entity.ChorePhotoKey;
import com.bana.dotori.chore.event.ChoreCompletionRejectedEvent;
import com.bana.dotori.chore.event.ChoreCompletionRequestedEvent;
import com.bana.dotori.chore.mapper.ChoreCompletionMapper;
import com.bana.dotori.chore.repository.ChoreCompletionRequestRepository;
import com.bana.dotori.chore.repository.ChoreRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.storage.PhotoStorage;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChoreCompletionServiceImpl implements ChoreCompletionService {

	/**
	 * "오늘"을 KST 로 고정한다. 이유는 {@code ChoreServiceImpl.KST} 주석과 같다 —
	 * 시스템 기본 타임존을 따라가면 배포 환경에 따라 기간 판정이 하루씩 어긋난다.
	 */
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/** 대기 중 요청이 할 일당 하나임을 보장하는 부분 유니크 인덱스 이름 (소문자 비교용). */
	private static final String PENDING_INDEX = "ux_chore_completion_requests_pending";

	private final ChoreRepository choreRepository;
	private final ChoreCompletionRequestRepository completionRequestRepository;
	private final ChoreAccessValidator accessValidator;
	private final ChoreCompletionMapper completionMapper;
	private final PhotoStorage photoStorage;
	private final UserRepository userRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 아이가 완료를 인증한다 (CHORE-11).
	 *
	 * <p>막는 경우가 넷이다. 순서는 "요청이 성립하지 않는 이유"부터 "지금은 안 되는 이유" 순이다.
	 *
	 * <ol>
	 *   <li>지워진 할 일 — 새 요청은 받지 않는다. 이미 올라온 요청의 처리는 계속 된다.</li>
	 *   <li>기간 밖 — 오늘이 이번 주기 안이어야 한다.</li>
	 *   <li>보상 미산정 — AI 가 아직 값을 못 채웠으면 승인해도 줄 금액이 없다.
	 *       상점의 {@code PRICE_NOT_EVALUATED} 와 같은 자리다.</li>
	 *   <li>이미 대기 중이거나 이번 주기에 이미 받았음 — 둘 다 409.</li>
	 * </ol>
	 */
	@Override
	@Transactional
	public ChoreCompletionResponse requestCompletion(Integer userId, Integer familyId, Integer childId,
			Integer choreId, ChoreCompletionCreateRequest request) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireChild(userId);

		Chore chore = findChore(familyId, childId, choreId);

		if (chore.isDeleted()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "지워진 할 일에는 완료 요청을 보낼 수 없습니다");
		}
		if (!chore.isInPeriod(LocalDate.now(KST))) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST,
					"수행 기간이 아닙니다. choreId=" + choreId + ", 기간=" + chore.getStartDate() + "~" + chore.getEndDate());
		}
		if (chore.getRewardPoints() == null) {
			throw new BusinessException(ErrorCode.REWARD_NOT_EVALUATED);
		}
		// 사진 키는 클라이언트가 보내는 문자열이라 그대로 믿으면 안 된다. 남의 사진 키를 넣어
		// 자기 인증으로 쓰거나 우리 버킷 밖의 값을 넣을 수 있다. 업로드할 때 서버가 만든 키만
		// 통과하도록 형식과 소유(familyId·childId)를 함께 본다.
		if (!ChorePhotoKey.belongsTo(request.photoKey(), familyId, childId)) {
			throw new BusinessException(ErrorCode.INVALID_FILE,
					"사진 키가 이 아이의 것이 아닙니다. photoKey=" + request.photoKey());
		}
		if (completionRequestRepository.existsByChoreIdAndStatus(choreId, ChoreCompletionStatus.PENDING)) {
			throw new BusinessException(ErrorCode.PENDING_REQUEST_EXISTS);
		}

		// 이번 주기에 이미 지급됐으면 요청을 받아 봐야 승인 단계에서 막힌다.
		// 부모 대기함에 처리할 수 없는 항목을 쌓지 않으려고 여기서 끊는다.
		if (completionRequestRepository.hasApprovedSince(choreId, chore.currentPeriodStartedAt())) {
			throw new BusinessException(ErrorCode.ALREADY_REWARDED);
		}

		ChoreCompletionRequest saved = saveRequest(choreId, childId, request);

		// 부모에게 알린다. 이름은 알림 문구에 쓰려고 여기서 읽는다 - 자녀가 둘 이상이면
		// "완료 요청이 왔어요" 만으로는 누구 것인지 알 수 없다.
		String childName = userRepository.findById(childId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
				.getName();
		eventPublisher.publishEvent(new ChoreCompletionRequestedEvent(
				choreId, chore.getParentId(), chore.getTitle(), childName));

		return completionMapper.toResponse(saved);
	}

	/**
	 * 완료 요청을 넣는다. 대기 중 중복은 DB 가 최종적으로 막는다.
	 *
	 * <p>위 {@code existsByChoreIdAndStatus} 검사는 원자적이지 않다. 검사와 INSERT 사이에
	 * 틈이 있어서 아이가 버튼을 연타하면 두 요청이 모두 통과해 PENDING 이 여러 건 쌓이고,
	 * 부모 대기함에 같은 항목이 여러 줄 뜬다. 지급은 멱등키가 한 번으로 막지만 대기함은
	 * 지저분해진다.
	 *
	 * <p>그 틈을 {@code UX_CHORE_COMPLETION_REQUESTS_PENDING} 부분 유니크 인덱스가 닫는다.
	 * PENDING 행만 유일성을 따지므로 처리가 끝난 행은 몇 건이든 남고, 거절당한 아이는
	 * 같은 주기에 다시 요청할 수 있다.
	 *
	 * <p>위반을 잡아 409 로 바꾸지 않으면 500 이 나간다. 지갑이 멱등키 위반을 다루는 방식과
	 * 같고, 다른 무결성 위반(외래키·CHECK)까지 삼키면 전혀 다른 문제가 "이미 대기 중"으로
	 * 둔갑해 원인을 못 찾으므로 이 인덱스 위반만 걸러 낸다.
	 */
	private ChoreCompletionRequest saveRequest(Integer choreId, Integer childId,
			ChoreCompletionCreateRequest request) {
		try {
			// 유니크 인덱스 위반을 이 자리에서 잡으려면 flush 가 필요하다.
			return completionRequestRepository.saveAndFlush(ChoreCompletionRequest.builder()
				.choreId(choreId)
				.childId(childId)
				.photoKey(request.photoKey())
				.description(request.description())
				.build());
		} catch (DataIntegrityViolationException e) {
			if (!isPendingIndexViolation(e)) {
				throw e;
			}
			throw new BusinessException(ErrorCode.PENDING_REQUEST_EXISTS,
					"이미 처리 대기 중인 요청이 있습니다. choreId=" + choreId + ", childId=" + childId, e);
		}
	}

	private boolean isPendingIndexViolation(DataIntegrityViolationException e) {
		// ddl-auto 는 none 이라 이 인덱스는 01-schema.sql 만 만든다. 이름이 그대로 메시지에
		// 실려 오므로 소문자로 맞춰 비교한다 (지갑도 대소문자를 맞춰 비교한다).
		return String.valueOf(e.getMostSpecificCause().getMessage())
				.toLowerCase(Locale.ROOT)
				.contains(PENDING_INDEX);
	}

	/**
	 * 아이가 자기 요청을 물린다 (CHORE-12).
	 *
	 * <p>행을 지우지 않고 CANCELLED 로 남긴다. 취소 이력이 남아야 부모가 "왜 사라졌지" 를
	 * 되짚을 수 있고, 주기당 지급 판정은 SUCCEED 만 보므로 남아 있어도 재요청을 막지 않는다.
	 */
	@Override
	@Transactional
	public void cancelCompletion(Integer userId, Integer familyId, Integer childId, Integer requestId) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireChild(userId);

		ChoreCompletionRequest completionRequest = completionRequestRepository.findByIdForUpdate(requestId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND));

		// 남의 요청이면 403 이다. 여기서는 404 로 숨기지 않는다 — 명세가 403 이고,
		// 요청 ID 는 아이가 자기 화면에서 이미 받아 본 값이라 존재를 감출 실익이 없다.
		if (!completionRequest.getChildId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		// 상태 검사는 엔티티가 한다. 이미 처리된 요청이면 INVALID_STATUS 로 409 가 나간다.
		completionRequest.cancel(OffsetDateTime.now(KST));
	}

	/**
	 * 완료 요청 목록을 본다 (CHORE-08). 부모는 대기함으로, 아이는 "내가 보낸 요청"으로 쓴다.
	 *
	 * <p>기본 필터는 PENDING 이다. 이 화면은 "지금 처리할 것"을 보러 오는 곳이라
	 * 아무 것도 지정하지 않았을 때 처리 끝난 이력까지 섞어 주면 목록이 쓸데없이 길어진다.
	 *
	 * <p><b>아이도 부른다.</b> 한동안 {@code requireParent} 로 부모만 통과시켰는데, 그러면
	 * 아이 쪽이 두 군데서 막힌다. 하나는 할 일 목록의 "확인 중" 표시다 — 이미 보낸 요청을
	 * 구분하지 못해 다시 눌렀다가 {@code PENDING_REQUEST_EXISTS} 를 보게 된다. 다른 하나는
	 * 요청 취소(CHORE-12)로, 아이 전용 API 인데 거기 필요한 requestId 를 얻을 곳이 이 목록뿐이라
	 * 사실상 호출할 수 없었다.
	 *
	 * <p>열어도 새로 보이는 것은 "아이 자신의 요청"뿐이다. 경로가 아이를 지목하고
	 * {@link ChoreAccessValidator#validate} 가 아이 호출자에게 자기 자신만 허용하므로,
	 * 형제 것은 403 이고 남의 가족은 404 다. 승인·거절(CHORE-09·10)은 그대로 부모 전용이다.
	 *
	 * <p>사진은 저장된 키를 그대로 내보내지 않는다. 버킷이 비공개라 키로는 볼 수 없으므로
	 * 건마다 presigned URL 을 발급해 담는다. 발급은 서명 계산뿐이라 네트워크를 타지 않아
	 * 목록 길이만큼 불러도 왕복 비용이 없다.
	 */
	@Override
	@Transactional(readOnly = true)
	public ChoreCompletionListResponse getCompletionRequests(Integer userId, Integer familyId, Integer childId,
			ChoreCompletionStatus status) {
		accessValidator.validate(userId, familyId, childId);

		return completionMapper.toListResponse(
			completionRequestRepository.findSummaries(familyId, childId, status).stream()
				.map(this::toSummary)
				.toList());
	}

	/**
	 * 저장된 키를 볼 수 있는 링크로 바꿔 응답 한 줄을 만든다.
	 *
	 * <p>발급만 여기서 하고 필드를 옮기는 일은 매퍼에 맡긴다. 매퍼가 저장소에 의존하면
	 * 서비스 테스트가 매퍼까지 모킹해야 해서, 발급 결과 문자열만 넘긴다.
	 *
	 * <p>키가 없는 행도 있을 수 있다. 사진이 필수가 되기 전(CHORE-13 이전)에 들어온 요청이라
	 * null 을 그대로 흘려보낸다 — 없는 키로 링크를 만들면 열었을 때 404 가 나는 죽은 URL 이
	 * 응답에 실린다.
	 */
	private ChoreCompletionSummary toSummary(ChoreCompletionRow row) {
		String photoUrl = row.photoKey() == null ? null : photoStorage.presignedUrl(row.photoKey());
		return completionMapper.toSummary(row, photoUrl);
	}

	/**
	 * 부모가 완료를 거절한다 (CHORE-10).
	 *
	 * <p>거절 사유는 받지 않는다. 담을 컬럼이 없고(아이가 쓴 description 을 덮어쓸 수는 없다)
	 * 사유가 향할 알림 도메인도 아직 없다. 팀 결정에 따라 이번 범위에서 뺐다.
	 *
	 * <p>거절해도 아이는 <b>같은 주기에 다시 요청할 수 있다.</b> 대기 중 중복 검사는
	 * PENDING 만 보고, 주기당 지급 판정은 SUCCEED 만 보므로 REJECTED 로 남은 행은
	 * 재요청을 막지 않는다. 재도전을 막으면 부모가 잘못 눌렀을 때 되돌릴 방법이 없다.
	 *
	 * <p>지급이 없으므로 승인과 달리 지갑을 건드리지 않는다. 트랜잭션도 이 한 행뿐이다.
	 */
	@Override
	@Transactional
	public ChoreRejectionResponse rejectCompletion(Integer userId, Integer familyId, Integer childId,
			Integer requestId) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireParent(userId);

		// 요청 ID 만으로 찾고 경로의 아이와 어긋나면 없는 것으로 본다. 403 을 주면
		// 그 requestId 가 실재한다는 사실이 새어 나간다 (승인과 같은 기준).
		ChoreCompletionRequest completionRequest = completionRequestRepository.findByIdForUpdate(requestId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND));
		if (!completionRequest.getChildId().equals(childId)) {
			throw new BusinessException(ErrorCode.CHORE_COMPLETION_REQUEST_NOT_FOUND);
		}

		// 상태 검사는 엔티티가 한다. 이미 처리된 요청이면 INVALID_STATUS 로 409 가 나간다.
		completionRequest.reject(OffsetDateTime.now(KST));

		// 알림 문구에 할 일 제목이 필요하다. 거절된 요청은 목록에서 사라지므로 아이가
		// 무엇이 거절됐는지 알림만 보고 알 수 있어야 한다.
		Chore chore = findChore(familyId, childId, completionRequest.getChoreId());
		eventPublisher.publishEvent(new ChoreCompletionRejectedEvent(
				chore.getId(), childId, chore.getTitle()));

		return completionMapper.toRejectionResponse(completionRequest);
	}

	/**
	 * 경로가 지목한 아이의 할 일을 집는다.
	 *
	 * <p>가족으로 먼저 좁히고 아이가 어긋나면 없는 것으로 본다. 404 로 돌려줘야
	 * 그 choreId 가 누구 것인지 짚어 보는 것을 막을 수 있다.
	 */
	private Chore findChore(Integer familyId, Integer childId, Integer choreId) {
		Chore chore = choreRepository.findByIdAndFamilyId(choreId, familyId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CHORE_NOT_FOUND));
		if (!chore.getChildId().equals(childId)) {
			throw new BusinessException(ErrorCode.CHORE_NOT_FOUND);
		}
		return chore;
	}
}
