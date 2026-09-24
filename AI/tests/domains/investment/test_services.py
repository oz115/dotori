"""investment/services.py (service) 검증.

LLM 은 `unittest.mock.patch`로 대체하고, DB는 conftest 의 in-memory SQLite를 쓴다.
실제 GMS·Postgres 는 여기서 안 건드린다.
"""

import json
from collections import Counter
from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from unittest.mock import patch

import pytest

from app.core.models import Family
from app.domains.investment import services
from app.domains.investment.models import InvestmentNews, InvestmentNewsProduct, InvestmentPriceHistory


def _fake_client():
    """고정된 JSON 뉴스를 돌려주는 목 LLM 클라이언트."""

    def create(**kwargs):
        payload = {"title": "원자재 가격 변화", "content": "시장에 들어오는 원자재의 양이 달라졌어요."}
        msg = SimpleNamespace(content=json.dumps(payload, ensure_ascii=False))
        return SimpleNamespace(choices=[SimpleNamespace(message=msg)])

    return SimpleNamespace(chat=SimpleNamespace(completions=SimpleNamespace(create=create)))


class TestSelectTodaysCompanies:
    def test_picks_three_distinct_companies_two_up_one_down(self, db_session, ten_companies, now_kst_afternoon):
        up, down = services.select_todays_companies(db_session, now_kst_afternoon)

        todays = up + [down]
        assert len(up) == 2
        assert len({p.investment_product_id for p in todays}) == 3

    def test_raises_when_fewer_than_three_active_companies(self, db_session, ten_companies, now_kst_afternoon):
        for product in ten_companies[2:]:
            product.is_active = False
        db_session.commit()

        with pytest.raises(ValueError):
            services.select_todays_companies(db_session, now_kst_afternoon)

    def test_rotation_is_fair_over_a_week(self, db_session, ten_companies):
        """월요일부터 7일을 돌리면 회사 10개가 2~3회씩 고르게 뽑혀야 한다 (21슬롯/10개)."""
        appearances = Counter()
        monday_afternoon = datetime(2026, 9, 7, 5, 0, tzinfo=timezone.utc)  # 월요일 KST 14시

        for day in range(7):
            now = monday_afternoon + timedelta(days=day)
            up, down = services.select_todays_companies(db_session, now)
            todays = up + [down]
            for product in todays:
                appearances[product.investment_product_id] += 1
            # 다음 날 로테이션 카운트에 반영되려면 실제로 뉴스를 기록해야 한다
            for product, impact in [(p, "POSITIVE") for p in up] + [(down, "NEGATIVE")]:
                news = InvestmentNews(title="t", content="c", impact=impact, published_at=now)
                db_session.add(news)
                db_session.flush()
                db_session.add(
                    InvestmentNewsProduct(
                        investment_news_id=news.investment_news_id,
                        investment_product_id=product.investment_product_id,
                    )
                )
            db_session.commit()

        counts = sorted(appearances.values())
        assert len(counts) == 10, "10개 회사 전부 한 번 이상 뽑혀야 한다"
        assert min(counts) >= 2
        assert max(counts) <= 3


