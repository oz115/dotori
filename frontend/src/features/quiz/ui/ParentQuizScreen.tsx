import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import { useCallback, type ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { hasSubmissions, parentQuizStatus } from '@/features/quiz/lib/parentQuiz';
import { parentQuizFailure, parentQuizFailureMessage } from '@/features/quiz/lib/quizError';
import type { QuizParentAttempt, QuizParentToday } from '@/features/quiz/model/types';
import { useParentTodayQuiz } from '@/features/quiz/model/useParentTodayQuiz';
import { ParentQuizStatusBadge } from '@/features/quiz/ui/ParentQuizStatusBadge';
import { useTheme } from '@/shared/theme';
import { Button, ContentCard, Header, Screen } from '@/shared/ui';

type ParentQuizScreenProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 가족·자녀 조회가 끝나기 전 — 아직 조회에 쓸 id 가 없다 */
  familyPending: boolean;
};

/**
 * 부모용 "오늘의 경제 퀴즈" 화면. 아이가 아직 안 풀었으면 문제와 정답만, 제출했으면
 * (맞췄든 틀렸든) 아이가 낸 답과 정오답까지 시도별로 보여 준다.
 *
 * 아이의 퀴즈 화면(QuizScreen)과 성격이 다르다 — 부모는 풀 수 없고 결과를 읽기만 한다.
 * 홈의 진입 카드(ParentQuizEntryCard)를 눌러 들어오는 독립 화면이다.
 *
 * 오늘의 퀴즈가 아직 생성 전이면(QUIZ_NOT_READY) 오류가 아니라 준비 중 안내로 처리한다.
 */
export function ParentQuizScreen({ familyId, childId, familyPending }: ParentQuizScreenProps) {
  const theme = useTheme();
  const query = useParentTodayQuiz(familyId, childId);

  // 아이가 그새 푼 결과를 화면에 돌아올 때마다 다시 받는다.
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

  const goBack = () => (router.canGoBack() ? router.back() : router.replace('/'));
  const loading = familyPending || (canQuery && query.isPending);

  return (
    <Screen
      header={<Header title="오늘의 경제 퀴즈" onBack={goBack} />}
      scroll
      // Screen 기본 상하 여백이 0이라 카드가 헤더·하단 제스처 바에 붙어 보인다.
      style={{ paddingTop: theme.space.lg, paddingBottom: theme.space['4xl'] }}
    >
      <ContentCard style={{ padding: theme.space['2xl'], gap: theme.space.xl }}>
        {query.data != null ? (
          <View style={styles.statusRow}>
            <ParentQuizStatusBadge status={parentQuizStatus(query.data)} />
          </View>
        ) : null}

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
    </Screen>
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

function QuizBody({ quiz }: { quiz: QuizParentToday }) {
  const theme = useTheme();

  return (
    <View style={{ gap: theme.space.xl }}>
      <Field label="문제">
        {/* 질문은 문장이 길어 h3(20px)면 줄이 자주 꺾여 읽기 어렵다 — 한 단계 작은 titleMd */}
        <Text style={[theme.typography.titleMd, { color: theme.color.text.strong }]}>
          {quiz.question}
        </Text>
      </Field>

      <Field label="정답">
        <Text style={[theme.typography.label, { color: theme.color.status.success.text }]}>
          {quiz.correctAnswer}
        </Text>
      </Field>

      {hasSubmissions(quiz) ? (
        <Field label={`아이가 제출한 답 (${quiz.attemptCount}/${quiz.maxAttempts}회)`}>
          <View style={{ gap: theme.space.md }}>
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
    <View style={{ gap: theme.space.sm }}>
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
      <Text style={[theme.typography.label, styles.answer, { color: theme.color.text.strong }]}>
        {attempt.submittedAnswer}
      </Text>
      <Text style={[theme.typography.bodySm, { color: status.text }]}>
        {attempt.isCorrect ? '정답' : '오답'}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  statusRow: { flexDirection: 'row' },
  retry: { alignSelf: 'flex-start' },
  attemptRow: { flexDirection: 'row', alignItems: 'center', minHeight: 32 },
  resultDot: { width: 24, height: 24, alignItems: 'center', justifyContent: 'center' },
  answer: { flex: 1 },
});
