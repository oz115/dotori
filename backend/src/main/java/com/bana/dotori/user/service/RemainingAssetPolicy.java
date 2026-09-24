package com.bana.dotori.user.service;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 자녀에게 "정리되지 않은 자산"이 남아 있는지 도메인 전체를 모아 묻는다.
 *
 * <p>탈퇴, 가족 해체, 자녀 제외가 모두 같은 질문을 한다 — 자산이 남은 채로 가족·계정에서 빠지면
 * 그 자산에 접근할 길(가족 소속·부모 승인)이 끊기기 때문이다. 질문을 한 곳에 모아 두면 새 자산
 * 도메인이 생겨도 {@link RemainingAssetReader} 구현체만 추가하면 세 경로에 모두 반영된다.
 */
@Component
@RequiredArgsConstructor
public class RemainingAssetPolicy {

	private final List<RemainingAssetReader> readers;

	public boolean hasRemainingAsset(Integer userId) {
		return readers.stream().anyMatch(reader -> reader.hasRemainingAsset(userId));
	}
}
