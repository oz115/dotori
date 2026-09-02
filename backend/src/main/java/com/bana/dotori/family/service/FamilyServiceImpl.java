package com.bana.dotori.family.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.dto.FamilyMembersResponse;
import com.bana.dotori.family.dto.FamilyResponse;
import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.mapper.FamilyMapper;
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

	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;
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
		if (!familyMemberRepository.existsByFamilyIdAndUserIdAndDeletedAtIsNull(familyId, userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		FamilyMember childMember = familyMemberRepository.findWithUserByFamilyIdAndUserId(familyId, childId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		// 부모 구성원을 제외 대상으로 지정하는 것을 막는다 — 자녀만 제외 가능
		if (childMember.getUser().getRole() != UserRole.CHILD) {
			throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
		}
		childMember.softDelete();
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
}
