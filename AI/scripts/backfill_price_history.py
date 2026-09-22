"""과거 며칠치 가격 이력을 지금 규칙(현재가 퍼센트 변동)으로 다시 만드는 일회용 스크립트.

QA DB 에 옛 규칙(시드 10000 기준 절대 변동폭)으로 쌓인 이력을 지운 뒤, **이미 저장돼
있는 지난 뉴스**로 그 날짜들의 가격을 하루씩 다시 굴린다. 뉴스는 다시 만들지 않는다 —
`investment_price_history` 만 쓴다. 차트에 며칠치 흐름이 바로 보이게 하는 용도다.

멱등: `update_prices_for_todays_news` 가 날짜별로 이미 이력이 있으면 건너뛰므로
같은 구간을 두 번 돌려도 가격이 두 번 움직이지 않는다. 단 **가장 오래된 날부터**
순서대로 돌린다 (미래 날짜 이력이 있으면 그 이전 날짜가 건너뛰어진다 — --wipe 로
비우고 시작하면 신경 쓸 일 없다).

실행 (AI 폴더, venv 활성화 상태, DB 는 `ssh -N A505` 터널 필요):

    python -m scripts.backfill_price_history --days 7 --wipe

--wipe 는 시작 전에 investment_price_history 를 통째로 비운다. 보유·거래
(investment_holdings/transactions)는 건드리지 않는다 — 옛 스케일 매수가가 남아 있으면
따로 정리해야 한다.
"""

import argparse
import logging
from datetime import datetime, time, timedelta
from zoneinfo import ZoneInfo

from sqlalchemy import delete

from app.core.database import SessionLocal
from app.domains.investment.models import InvestmentPriceHistory
from app.domains.investment.services import update_prices_for_todays_news, yesterdays_news

KST = ZoneInfo("Asia/Seoul")
# 실제 크론(00:05 KST)과 같은 시각으로 기록해 하루 경계 계산이 운영과 똑같이 굴러가게 한다
CRON_TIME = time(0, 5)

logger = logging.getLogger(__name__)


def backfill(days: int, wipe: bool) -> None:
    session = SessionLocal()
    try:
        if wipe:
            deleted = session.execute(delete(InvestmentPriceHistory)).rowcount
            session.commit()
            logger.info("기존 가격 이력 %d건 삭제", deleted)

        today = datetime.now(KST).date()
        for offset in range(days - 1, -1, -1):
            day = today - timedelta(days=offset)
            now = datetime.combine(day, CRON_TIME, tzinfo=KST)
            news = yesterdays_news(session, now)
            if not news:
                logger.info("%s: 전날 뉴스가 없어 건너뜀", day)
                continue
            written = update_prices_for_todays_news(session, news, now)
            logger.info("%s: 가격 이력 %d건 기록", day, written)
    finally:
        session.close()


def main() -> None:
    logging.basicConfig(level=logging.INFO)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--days", type=int, default=7, help="오늘 포함 며칠치를 굴릴지 (기본 7)")
    parser.add_argument("--wipe", action="store_true", help="시작 전에 가격 이력을 전부 지운다")
    args = parser.parse_args()
    backfill(args.days, args.wipe)


if __name__ == "__main__":
    main()
