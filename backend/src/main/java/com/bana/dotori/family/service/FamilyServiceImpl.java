package com.bana.dotori.family.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.dto.ConnectedChild;
import com.bana.dotori.family.dto.FamilyConnectRequest;
import com.bana.dotori.family.dto.FamilyConnectResponse;
import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.dto.FamilyMembersResponse;
import com.bana.dotori.family.dto.FamilyResponse;
import com.bana.dotori.family.dto.FamilyUpdateRequest;
import com.bana.dotori.family.dto.FamilyUpdateResponse;
import com.bana.dotori.family.dto.InvitationCodeResponse;
import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.entity.FamilyCode;
import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.mapper.FamilyMapper;
import com.bana.dotori.family.repository.FamilyCodeRepository;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.util.PointExchangePolicy;
import com.bana.dotori.global.util.RandomCodeGenerator;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {

	private static final int CODE_LENGTH = 6;
	private static final Duration CODE_TTL = Duration.ofMinutes(10);
	private static final int CODE_GENERATION_MAX_ATTEMPTS = 10;
	/** 온보딩 금액(기본 용돈·추가 보상 한도)의 최소값이자 입력 단위(원). */
	private static final int ONBOARDING_AMOUNT_UNIT_WON = 1000;

	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final FamilyCodeRepository familyCodeRepository;
	private final UserRepository userRepository;
	private final FamilyMapper familyMapper;

	@Override
	@Transactional
	public FamilyCreateResponse create(Integer userId, FamilyCreateRequest request) {
		User user = findUserForUpdate(userId);
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (familyMemberRepository.existsActiveMembership(userId)) {
			throw new BusinessException(ErrorCode.ALREADY_IN_FAMILY);
		}

		// 온보딩 금액은 원으로 들어와 포인트로 저장한다. DB 를 건드리기 전에 환산부터 한다.
		int allowancePoints = toPoints(request.weeklyAllowance(), "기본 용돈");
		int rewardLimitPoints = toPoints(request.weeklyRewardLimit(), "추가 보상 한도");

		Family family = familyRepository.save(
			familyMapper.toEntity(request, userId, allowancePoints, rewardLimitPoints));
		familyMemberRepository.save(FamilyMember.builder()
			.family(family)
			.user(user)
			.build());
		return familyMapper.toCreateResponse(family);
	}

	/**
	 * 온보딩 금액(원)을 포인트로 환산한다 (10원 = 1포인트, {@link PointExchangePolicy}).
	 *
	 * <p>최소 {@value ONBOARDING_AMOUNT_UNIT_WON}원, {@value ONBOARDING_AMOUNT_UNIT_WON}원 단위만
	 * 받는다 (기획 정책 — 화면의 금액 프리셋도 천원 단위다). 1000원 단위는 항상 10원 단위이므로
	 * 포인트 환산에서 잔돈이 사라질 일도 없다.
	 */
	private int toPoints(int won, String fieldLabel) {
		if (won < ONBOARDING_AMOUNT_UNIT_WON || won % ONBOARDING_AMOUNT_UNIT_WON != 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST,
				fieldLabel + "은(는) " + ONBOARDING_AMOUNT_UNIT_WON + "원 이상, "
					+ ONBOARDING_AMOUNT_UNIT_WON + "원 단위여야 합니다. 요청 금액=" + won);
		}
		return PointExchangePolicy.toPoints(won);
	}

	@Override
	@Transactional
	public FamilyUpdateResponse updateFamily(Integer userId, Integer familyId, FamilyUpdateRequest request) {
		User user = findUser(userId);
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		Family family = findMemberFamily(userId, familyId);
		// 가족 관리는 생성자 한 명이 한다 — 삭제(deleteFamily)와 같은 기준
		if (!family.getCreatedBy().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		// 부분 수정이라 안 온(null) 금액은 환산도 하지 않고 그대로 둔다.
		Integer allowancePoints = request.weeklyAllowance() == null
			? null : toPoints(request.weeklyAllowance(), "기본 용돈");
		Integer rewardLimitPoints = request.weeklyRewardLimit() == null
			? null : toPoints(request.weeklyRewardLimit(), "추가 보상 한도");

		family.updateSettings(request.name(), allowancePoints, rewardLimitPoints,
			request.financialKnowledgeLevel());

		// updatedAt 은 @UpdateTimestamp 가 flush 시점에 채운다.
		// 먼저 flush 하지 않으면 응답에 갱신 전 값이 담긴다 (회원 정보 수정과 같은 이유).
		return familyMapper.toUpdateResponse(familyRepository.saveAndFlush(family));
	}

	@Override
	@Transactional(readOnly = true)
	public FamilyResponse getFamily(Integer userId, Integer familyId) {
		Family family = findMemberFamily(userId, familyId);
		int memberCount = familyMemberRepository.countActiveMembers(familyId);
		return familyMapper.toFamilyResponse(family, memberCount);
	}

	@Override
	@Transactional(readOnly = true)
	public FamilyMembersResponse getMembers(Integer userId, Integer familyId) {
		findMemberFamily(userId, familyId);
		return familyMapper.toMembersResponse(familyMemberRepository.findAllWithUserByFamilyId(familyId));
	}

	@Override
	@Transactional
	public void deleteFamily(Integer userId, Integer familyId) {
		User user = findUser(userId);
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		Family family = familyRepository.findByIdAndDeletedAtIsNull(familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		if (!family.getCreatedBy().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		familyMemberRepository.findAllByFamilyIdAndDeletedAtIsNull(familyId)
			.forEach(FamilyMember::softDelete);
		family.softDelete();
	}

	@Override
	@Transactional
	public void removeChild(Integer userId, Integer familyId, Integer childId) {
		User user = findUser(userId);
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		findMemberFamily(userId, familyId);
		FamilyMember childMember = familyMemberRepository.findWithUserByFamilyIdAndUserId(familyId, childId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		// 부모 구성원을 제외 대상으로 지정하는 것을 막는다 — 자녀만 제외 가능
		if (childMember.getUser().getRole() != UserRole.CHILD) {
			throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
		}
		childMember.softDelete();
	}

	@Override
	@Transactional
	public InvitationCodeResponse createInvitationCode(Integer userId, Integer familyId) {
		User user = findUser(userId);
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		// 가족 행을 잠가 동시 재발급(더블탭·부모 2명)을 직렬화한다 — 락 없이는 delete-insert가 겹쳐 유효 코드가 2개 남을 수 있다
		Family family = familyRepository.findByIdAndDeletedAtIsNullForUpdate(familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		if (!familyMemberRepository.existsActiveMember(familyId, userId)) {
			throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
		}

		// 재발급 시 기존 코드는 무효화하고 가족당 유효 코드를 하나만 유지한다
		familyCodeRepository.deleteAllByFamilyId(familyId);
		FamilyCode familyCode = familyCodeRepository.save(FamilyCode.builder()
			.family(family)
			.code(generateUniqueCode())
			.expiredAt(OffsetDateTime.now().plus(CODE_TTL))
			.build());
		return familyMapper.toInvitationCodeResponse(familyCode);
	}

	@Override
	@Transactional
	public FamilyConnectResponse connect(Integer userId, FamilyConnectRequest request) {
		User user = findUserForUpdate(userId);
		if (user.getRole() != UserRole.CHILD) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (familyMemberRepository.existsActiveMembership(userId)) {
			throw new BusinessException(ErrorCode.ALREADY_IN_FAMILY);
		}

		String code = request.code().strip().toUpperCase(Locale.ROOT);
		FamilyCode familyCode = familyCodeRepository.findWithFamilyByCode(code)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_FAMILY_CODE));
		Family family = familyCode.getFamily();
		// 만료됐거나 이미 해체된 가족의 코드는 존재하지 않는 코드와 동일하게 취급한다
		if (familyCode.isExpired() || family.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.INVALID_FAMILY_CODE);
		}

		FamilyMember member = familyMemberRepository.save(FamilyMember.builder()
			.family(family)
			.user(user)
			.build());
		return familyMapper.toConnectResponse(family, member);
	}

	private String generateUniqueCode() {
		for (int attempt = 0; attempt < CODE_GENERATION_MAX_ATTEMPTS; attempt++) {
			String code = RandomCodeGenerator.generate(CODE_LENGTH);
			if (!familyCodeRepository.existsByCode(code)) {
				return code;
			}
		}
		throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
	}

	private User findUser(Integer userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}

	private User findUserForUpdate(Integer userId) {
		return userRepository.findByIdForUpdate(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}

	// 가족이 없거나 요청자가 그 가족의 구성원이 아니면 모두 FAMILY_NOT_FOUND로 처리해 타 가족 정보 노출을 막는다
	private Family findMemberFamily(Integer userId, Integer familyId) {
		Family family = familyRepository.findByIdAndDeletedAtIsNull(familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		if (!familyMemberRepository.existsActiveMember(familyId, userId)) {
			throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
		}
		return family;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ConnectedChild> findConnectedChildren(Integer parentUserId) {
		return familyMemberRepository.findConnectedChildren(parentUserId, UserRole.CHILD).stream()
			.map(member -> new ConnectedChild(
				member.getUser().getId(), member.getUser().getName(), member.getJoinedAt()))
			.toList();
	}
}
