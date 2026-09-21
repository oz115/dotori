import { StyleSheet, Text, View } from 'react-native';

import type { AssetReport } from '@/features/report/model/types';
import { formatPoint } from '@/shared/lib/money';
import { useTheme } from '@/shared/theme';
import { ContentCard } from '@/shared/ui';

type Props = {
  isPending: boolean;
  isError: boolean;
  assets: AssetReport | undefined;
};

/**
 * 자산 현황 (RPT-04). 총액을 크게 한 번 보여 주고 그 아래 포인트·예적금·투자로 쪼갠다.
 *
 * <p>숫자가 넷뿐이라 차트를 쓰지 않는다. 막대 한 개짜리 그래프는 숫자보다 읽기 어렵다.
 *
 * <p><b>기간 파라미터가 없는 스냅샷</b>이라 주를 넘겨도 이 값은 그대로다. 위의 주 이동과
 * 무관하다는 것을 화면에 적어 두지 않으면 부모가 "지난 주 자산"으로 읽는다.
 */
export function AssetSnapshot({ isPending, isError, assets }: Props) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.md }}>
      <View style={styles.headerRow}>
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          자산 현황
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>지금 기준</Text>
      </View>

      <ContentCard style={{ padding: theme.space.lg, gap: theme.space.lg }}>
        {isPending ? (
          <Notice text="불러오는 중이에요" />
        ) : isError || assets == null ? (
          <Notice text="잠시 후 다시 확인해 주세요" />
        ) : (
          <>
            <Text
              style={[
                theme.typography.display,
                { color: theme.color.text.strong, textAlign: 'center' },
              ]}
            >
              {formatPoint(assets.totalAssets)}
            </Text>

            <View style={styles.tileRow}>
              <AssetTile label="포인트" value={assets.point.balance} />
              <AssetTile
                label="예적금"
                value={assets.savings.totalAmount}
                note={`${assets.savings.activeCount}계좌`}
              />
              <AssetTile
                label="투자"
                value={assets.investment.valuation}
                note={signedNote(assets.investment.profitLoss)}
                noteTone={investmentTone(assets.investment.profitLoss)}
              />
            </View>
          </>
        )}
      </ContentCard>
    </View>
  );
}

/** 평가 손익은 부호를 붙여 보여 준다. 0 이면 `±0` — 손익이 없는 것과 값이 없는 것은 다르다 */
function signedNote(profitLoss: number): string {
  if (profitLoss === 0) {
    return '±0';
  }
  return `${profitLoss > 0 ? '+' : '-'}${Math.abs(profitLoss).toLocaleString('ko-KR')}`;
}

type NoteTone = 'up' | 'down' | 'flat';

function investmentTone(profitLoss: number): NoteTone {
  if (profitLoss > 0) {
    return 'up';
  }
  return profitLoss < 0 ? 'down' : 'flat';
}

type AssetTileProps = {
  label: string;
  value: number;
  note?: string;
  noteTone?: NoteTone;
};

function AssetTile({ label, value, note, noteTone = 'flat' }: AssetTileProps) {
  const theme = useTheme();

  // 손익 표시에만 색을 쓴다. 숫자 하나에 부호가 같이 있어 색이 유일한 단서가 아니라,
  // 차트와 달리 초록·빨강을 그대로 써도 된다 (앱의 다른 화면과도 같은 관행이다).
  const noteColor =
    noteTone === 'up'
      ? theme.color.status.success.text
      : noteTone === 'down'
        ? theme.color.status.danger.text
        : theme.color.text.muted;

  return (
    <View style={[styles.tile, { gap: 2 }]}>
      <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>{label}</Text>
      <Text style={[theme.typography.label, { color: theme.color.text.strong }]} numberOfLines={1}>
        {value.toLocaleString('ko-KR')}
      </Text>
      {note != null ? (
        <Text style={[theme.typography.bodySm, { color: noteColor }]} numberOfLines={1}>
          {note}
        </Text>
      ) : null}
    </View>
  );
}

function Notice({ text }: { text: string }) {
  const theme = useTheme();

  return (
    <Text style={[theme.typography.bodySm, { color: theme.color.text.sub, textAlign: 'center' }]}>
      {text}
    </Text>
  );
}

const styles = StyleSheet.create({
  headerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  tileRow: { flexDirection: 'row' },
  // 세 칸을 똑같이 나눈다. 금액 길이가 달라도 칸 폭은 흔들리지 않아야 한다
  tile: { flex: 1, alignItems: 'center' },
});
