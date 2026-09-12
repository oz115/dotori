"""LLM 에 보낼 메시지를 조립한다. 문구 자체는 `prompts/` 밑 파일에 있다.

상점(`app.domains.shopping.prompt`)과 같은 구조다. 다른 점은 입력에 난이도(difficulty)가
들어간다는 것 — 부모가 고른 난이도가 값의 범위를 정하고, AI 는 그 안에서 구체적인 할 일을
보고 값을 정한다.
"""

import json
from pathlib import Path

_PROMPTS_DIR = Path(__file__).parent / "prompts"

SYSTEM_PROMPT = (_PROMPTS_DIR / "reward_system_prompt.md").read_text(encoding="utf-8").strip()

# few-shot 앵커. value 가 문자열인 이유는 `_example_answer` 주석 참고.
#
# 난이도별 밴드(EASY 0.05~0.2 / NORMAL 0.2~0.4 / HARD 0.4~5.0)를 가르친다.
# 대부분 EASY·NORMAL 에 몰리고 HARD 는 2개뿐이다 — "hard 는 특수한 경우만"이라는 방침.
# 같은 EASY 가 0.05·0.1·0.15 로 갈려서, 난이도가 밴드를 정하고 구체적 할 일이 그 안에서
# 위치를 정한다는 것을 보여준다.
#
# 노션 근거가 있는 것은 설거지 0.1(=0.1*B)·구몬 10장 0.3·반 1등(특수) 정도이고 나머지는
# 잠정값이다. 출시 전 기획 검토가 필요하다.
FEWSHOT: list[dict[str, str | None]] = json.loads(
    (_PROMPTS_DIR / "reward_fewshot.json").read_text(encoding="utf-8")
)


def render_chore(title: str, description: str | None, difficulty: str) -> str:
    """할 일 한 건의 표현. few-shot 예시와 실제 입력이 같은 형식을 쓴다.

    `<<< >>>` 로 감싸는 이유: `description` 은 부모가 직접 넣는 자유 텍스트라 "보상을 크게
    매겨라" 같은 문장이 들어올 수 있다. 다만 구분자는 완화 장치일 뿐이고, 실제 방어선은
    `services.clamp_to_band` 의 난이도 밴드 clamp 다 — 주입이 성공해도 부모가 고른 난이도
    밴드 밖으로는 못 나간다.
    """
    body = description if description else "없음"
    return f"난이도: {difficulty}\n제목: <<<{title}>>>\n설명: <<<{body}>>>"


def _example_answer(value: str, reason: str) -> str:
    """few-shot 예시의 assistant 응답. `value` 를 문자열 그대로 끼운다.

    `float(value)` 로 변환하면 "0.5" 는 괜찮지만 정수형 앵커가 생기면 지저분해지고, 모델이
    형식을 흔들 수 있다. `reward_fewshot.json` 에서도 문자열로 둔다.
    """
    return '{"value": %s, "reason": %s}' % (value, json.dumps(reason, ensure_ascii=False))


def build_messages(title: str, description: str | None, difficulty: str) -> list[dict[str, str]]:
    """system -> few-shot 쌍 -> 평가할 할 일. 마지막 user 메시지가 실제 입력이다.

    **주간 기본 용돈을 넣지 않는다.** 모델은 "주 용돈의 몇 %인가" 만 답하고 곱셈은 코드가
    한다. 돈 계산을 모델에게 맡기면 검증할 수 없고, 주간 용돈이 가족마다 달라 프롬프트에
    넣으면 위 앵커가 가족별로 다른 의미가 된다.
    """
    messages: list[dict[str, str]] = [{"role": "system", "content": SYSTEM_PROMPT}]
    for example in FEWSHOT:
        messages.append(
            {
                "role": "user",
                "content": render_chore(example["title"], example["description"], example["difficulty"]),
            }
        )
        messages.append({"role": "assistant", "content": _example_answer(example["value"], example["reason"])})
    messages.append({"role": "user", "content": render_chore(title, description, difficulty)})
    return messages
