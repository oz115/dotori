import { Ionicons } from '@expo/vector-icons';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { BankBadge, BANKS, useLinkAccountStore } from '@/features/account';
import { useTheme } from '@/shared/theme';
import { Button, Header, Screen } from '@/shared/ui';

type BankSelectViewProps = {
  /** 선택을 끝내고(또는 취소하고) 이전 화면으로 */
  onClose: () => void;
};

/** 은행 선택 (부모·자녀 공용). 고른 은행은 연동 스토어에 담고 이전 화면으로 돌아간다. */
export function BankSelectView({ onClose }: BankSelectViewProps) {
  const theme = useTheme();
  const patch = useLinkAccountStore((s) => s.patch);
  const [selected, setSelected] = useState<string | null>(
    () => useLinkAccountStore.getState().bankKey,
  );

  const confirm = () => {
    if (!selected) return;
    patch({ bankKey: selected });
    onClose();
  };

  return (
    <Screen scroll header={<Header title="은행 선택" onBack={onClose} />}>
      <View style={[styles.grid, { marginTop: theme.space.lg }]}>
        {BANKS.map((bank) => {
          const on = selected === bank.key;
          return (
            <Pressable
              key={bank.key}
              onPress={() => setSelected(bank.key)}
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

      <Button label="은행 선택하기" variant="primary" onPress={confirm} disabled={!selected} />
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
