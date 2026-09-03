package com.bana.dotori.family.service;

import java.security.SecureRandom;
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
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {

	// 초대 코드 문자셋 — 아이가 보고 입력하기 쉽도록 혼동되는 문자(0, O, 1, I)는 제외
	private static final String CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int CODE_LENGTH = 6;
	private static final Duration CODE_TTL = Duration.ofMinutes(10);
	private static final int CODE_GENERATION_MAX_ATTEMPTS = 10;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
		if (familyMemberRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
			throw new BusinessException(ErrorCode.ALREADY_IN_FAMILY);
		}

		Family family = familyRepository.save(familyMapper.toEntity(request, userId));
		familyMemberRepository.save(FamilyMember.builder()
			.family(family)
			.user(user)
			.build());
		return familyMapper.toCreateResponse(family);
	}

	@Override
	@Transactional(readOnly = true)
	public FamilyResponse getFamily(Integer userId, Integer familyId) {
		Family family = findMemberFamily(userId, familyId);
		int memberCount = familyMemberRepository.countByFamilyIdAndDeletedAtIsNull(familyId);
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
		if (!familyMemberRepository.existsByFamilyIdAndUserIdAndDeletedAtIsNull(familyId, userId)) {
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
		if (familyMemberRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
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
			StringBuilder builder = new StringBuilder(CODE_LENGTH);
			for (int i = 0; i < CODE_LENGTH; i++) {
				builder.append(CODE_CHARACTERS.charAt(SECURE_RANDOM.nextInt(CODE_CHARACTERS.length())));
			}
			String code = builder.toString();
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
		if (!familyMemberRepository.existsByFamilyIdAndUserIdAndDeletedAtIsNull(familyId, userId)) {
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
