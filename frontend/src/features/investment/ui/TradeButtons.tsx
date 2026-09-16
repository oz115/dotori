import { Alert, StyleSheet, View } from 'react-native';

import { formatAcorns } from '@/features/investment/lib/format';
import { useBuyStock } from '@/features/investment/model/useBuyStock';
import { useSellStock } from '@/features/investment/model/useSellStock';
import type { OrderResponse, StockDetail } from '@/features/investment/model/types';
import { apiErrorCode, isNetworkError } from '@/shared/api';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

type Props = {
  detail: StockDetail;
};

/**
 * 상세 화면의 사기/팔기 버튼과 주문 플로우(확인 → 체결 → 결과 안내).
 * 종목당 1주 규칙이라 보유 중이면 사기가, 미보유면 팔기가 비활성화된다.
 * 처리 중에는 두 버튼을 모두 잠가 중복 주문을 막는다.
 */
export function TradeButtons({ detail }: Props) {
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
                Alert.alert(
                  '샀어요! 🌰',
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
                Alert.alert(
                  '팔았어요! 🌰',
                  `${detail.name} 1주를 ${formatAcorns(order.unitPrice)}에 팔았어요.\n` +
                    `도토리: ${formatAcorns(order.pointBalanceAfter)}`,
                );
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
        label="🌰 사기"
        variant="primary"
        style={styles.flex}
        disabled={detail.holding || busy}
        loading={buy.isPending}
        onPress={confirmBuy}
      />
      <Button
        label="🌰 팔기"
        variant="outline"
        style={styles.flex}
        disabled={!detail.holding || busy}
        loading={sell.isPending}
        onPress={confirmSell}
      />
    </View>
  );
}

/** 주문 실패를 아이 눈높이 문구로 바꾼다. 네트워크 단절은 결과를 모르는 상태라 따로 안내. */
function orderErrorMessage(error: unknown): string {
  if (isNetworkError(error)) {
    return '인터넷 연결을 확인하고 다시 시도해 주세요.';
  }
  switch (apiErrorCode(error)) {
    case 'INSUFFICIENT_POINT':
      return '도토리가 부족해요. 할 일을 하고 도토리를 모아 보세요!';
    case 'ALREADY_HOLDING':
      return '이미 가지고 있는 주식이에요. 한 회사는 1주만 살 수 있어요.';
    case 'PRODUCT_INACTIVE':
      return '지금은 거래가 잠시 멈춘 회사예요.';
    case 'INSUFFICIENT_QUANTITY':
      return '가지고 있지 않은 주식이에요.';
    default:
      return '잠시 후 다시 시도해 주세요.';
  }
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
  },
  flex: {
    flex: 1,
  },
});
