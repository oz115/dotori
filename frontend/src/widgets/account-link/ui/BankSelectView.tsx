import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { BankBadge, BANKS } from '@/features/account';
import { useTheme } from '@/shared/theme';
import { Button, Header, Screen } from '@/shared/ui';

type BankSelectViewProps = {
  /** 현재 선택된 은행 key (lib/banks 의 Bank.key) */
  selected: string | null;
  /** 은행을 고를 때 */
  onSelect: (bankKey: string) => void;
  /** '선택하기' 확정 — 어느 스토어에 담을지는 호출하는 쪽(퍼널)이 결정한다 */
  onConfirm: () => void;
  /** 선택을 끝내고(또는 취소하고) 이전 화면으로 */
  onClose: () => void;
};

/**
 * 은행 선택 (계좌 연동·이체 공용). 은행 그리드만 그리고, 선택 상태는 호출하는 쪽
 * 스토어(연동은 `useLinkAccountStore`, 이체는 `useTransferStore`)가 소유한다.
 */
export function BankSelectView({ selected, onSelect, onConfirm, onClose }: BankSelectViewProps) {
  const theme = useTheme();

  return (
    <Screen scroll header={<Header title="은행 선택" onBack={onClose} />}>
      <View style={[styles.grid, { marginTop: theme.space.lg }]}>
        {BANKS.map((bank) => {
          const on = selected === bank.key;
          return (
            <Pressable
              key={bank.key}
              onPress={() => onSelect(bank.key)}
              accessibilityRole="radio"
              accessibilityState={{ selected: on }}
              style={[
                styles.card,
                {
                  borderRadius: theme.radius.lg,
                  borderColor: on ? theme.color.brand.solid : theme.color.border.default,
                  backgroundColor: on ? theme.color.brand.tint : theme.color.background.surface,
                },
              ]}
            >
              <BankBadge bank={bank} size={32} />
              <Text style={[theme.typography.body, { color: theme.color.text.strong }]}>
                {bank.shortName}
              </Text>
              {on ? (
                <View style={styles.check}>
                  <Ionicons name="checkmark-circle" size={20} color={theme.color.brand.solid} />
                </View>
              ) : null}
            </Pressable>
          );
        })}
      </View>

      <View style={styles.spacer} />

      <Button label="은행 선택하기" variant="primary" onPress={onConfirm} disabled={!selected} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  card: {
    width: '47%',
    flexGrow: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    borderWidth: 1.5,
    paddingVertical: 18,
    paddingHorizontal: 16,
  },
  check: { position: 'absolute', top: -8, right: -8 },
  spacer: { flex: 1, minHeight: 24 },
});
