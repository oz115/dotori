import { router } from 'expo-router';

import { BankSelectView } from '@/widgets/account-link';

export default function ParentBankSelectScreen() {
  return <BankSelectView onClose={() => router.back()} />;
}
