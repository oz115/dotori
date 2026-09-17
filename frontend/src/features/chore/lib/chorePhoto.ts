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
};

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
