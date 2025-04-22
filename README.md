

## 📚 프로젝트 소개

저희 팀은 쿠팡, 11번가, 무신사와 같은 국내 최상위 이커머스 플랫폼의 핵심 요구사항을 분석하여, 대규모 트래픽에서의 안정적인 성능 확보를 목표로 삼았습니다. 이를 위해 체계적인 3단계 고도화 과정을 거쳐 기능을 개발하였습니다. 

1. **기획 및 MVP 구축**
	- 초기 기획 단계부터 핵심 요구사항에 집중하여 MVP를 신속하게 구현했습니다.
2. **단위/통합 테스트 및 1차 성능 개선**
	- 핵심 기능에 대해 단위/통합 테스트를 진행하였습니다.
	- 검색 쿼리 튜닝 및 Spring Batch의 성능 개선을 이루었습니다.
	- 선착순 쿠폰 발급, 주문/결제 로직에서 동시성 이슈를 방지하기 위한 다양한 전략을 수립하였습니다.
3. **부하 테스트 및 2차 성능 개선**
	- 선착순 쿠폰 발급과 주문/결제 로직에 대한 부하 테스트를 진행하였습니다.
	- 다양한 동시성 이슈와 병목 현상을 해결하였습니다.
	- 사전에 수립한 전략들을 비교/분석하여 최적의 솔루션으로 성능 개선을 이루었습니다.

📅 기간: 2025.02.03 - 2025.04.02


<br>

## 🛠 기술 스택
<img width="700" alt="기술스택" src="https://github.com/user-attachments/assets/41e9d66b-da1e-42d2-8f59-6b3c6efd5c12" />

<br>
<br>

## 📌 주요 기능

- **상품 검색:** 다양한 조건(카테고리, 브랜드, 키워드, 가격) 및 정렬 기준(최신순, 관련도순)을 지원하는 상품 검색
- **검색어 자동완성:** 사용자 검색 로그 기반 인기 검색어 Top 10 제안
- **선착순 쿠폰:** 특정 상품에 대한 선착순 쿠폰 발급 및 다운로드
- **주문 및 결제:** 상품 주문 시 카카오페이를 통한 결제. 주문 및 결제 내역 확인 가능
- **장바구니**: 주문할 상품을 장바구니에 담기 
- **(판매자 전용) 상품 등록**: 판매 상품 등록. 상품 이미지 등록 및 수정 시 이미지 순서 조정 가능
- **(판매자 전용) 판매 상품 통계 조회**: 판매 상품에 대한 주문량 및 매출 조회

<br>
<br>

## 📈 단위 테스트 및 통합 테스트

- 고도화할 기능 기준으로 테스트 커버리지 **80% 이상**을 유지
- Service 계층과 Controller 계층을 중심으로 단위 테스트를 작성
- 단위 테스트에서 FakeRepository를 구현함으로써 Mockito를 이용하는 Mocking을 줄임
- GitHub Actions를 활용한 자동 테스트 및 빌드 파이프라인 구축

<img width="600" alt="tests_passed" src="https://github.com/user-attachments/assets/c82089bd-12c6-4d7b-b7d6-6285392d80ff" />
<img width="600" alt="test_coverage" src="https://github.com/user-attachments/assets/d9dcd4b1-bd36-41be-b1d3-3ea6ec12afbf" />

<br>
<br>

## 🚀 기술적 도전 과제 및 개선 사항

본 프로젝트는 다음과 같은 주요 기술적 문제들을 해결하고 성능을 개선하는 데 집중했습니다. 각 항목에 대한 자세한 내용은 링크된 Wiki 문서를 참고해주세요.

### 1. 선착순 쿠폰 성능 개선

