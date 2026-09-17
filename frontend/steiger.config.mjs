import { defineConfig } from 'steiger';
import fsd from '@feature-sliced/steiger-plugin';

// Feature-Sliced Design 레이어 규칙(레이어 간 import 방향, public API 강제 등)을 검사합니다.
// 실행: npm run fsd-lint
export default defineConfig([
  ...fsd.configs.recommended,
  {
    // 예적금 슬라이스는 지금 부모 홈 카드 한 곳만 쓰지만, 저축 탭·아이 홈이 같은 조회를
    // 이어받을 자리라 widget 안으로 합치지 않는다. 참조가 둘 이상이 되면 이 예외를 지운다.
    files: ['./src/features/savings/**'],
    rules: { 'fsd/insignificant-slice': 'off' },
  },
]);
