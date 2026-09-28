package com.bana.dotori.wallet.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bana.dotori.user.service.RemainingAssetReader;
import com.bana.dotori.wallet.entity.PointCashoutRequestStatus;
import com.bana.dotori.wallet.repository.PointCashoutRequestRepository;

import lombok.RequiredArgsConstructor;

/**
 * 잔여 자산 판정 중 "처리되지 않은 환전 요청" 부분.
 *
 * <p>환전 요청은 접수 즉시 포인트를 차감하므로, 요청이 대기 중일 때는 지갑 잔액이 0 이어도
 * 그 포인트는 아직 자녀의 것이다. 승인·거절·취소로 끝나야 정산되므로 그 전에는 자산이 남은 것으로 본다.
 * (가족이 해체되면 승인할 부모도 없어져 대기 요청이 영구 보류된다.)
 */
@Component
@RequiredArgsConstructor
public class PendingCashoutRemainingAssetReader implements RemainingAssetReader {

	private static final List<PointCashoutRequestStatus> UNSETTLED = List.of(
		PointCashoutRequestStatus.PENDING, PointCashoutRequestStatus.PROGRESS);

	private final PointCashoutRequestRepository pointCashoutRequestRepository;

	@Override
	public boolean hasRemainingAsset(Integer userId) {
		return pointCashoutRequestRepository.existsByChildIdAndStatusIn(userId, UNSETTLED);
	}
}
