import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';

import { quizSubmitErrorMessage, quizTodayErrorMessage } from '@/features/quiz/lib/quizError';
import { useSubmitQuiz } from '@/features/quiz/model/useSubmitQuiz';
import { useTodayQuiz } from '@/features/quiz/model/useTodayQuiz';
import type { QuizSubmitResult } from '@/features/quiz/model/types';
import { AttemptDots } from '@/features/quiz/ui/AttemptDots';
import { apiErrorCode } from '@/shared/api';
import { useTheme, type Theme } from '@/shared/theme';
import { Button, Callout, ContentCard, Header, Input, Screen } from '@/shared/ui';

const MAX_ATTEMPTS = 3;
/** 퀴즈 화면 전용 배경색(연크림). 카드는 흰색으로 그 위에 뜬다 — 다른 화면의 기본 배경과는 다르다. */
const QUIZ_BACKGROUND = '#FBFAF6';
/** 정답 결과 화면 전용 배경색(연초록). 축하하는 화면이라 다른 퀴즈 화면보다 살짝 더 그린빛을 준다. */
const SUCCESS_BACKGROUND = '#F0F2ED';

type QuizScreenProps = {
  familyId: number | undefined;
  childId: number | undefined;
  /** 정답을 맞혀 포인트가 들어왔을 때 호출한다. 잔액은 wallet 슬라이스라 라우트가 새로고침을 맡는다. */
  onSubmitted?: () => void;
};

/**
 * 오늘의 경제 퀴즈. AI가 하루 1개 만들어 주는 문제를 풀고 정답을 맞히면 포인트를 받는다.
 * 최대 3번 도전할 수 있고, 정답을 맞히거나 3번을 다 쓰면 정답·해설이 공개되는 전용 결과
 * 화면(성공/소진)으로 넘어가고 더 이상 제출할 수 없다 — 그 상태는 서버가 alreadySubmitted로
 * 알려준다.
 */