class TestGenerateTodaysNews:
    @pytest.mark.parametrize(
        ("impact", "expected_condition", "excluded_condition"),
        [
            ("POSITIVE", "원자재 가격 하락", "원자재 가격 상승"),
            ("NEGATIVE", "원자재 가격 상승", "원자재 가격 하락"),
        ],
    )
    def test_llm_prompt_omits_company_identity(
        self, ten_companies, impact, expected_condition, excluded_condition
    ):
        product = ten_companies[0]
        product.name = "고소한빵"
        product.description = "밀가루로 빵을 만들어 판매해요."
        captured: dict = {}

        def create(**kwargs):
            captured.update(kwargs)
            payload = {"title": "원자재 가격 변화", "content": "원자재 가격이 달라졌어요."}
            msg = SimpleNamespace(content=json.dumps(payload, ensure_ascii=False))
            return SimpleNamespace(choices=[SimpleNamespace(message=msg)])

        fake_client = SimpleNamespace(chat=SimpleNamespace(completions=SimpleNamespace(create=create)))
        with patch.object(services, "get_client", return_value=fake_client):
            services._generate_news_text(product, impact)

        user_prompt = captured["messages"][1]["content"]
        assert product.name not in user_prompt
        assert product.description not in user_prompt
        assert "회사:" not in user_prompt
        assert "하는 일:" not in user_prompt
        assert "대표 경제 요인: 원자재 가격" in user_prompt
        assert f"선택된 사건 조건: {expected_condition}" in user_prompt
        assert excluded_condition not in user_prompt
        assert "사용 가능한 뉴스 소재: 생산량, 수입량, 재고량" in user_prompt
        assert "출력 금지어: 빵, 제빵, 밀가루" in user_prompt
        response_format = captured["response_format"]
        assert response_format["type"] == "json_schema"
        assert response_format["json_schema"]["strict"] is True
        assert response_format["json_schema"]["schema"]["required"] == ["title", "content"]
        assert response_format["json_schema"]["schema"]["additionalProperties"] is False

    def test_llm_prompt_supports_legacy_news_context(self):
        legacy_context = (
            "대표 경제 요인: 밀 가격\n"
            "상승 예시: 밀 가격이 내려 빵을 만드는 비용이 줄었어요.\n"
            "하락 예시: 밀 가격이 올라 빵을 만드는 비용이 늘었어요."
        )

        user_prompt = services._build_news_user_prompt(legacy_context, "POSITIVE")

        assert "대표 경제 요인: 밀 가격" in user_prompt
        assert "선택된 사건 조건: 밀 가격이 내려 빵을 만드는 비용이 줄었어요." in user_prompt
        assert "밀 가격이 올라" not in user_prompt

    def test_creates_three_news_with_two_up_one_down(self, db_session, ten_companies, now_kst_afternoon):
        with patch.object(services, "get_client", return_value=_fake_client()):
            created = services.generate_todays_news(db_session, now_kst_afternoon)

        assert sorted(n.impact for n in created) == ["NEGATIVE", "POSITIVE", "POSITIVE"]
        assert db_session.query(InvestmentNewsProduct).count() == 3

    def test_second_call_same_day_returns_existing_news_without_duplicating(
        self, db_session, ten_companies, now_kst_afternoon
    ):
        with patch.object(services, "get_client", return_value=_fake_client()):
            first = services.generate_todays_news(db_session, now_kst_afternoon)
            second = services.generate_todays_news(db_session, now_kst_afternoon)

        assert [n.investment_news_id for n in first] == [n.investment_news_id for n in second]
        assert db_session.query(InvestmentNews).count() == 3

    def test_next_day_generates_a_new_batch(self, db_session, ten_companies, now_kst_afternoon):
        tomorrow = now_kst_afternoon + timedelta(days=1)
        with patch.object(services, "get_client", return_value=_fake_client()):
            today_news = services.generate_todays_news(db_session, now_kst_afternoon)
            tomorrow_news = services.generate_todays_news(db_session, tomorrow)

        assert {n.investment_news_id for n in today_news}.isdisjoint(
            {n.investment_news_id for n in tomorrow_news}
        )
        assert db_session.query(InvestmentNews).count() == 6


