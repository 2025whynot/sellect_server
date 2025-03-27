import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1, // 단일 유저로 순차적 실행
  iterations: 100, // 총 1000번 요청
};

const BASE_URL = 'http://localhost:8080'; // 실제 API 베이스 URL로 변경 필요

// 주문 → 결제 시나리오 (정합성 테스트용)
export default function () {
  const userId = 1;

  // 주문 API 호출
  const orderPayload = {
    total_price: "10000", // productId 1개에 맞춘 가격
    order_items: [
      {
        product_id: 1,
        price: "10000",
        quantity: 1,
      },
    ],
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

  // 결제 API 호출 전 로그
  console.log(`Sending payment request for orderId: ${orderId}, userId: ${userId}`);
  const paymentResponse = http.post(
      `${BASE_URL}/api/v1/test/order/payment/${orderId}/ready/${userId}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
      }
  );

  const paymentCheck = check(paymentResponse, {
    'payment ready': (r) => r.status === 200,
  });

  if (!paymentCheck) {
    console.log(`Payment failed - Status: ${paymentResponse.status}, Body: ${paymentResponse.body}`);
  } else {
    console.log(`Payment succeeded - Status: ${paymentResponse.status}, Body: ${paymentResponse.body}`);
  }


}