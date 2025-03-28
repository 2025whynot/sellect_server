import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
  vus: 500,
  // iterations: 1,
  duration: '120s', // 테스트 기간
  tags: {
    name: '', // 기본 태그 비활성화
  },
};

const BASE_URL = 'http://172.16.24.78:8080';

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

  check(orderResponse, { 'order created': (r) => r.status === 200 });

  sleep(0.1); // 100ms 대기 후 다음 반복

  const response = orderResponse.json();
  const orderId = BigInt(response.result.order_id);

  const paymentResponse = http.post(
      `${BASE_URL}/api/v1/test/order/payment/${orderId}/ready/${userId}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'payment_ready' },
      }
  );

  const response2 = paymentResponse.json();
  const pid = BigInt(response2.result);

  check(paymentResponse, { 'payment ready': (r) => r.status === 200 });

  const approveResponse = http.post(
      `${BASE_URL}/api/v1/test/order/payment/in-progress/${pid}`,
      null,
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'payment_in_progress' },
      }
  );

  check(approveResponse, { 'payment approve': (r) => r.status === 200 });

  sleep(0.1); // 100ms 대기 후 다음 반복
}