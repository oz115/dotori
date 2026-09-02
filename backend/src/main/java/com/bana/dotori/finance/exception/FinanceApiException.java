package com.bana.dotori.finance.exception;

import lombok.Getter;

@Getter
public class FinanceApiException extends RuntimeException {

	private final String responseCode;

	public FinanceApiException(String message) {
		super(message);
		this.responseCode = null;
	}

	public FinanceApiException(String responseCode, String message) {
		super(String.format("[%s] %s", responseCode, message));
		this.responseCode = responseCode;
	}

	public FinanceApiException(String message, Throwable cause) {
		super(message, cause);
		this.responseCode = null;
	}
}