class TestPriceMath:
    def test_percent_move_uses_ratio_of_current_price(self):
        with patch.object(services.random, "uniform", side_effect=lambda a, b: a):
            assert services._percent_move(700, services.NEWS_MOVE_MIN, services.NEWS_MOVE_MAX) == 70
            assert services._percent_move(700, services.JITTER_MOVE_MIN, services.JITTER_MOVE_MAX) == 7

    def test_percent_move_moves_at_least_one_point(self):
        """반올림하면 0이 되는 작은 가격도 최소 1은 움직인다 — 변동 없음을 없애는 게 목적이다."""
        with patch.object(services.random, "uniform", side_effect=lambda a, b: a):
            assert services._percent_move(1, services.JITTER_MOVE_MIN, services.JITTER_MOVE_MAX) == 1

    @pytest.mark.parametrize(("start_price", "expected"), [(1, 1), (9999, 1000), (10000, 1000)])
    def test_price_floor(self, start_price, expected):
        assert services._price_floor(start_price) == expected

    @pytest.mark.parametrize(
        ("weekly_allowance", "expected"),
        [(1, 1), (1000, 100), (7000, 700), (10005, 1001)],
    )
    def test_start_price(self, weekly_allowance, expected):
        assert services._start_price(weekly_allowance) == expected


