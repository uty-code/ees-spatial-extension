# UI Revamp Implementation Plan: Intelligent Multi-store Performance Analysis System (v11)

기존 EES(사원 평가 시스템) UI를 공간 데이터 분석 기능을 중심으로 개편하여, 지점 운영 현황과 사원 성과를 시각적으로 통합 관리할 수 있는 **지능형 공간 BI 대시보드**로 업그레이드합니다.

## 💡 Design Intent (Rationale)
기존 EES는 텍스트와 표 기반의 조회 시스템으로, **지역별 운영 상태나 성과 분포를 직관적으로 파악하기 어려웠습니다.**
이번 개편 UI는 지도 기반 시각화와 실시간 KPI 연동을 통해 **"어느 지역의 어느 지점이 위험하며, 누가 관리하고 있는가"**를 즉각적으로 비교 분석할 수 있도록 설계되었습니다.

---

## 🔐 Role-based Access Control & Security
- **접근 권한**: 관리자/임원만 대시보드 접근 가능 (일반 사원은 메뉴 비노출 또는 리다이렉트)
- **보안 필터**: 사용자 권한에 따라 조회 가능한 권역(Region) 데이터 자동 필터링

---

## 🛠️ Definitions & Logic (정의 및 로직)

### 1. Default View & KPI Source
- **기본 뷰**: 전국 HeatMap + 운영 지점 마커 + 전사 평균 KPI 노출
- **KPI 출처**: 평균 성과 지표는 해당 분기 지점별 `composite_score`의 평균값을 사용함

### 2. Synchronization & Persistence
- **상태 유지**: 페이지 이동 후 복귀 시 `sessionStorage.getItem('spatialFilters')`를 통해 최종 필터 상태를 복원함
- **동기화**: 필터 변경 시 `spatialFilters` 키를 업데이트하고 API 재호출 및 UI 갱신을 즉시 수행함
- **Debounce 전략**: 잦은 필터 변경으로 인한 불필요한 API 호출 방지를 위해 **300ms Debounce** 적용

