package com.bana.dotori.store.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.store.dto.StoreProductCreateRequest;
import com.bana.dotori.store.dto.StoreProductDetailResponse;
import com.bana.dotori.store.dto.StoreProductListResponse;
import com.bana.dotori.store.dto.StoreProductUpdateRequest;
import com.bana.dotori.store.entity.StoreProduct;
import com.bana.dotori.store.event.StoreProductCreatedEvent;
import com.bana.dotori.store.mapper.StoreProductMapper;
import com.bana.dotori.store.repository.StoreProductRepository;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreProductServiceImpl implements StoreProductService {

	private final StoreProductRepository storeProductRepository;
	private final FamilyRepository familyRepository;
	private final FamilyMemberRepository familyMemberRepository;
	private final UserRepository userRepository;
	private final StoreProductMapper storeProductMapper;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional(readOnly = true)
	public StoreProductListResponse getProducts(Integer userId, Integer familyId) {
		validateFamilyMember(userId, familyId);
		return storeProductMapper.toListResponse(
			storeProductRepository.findAllByFamilyIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(familyId));
	}

	@Override
	@Transactional(readOnly = true)
	public StoreProductDetailResponse getProduct(Integer userId, Integer familyId, Integer productId) {
		validateFamilyMember(userId, familyId);
		StoreProduct product = storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(productId, familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		return storeProductMapper.toDetailResponse(product);
	}

	@Override
	@Transactional
	public StoreProductDetailResponse createProduct(Integer userId, Integer familyId,
			StoreProductCreateRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		validateFamilyMember(userId, familyId);

		StoreProduct product = storeProductRepository.save(StoreProduct.builder()
			.familyId(familyId)
			.createdBy(userId)
			.name(request.name())
			.description(request.description())
			.category(request.category())
			.quantity(request.quantity())
			.build());

		// value/price 는 AI 서버가 비동기로 채운다 — 커밋 이후에만 발행되도록 이벤트로 넘긴다
		eventPublisher.publishEvent(new StoreProductCreatedEvent(product.getId(), familyId, product.getName(),
			product.getDescription(), product.getCategory()));

		return storeProductMapper.toDetailResponse(product);
	}

	@Override
	@Transactional
	public StoreProductDetailResponse updateProduct(Integer userId, Integer familyId, Integer productId,
			StoreProductUpdateRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		validateFamilyMember(userId, familyId);
		if (request.name() != null && request.name().isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		StoreProduct product = storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(productId, familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		product.update(request.name(), request.description(), request.category(), request.price(),
			request.quantity());

		return storeProductMapper.toDetailResponse(product);
	}

	@Override
	@Transactional
	public void deleteProduct(Integer userId, Integer familyId, Integer productId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (user.getRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		validateFamilyMember(userId, familyId);

		StoreProduct product = storeProductRepository.findByIdAndFamilyIdAndDeletedAtIsNull(productId, familyId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		product.softDelete();
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
