import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { BankBadge, BANKS, type AccountInfo } from '@/features/account';
import { formatWonNumber } from '@/shared/lib/money';
import { bankBrand, fontFamily, useTheme } from '@/shared/theme';

type AccountCardProps = {
  account: AccountInfo | undefined;
  isPending: boolean;
  /** 계좌가 없을 때 연동 화면으로 */
  onLinkAccount: () => void;
  /** 아직 API가 없는 기능 — 준비 중 안내만 띄운다 */
  onTransfer: () => void;
  onHistory: () => void;
};

/**
 * 부모 본인 계좌 카드. 연동된 은행의 브랜드 컬러(`bankBrand`)로 배경을 칠해 한눈에
 * 어느 은행인지 알아보게 한다. 아직 연동 전이면 은행이 없으니 도토리 브라운으로
 * 안내 카드를 채운다 — 위 자녀 카드와 구분되면서도 "내 계좌" 자리라는 건 알아보게.
 *
 * 이 계좌는 충전(자녀→부모)의 수취 계좌이자 환전 승인(부모→자녀)의 출금 계좌라서,
 * 연동돼 있지 않으면 아이의 충전·환전이 서버에서 막힌다. 그래서 없을 때를 눈에 띄게 안내한다.
 */
export function AccountCard({
  account,
  isPending,
  onLinkAccount,
  onTransfer,
  onHistory,
}: AccountCardProps) {
  const theme = useTheme();
  const bank = BANKS.find((b) => b.code === account?.bankCode);
  const cardColor = bank ? bankBrand[bank.key].color : theme.color.accent.acorn;
  const textColor = bank ? bankBrand[bank.key].textOnBrand : theme.color.text.onBrand;

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: cardColor,
          borderRadius: theme.radius.lg,
          padding: theme.space.xl,
          gap: theme.space.md,
        },
      ]}
    >
      <View style={styles.row}>
        <View style={{ flex: 1, gap: theme.space.xs }}>
          {account ? (
            <Text style={[theme.typography.caption, { color: textColor, opacity: 0.85 }]}>
              {account.bankName}
              {'\n'}
              {account.accountName}
            </Text>
          ) : (
            <Text style={[theme.typography.caption, { color: textColor, opacity: 0.85 }]}>
              내 계좌
            </Text>
          )}

          {isPending ? (
            <Text style={[theme.typography.display, { color: textColor }]}>—</Text>
          ) : account ? (
            <>
              {/* 사용 가능한 가장 굵은 폰트 웨이트가 Bold라 extra bold 대신 이걸 쓴다 */}
              <Text
                style={[
                  theme.typography.caption,
                  { color: textColor, opacity: 0.85, fontFamily: fontFamily.bold },
                ]}
              >
                {account.accountNo}
              </Text>
              <Text style={[theme.typography.display, { color: textColor }]}>
                {formatWonNumber(account.accountBalance)}원
              </Text>
            </>
          ) : (
            <Text style={[theme.typography.body, { color: textColor }]}>
              아직 연동된 계좌가 없어요
            </Text>
          )}
        </View>

        {account ? (
          bank ? (
            <View
              style={[
                styles.bankBadgeBg,
                {
                  backgroundColor: theme.color.background.surface,
                  borderRadius: theme.radius.full,
                },
              ]}
            >
              <BankBadge bank={bank} size={30} />
            </View>
          ) : (
            <Ionicons name="wallet-outline" size={28} color={textColor} />
          )
        ) : (
          <Pressable
            onPress={onLinkAccount}
            style={[
              styles.linkButton,
              {
                backgroundColor: theme.color.background.surface,
                borderRadius: theme.radius.sm,
                paddingHorizontal: theme.space.md,
              },
            ]}
          >
            <Text style={[theme.typography.label, { color: theme.color.accent.acorn }]}>
              계좌 연동
            </Text>
          </Pressable>
        )}
      </View>

      {!isPending && !account ? (
        <Text style={[theme.typography.caption, { color: textColor, opacity: 0.85 }]}>
          계좌를 연결해야 아이가 도토리를 충전하고 환전할 수 있어요.
        </Text>
      ) : null}

      {account ? (
        <View style={[styles.actionRow, { gap: theme.space.sm }]}>
          <AccountPill label="이체하기" color={cardColor} onPress={onTransfer} />
          <AccountPill label="거래내역 조회" color={cardColor} onPress={onHistory} />
        </View>
      ) : null}
    </View>
  );
}

type AccountPillProps = {
  label: string;
  color: string;
  onPress: () => void;
};

function AccountPill({ label, color, onPress }: AccountPillProps) {
  const theme = useTheme();

  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.pill,
        {
          backgroundColor: theme.color.background.surface,
          borderColor: color,
          borderRadius: theme.radius.lg,
          paddingHorizontal: theme.space.md,
        },
      ]}
    >
      <Text style={[theme.typography.label, { color }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {},
  row: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  linkButton: { minHeight: 40, alignItems: 'center', justifyContent: 'center' },
  // 은행 로고가 카드 배경과 같은 계열 색이면 묻혀 보여서 흰 원판을 깔아 띄운다
  bankBadgeBg: { width: 48, height: 48, alignItems: 'center', justifyContent: 'center' },
  actionRow: { flexDirection: 'row' },
  pill: {
    flex: 1,
    minHeight: 46,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
  },
});
