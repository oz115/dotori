import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * 초 단위 카운트다운. `start(seconds)` 로 시작, 0 이 되면 멈춘다.
 * 이메일 인증코드 유효시간·초대 코드 만료 표시 등에 사용.
 *
 * `stop()` 은 타이머를 멈추고 남은 시간도 0 으로 되돌린다. 남은 시간을 그대로 두면
 * 멈춘 뒤에도 `running` 이 true 로 남아, 재전송 쿨다운처럼 `running` 으로 버튼을 막는 곳이
 * 영영 풀리지 않는다.
 */
export function useCountdown() {
  const [remaining, setRemaining] = useState(0);
  const timer = useRef<ReturnType<typeof setInterval> | null>(null);

  const clear = useCallback(() => {
    if (timer.current) {
      clearInterval(timer.current);
      timer.current = null;
    }
  }, []);

  const stop = useCallback(() => {
    clear();
    setRemaining(0);
  }, [clear]);

  const start = useCallback(
    (seconds: number) => {
      clear();
      setRemaining(Math.max(0, Math.floor(seconds)));
      timer.current = setInterval(() => {
        setRemaining((prev) => {
          if (prev <= 1) {
            clear();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    },
    [clear],
  );

  // 언마운트 시에는 타이머만 정리한다 (사라진 컴포넌트의 state 를 건드리지 않는다)
  useEffect(() => clear, [clear]);

  const mm = String(Math.floor(remaining / 60)).padStart(2, '0');
  const ss = String(remaining % 60).padStart(2, '0');

  return { remaining, label: `${mm}:${ss}`, running: remaining > 0, start, stop };
}
