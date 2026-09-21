import { LinearGradient } from 'expo-linear-gradient';
import { createContext, useContext, type ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useTheme } from '@/shared/theme';

import { ContentCard } from './ContentCard';

/**
 * 카드 최소 높이(띠 포함). 내용이 적어도 지갑답게 넉넉한 크기를 유지하고, 남는 높이는
 * 금액과 버튼 사이·제목과 금액 사이 여백으로 흘러 들어간다.
 */
const CARD_MIN_HEIGHT = 240;

type WalletCardTone = {
  /** 포인트 색 — 윗줄 띠, 아이콘 테두리, 채운 버튼 */
  accent: string;
  /** 채운 버튼 위의 글씨색 (노랑처럼 밝은 포인트 색은 어두운 글씨가 필요하다) */
  onAccent: string;
  /** 배경을 포인트 색으로 채우고 글씨·버튼을 흰색으로 뒤집는다 */
  inverted: boolean;
};

const WalletCardToneContext = createContext<WalletCardTone | null>(null);

type WalletCardProps = {
  accent: string;
  onAccent?: string;
  inverted?: boolean;
  /** 뒤집힌 카드의 바탕을 단색 대신 그라디언트로 칠한다 (자녀 도토리 카드의 초록) */
  gradient?: readonly [string, string];
  /** 아이콘 테두리 안에 들어가는 것 (48px 원 안쪽) */
  icon: ReactNode;
  title: string;
  subtitle?: string;
  /** 제목 줄 오른쪽 끝 (작은 이동 링크 등) */
  headerRight?: ReactNode;
  /** 큰 글씨로 보여줄 값 */
  amount: string;
  /** 금액 아래 한 줄 안내 */
  note?: string;
  /** 아래 버튼들. `WalletButton` 을 넣는다 */
  children?: ReactNode;
  /** 슬라이드처럼 높이가 정해진 자리에서 남는 높이를 채운다 */
  fill?: boolean;
};

/**
 * 홈 맨 위 지갑 카드의 공통 틀 (자녀 홈·부모 홈 슬라이드가 함께 쓴다).
 * 윗줄 색 띠 → 아이콘을 두른 테두리와 제목 → 큰 금액 → 버튼 순서다.
 * 색은 포인트(accent) 하나만 쓴다 — 흰 바탕에 그 색을 띠·테두리·버튼에 얹거나, `inverted` 로 뒤집어
 * 그 색 바탕에 흰색을 얹는다. 슬라이드의 장마다 색만 바뀌고 모양은 같아서 넘겨도 어긋나지 않는다.
 */
