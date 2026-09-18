import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, View } from 'react-native';

import { usePendingApprovals } from '@/features/chore';
import { useCashoutRequests } from '@/features/wallet';
import { useTheme } from '@/shared/theme';
import { Callout } from '@/shared/ui';

type ApprovalsCardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·구성원 조회가 아직 끝나지 않았다 */
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
 * 홈은 요약만 맡고 실제 처리(사진 확인·승인·거절)는 들어간 목록 화면이 한다.
 *
 * 처리할 것이 없으면 아무것도 그리지 않는다. 예전에는 빈 상태를 예시 배너로 채웠는데,
 * 요청이 없는데도 "환전을 요청했어요"가 떠 있으면 부모가 들어가 보고 빈 목록을 만난다 —
 * 알림이 거짓말을 하는 셈이라 배너 자체를 믿을 수 없게 된다.
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

  const choreCount = chores.data?.length ?? 0;
  const cashoutCount = cashouts.data?.length ?? 0;

  if (isPending || choreCount + cashoutCount === 0) return null;

  const showCashout = cashoutCount > 0;
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
        action={<Ionicons name="chevron-forward" size={18} color={theme.color.text.muted} />}
      />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  squirrel: { width: 28, height: 28 },
});
