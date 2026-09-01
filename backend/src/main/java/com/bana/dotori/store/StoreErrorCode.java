package com.bana.dotori.store;

import org.springframework.http.HttpStatus;

import com.bana.dotori.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 상점 도메인 에러 코드. API 명세 SHOP-06 에 정의된 코드를 그대로 쓴다.
 */
@Getter
@RequiredArgsConstructor
public enum StoreErrorCode implements ErrorCode {

	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
			"상품을 찾을 수 없습니다"),

	OUT_OF_STOCK(HttpStatus.CONFLICT, "OUT_OF_STOCK",
			"재고가 없습니다"),

	DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "DAILY_LIMIT_EXCEEDED",
			"오늘 이미 구매한 상품입니다");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
