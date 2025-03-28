import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus:10, // 가상 유저 수
  duration: '30s', // 테스트 기간
};

const BASE_URL = 'http://172.16.24.78:8080'; // 실제 API 베이스 URL로 변경 필요

// 랜덤 userId 생성 함수 (2001~4000)
function getRandomUserId() {
  return Math.floor(Math.random() * (4000 - 2001 + 1)) + 2001;
}

// 랜덤 order_items 생성 함수 (최소 1개, 최대 3개, product_id 중복 없음)
function generateOrderItems() {
  const itemCount = Math.floor(Math.random() * 3) + 1; // 1~3개
  const productIds = new Set();
  while (productIds.size < itemCount) {
    productIds.add(Math.floor(Math.random() * 100) + 1); // 1~100 사이
  }

  return Array.from(productIds).map(productId => ({
    product_id: productId,
    price: "10000", // 고정 10,000원
    quantity: 1, // 고정 1개
  }));
}

// 주문 → 결제 시나리오 (대기 없음)
export default function () {
  const userId = getRandomUserId();

  // 주문 API 호출
  const orderPayload = {
    total_price: "199900",
    order_items: generateOrderItems(),
  };

  const orderResponse = http.post(
      `${BASE_URL}/api/v1/test/order/pending/${userId}`,
      JSON.stringify(orderPayload),
      {
        headers: { 'Content-Type': 'application/json' },
      }
  );

  const orderCheck = check(orderResponse, {
    'order created': (r) => r.status === 200,
  });

  if (!orderCheck) {
    console.log(`Order failed - Status: ${orderResponse.status}, Body: ${orderResponse.body}`);
    return; // 주문 실패 시 종료
  }

  const response = orderResponse.json();
  const orderId = BigInt(response.result.order_id); // 수정된 부분: result.order_id로 접근
  console.log(`Order created - Order ID: ${orderId}`);

  // 결제 API 호출
  const paymentResponse = http.post(
      `${BASE_URL}/api/v1/test/order/payment/${orderId}/ready/${userId}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
      }
  );

  const response2 = paymentResponse.json();
  const pid = BigInt(response2.result); // 수정된 부분: result.order_id로 접근
  console.log(`Order created - Order ID: ${orderId}`);

  const paymentCheck = check(paymentResponse, {
    'payment ready': (r) => r.status === 200,
  });

  if (!paymentCheck) {
    console.log(`Payment failed - Status: ${paymentResponse.status}, Body: ${paymentResponse.body}`);
  } else {
    console.log(`Payment succeeded - Status: ${paymentResponse.status}, Body: ${paymentResponse.body}`);
  }

  const approveResponse = http.post(
      `${BASE_URL}/api/v1/test/order/payment/in-progress/${pid}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
      }
  );

  const approveCheck = check(approveResponse, {
    'payment approve': (r) => r.status === 200,
  });
}