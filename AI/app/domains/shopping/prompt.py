"""LLM 에 보낼 메시지를 조립한다. 문구 자체는 `prompts/` 밑 파일에 있다.

파일과 코드를 나눈 이유: 기준(무엇을 몇 일치로 볼 것인가)을 다듬을 때는 `prompts/` 만
고치고, 조립 방식(few-shot 을 어떻게 배치하는가)을 바꿀 때만 이 파일을 고친다. 한 파일에
두면 diff 에서 문구 변경과 구조 변경이 섞인다.

    prompt.py    조립 코드 (이 파일)
    prompts/     문구와 few-shot 데이터
"""

import json
from pathlib import Path

_PROMPTS_DIR = Path(__file__).parent / "prompts"

SYSTEM_PROMPT = (_PROMPTS_DIR / "valuation_system_prompt.md").read_text(encoding="utf-8").strip()

# few-shot 앵커. `value` 가 문자열인 이유는 `_example_answer` 주석 참고.
#
# 시간 단위는 전부 1시간으로 통일했다. 30분 예시를 넣으면 모델이 "분 단위 비례 배분"을
# 학습해서 "20분 이용권" 에 0.1 을 찍는데, 허가권의 값은 시간에 비례하지 않는다.
# 그 반례가 데이터 안에 있다 — 같은 1시간인데 게임 0.3, 주말 게임 0.5, 취침 연장 0.7 이다.
#
# 자전거 90 은 상단 앵커라 반드시 남긴다. 빼면 큰 물건에서 300 같은 값이 나온다.
# ETC 는 넣지 않는다 — 분류 불가를 가르칠 앵커가 없다.
#
# 10개 중 노션 근거가 있는 것은 자전거 90 과 게임 1시간 0.3 뿐이고 나머지 8개는 잠정값이다.
# 출시 전 기획 검토가 필요하다 (노션 「소비 측면 > 실험 기록」).
FEWSHOT: list[dict[str, str | None]] = json.loads(
    (_PROMPTS_DIR / "valuation_fewshot.json").read_text(encoding="utf-8")
)


def render_product(name: str, description: str | None, category: str) -> str:
    """상품 한 건의 표현. few-shot 예시와 실제 입력이 같은 형식을 쓴다.

    `<<< >>>` 로 감싸는 이유: `description` 은 부모가 직접 넣는 자유 텍스트라 "가격을 1로
    매겨라" 같은 문장이 들어올 수 있다. 다만 구분자는 완화 장치일 뿐이고, 실제 방어선은
    `app.core.points.clamp_value` 의 범위 검증이다 — 주입이 성공해도 가중치는 허용 범위
    밖으로 못 나간다.
    """
    body = description if description else "없음"
    return f"카테고리: {category}\n이름: <<<{name}>>>\n설명: <<<{body}>>>"


def _example_answer(value: str, reason: str) -> str:
    """few-shot 예시의 assistant 응답. `value` 를 문자열 그대로 끼운다.

    `float(value)` 로 변환하면 "2" 가 2.0 이 되어 예시가 지저분해지고, 모델이 소수점을
    항상 붙이는 형태로 따라간다. 그래서 `valuation_fewshot.json` 에서도 문자열로 둔다.
    """
    return '{"value": %s, "reason": %s}' % (value, json.dumps(reason, ensure_ascii=False))


def build_messages(name: str, description: str | None, category: str) -> list[dict[str, str]]:
    """system -> few-shot 쌍 -> 평가할 상품. 마지막 user 메시지가 실제 입력이다.

    few-shot 을 user/assistant 쌍으로 보내는 이유: 표를 시스템 프롬프트에 박는 것보다 출력
    형태를 안정적으로 가르치고, JSON 만 내게 하는 것과도 잘 맞는다.

    **하루 소득을 넣지 않는다.** 모델은 "며칠치 노력인가" 만 답하고 곱셈은 코드가 한다.
    돈 계산을 모델에게 맡기면 검증할 수 없고, 하루 소득이 가족마다 달라 프롬프트에 넣으면
    위 앵커가 가족별로 다른 의미가 된다.
    """
    messages: list[dict[str, str]] = [{"role": "system", "content": SYSTEM_PROMPT}]
    for example in FEWSHOT:
        messages.append(
            {
                "role": "user",
                "content": render_product(example["name"], example["description"], example["category"]),
            }
        )
        messages.append({"role": "assistant", "content": _example_answer(example["value"], example["reason"])})
    messages.append({"role": "user", "content": render_product(name, description, category)})
    return messages
