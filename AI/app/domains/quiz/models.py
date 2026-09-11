"""퀴즈 도메인이 쓰는 테이블 매핑.

접근 범위는 `docs/db-boundary.md` 참고. 이 서버가 실제로 읽거나 쓰는 컬럼만 매핑한다.
"""

from datetime import date, datetime

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


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
    created_at: Mapped[datetime] = mapped_column()