export function QuizScreen({ familyId, childId, onSubmitted }: QuizScreenProps) {
  const theme = useTheme();
  const { data: quiz, isPending, isError, error } = useTodayQuiz(familyId, childId);
  const submitQuiz = useSubmitQuiz(familyId, childId);
  const [answer, setAnswer] = useState('');
  const [result, setResult] = useState<QuizSubmitResult | null>(null);

  // "도전 기회 n/3" 표시는 서버의 attemptCount가 기준이지만, 방금 제출한 시도는 무효화 후
  // refetch가 끝나야 반영돼 짧게 한 박자 밀린다. 그 틈을 메우려고 로컬로 임시 가산(pendingBump)
  // 해두고, attemptCount가 바뀌어(=서버가 따라옴) 실제로 반영되면 렌더 중에 바로 0으로 되돌린다.
  const [pendingBump, setPendingBump] = useState(0);
  const [syncedAttemptCount, setSyncedAttemptCount] = useState<number | null>(null);
  if (quiz && syncedAttemptCount !== quiz.attemptCount) {
    setSyncedAttemptCount(quiz.attemptCount);
    if (pendingBump !== 0) setPendingBump(0);
  }

  const goBack = () => (router.canGoBack() ? router.back() : router.replace('/'));

  const handleSubmit = () => {
    if (!quiz || answer.trim().length === 0) return;
    submitQuiz.mutate(
      { quizId: quiz.quizId, answer: answer.trim() },
      {
        onSuccess: (res) => {
          setResult(res);
          setAnswer('');
          setPendingBump((n) => n + 1);
          if (res.isCorrect) onSubmitted?.();
        },
      },
    );
  };

  if (isPending) {
    return (
      <Screen
        header={
          <Header title="오늘의 경제 퀴즈" onBack={goBack} backgroundColor={QUIZ_BACKGROUND} />
        }
        backgroundColor={QUIZ_BACKGROUND}
      >
        <CenteredMessage text="불러오는 중이에요." theme={theme} />
      </Screen>
    );
  }

  if (isError) {
    return (
      <Screen
        header={
          <Header title="오늘의 경제 퀴즈" onBack={goBack} backgroundColor={QUIZ_BACKGROUND} />
        }
        backgroundColor={QUIZ_BACKGROUND}
      >
        {apiErrorCode(error) === 'QUIZ_NOT_READY' ? (
          <Callout
            title="오늘의 퀴즈는 아직 준비 중이에요"
            description="조금 이따 다시 확인해보세요!"
          />
        ) : (
          <CenteredMessage text={quizTodayErrorMessage(error)} theme={theme} />
        )}
      </Screen>
    );
  }

  if (!quiz) {
    return (
      <Screen
        header={
          <Header title="오늘의 경제 퀴즈" onBack={goBack} backgroundColor={QUIZ_BACKGROUND} />
        }
        backgroundColor={QUIZ_BACKGROUND}
      >
        <CenteredMessage text="오늘의 퀴즈를 찾을 수 없어요." theme={theme} />
      </Screen>
    );
  }

  const locked = quiz.alreadySubmitted || (result != null && result.correctAnswer != null);
  const usedAttempts = Math.min(quiz.attemptCount + pendingBump, MAX_ATTEMPTS);
  const showRetryHint = result != null && !result.isCorrect && result.correctAnswer == null;
  // 초성 힌트는 서버가 준다(정답은 잠기기 전까지 클라이언트에 없다). 방금 제출한 결과가 가장 최신이고,
  // 화면을 나갔다 다시 들어온 경우엔 오늘의 퀴즈 조회 응답이 같은 힌트를 들고 있다.
  const chosungHint = result?.hint ?? quiz.hint ?? null;

  if (locked) {
    // 이번 세션에서 막 제출해 결과를 아는 경우엔 그 정답 여부를 그대로 쓴다. 새로고침 등으로
    // result가 없는 경우엔 "3회를 채우기 전에 잠겼다"는 사실 자체가 정답이었다는 뜻이다 —
    // 3번째 시도에서 막 정답을 맞히고 재진입한 극히 드문 경우만 소진 화면으로 안내한다.
    const wasCorrect = result ? result.isCorrect : usedAttempts < MAX_ATTEMPTS;
    const revealedAnswer = result?.correctAnswer ?? quiz.correctAnswer;
    const revealedExplanation = result?.explanation ?? quiz.explanation;

    return wasCorrect ? (
      <QuizResultSuccess
        theme={theme}
        answer={revealedAnswer}
        explanation={revealedExplanation}
        earnedPoints={result?.earnedPoints ?? quiz.rewardPoints}
        balanceAfter={result?.pointBalanceAfter ?? null}
        onClose={goBack}
      />
    ) : (
      <QuizResultExhausted
        theme={theme}
        answer={revealedAnswer}
        explanation={revealedExplanation}
        onClose={goBack}
      />
    );
  }

  const attemptNumber = Math.min(usedAttempts + 1, MAX_ATTEMPTS);

  return (
    <Screen
      header={<Header title="오늘의 경제 퀴즈" onBack={goBack} backgroundColor={QUIZ_BACKGROUND} />}
      backgroundColor={QUIZ_BACKGROUND}
      keyboardAvoiding
      scroll
      // Screen 기본 상하 여백이 0이라 카드가 헤더·하단 제스처 바에 붙어 보인다.
      style={{ paddingTop: theme.space.lg, paddingBottom: theme.space['4xl'] }}
    >
      <View style={{ gap: theme.space.lg }}>
        <ContentCard style={styles.attemptCard}>
          <Text style={[theme.typography.label, { color: theme.color.text.sub }]}>
            도전 기회 {attemptNumber}/{MAX_ATTEMPTS}
          </Text>
          <AttemptDots filled={attemptNumber} total={MAX_ATTEMPTS} />
        </ContentCard>

        <ContentCard style={{ padding: theme.space['2xl'], gap: theme.space.xl }}>
          <View
            style={[
              styles.rewardBadge,
              styles.alignStart,
              { backgroundColor: theme.color.brand.subtle, borderRadius: theme.radius.full },
            ]}
          >
            <Image
              source={require('@/assets/images/icons/icon-dotori.png')}
              style={styles.rewardIcon}
              resizeMode="contain"
            />
            <Text style={[theme.typography.caption, { color: theme.color.brand.subtleText }]}>
              정답 시 +{quiz.rewardPoints}P
            </Text>
          </View>

          {/* 질문은 문장이 길어 h3(20px)면 줄이 자주 꺾여 읽기 어렵다 — 한 단계 작은 titleMd */}
          <Text
            style={[theme.typography.titleMd, styles.question, { color: theme.color.text.strong }]}
          >
            Q. {quiz.question}
          </Text>

          <Image
            source={require('@/assets/images/chore/squirrel-think.png')}
            style={[styles.mascot, styles.selfCenter, styles.mascotSpacing]}
            resizeMode="contain"
          />

          {showRetryHint ? (
            <View
              style={[
                styles.dangerBanner,
                {
                  backgroundColor: theme.color.status.danger.subtle,
                  borderRadius: theme.radius.lg,
                  padding: theme.space.md,
                },
              ]}
            >
              <View style={styles.dangerTitleRow}>
                <Ionicons name="close-circle" size={18} color={theme.color.status.danger.text} />
                <Text style={[theme.typography.label, { color: theme.color.status.danger.text }]}>
                  아쉬워요! 다시 생각해볼까요?
                </Text>
              </View>
              <Text
                style={[
                  theme.typography.bodySm,
                  styles.dangerSubtext,
                  { color: theme.color.status.danger.text },
                ]}
              >
                남은 기회 {MAX_ATTEMPTS - usedAttempts}번
              </Text>
            </View>
          ) : null}

          {chosungHint ? (
            <View
              accessible
              accessibilityLabel={`초성 힌트 ${chosungHint}`}
              style={[
                styles.hintBox,
                {
                  backgroundColor: theme.color.brand.subtle,
                  borderRadius: theme.radius.lg,
                  padding: theme.space.lg,
                },
              ]}
            >
              <Text style={[theme.typography.label, { color: theme.color.brand.subtleText }]}>
                초성 힌트
              </Text>
              <Text
                style={[theme.typography.h3, styles.hintText, { color: theme.color.text.strong }]}
              >
                {chosungHint}
              </Text>
            </View>
          ) : null}

          <Input
            variant="box"
            placeholder="정답을 입력해보세요"
            value={answer}
            onChangeText={setAnswer}
            returnKeyType="done"
            onSubmitEditing={handleSubmit}
          />

          {submitQuiz.isError ? (
            <Text style={[theme.typography.caption, { color: theme.color.status.danger.text }]}>
              {quizSubmitErrorMessage(submitQuiz.error)}
            </Text>
          ) : null}

          <Button
            label={showRetryHint ? '다시 답하기' : '정답 확인'}
            loading={submitQuiz.isPending}
            disabled={answer.trim().length === 0}
            onPress={handleSubmit}
          />
        </ContentCard>
      </View>
    </Screen>
  );
}

