import { router, useLocalSearchParams } from 'expo-router';

import { LinkFormView } from '@/widgets/account-link';

/**
 * 계좌 연동 (자녀). 온보딩에서도, 마이페이지의 "연동 계좌 변경" 에서도 들어온다.
 *
 * 1원 인증 절차는 두 경우가 같다 - 서버는 같은 계좌번호만 막고(ACCOUNT_ALREADY_LINKED),
 * 다른 계좌로 인증을 마치면 그 자리에서 교체한다. 그래서 문구만 갈라 쓴다 (부모와 같은 규칙).
 */
export default function ChildAccountLinkScreen() {
  const params = useLocalSearchParams<{ mode?: string }>();
  const isChange = params.mode === 'change';

  return (
    <LinkFormView
      headerTitle={isChange ? '연동 계좌 변경' : '계좌 연동'}
      title={isChange ? '새로운 계좌로 변경하기' : '내 계좌 연결하기'}
      subtitle={
        isChange
          ? '새로 연동할 내 계좌 정보를 입력해주세요.'
          : '내 계좌를 연결해서 도토리를 모으고 관리할 수 있어요.'
      }
      notes={
        isChange
          ? [
              '내 이름으로 된 계좌만 연결할 수 있어요.',
              '계좌 확인을 위해 1원이 입금돼요.',
              '인증을 마치기 전까지는 지금 계좌가 그대로 쓰여요.',
              '1원 인증을 마치면 새 계좌로 바로 바뀌어요.',
            ]
          : undefined
      }
      onBack={() => router.back()}
      onSelectBank={() => router.push('/setup/account/bank-select')}
      onRequested={(res) =>
        router.push({
          pathname: '/setup/account/verify',
          params: { authText: res.authText, expiresInSeconds: String(res.expiresInSeconds) },
        })
      }
    />
  );
}
