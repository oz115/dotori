import { useCallback, useState } from 'react';
import { RefreshControl } from 'react-native';

import { queryClient } from '@/shared/api';

/**
 * 스크롤 컨테이너(ScrollView·FlatList·SectionList)의 `refreshControl` 에 그대로 꽂는
 * 당겨서 새로고침. 지금 화면에 붙어 있는(active) 쿼리를 전부 서버에서 다시 받아온다.
 *
 * 화면마다 어떤 쿼리를 쓰는지 알 필요가 없도록 쿼리 키를 좁히지 않는다. 탭 화면은 전환해도
 * 언마운트되지 않아 다른 탭의 쿼리도 함께 갱신되지만, 사용자가 당길 때만 일어나는 일이라 감수한다.
 * 화면을 나갔다 오면 다시 받는 staleTime 규칙과는 별개로 "지금 최신으로" 를 보장하는 수단이다.
 *
 * 집안일·환전처럼 새로고침 범위를 좁히는 화면은 각자 자기 훅을 쓰고, 이 훅은 그 밖의 화면용이다.
 */
export function usePullToRefresh() {
  const [refreshing, setRefreshing] = useState(false);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await queryClient.refetchQueries({ type: 'active' });
    } finally {
      setRefreshing(false);
    }
  }, []);

  return <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />;
}
