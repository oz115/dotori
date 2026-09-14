import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, View } from 'react-native';

import { usePendingApprovals } from '@/features/chore';
import { mockCashoutRequests, useCashoutRequests } from '@/features/wallet';
import { useTheme } from '@/shared/theme';
import { Callout, MockBadge } from '@/shared/ui';

type ApprovalsCardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·구성원 조회가 아직 끝나지 않았다 — 이때는 예시로 넘어가지 않는다 */
  familyPending: boolean;
  /** 목록 화면(할 일 탭)으로 보낸다 */
  onOpenChores: () => void;
  onOpenCashouts: () => void;
};

/**
 * 부모 홈의 승인 대기 배너. 아이가 올린 집안일 완료 인증과 환전 요청은 부모가 승인해야
 * 다음으로 넘어가므로, 홈에서 바로 눈에 띄게 알려준다. 여러 건이 밀려 있어도 한 줄로
 * 가장 급한 것 하나만 보여주고(환전 → 집안일 순), 나머지는 눌러서 들어간 목록에서 본다.
 *
 * 홈은 요약만 맡고 실제 처리(사진 확인·승인·거절)는 탭 안 목록 화면이 한다.
 */
export function ApprovalsCard({
  familyId,
  childId,
  familyPending,
  onOpenChores,
  onOpenCashouts,
}: ApprovalsCardProps) {
  const theme = useTheme();
  const chores = usePendingApprovals(familyId, childId);
  const cashouts = useCashoutRequests(familyId, childId, 'PENDING');

  // 아이가 없으면 두 조회가 아예 돌지 않아 isPending 이 계속 true 라,
  // "아이 없음"과 "불러오는 중"을 따로 봐야 한다
  const isPending = familyPending || (childId != null && (chores.isPending || cashouts.isPending));

  // 대기 건이 하나도 없으면(아이 미연결·조회 실패 포함) 예시로 자리를 채운다 —
  // 그러지 않으면 이 배너가 부모 홈에서 가장 중요한데도 늘 안 보이기만 한다
  const usingMock = !isPending && (chores.data?.length ?? 0) + (cashouts.data?.length ?? 0) === 0;
  const cashoutCount = usingMock ? mockCashoutRequests.length : (cashouts.data?.length ?? 0);

  if (isPending) return null;

  const showCashout = cashoutCount > 0 || usingMock;
  const title = showCashout
    ? '자녀가 포인트 → 돈 환전을 요청했어요!'
    : '아이가 집안일 완료를 올렸어요. 확인해주세요!';
  const onPress = showCashout ? onOpenCashouts : onOpenChores;

  return (
    <Pressable onPress={onPress} accessibilityRole="button">
      <Callout
        icon={
          <View
            style={{
              width: 40,
              height: 40,
              borderRadius: theme.radius.md,
              backgroundColor: theme.color.brand.subtle,
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Image
              source={require('@/assets/images/invest/squirrel.png')}
              style={styles.squirrel}
              resizeMode="contain"
            />
          </View>
        }
        title={title}
        action={
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: theme.space.xs }}>
            {usingMock ? <MockBadge /> : null}
            <Ionicons name="chevron-forward" size={18} color={theme.color.text.muted} />
          </View>
        }
      />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  squirrel: { width: 28, height: 28 },
});
