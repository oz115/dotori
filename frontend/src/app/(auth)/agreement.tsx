import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { useSignupStore } from '@/features/auth';
import { useTheme } from '@/shared/theme';
import { Button, Callout, Checkbox, Header, Screen } from '@/shared/ui';

type TermKey = 'required' | 'optionalMarketing' | 'optionalActivity';

const TERMS: { key: TermKey; label: string; required: boolean }[] = [
  { key: 'required', label: '[필수] 개인정보 수집 및 이용', required: true },
  { key: 'optionalMarketing', label: '[선택] 마케팅 정보 수신', required: false },
  { key: 'optionalActivity', label: '[선택] 활동 데이터 분석', required: false },
];

export default function AgreementScreen() {
  const theme = useTheme();
  const patch = useSignupStore((s) => s.patch);
  const reset = useSignupStore((s) => s.reset);

  const [checked, setChecked] = useState<Record<TermKey, boolean>>({
    required: false,
    optionalMarketing: false,
    optionalActivity: false,
  });

  // 약관 동의는 회원가입 퍼널의 진입점 — 이전 회차 값 초기화
  useEffect(() => reset(), [reset]);

  const allChecked = TERMS.every((t) => checked[t.key]);
  const canProceed = TERMS.filter((t) => t.required).every((t) => checked[t.key]);

  const toggleAll = () => {
    const next = !allChecked;
    setChecked({ required: next, optionalMarketing: next, optionalActivity: next });
  };

  const next = () => {
    patch({ agreedOptional: checked.optionalMarketing || checked.optionalActivity });
    router.push('/signup/email');
  };

  return (
    <Screen scroll header={<Header title="약관 동의" onBack={() => router.back()} />}>
      <Callout
        icon={<Ionicons name="document-text-outline" size={22} color={theme.color.brand.solid} />}
        title="서비스 시작 전 동의가 필요해요"
        description="필요한 약관을 확인하고 동의해주세요."
      />

      <View
        style={[
          styles.card,
          {
            marginTop: theme.space.xl,
            borderColor: theme.color.border.default,
            borderRadius: theme.radius.lg,
          },
        ]}
      >
        <Pressable
          onPress={toggleAll}
          style={[styles.rowAll, { padding: theme.space.lg, gap: theme.space.sm }]}
          accessibilityRole="checkbox"
          accessibilityState={{ checked: allChecked }}
        >
          <Checkbox shape="square" checked={allChecked} onChange={toggleAll} />
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            전체 동의
          </Text>
        </Pressable>

        {TERMS.map((term) => (
          <View
            key={term.key}
            style={[
              styles.row,
              { padding: theme.space.lg, borderTopColor: theme.color.border.default },
            ]}
          >
            <Checkbox
              shape="square"
              checked={checked[term.key]}
              onChange={(v) => setChecked((prev) => ({ ...prev, [term.key]: v }))}
              label={term.label}
            />
            <Pressable
              onPress={() => Alert.alert(term.label, '약관 전문은 준비 중이에요.')}
              hitSlop={8}
            >
              <Ionicons name="chevron-forward" size={18} color={theme.color.text.muted} />
            </Pressable>
          </View>
        ))}
      </View>

      <View style={[styles.spacer, { minHeight: theme.space['2xl'] }]} />

      <Button label="다음" variant="primary" onPress={next} disabled={!canProceed} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    overflow: 'hidden',
  },
  rowAll: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderTopWidth: 1,
  },
  spacer: { flex: 1 },
});
