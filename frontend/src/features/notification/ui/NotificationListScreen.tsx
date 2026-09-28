import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useMemo } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { useDeleteAllNotifications } from '@/features/notification/model/useDeleteAllNotifications';
import { useDeleteNotification } from '@/features/notification/model/useDeleteNotification';
import { useMarkAllAsRead } from '@/features/notification/model/useMarkAllAsRead';
import { useMarkAsRead } from '@/features/notification/model/useMarkAsRead';
import { useNotifications } from '@/features/notification/model/useNotifications';
import type { NotificationItem } from '@/features/notification/model/types';
import { NotificationRow } from '@/features/notification/ui/NotificationRow';
import { useTheme } from '@/shared/theme';
import { Header, Screen, usePullToRefresh } from '@/shared/ui';

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
  const refreshControl = usePullToRefresh();
  const { data, isPending, isError, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useNotifications();
  const markAsRead = useMarkAsRead();
  const markAllAsRead = useMarkAllAsRead();
  const deleteNotification = useDeleteNotification();
  const deleteAll = useDeleteAllNotifications();

  const items = useMemo(() => data?.pages.flatMap((page) => page.notifications) ?? [], [data]);
  // unreadCount 는 필터와 무관하게 항상 전체 미읽 개수라, 어느 페이지에서 읽어도 같다.
  const unreadCount = data?.pages[0]?.unreadCount ?? 0;

  const handlePress = (item: NotificationItem) => {
    if (!item.isRead) {
      markAsRead.mutate(item.notificationId);
    }
    onPressNotification(item);
  };

  const confirmDeleteAll = () => {
    if (deleteAll.isPending) return;
    Alert.alert('알림을 모두 지울까요?', '지운 알림은 다시 볼 수 없어요.', [
      { text: '취소', style: 'cancel' },
      {
        text: '모두 삭제',
        style: 'destructive',
        onPress: () =>
          deleteAll.mutate(undefined, {
            onError: () => Alert.alert('지우지 못했어요', '잠시 후 다시 시도해주세요.'),
          }),
      },
    ]);
  };

  return (
    <Screen header={<Header title="알림" onBack={() => router.back()} />} padded={false}>
      <FlatList
        refreshControl={refreshControl}
        data={items}
        keyExtractor={(item) => String(item.notificationId)}
        renderItem={({ item }) => (
          <NotificationRow
            item={item}
            onPress={() => handlePress(item)}
            onDelete={() => deleteNotification.mutateAsync(item.notificationId)}
          />
        )}
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
              <Text style={[theme.typography.h3, styles.title, { color: theme.color.text.strong }]}>
                읽지 않은 알림 {unreadCount}개
              </Text>
              <View style={[styles.actionRow, { gap: theme.space.md }]}>
                {unreadCount > 0 ? (
                  <Pressable
                    accessibilityRole="button"
                    onPress={() => markAllAsRead.mutate()}
                    hitSlop={8}
                    style={styles.textButton}
                  >
                    <Ionicons name="checkmark" size={16} color={theme.color.brand.solid} />
                    <Text style={[theme.typography.label, { color: theme.color.brand.solid }]}>
                      모두 읽음
                    </Text>
                  </Pressable>
                ) : null}
                {/* 하나씩 지우는 것 말고는 알림함을 비울 방법이 없었다 (QA). 되돌릴 수 없어
                    지울 것이 있을 때만 보여 주고, 누르면 한 번 더 확인받는다 */}
                {items.length > 0 ? (
                  <Pressable
                    accessibilityRole="button"
                    onPress={confirmDeleteAll}
                    hitSlop={8}
                    style={styles.textButton}
                  >
                    <Ionicons
                      name="trash-outline"
                      size={16}
                      color={theme.color.status.danger.solid}
                    />
                    <Text
                      style={[theme.typography.label, { color: theme.color.status.danger.solid }]}
                    >
                      모두 삭제
                    </Text>
                  </Pressable>
                ) : null}
              </View>
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
                더 많은 알림이 있어요!
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
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 8,
  },
  title: { flexShrink: 1 },
  actionRow: { flexDirection: 'row', alignItems: 'center', flexShrink: 0 },
  textButton: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  footer: { alignItems: 'center' },
  emptyState: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 60 },
});
