import { useRef, useState } from 'react';
import {
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
} from 'react-native';

import type { InvestmentNews } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

type Props = {
  items: InvestmentNews[];
  /** 카드 탭 — 뉴스 본문 페이지로 넘어가는 데 쓴다 */
  onPressItem?: (news: InvestmentNews) => void;
  /**
   * 캐러셀에 손가락이 닿아 있는 동안 true 로 알린다. 부모 화면이 이 동안
   * 당겨서 새로고침을 꺼 두면, 가로 스와이프에 섞인 세로 움직임이 새로고침을
   * 잘못 당기는 일이 없어진다.
   */
  onTouchingChange?: (touching: boolean) => void;
};

/**
 * 오늘의 뉴스 가로 캐러셀. 뉴스 카드 한 장이 화면 폭을 꽉 채우고 스냅으로 넘기며,
 * 아래 점 인디케이터가 현재 위치를 보여준다. 카드를 탭하면 onPressItem 으로 알린다.
 */
export function NewsCarousel({ items, onPressItem, onTouchingChange }: Props) {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const [index, setIndex] = useState(0);
  const listRef = useRef<FlatList<InvestmentNews>>(null);

  // Screen 좌우 여백을 뺀 카드 폭. 카드 사이 간격만큼 더해 스냅 간격을 잡는다.
  const cardWidth = width - theme.layout.screenX * 2;
  const gap = theme.space.md;
  const interval = cardWidth + gap;

  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const next = Math.round(e.nativeEvent.contentOffset.x / interval);
    if (next !== index && next >= 0 && next < items.length) {
      setIndex(next);
    }
  };

  return (
    <View style={{ gap: theme.space.md }}>
      <FlatList
        ref={listRef}
        data={items}
        keyExtractor={(item) => String(item.id)}
        horizontal
        showsHorizontalScrollIndicator={false}
        snapToInterval={interval}
        decelerationRate="fast"
        onScroll={onScroll}
        scrollEventThrottle={16}
        onTouchStart={() => onTouchingChange?.(true)}
        onTouchEnd={() => onTouchingChange?.(false)}
        onTouchCancel={() => onTouchingChange?.(false)}
        ItemSeparatorComponent={() => <View style={{ width: gap }} />}
        renderItem={({ item }) => (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${item.title.replace('\n', ' ')} 자세히 보기`}
            onPress={() => onPressItem?.(item)}
            style={({ pressed }) => [
              {
                width: cardWidth,
                backgroundColor: theme.color.background.surfaceSunken,
                borderRadius: theme.radius.xl,
                padding: theme.space.xl,
                gap: theme.space.sm,
                opacity: pressed ? 0.85 : 1,
              },
            ]}
          >
            <View style={styles.cardBody}>
              <View style={[styles.texts, { gap: theme.space.sm }]}>
                <View
                  style={[
                    styles.badge,
                    {
                      backgroundColor: theme.color.brand.solid,
                      borderRadius: theme.radius.full,
                    },
                  ]}
                >
                  <Text style={[theme.typography.caption, { color: theme.color.text.onBrand }]}>
                    {item.tag}
                  </Text>
                </View>
                <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
                  {item.title}
                </Text>
              </View>
              {/* Figma 일러스트 자리 표시자 — 에셋 export 후 expo-image 로 교체 */}
              <Text style={styles.illustration}>{item.emoji}</Text>
            </View>
            <Text
              style={[theme.typography.caption, styles.date, { color: theme.color.text.muted }]}
            >
              {item.date}
            </Text>
          </Pressable>
        )}
      />

      <View style={[styles.dots, { gap: theme.space.sm }]}>
        {items.map((item, i) => (
          <View
            key={item.id}
            style={[
              styles.dot,
              {
                backgroundColor: i === index ? theme.color.brand.solid : theme.color.border.default,
              },
            ]}
          />
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  cardBody: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  texts: {
    flex: 1,
    alignItems: 'flex-start',
  },
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 3,
  },
  illustration: {
    fontSize: 64,
    marginLeft: 8,
  },
  date: {
    alignSelf: 'flex-end',
  },
  dots: {
    flexDirection: 'row',
    justifyContent: 'center',
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
});
