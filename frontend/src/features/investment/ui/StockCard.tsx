import { Ionicons } from '@expo/vector-icons';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { changeColor, changeLabel } from '@/features/investment/lib/change';
import { formatAcorns, signedAcorns } from '@/features/investment/lib/format';
import type { StockSummary } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

const ACORN = require('@/assets/images/acorn.png'); // 734 x 806

type Props = {
  stock: StockSummary;
  onPress?: () => void;
  /** 꾹 누르기 — 여러 종목을 골라 한 번에 사는 선택 모드로 들어간다 */
  onLongPress?: () => void;
  /** 선택 모드면 오른쪽에 체크 표시가 나온다 (탭 동작은 부모가 onPress 로 바꿔 끼운다) */
  selectionMode?: boolean;
  selected?: boolean;
  /** 이미 가지고 있는 종목. 예적금 가입 카드와 같은 초록 테두리로 표시한다 */
  held?: boolean;
};

/**
 * 주식 리스트의 종목 한 줄 카드 — 아이콘·이름과 현재가·전일 대비 등락.
 * 업종은 상세 화면에서만 보여준다 (목록에선 이름 가독성이 우선이라는 팀 결정).
 * 카드를 꾹 누르면 선택 모드가 되어 여러 종목을 한 번에 살 수 있다.
 */
export function StockCard({ stock, onPress, onLongPress, selectionMode, selected, held }: Props) {
  const theme = useTheme();

  const color = changeColor(stock.change, theme.color);

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${stock.name} 상세 보기`}
      onPress={onPress}
      onLongPress={onLongPress}
      delayLongPress={300}
      style={({ pressed }) => [
        styles.card,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderRadius: theme.radius.xl,
          // 고른 것 > 가진 것 순으로 테두리를 정한다 — 고르는 중에는 그게 더 급한 정보다
          borderColor: selected || held ? theme.color.brand.solid : theme.color.border.card,
          borderWidth: selected || held ? 2 : 1,
          padding: theme.space.lg,
          gap: theme.space.md,
          opacity: pressed ? 0.85 : 1,
        },
      ]}
    >
      {/* 아이콘 원본이 흰 배경이라 배경색 없이 둥근 모서리로만 자른다 */}
      <Image
        source={stock.image}
        style={[styles.icon, { borderRadius: theme.radius.md }]}
        resizeMode="cover"
      />

      <Text
        numberOfLines={1}
        style={[theme.typography.label, styles.name, { color: theme.color.text.strong }]}
      >
        {stock.name}
      </Text>

      <View style={[styles.priceBlock, { gap: theme.space.xs }]}>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
          {formatAcorns(stock.currentPrice)}
        </Text>
        <View style={styles.change} accessibilityLabel={changeLabel(stock.change)}>
          <Image source={ACORN} style={styles.acorn} resizeMode="contain" />
          <Text style={[theme.typography.label, { color }]}>
            {stock.change === 0 ? '변동 없음' : signedAcorns(stock.change)}
          </Text>
        </View>
      </View>

      {selectionMode ? (
        <Ionicons
          name={selected ? 'checkmark-circle' : 'ellipse-outline'}
          size={24}
          color={selected ? theme.color.brand.solid : theme.color.text.muted}
        />
      ) : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  // 상점 상품 카드와 같은 문법 — 뚜렷한 1px 테두리 + 옅은 그림자로 카드 경계를 살린다
  card: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  icon: {
    width: 44,
    height: 44,
  },
  // 이름이 남는 공간을 전부 가져가고, 넘치면 말줄임 — 가격 블록에 밀리지 않게 한다
  name: {
    flex: 1,
  },
  priceBlock: {
    alignItems: 'flex-end',
  },
  change: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  acorn: {
    width: 14,
    height: 15,
  },
});
