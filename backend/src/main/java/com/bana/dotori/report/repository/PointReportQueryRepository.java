package com.bana.dotori.report.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.report.dto.ReportTypeAmountRow;
import com.bana.dotori.report.dto.response.ReportDailyAmount;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;

/**
 * 원장을 집계해서 읽는 리포트 전용 통로 (RPT-02·03).
 *
 * <p>{@code PointTransactionRepository} 에 메서드를 더하지 않고 따로 뒀다. 그쪽은 원장에
 * 쓰고 커서로 한 건씩 훑는 책임이고, 여기는 통계를 뽑는 읽기 전용이다. 한 엔티티에
 * 리포지토리를 여럿 두는 것은 Spring Data 가 허용한다.
 *
 * <p>기간은 두 메서드 모두 {@code [startAt, endAt)} 반개구간이다. 종료일을 포함하려면
 * 호출부가 다음 날 0 시를 넘긴다. {@code created_at} 이 TIMESTAMPTZ 라
 * "종료일 23:59:59" 로 자르면 그 뒤 밀리초에 생긴 거래가 빠진다.
 */
public interface PointReportQueryRepository extends JpaRepository<PointTransaction, Integer> {

	/**
	 * 사유별 합계와 건수.
	 *
	 * <p>{@code reasons} 로 수입/소비 방향을 가른다. 방향은 컬럼에 없고
	 * {@link PointTransactionReason} 의 플래그에만 있으므로, 호출부가 해당 방향의 사유
	 * 목록을 넘긴다.
	 *
	 * <p>정렬을 금액 내림차순으로 고정한다. 화면이 "많이 번 순"으로 그리는 것이 기본이고,
	 * 금액이 같을 때는 사유 이름으로 갈라 순서가 호출마다 흔들리지 않게 한다.
	 */
	@Query("select new com.bana.dotori.report.dto.ReportTypeAmountRow("
			+ "t.reason, sum(t.amount), count(t)) "
			+ "from PointTransaction t "
			+ "where t.childId = :childId "
			+ "and t.createdAt >= :startAt and t.createdAt < :endAt "
			+ "and t.reason in :reasons "
			+ "group by t.reason "
			+ "order by sum(t.amount) desc, t.reason asc")
	List<ReportTypeAmountRow> aggregateByReason(@Param("childId") Integer childId,
			@Param("startAt") OffsetDateTime startAt, @Param("endAt") OffsetDateTime endAt,
			@Param("reasons") Collection<PointTransactionReason> reasons);

	/**
	 * 일자별 합계. <b>KST 기준으로 날짜를 자른다.</b>
	 *
	 * <p>네이티브 쿼리인 이유가 이 환산이다. {@code created_at} 은 TIMESTAMPTZ 이고
	 * 드라이버는 UTC 로 돌려주므로, JPQL 로 날짜만 떼면 한국 시간 자정부터 오전 9 시
	 * 사이의 거래가 <b>전날로 집계된다.</b> 저녁에 받은 용돈이 전날 그래프에 찍히는 셈이다.
	 * {@code AT TIME ZONE} 은 JPQL 에 대응 표현이 없어 SQL 로 내려간다.
	 *
	 * <p>사유는 {@code ::text} 로 캐스팅해서 비교한다. {@code transaction_type} 은 네이티브
	 * ENUM 이라 JDBC 가 varchar 로 바인딩한 목록과 직접 비교할 수 없다
	 * ({@code operator does not exist: point_transactions_transaction_type = character varying}).
	 * 이 컬럼에는 인덱스가 없어 캐스팅으로 잃는 것도 없다 — 조회는
	 * {@code (child_id, created_at)} 인덱스로 이미 좁혀진다.
	 *
	 * <p>{@code Object[]} 로 받는다. 네이티브 쿼리는 생성자 표현식을 쓸 수 없고, 인터페이스
	 * 프로젝션은 별칭과 프로퍼티 이름을 맞춰야 해서 조용히 어긋나기 쉽다. 변환은
	 * {@link #aggregateByDate} 가 한 곳에서 한다.
	 */
	@Query(value = "select (t.created_at at time zone 'Asia/Seoul')::date as stat_date, "
			+ "sum(t.amount) as total_amount "
			+ "from point_transactions t "
			+ "where t.child_id = :childId "
			+ "and t.created_at >= :startAt and t.created_at < :endAt "
			+ "and t.transaction_type::text in (:reasonNames) "
			+ "group by (t.created_at at time zone 'Asia/Seoul')::date "
			+ "order by 1", nativeQuery = true)
	List<Object[]> aggregateByDateRaw(@Param("childId") Integer childId,
			@Param("startAt") OffsetDateTime startAt, @Param("endAt") OffsetDateTime endAt,
			@Param("reasonNames") Collection<String> reasonNames);

	/**
	 * {@link #aggregateByDateRaw} 의 결과를 응답 타입으로 바꾼다.
	 *
	 * <p>{@code sum} 은 {@code Number} 로 받는다. 드라이버가 {@code Long} 대신
	 * {@code BigInteger} 를 주더라도 깨지지 않는다.
	 */
	default List<ReportDailyAmount> aggregateByDate(Integer childId, OffsetDateTime startAt,
			OffsetDateTime endAt, Collection<PointTransactionReason> reasons) {

		List<String> reasonNames = reasons.stream().map(Enum::name).toList();

		return aggregateByDateRaw(childId, startAt, endAt, reasonNames).stream()
				.map(row -> new ReportDailyAmount(
						toLocalDate(row[0]),
						((Number)row[1]).longValue()))
				.toList();
	}

	/**
	 * 네이티브 쿼리의 {@code ::date} 칸을 {@link LocalDate} 로 맞춘다.
	 *
	 * <p>Hibernate 6 은 이 칸을 {@link LocalDate} 로 돌려주지만, 예전 드라이버 조합은
	 * {@link Date} 를 준다. 어느 쪽이 오는지는 컴파일 시점에 알 수 없고 틀리면
	 * {@code ClassCastException} 으로 런타임에 터지므로 둘 다 받는다.
	 */
	private static LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate date) {
			return date;
		}
		return ((Date)value).toLocalDate();
	}
}
