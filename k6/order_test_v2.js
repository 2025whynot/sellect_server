import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
  vus: 500,
  // iterations: 1,
  duration: '300s', // 테스트 기간
  tags: {
    name: '', // 기본 태그 비활성화
  },
};

const BASE_URL = 'http://52.79.184.29:8080'; // 실제 API 베이스 URL로 변경 필요
const PAY_BASE_URL = 'http://43.202.235.222:8081'; // 실제 API 베이스 URL로 변경 필요

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

export default function () {
  const userId = getRandomUserId();

  const orderPayload = {
    total_price: "199900",
    order_items: generateOrderItems(),
  };

  const orderResponse = http.post(
      `${BASE_URL}/api/v1/test/order/pending/${userId}`,
      JSON.stringify(orderPayload),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'order_pending' }, // 고정 태그
      }
  );

  const orderCheck = check(orderResponse, {
    'order created': (r) => r.status === 200,
  });

  // if (!orderCheck) {
  //   console.log(`Order failed - Status: ${orderResponse.status}, Body: ${orderResponse.body}`);
  //   return; // 주문 실패 시 종료
  // }

  sleep(0.2); // 100ms 대기 후 다음 반복

  const response1 = orderResponse.json();
  const orderId = BigInt(response1.result.order_id); // 수정된 부분: result.order_id로 접근
  // console.log(`Order created - Order ID: ${orderId}`);

  // 결제 API 호출 전 로그
  // console.log(`Sending payment request for orderId: ${orderId}, userId: ${userId}`);
  const paymentResponse = http.post(
      `${BASE_URL}/api/v1/test/order/payment/${orderId}/ready/${userId}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'pay_ready' }, // 고정 태그
      }
  );

  const response2 = paymentResponse.json();
  const pid = BigInt(response2.result);


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
        tags: { name: 'order_approve' }, // 고정 태그
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