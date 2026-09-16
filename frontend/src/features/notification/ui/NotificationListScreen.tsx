import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo } from 'react';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { useMarkAllAsRead } from '@/features/notification/model/useMarkAllAsRead';
import { useMarkAsRead } from '@/features/notification/model/useMarkAsRead';
import { useNotifications } from '@/features/notification/model/useNotifications';
import type { NotificationItem } from '@/features/notification/model/types';
import { NotificationRow } from '@/features/notification/ui/NotificationRow';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

type NotificationListScreenProps = {
  /**
   * 알림 탭 시 이동. (child)/(parent) 둘 다 갖고 있는 라우트로 갈 수 있어서, 이 화면이
   * 직접 push하지 않고 어느 그룹인지 아는 라우트가 실제 경로를 만들어 콜백으로 넘긴다.
   * 갈 곳이 없는 타입(아직 트리거가 없는 타입 등)이면 라우트 쪽에서 아무 일도 안 한다.
   */
  onPressNotification: (item: NotificationItem) => void;
};

/** 알림함 (NOTI-01/03/04/11). 부모·자녀 공용. */
export function NotificationListScreen({ onPressNotification }: NotificationListScreenProps) {
  const theme = useTheme();
  const { data, isPending, isError, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useNotifications();
  const markAsRead = useMarkAsRead();
  const markAllAsRead = useMarkAllAsRead();

  const items = useMemo(() => data?.pages.flatMap((page) => page.notifications) ?? [], [data]);
  // unreadCount 는 필터와 무관하게 항상 전체 미읽 개수라, 어느 페이지에서 읽어도 같다.
  const unreadCount = data?.pages[0]?.unreadCount ?? 0;

  const handlePress = (item: NotificationItem) => {
    if (!item.isRead) {
      markAsRead.mutate(item.notificationId);
    }
    onPressNotification(item);
  };

  return (
    <Screen header={<Header title="알림" onBack={() => router.back()} />} padded={false}>
      <FlatList
        data={items}
        keyExtractor={(item) => String(item.notificationId)}
        renderItem={({ item }) => <NotificationRow item={item} onPress={() => handlePress(item)} />}
        onEndReached={() => {
          if (hasNextPage && !isFetchingNextPage) {
            fetchNextPage();
          }
        }}
        onEndReachedThreshold={0.4}
        contentContainerStyle={[
          styles.content,
          { paddingHorizontal: theme.layout.screenX, gap: theme.space.sm },
        ]}
        ListHeaderComponent={
          <View style={{ gap: theme.space.xs, marginBottom: theme.space.lg }}>
            <View style={styles.titleRow}>
              <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>
                읽지 않은 알림 {unreadCount}개
              </Text>
              {unreadCount > 0 ? (
                <Pressable
                  accessibilityRole="button"
                  onPress={() => markAllAsRead.mutate()}
                  hitSlop={8}
                  style={styles.markAllButton}
                >
                  <Ionicons name="checkmark" size={16} color={theme.color.brand.solid} />
                  <Text style={[theme.typography.label, { color: theme.color.brand.solid }]}>
                    모두 읽음
                  </Text>
                </Pressable>
              ) : null}
            </View>
            <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
              도토리와 함께하는 특별한 소식을 확인해 보세요!
            </Text>
          </View>
        }
        ListFooterComponent={
          hasNextPage ? (
            <View style={[styles.footer, { paddingVertical: theme.space.lg, gap: theme.space.sm }]}>
              <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>
                🌰 더 많은 알림이 있어요! 🌰
              </Text>
              {isFetchingNextPage ? <ActivityIndicator color={theme.color.brand.solid} /> : null}
            </View>
          ) : null
        }
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>
              {isPending
                ? '불러오는 중이에요.'
                : isError
                  ? '알림을 불러오지 못했어요.'
                  : '아직 도착한 알림이 없어요.'}
            </Text>
          </View>
        }
        showsVerticalScrollIndicator={false}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { paddingBottom: 40, flexGrow: 1, paddingTop: 16 },
  titleRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  markAllButton: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  footer: { alignItems: 'center' },
  emptyState: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 60 },
});
