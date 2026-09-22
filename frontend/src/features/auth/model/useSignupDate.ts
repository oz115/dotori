import { useProfile } from '@/features/auth/model/useProfile';
import { fromDateParam } from '@/shared/lib/dateRange';

/**
 * 로그인한 본인의 가입일 (그날 자정 기준 Date). 기간 조회 달력의 하한으로 쓴다 —
 * 가입 전 날짜는 어느 내역 화면에도 보여줄 데이터가 없다. 프로필 로딩 전엔 undefined
 * (그동안은 잠시 제한 없이 열리지만, 서버가 어차피 빈 결과를 준다).
 */
export function useSignupDate(): Date | undefined {
  const profile = useProfile();
  const createdAt = profile.data?.createdAt;
  return createdAt != null ? fromDateParam(createdAt.slice(0, 10)) : undefined;
}
