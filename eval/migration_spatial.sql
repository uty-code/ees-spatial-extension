SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ARITHABORT ON;
GO

-- 1. 프랜차이즈 브랜드 정보 테이블 생성
CREATE TABLE brands_51 (
    brand_id    BIGINT IDENTITY(1,1) PRIMARY KEY,
    brand_name  NVARCHAR(100) NOT NULL,
    category    NVARCHAR(50),
    created_at  DATETIME DEFAULT GETDATE()
);

-- 2. 지점 정보 테이블 생성 (공간 인덱스, 운영 상태 추적 및 UNIQUE 제약 조건 포함)
CREATE TABLE branches_51 (
    branch_id        BIGINT IDENTITY(1,1) PRIMARY KEY,
    brand_id         BIGINT         NOT NULL,
    branch_name      NVARCHAR(100)  NOT NULL,
    address          NVARCHAR(255)  NOT NULL,
    latitude         DECIMAL(10,7)  NOT NULL,
    longitude        DECIMAL(10,7)  NOT NULL,
    -- WGS84(EPSG:4326) 좌표계를 사용하여 카카오맵과 완벽하게 호환되는 geography 타입
    location         AS GEOGRAPHY::Point(latitude, longitude, 4326) PERSISTED,
    region_code      VARCHAR(10),
    operating_status VARCHAR(20)    DEFAULT 'OPERATING', -- 운영 상태 추적 (폐점률, 생존율 분석용)
    opened_at        DATETIME,
    closed_at        DATETIME,
    is_deleted       CHAR(1)        DEFAULT 'n',
    version          INT            DEFAULT 0,
    created_at       DATETIME       DEFAULT GETDATE(),
    created_by       BIGINT,
    updated_at       DATETIME,
    updated_by       BIGINT,
    CONSTRAINT FK_branch_brand FOREIGN KEY (brand_id) REFERENCES brands_51(brand_id),
    CONSTRAINT UK_BRANCH UNIQUE (brand_id, branch_name, address) -- 중복 삽입 방지 제약조건
);

-- 반경 검색 등 공간 쿼리의 성능 최적화를 위한 Spatial Index 생성
CREATE SPATIAL INDEX IDX_BRANCH_LOCATION ON branches_51(location);

-- 3. 지점별 성과 테이블 생성
CREATE TABLE branch_performance_51 (
    perf_id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    branch_id         BIGINT       NOT NULL,
    perf_year         INT          NOT NULL,
    perf_quarter      INT          NOT NULL,  -- 1~4분기
    revenue_growth    DECIMAL(5,2),           -- 매출 증감률 (%)
    hygiene_score     DECIMAL(5,2),           -- 위생 점검 점수
    claim_count       INT DEFAULT 0,          -- 클레임 건수
    customer_score    DECIMAL(5,2),           -- 고객 만족도
    composite_score   DECIMAL(5,2),           -- 종합 성과 점수
    is_deleted        CHAR(1) DEFAULT 'n',
    version           INT     DEFAULT 0,
    created_at        DATETIME DEFAULT GETDATE(),
    created_by        BIGINT,
    updated_at        DATETIME,
    updated_by        BIGINT,
    CONSTRAINT FK_perf_branch FOREIGN KEY (branch_id) REFERENCES branches_51(branch_id)
);

-- 4. 지점-관리자 연결 브릿지 테이블 생성 (기존 employees_51 테이블 수정 없이 기능 확장)
CREATE TABLE branch_managers_51 (
    branch_id  BIGINT NOT NULL,
    emp_id     BIGINT NOT NULL,
    is_deleted CHAR(1) DEFAULT 'n',
    version    INT     DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    created_by BIGINT,
    updated_at DATETIME,
    updated_by BIGINT,
    PRIMARY KEY (branch_id, emp_id),
    CONSTRAINT FK_mgr_branch FOREIGN KEY (branch_id) REFERENCES branches_51(branch_id),
    -- employees_51 테이블은 기존 EES 시스템의 사원 테이블이라고 가정
    CONSTRAINT FK_mgr_employee FOREIGN KEY (emp_id) REFERENCES employees_51(emp_id)
);
