import { router } from 'expo-router';

import { LinkFormView } from '@/widgets/account-link';

export default function ChildAccountLinkScreen() {
  return (
    <LinkFormView
      subtitle="내 계좌를 연결해서 도토리를 모으고 관리할 수 있어요."
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
