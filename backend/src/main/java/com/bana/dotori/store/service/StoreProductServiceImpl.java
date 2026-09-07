package com.bana.dotori.store.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.store.dto.StoreProductListResponse;
import com.bana.dotori.store.mapper.StoreProductMapper;
import com.bana.dotori.store.repository.StoreProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreProductServiceImpl implements StoreProductService {

	private final StoreProductRepository storeProductRepository;
	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final StoreProductMapper storeProductMapper;

	@Override
	@Transactional(readOnly = true)
	public StoreProductListResponse getProducts(Integer userId, Integer familyId) {
		validateFamilyMember(userId, familyId);
		return storeProductMapper.toListResponse(
			storeProductRepository.findAllByFamilyIdOrderByCreatedAtDescIdDesc(familyId));
	}

	/**
	 * 살아 있는 가족의 살아 있는 구성원인지 확인한다.
	 *
	 * <p>남의 가족 ID 를 찍어 보는 요청에는 403 대신 404 를 준다.
	 * 그 가족의 존재 자체를 알려 주지 않기 위해서다 (FamilyServiceImpl 과 같은 기준).
	 */
	private void validateFamilyMember(Integer userId, Integer familyId) {
		familyRepository.findByIdAndDeletedAtIsNull(familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_NOT_FOUND));
		if (!familyMemberRepository.existsActiveMember(familyId, userId)) {
			throw new BusinessException(ErrorCode.FAMILY_NOT_FOUND);
		}
	}
}
