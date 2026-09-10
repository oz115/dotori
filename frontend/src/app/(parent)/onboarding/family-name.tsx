import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';

import { OnboardingStepLayout, useOnboardingStore } from '@/features/family';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Input } from '@/shared/ui';

const MAX_NAME = 100;

export default function OnboardingFamilyNameScreen() {
  const theme = useTheme();
  const patch = useOnboardingStore((s) => s.patch);

  const [name, setName] = useState(() => useOnboardingStore.getState().familyName);

  const trimmed = name.trim();
  const canProceed = trimmed.length > 0 && trimmed.length <= MAX_NAME;

  const next = () => {
    if (!canProceed) return;
    patch({ familyName: trimmed });
    router.push('/onboarding/economy');
  };

  return (
    <OnboardingStepLayout
      step={1}
      title={'가족과 아이를 초대하고\n도토리 활동을 함께해 보세요!'}
      footer={<Button label="다음" variant="primary" onPress={next} disabled={!canProceed} />}
    >
      <View style={{ gap: theme.space['2xl'] }}>
        <Input
          variant="line"
          label="가족이름"
          placeholder="예: 도토리 가족"
          value={name}
          onChangeText={setName}
          maxLength={MAX_NAME}
          returnKeyType="done"
          onSubmitEditing={next}
        />
        <Callout
          icon={
            <Ionicons name="information-circle-outline" size={20} color={theme.color.brand.solid} />
          }
          title="가족 이름은 나중에 변경할 수 있어요"
        />
      </View>
    </OnboardingStepLayout>
  );
}
