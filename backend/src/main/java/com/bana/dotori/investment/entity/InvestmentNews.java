package com.bana.dotori.investment.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 가 생성하는 가상 회사 뉴스. 생성(NEWS-02)과 가격 반영(NEWS-03)은 내부 배치가 하고,
 * 이 API 범위(NEWS-01)에서는 조회만 한다.
 */
@Entity
@Table(name = "investment_news")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentNews {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "investment_news_id")
	private Integer id;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "content", nullable = false)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "impact", nullable = false)
	private InvestmentNewsImpact impact;

	@Column(name = "published_at", nullable = false)
	private OffsetDateTime publishedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Builder
	private InvestmentNews(String title, String content, InvestmentNewsImpact impact,
		OffsetDateTime publishedAt) {
		this.title = title;
		this.content = content;
		this.impact = impact;
		this.publishedAt = publishedAt;
	}
}
