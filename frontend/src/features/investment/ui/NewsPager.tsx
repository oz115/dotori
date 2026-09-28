import { useRef, useState } from 'react';
import {
  FlatList,
  ScrollView,
  StyleSheet,
  useWindowDimensions,
  View,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
} from 'react-native';

import type { InvestmentNews } from '@/features/investment/model/types';
import { useInvestmentNews } from '@/features/investment/model/useInvestmentNews';
import { NewsArticle } from '@/features/investment/ui/NewsArticle';
import { useTheme } from '@/shared/theme';
import { usePullToRefresh } from '@/shared/ui';

type Props = {
  /** 처음 열 뉴스. 목록에서 이 뉴스의 자리를 찾아 그 페이지부터 보여준다 */
  newsId: number;
};

/**
 * 뉴스 본문 페이저 — 좌우로 밀어 오늘의 다른 기사로 넘어간다.
 *
 * <p>목록에서 캐러셀로 넘겨 보다가 하나를 열면 거기서 막혀서, 다음 기사를 보려면 뒤로 갔다가
 * 다시 들어와야 했다 (QA). 목록과 같은 순서로 옆에 늘어놓고 밀어서 넘기게 한다.
 *
 * <p>순서는 오늘의 뉴스 목록(useInvestmentNews)을 그대로 쓴다 — 목록 캐러셀에서 보던 순서와
 * 같아야 "옆으로 넘긴다" 는 감각이 이어진다. 목록을 아직 못 받았으면(푸시로 곧장 들어온 경우
 * 등) 열려던 기사 한 편만 보여준다.
 */
export function NewsPager({ newsId }: Props) {
  const theme = useTheme();
  const { width } = useWindowDimensions();
  const refreshControl = usePullToRefresh();
  const news = useInvestmentNews();
  const listRef = useRef<FlatList<InvestmentNews>>(null);

  const items = news.data ?? [];
  const startIndex = Math.max(
    0,
    items.findIndex((item) => item.id === newsId),
  );
  const [index, setIndex] = useState(startIndex);

  const page = (content: React.ReactNode) => (
    <ScrollView
      style={{ width }}
      refreshControl={refreshControl}
      contentContainerStyle={{
        padding: theme.layout.screenX,
        paddingBottom: theme.space['4xl'],
      }}
    >
      {content}
    </ScrollView>
  );

  // 목록이 아직 없으면 넘길 곳도 없다 — 열려던 기사만 그대로 보여준다.
  if (items.length === 0) {
    return page(<NewsArticle newsId={newsId} />);
  }

  const onMomentumScrollEnd = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    const next = Math.round(e.nativeEvent.contentOffset.x / width);
    if (next !== index && next >= 0 && next < items.length) {
      setIndex(next);
    }
  };

  return (
    <View style={styles.flex}>
      <FlatList
        ref={listRef}
        data={items}
        keyExtractor={(item) => String(item.id)}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        initialScrollIndex={startIndex}
        // 페이지 폭이 화면 폭으로 고정이라 미리 알려준다 — initialScrollIndex 가 동작하려면 필요하다.
        getItemLayout={(_, i) => ({ length: width, offset: width * i, index: i })}
        onMomentumScrollEnd={onMomentumScrollEnd}
        renderItem={({ item }) => page(<NewsArticle newsId={item.id} />)}
      />

      {items.length > 1 ? (
        <View style={[styles.dots, { gap: theme.space.sm, paddingBottom: theme.space.lg }]}>
          {items.map((item, i) => (
            <View
              key={item.id}
              style={[
                styles.dot,
                {
                  backgroundColor:
                    i === index ? theme.color.brand.solid : theme.color.border.default,
                },
              ]}
            />
          ))}
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  dots: { flexDirection: 'row', justifyContent: 'center' },
  dot: { width: 8, height: 8, borderRadius: 4 },
});
