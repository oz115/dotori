package com.bana.dotori.family.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.bana.dotori.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "family_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "family_member_id")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@CreationTimestamp
	@Column(name = "joined_at", nullable = false, updatable = false)
	private OffsetDateTime joinedAt;

	@Builder
	private FamilyMember(Family family, User user) {
		this.family = family;
		this.user = user;
	}
}
