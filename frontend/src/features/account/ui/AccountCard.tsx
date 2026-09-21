import { Ionicons } from '@expo/vector-icons';

import { formatWonNumber } from '@/shared/lib/money';
import { bankBrand, useTheme } from '@/shared/theme';
import { WalletButton, WalletCard } from '@/shared/ui';

import { BANKS } from '@/features/account/lib/banks';
import type { AccountInfo } from '@/features/account/model/types';
import { BankBadge } from './BankBadge';

type AccountCardProps = {
  account: AccountInfo | null | undefined;
  isPending: boolean;
  /** 계좌가 없을 때 연동 화면으로 */
  onLinkAccount: () => void;
  /** 이체 화면(계좌번호 입력)으로 */
  onTransfer?: () => void;
  onHistory?: () => void;
  /** 이체·거래내역 대신 버튼 하나("계좌 관리")만 둘 때 (자녀 홈) */
  onManage?: () => void;
  /** false 면 이체·거래내역 버튼을 그리지 않는다 (계좌 관리 화면) */
  showActions?: boolean;
  /** 계좌가 없을 때 금액 아래에 띄울 안내. 계좌가 어디에 쓰이는지는 부모·자녀가 다르다 */
  unlinkedNote?: string;
  /** 슬라이드처럼 높이가 정해진 자리에서 남는 높이를 채운다 (기본은 내용만큼) */
  fill?: boolean;
};

const PARENT_NOTE = '계좌를 연결해야 아이가 도토리를 충전하고 환전할 수 있어요.';

/**
 * 내 계좌 카드 (부모 홈·자녀 홈 슬라이드, 연동 계좌 관리 화면). 흰 카드 바탕에 은행 브랜드 컬러
 * (`bankBrand`)를 포인트로만 쓴다 — 윗줄 띠, 로고를 두른 테두리, 채운 버튼. 배경 전체를 은행 색으로
 * 칠하면 노랑(국민·농협)처럼 강한 색이 화면을 압도하고 위 카드와도 부딪친다.
 * 모양은 다른 지갑 카드와 같은 틀(WalletCard)이라 슬라이드로 넘겨도 어긋나지 않는다.
 * 아직 연동 전이면 은행이 없으니 초록 포인트의 안내 카드로 "내 계좌" 자리라는 것만 알린다.
 *
 * 부모의 이 계좌는 충전(자녀→부모)의 수취 계좌이자 환전 승인(부모→자녀)의 출금 계좌라서,
 * 연동돼 있지 않으면 아이의 충전·환전이 서버에서 막힌다. 그래서 없을 때를 눈에 띄게 안내한다.
 */
export function AccountCard({
  account,
  isPending,
  onLinkAccount,
  onTransfer,
  onHistory,
  onManage,
  showActions = true,
  unlinkedNote = PARENT_NOTE,
  fill = false,
}: AccountCardProps) {
  const theme = useTheme();
  const bank = BANKS.find((b) => b.code === account?.bankCode);
  const brand = bank ? bankBrand[bank.key] : undefined;

  if (!account) {
    return (
      <WalletCard
        fill={fill}
        accent={theme.color.brand.solid}
        icon={<Ionicons name="wallet-outline" size={24} color={theme.color.brand.solid} />}
        title="내 계좌"
        amount={isPending ? '—' : '연동된 계좌 없음'}
        note={isPending ? undefined : unlinkedNote}
      >
        {isPending ? null : <WalletButton label="계좌 연동" onPress={onLinkAccount} />}
      </WalletCard>
    );
  }

  const showTransferRow = showActions && (onTransfer || onHistory);

  return (
    <WalletCard
      fill={fill}
      accent={brand?.color ?? theme.color.brand.solid}
      onAccent={brand?.textOnBrand}
      icon={
        bank ? (
          <BankBadge bank={bank} size={30} />
        ) : (
          <Ionicons name="wallet-outline" size={24} color={theme.color.text.muted} />
        )
      }
      title={account.bankName}
      // 계좌번호는 카드를 압도하지 않게 작은 보조 글씨(13px)로 둔다
      subtitle={`${account.accountName} · ${account.accountNo}`}
      amount={`${formatWonNumber(account.accountBalance)}원`}
    >
      {showTransferRow ? (
        <>
          {onTransfer ? <WalletButton label="이체하기" onPress={onTransfer} /> : null}
          {onHistory ? (
            <WalletButton label="거래내역 조회" onPress={onHistory} tone="soft" />
          ) : null}
        </>
      ) : onManage ? (
        <WalletButton label="계좌 관리" onPress={onManage} />
      ) : null}
    </WalletCard>
  );
}
