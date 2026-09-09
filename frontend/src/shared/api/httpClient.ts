import axios from 'axios';

import { env } from '@/shared/config';

/**
 * 백엔드 호출에 사용하는 공용 axios 인스턴스.
 * entities/features 의 api 모듈은 이 인스턴스만 import 해서 사용하고,
 * 새로운 axios 인스턴스를 각자 만들지 않습니다.
 */
// eslint-disable-next-line import/no-named-as-default-member -- axios 기본 export 사용, false positive
export const httpClient = axios.create({
  baseURL: env.apiUrl,
  timeout: 10_000,
});

// 인증 토큰 등 공통 헤더가 필요해지면 여기에 인터셉터를 추가하세요.
// httpClient.interceptors.request.use((config) => { ... return config; });

// 개발 중 모든 API 호출을 Metro 콘솔에 찍는다. 프로덕션 빌드에서는 실행되지 않음.
if (__DEV__) {
  const label = (m?: string, u?: string) => `${m?.toUpperCase() ?? '?'} ${u ?? ''}`;

  httpClient.interceptors.request.use((config) => {
    console.log(`[API] → ${label(config.method, config.url)}`);
    return config;
  });

  httpClient.interceptors.response.use(
    (res) => {
      console.log(`[API] ✓ ${res.status} ${label(res.config.method, res.config.url)}`);
      return res;
    },
    (error) => {
      const status = error.response?.status ?? 'NETWORK';
      const detail = error.response?.data?.code ?? error.code ?? error.message ?? '';
      console.log(`[API] ✗ ${status} ${label(error.config?.method, error.config?.url)}  ${detail}`);
      return Promise.reject(error);
    },
  );
}
