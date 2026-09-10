package com.bana.dotori.chore.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 1계층 - 사진 키 형식만 검증한다 (CHORE-13).
 *
 * <p>스프링도 DB 도 S3 도 없다. 이 테스트가 실패하면 원인은 무조건 {@link ChorePhotoKey} 안에 있다.
 *
 * <p>형식이 코드 두 곳에 걸쳐 있다는 점이 중요하다 — 만드는 쪽(업로드)과 검사하는 쪽(완료 요청).
 * 한쪽만 고치면 업로드한 사진을 자기가 못 쓰게 되므로 왕복을 꼭 확인한다.
 */
@DisplayName("ChorePhotoKey 사진 키 형식")
class ChorePhotoKeyTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;

	@Nested
	@DisplayName("키를 만들 때")
	class Issue {

		@Test
		@DisplayName("가족·아이·확장자가 경로에 들어간다")
		void 형식대로_만든다() {
			String key = ChorePhotoKey.issue(FAMILY_ID, CHILD_ID, "image/jpeg");

			assertThat(key).startsWith("chores/10/2/").endsWith(".jpg");
		}

		@Test
		@DisplayName("png 는 png 확장자가 된다")
		void png_확장자() {
			assertThat(ChorePhotoKey.issue(FAMILY_ID, CHILD_ID, "image/png")).endsWith(".png");
		}

		@Test
		@DisplayName("같은 아이가 두 번 올려도 키가 겹치지 않는다")
		void 매번_다른_키() {
			String first = ChorePhotoKey.issue(FAMILY_ID, CHILD_ID, "image/jpeg");
			String second = ChorePhotoKey.issue(FAMILY_ID, CHILD_ID, "image/jpeg");

			// 겹치면 앞서 올린 사진이 덮어써진다.
			assertThat(first).isNotEqualTo(second);
		}

		@Test
		@DisplayName("chores/ 로 시작한다")
		void IAM_정책과_같은_접두사() {
			// IAM 정책이 arn:aws:s3:::{bucket}/chores/* 만 허용한다.
			// 접두사가 달라지면 업로드가 403 으로 막힌다.
			assertThat(ChorePhotoKey.issue(FAMILY_ID, CHILD_ID, "image/jpeg")).startsWith("chores/");
		}

		@Test
		@DisplayName("지원하지 않는 타입이면 터진다")
		void 지원하지_않는_타입() {
			assertThatThrownBy(() -> ChorePhotoKey.issue(FAMILY_ID, CHILD_ID, "image/gif"))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("만든 키는 자기 검사를 통과한다")
		void 왕복한다() {
			// 만드는 쪽과 검사하는 쪽이 어긋나면 아이가 방금 올린 사진을 자기 요청에 못 쓴다.
			String key = ChorePhotoKey.issue(FAMILY_ID, CHILD_ID, "image/jpeg");

			assertThat(ChorePhotoKey.belongsTo(key, FAMILY_ID, CHILD_ID)).isTrue();
		}
	}

	@Nested
	@DisplayName("키가 자기 것인지 볼 때")
	class BelongsTo {

		private static final String VALID = "chores/10/2/550e8400-e29b-41d4-a716-446655440000.jpg";

		@Test
		@DisplayName("남의 아이 키는 거부한다")
		void 형제_키는_거부() {
			// 이걸 통과시키면 형제가 올린 사진으로 자기 인증을 할 수 있다.
			assertThat(ChorePhotoKey.belongsTo(VALID, FAMILY_ID, 3)).isFalse();
		}

		@Test
		@DisplayName("남의 가족 키는 거부한다")
		void 다른_가족_키는_거부() {
			assertThat(ChorePhotoKey.belongsTo(VALID, 99, CHILD_ID)).isFalse();
		}

		@Test
		@DisplayName("형식이 아니면 거부한다")
		void 형식이_아니면_거부() {
			assertThat(ChorePhotoKey.belongsTo(null, FAMILY_ID, CHILD_ID)).isFalse();
			assertThat(ChorePhotoKey.belongsTo("", FAMILY_ID, CHILD_ID)).isFalse();
			assertThat(ChorePhotoKey.belongsTo("chores/10/2", FAMILY_ID, CHILD_ID)).isFalse();
			assertThat(ChorePhotoKey.belongsTo("chores/10/2/a/b.jpg", FAMILY_ID, CHILD_ID)).isFalse();
			assertThat(ChorePhotoKey.belongsTo("other/10/2/a.jpg", FAMILY_ID, CHILD_ID)).isFalse();
		}

		@Test
		@DisplayName("우리 버킷 밖을 가리키는 값은 거부한다")
		void 외부_URL_은_거부() {
			// 아이가 아무 문자열이나 보낼 수 있다. URL 을 넣어도 통과하면 안 된다.
			assertThat(ChorePhotoKey.belongsTo("https://evil.example.com/a.jpg", FAMILY_ID, CHILD_ID)).isFalse();
		}

		@Test
		@DisplayName("경로 조작이 섞인 값은 거부한다")
		void 경로_조작은_거부() {
			assertThat(ChorePhotoKey.belongsTo("chores/10/2/../../secret.jpg", FAMILY_ID, CHILD_ID)).isFalse();
		}

		@Test
		@DisplayName("허용하지 않는 확장자는 거부한다")
		void 확장자를_본다() {
			assertThat(ChorePhotoKey.belongsTo("chores/10/2/a.exe", FAMILY_ID, CHILD_ID)).isFalse();
		}
	}

	@Nested
	@DisplayName("타입을 볼 때")
	class Supported {

		@Test
		@DisplayName("jpg 와 png 만 허용한다")
		void 두_가지만_허용() {
			assertThat(ChorePhotoKey.isSupported("image/jpeg")).isTrue();
			assertThat(ChorePhotoKey.isSupported("image/png")).isTrue();
			assertThat(ChorePhotoKey.isSupported("image/gif")).isFalse();
			assertThat(ChorePhotoKey.isSupported("application/pdf")).isFalse();
			assertThat(ChorePhotoKey.isSupported(null)).isFalse();
		}
	}
}
