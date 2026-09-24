import { router, useLocalSearchParams } from 'expo-router';

import { LinkFormView } from '@/widgets/account-link';

/**
 * 부모 계좌 연동. 자녀와 같은 화면(widgets/account-link)을 쓰되 안내 문구와 이동 경로만 다르다.
 * 부모 계좌는 충전(자녀→부모)의 수취 계좌이자 환전 승인(부모→자녀)의 출금 계좌라서,
 * 이게 없으면 두 기능이 모두 ACCOUNT_NOT_LINKED 로 막힌다.
 */
export default function ParentAccountLinkScreen() {
  const params = useLocalSearchParams<{ mode?: string }>();
  const isChange = params.mode === 'change';

  return (
    <LinkFormView
      headerTitle={isChange ? '연동 계좌 변경' : '계좌 연동'}
      title={isChange ? '새로운 계좌로 변경하기' : '내 계좌 연결하기'}
      subtitle={
        isChange
          ? '새로 연동할 본인 명의 계좌 정보를 입력해주세요.'
          : '아이의 충전·환전을 처리하려면 계좌 연결이 필요해요.'
      }
      notes={
        isChange
          ? [
              '본인 명의 계좌만 연결할 수 있어요.',
              '계좌 확인을 위해 1원이 입금돼요.',
              '인증 완료 전까지는 기존 계좌가 안전하게 유지돼요.',
              '1원 인증 완료 시 새 계좌로 즉시 교체돼요.',
            ]
          : undefined
      }
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
