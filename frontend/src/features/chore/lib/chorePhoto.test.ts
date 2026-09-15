import { normalizeMimeType, photoError, toPhotoFile } from './chorePhoto';

describe('normalizeMimeType', () => {
  it('서버가 아는 표기로 맞춘다', () => {
    expect(normalizeMimeType({ uri: 'file:///a.jpg', mimeType: 'image/jpg' })).toBe('image/jpeg');
    expect(normalizeMimeType({ uri: 'file:///a.png', mimeType: 'image/png' })).toBe('image/png');
  });

  it('mimeType 이 비면 확장자에서 유추한다', () => {
    expect(normalizeMimeType({ uri: 'file:///photo.JPEG' })).toBe('image/jpeg');
    expect(normalizeMimeType({ uri: 'file:///photo.png', mimeType: null })).toBe('image/png');
  });

  it('쿼리스트링이 붙어 있어도 확장자를 읽는다', () => {
    expect(normalizeMimeType({ uri: 'content://media/1.jpg?width=100' })).toBe('image/jpeg');
  });

  it('알 수 없으면 추측하지 않는다', () => {
    // 추측해서 보내면 서버가 헤더만 믿고 저장해 png 가 jpg 로 둔갑한다.
    expect(normalizeMimeType({ uri: 'file:///photo.heic', mimeType: 'image/heic' })).toBeNull();
  });
});

describe('photoError', () => {
  it('지원하지 않는 형식을 거른다', () => {
    expect(photoError({ uri: 'file:///a.heic', mimeType: 'image/heic' })).toBe('UNSUPPORTED_TYPE');
  });

  it('10MB 를 넘으면 거른다', () => {
    expect(photoError({ uri: 'file:///a.jpg', fileSize: 10 * 1024 * 1024 + 1 })).toBe('TOO_LARGE');
  });

  it('크기를 모르면 통과시킨다 (서버가 최종 판단)', () => {
    expect(photoError({ uri: 'file:///a.jpg', fileSize: null })).toBeUndefined();
  });
});

describe('toPhotoFile', () => {
  it('이름이 없으면 확장자에 맞춰 채운다', () => {
    expect(toPhotoFile({ uri: 'file:///a.png', mimeType: 'image/png' })).toEqual({
      uri: 'file:///a.png',
      name: 'chore-photo.png',
      type: 'image/png',
    });
  });

  it('기기가 준 이름이 있으면 그대로 쓴다', () => {
    expect(toPhotoFile({ uri: 'file:///a.jpg', fileName: 'IMG_0001.jpg' }).name).toBe(
      'IMG_0001.jpg',
    );
  });
});
