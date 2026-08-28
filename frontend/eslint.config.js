// https://docs.expo.dev/guides/using-eslint/
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');

module.exports = defineConfig([
  expoConfig,
  {
    ignores: ['dist/*'],
  },
  {
    // "../model/store" 처럼 부모 디렉터리로 거슬러 올라가는 import를 금지합니다.
    // 같은 폴더 안(./)은 허용하고, 그 밖은 전부 @/ 별칭을 쓰세요.
    // (import/no-relative-parent-imports는 @/ 별칭까지 "상위 경로"로 오인해서 오탐이 나 사용하지 않음)
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              regex: '^\\.\\./',
              message: '상위 디렉터리 상대경로 대신 @/ 별칭을 쓰세요. (같은 폴더 안 ./는 허용)',
            },
          ],
        },
      ],
    },
  },
]);
