package com.bana.dotori.family.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
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
		User user = findUser(userId);
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (familyMemberRepository.existsByUserId(userId)) {
			throw new BusinessException(ErrorCode.ALREADY_IN_FAMILY);
		}

		Family family = familyRepository.save(familyMapper.toEntity(request, userId));
		familyMemberRepository.save(FamilyMember.builder()
			.family(family)
			.user(user)
			.build());
		return familyMapper.toCreateResponse(family);
	}

	private User findUser(Integer userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}
}