- DB 락 기반 방식의 한계를 극복하고, Redis 분산 락과 Atomic 연산을 활용한 **Lock-Free 구조** 구현
- 응답 속도 및 처리량 대폭 향상
- [Wiki: 선착순 쿠폰 성능 개선](https://github.com/2025whynot/sellect_server/wiki/%5B%EC%84%B1%EB%8A%A5%EA%B0%9C%EC%84%A0%5D-%EC%84%A0%EC%B0%A9%EC%88%9C-%EC%BF%A0%ED%8F%B0-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0)

**개선 과정별 성능 비교**  
<img src="https://github.com/user-attachments/assets/b0042365-b25f-4ed3-b818-e9610bf93bbb" width="600" alt="쿠폰 성능 개선 최종 비교 그래프"/>

**최종 아키텍처 (Ver 3)**  
<img src="https://github.com/user-attachments/assets/d7a06e98-510e-40fb-9809-89ee6f147548" width="600" alt="쿠폰 시스템 최종 아키텍처"/>

<br>

### 2. 결제 시스템 장애 해결

- **문제:** 결제 승인 API 호출 시 주문 로직에서 재고 관리에 대한 동시성 이슈 발생
- **해결 과정**:
	- (1) DB 락 적용 -> 데드락 문제 발생
		- 데드락 해결 방법 중 예방/회피/회복을 시도함
		- **방법1 - 예방**: 격리수준을 `SERIALIZABLE`로 적용. 하지만 격리수준에 대한 잘못된 이해로 해당 방법은 잘못된 접근 방식이라는 것을 인식했고, 오히려 또다른 데드락 문제를 야기함
		- **방법2 - 회피**: `productId` 정렬 후 x-lock 획득하는 전략 시도. 하지만 트래픽이 집중되는 상황에서 특정 락에 대한 대기시간이 길어지면서 timeout 발생
		- **방법3 - 회복**: `DeadlockException` 발생 시 재시도하는 로직 추가. 트랜잭션 충돌이 자주 발생하지 않는 상황에서는 합리적인 선택임을 확인
		- [Wiki: DB 데드락 해결](https://github.com/2025whynot/sellect_server/wiki/%5B%ED%8A%B8%EB%9F%AC%EB%B8%94%EC%8A%88%ED%8C%85%5D-%EA%B2%B0%EC%A0%9C-%EC%8B%9C%EC%8A%A4%ED%85%9C-%EC%9E%A5%EC%95%A0-%ED%95%B4%EA%B2%B0%EA%B8%B0-%E2%80%90-2.-DB-%EB%8D%B0%EB%93%9C%EB%9D%BD-%ED%95%B4%EA%B2%B0:-%EB%8B%A4%EC%96%91%ED%95%9C-%EA%B8%B0%EB%B2%95-%EB%B6%84%EC%84%9D-%EB%B0%8F-%EC%B5%9C%EC%A0%81%ED%99%94)
	- (2) Redis 분산 락
		- 동시성 이슈는 원천적으로 차단할 수 있으나, DB 락보다 성능이 오히려 저하
		- 이는 결제 승인 흐름 전체를 직렬 처리한 것에서 기인
	- (3) Redis 재고 관리 (Lua Script 적용) 
		- 재고 차감을 MySQL에서 Redis로 이동
		- Redis에서 재고 차감 시 Lua Script를 이용한 원자적 연산 적용
		- 이를 통해 RDB 트랜잭션에 대한 부하는 감소시키면서, 재고 차감 로직을 Redis에서 빠른 속도로 실행할 수 있었기 때문에 유의미한 성능 개선을 이룸
		-  [Wiki: 분산 락의 한계와 Redis 기반 재고 관리를 통한 성능 개선](https://github.com/2025whynot/sellect_server/wiki/%5B%EC%84%B1%EB%8A%A5%EA%B0%9C%EC%84%A0%5D-%EA%B2%B0%EC%A0%9C-%EC%8B%9C%EC%8A%A4%ED%85%9C-%EC%9E%A5%EC%95%A0-%ED%95%B4%EA%B2%B0%EA%B8%B0-%E2%80%90-3.-%EB%B6%84%EC%82%B0-%EB%9D%BD%EC%9D%98-%ED%95%9C%EA%B3%84%EC%99%80-Redis-%EA%B8%B0%EB%B0%98-%EC%9E%AC%EA%B3%A0-%EA%B4%80%EB%A6%AC%EB%A5%BC-%ED%86%B5%ED%95%9C-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0)

**성능 비교**
<br>
<img src="https://github.com/user-attachments/assets/af533a8d-dc5a-4568-8c58-5fe7cb9495f3" width="600" alt="결제 방식별 성능 비교"/>

**Redis 기반 재고 관리 흐름**
<br>
<img src="https://github.com/user-attachments/assets/99e9b0cd-3213-4941-8b7f-32ffa8d6f719" width="800" alt="Redis 재고 관리" />

<br>

### 3. 검색 쿼리 튜닝

- **문제**: 약 43만 건의 상품 데이터와 160만 건 이상의 연관 데이터를 대상으로 하는 검색 쿼리를 실행한 결과, 쿼리 실행 시간이 **4736ms**로 측정. 크게 4가지 원인이 있었음
	- (1) `LIKE '%...%'` 조건으로 인해 B-트리 인덱스 활용 불가
	- (2) 메인 쿼리 결과 수만큼 스칼라 서브쿼리가 반복 실행
	- (3) `OR` 조건으로 인해 product 테이블이 풀 스캔된 후 JOIN됨
	- (4) brand 및 category 테이블이 전체 행에 대해 JOIN됨
- **해결 과정**:
	- **Full-Text Index를 적용**하여 키워드 검색 성능을 개선
	- 상품 이미지 조회용 **스칼라 서브쿼리를 JOIN으로** 변경
	- OR 조건을 사용하지 않고, **UNION으로 조건 분리**
	- 불필요한 **LEFT JOIN을 INNER JOIN으로** 변경
- **결과**: 검색 쿼리 실행 시간을 **5.45ms**까지 줄였으며, 기존 대비 868배 성능 개선
 - [Wiki: 상품 검색 쿼리 튜닝](https://github.com/2025whynot/sellect_server/wiki/%5B%EC%84%B1%EB%8A%A5%EA%B0%9C%EC%84%A0%5D-%EC%83%81%ED%92%88-%EA%B2%80%EC%83%89-%EC%BF%BC%EB%A6%AC-%ED%8A%9C%EB%8B%9D)

<br>

### 4. 대용량 업데이트 Batch 성능 개선

- **문제**: 하루 1,000만 건의 로그 데이터를 처리하는 Spring Batch 성능 테스트 결과, 실행 시간이 **66.7시간**으로 측정
- **해결 과정**:
	- Dirty Checking으로 인한 개별 업데이트 쿼리가 발생하는 문제를 **JdbcBatchItemWriter**를 도입하여 처리 성능을 개선하였지만, 여전히 Dirty Checking이 발생함
	- 정확한 원인 파악 후, JPA 의존성을 제거하는 방향으로 문제 해결 시도. 실행 시간을 **7.92시간**으로 단축
	- 이후 병렬 처리도 시도하였으나 데드락 문제가 발생하였고, 쓰기 성능보다 읽기 성능이 더 중요하다는 점을 인식하게 되어 구조적인 개선을 진행
	- 로그 데이터의 특성을 기반으로 Batch 종료 시 마지막 처리 로그 ID(PK)를 Batch 메타 테이블에 저장하고, 이후 실행 시에는 해당 ID 이후부터 처리하도록 로직을 변경 (**클러스터 인덱스 활용**)
	- 이를 통해 집계 쿼리를 적용하고, 전체 흐름을 단순화시킴
- **결과**: Batch 실행 시간을 **25초**까지 단축
- [Wiki: 자동완성 Batch 성능 개선](https://github.com/2025whynot/sellect_server/wiki/%5B%EC%84%B1%EB%8A%A5%EA%B0%9C%EC%84%A0%5D-%EA%B2%80%EC%83%89%EC%96%B4-%EC%9E%90%EB%8F%99%EC%99%84%EC%84%B1-%ED%82%A4%EC%9B%8C%EB%93%9C-%EC%97%85%EB%8D%B0%EC%9D%B4%ED%8A%B8-Batch-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0)

**성능 비교**
<br>
<img src="https://github.com/user-attachments/assets/ff0be924-81ac-4346-9886-56ffcf0f3a45" width="600" alt="Batch 성능 개선 정리"/>

<br>

## 📖 Wiki 및 참고 자료 

프로젝트 진행 중 겪었던 문제 해결 과정과 기술적 결정에 대한 더 자세한 내용은 아래 Wiki 페이지에서 확인하실 수 있습니다.
<br>
(**[Sellect Server Wiki](https://github.com/2025whynot/sellect_server/wiki)** )

<br>
<br>

## 📼 시연 영상

[https://github.com/2025whynot/sellect_client/issues/60#issue-2962480269](https://github.com/user-attachments/assets/370ddd1c-79d8-4a17-b1ad-5882d1a9c5c1)
