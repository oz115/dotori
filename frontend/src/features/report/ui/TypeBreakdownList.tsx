import { StyleSheet, Text, View } from 'react-native';

import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';

export type BreakdownRow = {
  key: string;
  label: string;
  amount: number;
  count: number;
};

type Props = {
  title: string;
  total: number;
  rows: BreakdownRow[];
  /** 막대 색. 수입·소비 섹션이 각자의 색을 넘긴다 */
  barColor: string;
  isPending: boolean;
  isError: boolean;
  /** 거래가 하나도 없을 때 보여 줄 문구 */
  emptyText: string;
};

/**
 * 유형별 집계를 막대 리스트로 보여 준다. 이름·막대·금액이 한 줄이다.
 *
 * <p><b>모든 막대가 같은 색이다.</b> 금액이 큰 순서로 진하게 칠하면 막대 길이를 색으로
 * 한 번 더 인코딩하는 셈이고, 충전·환전처럼 순서가 없는 분류에 값 램프를 씌우는 것은
 * 그 자체가 잘못된 표현이다. 크기는 길이가 이미 말한다.
 *
 * <p>파이·도넛을 쓰지 않은 이유는 수입 유형이 여덟 개까지 나오기 때문이다. 조각이
 * 여섯을 넘으면 서로 구분되지 않고, 막대 리스트는 그 자체로 표 역할까지 한다.
 *
 * <p>정렬은 하지 않는다 — 서버가 금액 내림차순으로 준다.
 */
export function TypeBreakdownList({
  title,
  total,
  rows,
  barColor,
  isPending,
  isError,
  emptyText,
}: Props) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={styles.headerRow}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>{title}</Text>
        {!isPending && !isError && rows.length > 0 ? (
          <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
            {formatPoint(total)}
          </Text>
        ) : null}
      </View>

      {isPending ? (
        <Notice text="불러오는 중이에요" />
      ) : isError ? (
        <Notice text="잠시 후 다시 확인해 주세요" />
      ) : rows.length === 0 ? (
        <Notice text={emptyText} />
      ) : (
        <View style={{ gap: theme.space.sm }}>
          {rows.map((row) => (
            <BreakdownBar key={row.key} row={row} max={rows[0].amount} barColor={barColor} />
          ))}
        </View>
      )}
    </View>
  );
}

/**
 * 한 줄. 막대 길이는 <b>가장 큰 항목 기준</b>이라 1등이 항상 꽉 찬다.
 *
 * <p>총액 기준으로 재면 한 유형이 8할을 차지할 때 나머지가 전부 보이지 않는 선이 된다.
 * 비중은 금액 숫자가 알려 주므로, 막대는 서로 비교하는 데 쓴다.
 */
function BreakdownBar({
  row,
  max,
  barColor,
}: {
  row: BreakdownRow;
  max: number;
  barColor: string;
}) {
  const theme = useTheme();
  const ratio = max > 0 ? row.amount / max : 0;

  return (
    <View style={[styles.row, { gap: theme.space.sm }]}>
      <Text
        style={[theme.typography.caption, styles.label, { color: theme.color.text.sub }]}
        numberOfLines={1}
      >
        {row.label}
      </Text>

      <View style={styles.track}>
        {/* 0 이 아니면 최소 2px 은 그린다 — 28원이 아예 안 보이면 "없음"으로 읽힌다 */}
        <View
          style={[
            styles.bar,
            {
              width: `${Math.max(ratio * 100, row.amount > 0 ? 2 : 0)}%`,
              backgroundColor: barColor,
            },
          ]}
        />
      </View>

      <Text
        style={[theme.typography.caption, styles.amount, { color: theme.color.text.strong }]}
        numberOfLines={1}
      >
        {row.amount.toLocaleString('ko-KR')}
      </Text>
    </View>
  );
}

function Notice({ text }: { text: string }) {
  const theme = useTheme();

  return <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>{text}</Text>;
}

const styles = StyleSheet.create({
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  row: { flexDirection: 'row', alignItems: 'center' },
  // 이름 칸을 고정 폭으로 둬서 막대 시작선이 줄마다 흔들리지 않게 한다
  label: { width: 78 },
  track: { flex: 1, height: 10, justifyContent: 'center' },
  bar: { height: 10, borderRadius: 4 },
  amount: { width: 62, textAlign: 'right' },
});
