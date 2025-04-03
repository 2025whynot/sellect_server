import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1, // 유저 몇 명
  iterations: 1, // 총 요청
  // iterations: 100000, // 총 요청
  // duration: '180s', // 테스트 기간
  tags: {
    name: '', // 기본 태그 비활성화
  },
};

// const BASE_URL = 'http://52.79.184.29:8080'; // 실제 API 베이스 URL로 변경 필요
// const PAY_BASE_URL = 'http://43.202.235.222:8081'; // 실제 API 베이스 URL로 변경 필요
const BASE_URL = 'http://localhost:8080'; // 실제 API 베이스 URL로 변경 필요
const PAY_BASE_URL = 'http://localhost:8081'; // 실제 API 베이스 URL로 변경 필요


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

  const response1 = orderResponse.json();
  const orderId = BigInt(response1.result.order_id); // 수정된 부분: result.order_id로 접근
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
      `${PAY_BASE_URL}/v1/payment/in-progress/${pid}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
      }
  );

  const approveCheck = check(approveResponse, {
    'payment approve': (r) => r.status === 200,
  });

  if (!approveCheck) {
    console.log(`Payment failed - Status: ${approveResponse.status}, Body: ${approveResponse.body}`);
  } else {
    console.log(`Payment succeeded - Status: ${approveResponse.status}, Body: ${approveResponse.body}`);
  }
}