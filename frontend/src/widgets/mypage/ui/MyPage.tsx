import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import {
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  type ImageSourcePropType,
} from 'react-native';

import { signOut, useAuthStore, useProfile, WithdrawButton } from '@/features/auth';
import { PushNotificationToggle } from '@/features/notification';
import { useTheme } from '@/shared/theme';
import { Header, Screen } from '@/shared/ui';

export type MyPageMenuItem = {
  label: string;
  onPress: () => void;
};

type MyPageProps = {
  /** 역할마다 다른 메뉴 (부모는 가족 관리가 더 있다) */
  menu: MyPageMenuItem[];
  /** 역할별 캐릭터 이미지 */
  character: ImageSourcePropType;
};

/**
 * 마이페이지 (부모·자녀 공용). 프로필 → 메뉴 → 계정 정리 순서로,
 * 자주 쓰는 것은 위에 두고 되돌릴 수 없는 동작(탈퇴)은 맨 아래에 작게 둔다.
 */
export function MyPage({ menu, character }: MyPageProps) {
  const theme = useTheme();
  // 세션의 이름은 로그인 직후에만 있어서(자동 로그인은 JWT 에 이름이 없다) 서버 값을 우선한다
  const profile = useProfile();
  const sessionName = useAuthStore((s) => s.user?.name);
  const name = profile.data?.name ?? sessionName;
  const signedInAt = useAuthStore((s) => s.signedInAt);

  return (
    <Screen
      padded={false}
      header={<Header title="마이페이지" onBack={() => router.back()} />}
      style={styles.screen}
    >
      <ScrollView contentContainerStyle={styles.grow} showsVerticalScrollIndicator={false}>
        <View style={[styles.profile, { paddingVertical: theme.space['3xl'] }]}>
          <Image source={character} style={styles.face} resizeMode="contain" />
          <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>
            {name ?? '—'}
          </Text>
        </View>

        {/* 프로필과 메뉴 사이 연회색 띠 — 두 영역을 갈라 준다 */}
        <View style={[styles.divider, { backgroundColor: theme.color.background.surfaceSunken }]} />

        <View style={{ paddingHorizontal: theme.layout.screenX }}>
          {menu.map((item) => (
            <Pressable
              key={item.label}
              onPress={item.onPress}
              accessibilityRole="button"
              style={[styles.menuRow, { paddingVertical: theme.space.xl }]}
            >
              <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
                {item.label}
              </Text>
              <Ionicons name="chevron-forward" size={20} color={theme.color.text.muted} />
            </Pressable>
          ))}
          <PushNotificationToggle />
        </View>

        <View style={styles.spacer} />

        <View style={[styles.footer, { gap: theme.space.md, paddingBottom: theme.space['3xl'] }]}>
          {signedInAt ? (
            <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
              최근접속 {formatSignedInAt(signedInAt)}
            </Text>
          ) : null}
          <Pressable onPress={() => void signOut()} hitSlop={8} accessibilityRole="button">
            <Text style={[theme.typography.bodySm, styles.link, { color: theme.color.text.sub }]}>
              로그아웃
            </Text>
          </Pressable>
          <WithdrawButton variant="link" />
        </View>
      </ScrollView>
    </Screen>
  );
}

/** 2026-08-25T13:35:46+09:00 → "2026.08.25 13:35:46" */
function formatSignedInAt(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

const styles = StyleSheet.create({
  screen: { paddingVertical: 0 },
  grow: { flexGrow: 1 },
  profile: { alignItems: 'center', gap: 12 },
  face: { width: 120, height: 120 },
  divider: { height: 12 },
  menuRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  spacer: { flex: 1, minHeight: 40 },
  footer: { alignItems: 'center' },
  link: { textDecorationLine: 'underline' },
});
