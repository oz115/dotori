import { useState } from 'react';
import { StyleSheet, Text, View, type LayoutChangeEvent } from 'react-native';
import { Circle, Defs, Line, LinearGradient, Polygon, Polyline, Stop, Svg } from 'react-native-svg';

import { axisTicks, scalePoints, scaleY, toPolylinePoints } from '@/features/investment/lib/chart';
import { formatNumber } from '@/features/investment/lib/format';
import type { PricePoint } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

const CHART_HEIGHT = 180;
const EDGE_INSET = 6;
const Y_LABEL_WIDTH = 48;

type Props = {
  history: PricePoint[];
};

/**
 * 상세 화면의 가격 추이 차트. y축 눈금·그리드·영역 그라디언트·마지막 값 배지를 그린다.
 * 좌표 계산은 lib/chart 순수 함수가 담당하고 여기는 SVG 배치만 한다.
 * 차트 폭은 onLayout 으로 재서 기기 크기에 맞춘다.
 */
export function PriceLineChart({ history }: Props) {
  const theme = useTheme();
  const [chartWidth, setChartWidth] = useState(0);

  const prices = history.map((point) => point.price);
  const ticks = axisTicks(Math.min(...prices), Math.max(...prices));
  const domainMin = ticks[0];
  const domainMax = ticks[ticks.length - 1];
  const points = scalePoints(prices, domainMin, domainMax, chartWidth, CHART_HEIGHT, EDGE_INSET);
  const last = points[points.length - 1];

  const onLayout = (event: LayoutChangeEvent) => {
    setChartWidth(Math.round(event.nativeEvent.layout.width));
  };

  return (
    <View style={{ gap: theme.space.xs }}>
      <View style={styles.chartRow}>
        <View style={styles.yLabels}>
          {[...ticks].reverse().map((tick) => (
            <Text
              key={tick}
              numberOfLines={1}
              style={[styles.axisLabel, { color: theme.color.text.muted }]}
            >
              {formatNumber(tick)}
            </Text>
          ))}
        </View>

        <View style={styles.plot} onLayout={onLayout}>
          {chartWidth > 0 && last ? (
            <>
              <Svg width={chartWidth} height={CHART_HEIGHT}>
                <Defs>
                  <LinearGradient id="priceAreaFill" x1="0" y1="0" x2="0" y2="1">
                    <Stop offset="0" stopColor={theme.color.brand.solid} stopOpacity={0.28} />
                    <Stop offset="1" stopColor={theme.color.brand.solid} stopOpacity={0} />
                  </LinearGradient>
                </Defs>
                {ticks.map((tick) => {
                  const y = scaleY(tick, domainMin, domainMax, CHART_HEIGHT, EDGE_INSET);
                  return (
                    <Line
                      key={tick}
                      x1={0}
                      y1={y}
                      x2={chartWidth}
                      y2={y}
                      stroke={theme.color.border.default}
                      strokeWidth={StyleSheet.hairlineWidth}
                    />
                  );
                })}
                <Polygon
                  points={`${toPolylinePoints(points)} ${last.x},${CHART_HEIGHT - EDGE_INSET} ${points[0].x},${CHART_HEIGHT - EDGE_INSET}`}
                  fill="url(#priceAreaFill)"
                />
                <Polyline
                  points={toPolylinePoints(points)}
                  fill="none"
                  stroke={theme.color.brand.solid}
                  strokeWidth={2.5}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <Circle
                  cx={last.x}
                  cy={last.y}
                  r={4.5}
                  fill={theme.color.brand.solidActive}
                  stroke={theme.color.background.surface}
                  strokeWidth={2}
                />
              </Svg>
              {/* 마지막 값 배지 — 점 위에 겹쳐 올린다 (Figma: 진한 말풍선) */}
              <View
                style={[
                  styles.badge,
                  {
                    backgroundColor: theme.color.text.strong,
                    borderRadius: theme.radius.full,
                    top: Math.max(0, last.y - 34),
                  },
                ]}
              >
                <Text style={[styles.axisLabel, { color: theme.color.text.onBrand }]}>
                  {formatNumber(prices[prices.length - 1])}
                </Text>
              </View>
            </>
          ) : null}
        </View>
      </View>

      <View style={styles.xLabels}>
        {history.map((point) => (
          <Text key={point.label} style={[styles.axisLabel, { color: theme.color.text.muted }]}>
            {point.label}
          </Text>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  chartRow: {
    flexDirection: 'row',
    gap: 8,
  },
  yLabels: {
    width: Y_LABEL_WIDTH,
    height: CHART_HEIGHT,
    justifyContent: 'space-between',
    alignItems: 'flex-end',
  },
  // 축·배지 전용 작은 글씨. caption(16px)은 44px 축 폭에 안 들어가 줄바꿈·클리핑됐다 —
  // 차트 눈금은 본문보다 작아야 그래프가 주인공이 된다.
  axisLabel: {
    fontSize: 12,
    lineHeight: 16,
  },
  plot: {
    flex: 1,
  },
  badge: {
    position: 'absolute',
    right: 0,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  xLabels: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginLeft: Y_LABEL_WIDTH + 8,
  },
});