class TestUpdatePricesForTodaysNews:
    def _seed_one_up_one_down(self, db_session, up_product, down_product, now):
        up_news = InvestmentNews(investment_news_id=1, title="up", content="up", impact="POSITIVE", published_at=now)
        down_news = InvestmentNews(
            investment_news_id=2, title="down", content="down", impact="NEGATIVE", published_at=now
        )
        db_session.add_all([up_news, down_news])
        db_session.add_all(
            [
                InvestmentNewsProduct(investment_news_id=1, investment_product_id=up_product.investment_product_id),
                InvestmentNewsProduct(investment_news_id=2, investment_product_id=down_product.investment_product_id),
            ]
        )
        db_session.commit()
        return up_news, down_news

    def test_moves_all_products_and_skips_families_before_onboarding(
        self, db_session, ten_companies, now_kst_afternoon
    ):
        up_product, down_product = ten_companies[0], ten_companies[1]
        up_news, down_news = self._seed_one_up_one_down(db_session, up_product, down_product, now_kst_afternoon)
        db_session.add_all(
            [
                Family(family_id=1, weekly_allowance=7000, weekly_reward_limit=3500, financial_knowledge_level="BEGINNER"),
                Family(family_id=2, weekly_allowance=0, weekly_reward_limit=0, financial_knowledge_level="BEGINNER"),
            ]
        )
        db_session.commit()

        # 비율은 구간 하한으로, 잔물결 방향은 상승으로 고정해 결정적으로 만든다
        with (
            patch.object(services.random, "uniform", side_effect=lambda a, b: a),
            patch.object(services.random, "random", return_value=0.0),
        ):
            written = services.update_prices_for_todays_news(
                db_session, [up_news, down_news], now_kst_afternoon
            )

        # 뉴스에 걸린 2개만이 아니라 활성 회사 10개가 전부 움직인다
        assert written == 10
        rows = db_session.query(InvestmentPriceHistory).all()
        price_by_product = {r.investment_product_id: r.price for r in rows if r.family_id == 1}
        # 이력이 없으면 가족 시작가 = 주간 용돈(7000) ÷ 종목 수(10) = 700 에서 출발한다.
        # 뉴스 종목은 현재가의 10%(하한 고정), 나머지는 1% 잔물결이다.
        assert price_by_product[up_product.investment_product_id] == 770
        assert price_by_product[down_product.investment_product_id] == 630
        jitter_prices = [
            price
            for product_id, price in price_by_product.items()
            if product_id
            not in {up_product.investment_product_id, down_product.investment_product_id}
        ]
        assert jitter_prices == [707] * 8
        assert all(r.family_id != 2 for r in rows), "온보딩 전(A=0) 가족은 건너뛰어야 한다"

    def test_backfills_past_week_for_families_without_history(
        self, db_session, ten_companies, now_kst_afternoon
    ):
        """막 온보딩한 가족은 지난 날짜 이력을 만들어 줘서, 어제 가입해도 차트에 흐름이 보인다."""
        product0, product1 = ten_companies[0], ten_companies[1]
        # 그제 뉴스(product0 상승) — 어제 가격을 움직였어야 할 뉴스라 백필 재료가 된다
        old_news = InvestmentNews(
            investment_news_id=1, title="o", content="o", impact="POSITIVE",
            published_at=now_kst_afternoon - timedelta(days=2),
        )
        # 어제 뉴스(product1 하락) — 오늘 가격을 움직인다
        yesterday_news = InvestmentNews(
            investment_news_id=2, title="y", content="y", impact="NEGATIVE",
            published_at=now_kst_afternoon - timedelta(days=1),
        )
        db_session.add_all([old_news, yesterday_news])
        db_session.add_all(
            [
                InvestmentNewsProduct(investment_news_id=1, investment_product_id=product0.investment_product_id),
                InvestmentNewsProduct(investment_news_id=2, investment_product_id=product1.investment_product_id),
            ]
        )
        db_session.add(
            Family(family_id=1, weekly_allowance=7000, weekly_reward_limit=3500, financial_knowledge_level="BEGINNER")
        )
        db_session.commit()

        with (
            patch.object(services.random, "uniform", side_effect=lambda a, b: a),
            patch.object(services.random, "random", return_value=0.0),
        ):
            written = services.update_prices_for_todays_news(db_session, [yesterday_news], now_kst_afternoon)

        # 어제(그제 뉴스가 움직인 날) 10건 + 오늘 10건. 더 과거는 뉴스가 없어 만들지 않는다.
        assert written == 20
        rows = db_session.query(InvestmentPriceHistory).order_by(InvestmentPriceHistory.recorded_at).all()
        assert len({r.recorded_at for r in rows}) == 2

        def series(product_id):
            return [r.price for r in rows if r.investment_product_id == product_id]

        # 시작가 700. 어제: product0 뉴스 상승 10% → 770, 나머지 잔물결 +1% → 707.
        # 오늘: product0 잔물결 770+8 → 778, product1 뉴스 하락 707-71 → 636 (가격이 날마다 이어진다).
        assert series(product0.investment_product_id) == [770, 778]
        assert series(product1.investment_product_id) == [707, 636]

    def test_price_never_drops_below_floor(self, db_session, ten_companies, now_kst_afternoon):
        up_product, down_product = ten_companies[0], ten_companies[1]
        up_news, down_news = self._seed_one_up_one_down(db_session, up_product, down_product, now_kst_afternoon)
        # 하한 = 가족 시작가(용돈 7000 ÷ 10 = 700)의 10% = 70. 그 근처까지 내려와 있는 상태.
        db_session.add(
            InvestmentPriceHistory(
                family_id=1, investment_product_id=down_product.investment_product_id, price=75,
                recorded_at=now_kst_afternoon - timedelta(days=1),
            )
        )
        db_session.add(Family(family_id=1, weekly_allowance=7000, weekly_reward_limit=3500, financial_knowledge_level="BEGINNER"))
        db_session.commit()

        # 비율을 구간 상한(20%)으로 고정: 75 - 15 = 60 이지만 하한 70 에서 멈춰야 한다.
        # 잔물결 방향은 상승으로 고정해 하한 검증 대상이 하락 뉴스 종목 하나가 되게 한다.
        with (
            patch.object(services.random, "uniform", side_effect=lambda a, b: b),
            patch.object(services.random, "random", return_value=0.0),
        ):
            services.update_prices_for_todays_news(db_session, [up_news, down_news], now_kst_afternoon)

        down_row = (
            db_session.query(InvestmentPriceHistory)
            .filter_by(
                family_id=1,
                investment_product_id=down_product.investment_product_id,
                recorded_at=now_kst_afternoon,
            )
            .one()
        )
        assert down_row.price == 70, "75 - 15 = 60 이지만 하한 70 아래로는 못 내려간다"

    def test_second_call_same_day_is_idempotent(self, db_session, ten_companies, now_kst_afternoon):
        product = ten_companies[0]
        news = InvestmentNews(investment_news_id=1, title="t", content="c", impact="POSITIVE", published_at=now_kst_afternoon)
        db_session.add(news)
        db_session.add(InvestmentNewsProduct(investment_news_id=1, investment_product_id=product.investment_product_id))
        db_session.add(Family(family_id=1, weekly_allowance=0, weekly_reward_limit=3500, financial_knowledge_level="BEGINNER"))
        db_session.commit()

        first = services.update_prices_for_todays_news(db_session, [news], now_kst_afternoon)
        second = services.update_prices_for_todays_news(db_session, [news], now_kst_afternoon)

        assert first == 10
        assert second == 0


