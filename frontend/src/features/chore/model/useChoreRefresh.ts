import { useQueryClient } from '@tanstack/react-query';
import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';

import { choreKeys } from '@/features/chore/model/choreKeys';

/**
 * 집안일 화면의 새로고침. 화면에 들어올 때 한 번, 그리고 당겨서 한 번.
 *
 * 보상은 등록 직후가 아니라 AI 가 산정한 뒤에 채워진다. 그 사이 화면은 "산정 중"을 들고
 * 있는데, 탭 화면은 전환해도 언마운트되지 않아서 다시 와도 그대로다 - 기본 staleTime 이
 * 60초라 그 안에는 다시 받아오지도 않는다. 그래서 들어올 때마다 명시적으로 턴다.
 *
 * 부모가 등록한 할 일이 아이 화면에 늦게 뜨는 것도 같은 이유다. 서로 다른 기기라 푸시가
 * 붙기 전까지는 앱이 먼저 물어보는 수밖에 없다.
 *
 * refetch 대신 invalidate 를 쓴다 - 화면마다 쿼리가 여러 개인데(목록·대기함·완료 내역)
 * useQueries 의 refetch 는 렌더마다 새로 만들어져 의존성에 넣으면 무한 루프가 된다.
 * queryClient 는 안정적이라 그런 문제가 없다.
 */
export function useChoreRefresh() {
  const queryClient = useQueryClient();
  const [refreshing, setRefreshing] = useState(false);

  const refresh = useCallback(
    () => queryClient.invalidateQueries({ queryKey: choreKeys.all }),
    [queryClient],
  );

  /** 당겨서 새로고침. 스피너를 직접 들고 있어야 해서 따로 둔다 */
  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await refresh();
    } finally {
      setRefreshing(false);
    }
  }, [refresh]);

  useFocusEffect(
    useCallback(() => {
      refresh();
    }, [refresh]),
  );

  return { refreshing, onRefresh };
}
