import { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import { signedPoint } from '@/features/report/lib/format';
import { incomeTypeLabel, spendingTypeLabel } from '@/features/report/lib/reasonLabels';
import { currentWeek, shiftWeek } from '@/features/report/lib/reportPeriod';
import { buildWeeklyFlow, netFlow } from '@/features/report/lib/weeklyFlow';
import { useAssetReport } from '@/features/report/model/useAssetReport';
import { useIncomeReport } from '@/features/report/model/useIncomeReport';
import { useSpendingReport } from '@/features/report/model/useSpendingReport';
import { AssetSnapshot } from '@/features/report/ui/AssetSnapshot';
import { TypeBreakdownList, type BreakdownRow } from '@/features/report/ui/TypeBreakdownList';
import {
  INCOME_CHART_COLOR,
  SPENDING_CHART_COLOR,
  WeeklyFlowChart,
} from '@/features/report/ui/WeeklyFlowChart';
import { WeekStepper } from '@/features/report/ui/WeekStepper';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type ReportScreenProps = {
  familyId: number | undefined;
  childId: number | undefined;
  childName: string | undefined;
  /** 가족·자녀 조회가 끝나기 전 — 아직 조회에 쓸 id 가 없다 */
  familyPending: boolean;
  onBack: () => void;
};

/**
 * 자녀 리포트 화면 (RPT-02·03·04).
 *
 * <p>기간은 <b>월요일부터 일요일까지 한 주</b>다. 한 달을 일 단위로 그리면 막대가 31개,
 * 폰 폭에서 하나에 4px 이라 읽을 수 없다. 7칸이면 x축이 요일로 고정되고 하나에 40px 쯤 된다.
 *
 * <p>자산 현황만 기간과 무관한 스냅샷이다. 주를 넘겨도 그 값은 바뀌지 않는다.
 */
export function ReportScreen({
  familyId,
  childId,
  childName,
  familyPending,
  onBack,
}: ReportScreenProps) {
  const theme = useTheme();
  const [period, setPeriod] = useState(currentWeek);

  const income = useIncomeReport(familyId, childId, period);
  const spending = useSpendingReport(familyId, childId, period);
  const assets = useAssetReport(familyId, childId);

  // 흐름 차트는 두 응답을 합쳐야 하나의 축이 된다. 한쪽만 와 있으면 절반만 그려진
  // 차트가 보이므로 둘 다 기다린다.
  const flowPending = familyPending || income.isPending || spending.isPending;
  const flowError = income.isError || spending.isError;

  const flow = useMemo(
    () => buildWeeklyFlow(period, income.data?.byPeriod, spending.data?.byPeriod),
    [period, income.data?.byPeriod, spending.data?.byPeriod],
  );

  const incomeRows: BreakdownRow[] = (income.data?.byType ?? []).map((row) => ({
    key: row.incomeType,
    label: incomeTypeLabel(row.incomeType),
    amount: row.amount,
    count: row.count,
  }));

  const spendingRows: BreakdownRow[] = (spending.data?.byType ?? []).map((row) => ({
    key: row.spendingType,
    label: spendingTypeLabel(row.spendingType),
    amount: row.amount,
    count: row.count,
  }));

  return (
    <Screen
      header={
        <Header title={childName != null ? `${childName} 리포트` : '리포트'} onBack={onBack} />
      }
    >
      <ScrollView
        contentContainerStyle={[styles.content, { gap: theme.space['2xl'] }]}
        showsVerticalScrollIndicator={false}
      >
        <WeekStepper period={period} onChange={(weeks) => setPeriod(shiftWeek(period, weeks))} />

        <AssetSnapshot
          isPending={familyPending || assets.isPending}
          isError={assets.isError}
          assets={assets.data}
        />

        <View style={{ gap: theme.space.md }}>
          <View style={styles.headerRow}>
            <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
              수입과 소비
            </Text>
            {!flowPending && !flowError && income.data != null && spending.data != null ? (
              <NetFlowLabel value={netFlow(income.data.total, spending.data.total)} />
            ) : null}
          </View>

          {flowPending ? (
            <Notice text="불러오는 중이에요" />
          ) : flowError ? (
            <Notice text="잠시 후 다시 확인해 주세요" />
          ) : flow.peak === 0 ? (
            <Notice text="이 주에는 포인트가 움직이지 않았어요" />
          ) : (
            <WeeklyFlowChart flow={flow} />
          )}
        </View>

        <TypeBreakdownList
          title="수입"
          total={income.data?.total ?? 0}
          rows={incomeRows}
          barColor={INCOME_CHART_COLOR}
          isPending={familyPending || income.isPending}
          isError={income.isError}
          emptyText="이 주에는 받은 포인트가 없어요"
        />

        <TypeBreakdownList
          title="지출"
          total={spending.data?.total ?? 0}
          rows={spendingRows}
          barColor={SPENDING_CHART_COLOR}
          isPending={familyPending || spending.isPending}
          isError={spending.isError}
          emptyText="이 주에는 쓴 포인트가 없어요"
        />
      </ScrollView>
    </Screen>
  );
}

/**
 * 순유입. 숫자에 부호가 붙어 색이 유일한 단서가 아니라, 여기는 앱의 초록·빨강 관행을
 * 그대로 쓴다 — 차트 안(파랑·빨강)과 다른 이유가 그것이다.
 */
function NetFlowLabel({ value }: { value: number }) {
  const theme = useTheme();

  return (
    <Text
      style={[
        theme.typography.label,
        {
          color: value >= 0 ? theme.color.status.success.text : theme.color.status.danger.text,
        },
      ]}
    >
      순유입 {signedPoint(value)}
    </Text>
  );
}

function Notice({ text }: { text: string }) {
  const theme = useTheme();

  return <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>{text}</Text>;
}

const styles = StyleSheet.create({
  content: { paddingBottom: 32 },
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
});
