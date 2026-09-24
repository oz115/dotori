"""내부 호출 인증.

프론트(React Native)는 이 서버를 직접 부르지 않는다. 사용자 인증(JWT)은 Spring 이
처리하고, Spring 이 이 서버를 부를 때는 양쪽이 공유하는 토큰 하나만 확인한다.

업무 엔드포인트를 추가할 때 이렇게 붙인다.

    @router.post("/internal/quiz/generate", dependencies=[Depends(require_internal_token)])
"""

import secrets

from fastapi import Header, HTTPException, status

from app.core.config import get_settings


def require_internal_token(x_internal_token: str = Header(default="")) -> None:
    expected = get_settings().internal_token
    if not expected:
        # 로컬 개발 편의를 위해 토큰이 설정되지 않았으면 통과시킨다.
        # 배포 환경에서는 INTERNAL_TOKEN 을 반드시 주입한다.
        return
    # 문자열 비교 시간으로 토큰을 추측하는 것을 막는다.
    if not secrets.compare_digest(x_internal_token, expected):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="유효하지 않은 내부 호출 토큰입니다.",
        )
