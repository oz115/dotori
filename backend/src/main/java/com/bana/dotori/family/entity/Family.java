package com.bana.dotori.family.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "families")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Family {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "family_id")
	private Integer id;

	@Column(name = "name", length = 100, nullable = false)
	private String name;

	@Column(name = "created_by", nullable = false)
	private Integer createdBy;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Builder
	private Family(String name, Integer createdBy) {
		this.name = name;
		this.createdBy = createdBy;
	}
}
