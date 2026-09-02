package com.bana.dotori.finance.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.bana.dotori.finance.config.FinanceProperties;
import com.bana.dotori.finance.dto.common.FinanceCommonHeader;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FinanceHeaderGenerator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final int MAX_SEQUENCE = 999999;

	private final FinanceProperties properties;
	private final AtomicInteger sequence = new AtomicInteger(1);

	public FinanceCommonHeader createHeader(String apiName, String userKey) {
		LocalDateTime now = LocalDateTime.now();
		String date = now.format(DATE_FORMATTER);
		String time = now.format(TIME_FORMATTER);
		String uniqueNo = generateUniqueNo(now);

		return FinanceCommonHeader.builder()
			.apiName(apiName)
			.transmissionDate(date)
			.transmissionTime(time)
			.institutionCode(properties.institutionCode())
			.fintechAppNo(properties.fintechAppNo())
			.apiServiceCode(apiName)
			.institutionTransactionUniqueNo(uniqueNo)
			.apiKey(properties.apiKey())
			.userKey(userKey)
			.build();
	}

	private String generateUniqueNo(LocalDateTime now) {
		String timestamp = now.format(TIMESTAMP_FORMATTER);
		int currentSeq = sequence.getAndUpdate(val -> (val >= MAX_SEQUENCE) ? 1 : val + 1);
		return String.format("%s%06d", timestamp, currentSeq);
	}
}
