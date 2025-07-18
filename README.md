

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

## 🚀 기술적 도전 과제 및 개선 사항

본 프로젝트는 다음과 같은 주요 기술적 문제들을 해결하고 성능을 개선하는 데 집중했습니다. 각 항목에 대한 자세한 내용은 링크된 Wiki 문서를 참고해주세요.

### 1. 선착순 쿠폰 성능 개선

문제: 동시 사용자 5,000명 기준 API 응답 시간이 36.99초로 측정되었으며, 선착순 쿠폰 시스템 특성상 빠른 응답 속도가 서비스 품질에 치명적이라고 판단해 성능 개선 작업을 수행함.

**해결 과정**

- (1) **락 성능 측정 실험 (`ReentrantLock` vs `DB Pessimistic Lock` vs `Redis Distributed Lock`)**
    - 3가지 락 방식에 대해 응답 시간 비교를 진행했으나, 시간 차이는 유의미하지 않았고 구조적인 개선이 필요하다고 판단함.
- (2) **Redis + 이벤트 큐 기반 구조 적용**
    - Redis를 통해 재고 차감을 비동기 처리하고, 내부 이벤트 큐를 통해 DB와의 동기화를 수행함.
    - Redis의 원자 연산으로 동시성 문제를 해결했으나, 이벤트 처리 지연으로 인해 DB 재고와의 불일치 문제가 발생함.
- (3) **Redis Set 기반 재고 관리 구조로 전환**
    - 쿠폰 발급 시 사용자 ID를 Redis Set에 저장하여 중복 발급 방지와 재고 관리를 동시에 수행.
    - 재고 판단 기준을 Redis Set의 크기로 변경하여 DB와의 불일치 문제를 제거함.

**성과**
- 구조 개선을 통해 병목이 제거되고, 응답 시간이 36.99초에서 1.87초로 단축되어 95% 이상의 성능 향상 달성
- [Wiki: 선착순 쿠폰 성능 개선](https://github.com/2025whynot/sellect_server/wiki/%5B%EC%84%B1%EB%8A%A5%EA%B0%9C%EC%84%A0%5D-%EC%84%A0%EC%B0%A9%EC%88%9C-%EC%BF%A0%ED%8F%B0-%EC%84%B1%EB%8A%A5-%EA%B0%9C%EC%84%A0)

**개선 과정별 성능 비교**  
<img src="https://github.com/user-attachments/assets/b0042365-b25f-4ed3-b818-e9610bf93bbb" width="600" alt="쿠폰 성능 개선 최종 비교 그래프"/>

**최종 아키텍처 (Ver 3)**  
<img src="https://github.com/user-attachments/assets/d7a06e98-510e-40fb-9809-89ee6f147548" width="600" alt="쿠폰 시스템 최종 아키텍처"/>
## 📖 Wiki 및 참고 자료 

프로젝트 진행 중 겪었던 문제 해결 과정과 기술적 결정에 대한 더 자세한 내용은 아래 Wiki 페이지에서 확인하실 수 있습니다.
<br>
(**[Sellect Server Wiki](https://github.com/2025whynot/sellect_server/wiki)** )
<br>

## 📼 시연 영상

[https://github.com/2025whynot/sellect_client/issues/60#issue-2962480269](https://github.com/user-attachments/assets/370ddd1c-79d8-4a17-b1ad-5882d1a9c5c1)