class TestBackfillFamilyPrices:
    """온보딩 직후 백엔드가 부르는 단일 가족 백필."""

    def _seed_news(self, db_session, product, days_ago, now, news_id, impact="POSITIVE"):
        news = InvestmentNews(
            investment_news_id=news_id, title="t", content="c", impact=impact,
            published_at=now - timedelta(days=days_ago),
        )
        db_session.add(news)
        db_session.add(
            InvestmentNewsProduct(investment_news_id=news_id, investment_product_id=product.investment_product_id)
        )

    def test_creates_past_week_and_today_for_fresh_family(self, db_session, ten_companies, now_kst_afternoon):
        # 그제 뉴스(어제 가격 재료) + 어제 뉴스(오늘 가격 재료)
        self._seed_news(db_session, ten_companies[0], 2, now_kst_afternoon, news_id=1)
        self._seed_news(db_session, ten_companies[1], 1, now_kst_afternoon, news_id=2, impact="NEGATIVE")
        db_session.add(
            Family(family_id=1, weekly_allowance=7000, weekly_reward_limit=3500, financial_knowledge_level="BEGINNER")
        )
        db_session.commit()

        with (
            patch.object(services.random, "uniform", side_effect=lambda a, b: a),
            patch.object(services.random, "random", return_value=0.0),
        ):
            written = services.backfill_family_prices(db_session, 1, now_kst_afternoon)

        # 어제 10건 + 오늘 10건 — 가입 당일에도 차트에 흐름이 보인다
        assert written == 20
        days = {r.recorded_at for r in db_session.query(InvestmentPriceHistory).all()}
        assert len(days) == 2
        # SQLite 가 tz 를 떨궈 naive 로 돌려주므로 naive 로 맞춰 비교한다
        assert now_kst_afternoon.replace(tzinfo=None) in days, "오늘 몫은 지금 시각으로 기록한다"

    def test_noop_when_family_already_has_history(self, db_session, ten_companies, now_kst_afternoon):
        self._seed_news(db_session, ten_companies[0], 1, now_kst_afternoon, news_id=1)
        db_session.add(
            Family(family_id=1, weekly_allowance=7000, weekly_reward_limit=3500, financial_knowledge_level="BEGINNER")
        )
        db_session.add(
            InvestmentPriceHistory(
                family_id=1, investment_product_id=ten_companies[0].investment_product_id, price=700,
                recorded_at=now_kst_afternoon - timedelta(days=1),
            )
        )
        db_session.commit()

        assert services.backfill_family_prices(db_session, 1, now_kst_afternoon) == 0
        assert db_session.query(InvestmentPriceHistory).count() == 1

    def test_noop_for_unknown_or_not_onboarded_family(self, db_session, ten_companies, now_kst_afternoon):
        db_session.add(
            Family(family_id=2, weekly_allowance=0, weekly_reward_limit=0, financial_knowledge_level="BEGINNER")
        )
        db_session.commit()

        assert services.backfill_family_prices(db_session, 99, now_kst_afternoon) == 0
        assert services.backfill_family_prices(db_session, 2, now_kst_afternoon) == 0

    def test_daily_update_skips_backfilled_family_but_moves_others(
        self, db_session, ten_companies, now_kst_afternoon
    ):
        """가드가 가족 단위라, 당일 온보딩 백필을 받은 가족 때문에 다른 가족이 안 멈춘다."""
        self._seed_news(db_session, ten_companies[0], 1, now_kst_afternoon, news_id=1)
        db_session.add_all(
            [
                Family(family_id=1, weekly_allowance=7000, weekly_reward_limit=3500, financial_knowledge_level="BEGINNER"),
                Family(family_id=2, weekly_allowance=7000, weekly_reward_limit=3500, financial_knowledge_level="BEGINNER"),
            ]
        )
        db_session.commit()
        # 가족 1은 오늘 온보딩해 이미 백필을 받았다
        written_backfill = services.backfill_family_prices(db_session, 1, now_kst_afternoon)
        assert written_backfill > 0

        news = services.yesterdays_news(db_session, now_kst_afternoon)
        written_daily = services.update_prices_for_todays_news(db_session, news, now_kst_afternoon)

        # 가족 2 몫(10건)만 새로 쓰고, 가족 1은 오늘 이미 움직였으니 건너뛴다
        assert written_daily == 10
        family1_today = (
            db_session.query(InvestmentPriceHistory)
            .filter_by(family_id=1, recorded_at=now_kst_afternoon)
            .count()
        )
        assert family1_today == 10, "백필분 외에 더 쌓이면 하루 두 번 움직인 것이다"


