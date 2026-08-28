/**
 * 앱 전역 환경 변수 접근점.
 *
 * Expo는 `EXPO_PUBLIC_` 접두사가 붙은 .env 값을 클라이언트 번들에 인라인합니다.
 * 이 파일 하나만 거쳐서 process.env 에 접근하도록 강제해, 값 이름 오타나
 * 누락된 기본값을 여기서 한 번에 잡습니다. 다른 레이어에서는 절대
 * process.env 를 직접 읽지 말고 이 모듈을 import 하세요.
 */

function required(name: string, value: string | undefined): string {
  if (!value) {
    throw new Error(`[env] Missing required environment variable: ${name}`);
  }
  return value;
}

export const env = {
  apiUrl: required('EXPO_PUBLIC_API_URL', process.env.EXPO_PUBLIC_API_URL),
} as const;
