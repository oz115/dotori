import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { SavingsGroup } from '@/features/savings/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  value: SavingsGroup;
  onChange: (group: SavingsGroup) => void;
};

const GROUPS: { key: SavingsGroup; label: string }[] = [
  { key: 'DEPOSIT', label: '예금' },
  { key: 'INSTALLMENT', label: '적금' },
];

/**
 * 예금 | 적금 세그먼트. 상점 카드처럼 뚜렷한 테두리 + 그림자로 경계를 살린다.
 *
 * 아이 저축 탭(SavingsHome)과 부모 저축 탭(ParentSavingsHome)이 같이 쓴다 - 부모와 아이가
 * 같은 상품을 이야기할 때 고르는 자리가 달라 보이면 안 된다.
 */
export function SavingsGroupSegment({ value, onChange }: Props) {
  const theme = useTheme();

  return (
    <View
      style={[
        styles.segment,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderColor: theme.color.border.default,
          borderRadius: theme.radius.full,
        },
      ]}
    >
      {GROUPS.map(({ key, label }) => {
        const active = key === value;
        return (
          <Pressable
            key={key}
            accessibilityRole="button"
            accessibilityState={{ selected: active }}
            onPress={() => onChange(key)}
            style={[
              styles.segmentItem,
              active && {
                backgroundColor: theme.color.brand.solid,
                borderRadius: theme.radius.full,
              },
            ]}
          >
            <Text
              style={[
                theme.typography.label,
                { color: active ? theme.color.text.onBrand : theme.color.text.sub },
              ]}
            >
              {label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export const savingsGroupLabel = (group: SavingsGroup) => (group === 'DEPOSIT' ? '예금' : '적금');

const styles = StyleSheet.create({
  segment: {
    flexDirection: 'row',
    borderWidth: 1,
    padding: 4,
  },
  segmentItem: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 8,
  },
});
