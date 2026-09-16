import { Platform, StyleSheet, Switch, Text, View } from 'react-native';

import { usePushNotificationSetting } from '@/features/notification/model/usePushNotificationSetting';
import { useTheme } from '@/shared/theme';

/**
 * 마이페이지 "푸시 알림" 토글. iOS는 FCM 연동이 없어 범위 밖이라 아예 숨긴다.
 */
export function PushNotificationToggle() {
  const theme = useTheme();
  const { enabled, isLoading, isToggling, toggle } = usePushNotificationSetting();

  if (Platform.OS !== 'android') return null;

  return (
    <View style={[styles.row, { paddingVertical: theme.space.xl }]}>
      <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>푸시 알림</Text>
      <Switch
        value={enabled}
        onValueChange={toggle}
        disabled={isLoading || isToggling}
        trackColor={{ false: theme.color.background.surfaceSunken, true: theme.color.brand.solid }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
});
