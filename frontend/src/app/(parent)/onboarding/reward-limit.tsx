import { router } from 'expo-router';
import { useState } from 'react';

import {
  AmountStepInput,
  isValidOnboardingAmount,
  OnboardingStepLayout,
  REWARD_LIMIT_PRESETS_WON,
  StepFooter,
  useOnboardingStore,
} from '@/features/family';

const HINT = '1,000원 이상, 1,000원 단위로 입력해요.';

export default function OnboardingRewardLimitScreen() {
  const patch = useOnboardingStore((s) => s.patch);

  const [rewardLimit, setRewardLimit] = useState<number | null>(
    () => useOnboardingStore.getState().weeklyRewardLimit,
  );
  const [error, setError] = useState('');

  const next = () => {
    if (!isValidOnboardingAmount(rewardLimit)) {
      setError(HINT);
      return;
    }
    setError('');
    patch({ weeklyRewardLimit: rewardLimit });
    router.push('/onboarding/knowledge');
  };

  return (
    <OnboardingStepLayout
      step={2}
      compactHeader
      headerTitle="가족 생태계 만들기"
      eyebrow="추가 보상 한도"
      title={'한 주 보상 한도를 정해주세요'}
      subtitle={
        '예·적금, 투자, 퀴즈 활동으로 아이가 한 주에 받을 수 있는 보상의 합계예요. \n' +
        '부모님이 정한 미션 보상은 포함되지 않아요.'
      }
      footer={
        <StepFooter
          showBack
          primaryLabel="다음"
          onPrimary={next}
          primaryDisabled={!isValidOnboardingAmount(rewardLimit)}
        />
      }
    >
      <AmountStepInput
        value={rewardLimit}
        onChange={(v) => {
          setRewardLimit(v);
          if (error) setError('');
        }}
        presets={REWARD_LIMIT_PRESETS_WON}
        hint={HINT}
        error={error || undefined}
      />
    </OnboardingStepLayout>
  );
}
