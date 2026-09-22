package com.bana.dotori.global.util;

/**
 * 알림 문구에 남의 입력(상품명·미션 제목)을 끼워 넣을 때 쓰는 줄임 처리.
 *
 * <p>상품명과 미션 제목은 100자까지 허용된다. 그걸 그대로 본문에 넣으면 알림함의 두 줄
 * 안에서 이름만으로 자리가 다 차고, 정작 무슨 일이 일어났는지("1개가 구매됐어요")가
 * 화면 밖으로 밀려난다 (QA). 이름은 어느 것인지 알아볼 만큼만 남기고 자른다.
 *
 * <p>{@link #MAX_NAME} 은 알림함 한 줄에 들어가는 한글 글자 수에서 왔다 — 411dp 기기에서
 * 아이콘·여백을 빼면 한 줄이 열다섯 자 남짓이라, 이름이 첫 줄을 넘기지 않는 값이다.
 */
public final class NotificationText {

	/** 알림 본문에 넣는 이름의 최대 글자 수. */
	public static final int MAX_NAME = 15;

	private static final String ELLIPSIS = "...";

	private NotificationText() {
	}

	/**
	 * 알림 본문에 넣을 이름을 {@value #MAX_NAME} 자로 줄인다. 넘치면 뒤를 {@code ...} 로 바꾼다.
	 *
	 * <p>줄일 때 앞뒤 공백을 먼저 털어낸다 — 공백 때문에 한 글자가 밀려 잘리면
	 * "설거지 도와주" 처럼 마지막 글자만 애매하게 사라진다.
	 */
	public static String shortName(String name) {
		if (name == null) {
			return "";
		}
		String trimmed = name.trim();
		return trimmed.length() <= MAX_NAME ? trimmed : trimmed.substring(0, MAX_NAME) + ELLIPSIS;
	}
}
