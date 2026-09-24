import { router, useLocalSearchParams } from 'expo-router';

import { VerifyView } from '@/widgets/account-link';

export default function ChildAccountVerifyScreen() {
  const params = useLocalSearchParams<{ authText?: string; expiresInSeconds?: string }>();

  return (
    <VerifyView
      authText={params.authText ?? '도토리'}
      expiresInSeconds={Number(params.expiresInSeconds) || 180}
      onBack={() => router.back()}
      onVerified={() => router.replace('/')}
    />
  );
}
