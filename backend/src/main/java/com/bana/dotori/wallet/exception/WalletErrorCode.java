package com.bana.dotori.wallet.exception;

import org.springframework.http.HttpStatus;

import com.bana.dotori.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포인트 지갑 도메인의 에러 코드.
 *
 * <p>규칙 위반 하나당 코드 하나를 부여해, 테스트 실패 메시지만 보고
 * 어떤 검증이 깨졌는지 바로 알 수 있게 한다.
 */
@Getter
@RequiredArgsConstructor
public enum WalletErrorCode implements ErrorCode {

	INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_POINT_AMOUNT",
			"포인트 금액은 1 이상이어야 합니다"),

	INSUFFICIENT_POINT(HttpStatus.CONFLICT, "INSUFFICIENT_POINT",
			"보유 포인트가 부족합니다"),

	POINT_BALANCE_EXCEEDED(HttpStatus.CONFLICT, "POINT_BALANCE_EXCEEDED",
			"보유 가능한 최대 포인트를 초과했습니다"),

	WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND",
			"포인트 지갑을 찾을 수 없습니다"),

	DUPLICATE_POINT_REQUEST(HttpStatus.CONFLICT, "DUPLICATE_POINT_REQUEST",
			"이미 처리된 요청입니다"),

	BANK_TRANSFER_FAILED(HttpStatus.BAD_GATEWAY, "BANK_TRANSFER_FAILED",
			"계좌 송금에 실패했습니다"),

	// 아래 둘은 API 명세(WLT-01)에 정의된 코드다. 계좌 도메인(ACC)이 생기면 그쪽으로 옮긴다.
	INSUFFICIENT_ACCOUNT_BALANCE(HttpStatus.BAD_REQUEST, "INSUFFICIENT_ACCOUNT_BALANCE",
			"계좌 잔액이 부족합니다"),

	ACCOUNT_NOT_LINKED(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_LINKED",
			"연동된 계좌가 없습니다");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
