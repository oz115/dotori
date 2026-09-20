package com.bana.dotori.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bana.dotori.family.dto.FamilySettings;
import com.bana.dotori.family.entity.FinancialKnowledgeLevel;
import com.bana.dotori.family.service.FamilySettingsReader;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.quiz.dto.QuizAttemptHistoryItem;
import com.bana.dotori.quiz.dto.QuizHistoryResponse;
import com.bana.dotori.quiz.dto.QuizParentTodayResponse;
import com.bana.dotori.quiz.dto.QuizSubmitRequest;
import com.bana.dotori.quiz.dto.QuizSubmitResponse;
import com.bana.dotori.quiz.dto.QuizTodayResponse;
import com.bana.dotori.quiz.entity.Quiz;
import com.bana.dotori.quiz.entity.QuizAttempt;
import com.bana.dotori.quiz.repository.QuizAttemptRepository;
import com.bana.dotori.quiz.repository.QuizRepository;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransaction;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.service.PointWalletService;

@ExtendWith(MockitoExtension.class)
class QuizServiceImplTest {

	private static final Integer FAMILY_ID = 10;
	private static final Integer CHILD_ID = 2;
	private static final Integer QUIZ_ID = 100;
	// weeklyRewardLimit 3500 -> 퀴즈 1건당 3500/35 = 100포인트
	private static final int WEEKLY_REWARD_LIMIT = 3500;
	private static final int QUIZ_REWARD_POINTS = 100;

	@Mock
	private QuizRepository quizRepository;

	@Mock
	private QuizAttemptRepository quizAttemptRepository;

	@Mock
	private QuizAccessValidator accessValidator;

	@Mock
	private FamilySettingsReader familySettingsReader;

	@Mock
	private PointWalletService pointWalletService;

	@Mock
	private PointWalletRepository pointWalletRepository;

	@InjectMocks
	private QuizServiceImpl quizService;

	private Quiz quiz() {
		Quiz quiz = Quiz.builder()
			.familyId(FAMILY_ID)
			.childId(CHILD_ID)
			.wordId(1)
			.question("'이자'란 무엇일까요?")
			.correctAnswer("이자")
			.explanation("돈을 맡기거나 빌려줘서 받는 보상이에요.")
			.quizDate(LocalDate.now())
			.build();
		ReflectionTestUtils.setField(quiz, "id", QUIZ_ID);
		return quiz;
	}

	private QuizAttempt attempt(boolean correct) {
		QuizAttempt attempt = QuizAttempt.builder()
			.quizId(QUIZ_ID)
			.childId(CHILD_ID)
			.submittedAnswer(correct ? "이자" : "수수료")
			.correct(correct)
			.earnedPoints(correct ? QUIZ_REWARD_POINTS : 0)
			.build();
		ReflectionTestUtils.setField(attempt, "id", 1);
		return attempt;
	}

	private void givenWeeklyRewardLimit() {
		BDDMockito.given(familySettingsReader.read(FAMILY_ID, CHILD_ID))
			.willReturn(new FamilySettings(10000, WEEKLY_REWARD_LIMIT, FinancialKnowledgeLevel.BEGINNER));
	}

	@Nested
	@DisplayName("오늘의 퀴즈 조회")
	class GetTodayQuiz {

