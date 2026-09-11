"""investment/router.py (controller) 검증.

`get_session` 을 in-memory SQLite 세션으로 오버라이드해서 실제 Postgres는 안 건드리고,
LLM 은 `unittest.mock.patch`로 대체한다. `TestClient`로 실제 HTTP 요청 경로를 탄다.
"""

import json
from types import SimpleNamespace
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

from app.core.config import get_settings
from app.core.database import get_session
from app.domains.investment import services
from app.main import app


@pytest.fixture
def client(db_session, ten_companies, monkeypatch):
    """토큰 검증 없이(로컬 개발 기본값) 요청을 보내는 기본 클라이언트.

    개발 머신의 실제 `.env` 에 `INTERNAL_TOKEN` 이 설정돼 있으면 이 테스트들이
    그 값에 좌우돼서는 안 된다 — 명시적으로 빈 값으로 고정한다. 토큰 검증 자체를
    테스트하는 케이스는 `client_requiring_token` 을 따로 쓴다.
    """
    monkeypatch.setenv("INTERNAL_TOKEN", "")
    get_settings.cache_clear()

    def _override_get_session():
        yield db_session

    app.dependency_overrides[get_session] = _override_get_session
    try:
        yield TestClient(app)
    finally:
        app.dependency_overrides.pop(get_session, None)
        get_settings.cache_clear()


@pytest.fixture
def client_requiring_token(client, monkeypatch):
    """`INTERNAL_TOKEN` 이 "secret"으로 설정된 상태의 클라이언트."""
    monkeypatch.setenv("INTERNAL_TOKEN", "secret")
    get_settings.cache_clear()
    yield client
    get_settings.cache_clear()


def _fake_client():
    def create(**kwargs):
        user_msg = kwargs["messages"][1]["content"]
        company_line = next(line for line in user_msg.splitlines() if line.startswith("회사:"))
        payload = {"title": f"{company_line} 뉴스", "content": "오늘도 잘 지내요."}
        msg = SimpleNamespace(content=json.dumps(payload, ensure_ascii=False))
        return SimpleNamespace(choices=[SimpleNamespace(message=msg)])

    return SimpleNamespace(chat=SimpleNamespace(completions=SimpleNamespace(create=create)))


def test_daily_endpoint_returns_three_news_with_company_names(client):
    with patch.object(services, "get_client", return_value=_fake_client()):
        response = client.post("/internal/investment/daily")

    assert response.status_code == 200
    body = response.json()
    assert len(body["news"]) == 3
    assert all(item["company_name"] for item in body["news"])
    assert sorted(item["impact"] for item in body["news"]) == ["NEGATIVE", "POSITIVE", "POSITIVE"]


def test_daily_endpoint_is_idempotent_on_second_call(client):
    with patch.object(services, "get_client", return_value=_fake_client()):
        first = client.post("/internal/investment/daily").json()
        second = client.post("/internal/investment/daily").json()

    assert [n["investment_news_id"] for n in first["news"]] == [n["investment_news_id"] for n in second["news"]]
    assert second["price_updates_written"] == 0


def test_daily_endpoint_rejects_missing_internal_token_when_configured(client_requiring_token):
    response = client_requiring_token.post("/internal/investment/daily")
    assert response.status_code == 401


def test_daily_endpoint_accepts_correct_internal_token(client_requiring_token):
    with patch.object(services, "get_client", return_value=_fake_client()):
        response = client_requiring_token.post("/internal/investment/daily", headers={"X-Internal-Token": "secret"})
    assert response.status_code == 200
