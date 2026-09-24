import { decodeJwt } from './decodeJwt';

describe('decodeJwt', () => {
  it('decodes the payload claims', () => {
    // payload: {"sub":"7","role":"PARENT","exp":1893456000}
    const token =
      'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZSI6IlBBUkVOVCIsImV4cCI6MTg5MzQ1NjAwMH0.signature';

    expect(decodeJwt(token)).toEqual({ sub: '7', role: 'PARENT', exp: 1893456000 });
  });

  it('throws on a malformed token', () => {
    expect(() => decodeJwt('not-a-jwt')).toThrow();
  });
});
