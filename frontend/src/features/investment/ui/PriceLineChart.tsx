import { useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  type GestureResponderEvent,
  type LayoutChangeEvent,
} from 'react-native';
import { Circle, Defs, Line, LinearGradient, Polygon, Polyline, Stop, Svg } from 'react-native-svg';

import { axisTicks, scalePoints, scaleY, toPolylinePoints } from '@/features/investment/lib/chart';
import { formatNumber } from '@/features/investment/lib/format';
import type { PricePoint } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';

const CHART_HEIGHT = 180;
const EDGE_INSET = 6;

type Props = {
  history: PricePoint[];
};

/**
 * 상세 화면의 가격 추이 차트. y축 눈금·그리드·영역 그라디언트를 그리고, 그래프를
 * 누르면(끌면) 그 지점의 날짜·가격 배지가 뜬다 — 오늘 가격은 위의 현재가가 이미
 * 보여주므로 배지를 기본으로 띄우지 않는다.
 * 좌표 계산은 lib/chart 순수 함수가 담당하고 여기는 SVG 배치만 한다.
 * 차트 폭은 onLayout 으로 재서 기기 크기에 맞춘다.
 */
export function PriceLineChart({ history }: Props) {
  const theme = useTheme();
  const [chartWidth, setChartWidth] = useState(0);
  // y축 라벨 칸의 실제 폭. 고정 폭으로 잡으면 가격 자릿수가 적을 때 왼쪽에 빈 공간이 남아
  // 차트 왼쪽 여백만 넓어 보인다 — 가장 긴 눈금 글씨에 맞춰 잰다.
  const [yAxisWidth, setYAxisWidth] = useState(0);
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  if (history.length === 0) {
    return (
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
        아직 가격 기록을 모으고 있어요.
      </Text>
    );
  }

  const prices = history.map((point) => point.price);
  // 등락과 상관없이 늘 초록으로 고정한다(팀 결정). 상승·하락·보합을 색으로 나누지 않고,
  // 등락은 다른 화면의 부호·문구가 알려 준다.
  const lineColor = theme.color.status.success.solid;
  const ticks = axisTicks(Math.min(...prices), Math.max(...prices));
  const domainMin = ticks[0];
  const domainMax = ticks[ticks.length - 1];
  const points = scalePoints(prices, domainMin, domainMax, chartWidth, CHART_HEIGHT, EDGE_INSET);
  const last = points[points.length - 1];
  // 점이 하나(오늘 시작한 가족)면 오른쪽 끝에 점만 남아 그래프가 비어 보인다 —
  // 같은 값으로 왼쪽 끝까지 수평선을 그려 "오늘 이 가격에서 시작했다"가 한눈에 보이게 한다.
  const linePoints = points.length === 1 ? [{ x: 0, y: points[0].y }, points[0]] : points;

  // 새로고침으로 이력 길이가 줄면 선택이 범위를 벗어날 수 있다 — 그땐 없는 셈 친다.
  const selected = selectedIndex != null && selectedIndex < points.length ? selectedIndex : null;

  const onLayout = (event: LayoutChangeEvent) => {
    setChartWidth(Math.round(event.nativeEvent.layout.width));
  };

  // 누른 x 좌표에서 가장 가까운 데이터 점을 고른다. 점 사이를 눌러도 반올림으로 붙는다.
  const selectAt = (event: GestureResponderEvent) => {
    if (chartWidth === 0 || points.length === 0) {
      return;
    }
    if (points.length === 1) {
      setSelectedIndex(0);
      return;
    }
    const stepX = (chartWidth - EDGE_INSET * 2) / (points.length - 1);
    const index = Math.round((event.nativeEvent.locationX - EDGE_INSET) / stepX);
    setSelectedIndex(Math.min(points.length - 1, Math.max(0, index)));
  };

  return (
    <View style={{ gap: theme.space.xs }}>
      {/* 선택한 날의 가격 자리. 높이를 고정해 배지가 떠도 차트가 안 밀린다 */}
      <View style={styles.inspectRow}>
        {selected != null ? (
          <View
            style={[
              styles.selectedBadge,
              { backgroundColor: theme.color.text.strong, borderRadius: theme.radius.full },
            ]}
          >
            <Text style={[styles.axisLabel, { color: theme.color.text.onBrand }]}>
              {history[selected].label} · {formatNumber(prices[selected])} 도토리
            </Text>
          </View>
        ) : (
          <Text style={[styles.axisLabel, { color: theme.color.text.muted }]}>
            그래프를 누르면 그날의 가격을 볼 수 있어요.
          </Text>
        )}
      </View>

      <View style={styles.chartRow}>
        <View
          style={styles.yLabels}
          onLayout={(event) => setYAxisWidth(Math.round(event.nativeEvent.layout.width))}
        >
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

        <View
          style={styles.plot}
          onLayout={onLayout}
          testID="price-chart-plot"
          onStartShouldSetResponder={() => true}
          onMoveShouldSetResponder={() => true}
          onResponderGrant={selectAt}
          onResponderMove={selectAt}
        >
          {chartWidth > 0 && last ? (
            <Svg width={chartWidth} height={CHART_HEIGHT}>
              <Defs>
                <LinearGradient id="priceAreaFill" x1="0" y1="0" x2="0" y2="1">
                  <Stop offset="0" stopColor={lineColor} stopOpacity={0.28} />
                  <Stop offset="1" stopColor={lineColor} stopOpacity={0} />
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
              {linePoints.length >= 2 ? (
                <>
                  <Polygon
                    points={`${toPolylinePoints(linePoints)} ${last.x},${CHART_HEIGHT - EDGE_INSET} ${linePoints[0].x},${CHART_HEIGHT - EDGE_INSET}`}
                    fill="url(#priceAreaFill)"
                  />
                  <Polyline
                    points={toPolylinePoints(linePoints)}
                    fill="none"
                    stroke={lineColor}
                    strokeWidth={2.5}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </>
              ) : null}
              {/* 점은 사용자가 고른 지점에만 찍는다 — 오늘(마지막) 점을 기본으로 찍어두면
                  아무것도 안 골랐는데 뭔가 선택된 것처럼 보인다 */}
              {selected != null ? (
                <>
                  <Line
                    x1={points[selected].x}
                    y1={0}
                    x2={points[selected].x}
                    y2={CHART_HEIGHT}
                    stroke={theme.color.text.muted}
                    strokeWidth={1}
                    strokeDasharray="4 3"
                  />
                  <Circle
                    cx={points[selected].x}
                    cy={points[selected].y}
                    r={5}
                    fill={lineColor}
                    stroke={theme.color.background.surface}
                    strokeWidth={2}
                  />
                </>
              ) : null}
            </Svg>
          ) : null}
        </View>
      </View>

      <View style={[styles.xLabels, { marginLeft: yAxisWidth + AXIS_GAP }]}>
        {history
          .map((point, index) => ({ ...point, index }))
          // 오늘(마지막)을 기준으로 같은 간격씩 거슬러 라벨을 찍는다 — 앞에서부터 찍고
          // 마지막을 억지로 끼우면 끝의 두 라벨만 날짜 간격이 달라져 이상해 보인다
          .filter(
            (_, index) =>
              (history.length - 1 - index) % Math.max(1, Math.ceil((history.length - 1) / 4)) === 0,
          )
          .map((point) => (
            <Text
              key={point.index}
              style={[
                styles.axisLabel,
                styles.xLabel,
                { color: theme.color.text.muted },
                point.index === history.length - 1
                  ? { right: 0 }
                  : point.index === 0
                    ? { left: 0 }
                    : {
                        left: points[point.index]?.x ?? 0,
                        transform: [{ translateX: -16 }],
                      },
              ]}
            >
              {point.label}
            </Text>
          ))}
      </View>
    </View>
  );
}

const AXIS_GAP = 8;

const styles = StyleSheet.create({
  chartRow: {
    flexDirection: 'row',
    gap: AXIS_GAP,
  },
  // 폭을 고정하지 않는다 — 가장 긴 눈금 글씨만큼만 차지해 왼쪽 여백이 남지 않는다
  yLabels: {
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
  inspectRow: {
    minHeight: 26,
    alignItems: 'center',
    justifyContent: 'center',
  },
  selectedBadge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  xLabel: { position: 'absolute', textAlign: 'center' },
  // 왼쪽 여백(y축 라벨 폭 + 간격)은 실측값이라 렌더에서 marginLeft 로 넣는다
  xLabels: {
    height: 20,
  },
});
