import http from 'k6/http';
import { check, sleep } from 'k6';

// 테스트 설정: VUser 단계별 증가 및 유지, 성능 임계값
export const options = {
  stages: [
    { duration: '3m', target: 230 },  // 초기 3분: 0 → 230 VUser (25% 부하)
    { duration: '10m', target: 230 }, // 10분: 230 VUser 유지 (준비 단계)
    { duration: '3m', target: 920 },  // 3분: 230 → 920 VUser (피크 부하로 전환)
    { duration: '10m', target: 920 }, // 10분: 920 VUser 유지 (피크 부하 테스트)
    { duration: '3m', target: 0 },    // 마지막 3분: 920 → 0 VUser (종료)
  ],
  thresholds: {
    'http_req_duration{name: "pending_order"}': ['p(95)<200'],   // 주문 생성: 100ms + extra time
    'http_req_duration{name: "payment_ready"}': ['p(95)<200'],   // 결제 준비: 100ms + extra time
    'http_req_duration{name: "get_payment_url"}': ['p(95)<200'], // QR 결제: 서버 100ms + extra time
    'http_req_duration{name: "approve_payment"}': ['p(95)<1000'],// 결제 승인: 1000ms
    'http_req_failed': ['rate<0.01'], // 실패율 1% 미만
  },
};

// export const options = {
//   vus: 1,
//   iterations: 1,
//   // vus: 500,
//   // duration: '300s', // 테스트 기간
//   tags: {
//     name: '', // 기본 태그 비활성화
//   },
// };


// 환경 변수에서 URL 및 버전 설정 (기본값 제공)
const TARGET_URL = __ENV.TARGET_URL || 'http://52.79.184.29:8080';
const FAKE_PAYMENT_URL = __ENV.FAKE_PAYMENT_URL || 'http://43.202.235.222:8081';
const TEST_VERSION = __ENV.TEST_VERSION || 'v4';

function getRandomUserId() {
  return Math.floor(Math.random() * (4000 - 2001 + 1)) + 2001;
}

function generateOrderItems() {
  const itemCount = Math.floor(Math.random() * 3) + 1;
  const productIds = new Set();
  while (productIds.size < itemCount) {
    productIds.add(Math.floor(Math.random() * 100) + 1);
  }
  return Array.from(productIds).map(productId => ({
    product_id: productId,
    price: "10000",
    quantity: 1,
  }));
}


// 주문 생성 페이로드 (재사용 가능하도록 상수로 분리)
const orderPayload = {
  total_price: "199900",
  order_items: generateOrderItems(),
};

// 공통 HTTP 헤더
const JSON_HEADERS = { 'Content-Type': 'application/json' };

// 랜덤 think time 생성 함수 (2~3초)
function randomThinkTime() {
  return 2 + Math.random(); // 2초 ~ 3초 사이
}

// HTTP 요청 상태 체크 및 로깅 함수
function checkResponse(response, successMessage, failureMessage) {
  const isSuccess = check(response, {
    [successMessage]: (res) => res.status === 200,
  });
  if (!isSuccess) {
    console.log(`${failureMessage} - Status: ${response.status}, Body: ${response.body}`);
  }
  return isSuccess;
}

// 메인 테스트 시나리오
export default function () {
  const userId = 1;

  // 1. 주문 생성 (Pending Order)
  const pendingOrderResponse = http.post(
      `${TARGET_URL}/api/v1/test/order/pending/${userId}`,
      JSON.stringify(orderPayload),
      { headers: JSON_HEADERS, tags: { name: 'pending_order' } }
  );

  if (!checkResponse(pendingOrderResponse, 'order created', 'Order creation failed')) {
    return;
  }

  const orderData = pendingOrderResponse.json();
  const orderId = BigInt(orderData.result.order_id);
  console.log(`Order created - Order ID: ${orderId}`);

  // Think time 1: 사용자 대기 (2~3초)
  sleep(randomThinkTime());
  //
  // sleep(0.5);

  // 2. 결제 준비 (Payment Ready)
  console.log(`Sending pay-ready request - Order ID: ${orderId}, User ID: ${userId}`);
  const payReadyResponse = http.post(
      `${TARGET_URL}/api/${TEST_VERSION}/test/order/payment/${orderId}/ready/${userId}`,
      null,
      { headers: JSON_HEADERS, tags: { name: 'payment_ready' } }
  );

  if (!checkResponse(payReadyResponse, 'payment ready', 'Payment ready failed')) {
    return;
  }

  // 3. 결제 URL 조회 (QR 결제 화면) - 300ms 브라우저 대기 + 서버 응답
  sleep(0.3); // 브라우저 대기 시간 300ms
  const getPaymentUrlResponse = http.get(
      `${TARGET_URL}/api/${TEST_VERSION}/test/order/payment/${orderId}/redirect-url/${userId}`,
      { tags: { name: 'get_payment_url' } }
  );

  if (!checkResponse(getPaymentUrlResponse, 'payment URL retrieved', 'Failed to retrieve payment URL')) {
    return;
  }

  const paymentUrlData = getPaymentUrlResponse.json();
  console.log(`Payment URL: ${paymentUrlData.result.payment_url}`);

  let pid = null;
  if (paymentUrlData.result.payment_url) {
    pid = paymentUrlData.result.payment_url.split('/').at(-1);
    console.log(`Payment ID: ${pid}`);
  } else {
    console.log(`Failed to retrieve payment URL - Order ID: ${orderId}`);
    return;
  }

  // // Think time 2: 사용자 대기 (2~3초)
  sleep(randomThinkTime());

  // 4. 결제 승인 (Approve Payment)
  const approveResponse = http.post(
      `${FAKE_PAYMENT_URL}/v1/payment/in-progress/${pid}`,
      null,
      { headers: JSON_HEADERS, tags: { name: 'approve_payment' } }
  );

  checkResponse(approveResponse, 'payment approved', 'Payment approval failed');
  if (approveResponse.status === 200) {
    console.log(`Payment approval succeeded - Status: ${approveResponse.status}`);
  }

}