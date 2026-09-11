import { Ionicons } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Pressable, Share, StyleSheet, Text, View } from 'react-native';

import {
  invitationErrorMessage,
  OnboardingStepLayout,
  StepFooter,
  useCreateInvitation,
  useFamilyId,
} from '@/features/family';
import { useCountdown } from '@/shared/lib/useCountdown';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

export default function OnboardingInviteScreen() {
  const theme = useTheme();
  const params = useLocalSearchParams<{ familyId?: string }>();

  const [error, setError] = useState('');

  const invitation = useCreateInvitation();
  const ttl = useCountdown();
  const issuedFor = useRef<number | null>(null);

  // 온보딩에서 넘어올 땐 라우트 파라미터로 받고, 그 밖(앱 재시작 후 등)에서는
  // 서버가 알려주는 값을 쓴다. 둘 다 없으면 아직 조회 중이다.
  const serverFamilyId = useFamilyId().data;
  const paramFamilyId = Number(params.familyId);
  const familyId =
    Number.isInteger(paramFamilyId) && paramFamilyId > 0 ? paramFamilyId : (serverFamilyId ?? null);

  const issue = useCallback(
    (id: number) => {
      setError('');
      invitation.mutate(id, {
        onSuccess: (res) => {
          const seconds = (new Date(res.expiresAt).getTime() - Date.now()) / 1000;
          ttl.start(seconds);
        },
        onError: (e) => setError(invitationErrorMessage(e)),
      });
    },
    [invitation, ttl],
  );

  // 화면 진입 시 코드 1회 발급 (familyId 확보된 뒤)
  useEffect(() => {
    if (familyId != null && issuedFor.current !== familyId) {
      issuedFor.current = familyId;
      issue(familyId);
    }
  }, [familyId, issue]);

  const code = invitation.data?.code ?? '';
  const expired = !!invitation.data && !ttl.running;

  const share = () => {
    if (!code) return;
    void Share.share({
      message: `도토리 가족 초대 코드예요.\n앱에서 회원가입 후 이 코드를 입력하면 가족이 연결돼요.\n\n초대 코드: ${code}`,
    });
  };

  return (
    <OnboardingStepLayout
      step={3}
      compactHeader
      headerTitle="아이 초대하기"
      eyebrow="아이 초대"
      title="아이에게 코드를 보내주세요"
      subtitle="아이가 회원가입한 뒤 이 코드를 입력하면 가족이 연결돼요. 코드는 10분간 유효해요."
      footer={<StepFooter primaryLabel="완료" onPrimary={() => router.replace('/')} />}
    >
      <View style={{ gap: theme.space.lg }}>
        <View
          style={[
            styles.codeBox,
            {
              borderColor: expired ? theme.color.border.default : theme.color.brand.solid,
              borderRadius: theme.radius.lg,
              backgroundColor: theme.color.background.surface,
              opacity: expired ? 0.5 : 1,
            },
          ]}
        >
          {invitation.isPending && !code ? (
            <Text style={[theme.typography.h2, { color: theme.color.text.muted }]}>
              · · · · · ·
            </Text>
          ) : (
            <Text style={[styles.code, { color: theme.color.text.strong }]}>
              {code.split('').join(' ')}
            </Text>
          )}
          <View style={styles.timerRow}>
            <Ionicons name="time-outline" size={14} color={theme.color.text.muted} />
            <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
              {expired ? '만료됨' : `남은 시간 ${ttl.label}`}
            </Text>
          </View>
        </View>

        <View style={{ flexDirection: 'row', gap: theme.space.sm }}>
          <Button
            label="공유하기"
            variant="primary"
            onPress={share}
            disabled={!code || expired}
            trailingIcon={
              <Ionicons name="share-outline" size={16} color={theme.color.text.onBrand} />
            }
            style={styles.flex}
          />
          <Button
            label="코드 재발급"
            variant="ghost"
            onPress={() => familyId != null && issue(familyId)}
            loading={invitation.isPending}
            style={styles.flex}
          />
        </View>

        {familyId == null ? (
          <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
            가족 정보를 찾을 수 없어요. 홈에서 다시 시도해주세요.
          </Text>
        ) : error ? (
          <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
            {error}
          </Text>
        ) : (
          <Pressable onPress={() => router.replace('/')} hitSlop={6}>
            <Text
              style={[theme.typography.bodySm, styles.later, { color: theme.color.text.muted }]}
            >
              나중에 초대하기
            </Text>
          </Pressable>
        )}
      </View>
    </OnboardingStepLayout>
  );
}

const styles = StyleSheet.create({
  codeBox: {
    borderWidth: 1.5,
    paddingVertical: 24,
    alignItems: 'center',
    gap: 10,
  },
  code: {
    fontSize: 34,
    fontWeight: '700',
    letterSpacing: 4,
  },
  timerRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  flex: { flex: 1 },
  later: { textAlign: 'center', textDecorationLine: 'underline' },
});
