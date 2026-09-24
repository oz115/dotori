import type { FamilyMember } from '@/features/family/model/types';

import { hasChildMember } from './familyReady';

const parent: FamilyMember = { userId: 1, name: '부모', role: 'PARENT', joinedAt: '2026-09-21' };
const child: FamilyMember = { userId: 2, name: '아이', role: 'CHILD', joinedAt: '2026-09-21' };

describe('hasChildMember', () => {
  it('부모만 있으면 false', () => {
    expect(hasChildMember([parent])).toBe(false);
  });

  it('아이가 합류하면 true', () => {
    expect(hasChildMember([parent, child])).toBe(true);
  });

  it('아직 조회 전(undefined)이면 false', () => {
    expect(hasChildMember(undefined)).toBe(false);
  });
});
