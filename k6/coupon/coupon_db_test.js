import http from "k6/http";
import { check, fail } from "k6";

// 기본 설정
const BASE_URL = "http://127.0.0.1:8080/api/v1/coupon/register";
const COUPON_ID = 4; // 테스트용 쿠폰 ID

// k6 실행 옵션 (스파이크 테스트 설정)
export const options = {
  //   vus: 3000, // 가상 사용자 수 (2만 명)
  //   iterations: 3000, // 총 요청 수 (각 VU가 1회 요청)

  vus: 1000, // 가상 사용자 수 (2만 명)
  iterations: 1000, // 총 요청 수 (각 VU가 1회 요청)

  // vus: 100,       // 가상 사용자 수 (2만 명)
  // iterations: 100, // 총 요청 수 (각 VU가 1회 요청)
  // rate: 30
  startTime: "3s",
  // duration: '250s'
};

// export const options = {
//     executor: 'constant-arrival-rate',
//     rate: 1000,         // 초당 50 요청
//     timeUnit: '1s',
//     duration: '5s',  // 3000 / 50 = 60초
//     preAllocatedVUs: 3000,
//     maxVUs: 3000,
// };

// 기본 헤더 설정
const headers = {
  "Content-Type": "application/json",
};

// 메인 테스트 함수
export default function () {
  // 고유한 userId 생성: VU ID (1부터 시작) + 오프셋(100 추가)
  //   const userId = 2000 + __VU;
  const userId = 2030;
  const url = `${BASE_URL}/${COUPON_ID}/db/${userId}`;

  // PUT 요청 전송
  const response = http.put(url, null, { headers: headers });

  // 로그 출력 (필요 시 주석 처리 가능)
  console.log(`VU ${__VU}: Generated URL: ${url}`);

  // 응답 검증
  if (response.status === 200) {
    // console.log(`VU ${__VU}: Successfully used coupon for userId ${userId}`);
  } else {
    fail(`Request failed with status: ${response.status}`);
  }

  // k6 체크: 상태 코드가 200인지 확인
  check(response, {
    "status is 200": (r) => r.status === 200,
  });
}
