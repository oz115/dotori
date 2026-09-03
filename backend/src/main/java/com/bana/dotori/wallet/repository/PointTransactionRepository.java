package com.bana.dotori.wallet.repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.bana.dotori.wallet.dto.request.TransactionCursor;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Integer>,
		JpaSpecificationExecutor<PointTransaction> {

	/**
	 * 이 지갑에서 같은 멱등키로 이미 처리된 거래를 찾는다.
	 *
	 * <p>지갑으로 범위를 좁혀 조회해야 한다. 멱등키만으로 찾으면 다른 사용자가 우연히 같은
	 * {@code Idempotency-Key} 를 보냈을 때 남의 거래가 걸려 나와, 이번 요청이 반영되지 않은 채
	 * 남의 거래 정보를 성공 응답으로 돌려주게 된다.
	 */
	Optional<PointTransaction> findByWalletIdAndIdempotencyKey(Integer walletId, String idempotencyKey);

	List<PointTransaction> findAllByWalletIdOrderByPointTransactionIdAsc(Integer walletId);

	/**
	 * WLT-06 거래내역 조회. 커서 지점 이후를 기간·사유로 걸러 {@code limit} 건까지 가져온다.
	 *
	 * <p>조건이 모두 선택이라 조건을 붙일지 말지를 코드로 정한다.
	 * JPQL 에 {@code (:startAt is null or ...)} 로 쓰면 PostgreSQL 이
	 * "could not determine data type of parameter" 로 거부한다.
	 * 비교 대상 없이 홀로 놓인 파라미터는 타입을 추론할 수 없기 때문이다.
	 * 조건 조합마다 메서드를 만들면 개수가 2의 제곱으로 늘어나므로 Criteria 로 조립한다.
	 *
	 * <p>기간은 {@code [startAt, endAt)} 반개구간이다. 종료일을 포함하려면
	 * 호출부가 다음 날 0시를 넘긴다. {@code created_at} 이 TIMESTAMPTZ 라
	 * "종료일 23:59:59" 로 자르면 그 뒤 밀리초에 생긴 거래가 빠진다.
	 *
	 * <p>{@code Page} 대신 {@code List} 를 돌려준다. {@code Page} 는 매 호출마다
	 * {@code COUNT(*)} 를 한 번 더 실행하는데, 무한 스크롤은 총 개수를 쓰지 않는다.
	 * "다음이 있는지"는 호출부가 {@code limit} 을 한 건 더 요청해 판단한다.
	 *
	 * <p>정렬은 항상 {@code (createdAt, pointTransactionId)} 이다. 커서 비교가
	 * 이 순서에 기대고 있어 호출부가 바꿀 수 없게 안에서 정한다.
	 */
	default List<PointTransaction> search(Integer walletId, OffsetDateTime startAt,
			OffsetDateTime endAt, PointTransactionReason reason,
			TransactionCursor cursor, Sort.Direction direction, int limit) {

		Sort sort = Sort.by(direction, "createdAt", "pointTransactionId");

		return findBy(
				(root, query, builder) -> {
					List<Predicate> conditions = new ArrayList<>();
					conditions.add(builder.equal(root.get("walletId"), walletId));
					if (startAt != null) {
						conditions.add(builder.greaterThanOrEqualTo(root.get("createdAt"), startAt));
					}
					if (endAt != null) {
						conditions.add(builder.lessThan(root.get("createdAt"), endAt));
					}
					if (reason != null) {
						conditions.add(builder.equal(root.get("reason"), reason));
					}
					if (cursor != null) {
						conditions.add(afterCursor(root, builder, cursor, direction));
					}
					return builder.and(conditions.toArray(new Predicate[0]));
				},
				query -> query.sortBy(sort).limit(limit).all());
	}

	/**
	 * 커서 지점 "이후"를 뜻하는 조건.
	 *
	 * <p>정렬이 {@code (createdAt, pointTransactionId)} 이므로 두 컬럼을 사전식으로 비교한다.
	 * 시각이 확실히 지난 행은 전부, 시각이 같은 행은 식별자로 가른다.
	 * 식별자 비교를 빼면 같은 시각 거래가 여럿일 때 그 묶음이 매 페이지에 다시 나오거나
	 * 통째로 건너뛰어진다.
	 *
	 * <p>PostgreSQL 의 행 값 비교({@code (a, b) < (?, ?)})가 더 짧지만
	 * Criteria API 에 대응하는 표현이 없어 OR 로 펼친다.
	 */
	private static Predicate afterCursor(Root<PointTransaction> root, CriteriaBuilder builder,
			TransactionCursor cursor, Sort.Direction direction) {

		Path<OffsetDateTime> createdAt = root.get("createdAt");
		Path<Integer> id = root.get("pointTransactionId");

		if (direction == Sort.Direction.ASC) {
			return builder.or(
					builder.greaterThan(createdAt, cursor.createdAt()),
					builder.and(
							builder.equal(createdAt, cursor.createdAt()),
							builder.greaterThan(id, cursor.id())));
		}
		return builder.or(
				builder.lessThan(createdAt, cursor.createdAt()),
				builder.and(
						builder.equal(createdAt, cursor.createdAt()),
						builder.lessThan(id, cursor.id())));
	}
}
