import { Image, StyleSheet } from 'react-native';

import { mockChildBalance, useWalletBalance } from '@/features/wallet';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { WalletButton, WalletCard } from '@/shared/ui';

type ChildBalanceCardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  childName: string | undefined;
  /** 가족·구성원 조회가 아직 끝나지 않았다 — 이때는 예시로 넘어가지 않는다 */
  familyPending: boolean;
  /** 아이에게 도토리를 주는 화면으로 */
  onGrant: () => void;
  /** 아이가 보낸 환전 요청을 보고 승인·거절하는 화면으로 */
  onReviewCashouts: () => void;
};

/**
 * 부모 홈 첫 장. 부모가 가장 먼저 알고 싶은 건 자기 돈이 아니라 "우리 아이가 지금 얼마를 갖고 있나" 다.
 * 옆으로 넘기면 부모 본인 계좌(AccountCard)가 나온다.
 *
 * 모양은 다른 지갑 카드와 같은 틀(WalletCard)이다. 계좌 카드는 흰 바탕에 은행 색을 포인트로 쓰고,
 * 이 카드는 그 색을 뒤집어 도토리 브라운(accent.acorn) 바탕에 흰색을 포인트로 쓴다 —
 * 초록인 자녀 지갑과 구분되면서 옆 장과 짝이 맞는다.
 */
export function ChildBalanceCard({
  familyId,
  childId,
  childName,
  familyPending,
  onGrant,
  onReviewCashouts,
}: ChildBalanceCardProps) {
  const theme = useTheme();
  const balance = useWalletBalance(familyId, childId);

  const hasChild = childId != null;
  const loading = familyPending || (hasChild && balance.isPending);

  // 아이를 아직 연결하지 않았거나 지갑 조회가 실패하면 예시 금액으로 자리를 채운다.
  // 잔액 0 은 "아직 안 모았다"는 실제 정보라 예시로 덮지 않는다.
  const usingMock = !loading && balance.data == null;
  const amount = usingMock ? mockChildBalance : (balance.data?.balance ?? 0);

  return (
    <WalletCard
      fill
      inverted
      accent={theme.color.accent.acorn}
      icon={
        <Image
          source={require('@/assets/images/icons/icon-dotori.png')}
          style={styles.acornIcon}
          resizeMode="contain"
        />
      }
      title={usingMock ? '자녀 보유 도토리 (예시)' : '자녀 보유 도토리'}
      subtitle={hasChild ? (childName ?? '우리 아이') : undefined}
      amount={loading ? '—' : formatPoint(amount)}
      note={
        !loading && !hasChild
          ? '아직 연결된 아이가 없어요. 가족 관리에서 초대 코드를 받아 알려주세요.'
          : undefined
      }
    >
      <WalletButton label="도토리 주기" onPress={onGrant} disabled={!hasChild} />
      <WalletButton label="환전 요청 확인" onPress={onReviewCashouts} tone="soft" />
    </WalletCard>
  );
}

const styles = StyleSheet.create({
  acornIcon: { width: 28, height: 28 },
});
