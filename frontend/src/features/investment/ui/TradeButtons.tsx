import { Alert, StyleSheet, View } from 'react-native';

import { formatAcorns } from '@/features/investment/lib/format';
import { orderErrorMessage, sellOutcomeMessage } from '@/features/investment/lib/orderMessages';
import { useBuyStock } from '@/features/investment/model/useBuyStock';
import { useSellStock } from '@/features/investment/model/useSellStock';
import type { OrderResponse, StockDetail } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

type Props = {
  detail: StockDetail;
  onTradeSuccess?: () => void;
};

/**
 * 상세 화면의 사기/팔기 버튼과 주문 플로우(확인 → 체결 → 결과 안내).
 * 종목당 1주 규칙이라 보유 중이면 사기가, 미보유면 팔기가 비활성화된다.
 * 처리 중에는 두 버튼을 모두 잠가 중복 주문을 막는다.
 */
export function TradeButtons({ detail, onTradeSuccess }: Props) {
  const theme = useTheme();
  const buy = useBuyStock();
  const sell = useSellStock();
  const busy = buy.isPending || sell.isPending;

  const confirmBuy = () => {
    Alert.alert(
      '주식 사기',
      `${detail.name} 1주를 ${formatAcorns(detail.currentPrice)}에 살까요?`,
      [
        { text: '아니요', style: 'cancel' },
        {
          text: '살래요',
          onPress: () =>
            buy.mutate(detail.id, {
              onSuccess: (order: OrderResponse) => {
                onTradeSuccess?.();
                Alert.alert(
                  '샀어요!',
                  `${detail.name} 1주를 ${formatAcorns(order.unitPrice)}에 샀어요.\n` +
                    `남은 도토리: ${formatAcorns(order.pointBalanceAfter)}`,
                );
              },
              onError: (error: unknown) => {
                Alert.alert('살 수 없어요', orderErrorMessage(error));
              },
            }),
        },
      ],
    );
  };

  const confirmSell = () => {
    Alert.alert(
      '주식 팔기',
      `${detail.name} 1주를 지금 가격 ${formatAcorns(detail.currentPrice)}에 팔까요?`,
      [
        { text: '아니요', style: 'cancel' },
        {
          text: '팔래요',
          onPress: () =>
            sell.mutate(detail.id, {
              onSuccess: (order: OrderResponse) => {
                onTradeSuccess?.();
                const buyPrice = order.buyPrice ?? detail.buyPrice;
                const profit =
                  order.realizedProfit ??
                  (buyPrice == null ? undefined : (order.unitPrice - buyPrice) * order.quantity);
                // 결과는 번/잃은 도토리 한 줄만 — 산 값·판 값·잔액까지 늘어놓으면 못 읽는다
                Alert.alert('팔았어요!', sellOutcomeMessage(profit));
              },
              onError: (error: unknown) => {
                Alert.alert('팔 수 없어요', orderErrorMessage(error));
              },
            }),
        },
      ],
    );
  };

  return (
    <View style={[styles.row, { gap: theme.space.md }]}>
      <Button
        label="사기"
        variant="primary"
        style={styles.flex}
        disabled={detail.holding || busy}
        loading={buy.isPending}
        onPress={confirmBuy}
      />
      <Button
        label="팔기"
        variant="outline"
        style={styles.flex}
        disabled={!detail.holding || busy}
        loading={sell.isPending}
        onPress={confirmSell}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
  },
  flex: {
    flex: 1,
  },
});
