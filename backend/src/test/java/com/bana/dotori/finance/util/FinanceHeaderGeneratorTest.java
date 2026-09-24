package com.bana.dotori.finance.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bana.dotori.finance.config.FinanceProperties;
import com.bana.dotori.finance.dto.common.FinanceCommonHeader;

class FinanceHeaderGeneratorTest {

	private FinanceProperties properties;
	private FinanceHeaderGenerator generator;

	@BeforeEach
	void setUp() {
		properties = new FinanceProperties(
			"https://finopenapi.ssafy.io/ssafy",
			"test-api-key",
			"00100",
			"001",
			"도토리"
		);
		generator = new FinanceHeaderGenerator(properties);
	}

	@Test
	@DisplayName("createHeader_호출시_20자리_거래고유번호와_필수_헤더정보가_생성된다")
	void createHeader_정상생성() {
		// given
		String apiName = "openAccountAuth";
		String userKey = "633d65a0-67c4-48b2-9bbf-94b948d8e141";

		// when
		FinanceCommonHeader header = generator.createHeader(apiName, userKey);

		// then
		assertThat(header).isNotNull();
		assertThat(header.apiName()).isEqualTo("openAccountAuth");
		assertThat(header.apiServiceCode()).isEqualTo("openAccountAuth");
		assertThat(header.apiKey()).isEqualTo("test-api-key");
		assertThat(header.userKey()).isEqualTo(userKey);
		assertThat(header.institutionCode()).isEqualTo("00100");
		assertThat(header.fintechAppNo()).isEqualTo("001");
		assertThat(header.transmissionDate()).hasSize(8);
		assertThat(header.transmissionTime()).hasSize(6);
		assertThat(header.institutionTransactionUniqueNo()).hasSize(20);
	}

	@Test
	@DisplayName("createHeader_연속호출시_기관거래고유번호의_시퀀스가_증가한다")
	void createHeader_시퀀스증가() {
		// given
		String userKey = "test-user-key";

		// when
		FinanceCommonHeader header1 = generator.createHeader("testApi", userKey);
		FinanceCommonHeader header2 = generator.createHeader("testApi", userKey);

		// then
		assertThat(header1.institutionTransactionUniqueNo())
			.isNotEqualTo(header2.institutionTransactionUniqueNo());
	}
}
