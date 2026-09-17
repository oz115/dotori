import { router } from 'expo-router';

import { useTransferStore } from '@/features/account';
import { BankSelectView } from '@/widgets/account-link';

/** 이체 대상 계좌의 은행 선택 — 표시 전용(서버에는 계좌번호만 보낸다). */
export default function TransferBankSelectScreen() {
  const bankKey = useTransferStore((s) => s.bankKey);
  const patch = useTransferStore((s) => s.patch);

  return (
    <BankSelectView
      selected={bankKey}
      onSelect={(key) => patch({ bankKey: key })}
      onConfirm={() => router.back()}
      onClose={() => router.back()}
    />
  );
}
