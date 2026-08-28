/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-expo',
  // node/web/ios/android 중 하나만 골라서 테스트를 돌립니다.
  // 컴포넌트는 대부분 플랫폼 공통이라 기본값(ios)이면 충분합니다.
  collectCoverageFrom: ['src/**/*.{ts,tsx}', '!src/app/**'],
};
