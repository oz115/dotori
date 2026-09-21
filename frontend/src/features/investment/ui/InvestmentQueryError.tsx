import type { AxiosError } from 'axios';
import { Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

/** 재시도 중 표시와, 재시도로 해결되지 않는 권한·미지원 상태를 구분한다. */
export function InvestmentQueryError({
  error,
  fetching,
  onRetry,
}: {
  error: unknown;
  fetching: boolean;
  onRetry: () => void;
}) {
  const theme = useTheme();
  const status = (error as AxiosError)?.response?.status;
  const blocked = status === 401 || status === 403 || status === 404;
  const message =
    status === 401
      ? '로그인이 만료됐어요. 다시 로그인해 주세요.'
      : status === 403
        ? '이 정보를 볼 수 없어요. 가족 연결 상태를 확인해 주세요.'
        : status === 404
          ? '요청한 정보를 찾을 수 없어요.'
          : '정보를 불러오지 못했어요. 연결을 확인한 뒤 다시 시도해 주세요.';
  return (
    <View style={{ gap: theme.space.sm }}>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
        {fetching ? '다시 불러오는 중이에요.' : message}
      </Text>
      {!blocked ? (
        <Button
          label="다시 불러오기"
          size="sm"
          variant="outline"
          loading={fetching}
          onPress={onRetry}
        />
      ) : null}
    </View>
  );
}
