import { useCounterStore } from '@/features/counter/model/store';
import { Button } from '@/shared/ui';

/**
 * "카운터를 1 증가시킨다" 라는 사용자 행동 하나를 캡슐화한 조각.
 * widgets 는 이 버튼을 갖다 붙이기만 합니다.
 */
export function IncrementButton() {
  const increment = useCounterStore((state) => state.increment);

  return <Button label="+1" onPress={increment} />;
}
