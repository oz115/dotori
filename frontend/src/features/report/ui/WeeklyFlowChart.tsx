import { useState } from 'react';
import { StyleSheet, Text, View, type LayoutChangeEvent } from 'react-native';
import { Line, Rect, Svg } from 'react-native-svg';

import { layoutFlowChart } from '@/features/report/lib/flowChart';
import type { WeeklyFlow } from '@/features/report/lib/weeklyFlow';
import { useTheme } from '@/shared/theme';

/**
 * 수입·소비 차트에만 쓰는 색.
 *
 * <p>앱은 증감에 초록·빨강을 쓰지만 <b>차트에서는 못 쓴다.</b> 적색약에서 두 색의
 * 거리가 ΔE 2.6 으로 사실상 같은 색이고, 정상 시력에서는 32.9 로 멀쩡해 보여서 눈으로는
 * 잡히지 않는 문제다. 파랑↔빨강은 24.3 으로 통과한다(대비·명도·채도 검사도 함께 통과).
 *
 * <p>숫자 하나에 부호가 붙는 자리(자산 타일의 손익 등)는 색이 유일한 단서가 아니라
 * 기존 초록·빨강을 그대로 쓴다. 여기만 다른 이유가 그것이다.
 */
export const INCOME_CHART_COLOR = '#2F80ED';
export const SPENDING_CHART_COLOR = '#E5484D';

/** 기준선 위·아래로 각각 쓸 높이 */
const HALF_HEIGHT = 56;

/** 막대 끝 둥글기. 기준선 쪽은 각지게 두고 바깥쪽만 둥글린다 */
const BAR_RADIUS = 4;

type Props = {
  flow: WeeklyFlow;
};

/**
 * 한 주의 수입·소비를 기준선 위아래로 그린다.
 *
 * <p>선 그래프를 쓰지 않은 이유가 있다. 서버는 거래가 없는 날의 행을 보내지 않으므로,
 * 선으로 이으면 없는 추세를 빈 구간에 그려 넣는다. 막대는 없으면 없는 대로 정직하다.
 *
 * <p>차트 폭은 `onLayout` 으로 재서 기기 크기에 맞춘다 (투자 차트와 같은 방식).
 */
export function WeeklyFlowChart({ flow }: Props) {
  const theme = useTheme();
  const [chartWidth, setChartWidth] = useState(0);

  const { bars, baselineY, height } = layoutFlowChart(
    flow.days,
    flow.peak,
    chartWidth,
    HALF_HEIGHT,
  );

  const onLayout = (event: LayoutChangeEvent) => {
    setChartWidth(Math.round(event.nativeEvent.layout.width));
  };

  return (
    <View style={{ gap: theme.space.xs }}>
      <View style={[styles.legend, { gap: theme.space.md }]}>
        <LegendItem color={INCOME_CHART_COLOR} label="수입" />
        <LegendItem color={SPENDING_CHART_COLOR} label="소비" />
      </View>

      <View onLayout={onLayout}>
        <Svg width="100%" height={height}>
          {/* 기준선은 hairline 으로 recessive 하게. 데이터보다 눈에 띄면 안 된다 */}
          <Line
            x1={0}
            y1={baselineY}
            x2={chartWidth}
            y2={baselineY}
            stroke={theme.color.border.default}
            strokeWidth={1}
          />

          {bars.map((bar) => (
            <Rect
              key={`in-${bar.date}`}
              x={bar.x}
              y={baselineY - bar.incomeHeight}
              width={bar.width}
              height={bar.incomeHeight}
              rx={BAR_RADIUS}
              fill={INCOME_CHART_COLOR}
            />
          ))}

          {bars.map((bar) => (
            <Rect
              key={`out-${bar.date}`}
              x={bar.x}
              y={baselineY}
              width={bar.width}
              height={bar.spendingHeight}
              rx={BAR_RADIUS}
              fill={SPENDING_CHART_COLOR}
            />
          ))}
        </Svg>
      </View>

      <View style={styles.axis}>
        {flow.days.map((day) => (
          <Text
            key={day.date}
            style={[
              theme.typography.caption,
              styles.axisLabel,
              { color: day.isFuture ? theme.color.text.muted : theme.color.text.sub },
            ]}
          >
            {day.label}
          </Text>
        ))}
      </View>
    </View>
  );
}

/** 계열이 둘이라 범례를 항상 둔다 — 색만으로 구분하게 두지 않는다 */
function LegendItem({ color, label }: { color: string; label: string }) {
  const theme = useTheme();

  return (
    <View style={[styles.legendItem, { gap: theme.space.xs }]}>
      <View style={[styles.swatch, { backgroundColor: color }]} />
      <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  legend: { flexDirection: 'row', alignItems: 'center' },
  legendItem: { flexDirection: 'row', alignItems: 'center' },
  swatch: { width: 10, height: 10, borderRadius: 2 },
  axis: { flexDirection: 'row' },
  axisLabel: { flex: 1, textAlign: 'center' },
});