type QuizResultProps = {
  theme: Theme;
  answer: string | null;
  explanation: string | null;
  onClose: () => void;
};

/**
 * 정답 결과 화면 공통 카드 — "정답: {답}" 카드와 "{답}의 뜻: {해설}" 카드를 따로 둔다.
 * 문제는 앞 화면(카드형 질문)에서 이미 봤으니 여기선 반복하지 않는다.
 */
function ResultAnswerCard({ theme, answer, explanation }: Omit<QuizResultProps, 'onClose'>) {
  if (!answer) return null;

  return (
    <View style={styles.resultAnswerGroup}>
      <ContentCard style={[styles.resultAnswerCard, { padding: theme.space.lg }]}>
        <Ionicons name="checkmark-circle" size={40} color={theme.color.brand.solid} />
        <View style={{ gap: 2 }}>
          <Text style={[theme.typography.caption, { color: theme.color.text.sub }]}>정답</Text>
          <Text style={[theme.typography.h3, { color: theme.color.text.strong }]}>{answer}</Text>
        </View>
      </ContentCard>

      {explanation ? (
        <ContentCard style={{ padding: theme.space.lg, gap: theme.space.sm }}>
          <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
            {answer}의 뜻
          </Text>
          {/* 해설은 아이가 실제로 읽는 본문이라 작은 글씨(bodySm) 대신 본문 크기 + 넉넉한 줄간격 */}
          <Text
            style={[theme.typography.body, styles.explanation, { color: theme.color.text.default }]}
          >
            {explanation}
          </Text>
        </ContentCard>
      ) : null}
    </View>
  );
}

