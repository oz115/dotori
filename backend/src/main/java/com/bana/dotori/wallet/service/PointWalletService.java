package com.bana.dotori.wallet.service;

import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionType;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.exception.DuplicatePointRequestException;
import com.bana.dotori.wallet.exception.PointWalletNotFoundException;
import com.bana.dotori.wallet.repository.PointTransactionRepository;
import com.bana.dotori.wallet.repository.PointWalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포인트 잔액을 바꾸는 유일한 통로.
 *
 * <p>충전(WLT-01)·지급(WLT-02)·차감(WLT-05)은 물론 구매·환전·예적금·투자·퀴즈 보상까지
 * 포인트가 움직이는 모든 경로가 이 서비스를 거친다.
 * 잔액과 거래이력을 함께 저장하는 책임을 한 곳에 모아야
 * "잔액과 이력이 항상 일치한다"는 불변식을 지킬 수 있기 때문이다.
 *
 * <p>동시성·멱등성 처리 순서가 이 클래스의 핵심이다. 자세한 내용은
 * {@link #changeBalance} 의 주석을 참고.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointWalletService {

	private final PointWalletRepository pointWalletRepository;
	private final PointTransactionRepository pointTransactionRepository;

	/**
	 * 포인트를 지급한다. 충전·지급·보상·환불이 모두 여기로 들어온다.
	 */
	@Transactional
	public PointChangeResult deposit(PointChangeCommand command) {
		return changeBalance(command, PointTransactionType.DEPOSIT);
	}

	/**
	 * 포인트를 차감한다. 구매·환전·예적금 가입·투자 매수가 모두 여기로 들어온다.
	 */
	@Transactional
	public PointChangeResult withdraw(PointChangeCommand command) {
		return changeBalance(command, PointTransactionType.WITHDRAWAL);
	}

	/**
	 * 지갑이 없으면 잔액 0으로 만들어 준다. 회원가입·가족 합류 시점에 호출한다.
	 *
	 * <p>동시에 호출되면 {@code UX_POINT_WALLETS_USER} 제약에 걸려 한 쪽만 성공한다.
	 * 실패한 쪽은 재호출하면 이미 만들어진 지갑을 조회하게 된다.
	 */
	@Transactional
	public PointWallet createWalletIfAbsent(Integer userId) {
		return pointWalletRepository.findByUserId(userId)
				.orElseGet(() -> pointWalletRepository.save(PointWallet.openFor(userId)));
	}

	@Transactional(readOnly = true)
	public int getBalance(Integer userId) {
		return pointWalletRepository.findByUserId(userId)
				.orElseThrow(() -> new PointWalletNotFoundException(userId))
				.getBalance();
	}

	private PointChangeResult changeBalance(PointChangeCommand command, PointTransactionType transactionType) {
		// 1. 먼저 지갑 행을 잠근다.
		//    같은 지갑에 대한 모든 잔액 변경이 이 지점에서 직렬화되므로,
		//    잔액 검사와 차감 사이에 다른 트랜잭션이 끼어들 수 없다.
		PointWallet wallet = pointWalletRepository.findByUserIdForUpdate(command.userId())
				.orElseThrow(() -> new PointWalletNotFoundException(command.userId()));

		// 2. 멱등성 확인은 반드시 락을 잡은 뒤에 한다.
		//    락 이전에 확인하면 동시에 들어온 같은 요청이 둘 다 "없음"으로 판정해 중복 반영된다.
		//    락 이후에는 READ COMMITTED 기준으로 앞선 트랜잭션이 커밋한 거래가 보인다.
		if (command.hasIdempotencyKey()) {
			Optional<PointTransaction> alreadyProcessed =
					pointTransactionRepository.findByIdempotencyKey(command.idempotencyKey());
			if (alreadyProcessed.isPresent()) {
				log.info("duplicate point request ignored: key={}, userId={}",
						command.idempotencyKey(), command.userId());
				return PointChangeResult.replayed(alreadyProcessed.get());
			}
		}

		// 3. 금액·잔액 규칙 검증과 잔액 변경은 도메인이 담당한다.
		int balanceAfter = transactionType == PointTransactionType.DEPOSIT
				? wallet.deposit(command.amount())
				: wallet.withdraw(command.amount());

		// 4. 잔액 변경과 이력 저장이 같은 트랜잭션 안에 있다.
		//    둘 중 하나라도 실패하면 전체가 롤백되어 잔액과 이력이 어긋나지 않는다.
		PointTransaction transaction = PointTransaction.record(
				wallet.getWalletId(), transactionType, command.reason(),
				command.amount(), balanceAfter, command.description(), command.idempotencyKey());

		try {
			// 유니크 인덱스 위반을 이 자리에서 잡으려면 flush 가 필요하다.
			pointTransactionRepository.saveAndFlush(transaction);
		} catch (DuplicateKeyException e) {
			// 멱등키 유니크 위반만 잡는다. CHECK·외래키 위반까지 여기서 삼키면
			// 전혀 다른 문제가 "이미 처리된 요청"으로 둔갑해 원인을 못 찾는다.
			// 같은 멱등키를 서로 다른 지갑에 쓴 경우가 여기 해당한다.
			throw new DuplicatePointRequestException(command.idempotencyKey(), e);
		}

		return PointChangeResult.applied(transaction);
	}
}
