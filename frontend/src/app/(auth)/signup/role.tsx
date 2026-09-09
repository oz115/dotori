import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import {
  SignupStepLayout,
  signupErrorMessage,
  useSignup,
  useSignupStore,
  type UserRole,
} from '@/features/auth';
import { useTheme } from '@/shared/theme';
import { Button, SelectableCard } from '@/shared/ui';

const ROLES: { key: UserRole; title: string; description: string; benefits: string[] }[] = [
  {
    key: 'PARENT',
    title: '부모',
    description: '가족의 경제 규칙을 만들고 관리해요',
    benefits: [
      '집안일 보상·예적금·상점 가격을 직접 설정해요',
      '자녀의 경제활동과 승인 요청을 확인해요',
    ],
  },
  {
    key: 'CHILD',
    title: '자녀',
    description: '도토리를 모으고 배우며 직접 경험해요',
    benefits: [
      '용돈을 도토리로 교환할 수 있어요',
      '도토리로 소비·저축·투자·수입 활동을 해요',
      '금융 관련 지식을 배울 수 있어요',
    ],
  },
];

export default function SignupRoleScreen() {
  const theme = useTheme();
  const signup = useSignup();

  const [role, setRole] = useState<UserRole | null>(() => useSignupStore.getState().role);

  const selected = ROLES.find((r) => r.key === role);

  const complete = () => {
    if (!role || signup.isPending) return;
    const { loginId, password, name, email, verificationToken, reset } = useSignupStore.getState();
    signup.mutate(
      { loginId, password, name, email, role, verificationToken },
      {
        onSuccess: () => {
          reset();
          Alert.alert('회원가입 완료', '가입한 아이디로 로그인해주세요.', [
            { text: '확인', onPress: () => router.replace('/') },
          ]);
        },
        onError: (error) => Alert.alert('회원가입 실패', signupErrorMessage(error)),
      },
    );
  };

  return (
    <SignupStepLayout
      step={3}
      title="역할 선택"
      subtitle={'어떤 역할로 시작할까요?\n도토리에서의 역할을 선택해주세요.'}
      footer={
        <Button
          label="가입 완료"
          variant="primary"
          onPress={complete}
          loading={signup.isPending}
          disabled={!role}
        />
      }
    >
      <View style={[styles.cards, { gap: theme.space.md }]}>
        {ROLES.map((r) => (
          <SelectableCard
            key={r.key}
            selected={role === r.key}
            onPress={() => setRole(r.key)}
            style={styles.card}
          >
            <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
              {r.title}
            </Text>
            <Text
              style={[
                theme.typography.bodySm,
                { color: theme.color.text.sub, marginTop: theme.space.xs },
              ]}
            >
              {r.description}
            </Text>
          </SelectableCard>
        ))}
      </View>

      {selected ? (
        <View style={{ marginTop: theme.space['2xl'], gap: theme.space.sm }}>
          {selected.benefits.map((b) => (
            <View key={b} style={styles.bulletRow}>
              <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>•</Text>
              <Text
                style={[
                  theme.typography.bodySm,
                  styles.bulletText,
                  { color: theme.color.text.sub },
                ]}
              >
                {b}
              </Text>
            </View>
          ))}
        </View>
      ) : null}
    </SignupStepLayout>
  );
}

const styles = StyleSheet.create({
  cards: { flexDirection: 'row' },
  card: { flex: 1 },
  bulletRow: { flexDirection: 'row', gap: 8 },
  bulletText: { flex: 1 },
});
