package com.bana.dotori.finance.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bana.dotori.finance.config.FinanceProperties;
import com.bana.dotori.finance.dto.transaction.InquireTransactionHistoryListRec;
import com.bana.dotori.finance.exception.FinanceApiException;
import com.bana.dotori.finance.util.FinanceHeaderGenerator;

class RestClientFinanceClientTest {

	private MockRestServiceServer mockServer;
	private RestClientFinanceClient client;

	@BeforeEach
	void setUp() {
		FinanceProperties properties = new FinanceProperties(
			"https://finopenapi.ssafy.io/ssafy/api/v1",
			"test-api-key",
			"00100",
			"001",
			"도토리"
		);
		FinanceHeaderGenerator headerGenerator = new FinanceHeaderGenerator(properties);

		RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl());
		mockServer = MockRestServiceServer.bindTo(builder).build();
		RestClient restClient = builder.build();

		client = new RestClientFinanceClient(restClient, properties, headerGenerator);
	}

	@Test
	@DisplayName("getUserKey_회원이_존재하면_해당_userKey를_반환한다")
	void getUserKey_회원조회_성공() {
		// given
		String userId = "dotori@example.com";
		String expectedUserKey = "633d65a0-67c4-48b2-9bbf-94b948d8e141";
		String searchResponseJson = String.format("""
			{
				"userId": "%s",
				"userName": "김도토",
				"institutionCode": "00100",
				"userKey": "%s"
			}
			""", userId, expectedUserKey);

		mockServer.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/member/search"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(searchResponseJson, MediaType.APPLICATION_JSON));

		// when
		String userKey = client.getUserKey(userId);

		// then
		assertThat(userKey).isEqualTo(expectedUserKey);
		mockServer.verify();
	}

	@Test
	@DisplayName("getUserKey_회원이_없으면_FinanceApiException이_발생한다")
	void getUserKey_회원없음_예외발생() {
		// given
		String userId = "unknown@example.com";

		mockServer.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/member/search"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withStatus(HttpStatus.NOT_FOUND));

		// when & then
		assertThatThrownBy(() -> client.getUserKey(userId))
			.isInstanceOf(FinanceApiException.class);

		mockServer.verify();
	}

	@Test
	@DisplayName("openAccountAuth_정상_응답_수신시_예외_없이_완료된다")
	void openAccountAuth_성공() {
		// given
		String userKey = "test-user-key";
		String accountNo = "0011214764051239";
		String responseJson = """
			{
				"Header": {
					"responseCode": "H0000",
					"responseMessage": "정상처리 되었습니다.",
					"apiName": "openAccountAuth"
				},
				"REC": {
					"transactionUniqueNo": "1234567890",
					"accountNo": "0011214764051239"
				}
			}
			""";

		mockServer.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/openAccountAuth"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

		// when & then
		client.openAccountAuth(userKey, accountNo, "도토리");
		mockServer.verify();
	}

	@Test
	@DisplayName("openAccountAuth_금융망_오류코드_반환시_FinanceApiException이_발생한다")
	void openAccountAuth_금융망오류_예외발생() {
		// given
		String userKey = "test-user-key";
		String accountNo = "0011214764051239";
		String responseJson = """
			{
				"Header": {
					"responseCode": "A1001",
					"responseMessage": "존재하지 않는 계좌번호입니다.",
					"apiName": "openAccountAuth"
				}
			}
			""";

		mockServer.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/openAccountAuth"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

		// when & then
		assertThatThrownBy(() -> client.openAccountAuth(userKey, accountNo, "도토리"))
			.isInstanceOf(FinanceApiException.class)
			.hasMessageContaining("A1001")
			.hasMessageContaining("존재하지 않는 계좌번호입니다.");

		mockServer.verify();
	}

	@Test
	@DisplayName("checkAuthCode_정상_인증시_예외_없이_완료된다")
	void checkAuthCode_성공() {
		// given
		String userKey = "test-user-key";
		String accountNo = "0011214764051239";
		String responseJson = """
			{
				"Header": {
					"responseCode": "H0000",
					"responseMessage": "정상처리 되었습니다."
				},
				"REC": {
					"status": "SUCCESS"
				}
			}
			""";

		mockServer.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/checkAuthCode"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

		// when & then
		client.checkAuthCode(userKey, accountNo, "도토리", "1234");
		mockServer.verify();
	}

	@Test
	@DisplayName("checkAuthCode_인증코드_불일치시_FinanceApiException이_발생한다")
	void checkAuthCode_불일치_예외발생() {
		// given
		String userKey = "test-user-key";
		String accountNo = "0011214764051239";
		String responseJson = """
			{
				"Header": {
					"responseCode": "A1002",
					"responseMessage": "인증코드가 일치하지 않습니다."
				}
			}
			""";

		mockServer.expect(requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/accountAuth/checkAuthCode"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

		// when & then
		assertThatThrownBy(() -> client.checkAuthCode(userKey, accountNo, "도토리", "9999"))
			.isInstanceOf(FinanceApiException.class)
			.hasMessageContaining("A1002")
			.hasMessageContaining("인증코드가 일치하지 않습니다.");

		mockServer.verify();
	}

	@Test
	@DisplayName("inquireTransactionHistoryList_정상_응답이면_REC를_반환한다")
	void inquireTransactionHistoryList_성공() {
		// given
		String userKey = "test-user-key";
		String accountNo = "0016174648358792";
		String responseJson = """
			{
				"Header": {
					"responseCode": "H0000",
					"responseMessage": "정상처리 되었습니다.",
					"apiName": "inquireTransactionHistoryList"
				},
				"REC": {
					"totalCount": "1",
					"list": [
						{
							"transactionUniqueNo": "59",
							"transactionDate": "20240401",
							"transactionTime": "102447",
							"transactionType": "1",
							"transactionTypeName": "입금",
							"transactionAccountNo": "",
							"transactionBalance": "100000000",
							"transactionAfterBalance": "100000000",
							"transactionSummary": "(수시입출금) : 입금",
							"transactionMemo": ""
						}
					]
				}
			}
			""";

		mockServer.expect(
				requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistoryList"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

		// when
		InquireTransactionHistoryListRec rec = client.inquireTransactionHistoryList(userKey, accountNo, "20240101",
			"20241231", "A", "DESC");

		// then
		assertThat(rec.totalCount()).isEqualTo("1");
		assertThat(rec.list()).hasSize(1);
		assertThat(rec.list().get(0).transactionUniqueNo()).isEqualTo("59");
		mockServer.verify();
	}

	@Test
	@DisplayName("inquireTransactionHistoryList_금융망_오류코드_반환시_FinanceApiException이_발생한다")
	void inquireTransactionHistoryList_금융망오류_예외발생() {
		// given
		String userKey = "test-user-key";
		String accountNo = "0016174648358792";
		String responseJson = """
			{
				"Header": {
					"responseCode": "A1003",
					"responseMessage": "계좌번호가 유효하지 않습니다.",
					"apiName": "inquireTransactionHistoryList"
				}
			}
			""";

		mockServer.expect(
				requestTo("https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/inquireTransactionHistoryList"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

		// when & then
		assertThatThrownBy(() -> client.inquireTransactionHistoryList(userKey, accountNo, "20240101", "20241231",
			"A", "DESC"))
			.isInstanceOf(FinanceApiException.class)
			.hasMessageContaining("A1003")
			.hasMessageContaining("계좌번호가 유효하지 않습니다.");

		mockServer.verify();
	}
}
