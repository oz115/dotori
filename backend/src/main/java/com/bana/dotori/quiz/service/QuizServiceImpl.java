package com.bana.dotori.quiz.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.dto.FamilySettings;
import com.bana.dotori.family.service.FamilySettingsReader;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.util.ChosungHint;
import com.bana.dotori.quiz.dto.QuizAttemptHistoryItem;
import com.bana.dotori.quiz.dto.QuizHistoryResponse;
import com.bana.dotori.quiz.dto.QuizParentAttemptItem;
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
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.service.PointWalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 경제 단어장 퀴즈 조회·제출·이력 (QUIZ-01·02·03).
 *
 * <p>제출은 최대 3회, 정답을 맞히면 그 즉시 종료된다. 3회를 다 써도 정답을 못 맞히면 그때
 * 정답을 공개한다 — Notion 명세의 "제출 즉시 정답 공개"·"재제출 금지"를 팀이 의도적으로
 * 바꾼 규칙이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/** 제출 최대 횟수. 정답을 맞히기 전까지 이 횟수만큼 재도전할 수 있다. */
	private static final int MAX_ATTEMPTS = 3;

	/**
	 * 퀴즈 1건당 지급 포인트 = 주간 보상 한도(weeklyRewardLimit)의 20% ÷ 7.
	 * 하루 퀴즈 1개 전제라 7일 다 맞혀도 정확히 20%만 쓰인다 — 별도 초과 체크가 필요 없다.
	 */
	private static final int QUIZ_REWARD_DIVISOR = 35;

	/**
	 * 보상 거래 설명. 문제 본문을 붙이지 않는다 — 거래내역 한 줄에 문제 전문이 들어가면
	 * 목록이 읽기 힘들어진다는 피드백. 어떤 퀴즈였는지는 퀴즈 이력 화면이 보여줄 몫이다.
	 */
	private static final String REWARD_DESCRIPTION = "퀴즈 정답 보상";

	private final QuizRepository quizRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final QuizAccessValidator accessValidator;
	private final FamilySettingsReader familySettingsReader;
	private final PointWalletService pointWalletService;
	private final PointWalletRepository pointWalletRepository;

	@Override
	@Transactional(readOnly = true)
	public QuizTodayResponse getTodayQuiz(Integer userId, Integer familyId, Integer childId) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireChild(userId);

		Quiz quiz = quizRepository.findByFamilyIdAndChildIdAndQuizDate(familyId, childId, LocalDate.now(KST))
			.orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_READY));

		List<QuizAttempt> attempts = quizAttemptRepository.findByQuizIdAndChildId(quiz.getId(), childId);
		boolean locked = isLocked(attempts);
		int rewardPoints = calculateRewardPoints(familyId, childId);

		return new QuizTodayResponse(
			quiz.getId(), quiz.getQuizDate(), quiz.getQuestion(), null, rewardPoints, locked,
			locked ? quiz.getCorrectAnswer() : null,
			locked ? quiz.getExplanation() : null,
			attempts.size(),
			hintFor(quiz, attempts.size(), locked));
	}

	/**
	 * 부모용 오늘의 퀴즈 확인. 정답·해설을 잠금 여부와 상관없이 내려주므로 부모만 부를 수 있다.
	 * 퀴즈 생성은 AI 배치 몫이라 여기서 만들지 않는다 — 아직 없으면 {@code QUIZ_NOT_READY}다.
	 */
	@Override
	@Transactional(readOnly = true)
	public QuizParentTodayResponse getTodayQuizForParent(Integer userId, Integer familyId, Integer childId) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireParent(userId);

		Quiz quiz = quizRepository.findByFamilyIdAndChildIdAndQuizDate(familyId, childId, LocalDate.now(KST))
			.orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_READY));

		// 시도 id 는 IDENTITY 로 제출 순서대로 커지므로 id 순이 곧 제출 순서다.
		List<QuizAttempt> attempts = quizAttemptRepository.findByQuizIdAndChildId(quiz.getId(), childId).stream()
			.sorted(Comparator.comparing(QuizAttempt::getId))
			.toList();
		List<QuizParentAttemptItem> items = new ArrayList<>();
		for (int i = 0; i < attempts.size(); i++) {
			QuizAttempt attempt = attempts.get(i);
			items.add(new QuizParentAttemptItem(attempt.getId(), i + 1, attempt.getSubmittedAnswer(),
				attempt.isCorrect(), attempt.getEarnedPoints(), attempt.getSubmittedAt()));
		}

		return new QuizParentTodayResponse(
			quiz.getId(), quiz.getQuizDate(), quiz.getQuestion(), quiz.getCorrectAnswer(), quiz.getExplanation(),
			attempts.size(), MAX_ATTEMPTS, attempts.stream().anyMatch(QuizAttempt::isCorrect), isLocked(attempts),
			items);
	}

	@Override
	@Transactional
	public QuizSubmitResponse submitQuiz(Integer userId, Integer familyId, Integer childId, Integer quizId,
		QuizSubmitRequest request) {
		accessValidator.validate(userId, familyId, childId);
		accessValidator.requireChild(userId);

		// 퀴즈 행을 잠근다 — 같은 퀴즈에 대한 동시 제출을 여기서 한 줄로 세운다.
		Quiz quiz = quizRepository.findByIdForUpdate(quizId)
			.orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
		if (!quiz.getFamilyId().equals(familyId) || !quiz.getChildId().equals(childId)) {
			// 존재는 하지만 남의 가족/자녀 퀴즈다. 404로 가려 남의 quizId가 유효한지 새어 나가지 않게 한다.
			throw new BusinessException(ErrorCode.QUIZ_NOT_FOUND);
		}

		List<QuizAttempt> attempts = quizAttemptRepository.findByQuizIdAndChildId(quiz.getId(), childId);
		if (attempts.stream().anyMatch(QuizAttempt::isCorrect)) {
			throw new BusinessException(ErrorCode.ALREADY_SUBMITTED);
		}
		if (attempts.size() >= MAX_ATTEMPTS) {
			throw new BusinessException(ErrorCode.QUIZ_ATTEMPT_LIMIT_EXCEEDED);
		}

		boolean isCorrect = isAnswerCorrect(request.answer(), quiz.getCorrectAnswer());
		int attemptNumber = attempts.size() + 1;
		boolean reveal = isCorrect || attemptNumber == MAX_ATTEMPTS;

		int earnedPoints = 0;
		int balanceAfter;
		if (isCorrect) {
			earnedPoints = calculateRewardPoints(familyId, childId);
			pointWalletService.createWalletIfAbsent(childId);
			PointChangeResult result = pointWalletService.deposit(PointChangeCommand.of(
				childId, earnedPoints, PointTransactionReason.QUIZ_REWARD,
				REWARD_DESCRIPTION, quiz.rewardIdempotencyKey()));
			if (result.replayed()) {
				// 퀴즈 행 락이 이미 동시 제출을 막아서 정상 흐름에서는 오지 않는다. 대비용 방어다.
				throw new BusinessException(ErrorCode.ALREADY_SUBMITTED,
					"이미 지급된 퀴즈입니다. quizId=" + quiz.getId() + ", childId=" + childId);
			}
			balanceAfter = result.balanceAfter();
		} else {
			balanceAfter = currentBalance(childId);
		}

		QuizAttempt attempt = quizAttemptRepository.save(QuizAttempt.builder()
			.quizId(quiz.getId())
			.childId(childId)
			.submittedAnswer(request.answer())
			.correct(isCorrect)
			.earnedPoints(earnedPoints)
			.build());

		log.info("퀴즈 제출 완료. familyId={}, childId={}, quizId={}, attemptNumber={}, isCorrect={}, earnedPoints={}",
			familyId, childId, quiz.getId(), attemptNumber, isCorrect, earnedPoints);

		return new QuizSubmitResponse(
			attempt.getId(), isCorrect,
			reveal ? quiz.getCorrectAnswer() : null,
			reveal ? quiz.getExplanation() : null,
			earnedPoints, balanceAfter,
			hintFor(quiz, attemptNumber, reveal));
	}

	@Override
	@Transactional(readOnly = true)
	public QuizHistoryResponse getHistory(Integer userId, Integer familyId, Integer childId, LocalDate startDate,
		LocalDate endDate) {
		// QUIZ-01은 "사용자: 공통" — 자녀 본인 또는 같은 가족 부모가 볼 수 있고, validate()가
		// 그 규칙을 그대로 강제한다(부모는 제한 없음, 자녀는 본인만).
		accessValidator.validate(userId, familyId, childId);

		List<QuizAttemptHistoryItem> attempts = quizAttemptRepository.findHistory(childId, startDate, endDate);
		return new QuizHistoryResponse(attempts);
	}

	/**
	 * 초성 힌트. 한 번이라도 풀어 봤고(attemptCount >= 1) 아직 정답이 공개되지 않았을 때만 준다.
	 * 정답이 공개되는 상태에서는 힌트가 필요 없고, 한 번도 안 풀었으면 먼저 스스로 풀어 보게 한다.
	 */
	private String hintFor(Quiz quiz, int attemptCount, boolean revealed) {
		if (attemptCount < 1 || revealed) {
			return null;
		}
		return ChosungHint.of(quiz.getCorrectAnswer());
	}

	private boolean isLocked(List<QuizAttempt> attempts) {
		boolean solved = attempts.stream().anyMatch(QuizAttempt::isCorrect);
		return solved || attempts.size() >= MAX_ATTEMPTS;
	}

	private boolean isAnswerCorrect(String answer, String correctAnswer) {
		return answer.trim().equalsIgnoreCase(correctAnswer.trim());
	}

	private int calculateRewardPoints(Integer familyId, Integer childId) {
		FamilySettings settings = familySettingsReader.read(familyId, childId);
		return settings.weeklyRewardLimit() / QUIZ_REWARD_DIVISOR;
	}

	private int currentBalance(Integer childId) {
		return pointWalletRepository.findByChildId(childId).map(PointWallet::getBalance).orElse(0);
	}
}
