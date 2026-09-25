# DB 접속 정보 · 스키마 · 덤프

[← 포팅 매뉴얼로 돌아가기](포팅메뉴얼.md)

## 1. 접속 정보


| 항목 | 값 |
| --- | --- |
| DBMS | PostgreSQL 16 |
| 호스트 | 컨테이너 내부 `postgres` / 서버 `127.0.0.1` |
| 포트 | 5432 (보안그룹 미개방, SSH 터널로 접속) |
| DB 이름 | `dotori_town` |
| 계정 | `dotori` |
| 비밀번호 | `<vault_postgres_password>` |
| JDBC URL | `jdbc:postgresql://postgres:5432/dotori_town?stringtype=unspecified` |

로컬 PC에서 운영 DB에 접속할 때는 SSH 터널을 엽니다.

```bash
ssh -i <key.pem> -N -L 5432:localhost:5432 ubuntu@<EC2_IP>
psql -h localhost -p 5432 -U dotori -d dotori_town
```

Redis·RabbitMQ 접속 정보:

| 서비스 | 호스트:포트 | 계정 | 비밀번호 |
| --- | --- | --- | --- |
| Redis | `redis:6379` | — | `<vault_redis_password>` |
| RabbitMQ | `rabbitmq:5672` (관리 UI 15672) | `dotori` | `<vault_rabbitmq_password>` |

<br>

## 2. 스키마 및 덤프

| 파일 | 설명 |
| --- | --- |
| [`backend/docker/initdb/01-schema.sql`](../backend/docker/initdb/01-schema.sql) | 전체 스키마 (테이블, ENUM 타입, 인덱스) — 원본 |
| [`backend/docker/initdb/02-seed-investment-products.sql`](../backend/docker/initdb/02-seed-investment-products.sql) | 투자 상품 초기 데이터 |
| [`backend/docker/migration/*.sql`](../backend/docker/migration/) | 기존 DB에 적용할 날짜별 변경분 |
| `exec/db/` | 운영 DB 덤프 (추후 첨부) |

<!-- TODO: 운영 DB 덤프 첨부 후 복원 방법 기재 -->

덤프 생성·복원 방법:

```bash
# 생성 (서버)
docker exec dotori-postgres pg_dump -U dotori -d dotori_town --no-owner > dotori_town_$(date +%Y%m%d).sql
# 복원 (빈 DB)
docker exec -i dotori-postgres psql -U dotori -d dotori_town < dotori_town_YYYYMMDD.sql
```