### 3. Risk Level & Color Policy
| 등급 | 판정 기준 | UI Color |
| :--- | :--- | :--- |
| **NORMAL** | Gap < 15.0 (정상 범위) | Green (#52C41A) |
| **WARNING** | 15.0 <= Gap < 25.0 (주의) | Amber (#FAAD14) |
| **ANOMALY** | Gap >= 25.0 (이상 징후) | Red (#FF4D4F) | ▲ (Triangle) |

### 4. HeatMap Legend & Intensity
- **위치**: 지도 우측 상단 (Floating Overlay)
- **표시**: 
    - 리스크 등급별 색상 + **고유 아이콘**(▲, ●, ✓) 병기
    - Heat Intensity (Density 기반) 그라데이션 가이드 (Low -> High)

### 5. Interaction Flow (Click Stream)
1. **HeatMap Marker Click**: `GET /api/evaluations/executive-context?branchId={id}` 호출하여 상세 데이터 fetch
2. **Side Detail Panel**: 지점별 상세 정보(Trend, Density) 노출
3. **Audit Button Click**: `Snapshot Viewer` (Modal)를 통해 평가 시점 근거 데이터(JSON) 확인

### 6. KPI Refresh & Data Fetch Strategy
| 상황 | 데이터 호출 방식 | 영향 범위 |
| :--- | :--- | :--- |
| **Filter 변경** | Full Reload | 전사 HeatMap + Executive KPI 전체 갱신 |
| **Marker 클릭** | Partial Update | Side Detail Panel 전용 데이터 fetch |
| **Snapshot 열기** | Lazy Fetch | 해당 시점에만 Snapshot JSON fetch |

### 7. Rendering & Accessibility (WCAG)
- **Layer Priority**: 지도상 마커 중첩 시 **ANOMALY > WARNING > NORMAL** 순으로 우선 렌더링하여 위험 지점 가림 방지
- **Visual Accessibility**: 상태 표시 시 색상(Red/Green)에만 의존하지 않고, **고유 아이콘(▲, ●, ✓)**을 병행 표기하여 색약자 접근성 보장
- **Mobile UX Strategy**:
    - 데스크탑: Side Detail Panel (좌/우 고정)
    - 모바일: **Bottom Sheet** 형태로 전환하여 지도 가시성 확보
- **Error Boundary & API Status Handling**: 
    - **401**: Unauthorized 페이지로 리다이렉트
    - **404**: "데이터를 찾을 수 없습니다" 토스트 알림 + Empty State 노출
    - **500**: "서버 오류" 모달 + **Retry(재시도)** 버튼 제공

---

## 📊 Core Dashboard Components

### 1. Executive Intelligence Panel
- **Data Source**: `GET /api/evaluations/executive-context`
- **Key KPI Indicators**:
    - **Avg Final Score**: 전사 평균 최종 점수 (보정 후)
    - **WARNING / ANOMALY Count**: 리스크 판정 건수 (Badge 형태)
    - **Spatial Adjusted Ratio**: 전체 지점 대비 공간 보정 혜택 비율
- **Features**: 
    - 최종 가중 점수 (운영 60% : 정성 40%) 시각화
    - **KPI Animation**: 필터 변경으로 인한 데이터 갱신 시에만 **Count-up 애니메이션** 트리거
    - **Empty State**: 데이터 부재 시 "-", "No Data" 등 명확한 Placeholder 표시
    - **Performance Trend**: 4분기 성과 스파크라인 그래프

### 2. Spatial HeatMap (Kakao Map)
- **Data Source**: `GET /api/spatial/heatmap`
- **Loading & Error UX**: 
    - 지도 영역 전용 **Overlay Spinner**를 통해 데이터 로딩 상태 시각화
    - 지도 타일 로딩 실패 시 **Fallback Background** 및 **재시도(Retry)** 버튼 제공 (네트워크 이슈 대응)
- **Map Control Modes & Strategy**:
    - **[HeatMap]**: 지점 밀집도 기반의 열지도 시각화 (Zoom Level 10 이상)
    - **[Cluster]**: 지점 마커 클러스터링 (Zoom Level 7~9)
    - **[Risk Overlay]**: 리스크 등급 위주의 포인트 시각화 (Zoom Level 6 이하 상세 뷰)
- **Interactive Layers**:
    - **Density Level**: 지점별 `LOW/MID/HIGH` 밀집도 표시
    - **Risk Overlay**: 리스크 레벨에 따른 마커 색상 차별화
    - **Enhanced Tooltip**: 
        - **Hover Delay**: 150~200ms 적용 (불필요한 팝업 방지)
        - `branchName` (지점명)
        - `nearbySameBrandCount` (인근 동일 브랜드 수)
        - `densityLevel` (밀집 등급)
        - `score` (보정 후 최종 점수)
        - `riskLevel` (위험도 판정)

### 3. Evaluation Gap & Audit Trail
- **Gap Analysis List**: `GET /api/evaluations/gap-analysis`를 통해 평가 왜곡(Leniancy/Harshness) 탐지
- **Empty State**: 
    - "선택한 조건에 해당하는 분석 데이터가 없습니다." 안내 문구 출력
    - **Reset Flow**: Reset 클릭 시 `sessionStorage` 초기화 → 초기 View API 재호출 → 전국 HeatMap 복원
- **Snapshot Viewer UI Layout**:
    - **Security**: 해당 기능은 `ROLE_ADMIN` 또는 `ROLE_EXECUTIVE` 권한이 있는 사용자에게만 활성화됨
    - **Header**: 평가 기간, 대상자 정보, 최종 등급
    - **KPI Section**: 운영 지표별 카드 형태 렌더링
    - **Spatial Section**: **Highlight Color**로 보정 계수 및 밀집도 강조
    - **Raw JSON**: JSON 트리 형태의 Collapsible 영역 및 대용량 데이터 대응을 위한 **Virtual Scroll** 적용
    - **Export**: 관리자/임원용 **JSON Copy & Download** 기능 제공 (증빙용)
- **Snapshot Viewer API**: `GET /api/evaluations/snapshots/{id}`

### 4. Loading Priority
| 순위 | 컴포넌트 | 이유 |
| :--- | :--- | :--- |
| **1** | Executive KPI | 가장 가벼운 데이터이며 즉각적인 요약 정보 제공 필요 |
| **2** | Spatial HeatMap | 지리 정보 렌더링 후 마커/열지도 순차 표시 |
| **3** | Snapshot | 사용자 요청 시에만 로드 (On-demand) |

---

## 🛠️ Frontend State Management (Draft)

```javascript
const appState = {
  filters: {
    periodId: null,
    regionCode: 'ALL',
    riskLevel: 'ALL'
  },
  selectedBranch: null,    // 현재 선택된 지도 지점
  selectedSnapshot: null,  // 현재 열린 스냅샷 데이터
  heatmapData: [],         // /api/spatial/heatmap 결과
  executiveContext: [],    // /api/evaluations/executive-context 결과
  loading: {
    heatmap: false,
    executive: false,
    snapshot: false
  }
};

### 2. Cache Strategy
| 데이터 유형 | 캐시 정책 | 이유 |
| :--- | :--- | :--- |
| **HeatMap Data** | Long Cache (Session) | 지리 정보 및 기반 점수는 변동 폭이 적음 |
| **Executive KPI** | Short Cache (5 min) | 실시간 성과 지표 반영 필요 |
| **Snapshot JSON** | No Cache | 감사 무결성을 위해 실시간 fetch 필수 |
```

---

## 🎨 Visual Identity & Aesthetics
- **Theme**: Dark Modern (Aura Blue Accent)
- **Typography**: Inter (Global) / Outfit (Header)
- **Design Principles**:
    - **Glassmorphism**: 카드 UI에 투명도와 블러 효과 적용
    - **Micro-interactions**: 버튼 호버 시 그림자 확장 및 지도 마커 줌 효과

---

## 🏗️ UI Component Structure (프론트엔드 구성)
- **Filter Bar**: 연도, 분기, 지역, 위험도 제어
- **KPI Card**: 핵심 지표 요약 (수치 갱신 시 Fade 애니메이션 및 Count-up 적용)
- **Map Layer**: 카카오맵 기반 클러스터링(상세 탐색용) 및 HeatMap(거시 분석용) 병행
- **Detail Analysis Panel**: 탭(Tab) 구조를 통한 정보 계층화
    - **Tab 1 (정보)**: 지점 주소, 연락처, 주요 운영 현황
    - **Tab 2 (성과)**: 최근 4분기 성과 트렌드 차트 (Chart.js)
    - **Tab 3 (관리)**: 담당 관리자 프로필 및 관리자 최종 평가 점수 비교

---

## ⚡ Performance & UX Flow
- **Initial Loading**: 진입 시 **Skeleton UI**를 우선 렌더링하고, 데이터 로드 완료 후 컴포넌트 표시
- **Responsiveness**: 평균 응답 시간 목표 500ms 및 부드러운 지도 인터랙션 유지
- **PC/Mobile**: PC는 Hover, 모바일은 **Tap 기반 상세 표시** 및 Bottom Sheet 전환 적용
- **Recovery**: 데이터 없음/에러 시 안내 문구와 **필터 초기화 버튼** 제공

---

## 🧪 Verification Plan
1. **상태 복구**: 브라우저 새로고침 및 페이지 이동 후 기존 필터 값이 유지되는지 확인
2. **로딩 검증**: 네트워크 지연 환경에서 Skeleton UI가 정상 노출되는지 확인
3. **탭 인터랙션**: 상세 패널 내 탭 전환 시 각 정보(정보/성과/관리)가 정확히 표시되는지 확인
