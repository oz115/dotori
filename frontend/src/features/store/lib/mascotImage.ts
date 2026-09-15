export type StoreMascotCharacter = 'parent' | 'child';

/**
 * 상점 배너 마스코트. 부모/자녀 화면이 서로 다른 캐릭터를 보여준다.
 * 지금은 색만 채운 placeholder — 실제 일러스트가 assets/images/store/ 에 같은
 * 파일명(mascot-parent.png, mascot-child.png)으로 들어오면 코드 변경 없이 자동 교체된다.
 */
const MASCOT_IMAGES: Record<StoreMascotCharacter, number> = {
  parent: require('@/assets/images/store/mascot-parent.png'),
  child: require('@/assets/images/store/mascot-child.png'),
};

export function mascotImage(character: StoreMascotCharacter) {
  return MASCOT_IMAGES[character];
}