class TestYesterdaysNews:
    def test_no_yesterday_on_the_first_day(self, db_session, ten_companies, now_kst_afternoon):
        assert services.yesterdays_news(db_session, now_kst_afternoon) == []

    def test_todays_news_becomes_tomorrows_yesterday(self, db_session, ten_companies, now_kst_afternoon):
        tomorrow = now_kst_afternoon + timedelta(days=1)
        with patch.object(services, "get_client", return_value=_fake_client()):
            today_news = services.generate_todays_news(db_session, now_kst_afternoon)

        yesterday_from_tomorrows_view = services.yesterdays_news(db_session, tomorrow)

        assert {n.investment_news_id for n in yesterday_from_tomorrows_view} == {
            n.investment_news_id for n in today_news
        }


class TestPriceLagAcrossDays:
    """price 는 뉴스가 난 다음 날 움직여야 한다 — 같은 날 만든 뉴스가 그날 가격을
    바로 움직이면(셀프 루프), 실제 사용해보니 그렇게 동작했다는 버그 리포트를 재현·고정한다."""

    def test_todays_news_does_not_move_todays_price(self, db_session, ten_companies, now_kst_afternoon):
        db_session.add(Family(family_id=1, weekly_allowance=0, weekly_reward_limit=3500, financial_knowledge_level="BEGINNER"))
        db_session.commit()

        with patch.object(services, "get_client", return_value=_fake_client()):
            services.generate_todays_news(db_session, now_kst_afternoon)
            written_today = services.update_prices_for_todays_news(
                db_session, services.yesterdays_news(db_session, now_kst_afternoon), now_kst_afternoon
            )

        assert written_today == 0
        assert db_session.query(InvestmentPriceHistory).count() == 0

    def test_todays_news_moves_tomorrows_price(self, db_session, ten_companies, now_kst_afternoon):
        tomorrow = now_kst_afternoon + timedelta(days=1)
        db_session.add(Family(family_id=1, weekly_allowance=0, weekly_reward_limit=3500, financial_knowledge_level="BEGINNER"))
        db_session.commit()

        with patch.object(services, "get_client", return_value=_fake_client()):
            services.generate_todays_news(db_session, now_kst_afternoon)
            written_tomorrow = services.update_prices_for_todays_news(
                db_session, services.yesterdays_news(db_session, tomorrow), tomorrow
            )

        assert written_tomorrow == 10  # 활성 회사 10개 전부 * 가족 1개 (뉴스 3 + 잔물결 7)
        assert db_session.query(InvestmentPriceHistory).count() == 10
