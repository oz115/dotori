import { router } from 'expo-router';

import { BankSelectView } from '@/widgets/account-link';

export default function ChildBankSelectScreen() {
  return <BankSelectView onClose={() => router.back()} />;
}
