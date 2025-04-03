import http from "k6/http";
import { check, fail } from "k6";

// 기본 설정
const BASE_URL = "http://127.0.0.1:8080/api/v1/coupon/register";
// const BASE_URL = "http://172.16.24.78:8080/api/v1/coupon/register";

const COUPON_ID = 91; // 테스트용 쿠폰 ID

// k6 실행 옵션 (스파이크 테스트 설정)
export const options = {
  vus: 5000, // 가상 사용자 수 (2만 명)
  iterations: 5000, // 총 요청 수 (각 VU가 1회 요청)

  // vus: 100,       // 가상 사용자 수 (2만 명)
  // iterations: 100, // 총 요청 수 (각 VU가 1회 요청)
  // rate: 30
  startTime: "5s",
  // duration: '250s'
};

// 기본 헤더 설정
const headers = {
  "Content-Type": "application/json",
  Connection: "close",
};

// 메인 테스트 함수
export default function () {
  // 고유한 userId 생성: VU ID (1부터 시작) + 오프셋(100 추가)
  const userId = 2001 + __VU;
  const url = `${BASE_URL}/${COUPON_ID}/redis/${userId}/v3`;

  // PUT 요청 전송
  const response = http.put(url, null, {
    // tags: { name: "redis 분산락 API" },
    tags: { name: "redis 재고관리 + 싱글스레드 이벤트 큐" },
    headers: headers,
  });

  // 로그 출력 (필요 시 주석 처리 가능)
  //   console.log(`VU ${__VU}: Generated URL: ${url}`);

  // 응답 검증
  // if (response.status === 200) {
  // console.log(`VU ${__VU}: Successfully used coupon for userId ${userId}`);
  // } else {
  // fail(`Request failed with status: ${response.status}`);
  // }

  // k6 체크: 상태 코드가 200인지 확인
  check(response, {
    "status is 200": (r) => r.status === 200,
  });
}
