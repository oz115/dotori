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
