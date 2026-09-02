package com.bana.dotori.wallet.repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;

import jakarta.persistence.criteria.Predicate;

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
	 * WLT-06 거래내역 조회. 기간과 사유로 걸러 한 페이지를 가져온다.
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
	 * <p>정렬은 {@link Pageable} 이 들고 온다. 반드시 유일한 컬럼을 마지막 정렬 키로
	 * 넣어야 한다. 같은 시각 거래가 여럿이면 페이지 경계에서 순서가 흔들려
	 * 어떤 거래는 두 번 나오고 어떤 거래는 빠진다.
	 */
	default Page<PointTransaction> search(Integer walletId, OffsetDateTime startAt,
			OffsetDateTime endAt, PointTransactionReason reason, Pageable pageable) {

		return findAll((root, query, builder) -> {
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
			return builder.and(conditions.toArray(new Predicate[0]));
		}, pageable);
	}
}
