package com.bana.dotori.global.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서비스 전체의 에러 코드.
 *
 * <p>응답의 {@code code} 필드에는 상수 이름이 그대로 나간다({@link ErrorResponse#of}).
 * 따라서 상수 이름은 API 명세에 적힌 코드와 정확히 같아야 한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 공통
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
	NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),

	// 회원
	DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다"),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),

	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다"),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다"),

	ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 연동된 계좌입니다"),
	AUTH_SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "1원 인증 요청이 없거나 만료되었습니다"),
	AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "1원 인증코드가 일치하지 않습니다"),
	AUTH_ATTEMPT_EXCEEDED(HttpStatus.BAD_REQUEST, "1원 인증 시도 횟수를 초과했습니다. 다시 요청해 주세요"),
	FINANCE_API_ERROR(HttpStatus.BAD_GATEWAY, "금융망 처리 중 오류가 발생했습니다");

	ALREADY_IN_FAMILY(HttpStatus.CONFLICT, "이미 가족에 소속되어 있습니다"),
	FAMILY_NOT_FOUND(HttpStatus.NOT_FOUND, "소속된 가족을 찾을 수 없습니다"),
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 자녀가 가족에 없습니다"),
	INVALID_FAMILY_CODE(HttpStatus.NOT_FOUND, "유효하지 않은 가족 초대 코드입니다"),

	// 포인트 지갑 (WLT)
	INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "포인트 금액은 1 이상이어야 합니다"),
	INVALID_CHARGE_UNIT(HttpStatus.BAD_REQUEST, "충전 금액은 10원 단위여야 합니다"),
	INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "보유 포인트가 부족합니다"),
	POINT_BALANCE_EXCEEDED(HttpStatus.CONFLICT, "보유 가능한 최대 포인트를 초과했습니다"),
	WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "포인트 지갑을 찾을 수 없습니다"),
	DUPLICATE_POINT_REQUEST(HttpStatus.CONFLICT, "이미 처리된 요청입니다"),
	NOT_FAMILY_CHILD(HttpStatus.FORBIDDEN, "해당 가족의 자녀가 아닙니다"),
	INVALID_TRANSACTION_REASON(HttpStatus.INTERNAL_SERVER_ERROR, "거래 사유와 방향이 맞지 않습니다"),

	// 계좌 (ACC) - 계좌 연동이 구현되면 그쪽에서 함께 쓴다
	BANK_TRANSFER_FAILED(HttpStatus.BAD_GATEWAY, "계좌 송금에 실패했습니다"),
	INSUFFICIENT_ACCOUNT_BALANCE(HttpStatus.BAD_REQUEST, "계좌 잔액이 부족합니다"),
	ACCOUNT_NOT_LINKED(HttpStatus.NOT_FOUND, "연동된 계좌가 없습니다"),

	// 상점 (SHOP)
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다"),
	OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 없습니다"),
	DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "오늘 이미 구매한 상품입니다");

	private final HttpStatus status;
	private final String message;
}
