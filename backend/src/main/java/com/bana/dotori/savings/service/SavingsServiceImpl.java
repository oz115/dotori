package com.bana.dotori.savings.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bana.dotori.family.dto.FamilySettings;
import com.bana.dotori.family.service.FamilySettingsReader;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.savings.dto.request.SavingsContributeRequest;
import com.bana.dotori.savings.dto.request.SavingsSubscribeRequest;
import com.bana.dotori.savings.dto.response.SavingsExpectedReturnResponse;
import com.bana.dotori.savings.dto.response.SavingsProductResponse;
import com.bana.dotori.savings.dto.response.SavingsSubscriptionResponse;
import com.bana.dotori.savings.dto.response.SavingsSummaryResponse;
import com.bana.dotori.savings.dto.response.SavingsTransactionResponse;
import com.bana.dotori.savings.entity.ContributionStyle;
import com.bana.dotori.savings.entity.PayoutTiming;
import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsSubscription;
import com.bana.dotori.savings.entity.SavingsSubscriptionStatus;
import com.bana.dotori.savings.entity.SavingsTransaction;
import com.bana.dotori.savings.entity.SavingsTransactionType;
import com.bana.dotori.savings.mapper.SavingsMapper;
import com.bana.dotori.savings.repository.SavingsProductRepository;
import com.bana.dotori.savings.repository.SavingsSubscriptionRepository;
import com.bana.dotori.savings.repository.SavingsTransactionRepository;
import com.bana.dotori.wallet.dto.command.PointChangeCommand;
import com.bana.dotori.wallet.dto.response.PointChangeResult;
import com.bana.dotori.wallet.entity.PointTransactionReason;
import com.bana.dotori.wallet.service.PointWalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 예적금 상품 조회·가입·납입·수령·해지. 부모의 승인이나 커스터마이징이 없는 완전 자동 상품이라
 * (6개 고정) 여기서 하는 일은 "가족 RL 로 한도를 계산하고, 그 안에서 지갑과 가입 건 사이로
 * 포인트를 옮기는" 것뿐이다.
 *
 * <p>주간 이자 적립·만기 도달 판정은 {@link SavingsSettlementService} 가 맡는다. 시간이 흘러서
 * 일어나는 일과 아이가 눌러서 일어나는 일을 갈라 둔 것이다.
 *
 * <p><b>포인트가 움직이는 모든 경로는 가입 건 행을 먼저 잠근다.</b> 지갑 락만으로는 부족하다 —
 * 지갑은 포인트가 두 번 빠지는 것만 막고, 목표 금액·납입 회차 같은 가입 건의 한도는 지갑이
 * 알지 못한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsServiceImpl implements SavingsService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/** 부모 홈 요약이 "아이의 예적금 잔액"으로 세는 상태 */
	private static final Set<SavingsSubscriptionStatus> SUMMARY_STATUSES =
		EnumSet.of(SavingsSubscriptionStatus.ACTIVE, SavingsSubscriptionStatus.MATURED);

	private final SavingsProductRepository savingsProductRepository;
	private final SavingsSubscriptionRepository savingsSubscriptionRepository;
	private final SavingsTransactionRepository savingsTransactionRepository;
	private final SavingsAccessValidator savingsAccessValidator;
	private final FamilySettingsReader familySettingsReader;
	private final PointWalletService pointWalletService;
	private final SavingsExpectedReturnCalculator expectedReturnCalculator;
	private final SavingsMapper savingsMapper;

	@Override
	@Transactional(readOnly = true)
	public List<SavingsProductResponse> listProducts(Integer userId, Integer familyId, Integer childId) {
		savingsAccessValidator.validate(userId, familyId, childId);
		FamilySettings familySettings = familySettingsReader.read(familyId, childId);

		return savingsProductRepository.findByActiveIsTrue().stream()
			.map(product -> savingsMapper.toProductResponse(product, familySettings))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public SavingsProductResponse getProduct(Integer userId, Integer familyId, Integer childId,
		Integer savingsProductId) {
		savingsAccessValidator.validate(userId, familyId, childId);
		FamilySettings familySettings = familySettingsReader.read(familyId, childId);

		return savingsMapper.toProductResponse(findProduct(savingsProductId), familySettings);
	}

	/**
	 * 가입. 가입 즉시 최초 납입액이 지갑에서 빠진다 (목표적금만 0 — 목표만 정하고 납입은 이후다).
	 *
	 * <p><b>재시도로 같은 멱등키가 다시 오면 새 가입을 만들지 않고 먼저 만든 가입을 그대로
	 * 돌려준다.</b> 지갑은 멱등키로 이중 차감을 막아 주지만 가입 행까지 막아 주지는 못해서,
	 * 그것만 믿으면 포인트는 한 번 빠졌는데 가입은 두 건이 된다.
	 */
	@Override
	@Transactional
	public SavingsSubscriptionResponse subscribe(Integer userId, Integer familyId, Integer childId,
		SavingsSubscribeRequest request, String idempotencyKey) {
		// 가입은 아이 본인만 — 부모는 조회만 하고 개입하지 않는다 (컨트롤러에서 본인 확인을 먼저 한다).
		savingsAccessValidator.validate(userId, familyId, childId);

		Optional<SavingsSubscriptionResponse> replayed = findAlreadySubscribed(childId, idempotencyKey);
		if (replayed.isPresent()) {
			return replayed.get();
		}

		SavingsProduct product = findProduct(request.savingsProductId());
		if (!product.isActive()) {
			throw new BusinessException(ErrorCode.SAVINGS_PRODUCT_INACTIVE,
				"savingsProductId=" + product.getId());
		}

		FamilySettings familySettings = familySettingsReader.read(familyId, childId);
		long maxAmount = product.getProductType().calculateMaxAmount(familySettings);
		if (request.amount() > maxAmount) {
			throw new BusinessException(ErrorCode.SAVINGS_AMOUNT_EXCEEDS_LIMIT,
				"요청 금액=" + request.amount() + ", 가입 가능 최대 금액=" + maxAmount);
		}

		ContributionStyle contributionStyle = product.getContributionStyle();
		int initialDeposit = contributionStyle.initialDeposit(request.amount());

		OffsetDateTime startedAt = OffsetDateTime.now(KST);
		Integer termWeeks = product.getTermWeeks();
		OffsetDateTime maturityAt = termWeeks == null ? null : startedAt.plusWeeks(termWeeks);

		SavingsSubscription subscription = SavingsSubscription.subscribe(familyId, childId, product.getId(),
			request.amount(), initialDeposit, contributionStyle.countsInitialDepositAsInstallment(), idempotencyKey,
			startedAt, maturityAt, startedAt.plusWeeks(1));
		try {
			// 거래 이력이 가입 id 를 필요로 하고, 중복 가입을 막는 유니크 제약도 이 시점에 걸려야 한다.
			savingsSubscriptionRepository.saveAndFlush(subscription);
		} catch (DataIntegrityViolationException e) {
			// 같은 멱등키가 동시에 둘 들어오면 위의 조회는 둘 다 "없음"으로 보고 지나간다. 뒤늦게 유니크
			// 제약에 걸린 쪽은 409 로 돌려보낸다 — 먼저 들어온 트랜잭션이 아직 커밋 전이라 그 가입을
			// 읽어 돌려줄 수가 없다. 클라이언트가 같은 키로 다시 부르면 그때는 조회에 걸려 그대로 받는다.
			throw new BusinessException(ErrorCode.DUPLICATE_POINT_REQUEST,
				"같은 요청이 이미 처리 중입니다. childId=" + childId + ", idempotencyKey=" + idempotencyKey, e);
		}

		if (initialDeposit > 0) {
			withdrawFromWallet(childId, initialDeposit, product.getName() + " 가입", idempotencyKey);
			record(subscription, SavingsTransactionType.INITIAL_DEPOSIT, initialDeposit);
		}

		log.info("예적금 가입 완료. familyId={}, childId={}, productType={}, principal={}, 최초납입={}",
			familyId, childId, product.getProductType(), request.amount(), initialDeposit);

		return savingsMapper.toSubscriptionResponse(subscription, product);
	}

	/**
	 * 추가 납입. 금액 규칙은 상품의 납입 방식이 정한다 ({@link #validateContribution}).
	 *
	 * <p>목표적금이 이 납입으로 목표를 채우면 그 자리에서 이자를 얹어 지급까지 끝낸다 —
	 * 목표 달성 즉시 지급이 이 상품의 규칙이라 만기를 기다리게 할 이유가 없다.
	 */
	@Override
	@Transactional
	public SavingsSubscriptionResponse contribute(Integer userId, Integer familyId, Integer childId,
		Integer savingsSubscriptionId, SavingsContributeRequest request, String idempotencyKey) {
		savingsAccessValidator.validate(userId, familyId, childId);

		SavingsSubscription subscription = findForUpdate(familyId, childId, savingsSubscriptionId);
		SavingsProduct product = findProduct(subscription.getSavingsProductId());
		if (!subscription.isActive()) {
			throw new BusinessException(ErrorCode.SAVINGS_SUBSCRIPTION_NOT_ACTIVE,
				"savingsSubscriptionId=" + savingsSubscriptionId + ", status=" + subscription.getStatus());
		}

		validateContribution(subscription, product, familyId, childId, request.amount());

		withdrawFromWallet(childId, request.amount(), product.getName() + " 납입", idempotencyKey);
		subscription.contribute(request.amount());
		record(subscription, SavingsTransactionType.CONTRIBUTION, request.amount());

		if (product.getPayoutTiming() == PayoutTiming.ON_GOAL_ACHIEVED && subscription.hasReachedGoal()) {
			payOutOnGoalAchieved(subscription, product, idempotencyKey);
		}

		return savingsMapper.toSubscriptionResponse(subscription, product);
	}

	/**
	 * 만기 수령. 만기에 도달해 지급 대기(MATURED) 중인 건만 받을 수 있다.
	 *
	 * <p>보통예금은 만기가 없어서 이 경로로 오지 않는다 — 꺼낼 때는 해지({@link #terminate})를 쓴다.
	 * 이자를 이미 매주 받아 갔으므로 해지해도 잃는 것이 없다.
	 */
	@Override
	@Transactional
	public SavingsSubscriptionResponse receive(Integer userId, Integer familyId, Integer childId,
		Integer savingsSubscriptionId, String idempotencyKey) {
		savingsAccessValidator.validate(userId, familyId, childId);

		SavingsSubscription subscription = findForUpdate(familyId, childId, savingsSubscriptionId);
		SavingsProduct product = findProduct(subscription.getSavingsProductId());

		if (subscription.getStatus() != SavingsSubscriptionStatus.MATURED) {
			throw new BusinessException(ErrorCode.SAVINGS_NOT_MATURED,
				"savingsSubscriptionId=" + savingsSubscriptionId + ", status=" + subscription.getStatus()
					+ ", maturityAt=" + subscription.getMaturityAt());
		}

		int payout = subscription.payoutAmount();
		// 상태를 먼저 바꾼다. 같은 건에 동시에 두 번 들어와도 뒤엣것은 MATURED 가 아니라 막힌다.
		subscription.complete(OffsetDateTime.now(KST));
		depositToWallet(childId, payout, product.getName() + " 만기 수령", idempotencyKey);
		record(subscription, SavingsTransactionType.MATURITY_PAYOUT, payout);

		log.info("예적금 만기 수령. childId={}, savingsSubscriptionId={}, 지급액={}", childId, savingsSubscriptionId, payout);

		return savingsMapper.toSubscriptionResponse(subscription, product);
	}

	/**
	 * 중도 해지. 납입한 원금만 돌려주고 쌓인 이자는 지급하지 않는다 — 만기를 지키는 것이
	 * 이 상품들이 가르치려는 것이라, 중간에 깨면 이자를 잃는다는 사실 자체가 학습 내용이다.
	 */
	@Override
	@Transactional
	public SavingsSubscriptionResponse terminate(Integer userId, Integer familyId, Integer childId,
		Integer savingsSubscriptionId, String idempotencyKey) {
		savingsAccessValidator.validate(userId, familyId, childId);

		SavingsSubscription subscription = findForUpdate(familyId, childId, savingsSubscriptionId);
		SavingsProduct product = findProduct(subscription.getSavingsProductId());

		int refund = subscription.getCurrentAmount();
		int forfeitedInterest = subscription.getAccruedInterest();
		subscription.terminate(OffsetDateTime.now(KST));

		if (refund > 0) {
			depositToWallet(childId, refund, product.getName() + " 중도 해지", idempotencyKey);
			record(subscription, SavingsTransactionType.EARLY_TERMINATION_PAYOUT, refund);
		}

		log.info("예적금 중도 해지. childId={}, savingsSubscriptionId={}, 반환액={}, 포기한 이자={}",
			childId, savingsSubscriptionId, refund, forfeitedInterest);

		return savingsMapper.toSubscriptionResponse(subscription, product);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SavingsSubscriptionResponse> listSubscriptions(Integer userId, Integer familyId, Integer childId) {
		savingsAccessValidator.validate(userId, familyId, childId);

		return savingsSubscriptionRepository.findByFamilyIdAndChildIdOrderByStartedAtDesc(familyId, childId).stream()
			.map(subscription -> savingsMapper.toSubscriptionResponse(subscription,
				findProduct(subscription.getSavingsProductId())))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public SavingsSubscriptionResponse getSubscription(Integer userId, Integer familyId, Integer childId,
		Integer savingsSubscriptionId) {
		savingsAccessValidator.validate(userId, familyId, childId);

		SavingsSubscription subscription = savingsSubscriptionRepository
			.findByIdAndFamilyIdAndChildId(savingsSubscriptionId, familyId, childId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SAVINGS_SUBSCRIPTION_NOT_FOUND,
				"savingsSubscriptionId=" + savingsSubscriptionId));

		return savingsMapper.toSubscriptionResponse(subscription, findProduct(subscription.getSavingsProductId()));
	}

	/**
	 * 부모 홈 카드용 요약. 조회라 부모도 부를 수 있고, 값을 바꾸지 않으니 행을 잠그지 않는다.
	 *
	 * <p>끝난 건(COMPLETED·CANCELLED)은 빼고 센다 — 그 포인트는 이미 지갑으로 돌아가
	 * 잔액에 잡혀 있어서, 여기서 또 세면 같은 포인트를 두 군데서 보여 주는 셈이 된다.
	 */
	@Override
	@Transactional(readOnly = true)
	public SavingsSummaryResponse getSummary(Integer userId, Integer familyId, Integer childId) {
		savingsAccessValidator.validate(userId, familyId, childId);

		return savingsMapper.toSummaryResponse(savingsSubscriptionRepository
			.findByFamilyIdAndChildIdAndStatusIn(familyId, childId, SUMMARY_STATUSES));
	}

	/**
	 * 거래 이력. 아이의 가입 건 목록을 먼저 찾고 그 가입들의 거래를 한 번에 모은다 —
	 * 거래 테이블에는 가입 id 만 있어서, 가족·아이 경계는 가입 건 조회가 대신 그어 준다.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<SavingsTransactionResponse> listTransactions(Integer userId, Integer familyId, Integer childId) {
		savingsAccessValidator.validate(userId, familyId, childId);

		List<SavingsSubscription> subscriptions = savingsSubscriptionRepository
			.findByFamilyIdAndChildIdOrderByStartedAtDesc(familyId, childId);
		if (subscriptions.isEmpty()) {
			return List.of();
		}

		Map<Integer, SavingsProduct> productBySubscriptionId = subscriptions.stream()
			.collect(Collectors.toMap(SavingsSubscription::getId,
				subscription -> findProduct(subscription.getSavingsProductId())));

		return savingsTransactionRepository
			.findBySavingsSubscriptionIdInOrderByCreatedAtDescIdDesc(productBySubscriptionId.keySet()).stream()
			.map(transaction -> savingsMapper.toTransactionResponse(transaction,
				productBySubscriptionId.get(transaction.getSavingsSubscriptionId())))
			.toList();
	}

	/**
	 * 예상 손익. 조회만 하므로 행을 잠그지 않는다 — 어차피 "지금 기준 예상"이라
	 * 읽는 순간의 값이면 충분하다.
	 */
	@Override
	@Transactional(readOnly = true)
	public SavingsExpectedReturnResponse getExpectedReturn(Integer userId, Integer familyId, Integer childId,
		Integer savingsSubscriptionId) {
		savingsAccessValidator.validate(userId, familyId, childId);

		SavingsSubscription subscription = savingsSubscriptionRepository
			.findByIdAndFamilyIdAndChildId(savingsSubscriptionId, familyId, childId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SAVINGS_SUBSCRIPTION_NOT_FOUND,
				"savingsSubscriptionId=" + savingsSubscriptionId));
		SavingsProduct product = findProduct(subscription.getSavingsProductId());

		return savingsMapper.toExpectedReturnResponse(subscription,
			expectedReturnCalculator.remainingInterest(subscription, product),
			expectedReturnCalculator.earlyTerminationAmount(subscription));
	}

	/**
	 * 납입 금액이 상품 규칙에 맞는지 본다.
	 *
	 * <ul>
	 *   <li>거치식(단리·복리) — 추가 납입 자체가 없다.</li>
	 *   <li>정액 적립식(소액·장기) — 가입 때 정한 회당 약정액과 정확히 같아야 하고, 약속한 회차를
	 *       넘길 수 없다. 금액이 달라지면 매주 같은 금액을 지킨다는 상품의 뜻이 사라진다.</li>
	 *   <li>목표적금 — 목표 금액을 넘길 수 없다. 넘겨 받으면 목표 이자를 목표보다 큰 원금에 주게 된다.</li>
	 *   <li>보통예금 — 이 가족의 한도(X)가 잔액 상한이다. 한도는 가입 시점이 아니라 지금 기준으로
	 *       다시 계산한다. 수시입출금이라 스냅샷해 둘 약정 금액이 없기 때문이다.</li>
	 * </ul>
	 */
	private void validateContribution(SavingsSubscription subscription, SavingsProduct product, Integer familyId,
		Integer childId, int amount) {
		ContributionStyle style = product.getContributionStyle();
		if (!style.allowsContribution()) {
			throw new BusinessException(ErrorCode.SAVINGS_CONTRIBUTION_NOT_ALLOWED,
				"가입 시 1회 납입으로 끝나는 상품입니다. productType=" + product.getProductType());
		}

		switch (style) {
			case FIXED_INSTALLMENT -> {
				if (amount != subscription.getPrincipal()) {
					throw new BusinessException(ErrorCode.SAVINGS_INVALID_CONTRIBUTION_AMOUNT,
						"매주 같은 금액을 납입해야 합니다. 약정액=" + subscription.getPrincipal() + ", 요청=" + amount);
				}
				Integer installmentCount = product.getInstallmentCount();
				if (installmentCount != null && subscription.getContributionCount() >= installmentCount) {
					throw new BusinessException(ErrorCode.SAVINGS_CONTRIBUTION_NOT_ALLOWED,
						"약속한 납입을 모두 마쳤습니다. 납입 회차=" + subscription.getContributionCount()
							+ "/" + installmentCount);
				}
				// 한 주에 한 번씩만 받는다. 회차를 몰아서 넣으면 이자 기준이 되는 잔액이 그만큼 일찍
				// 커져서, 같은 회차를 넣고도 약속한 것보다 많은 이자를 받게 된다.
				long elapsedWeeks = ChronoUnit.WEEKS.between(subscription.getStartedAt(), OffsetDateTime.now(KST));
				if (subscription.getContributionCount() > elapsedWeeks) {
					throw new BusinessException(ErrorCode.SAVINGS_CONTRIBUTION_NOT_ALLOWED,
						"이번 주 납입은 이미 마쳤습니다. 납입 회차=" + subscription.getContributionCount()
							+ ", 가입 후 경과=" + elapsedWeeks + "주");
				}
			}
			case FLEXIBLE_GOAL -> {
				long remaining = (long)subscription.getPrincipal() - subscription.getCurrentAmount();
				if (amount > remaining) {
					throw new BusinessException(ErrorCode.SAVINGS_INVALID_CONTRIBUTION_AMOUNT,
						"목표 금액을 넘을 수 없습니다. 남은 금액=" + remaining + ", 요청=" + amount);
				}
			}
			case FREE -> {
				FamilySettings familySettings = familySettingsReader.read(familyId, childId);
				long maxAmount = product.getProductType().calculateMaxAmount(familySettings);
				long after = (long)subscription.getCurrentAmount() + amount;
				if (after > maxAmount) {
					throw new BusinessException(ErrorCode.SAVINGS_AMOUNT_EXCEEDS_LIMIT,
						"예치 한도를 넘습니다. 납입 후 잔액=" + after + ", 한도=" + maxAmount);
				}
			}
			default -> throw new BusinessException(ErrorCode.SAVINGS_CONTRIBUTION_NOT_ALLOWED,
				"납입 규칙이 정의되지 않은 상품입니다. contributionStyle=" + style);
		}
	}

	/** 목표 달성 즉시 지급 — 목표 금액에 이자를 얹어 한 번에 돌려주고 가입을 끝낸다 */
	private void payOutOnGoalAchieved(SavingsSubscription subscription, SavingsProduct product,
		String idempotencyKey) {
		int interest = Math.toIntExact(
			product.getProductType().goalAchievementInterest(subscription.getCurrentAmount()));
		if (interest > 0) {
			subscription.accrueInterest(interest);
			record(subscription, SavingsTransactionType.INTEREST, interest);
		}

		int payout = subscription.payoutAmount();
		subscription.complete(OffsetDateTime.now(KST));
		// 납입 차감과 같은 멱등키를 그대로 쓰면 지갑이 이미 처리된 요청으로 보고 지급을 건너뛴다.
		depositToWallet(subscription.getChildId(), payout, product.getName() + " 목표 달성 수령",
			goalPayoutKey(idempotencyKey));
		record(subscription, SavingsTransactionType.MATURITY_PAYOUT, payout);

		log.info("목표적금 달성 지급. childId={}, savingsSubscriptionId={}, 목표={}, 이자={}, 지급액={}",
			subscription.getChildId(), subscription.getId(), subscription.getPrincipal(), interest, payout);
	}

	private String goalPayoutKey(String contributionIdempotencyKey) {
		return contributionIdempotencyKey == null ? null : contributionIdempotencyKey + ":goal-payout";
	}

	/**
	 * 같은 멱등키로 이미 만들어 둔 가입을 찾는다. 멱등키가 없는 요청은 재시도를 구분할 방법이
	 * 없으므로 그냥 새로 만든다 (지갑도 같은 규칙이다).
	 */
	private Optional<SavingsSubscriptionResponse> findAlreadySubscribed(Integer childId, String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return Optional.empty();
		}
		return savingsSubscriptionRepository.findByChildIdAndIdempotencyKey(childId, idempotencyKey)
			.map(existing -> {
				log.info("이미 처리된 가입 요청이라 기존 가입을 그대로 돌려준다. childId={}, idempotencyKey={}",
					childId, idempotencyKey);
				return savingsMapper.toSubscriptionResponse(existing, findProduct(existing.getSavingsProductId()));
			});
	}

	private void withdrawFromWallet(Integer childId, int amount, String description, String idempotencyKey) {
		pointWalletService.createWalletIfAbsent(childId);
		PointChangeResult result = pointWalletService.withdraw(PointChangeCommand.of(childId, amount,
			PointTransactionReason.SAVINGS_DEPOSIT, description, idempotencyKey));
		requireApplied(result, childId, idempotencyKey);
	}

	private void depositToWallet(Integer childId, int amount, String description, String idempotencyKey) {
		pointWalletService.createWalletIfAbsent(childId);
		PointChangeResult result = pointWalletService.deposit(PointChangeCommand.of(childId, amount,
			PointTransactionReason.SAVINGS_WITHDRAWAL, description, idempotencyKey));
		requireApplied(result, childId, idempotencyKey);
	}

	/**
	 * 지갑이 이전 거래를 되돌려줬다면(replayed) 이번 요청은 아무것도 반영하지 못했다는 뜻이다.
	 * 그대로 두면 포인트는 그대로인데 가입 건만 바뀐 채 커밋되므로, 던져서 전부 되돌린다.
	 *
	 * <p>가입은 이 경로로 오지 않는다 — 그쪽은 멱등키로 기존 가입을 찾아 그대로 돌려주므로
	 * 애초에 지갑을 부르지 않는다.
	 */
	private void requireApplied(PointChangeResult result, Integer childId, String idempotencyKey) {
		if (result.replayed()) {
			throw new BusinessException(ErrorCode.DUPLICATE_POINT_REQUEST,
				"이미 처리된 요청입니다. childId=" + childId + ", idempotencyKey=" + idempotencyKey);
		}
	}

	private void record(SavingsSubscription subscription, SavingsTransactionType type, int amount) {
		savingsTransactionRepository.save(SavingsTransaction.record(subscription.getId(), type, amount));
	}

	private SavingsSubscription findForUpdate(Integer familyId, Integer childId, Integer savingsSubscriptionId) {
		return savingsSubscriptionRepository
			.findByIdAndFamilyIdAndChildIdForUpdate(savingsSubscriptionId, familyId, childId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SAVINGS_SUBSCRIPTION_NOT_FOUND,
				"savingsSubscriptionId=" + savingsSubscriptionId));
	}

	private SavingsProduct findProduct(Integer savingsProductId) {
		return savingsProductRepository.findById(savingsProductId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SAVINGS_PRODUCT_NOT_FOUND,
				"savingsProductId=" + savingsProductId));
	}
}
