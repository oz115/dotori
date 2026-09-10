import { router } from 'expo-router';
import { useState } from 'react';

import {
  ALLOWANCE_PRESETS_WON,
  AmountStepInput,
  isValidOnboardingAmount,
  OnboardingStepLayout,
  StepFooter,
  useOnboardingStore,
} from '@/features/family';

const HINT = '1,000원 이상, 1,000원 단위로 입력해요.';

export default function OnboardingAllowanceScreen() {
  const patch = useOnboardingStore((s) => s.patch);

  const [allowance, setAllowance] = useState<number | null>(
    () => useOnboardingStore.getState().weeklyAllowance,
  );
  const [error, setError] = useState('');

  const next = () => {
    if (!isValidOnboardingAmount(allowance)) {
      setError(HINT);
      return;
    }
    setError('');
    patch({ weeklyAllowance: allowance });
    router.push('/onboarding/reward-limit');
  };

  return (
    <OnboardingStepLayout
      step={2}
      compactHeader
      headerTitle="가족 생태계 만들기"
      eyebrow="기본 용돈"
      title="기본 용돈을 정해주세요"
      subtitle="미션이나 활동 보상을 뺀, 매주 정기적으로 주는 용돈이에요."
      footer={
        <StepFooter
          primaryLabel="다음"
          onPrimary={next}
          primaryDisabled={!isValidOnboardingAmount(allowance)}
        />
      }
    >
      <AmountStepInput
        value={allowance}
        onChange={(v) => {
          setAllowance(v);
          if (error) setError('');
        }}
        presets={ALLOWANCE_PRESETS_WON}
        hint={HINT}
        error={error || undefined}
      />
    </OnboardingStepLayout>
  );
}
