import { useCounterStore } from './store';

// zustand 스토어는 모듈 단위 싱글턴이라 테스트 간 상태가 새면 안 되므로 매번 초기화합니다.
beforeEach(() => {
  useCounterStore.setState({ count: 0 });
});

describe('useCounterStore', () => {
  it('starts at 0', () => {
    expect(useCounterStore.getState().count).toBe(0);
  });

  it('increments the count', () => {
    useCounterStore.getState().increment();
    useCounterStore.getState().increment();

    expect(useCounterStore.getState().count).toBe(2);
  });

  it('decrements the count', () => {
    useCounterStore.getState().increment();
    useCounterStore.getState().decrement();

    expect(useCounterStore.getState().count).toBe(0);
  });

  it('resets the count', () => {
    useCounterStore.getState().increment();
    useCounterStore.getState().reset();

    expect(useCounterStore.getState().count).toBe(0);
  });
});
