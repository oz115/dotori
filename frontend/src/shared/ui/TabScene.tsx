import { createContext, useContext, type PropsWithChildren } from 'react';

import { useTheme } from '@/shared/theme';

const TabOverlapContext = createContext(0);

/** 탭바의 둥근 상단이 화면에 겹치는 높이. 탭 밖에서는 여백을 추가하지 않는다. */
export function TabScene({ children }: PropsWithChildren) {
  const theme = useTheme();

  return (
    <TabOverlapContext.Provider value={theme.radius['2xl']}>{children}</TabOverlapContext.Provider>
  );
}

export function useTabOverlap() {
  return useContext(TabOverlapContext);
}
