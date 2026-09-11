import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * 초 단위 카운트다운. `start(seconds)` 로 시작, 0 이 되면 멈춘다.
 * 이메일 인증코드 유효시간·초대 코드 만료 표시 등에 사용.
 */
export function useCountdown() {
  const [remaining, setRemaining] = useState(0);
  const timer = useRef<ReturnType<typeof setInterval> | null>(null);

  const stop = useCallback(() => {
    if (timer.current) {
      clearInterval(timer.current);
      timer.current = null;
    }
  }, []);

  const start = useCallback(
    (seconds: number) => {
      stop();
      setRemaining(Math.max(0, Math.floor(seconds)));
      timer.current = setInterval(() => {
        setRemaining((prev) => {
          if (prev <= 1) {
            stop();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    },
    [stop],
  );

  useEffect(() => stop, [stop]);

  const mm = String(Math.floor(remaining / 60)).padStart(2, '0');
  const ss = String(remaining % 60).padStart(2, '0');

  return { remaining, label: `${mm}:${ss}`, running: remaining > 0, start, stop };
}
