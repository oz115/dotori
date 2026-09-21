import { useState, type ReactNode } from 'react';
import {
  ScrollView,
  StyleSheet,
  View,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
} from 'react-native';

import { useTheme } from '@/shared/theme';

/** 장과 장 사이 간격. 넘기는 도중에만 보이고, 멈추면 이만큼 화면 밖으로 빠진다 */
const PAGE_GAP = 16;

type PagedCardsProps = {
  /** 옆으로 넘겨 보는 장들. 각 장은 `flex: 1` 로 높이를 채워야 카드 높이가 맞는다 */
  pages: ReactNode[];
  testID?: string;
};

/**
 * 홈 맨 위 카드를 옆으로 넘기며 보는 슬라이드 (자녀 지갑·부모 지갑 공용).
 *
 * 장마다 내용 길이가 달라서 그냥 두면 넘길 때마다 아래 섹션이 위아래로 출렁인다.
 * 가로 ScrollView 는 자식을 가장 큰 높이에 맞춰 늘리므로, 각 장을 `flex: 1` 로 두어
 * 높이를 고정한다. 아래에는 몇 번째 장인지 알려주는 점이 붙는다.
 */
export function PagedCards({ pages, testID }: PagedCardsProps) {
  const theme = useTheme();
  const [page, setPage] = useState(0);

  // 한 장의 폭은 화면 폭에서 계산하지 않고 실제로 그려진 폭을 잰다 —
  // 홈 화면의 좌우 여백이 바뀌면 계산값이 어긋나 다음 장이 옆에 걸쳐 보인다.
  const [pageWidth, setPageWidth] = useState(0);

  // 넘기는 동안 두 장이 맞붙어 보이지 않게 사이를 띄우고, 그만큼 더 스크롤해서 멈춘다
  const interval = pageWidth + PAGE_GAP;

  const onScrollEnd = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    setPage(Math.round(e.nativeEvent.contentOffset.x / interval));
  };

  return (
    <View style={{ gap: theme.space.md }}>
      <ScrollView
        testID={testID}
        horizontal
        showsHorizontalScrollIndicator={false}
        onLayout={(e) => setPageWidth(e.nativeEvent.layout.width)}
        snapToInterval={interval}
        snapToAlignment="start"
        decelerationRate="fast"
        disableIntervalMomentum
        onMomentumScrollEnd={onScrollEnd}
        // 카드 그림자가 ScrollView 경계 밖으로 번져 잘리는 걸 막는다
        contentContainerStyle={{ gap: PAGE_GAP, paddingTop: 4, paddingBottom: 8 }}
      >
        {/* 폭을 재기 전(첫 프레임)에 그리면 카드가 폭 0 으로 찌그러진 채 한 번 보인다 */}
        {pageWidth > 0
          ? pages.map((content, i) => (
              <View key={i} style={{ width: pageWidth }}>
                {content}
              </View>
            ))
          : null}
      </ScrollView>

      <PageDots count={pages.length} active={page} />
    </View>
  );
}

/** 몇 번째 장인지 알려주는 점. 현재 장만 길쭉한 알약이 된다. */
function PageDots({ count, active }: { count: number; active: number }) {
  const theme = useTheme();

  return (
    <View style={[styles.dots, { gap: theme.space.sm }]}>
      {Array.from({ length: count }, (_, i) => (
        <View
          key={i}
          style={[
            styles.dot,
            {
              width: i === active ? 22 : 8,
              borderRadius: theme.radius.full,
              backgroundColor: i === active ? theme.color.text.muted : theme.color.border.default,
            },
          ]}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  dots: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  dot: { height: 8 },
});
