import type { FamilyMember } from '@/features/family/model/types';

/** 구성원 중 아이가 한 명이라도 있는지 */
export function hasChildMember(members: FamilyMember[] | undefined): boolean {
  return members?.some((m) => m.role === 'CHILD') ?? false;
}
