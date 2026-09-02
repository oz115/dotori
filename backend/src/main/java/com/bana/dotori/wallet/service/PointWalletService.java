package com.bana.dotori.wallet.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionType;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
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

	/**
	 * 멱등키 유니크 위반을 다른 무결성 위반과 구별하기 위한 표식.
	 *
	 * <p>제약 이름 전체가 아니라 컬럼명만 본다. 이미 만들어진 DB 에는 단일 컬럼 제약
	 * ({@code uk_point_transactions_idempotency_key})이 남아 있을 수 있고,
	 * 새로 만든 DB 에는 복합 제약({@code uk_point_transactions_wallet_idempotency_key})이 있는데
	 * 둘 다 잡아야 하기 때문이다. CHECK·외래키 위반 메시지에는 이 문자열이 없다.
	 */
	private static final String IDEMPOTENCY_KEY_CONSTRAINT = "idempotency_key";

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
	 * 지갑이 없으면 잔액 0으로 만들어 준다. 첫 충전·지급 직전에 호출한다.
	 *
	 * <p><b>별도 트랜잭션에서 돈다.</b> 동시에 첫 거래가 두 건 들어오면 한 쪽이
	 * {@code uk_point_wallets_user_id} 제약에 걸리는데, 호출자와 같은 트랜잭션이라면
	 * PostgreSQL 이 그 트랜잭션을 abort 상태로 만들어 뒤이은 쿼리가 전부 실패한다.
	 * 트랜잭션을 분리해 두면 실패한 쪽만 되돌아가고, 호출자는 상대가 커밋한 지갑을 보게 된다.
	 *
	 * <p>충전이 나중에 롤백되면 잔액 0인 지갑만 남는다. 돈과 무관하고 다음 요청이 그대로
	 * 재사용하므로 되돌릴 필요가 없다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public PointWallet createWalletIfAbsent(Integer userId) {
		return pointWalletRepository.findByUserId(userId)
				.orElseGet(() -> pointWalletRepository.save(PointWallet.openFor(userId)));
	}

	private PointChangeResult changeBalance(PointChangeCommand command, PointTransactionType transactionType) {
		// 1. 먼저 지갑 행을 잠근다.
		//    같은 지갑에 대한 모든 잔액 변경이 이 지점에서 직렬화되므로,
		//    잔액 검사와 차감 사이에 다른 트랜잭션이 끼어들 수 없다.
		PointWallet wallet = pointWalletRepository.findByUserIdForUpdate(command.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND, "포인트 지갑을 찾을 수 없습니다. userId=" + command.userId()));

		// 2. 멱등성 확인은 반드시 락을 잡은 뒤에, 그리고 이 지갑 안에서만 한다.
		//    락 이전에 확인하면 동시에 들어온 같은 요청이 둘 다 "없음"으로 판정해 중복 반영된다.
		//    락 이후에는 READ COMMITTED 기준으로 앞선 트랜잭션이 커밋한 거래가 보인다.
		//    지갑으로 범위를 좁히지 않으면 다른 사용자가 같은 키를 쓴 거래가 걸려 나와,
		//    이번 요청이 반영되지 않은 채 남의 거래를 성공 응답으로 돌려주게 된다.
		if (command.hasIdempotencyKey()) {
			Optional<PointTransaction> alreadyProcessed = pointTransactionRepository
					.findByWalletIdAndIdempotencyKey(wallet.getWalletId(), command.idempotencyKey());
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
				wallet.getWalletId(), command.reason(),
				command.amount(), balanceAfter, command.description(), command.idempotencyKey());

		try {
			// 유니크 인덱스 위반을 이 자리에서 잡으려면 flush 가 필요하다.
			pointTransactionRepository.saveAndFlush(transaction);
		} catch (DataIntegrityViolationException e) {
			// JPA 경로에서 유니크 위반은 DataIntegrityViolationException 으로 올라온다.
			// DuplicateKeyException 을 잡으면 안 된다. 그쪽은 하위 타입이라 절대 걸리지 않고,
			// 스프링은 SQLExceptionTranslator 빈이 등록돼 있을 때만 그 타입을 만든다.
			//
			// 멱등키 위반만 409 로 바꾸고 나머지는 그대로 올려보낸다. CHECK·외래키 위반까지
			// 여기서 삼키면 전혀 다른 문제가 "이미 처리된 요청"으로 둔갑해 원인을 못 찾는다.
			if (!isIdempotencyKeyViolation(e)) {
				throw e;
			}
			throw new BusinessException(ErrorCode.DUPLICATE_POINT_REQUEST,
					"이미 처리된 요청 키입니다. walletId=" + wallet.getWalletId()
							+ ", idempotencyKey=" + command.idempotencyKey(), e);
		}

		return PointChangeResult.applied(transaction);
	}

	private boolean isIdempotencyKeyViolation(DataIntegrityViolationException e) {
		return String.valueOf(e.getMostSpecificCause().getMessage()).contains(IDEMPOTENCY_KEY_CONSTRAINT);
	}
}
