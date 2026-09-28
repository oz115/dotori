import { useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { formatAcorns } from '@/features/investment/lib/format';
import { orderErrorMessage, sellOutcomeMessage } from '@/features/investment/lib/orderMessages';
import { useBuyStock } from '@/features/investment/model/useBuyStock';
import { useSellStock } from '@/features/investment/model/useSellStock';
import type { OrderResponse } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

/** 선택 모드에서 고른 종목 하나 — 가격은 지금 화면에 보이는 현재가다 */
export type TradeSelectionItem = {
  id: number;
  name: string;
  price: number;
};

type Props = {
  mode: 'buy' | 'sell';
  items: TradeSelectionItem[];
  /** 취소했거나 거래가 끝나 선택 모드를 닫을 때 */
  onClose: () => void;
};

/**
 * 카드를 꾹 눌러 여러 종목을 고른 뒤 화면 아래에 뜨는 일괄 사기/팔기 바.
 * 주문 API 가 종목당 한 건이라 고른 순서대로 하나씩 체결하고, 일부가 실패해도
 * 나머지는 계속 진행한 뒤 결과를 한 번에 알려준다.
 */
export function TradeSelectionBar({ mode, items, onClose }: Props) {
  const theme = useTheme();
  const buy = useBuyStock();
  const sell = useSellStock();
  const [running, setRunning] = useState(false);

  const buying = mode === 'buy';
  const total = items.reduce((sum, item) => sum + item.price, 0);
  const actionLabel = buying ? '사기' : '팔기';

  const runTrade = async () => {
    const mutateAsync = buying ? buy.mutateAsync : sell.mutateAsync;
    const results: OrderResponse[] = [];
    const failures: { name: string; message: string }[] = [];

    setRunning(true);
    try {
      // 금융 요청이라 동시에 쏘지 않고 하나씩 — 잔액 차감 순서가 뒤엉키지 않는다
      for (const item of items) {
        try {
          results.push(await mutateAsync(item.id));
        } catch (error) {
          failures.push({ name: item.name, message: orderErrorMessage(error) });
        }
      }
    } finally {
      setRunning(false);
    }

    const failureLines = failures.map((f) => `${f.name}: ${f.message}`).join('\n');
    if (results.length === 0) {
      // 전부 실패 — 선택을 그대로 두어 아이가 바로 다시 시도할 수 있게 한다
      Alert.alert(buying ? '사지 못했어요' : '팔지 못했어요', failureLines);
      return;
    }

    if (buying) {
      const last = results[results.length - 1];
      Alert.alert(
        '샀어요!',
        `주식 ${results.length}개를 샀어요.\n남은 도토리: ${formatAcorns(last.pointBalanceAfter)}` +
          (failureLines ? `\n\n못 산 주식\n${failureLines}` : ''),
      );
    } else {
      const profits = results
        .map((order) => order.realizedProfit)
        .filter((profit): profit is number => profit != null);
      const outcome =
        profits.length === results.length
          ? sellOutcomeMessage(profits.reduce((sum, profit) => sum + profit, 0))
          : '판 돈이 지갑으로 들어왔어요.';
      Alert.alert(
        '팔았어요!',
        `주식 ${results.length}개를 팔았어요.\n${outcome}` +
          (failureLines ? `\n\n못 판 주식\n${failureLines}` : ''),
      );
    }
    onClose();
  };

  const confirm = () => {
    const names = items.map((item) => item.name).join(', ');
    Alert.alert(
      buying ? '주식 사기' : '주식 팔기',
      `${names}\n${items.length}개를 총 ${formatAcorns(total)}에 ${buying ? '살까요?' : '팔까요?'}`,
      [
        { text: '아니요', style: 'cancel' },
        { text: buying ? '살래요' : '팔래요', onPress: () => void runTrade() },
      ],
    );
  };

  return (
    <View
      style={[
        styles.bar,
        theme.shadow.card,
        {
          backgroundColor: theme.color.background.surface,
          borderColor: theme.color.border.card,
          paddingHorizontal: theme.layout.screenX,
          paddingVertical: theme.space.md,
          gap: theme.space.md,
        },
      ]}
    >
      <View style={styles.summary}>
        <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
          {items.length}개 골랐어요
        </Text>
        <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>
          총 {formatAcorns(total)}
        </Text>
      </View>
      <Button label="취소" variant="outline" disabled={running} onPress={onClose} />
      <Button
        label={actionLabel}
        variant="primary"
        style={styles.action}
        disabled={items.length === 0 || running}
        loading={running}
        onPress={confirm}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    borderTopWidth: 1,
  },
  summary: {
    flex: 1,
    gap: 2,
  },
  action: {
    minWidth: 96,
  },
});
