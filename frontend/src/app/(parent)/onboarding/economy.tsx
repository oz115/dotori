import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, Text, View } from 'react-native';

import {
  ALLOWANCE_PRESETS_WON,
  AmountPresetField,
  createFamilyErrorMessage,
  isValidOnboardingAmount,
  KNOWLEDGE_LEVELS,
  OnboardingStepLayout,
  REWARD_LIMIT_PRESETS_WON,
  useCreateFamily,
  useOnboardingStore,
  type FinancialKnowledgeLevel,
} from '@/features/family';
import { useTheme } from '@/shared/theme';
import { Button, SelectableCard } from '@/shared/ui';

const AMOUNT_HINT = '1,000원 이상, 1,000원 단위로 입력해요.';

export default function OnboardingEconomyScreen() {
  const theme = useTheme();
  const createFamily = useCreateFamily();

  const [allowance, setAllowance] = useState<number | null>(
    () => useOnboardingStore.getState().weeklyAllowance,
  );
  const [rewardLimit, setRewardLimit] = useState<number | null>(
    () => useOnboardingStore.getState().weeklyRewardLimit,
  );
  const [level, setLevel] = useState<FinancialKnowledgeLevel | null>(
    () => useOnboardingStore.getState().financialKnowledgeLevel,
  );
  const [errors, setErrors] = useState<{ allowance?: string; rewardLimit?: string }>({});
  const [submitError, setSubmitError] = useState('');

  const submit = () => {
    if (createFamily.isPending) return;

    const nextErrors: typeof errors = {};
    if (!isValidOnboardingAmount(allowance)) nextErrors.allowance = AMOUNT_HINT;
    if (!isValidOnboardingAmount(rewardLimit)) nextErrors.rewardLimit = AMOUNT_HINT;
    setErrors(nextErrors);
    if (!isValidOnboardingAmount(allowance) || !isValidOnboardingAmount(rewardLimit) || !level) {
      return;
    }

    setSubmitError('');
    const { familyName, patch, reset } = useOnboardingStore.getState();
    patch({
      weeklyAllowance: allowance,
      weeklyRewardLimit: rewardLimit,
      financialKnowledgeLevel: level,
    });

    createFamily.mutate(
      {
        name: familyName,
        weeklyAllowance: allowance,
        weeklyRewardLimit: rewardLimit,
        financialKnowledgeLevel: level,
      },
      {
        onSuccess: () => {
          reset();
          Alert.alert('가족이 만들어졌어요', '이제 아이를 초대해 함께 시작해요.', [
            { text: '확인', onPress: () => router.replace('/') },
          ]);
        },
        onError: (e) => setSubmitError(createFamilyErrorMessage(e)),
      },
    );
  };

  const canSubmit =
    isValidOnboardingAmount(allowance) && isValidOnboardingAmount(rewardLimit) && !!level;

  return (
    <OnboardingStepLayout
      step={2}
      title="우리집 경제 규칙"
      subtitle={'아이의 용돈과 보상 한도를 정하고,\n금융 이해 수준을 알려주세요.'}
      footer={
        <Button
          label="가족 만들기 완료"
          variant="primary"
          onPress={submit}
          loading={createFamily.isPending}
          disabled={!canSubmit}
        />
      }
    >
      <View style={{ gap: theme.space['3xl'] }}>
        <AmountPresetField
          label="주간 기본 용돈"
          presets={ALLOWANCE_PRESETS_WON}
          value={allowance}
          onChange={(v) => {
            setAllowance(v);
            if (errors.allowance) setErrors((e) => ({ ...e, allowance: undefined }));
          }}
          hint={AMOUNT_HINT}
          error={errors.allowance}
        />

        <AmountPresetField
          label="주간 추가 보상 한도"
          presets={REWARD_LIMIT_PRESETS_WON}
          value={rewardLimit}
          onChange={(v) => {
            setRewardLimit(v);
            if (errors.rewardLimit) setErrors((e) => ({ ...e, rewardLimit: undefined }));
          }}
          hint={`도토리 활동으로 한 주에 받을 수 있는 최대 금액이에요. ${AMOUNT_HINT}`}
          error={errors.rewardLimit}
        />

        <View style={{ gap: theme.space.md }}>
          <Text style={[theme.typography.label, { color: theme.color.text.label }]}>
            아이의 금융 이해 수준
          </Text>
          <View style={{ gap: theme.space.sm }}>
            {KNOWLEDGE_LEVELS.map((lv) => (
              <SelectableCard
                key={lv.key}
                selected={level === lv.key}
                onPress={() => setLevel(lv.key)}
              >
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
        </View>

        {submitError ? (
          <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
            {submitError}
          </Text>
        ) : null}
      </View>
    </OnboardingStepLayout>
  );
}
