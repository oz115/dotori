import { useFamilyStatus } from '@/features/family/model/useFamilyStatus';

/**
 * 내 가족 id. 서버의 가족 상태 조회(`GET /families/status`)가 함께 내려준다.
 *
 * 예전에는 기기(SecureStore)에 저장해 둔 값을 읽었는데, 로그아웃하면 지워지고
 * 다시 로그인해도 채울 방법이 없어 화면이 영영 로딩 상태로 남았다. 서버가 값을 주는 지금은
 * 그쪽이 유일한 출처다 — 계정을 바꿔도 항상 그 계정의 가족을 가리킨다.
 *
 * `useFamilyStatus` 와 같은 쿼리를 공유하므로 요청이 더 늘지 않는다.
 */
export function useFamilyId() {
  const status = useFamilyStatus();

  return {
    data: status.data?.familyId ?? undefined,
    isPending: status.isPending,
    isError: status.isError,
  };
}
