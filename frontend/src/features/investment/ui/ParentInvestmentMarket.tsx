import { ScrollView, Text, View } from 'react-native';

import { useInvestmentProducts } from '@/features/investment/model/useInvestmentProducts';
import { InvestmentQueryError } from '@/features/investment/ui/InvestmentQueryError';
import { StockCard } from '@/features/investment/ui/StockCard';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type Props = {
  onBack: () => void;
  onPressStock: (productId: number) => void;
};

/** 주식 시세 (부모). 아이가 보는 종목 목록과 같은 가격이다. 가격은 하루에 한 번(자정 직후) 바뀐다. */
export function ParentInvestmentMarket({ onBack, onPressStock }: Props) {
  const theme = useTheme();
  const products = useInvestmentProducts();
  const muted = [theme.typography.bodySm, { color: theme.color.text.sub }];

  return (
    <Screen
      header={<Header title="주식 시세" onBack={onBack} />}
      // 탭 화면이라 하단 안전영역은 탭바가 이미 확보해준다 — 여기서 또 넣으면 탭바 위에 빈 틈이 생긴다.
      edges={[]}
      padded={false}
    >
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingHorizontal: theme.layout.screenX,
          paddingTop: theme.layout.screenY,
          // 탭바가 스크롤 영역 아래를 덮으므로 마지막 항목이 가려지지 않게 하단 여백을 넉넉히 둔다
          paddingBottom: theme.space['4xl'],
          gap: theme.space.lg,
        }}
      >
        <Text style={muted}>
          가격은 하루에 한 번, 자정 직후에 바뀌어요. 어제보다 오른 만큼(내린 만큼)도 보여줘요.
        </Text>
        {products.isPending ? (
          <Text style={muted}>주식을 불러오는 중이에요.</Text>
        ) : products.isError ? (
          <InvestmentQueryError
            error={products.error}
            fetching={products.isFetching}
            onRetry={() => products.refetch()}
          />
        ) : (
          <View style={{ gap: theme.space.md }}>
            {products.data.map((stock) => (
              <StockCard key={stock.id} stock={stock} onPress={() => onPressStock(stock.id)} />
            ))}
          </View>
        )}
      </ScrollView>
    </Screen>
  );
}
