import { ComingSoonScreen } from '@/shared/ui';

/**
 * 부모용 투자 화면. 자녀의 투자 화면(`(child)/(tabs)/invest`)과는 보이는 내용이 달라야 해서
 * 같은 화면을 공유하지 않고 라우트 이름도 다르게 뒀다 (invest ↔ investment).
 * 실제 내용은 부모 투자 화면 태스크에서 채운다.
 */
export default function ParentInvestmentScreen() {
  return <ComingSoonScreen title="투자" />;
}