/** 정답을 맞혔을 때의 전체화면 결과 — 포인트 지급과 보유 잔액을 함께 보여준다. */
function QuizResultSuccess({
  theme,
  answer,
  explanation,
  earnedPoints,
  balanceAfter,
  onClose,
}: QuizResultProps & { earnedPoints: number; balanceAfter: number | null }) {
  return (
    <Screen
      header={<Header title="" onClose={onClose} backgroundColor={SUCCESS_BACKGROUND} />}
      backgroundColor={SUCCESS_BACKGROUND}
      scroll
    >
      <View style={[styles.resultBody, { gap: theme.space.xl, paddingTop: theme.space['4xl'] }]}>
        <View style={styles.mascotStage}>
          <Image
            source={require('@/assets/images/chore/fanfare.png')}
            style={styles.fanfare}
            resizeMode="contain"
          />
          <Image
            source={require('@/assets/images/chore/squirrel-win.png')}
            style={styles.resultMascot}
            resizeMode="contain"
          />
        </View>

        <View style={{ gap: theme.space.xs, alignItems: 'center' }}>
          <Text style={[theme.typography.h2, styles.center, { color: theme.color.text.strong }]}>
            정답이에요!
          </Text>
          <Text style={[theme.typography.body, styles.center, { color: theme.color.text.sub }]}>
            +{earnedPoints}P를 받았어요
          </Text>
        </View>

        <ResultAnswerCard theme={theme} answer={answer} explanation={explanation} />

        {balanceAfter != null ? (
          <View
            style={[
              styles.balanceRow,
              {
                backgroundColor: theme.color.background.surfaceSunken,
                borderRadius: theme.radius.md,
                padding: theme.space.md,
                width: '100%',
              },
            ]}
          >
            <Text style={[theme.typography.bodySm, { color: theme.color.text.sub }]}>
              현재 보유 도토리
            </Text>
            <View style={styles.priceRow}>
              <Image
                source={require('@/assets/images/icons/icon-dotori.png')}
                style={styles.acornIcon}
                resizeMode="contain"
              />
              <Text style={[theme.typography.label, { color: theme.color.text.strong }]}>
                {balanceAfter.toLocaleString('ko-KR')}
              </Text>
            </View>
          </View>
        ) : null}

        <Button label="포인트 받기" onPress={onClose} style={styles.fullWidth} />
      </View>
    </Screen>
  );
}

/** 3번을 다 썼지만 못 맞혔을 때의 전체화면 결과 — 성공 화면보다 차분한 톤으로 안내한다. */
function QuizResultExhausted({ theme, answer, explanation, onClose }: QuizResultProps) {
  return (
    <Screen
      header={<Header title="" onClose={onClose} backgroundColor={QUIZ_BACKGROUND} />}
      backgroundColor={QUIZ_BACKGROUND}
      scroll
    >
      <View style={[styles.resultBody, { gap: theme.space.xl, paddingTop: theme.space.xl }]}>
        <View style={{ gap: theme.space.xs, alignItems: 'center' }}>
          <Text style={[theme.typography.h2, styles.center, { color: theme.color.text.strong }]}>
            오늘은 여기까지예요
          </Text>
          <Text style={[theme.typography.body, styles.center, { color: theme.color.text.sub }]}>
            내일 다시 도전해봐요!
          </Text>
        </View>

        <ResultAnswerCard theme={theme} answer={answer} explanation={explanation} />

        <Button label="확인" onPress={onClose} style={styles.fullWidth} />
      </View>
    </Screen>
  );
}

function CenteredMessage({ text, theme }: { text: string; theme: Theme }) {
  return (
    <View style={styles.centered}>
      <Text style={[theme.typography.body, { color: theme.color.text.sub }]}>{text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  center: { textAlign: 'center' },
  attemptCard: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 14,
    paddingHorizontal: 16,
  },
  rewardBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  rewardIcon: { width: 16, height: 16 },
  mascot: { width: 96, height: 96 },
  mascotSpacing: { marginTop: 12, marginBottom: 24 },
  selfCenter: { alignSelf: 'center' },
  alignStart: { alignSelf: 'flex-start' },
  dangerBanner: { gap: 4 },
  hintBox: { gap: 6 },
  // 질문·해설은 긴 문장이 오는 자리라 줄간격을 넉넉히 — 붙은 줄은 아이가 읽다 놓친다
  question: { lineHeight: 28 },
  explanation: { lineHeight: 28 },
  // 초성은 글자 사이가 벌어져야 "ㅇㅈ" 같은 조합이 한 글자처럼 안 보인다
  hintText: { letterSpacing: 6 },
  dangerTitleRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  dangerSubtext: { marginLeft: 22 },
  resultBody: { alignItems: 'center' },
  mascotStage: { alignItems: 'center', justifyContent: 'center' },
  fanfare: { position: 'absolute', width: 320, height: 107, top: -56, zIndex: 0 },
  resultMascot: { width: 140, height: 140, zIndex: 1 },
  resultAnswerGroup: { gap: 12, width: '100%' },
  resultAnswerCard: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  balanceRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  priceRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  acornIcon: { width: 16, height: 16 },
  fullWidth: { width: '100%' },
});
