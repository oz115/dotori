// jest 는 .env 를 읽지 않으므로 테스트용 EXPO_PUBLIC_* 값을 여기서 채운다.
// (모듈 import 전에 실행되도록 setupFiles 로 등록)
process.env.EXPO_PUBLIC_API_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080';
