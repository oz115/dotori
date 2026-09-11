"""퀴즈 도메인이 쓰는 테이블 매핑.

접근 범위는 `docs/db-boundary.md` 참고. 이 서버가 실제로 읽거나 쓰는 컬럼만 매핑한다.

`User`/`FamilyMember` 는 이 도메인이 처음 필요로 해서 여기 둔다 — `Family`(`app.core.models`)
처럼 다른 도메인도 쓰게 되면 그때 `core/models.py` 로 옮긴다 (같은 `__tablename__` 을
두 번 등록하면 SQLAlchemy 기동이 실패하므로).
"""

from datetime import date, datetime

from sqlalchemy import String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class User(Base):
    """읽기 전용. 자녀만 골라내는 `role` 필터링과 탈퇴 여부 확인에 쓴다."""

    __tablename__ = "users"

    user_id: Mapped[int] = mapped_column(primary_key=True)
    role: Mapped[str] = mapped_column(String)
    deleted_at: Mapped[datetime | None] = mapped_column()


class FamilyMember(Base):
    """읽기 전용. 가족과 자녀를 잇는 조인 테이블.

    `docs/db-boundary.md` 의 접근 범위 표에는 없던 테이블이라 이번에 읽기 전용으로 추가
    (문서도 같이 갱신함). 이 없이는 `users` 만으로 자녀가 어느 가족 소속인지 알 수 없다
    (`users` 에 `family_id` 컬럼이 없다).
    """

    __tablename__ = "family_members"

    family_member_id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column()
    user_id: Mapped[int] = mapped_column()
    deleted_at: Mapped[datetime | None] = mapped_column()


class Word(Base):
    """읽기 (초기 적재 시에만 INSERT). 퀴즈 생성의 재료가 되는 경제용어 사전."""

    __tablename__ = "words"

    word_id: Mapped[int] = mapped_column(primary_key=True)
    word: Mapped[str] = mapped_column(String(100))
    description: Mapped[str] = mapped_column(String(255))
    difficulty_level: Mapped[int] = mapped_column()
    category: Mapped[str | None] = mapped_column(String)


class Quiz(Base):
    """INSERT. 생성한 퀴즈를 아이·날짜별로 넣는다."""

    __tablename__ = "quizzes"

    quiz_id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column()
    child_id: Mapped[int] = mapped_column()
    word_id: Mapped[int] = mapped_column()
    question: Mapped[str] = mapped_column()
    correct_answer: Mapped[str] = mapped_column(String(255))
    explanation: Mapped[str | None] = mapped_column(String(255))
    reward_points: Mapped[int] = mapped_column()
    quiz_date: Mapped[date] = mapped_column()
    # DB 기본값(CURRENT_TIMESTAMP)이 있지만, SQLAlchemy 는 모르는 값이라 `server_default`
    # 없이 두면 매번 NULL 을 명시적으로 보내 NOT NULL 제약을 위반한다(직접 겪음).
    created_at: Mapped[datetime] = mapped_column(server_default=func.now())
