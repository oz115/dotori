package com.bana.dotori.wallet.dto.response;

import java.util.List;

/**
 * 무한 스크롤 한 묶음.
 *
 * <pre>
 * {
 *   "items": [ ... ],
 *   "nextCursor": "MjAyNi0wOS0wMlQxNTowMDowMSs..." ,
 *   "hasNext": true
 * }
 * </pre>
 *
 * <p>총 개수를 담지 않는다. 무한 스크롤은 "다음이 있는지"만 알면 되고,
 * 총 개수를 주려면 매 스크롤마다 {@code COUNT(*)} 를 한 번 더 실행해야 한다.
 * 화면에 총 건수를 보여줄 일이 생기면 그때 별도 엔드포인트로 빼는 편이 낫다.
 *
 * <p>{@code nextCursor} 는 다음 요청에 그대로 실어 보내면 되는 불투명한 문자열이다.
 * 더 가져올 것이 없으면 null 이다.
 *
 * <p>주식·예적금에도 같은 목록 화면이 생기면 이 레코드를 {@code global} 로 올려
 * 공통으로 쓸 예정이다. 지금은 사례가 하나뿐이라 무엇이 공통인지 판단할 근거가 없어
 * {@code wallet} 안에 둔다.
 */
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {

	public static <T> CursorPage<T> of(List<T> items, String nextCursor) {
		return new CursorPage<>(items, nextCursor, nextCursor != null);
	}

	public static <T> CursorPage<T> lastPage(List<T> items) {
		return new CursorPage<>(items, null, false);
	}

	public static <T> CursorPage<T> empty() {
		return new CursorPage<>(List.of(), null, false);
	}
}
