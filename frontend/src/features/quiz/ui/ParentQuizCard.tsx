import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from 'expo-router';
import { useCallback, type ReactNode } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';

import {
  hasSubmissions,
  parentQuizStatus,
  parentQuizStatusLabel,
  type ParentQuizStatus,
} from '@/features/quiz/lib/parentQuiz';
import { parentQuizFailure, parentQuizFailureMessage } from '@/features/quiz/lib/quizError';
import type { QuizParentAttempt, QuizParentToday } from '@/features/quiz/model/types';
import { useParentTodayQuiz } from '@/features/quiz/model/useParentTodayQuiz';
import { useTheme, type Theme } from '@/shared/theme';
import { Button, ContentCard } from '@/shared/ui';

type ParentQuizCardProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·자녀 조회가 끝나기 전 — 아직 조회에 쓸 id 가 없다 */
  familyPending: boolean;
};

/**
 * 부모 홈 "오늘의 경제 퀴즈" 카드. 아이가 아직 안 풀었으면 문제와 정답만, 제출했으면
 * (맞췄든 틀렸든) 아이가 낸 답과 정오답까지 시도별로 보여 준다.
 *
 * 아이 홈의 퀴즈 카드(풀러 가는 진입 카드)와 성격이 다르다 — 부모는 풀 수 없고 결과를 읽기만
 * 하므로 눌러서 이동하는 자리가 아니라 그 자리에서 내용을 펼쳐 보여 준다.
 *
 * 오늘의 퀴즈가 아직 생성 전이면(QUIZ_NOT_READY) 오류가 아니라 준비 중 안내로 처리한다.
 */
export function ParentQuizCard({ familyId, childId, familyPending }: ParentQuizCardProps) {
  const theme = useTheme();
  const query = useParentTodayQuiz(familyId, childId);

  // 탭 화면은 전환해도 언마운트되지 않는다 — 홈으로 돌아올 때마다 아이가 그새 푼 결과를 다시 받는다.
  // 마운트 직후의 첫 조회와 겹쳐도 진행 중인 요청을 끊지 않는다.
  const refetch = query.refetch;
  const canQuery = familyId != null && childId != null;
  useFocusEffect(
    useCallback(() => {
      if (canQuery) {
        refetch({ cancelRefetch: false });
      }
    }, [canQuery, refetch]),
  );

  const loading = familyPending || (canQuery && query.isPending);

  return (
    <ContentCard style={[styles.card, { padding: theme.space.lg, gap: theme.space.lg }]}>
      <View style={[styles.headRow, { gap: theme.space.md }]}>
        <Image
          source={require('@/assets/images/icons/icon-quiz.png')}
          style={styles.icon}
          resizeMode="contain"
        />
        <View style={styles.headText}>
          <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
            오늘의 경제 퀴즈
          </Text>
        </View>
        {query.data != null ? <StatusBadge status={parentQuizStatus(query.data)} /> : null}
      </View>

      {loading ? (
        <Notice text="불러오는 중이에요" />
      ) : !canQuery ? (
        <Notice text="연결된 자녀가 없어요" />
      ) : query.isError ? (
        <FailureNotice
          error={query.error}
          retrying={query.isFetching}
          onRetry={() => query.refetch()}
        />
      ) : query.data != null ? (
        <QuizBody quiz={query.data} />
      ) : null}
    </ContentCard>
  );
}

function Notice({ text }: { text: string }) {
  const theme = useTheme();

  return <Text style={[theme.typography.bodySm, { color: theme.color.text.muted }]}>{text}</Text>;
}

/** 조회 실패. 준비 전·권한 없음은 다시 눌러 봐야 소용없어 재시도 버튼을 두지 않는다. */
function FailureNotice({
  error,
  retrying,
  onRetry,
}: {
  error: unknown;
  retrying: boolean;
  onRetry: () => void;
}) {
  const theme = useTheme();
  const failure = parentQuizFailure(error);
  const retryable = failure === 'NETWORK' || failure === 'ERROR';

  return (
    <View style={{ gap: theme.space.md }}>
      <Text
        style={[
          theme.typography.bodySm,
          { color: failure === 'NOT_READY' ? theme.color.text.muted : theme.color.text.default },
        ]}
      >
        {parentQuizFailureMessage(failure)}
      </Text>
      {retryable ? (
        <Button
          label="다시 시도"
          variant="outline"
          size="sm"
          loading={retrying}
          onPress={onRetry}
          style={styles.retry}
        />
      ) : null}
    </View>
  );
}

