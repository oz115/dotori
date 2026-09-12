import { router } from 'expo-router';

import { LinkFormView } from '@/widgets/account-link';

/**
 * 부모 계좌 연동. 자녀와 같은 화면(widgets/account-link)을 쓰되 안내 문구와 이동 경로만 다르다.
 * 부모 계좌는 충전(자녀→부모)의 수취 계좌이자 환전 승인(부모→자녀)의 출금 계좌라서,
 * 이게 없으면 두 기능이 모두 ACCOUNT_NOT_LINKED 로 막힌다.
 */
export default function ParentAccountLinkScreen() {
  return (
    <LinkFormView
      subtitle="아이의 충전·환전을 처리하려면 계좌 연결이 필요해요."
      onBack={() => router.back()}
      onSelectBank={() => router.push('/account/bank-select')}
      onRequested={(res) =>
        router.push({
          pathname: '/account/verify',
          params: { authText: res.authText, expiresInSeconds: String(res.expiresInSeconds) },
        })
      }
    />
  );
}
