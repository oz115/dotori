import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, Text, View } from 'react-native';

import {
  createFamilyErrorMessage,
  KNOWLEDGE_LEVELS,
  OnboardingStepLayout,
  StepFooter,
  useCreateFamily,
  useOnboardingStore,
  type FinancialKnowledgeLevel,
} from '@/features/family';
import { useTheme } from '@/shared/theme';
import { SelectableCard } from '@/shared/ui';

export default function OnboardingKnowledgeScreen() {
  const theme = useTheme();
  const createFamily = useCreateFamily();

  const [level, setLevel] = useState<FinancialKnowledgeLevel | null>(
    () => useOnboardingStore.getState().financialKnowledgeLevel,
  );
  const [error, setError] = useState('');

  const complete = () => {
    if (!level || createFamily.isPending) return;
    setError('');

    const { familyName, weeklyAllowance, weeklyRewardLimit, reset } = useOnboardingStore.getState();
    if (weeklyAllowance == null || weeklyRewardLimit == null) {
      // 앞 단계를 건너뛰고 들어온 경우 — 처음부터 다시
      router.replace('/onboarding');
      return;
    }

    createFamily.mutate(
      {
        name: familyName,
        weeklyAllowance,
        weeklyRewardLimit,
        financialKnowledgeLevel: level,
      },
      {
        onSuccess: () => {
          reset();
          Alert.alert('가족이 만들어졌어요', '이제 아이를 초대해 함께 시작해요.', [
            { text: '확인', onPress: () => router.replace('/') },
          ]);
        },
        onError: (e) => setError(createFamilyErrorMessage(e)),
      },
    );
  };

  return (
    <OnboardingStepLayout
      step={2}
      compactHeader
      headerTitle="가족 생태계 만들기"
      eyebrow="금융 지식 수준"
      title={'아이의 금융 지식 수준을 골라주세요'}
      subtitle="아이에게 맞는 설명과 퀴즈 난이도를 정하는 데 활용해요."
      footer={
        <StepFooter
          showBack
          primaryLabel="설정 완료"
          onPrimary={complete}
          primaryDisabled={!level}
          primaryLoading={createFamily.isPending}
        />
      }
    >
      <View style={{ gap: theme.space.sm }}>
        {KNOWLEDGE_LEVELS.map((lv) => (
          <SelectableCard key={lv.key} selected={level === lv.key} onPress={() => setLevel(lv.key)}>
            <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
              {lv.title}
            </Text>
            <Text
              style={[
                theme.typography.bodySm,
                { color: theme.color.text.sub, marginTop: theme.space.xs },
              ]}
            >
              {lv.description}
            </Text>
          </SelectableCard>
        ))}
      </View>

      {error ? (
        <Text
          style={[
            theme.typography.caption,
            { color: theme.color.status.danger.text, marginTop: theme.space.lg },
          ]}
        >
          {error}
        </Text>
      ) : null}
    </OnboardingStepLayout>
  );
}
