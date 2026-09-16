import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import { useInvestmentNewsDetail } from '@/features/investment/model/useInvestmentNewsDetail';
import type { InvestmentNews } from '@/features/investment/model/types';
import { useTheme } from '@/shared/theme';
import { Button } from '@/shared/ui';

type Props = {
  /** 보여줄 뉴스. null 이면 팝업이 닫혀 있는 상태다. */
  news: InvestmentNews | null;
  onClose: () => void;
};

/**
 * 뉴스 카드를 탭하면 뜨는 상세 팝업. 본문과 관련 회사, 가격에 주는 영향 방향을
 * 보여줘서 아이가 "어느 회사가 오를지/내릴지"를 판단하게 돕는다.
 * 딤 영역 탭·안드로이드 뒤로가기·확인 버튼 모두로 닫힌다.
 * 본문(content)은 목록 응답에 없어서 팝업이 열릴 때 상세 API 로 가져온다 —
 * news.content 가 이미 있으면(테스트 등) 그 값을 그대로 쓴다.
 */
export function NewsDetailModal({ news, onClose }: Props) {
  const theme = useTheme();
  const detail = useInvestmentNewsDetail(news != null && news.content == null ? news.id : undefined);

  if (news == null) {
    return null;
  }

  const content = news.content ?? detail.data?.content;
  const contentText = detail.isError
    ? '소식을 불러오지 못했어요. 잠시 후 다시 열어 주세요.'
    : (content ?? '소식을 불러오는 중이에요…');
  const rising = news.impact === 'POSITIVE';
  const impactColors = rising ? theme.color.status.success : theme.color.status.danger;

  return (
    <Modal visible transparent animationType="fade" onRequestClose={onClose}>
      <Pressable
        accessibilityLabel="팝업 닫기"
        style={[styles.dim, { backgroundColor: theme.color.background.overlay }]}
        onPress={onClose}
      >
        {/* 카드 안쪽 탭이 딤의 onPress 로 새지 않게 Pressable 로 삼킨다 */}
        <Pressable
          style={[
            styles.card,
            {
              backgroundColor: theme.color.background.surface,
              borderRadius: theme.radius.xl,
              padding: theme.space.xl,
              gap: theme.space.lg,
            },
          ]}
        >
          <View style={styles.badgeRow}>
            <View
              style={[
                styles.badge,
                { backgroundColor: theme.color.brand.solid, borderRadius: theme.radius.full },
              ]}
            >
              <Text style={[theme.typography.caption, { color: theme.color.text.onBrand }]}>
                {news.tag}
              </Text>
            </View>
            <View
              style={[
                styles.badge,
                { backgroundColor: impactColors.subtle, borderRadius: theme.radius.full },
              ]}
            >
              <Text style={[theme.typography.caption, { color: impactColors.text }]}>
                {rising ? '📈 오르는 뉴스' : '📉 내리는 뉴스'}
              </Text>
            </View>
          </View>

          <View style={[styles.titleRow, { gap: theme.space.md }]}>
            {/* 일러스트 자리 표시자 — Figma 에셋 export 후 이미지로 교체 */}
            <Text style={styles.emoji}>{news.emoji}</Text>
            <Text style={[theme.typography.h3, styles.title, { color: theme.color.text.strong }]}>
              {news.title.replace('\n', ' ')}
            </Text>
          </View>

          <Text style={[theme.typography.bodySm, { color: theme.color.text.default }]}>
            {contentText}
          </Text>

          <View style={styles.metaRow}>
            <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
              관련 회사{' '}
              <Text style={{ color: theme.color.text.strong }}>{news.relatedProductName}</Text>
            </Text>
            <Text style={[theme.typography.caption, { color: theme.color.text.muted }]}>
              {news.date}
            </Text>
          </View>

          <Button label="확인" variant="primary" onPress={onClose} />
        </Pressable>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  dim: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  card: {},
  badgeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 3,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  emoji: {
    fontSize: 40,
  },
  title: {
    flex: 1,
  },
  metaRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
});
