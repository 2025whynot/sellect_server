import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 150, // 가상 사용자 수
  iterations: 5000, // 총 요청 수
};

const TARGET_URL = __ENV.TARGET_URL || 'http://localhost:8080';
const FAKE_PAYMENT_URL = __ENV.FAKE_PAYMENT_URL || 'http://localhost:8081';
const TEST_VERSION = __ENV.TEST_VERSION || 'v1'; // 기본값 v1

export default function () {
  const userId = 1;
  const orderPayload = {
    total_price: "10000",
    order_items: [
      {
        product_id: 1,
        price: "10000",
        quantity: 1,
      },
    ],
  };

  // 1. 주문 생성 요청 (pending order)
  const pendingOrderResponse = http.post(
      `${TARGET_URL}/api/v1/test/order/pending/${userId}`,
      JSON.stringify(orderPayload),
      { headers: { 'Content-Type': 'application/json' } }
  );

  const isOrderCreated = check(pendingOrderResponse, {
    'order created': (res) => res.status === 200,
  });

  if (!isOrderCreated) {
    console.log(`Order creation failed - Status: ${pendingOrderResponse.status}, Body: ${pendingOrderResponse.body}`);
    return;
  }

  const orderData = pendingOrderResponse.json();
  const orderId = BigInt(orderData.result.order_id);
  console.log(`Order created - Order ID: ${orderId}`);

  // 2. 결제 준비 요청 (payment ready)
  console.log(`Sending pay-ready request for orderId: ${orderId}, userId: ${userId}`);
  const payReadyResponse = http.post(
      `${TARGET_URL}/api/${TEST_VERSION}/test/order/payment/${orderId}/ready/${userId}`,
      null,
      { headers: { 'Content-Type': 'application/json' } }
  );

  const isPaymentReady = check(payReadyResponse, {
    'payment ready': (res) => res.status === 200,
  });

  if (!isPaymentReady) {
    console.log(`Payment ready failed - Status: ${payReadyResponse.status}, Body: ${payReadyResponse.body}`);
    return;
  }

  // 3. 결제 URL 조회 및 paymentId 추출 (재시도 최대 5회)
  let pid = null;
  for (let attempt = 0; attempt < 5; attempt++) {
    sleep(0.3); // 300ms 대기
    const getPaymentUrlResponse = http.get(
        `${TARGET_URL}/api/${TEST_VERSION}/test/order/payment/${orderId}/redirect-url/${userId}`
    );
    const paymentUrlData = getPaymentUrlResponse.json();
    console.log(paymentUrlData.result.payment_url);
    if (paymentUrlData.result.payment_url) {
      pid = paymentUrlData.result.payment_url.split("/").at(-1);
      console.log(`Payment ID: ${pid}`);
      break;
    }
  }

  // 4. 결제 승인 요청 (fake pay 서버)
  const approvePaymentResponse = http.post(
      `${TARGET_URL}/api/v1/test/order/payment/in-progress/${pid}`,
      null,
      { headers: { 'Content-Type': 'application/json' } }
  );

  const isPaymentApproved = check(approvePaymentResponse, {
    'payment approved': (res) => res.status === 200,
  });

  if (!isPaymentApproved) {
    console.log(`Payment approval failed - Status: ${approvePaymentResponse.status}, Body: ${approvePaymentResponse.body}`);
  } else {
    console.log(`Payment approval succeeded - Status: ${approvePaymentResponse.status}, Body: ${approvePaymentResponse.body}`);
  }
}