export function WalletCard({
  accent,
  onAccent,
  inverted = false,
  gradient,
  icon,
  title,
  subtitle,
  headerRight,
  amount,
  note,
  children,
  fill = false,
}: WalletCardProps) {
  const theme = useTheme();
  const white = theme.color.text.onBrand;

  const titleColor = inverted ? white : theme.color.text.strong;
  const softColor = inverted ? white : theme.color.text.muted;

  return (
    <WalletCardToneContext.Provider value={{ accent, onAccent: onAccent ?? white, inverted }}>
      <ContentCard
        style={[
          fill ? styles.fill : undefined,
          inverted
            ? { backgroundColor: gradient?.[1] ?? accent, borderColor: gradient?.[1] ?? accent }
            : undefined,
        ]}
      >
        {/* 안쪽을 따로 둥글게 잘라야 띠가 카드 모서리를 넘지 않는다 (바깥은 그림자 때문에 자르지 않는다) */}
        <View style={[styles.clip, { borderRadius: theme.radius.lg }]}>
          {inverted && gradient ? (
            <LinearGradient
              colors={gradient}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={StyleSheet.absoluteFill}
            />
          ) : null}
          <View
            style={[
              styles.accentBar,
              { backgroundColor: inverted ? white : accent, opacity: inverted ? 0.85 : 1 },
            ]}
          />

          <View
            style={[
              styles.body,
              {
                paddingHorizontal: theme.space.xl,
                paddingTop: theme.space.xl,
                // 버튼 아래가 카드 가장자리에 붙어 답답하지 않게 위쪽보다 더 둔다
                paddingBottom: theme.space['2xl'],
                gap: theme.space.md,
              },
            ]}
          >
            <View style={[styles.headRow, { gap: theme.space.md }]}>
              <View
                style={[
                  styles.iconRing,
                  {
                    borderColor: inverted ? white : accent,
                    backgroundColor: inverted ? theme.color.background.surface : 'transparent',
                    borderRadius: theme.radius.full,
                  },
                ]}
              >
                {icon}
              </View>
              <View style={{ flex: 1 }}>
                <Text numberOfLines={1} style={[theme.typography.label, { color: titleColor }]}>
                  {title}
                </Text>
                {subtitle ? (
                  <Text
                    numberOfLines={1}
                    style={[styles.sub, { color: softColor, opacity: inverted ? 0.8 : 1 }]}
                  >
                    {subtitle}
                  </Text>
                ) : null}
              </View>
              {headerRight}
            </View>

            <Text style={[theme.typography.display, { color: titleColor }]}>{amount}</Text>

            {note ? (
              <Text
                style={[
                  theme.typography.caption,
                  { color: softColor, opacity: inverted ? 0.85 : 1 },
                ]}
              >
                {note}
              </Text>
            ) : null}

            {children ? (
              <View style={[styles.actionRow, { gap: theme.space.sm, marginTop: theme.space.sm }]}>
                {children}
              </View>
            ) : null}
          </View>
        </View>
      </ContentCard>
    </WalletCardToneContext.Provider>
  );
}

type WalletButtonProps = {
  label: string;
  onPress: () => void;
  /** primary: 포인트 색(뒤집힌 카드에서는 흰색)으로 채운 주 버튼 · soft: 테두리 없는 연한 보조 버튼 */
  tone?: 'primary' | 'soft';
  disabled?: boolean;
};

/**
 * `WalletCard` 안의 버튼. 카드의 색과 뒤집힘 여부를 알아서 따라간다 — 카드마다 버튼 색을
 * 따로 넘기지 않아도 모양이 통일된다. 테두리는 두르지 않는다(은행 노랑처럼 색이 강하면 흰 카드에서 튄다).
 */
export function WalletButton({ label, onPress, tone = 'primary', disabled }: WalletButtonProps) {
  const theme = useTheme();
  const card = useContext(WalletCardToneContext);
  const accent = card?.accent ?? theme.color.brand.solid;
  const onAccent = card?.onAccent ?? theme.color.text.onBrand;
  const inverted = card?.inverted ?? false;

  const primary = tone === 'primary';
  const background = primary
    ? inverted
      ? theme.color.background.surface
      : accent
    : inverted
      ? 'rgba(255, 255, 255, 0.2)'
      : theme.color.background.surfaceSunken;
  const color = primary
    ? inverted
      ? accent
      : onAccent
    : inverted
      ? theme.color.text.onBrand
      : theme.color.text.strong;

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      style={[
        styles.button,
        {
          backgroundColor: background,
          borderRadius: theme.radius.lg,
          paddingHorizontal: theme.space.md,
          opacity: disabled ? 0.5 : 1,
        },
      ]}
    >
      <Text numberOfLines={1} style={[theme.typography.label, { color }]}>
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  clip: { flex: 1, overflow: 'hidden', minHeight: CARD_MIN_HEIGHT },
  accentBar: { height: 5 },
  body: { flex: 1, justifyContent: 'space-between' },
  headRow: { flexDirection: 'row', alignItems: 'center' },
  // 아이콘 둘레의 테두리가 포인트 색의 주된 자리다
  iconRing: {
    width: 48,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
  },
  sub: { fontSize: 13, lineHeight: 18 },
  actionRow: { flexDirection: 'row' },
  button: { flex: 1, minHeight: 46, alignItems: 'center', justifyContent: 'center' },
});
