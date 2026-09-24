import { Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';
import { Screen } from '@/shared/ui/Screen';

type ComingSoonScreenProps = {
  title: string;
};

/**
 * 탭은 있는데 화면이 아직 없는 자리(할 일·상점·저축 등)를 채우는 공통 placeholder.
 * 해당 화면 태스크가 시작되면 이 자리를 실제 화면으로 바꾼다.
 */
export function ComingSoonScreen({ title }: ComingSoonScreenProps) {
  const theme = useTheme();

  return (
    <Screen edges={['top']}>
      <View
        style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: theme.space.xs }}
      >
        <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>{title}</Text>
        <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
          곧 만나볼 수 있어요.
        </Text>
      </View>
    </Screen>
  );
}
