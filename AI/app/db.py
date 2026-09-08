"""DB 연결.

경계 규칙 (중요)
---------------
이 DB 의 스키마 주인은 **백엔드(Spring)** 다. `ddl-auto: update` 로 Hibernate 가
엔티티에서 테이블을 만든다. 따라서 이 서버는 다음을 지킨다.

**이 서버는 테이블을 하나도 소유하지 않는다.** 팀원들이 이미 만들어 둔 테이블만 쓴다.

* 테이블을 새로 만들지 않는다. `CREATE TABLE` 을 실행하지 않는다.
* `Base.metadata.create_all()` 을 **절대 호출하지 않는다.** 남의 테이블까지 건드린다.
* Alembic 을 쓰지 않는다. `--autogenerate` 가 자기가 모르는 테이블에 대해
  `drop_table` 을 만들어 내므로 공유 DB 에서는 위험하다.
* 필요한 컬럼이 없으면 **직접 추가하지 않고 담당자와 협의한다.** 컬럼은 백엔드 엔티티에
  선언되어야 Hibernate 가 만든다.
* 읽기/쓰기 범위는 `docs/db-boundary.md` 를 따른다.
"""

from collections.abc import Iterator

from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from app.config import get_settings

_settings = get_settings()

# create_engine 은 이 시점에 접속하지 않는다. 그래서 DB(SSH 터널)가 없어도 서버는 뜬다.
engine = create_engine(
    _settings.database_url,
    # 로컬에서는 `ssh -N A505` 터널을 타므로 터널이 끊기면 풀에 죽은 커넥션이 남는다.
    # 쓰기 직전에 확인해서 걸러낸다.
    pool_pre_ping=True,
    # EC2 DB 를 팀 전체가 공유하므로 커넥션을 적게 잡는다.
    pool_size=5,
    max_overflow=2,
    pool_recycle=1800,
    connect_args={
        # 터널이 닫혀 있으면 접속 시도가 무한정 매달린다. 5초 만에 포기시킨다.
        # 이것이 없으면 DB 가 죽었을 때 요청이 끝나지 않아 서버가 통째로 멎는다.
        "connect_timeout": 5,
        # 백엔드가 TIMESTAMPTZ 를 KST 기준으로 읽고 쓴다(hibernate.jdbc.time_zone).
        # 세션 타임존을 맞춰 두지 않으면 같은 행의 시각이 서로 다르게 보인다.
        "options": "-c timezone=Asia/Seoul",
    },
    # 풀이 꽉 찼을 때 대기하는 시간. 기본 30초는 너무 길다.
    pool_timeout=10,
    future=True,
)

SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


def get_session() -> Iterator[Session]:
    """FastAPI 의존성. 요청 하나당 세션 하나를 쓰고 반드시 닫는다."""
    session = SessionLocal()
    try:
        yield session
    finally:
        session.close()
