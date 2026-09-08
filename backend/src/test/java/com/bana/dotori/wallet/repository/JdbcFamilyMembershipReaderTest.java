package com.bana.dotori.wallet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * 통합 - family_members 를 직접 조회하는 어댑터가 실제 DB 에서 맞게 동작하는지 본다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JdbcFamilyMembershipReader 가족 조회")
class JdbcFamilyMembershipReaderTest {

	@Autowired
	private FamilyMembershipReader familyMembershipReader;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
	}

	@Test
	@DisplayName("가족의 부모 userId를 찾는다")
	void 부모를_찾는다() {
		Integer parentId = WalletTestFixture.insertParent(jdbcTemplate, "parent-find");
		Integer childId = WalletTestFixture.insertChild(jdbcTemplate, "child-find");
		Integer familyId = WalletTestFixture.insertFamilyWithChild(jdbcTemplate, parentId, childId);

		Integer found = familyMembershipReader.findParentUserId(familyId);

		assertThat(found).isEqualTo(parentId);
	}

	@Test
	@DisplayName("존재하지 않는 가족이면 null을 반환한다")
	void 존재하지_않는_가족이면_null() {
		Integer found = familyMembershipReader.findParentUserId(-1);

		assertThat(found).isNull();
	}
}
