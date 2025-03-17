# 주문 서비스 리팩토링 문서 (v0After -> v1After)

## 1. 개요

기존 `OrderServiceV0After`는 `PaymentServiceV0`와 강하게 결합되어 있어 유지보수성과 확장성이 떨어지는 구조였다. 또한, 트랜잭션 범위가 넓어져 성능 저하가 발생하고, 동시성 처리 및 주문 상태 관리가 복잡했다. 이를 해결하기 위해 `OrderServiceV1After`에서는 이벤트 기반 아키텍처를 도입하여 **결제 로직을 주문 서비스에서 분리하고 트랜잭션을 최적화하는 방식으로 개선**했다.

---

## 2. 기존 문제점 (`OrderServiceV0After`)

### 1) 결제 서비스와의 강한 결합

- `OrderServiceV0After`에서 결제 처리를 직접 수행 (`paymentService.getKakaoPayReadyResponse()`, `paymentService.findReadyPaymentByPid()` 호출)
- 결제 로직 변경 시 `OrderServiceV0After`도 함께 수정해야 하는 문제 발생

### 2) 트랜잭션 범위 증가로 인한 성능 저하

- 주문 승인(`approvePayment()`) 시 결제 승인 API 호출이 같은 트랜잭션 내에서 실행됨
- 외부 API 요청이 포함된 긴 트랜잭션으로 인해 성능 저하 및 롤백 시 불필요한 데이터 변경 발생 가능

### 3) 동시성 처리 및 락 이슈

- `Orders` 엔티티에 대한 비관적 락(PESSIMISTIC_WRITE) 적용으로 인해 동시 주문 처리 성능 저하
- `Inventory`(재고) 차감 시에도 락을 사용해 전체적인 트랜잭션 속도가 느려짐

### 4) 주문 상태 변경의 복잡성

- 주문 상태 변경(`OrderStatus`)이 결제와 직접 연결되어 있어 새로운 결제 방식 추가 시 로직이 복잡해짐
- 주문 상태 변경과 결제 승인 로직이 결합되어 있어 유지보수가 어려움

---

## 3. 개선된 구조 (`OrderServiceV1After`)

### 1) 결제 요청 비동기 이벤트 처리

기존의 동기 방식 대신 **이벤트 기반 비동기 방식**으로 변경하여 `OrderServiceV1After`와 결제 서비스 간의 결합도를 낮췄다.

```java
CompletableFuture<String> future = new CompletableFuture<>();
KakaoPayReadyEvent event = new KakaoPayReadyEvent(this, user, orderId, order, future);
eventPublisher.publishEvent(event);
String nextRedirectPcUrl = future.get();
```

- 주문 서비스는 결제 요청 이벤트(`KakaoPayReadyEvent`)만 발생시키고, 결제 처리는 `PaymentEventListener`에서 비동기 실행
- `CompletableFuture`를 활용해 비동기 요청이 완료된 후 리디렉션 URL을 반환

**변경 효과**

- 결제 서비스와의 결합도를 낮춰 유지보수성을 향상
- 주문과 결제의 트랜잭션을 분리하여 성능 최적화

---

### 2) 결제 승인 이벤트 처리

기존에는 `approvePayment()`에서 직접 결제 승인 API를 호출했지만, 이제는 이벤트를 발행하고 비동기 처리하도록 변경했다.

```java

KakaoPayApproveEvent event = KakaoPayApproveEvent.publish(payment, token, pid);
eventPublisher.publishEvent(event);
```

결제 승인 로직은 `PaymentEventListener`에서 `@TransactionalEventListener`를 통해 트랜잭션이 완료된 후 실행된다.

```java
@Async("paymentTaskExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void kakaoPayApproveEvent(KakaoPayApproveEvent event) {
    Payment approvePayment = approveAndSavePayment(event);
    requestKakaoPayApporve(event, approvePayment);

```

**변경 효과**

- 트랜잭션이 완료된 후 결제 승인 API를 호출하여 트랜잭션 범위를 최소화
- 주문 승인과 결제 승인 로직이 분리되어 유지보수성과 확장성이 증가

---

### 3) 주문 상태 변경 방식 개선

기존에는 주문 상태(`OrderStatus`) 변경이 `OrderServiceV0After` 내부에서 직접 이루어졌지만, 이제는 이벤트를 통해 자연스럽게 상태가 변경된다.

```java
ordersRepository.save(order.changeStatus(OrderStatus.COMPLETED));
```

- 주문이 완료되면 이벤트 기반으로 상태가 변경되므로 주문 서비스의 로직이 단순해짐

**변경 효과**

- 주문 상태 관리가 결제 승인과 분리되어 새로운 결제 방식 추가가 용이
- 주문 상태 변경 로직이 간결해지고, 확장성이 높아짐

---

## 4. 개선된 구조의 장단점

### 장점

1. **서비스 간 결합도 감소**
    - 이벤트 기반 아키텍처를 도입하여 주문과 결제 서비스 간 강한 의존성을 제거
2. **트랜잭션 성능 최적화**
    - 결제 승인 API 호출을 `AFTER_COMMIT`에서 실행하여 트랜잭션 시간을 단축
3. **동시성 문제 해결**
    - 주문과 결제 트랜잭션을 분리하여 불필요한 락 사용을 줄이고 성능을 향상
4. **유지보수성과 확장성 향상**
    - 새로운 결제 방식 추가 시 기존 로직을 수정하지 않고 이벤트 리스너만 추가하면 됨

### 단점

1. **이벤트 기반으로 인해 디버깅 난이도 증가**
    - 코드 실행 흐름이 명확하지 않기 때문에 로깅과 모니터링이 중요
2. **이벤트 처리 지연 가능성**
    - 비동기 이벤트 실행이므로 예상보다 늦게 실행될 가능성이 있음
3. **보상 트랜잭션(Saga) 적용 필요**
    - 결제 실패 시 주문 상태를 원복하는 로직이 필요하며, 추가 설계가 필요

---

## 5. 결론

기존 `OrderServiceV0After`는 주문과 결제 서비스 간 결합도가 높고, 트랜잭션 범위가 넓어 성능 저하와 유지보수 어려움이 있었다.

이를 해결하기 위해 **이벤트 기반 아키텍처를 적용하여 주문과 결제 로직을 분리하고, 트랜잭션을 최적화**하는 방식으로 개선했다.

이번 개선을 통해

- 주문과 결제 서비스 간 결합도를 줄이고,
- 성능 최적화 및 유지보수성을 향상했으며,
- 확장성을 고려한 구조로 변경했다.

향후 보상 트랜잭션(Saga) 패턴을 적용하여 결제 실패 시 롤백 전략을 보완할 예정이다.