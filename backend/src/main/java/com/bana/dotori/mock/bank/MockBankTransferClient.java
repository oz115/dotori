package com.bana.dotori.mock.bank;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 싸피 금융망 연동 전까지 쓰는 메모리 기반 목업.
 *
 * <p>계좌 연동(ACC-01·02)을 맡은 팀원의 작업이 끝나기 전에도 충전 API 를 완성하고
 * 검증할 수 있도록 만들었다. 계좌 표현은 API 명세 ACC-02·03 의 응답 형식
 * ({@link LinkedAccountSnapshot})을 그대로 따르므로, 실제 연동이 붙을 때
 * 이 클래스만 지우면 충전 로직은 손대지 않아도 된다.
 *
 * <p>명세에 정의된 실패도 그대로 재현한다.
 * 잔액 부족이면 400 INSUFFICIENT_ACCOUNT_BALANCE, 계좌가 없으면 404 ACCOUNT_NOT_LINKED 다.
 *
 * <p>계좌가 등록돼 있지 않으면 기본 잔액으로 하나 만들어 준다. 그래야 인증·계좌 기능이
 * 없는 상태에서도 충전 흐름을 시연할 수 있다. 실제 연동이 붙으면 이 동작은 사라진다.
 */
@Slf4j
@Component
@Profile("test")
public class MockBankTransferClient implements BankTransferClient {

	/** 명세 예시(500000원)를 그대로 쓴다. */
	private static final int DEFAULT_BALANCE = 500_000;
	private static final String DEFAULT_BANK_CODE = "088";
	private static final String DEFAULT_BANK_NAME = "신한은행";
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final Map<Integer, LinkedAccountSnapshot> accountsByUserId = new ConcurrentHashMap<>();
	private final AtomicInteger accountIdSequence = new AtomicInteger(1);

	@Override
	public synchronized BankTransferResult transferFromChildToParent(Integer familyId, Integer childId, int amount,
			String idempotencyKey) {
		Integer parentId = mockParentIdOf(familyId);

		LinkedAccountSnapshot childAccount = requireAccount("child", childId);
		LinkedAccountSnapshot parentAccount = requireAccount("parent", parentId);

		if (childAccount.balance() < amount) {
			throw new BusinessException(ErrorCode.INSUFFICIENT_ACCOUNT_BALANCE,
					"계좌 잔액이 부족합니다. childId=" + childId + ", 계좌 잔액=" + childAccount.balance()
							+ ", 송금 요청=" + amount);
		}

		OffsetDateTime now = OffsetDateTime.now(KST);
		LinkedAccountSnapshot debited = childAccount.withBalance(childAccount.balance() - amount, now);
		LinkedAccountSnapshot credited = parentAccount.withBalance(parentAccount.balance() + amount, now);
		accountsByUserId.put(childId, debited);
		accountsByUserId.put(parentId, credited);

		String externalTransactionId = "MOCK-" + UUID.randomUUID();
		log.warn("목업 송금이 실행됐다. 실제 이체는 일어나지 않는다."
						+ " familyId={}, childId={}, amount={}, externalTransactionId={}",
				familyId, childId, amount, externalTransactionId);

		return new BankTransferResult(externalTransactionId, amount);
	}

	/**
	 * 테스트가 계좌 잔액을 원하는 값으로 맞출 때 쓴다.
	 */
	public LinkedAccountSnapshot registerAccount(Integer userId, String accountName, int balance) {
		OffsetDateTime now = OffsetDateTime.now(KST);
		LinkedAccountSnapshot account = new LinkedAccountSnapshot(
				accountIdSequence.getAndIncrement(), DEFAULT_BANK_CODE, DEFAULT_BANK_NAME,
				"110-123-%06d".formatted(userId), accountName, balance, now, now);
		accountsByUserId.put(userId, account);
		return account;
	}

	/**
	 * 등록된 계좌를 연동 해제 상태로 만든다. ACCOUNT_NOT_LINKED 경로를 시험할 때 쓴다.
	 */
	public void unlinkAccount(Integer userId) {
		accountsByUserId.remove(userId);
	}

	public void clear() {
		accountsByUserId.clear();
	}

	public LinkedAccountSnapshot findAccount(Integer userId) {
		return accountsByUserId.get(userId);
	}

	private LinkedAccountSnapshot requireAccount(String owner, Integer userId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.ACCOUNT_NOT_LINKED,
					"연동된 계좌가 없습니다. owner=" + owner);
		}
		return accountsByUserId.computeIfAbsent(userId,
				id -> {
					log.warn("연동 계좌가 없어 목업 계좌를 즉석에서 만든다. userId={}", id);
					OffsetDateTime now = OffsetDateTime.now(KST);
					return new LinkedAccountSnapshot(accountIdSequence.getAndIncrement(),
							DEFAULT_BANK_CODE, DEFAULT_BANK_NAME, "110-123-%06d".formatted(id),
							owner + "-" + id, DEFAULT_BALANCE, now, now);
				});
	}

	/**
	 * 가족의 부모를 찾는 자리. family_members 조회가 들어올 곳이며,
	 * 지금은 가족당 하나의 가상 부모 계좌를 쓴다.
	 */
	private Integer mockParentIdOf(Integer familyId) {
		if (familyId == null) {
			return null;
		}
		return -familyId;
	}
}
