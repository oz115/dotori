package com.bana.dotori.wallet.dto.request;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;

/**
 * 거래내역 무한 스크롤에서 "여기까지 봤다"를 나타내는 지점.
 *
 * <p>정렬 키와 같은 값을 담는다. 거래내역은 {@code (created_at, point_transaction_id)} 로
 * 정렬하므로 커서도 그 둘이다. 식별자가 함께 있어야 같은 시각 거래가 여럿일 때도
 * "이 지점 이후"가 하나로 정해진다.
 *
 * <p><b>왜 offset 대신 커서인가.</b> 최신순 목록에서 {@code OFFSET} 으로 다음 장을 넘기면,
 * 그 사이 새 거래가 앞에 끼어들었을 때 전체가 한 칸씩 밀려 이미 본 항목이 다시 나온다.
 * 아이가 자기 내역을 보는 중에 부모가 용돈을 주면 바로 재현된다.
 * 커서는 절대 위치가 아니라 "이 값 다음"을 가리키므로 앞쪽이 늘어나도 흔들리지 않는다.
 *
 * <p><b>왜 인코딩하는가.</b> 클라이언트에게는 불투명한 문자열로 보여야 한다.
 * {@code createdAt} 과 {@code id} 를 그대로 노출하면 클라이언트가 커서를 직접 조립하기
 * 시작하고, 나중에 정렬 키를 바꿀 때 서버와 클라이언트를 같이 고쳐야 한다.
 *
 * @param createdAt 마지막으로 본 거래의 생성 시각
 * @param id        마지막으로 본 거래의 식별자
 */
public record TransactionCursor(OffsetDateTime createdAt, Integer id) {

	private static final String DELIMITER = "|";

	/**
	 * 커서를 URL 에 실을 수 있는 문자열로 만든다.
	 *
	 * <p>시각은 {@code toString()} 으로 직렬화한다. PostgreSQL timestamptz 는 마이크로초까지
	 * 저장하는데 epoch milli 로 줄이면 같은 밀리초 안의 거래를 건너뛰거나 중복시킬 수 있다.
	 */
	public String encode() {
		String raw = createdAt + DELIMITER + id;
		return Base64.getUrlEncoder().withoutPadding()
				.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 클라이언트가 돌려준 커서를 해석한다.
	 *
	 * @return 커서가 비어 있으면 null (첫 페이지 요청)
	 * @throws BusinessException INVALID_CURSOR 형식이 깨진 경우
	 */
	public static TransactionCursor decode(String encoded) {
		if (encoded == null || encoded.isBlank()) {
			return null;
		}
		try {
			String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
			int separator = raw.lastIndexOf(DELIMITER);
			if (separator < 0) {
				throw new IllegalArgumentException("구분자가 없습니다");
			}
			return new TransactionCursor(
					OffsetDateTime.parse(raw.substring(0, separator)),
					Integer.valueOf(raw.substring(separator + DELIMITER.length())));
		} catch (RuntimeException e) {
			// 커서는 서버가 만들어 준 값이므로, 깨졌다면 조작되거나 잘린 것이다.
			// 조용히 첫 페이지로 되돌리면 클라이언트가 같은 목록을 무한히 다시 받는다.
			throw new BusinessException(ErrorCode.INVALID_CURSOR,
					"조회 커서를 해석할 수 없습니다. cursor=" + encoded, e);
		}
	}
}
