# 🌍 Intelligent Spatial BI Platform (EES)

본 프로젝트는 단순한 성과 평가 시스템을 넘어, 지리 좌표 기반 공간 분석(GIS) 데이터와 점포 성과 지표를 결합하여 **상권 유형별 운영 난이도를 자동으로 보정하고 분석하는 지능형 공간 BI 플랫폼**입니다.

## 🚀 프로젝트 핵심 가치 (4 Core Principles)
1.  **Context-aware Spatial Intelligence**: 동일한 성과라도 상권 유형(도심형, 일반형, 외곽형)에 따라 분석 반경과 밀도 기준을 다르게 적용하여 '진정한 운영 난이도'를 산출합니다.
2.  **Evaluation Integrity (Immutable Audit Snapshot)**: 평가 확정 시점의 공간 분석 상태를 JSON 형태의 불변(Immutable) 증거로 저장하여, 사후 감사 및 시계열 비교의 신뢰성을 보장합니다.
3.  **Modern GIS Experience**: 카카오맵 API 기반의 **Risk-aware Cluster** 및 공간 시각화를 통해 방대한 지역 데이터와 리스크 비중을 직관적으로 파악할 수 있는 고급 공간 분석 대시보드를 제공합니다.
4.  **Operational Stability**: 시스템 산출 점수와 현장 평가 점수 간의 격차(Gap)를 자동 탐지하고 리스크 등급을 분류하여 경영진의 의사결정을 지원합니다.

## 🧠 Spatial Evaluation Flow
지능형 공간 평가 엔진은 아래와 같은 엄격한 순서에 따라 데이터를 처리합니다.
```text
Branch Location (Coordinates)
    ↓
Region Type Classification (Urban/General/Suburban)
    ↓
Dynamic Radius Calculation (300m / 500m / 1000m)
    ↓
Nearby Same-brand Density Analysis
    ↓
Difficulty Coefficient Derivation (1.05 / 1.03 / 1.00)
    ↓
Operational Score Correction (with Bonus Caps)
    ↓
Immutable Audit Snapshot Storage (JSON)
```

## 🛠️ Tech Stack
- **Backend**: Spring Boot 3.x + MyBatis + MSSQL Spatial
- **Frontend**: Thymeleaf + Vanilla JS + Kakao Maps API + Chart.js
- **Infra**: Azure App Service + Azure SQL Database

## 🗺️ 프로젝트 맵 (Project Map)
### 🎨 Frontend (GIS Dashboard)
- `spatial_dashboard.html`: 메인 분석 화면 (지도 모드 전환, 리스크 집계, 공간 보정 현황)
- `kakaomap-integration.js`: 카카오맵 API 연동 및 **Risk-aware Clusterer** 커스텀 계산 로직

### ⚙️ Backend (Analysis Engine)
- `SpatialAnalysisService`: 공간 분석 **오케스트레이션** 및 Dashboard용 DTO 조합
- `DensityCalculationService`: 상권 유형(region_type)에 따른 **동적 분석 반경** 및 밀집도 판정 엔진
- `DifficultyEvaluationService`: 보정 계수 산출 및 **Bonus Cap(+5점/3점)** 적용 로직
- `EvaluationAnalysisService`: 평가 데이터 Gap 분석 및 리스크 상태 판정

## 📍 Region Types & Definitions
- **URBAN_CORE**: 초고밀도 도심 상권 (분석 반경: 300m / HIGH 기준: 9개 이상)
- **GENERAL_CITY**: 일반 시가지 상권 (분석 반경: 500m / HIGH 기준: 6개 이상)
- **SUBURBAN**: 외곽 및 저밀도 상권 (분석 반경: 1000m / HIGH 기준: 4개 이상)

## ⚠️ 빈번한 실수 방지 (Common Mistakes Prevention)
1.  **Region Type 누락 주의**: 신규 지점 등록 시 상권 유형을 반드시 지정해야 동적 반경 로직이 정상 작동합니다.
2.  **API Key 도메인**: 지도 로딩 실패 시 `application.yml`의 API 키와 카카오 개발자 센터에 등록된 허용 도메인을 확인하십시오.
3.  **JSON 스키마 버전**: `spatial_snapshot` 구조 변경 시 하위 호환성을 위해 `snapshot_version` 필드를 활용하십시오.

## 🔗 접속 및 리소스
- **운영 서버(Azure)**: [ees-eval-hk.azurewebsites.net](https://ees-eval-hk.azurewebsites.net)
- **개발 문서**: [PROJECT_GUIDE.md](PROJECT_GUIDE.md)
- **구현 계획**: [implementation_plan.md](implementation_plan.md)
