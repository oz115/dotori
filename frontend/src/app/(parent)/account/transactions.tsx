import { router } from 'expo-router';

import { TransactionHistoryView } from '@/widgets/account-transactions';

/** 부모 본인 계좌의 거래내역 조회. */
export default function ParentAccountTransactionsScreen() {
  return <TransactionHistoryView onBack={() => router.back()} />;
}
