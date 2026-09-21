import { ImageManipulator, SaveFormat } from 'expo-image-manipulator';

import type { ChorePhotoFile } from '@/features/chore/model/types';

/**
 * expo-image-picker 결과를 업로드용 파일로 옮긴다.
 *
 * 서버는 jpg·png 만 받고 10MB 를 넘기면 거절한다(ChorePhotoServiceImpl). 올려 보고 나서
 * 실패를 알려 주면 아이가 사진을 다시 찍어야 하므로, 고른 직후에 여기서 먼저 거른다.
 */

const SUPPORTED: Record<string, 'jpg' | 'png'> = {
  'image/jpeg': 'jpg',
  'image/jpg': 'jpg',
  'image/png': 'png',
};

/** 서버 MAX_PHOTO_SIZE_BYTES 와 같은 값 */
export const MAX_PHOTO_BYTES = 10 * 1024 * 1024;

export type PickedAsset = {
  uri: string;
  fileName?: string | null;
  mimeType?: string | null;
  fileSize?: number | null;
  width?: number | null;
  height?: number | null;
};

/**
 * 업로드 전에 줄일 긴 변 길이.
 *
 * 요즘 폰 카메라는 4000px 짜리를 준다 (3~5MB). 이 사진은 부모가 "했구나" 를 확인하는
 * 용도라 그만한 해상도가 필요 없는데, 업로드는 느려지고 목록에서 다시 받을 때마다
 * 로딩이 길어진다 (QA - 썸네일이 늦게 뜬다).
 *
 * <p>1280 이면 부모 화면의 사진 칸(폭 전체 × 200dp)에서 확대해 봐도 충분하고, jpeg 0.7
 * 과 함께 보통 200~400KB 로 떨어진다.
 */
export const MAX_PHOTO_EDGE = 1280;

/** 업로드 jpeg 품질. 0.7 은 인증 사진에서 열화가 눈에 띄지 않는 선이다 */
const COMPRESS_QUALITY = 0.7;

/**
 * 긴 변을 MAX_PHOTO_EDGE 로 맞추는 resize 값. 이미 작으면 null (건드리지 않는다).
 *
 * 한쪽만 넘겨야 비율이 유지된다 - 둘 다 주면 라이브러리가 그대로 늘려서 찌그러진다.
 */
export function resizeTarget(
  width: number | null | undefined,
  height: number | null | undefined,
): { width: number } | { height: number } | null {
  if (width == null || height == null || width <= 0 || height <= 0) {
    // 크기를 모르면 긴 변을 알 수 없다. 폭 기준으로 줄이면 세로 사진이 과하게 작아진다.
    return null;
  }
  if (Math.max(width, height) <= MAX_PHOTO_EDGE) {
    return null;
  }
  return width >= height ? { width: MAX_PHOTO_EDGE } : { height: MAX_PHOTO_EDGE };
}

export type PhotoError = 'UNSUPPORTED_TYPE' | 'TOO_LARGE';

/**
 * 서버가 받는 contentType 으로 정규화한다.
 *
 * mimeType 은 기기에 따라 비거나 'image/jpg' 처럼 서버가 모르는 표기로 온다. 비었으면
 * 확장자에서 유추하고, 그래도 모르면 null 이다 - 추측해서 jpeg 로 보내면 서버가 헤더만
 * 믿고 저장하므로 png 를 jpg 로 둔갑시키게 된다.
 */
export function normalizeMimeType(asset: PickedAsset): 'image/jpeg' | 'image/png' | null {
  const known = asset.mimeType ? SUPPORTED[asset.mimeType.toLowerCase()] : undefined;
  const ext = known ?? extensionOf(asset.fileName ?? asset.uri);
  if (ext === 'jpg') return 'image/jpeg';
  if (ext === 'png') return 'image/png';
  return null;
}

function extensionOf(path: string): 'jpg' | 'png' | undefined {
  const lower = path.toLowerCase().split('?')[0];
  if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) return 'jpg';
  if (lower.endsWith('.png')) return 'png';
  return undefined;
}

/** 올려도 되는 사진인지. 문제 없으면 undefined */
export function photoError(asset: PickedAsset): PhotoError | undefined {
  if (normalizeMimeType(asset) == null) return 'UNSUPPORTED_TYPE';
  if (asset.fileSize != null && asset.fileSize > MAX_PHOTO_BYTES) return 'TOO_LARGE';
  return undefined;
}

export function photoErrorMessage(error: PhotoError): string {
  return error === 'TOO_LARGE'
    ? '사진이 너무 커요. 10MB 보다 작은 사진으로 올려주세요.'
    : 'jpg 또는 png 사진만 올릴 수 있어요.';
}

/**
 * 업로드용으로 사진을 줄인다.
 *
 * 크기를 모르거나 이미 작으면 다시 인코딩만 한다 - jpeg 품질만 낮춰도 원본보다 작아지고,
 * 무엇보다 카메라가 준 heic·png 를 jpeg 로 통일해 준다.
 *
 * <p>실패하면 원본을 그대로 돌려준다. 압축은 빠르자고 하는 일이라, 여기서 막혀 완료 요청
 * 자체를 못 보내면 본말이 바뀐다.
 */
export async function compressPhoto(asset: PickedAsset): Promise<PickedAsset> {
  try {
    const context = ImageManipulator.manipulate(asset.uri);
    const target = resizeTarget(asset.width, asset.height);
    if (target != null) {
      context.resize(target);
    }

    const image = await context.renderAsync();
    const result = await image.saveAsync({
      compress: COMPRESS_QUALITY,
      format: SaveFormat.JPEG,
    });

    return {
      uri: result.uri,
      fileName: 'chore-photo.jpg',
      mimeType: 'image/jpeg',
      // 줄인 파일의 바이트 수는 알 수 없다. null 이면 photoError 가 크기 검사를 건너뛰는데,
      // 1280px jpeg 0.7 이 10MB 를 넘을 수는 없어서 문제가 되지 않는다.
      fileSize: null,
      width: result.width,
      height: result.height,
    };
  } catch {
    return asset;
  }
}

/**
 * multipart 로 보낼 파일. 이름이 비면 서버의 multipart 파싱이 파일로 인식하지 못하므로
 * 항상 채운다 (저장될 이름은 서버가 따로 만든다 - 여기 이름은 전송용이다).
 */
export function toPhotoFile(asset: PickedAsset): ChorePhotoFile {
  const type = normalizeMimeType(asset) ?? 'image/jpeg';
  return {
    uri: asset.uri,
    name: asset.fileName?.trim() || `chore-photo.${type === 'image/png' ? 'png' : 'jpg'}`,
    type,
  };
}