function StatusBadge({ status }: { status: ParentQuizStatus }) {
  const theme = useTheme();
  const { background, text } = statusColors(theme, status);

  return (
    <View
      style={[
        styles.badge,
        {
          backgroundColor: background,
          borderRadius: theme.radius.full,
          paddingHorizontal: theme.space.md,
          paddingVertical: theme.space.xs,
        },
      ]}
    >
      <Text style={[theme.typography.bodySm, { color: text }]}>
        {parentQuizStatusLabel(status)}
      </Text>
    </View>
  );
}

function statusColors(theme: Theme, status: ParentQuizStatus) {
  switch (status) {
    case 'SOLVED':
      return {
        background: theme.color.status.success.subtle,
        text: theme.color.status.success.text,
      };
    case 'FAILED':
      return { background: theme.color.status.danger.subtle, text: theme.color.status.danger.text };
    case 'IN_PROGRESS':
      return {
        background: theme.color.status.warning.subtle,
        text: theme.color.status.warning.text,
      };
    case 'UNSOLVED':
      return { background: theme.color.background.surfaceSunken, text: theme.color.text.sub };
  }
}

function QuizBody({ quiz }: { quiz: QuizParentToday }) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.lg }}>
      <Field label="문제">
        <Text style={[theme.typography.body, { color: theme.color.text.strong }]}>
          {quiz.question}
        </Text>
      </Field>

      <Field label="정답">
        <Text style={[theme.typography.body, { color: theme.color.status.success.text }]}>
          {quiz.correctAnswer}
        </Text>
      </Field>

      {hasSubmissions(quiz) ? (
        <Field label={`아이가 제출한 답 (${quiz.attemptCount}/${quiz.maxAttempts}회)`}>
          <View style={{ gap: theme.space.sm }}>
            {quiz.attempts.map((attempt) => (
              <AttemptRow key={attempt.quizAttemptId} attempt={attempt} />
            ))}
          </View>
        </Field>
      ) : (
        <Notice text="아이가 아직 풀지 않았어요" />
      )}
    </View>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.xs }}>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>{label}</Text>
      {children}
    </View>
  );
}

/** 제출 1회. 색만으로 정오답을 알리지 않도록 아이콘과 글자를 함께 둔다. */
function AttemptRow({ attempt }: { attempt: QuizParentAttempt }) {
  const theme = useTheme();
  const status = attempt.isCorrect ? theme.color.status.success : theme.color.status.danger;

  return (
    <View style={[styles.attemptRow, { gap: theme.space.sm }]}>
      <View
        style={[
          styles.resultDot,
          { backgroundColor: status.solid, borderRadius: theme.radius.full },
        ]}
      >
        <Ionicons
          name={attempt.isCorrect ? 'checkmark' : 'close'}
          size={16}
          color={theme.color.text.onBrand}
        />
      </View>
      <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
        {attempt.attemptNumber}회
      </Text>
      <Text style={[theme.typography.body, styles.answer, { color: theme.color.text.strong }]}>
        {attempt.submittedAnswer}
      </Text>
      <Text style={[theme.typography.bodySm, { color: status.text }]}>
        {attempt.isCorrect ? '정답' : '오답'}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: { flexDirection: 'column' },
  headRow: { flexDirection: 'row', alignItems: 'center' },
  icon: { width: 56, height: 56 },
  headText: { flex: 1 },
  badge: { alignSelf: 'center' },
  retry: { alignSelf: 'flex-start' },
  attemptRow: { flexDirection: 'row', alignItems: 'center', minHeight: 32 },
  resultDot: { width: 24, height: 24, alignItems: 'center', justifyContent: 'center' },
  answer: { flex: 1 },
});
