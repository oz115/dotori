package com.bana.dotori.auth.dto;

/**
 * 이메일 인증 코드 발송의 용도 (AUTH-07).
 *
 * <p>발송 API 는 회원가입·아이디 찾기·비밀번호 찾기 화면이 공유하는데, 용도에 따라
 * "가입된 이메일" 처리가 정반대다 — 회원가입은 막아야 하고(중복 가입), 찾기·재설정은
 * 가입된 이메일이어야만 의미가 있다.
 *
 * <p>{@link #SIGNUP} 만 동작을 바꾼다: 이미 가입된 이메일이면 코드를 보내지 않고
 * DUPLICATE_EMAIL(409)로 즉시 알려준다. 나머지 용도(및 미지정)는 기존대로
 * 가입 여부와 무관하게 동일하게 동작한다 — 계정 존재 여부를 노출하지 않기 위해서다.
 */
public enum EmailVerificationPurpose {
	SIGNUP,
	FIND_ID,
	PASSWORD_RESET
}
