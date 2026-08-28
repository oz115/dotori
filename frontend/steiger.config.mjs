import { defineConfig } from 'steiger';
import fsd from '@feature-sliced/steiger-plugin';

// Feature-Sliced Design 레이어 규칙(레이어 간 import 방향, public API 강제 등)을 검사합니다.
// 실행: npm run fsd-lint
export default defineConfig([...fsd.configs.recommended]);
