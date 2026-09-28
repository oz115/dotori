import { StyleSheet, Text, View } from 'react-native';

import type { StockDetail } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  detail: StockDetail;
};

/**
 * 상세 화면의 상품 정보 카드 (종류·설명).
 */
export function StockInfoCard({ detail }: Props) {
  const theme = useTheme();

  const rows = [
    { key: '종류', emoji: '🍃', value: detail.category },
    { key: '설명', emoji: '📝', value: detail.description },
  ];

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: theme.color.background.surfaceSunken,
          borderRadius: theme.radius.xl,
          padding: theme.space.xl,
          gap: theme.space.lg,
        },
      ]}
    >
      <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>상품 정보</Text>

      <View style={{ gap: theme.space.md }}>
        {rows.map((row, index) => (
          <View
            key={row.key}
            style={[
              styles.row,
              { gap: theme.space.md },
              index < rows.length - 1 && {
                borderBottomWidth: StyleSheet.hairlineWidth,
                borderBottomColor: theme.color.border.default,
                paddingBottom: theme.space.md,
              },
            ]}
          >
            {/* 항목 아이콘 자리 표시자 — Figma 에셋 export 후 이미지로 교체 */}
            <View
              style={[
                styles.rowIcon,
                {
                  backgroundColor: theme.color.background.surface,
                  borderRadius: theme.radius.full,
                },
              ]}
            >
              <Text style={[styles.rowEmoji, { fontSize: 18 * theme.textScale }]}>{row.emoji}</Text>
            </View>
            <Text style={[theme.typography.label, styles.rowKey, { color: theme.color.text.sub }]}>
              {row.key}
            </Text>
            <Text
              style={[
                theme.typography.bodySm,
                styles.rowValue,
                { color: theme.color.text.default },
              ]}
            >
              {row.value}
            </Text>
          </View>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {},
  row: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  rowIcon: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  // 크기는 쓰는 쪽에서 기기 글꼴 배율을 상쇄해 넣는다 (shared/theme/fontScale.ts)
  rowEmoji: {},
  rowKey: {
    width: 40,
  },
  rowValue: {
    flex: 1,
  },
});
