import { apiErrorCode, isNetworkError } from '@/shared/api';

/** 회원 탈퇴 실패 원인 → 사용자 문구. 막힌 이유와 다음에 할 일을 함께 알려준다. */
export function withdrawErrorMessage(error: unknown): string {
  if (isNetworkError(error)) return '서버에 연결할 수 없어요.';
  switch (apiErrorCode(error)) {
    case 'PASSWORD_MISMATCH':
      return '비밀번호가 일치하지 않아요.';
    case 'FAMILY_HAS_CHILDREN':
      // 부모: 아이가 남은 채로 나가면 아이가 부모 없는 가족에 갇힌다
      return '가족에 아이가 남아 있어요. 마이페이지의 가족 관리에서 가족을 삭제한 뒤 탈퇴할 수 있어요.';
    case 'HAS_ACTIVE_ASSETS':
      // 자녀: 자산이 남은 채로 탈퇴하면 되찾을 길이 없다
      return '도토리, 예적금, 투자 보유분이나 환전 대기 중인 요청이 남아 있어요. 모두 정리한 뒤 탈퇴할 수 있어요.';
    default:
      return '탈퇴에 실패했어요. 잠시 후 다시 시도해주세요.';
  }
}