		@Test
		@DisplayName("시도가 없으면 잠기지 않은 상태로, 정답은 숨긴 채 내려준다")
		void 시도_없으면_공개하지_않는다() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.of(quiz()));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of());
			givenWeeklyRewardLimit();

			QuizTodayResponse response = quizService.getTodayQuiz(CHILD_ID, FAMILY_ID, CHILD_ID);

			assertThat(response.quizId()).isEqualTo(QUIZ_ID);
			assertThat(response.alreadySubmitted()).isFalse();
			assertThat(response.rewardPoints()).isEqualTo(QUIZ_REWARD_POINTS);
			assertThat(response.correctAnswer()).isNull();
			assertThat(response.explanation()).isNull();
			assertThat(response.choices()).isNull();
			assertThat(response.hint()).isNull();
		}

		@Test
		@DisplayName("한 번 틀렸고 재도전할 수 있으면 정답은 숨긴 채 초성 힌트를 준다")
		void 오답_뒤_재도전_가능하면_초성_힌트() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.of(quiz()));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of(attempt(false)));
			givenWeeklyRewardLimit();

			QuizTodayResponse response = quizService.getTodayQuiz(CHILD_ID, FAMILY_ID, CHILD_ID);

			assertThat(response.alreadySubmitted()).isFalse();
			assertThat(response.hint()).isEqualTo("ㅇㅈ");
			assertThat(response.correctAnswer()).isNull();
		}

		@Test
		@DisplayName("정답이 공개되는 상태에서는 힌트를 주지 않는다")
		void 정답_공개되면_힌트_없다() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.of(quiz()));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of(attempt(false), attempt(true)));
			givenWeeklyRewardLimit();

			QuizTodayResponse response = quizService.getTodayQuiz(CHILD_ID, FAMILY_ID, CHILD_ID);

			assertThat(response.alreadySubmitted()).isTrue();
			assertThat(response.correctAnswer()).isEqualTo("이자");
			assertThat(response.hint()).isNull();
		}

		@Test
		@DisplayName("오늘자 퀴즈가 없으면 QUIZ_NOT_READY 예외가 발생한다")
		void 퀴즈_없으면_예외() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.empty());

			assertThatThrownBy(() -> quizService.getTodayQuiz(CHILD_ID, FAMILY_ID, CHILD_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_READY);
		}

		@Test
		@DisplayName("정답을 맞힌 시도가 있으면 잠긴 상태로 정답을 공개한다")
		void 정답_맞히면_공개() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.of(quiz()));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of(attempt(false), attempt(true)));
			givenWeeklyRewardLimit();

			QuizTodayResponse response = quizService.getTodayQuiz(CHILD_ID, FAMILY_ID, CHILD_ID);

			assertThat(response.alreadySubmitted()).isTrue();
			assertThat(response.correctAnswer()).isEqualTo("이자");
			assertThat(response.explanation()).isEqualTo("돈을 맡기거나 빌려줘서 받는 보상이에요.");
		}

		@Test
		@DisplayName("3회 다 오답이면 잠긴 상태로 정답을 공개한다")
		void 세번_다_오답이면_공개() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.of(quiz()));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of(attempt(false), attempt(false), attempt(false)));
			givenWeeklyRewardLimit();

			QuizTodayResponse response = quizService.getTodayQuiz(CHILD_ID, FAMILY_ID, CHILD_ID);

			assertThat(response.alreadySubmitted()).isTrue();
			assertThat(response.correctAnswer()).isEqualTo("이자");
		}
	}

	@Nested
	@DisplayName("부모용 오늘의 퀴즈 확인")
	class GetTodayQuizForParent {

		private static final Integer PARENT_ID = 1;

		private QuizAttempt attemptWithId(int id, String answer, boolean correct) {
			QuizAttempt attempt = QuizAttempt.builder()
				.quizId(QUIZ_ID)
				.childId(CHILD_ID)
				.submittedAnswer(answer)
				.correct(correct)
				.earnedPoints(correct ? QUIZ_REWARD_POINTS : 0)
				.build();
			ReflectionTestUtils.setField(attempt, "id", id);
			return attempt;
		}

		@Test
		@DisplayName("아직 안 풀었어도 문제와 정답·해설을 내려주고 제출 내역은 비어 있다")
		void 안_풀었어도_정답을_준다() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.of(quiz()));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of());

			QuizParentTodayResponse response = quizService.getTodayQuizForParent(PARENT_ID, FAMILY_ID, CHILD_ID);

			assertThat(response.quizId()).isEqualTo(QUIZ_ID);
			assertThat(response.question()).isEqualTo("'이자'란 무엇일까요?");
			assertThat(response.correctAnswer()).isEqualTo("이자");
			assertThat(response.explanation()).isEqualTo("돈을 맡기거나 빌려줘서 받는 보상이에요.");
			assertThat(response.attemptCount()).isZero();
			assertThat(response.maxAttempts()).isEqualTo(3);
			assertThat(response.solved()).isFalse();
			assertThat(response.locked()).isFalse();
			assertThat(response.attempts()).isEmpty();
		}

		@Test
		@DisplayName("오답 뒤 정답이면 제출 순서대로 답과 정오답을 내려주고 잠긴 상태로 본다")
		void 제출_내역을_순서대로_준다() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.of(quiz()));
			// 리포지토리 순서와 무관하게 id(=제출) 순으로 정렬돼야 한다
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of(attemptWithId(12, "이자", true), attemptWithId(11, "수수료", false)));

			QuizParentTodayResponse response = quizService.getTodayQuizForParent(PARENT_ID, FAMILY_ID, CHILD_ID);

			assertThat(response.attemptCount()).isEqualTo(2);
			assertThat(response.solved()).isTrue();
			assertThat(response.locked()).isTrue();
			assertThat(response.attempts()).hasSize(2);
			assertThat(response.attempts().get(0).attemptNumber()).isEqualTo(1);
			assertThat(response.attempts().get(0).submittedAnswer()).isEqualTo("수수료");
			assertThat(response.attempts().get(0).isCorrect()).isFalse();
			assertThat(response.attempts().get(1).attemptNumber()).isEqualTo(2);
			assertThat(response.attempts().get(1).submittedAnswer()).isEqualTo("이자");
			assertThat(response.attempts().get(1).isCorrect()).isTrue();
			assertThat(response.attempts().get(1).earnedPoints()).isEqualTo(QUIZ_REWARD_POINTS);
		}

		@Test
		@DisplayName("3회 다 오답이면 맞히진 못했지만 잠긴 상태다")
		void 세번_다_오답이면_잠긴다() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.of(quiz()));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of(attemptWithId(1, "a", false), attemptWithId(2, "b", false),
					attemptWithId(3, "c", false)));

			QuizParentTodayResponse response = quizService.getTodayQuizForParent(PARENT_ID, FAMILY_ID, CHILD_ID);

			assertThat(response.solved()).isFalse();
			assertThat(response.locked()).isTrue();
			assertThat(response.attemptCount()).isEqualTo(3);
		}

		@Test
		@DisplayName("2회 오답이고 기회가 남아 있으면 잠기지 않은 채 제출 내역을 준다")
		void 기회가_남았으면_잠기지_않는다() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.of(quiz()));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of(attemptWithId(1, "a", false), attemptWithId(2, "b", false)));

			QuizParentTodayResponse response = quizService.getTodayQuizForParent(PARENT_ID, FAMILY_ID, CHILD_ID);

			assertThat(response.locked()).isFalse();
			assertThat(response.attempts()).hasSize(2);
		}

		@Test
		@DisplayName("오늘자 퀴즈가 없으면 QUIZ_NOT_READY 예외가 발생한다")
		void 퀴즈_없으면_예외() {
			BDDMockito.given(quizRepository.findByFamilyIdAndChildIdAndQuizDate(any(), any(), any()))
				.willReturn(Optional.empty());

			assertThatThrownBy(() -> quizService.getTodayQuizForParent(PARENT_ID, FAMILY_ID, CHILD_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_READY);
		}

		@Test
		@DisplayName("부모가 아니면 퀴즈를 조회하기 전에 막는다")
		void 부모가_아니면_조회하지_않는다() {
			BDDMockito.willThrow(new BusinessException(ErrorCode.FORBIDDEN))
				.given(accessValidator).requireParent(CHILD_ID);

			assertThatThrownBy(() -> quizService.getTodayQuizForParent(CHILD_ID, FAMILY_ID, CHILD_ID))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

			BDDMockito.then(quizRepository).shouldHaveNoInteractions();
			BDDMockito.then(quizAttemptRepository).shouldHaveNoInteractions();
		}
	}

	@Nested
	@DisplayName("퀴즈 제출")
	class SubmitQuiz {

		@Test
		@DisplayName("첫 시도에 정답이면 포인트를 지급하고 정답을 공개한다")
		void 첫시도_정답() {
			Quiz quiz = quiz();
			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.of(quiz));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID)).willReturn(List.of());
			givenWeeklyRewardLimit();
			BDDMockito.given(pointWalletService.deposit(any(PointChangeCommand.class)))
				.willReturn(PointChangeResult.applied(PointTransaction.record(
					CHILD_ID, PointTransactionReason.QUIZ_REWARD, QUIZ_REWARD_POINTS, 17600, "퀴즈 보상", "quiz:100")));
			BDDMockito.given(quizAttemptRepository.save(any(QuizAttempt.class)))
				.willAnswer(invocation -> {
					QuizAttempt saved = invocation.getArgument(0);
					ReflectionTestUtils.setField(saved, "id", 1);
					return saved;
				});

			QuizSubmitResponse response =
				quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID, new QuizSubmitRequest(" 이자 "));

			assertThat(response.isCorrect()).isTrue();
			assertThat(response.earnedPoints()).isEqualTo(QUIZ_REWARD_POINTS);
			assertThat(response.correctAnswer()).isEqualTo("이자");
			assertThat(response.explanation()).isEqualTo("돈을 맡기거나 빌려줘서 받는 보상이에요.");
			assertThat(response.pointBalanceAfter()).isEqualTo(17600);

			BDDMockito.then(pointWalletService).should()
				.deposit(BDDMockito.argThat(command -> command.reason() == PointTransactionReason.QUIZ_REWARD
					&& command.amount() == QUIZ_REWARD_POINTS
					&& "quiz:100".equals(command.idempotencyKey())));
		}

		@Test
		@DisplayName("공백·대소문자만 다른 정답도 정답으로 인정한다")
		void 대소문자_공백_무시() {
			Quiz quiz = Quiz.builder()
				.familyId(FAMILY_ID).childId(CHILD_ID).wordId(1)
				.question("Q").correctAnswer("Interest").explanation("설명")
				.quizDate(LocalDate.now()).build();
			ReflectionTestUtils.setField(quiz, "id", QUIZ_ID);

			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.of(quiz));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID)).willReturn(List.of());
			givenWeeklyRewardLimit();
			BDDMockito.given(pointWalletService.deposit(any(PointChangeCommand.class)))
				.willReturn(PointChangeResult.applied(PointTransaction.record(
					CHILD_ID, PointTransactionReason.QUIZ_REWARD, QUIZ_REWARD_POINTS, 100, "d", "k")));
			BDDMockito.given(quizAttemptRepository.save(any(QuizAttempt.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			QuizSubmitResponse response =
				quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID, new QuizSubmitRequest("  interest  "));

			assertThat(response.isCorrect()).isTrue();
		}

		@Test
		@DisplayName("문제가 아주 길어도 보상 설명을 컬럼 길이(255자) 안으로 잘라 지급한다")
		void 긴_문제도_보상_설명이_잘려서_지급() {
			Quiz quiz = quiz();
			ReflectionTestUtils.setField(quiz, "question", "가".repeat(400));
			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.of(quiz));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID)).willReturn(List.of());
			givenWeeklyRewardLimit();
			BDDMockito.given(pointWalletService.deposit(any(PointChangeCommand.class)))
				.willReturn(PointChangeResult.applied(PointTransaction.record(
					CHILD_ID, PointTransactionReason.QUIZ_REWARD, QUIZ_REWARD_POINTS, 17600, "퀴즈 보상", "quiz:100")));
			BDDMockito.given(quizAttemptRepository.save(any(QuizAttempt.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			QuizSubmitResponse response =
				quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID, new QuizSubmitRequest("이자"));

			assertThat(response.isCorrect()).isTrue();
			BDDMockito.then(pointWalletService).should()
				.deposit(BDDMockito.argThat(command -> command.description().length() == 255
					&& command.description().startsWith("퀴즈 정답 보상: ")
					&& command.description().endsWith("…")));
		}

		@Test
		@DisplayName("짧은 문제의 보상 설명은 자르지 않는다")
		void 짧은_문제는_설명을_자르지_않는다() {
			Quiz quiz = quiz();
			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.of(quiz));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID)).willReturn(List.of());
			givenWeeklyRewardLimit();
			BDDMockito.given(pointWalletService.deposit(any(PointChangeCommand.class)))
				.willReturn(PointChangeResult.applied(PointTransaction.record(
					CHILD_ID, PointTransactionReason.QUIZ_REWARD, QUIZ_REWARD_POINTS, 17600, "퀴즈 보상", "quiz:100")));
			BDDMockito.given(quizAttemptRepository.save(any(QuizAttempt.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID, new QuizSubmitRequest("이자"));

			BDDMockito.then(pointWalletService).should()
				.deposit(BDDMockito.argThat(command ->
					"퀴즈 정답 보상: '이자'란 무엇일까요?".equals(command.description())));
		}

		@Test
		@DisplayName("첫 시도 오답이면 포인트를 지급하지 않고 정답을 숨긴다")
		void 첫시도_오답() {
			Quiz quiz = quiz();
			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.of(quiz));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID)).willReturn(List.of());
			BDDMockito.given(pointWalletRepository.findByChildId(CHILD_ID))
				.willReturn(Optional.of(PointWallet.openWithBalance(CHILD_ID, 5000)));
			BDDMockito.given(quizAttemptRepository.save(any(QuizAttempt.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			QuizSubmitResponse response =
				quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID, new QuizSubmitRequest("수수료"));

			assertThat(response.isCorrect()).isFalse();
			assertThat(response.earnedPoints()).isZero();
			assertThat(response.correctAnswer()).isNull();
			assertThat(response.explanation()).isNull();
			assertThat(response.pointBalanceAfter()).isEqualTo(5000);
			// 답은 숨기되 재도전을 돕도록 초성만 준다
			assertThat(response.hint()).isEqualTo("ㅇㅈ");
			BDDMockito.then(pointWalletService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("세 번째 시도는 오답이어도 정답을 공개한다")
		void 세번째_시도는_오답이어도_공개() {
			Quiz quiz = quiz();
			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.of(quiz));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of(attempt(false), attempt(false)));
			BDDMockito.given(pointWalletRepository.findByChildId(CHILD_ID))
				.willReturn(Optional.of(PointWallet.openWithBalance(CHILD_ID, 0)));
			BDDMockito.given(quizAttemptRepository.save(any(QuizAttempt.class)))
				.willAnswer(invocation -> invocation.getArgument(0));

			QuizSubmitResponse response =
				quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID, new QuizSubmitRequest("수수료"));

			assertThat(response.isCorrect()).isFalse();
			assertThat(response.correctAnswer()).isEqualTo("이자");
			// 정답이 공개되는 마지막 시도에는 힌트가 필요 없다
			assertThat(response.hint()).isNull();
			assertThat(response.explanation()).isEqualTo("돈을 맡기거나 빌려줘서 받는 보상이에요.");
		}

		@Test
		@DisplayName("이미 정답 처리된 퀴즈를 다시 제출하면 ALREADY_SUBMITTED 예외가 발생한다")
		void 이미_정답이면_예외() {
			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.of(quiz()));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of(attempt(true)));

			assertThatThrownBy(() -> quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID,
				new QuizSubmitRequest("이자")))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_SUBMITTED);

			BDDMockito.then(quizAttemptRepository).should(BDDMockito.never()).save(any());
		}

		@Test
		@DisplayName("3회를 다 오답으로 쓰고 또 제출하면 QUIZ_ATTEMPT_LIMIT_EXCEEDED 예외가 발생한다")
		void 횟수_소진하면_예외() {
			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.of(quiz()));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID))
				.willReturn(List.of(attempt(false), attempt(false), attempt(false)));

			assertThatThrownBy(() -> quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID,
				new QuizSubmitRequest("이자")))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_ATTEMPT_LIMIT_EXCEEDED);

			BDDMockito.then(quizAttemptRepository).should(BDDMockito.never()).save(any());
		}

		@Test
		@DisplayName("존재하지 않는 quizId면 QUIZ_NOT_FOUND 예외가 발생한다")
		void 없는_퀴즈는_예외() {
			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.empty());

			assertThatThrownBy(() -> quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID,
				new QuizSubmitRequest("이자")))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_FOUND);
		}

		@Test
		@DisplayName("남의 가족·자녀 퀴즈면 QUIZ_NOT_FOUND 예외가 발생한다")
		void 남의_퀴즈는_예외() {
			Quiz othersQuiz = Quiz.builder()
				.familyId(999).childId(999).wordId(1)
				.question("Q").correctAnswer("A").quizDate(LocalDate.now()).build();
			ReflectionTestUtils.setField(othersQuiz, "id", QUIZ_ID);
			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.of(othersQuiz));

			assertThatThrownBy(() -> quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID,
				new QuizSubmitRequest("이자")))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.QUIZ_NOT_FOUND);
		}

		@Test
		@DisplayName("지갑 지급이 멱등키로 재생되면 ALREADY_SUBMITTED 예외가 발생한다")
		void 지갑_재생이면_예외() {
			Quiz quiz = quiz();
			BDDMockito.given(quizRepository.findByIdForUpdate(QUIZ_ID)).willReturn(Optional.of(quiz));
			BDDMockito.given(quizAttemptRepository.findByQuizIdAndChildId(QUIZ_ID, CHILD_ID)).willReturn(List.of());
			givenWeeklyRewardLimit();
			BDDMockito.given(pointWalletService.deposit(any(PointChangeCommand.class)))
				.willReturn(PointChangeResult.replayed(PointTransaction.record(
					CHILD_ID, PointTransactionReason.QUIZ_REWARD, QUIZ_REWARD_POINTS, 100, "d", "quiz:100")));

			assertThatThrownBy(() -> quizService.submitQuiz(CHILD_ID, FAMILY_ID, CHILD_ID, QUIZ_ID,
				new QuizSubmitRequest("이자")))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_SUBMITTED);

			BDDMockito.then(quizAttemptRepository).should(BDDMockito.never()).save(any());
		}
	}

	@Nested
	@DisplayName("퀴즈 이력 조회")
	class GetHistory {

		@Test
		@DisplayName("리포지토리가 돌려준 이력을 그대로 내려준다")
		void 이력_그대로_반환() {
			QuizAttemptHistoryItem item = new QuizAttemptHistoryItem(1, LocalDate.now(), "Q", "이자", true, 100,
				java.time.OffsetDateTime.now());
			BDDMockito.given(quizAttemptRepository.findHistory(CHILD_ID, null, null)).willReturn(List.of(item));

			QuizHistoryResponse response = quizService.getHistory(CHILD_ID, FAMILY_ID, CHILD_ID, null, null);

			assertThat(response.attempts()).containsExactly(item);
		}
	}
}
