-- MSSQL EES 전체 테이블 생성 스크립트 (소문자 및 락 기본 정책 반영)

alter database scoped configuration set IDENTITY_CACHE = OFF;
-- 이전 _51 테이블의 외래 키 제약 조건만 삭제 (다른 팀 테이블 보호)
EXEC sp_executesql N'
DECLARE @drop_constraints_sql NVARCHAR(MAX) = N'''';
SELECT @drop_constraints_sql += ''ALTER TABLE '' + QUOTENAME(schema_name(schema_id)) + ''.'' + QUOTENAME(object_name(parent_object_id)) +
    '' DROP CONSTRAINT '' + QUOTENAME(name) + '';''
FROM sys.foreign_keys
WHERE object_name(parent_object_id) LIKE ''%_51'';
EXEC sp_executesql @drop_constraints_sql;
';

-- 이전 _51 테이블의 외래 키 제약 조건만 삭제 (다른 팀 테이블 보호)
drop table if exists branch_managers_51;
drop table if exists branch_performance_51;
drop table if exists branches_51;
drop table if exists brands_51;
drop table if exists evaluation_type_weights_51;
drop table if exists evidences_51;
drop table if exists login_logs_51;
drop table if exists final_grades_51;
drop table if exists evaluations_51;
drop table if exists evaluator_mappings_51;
drop table if exists evaluation_elements_51;
drop table if exists evaluation_periods_51;
drop table if exists employee_roles_51;
drop table if exists interviews_51;
drop table if exists employees_51;
drop table if exists departments_51;
drop table if exists roles_51;
drop table if exists positions_51;
drop table if exists common_codes_51;
drop table if exists api_logs_51;

-- ==========================================
-- 1. 기초 시스템 데이터
-- ==========================================
create table common_codes_51
(
    code_id bigint identity(1,1) primary key,
    group_code varchar(50) not null,
    code_value varchar(50) not null,
    code_name nvarchar(100) not null,
    description nvarchar(255),
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint
);

create table positions_51
(
    position_id bigint identity(1,1) primary key,
    position_name nvarchar(50) not null,
    hierarchy_level int not null,
    weight_base decimal(5,2) not null,
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint
);

create table roles_51
(
    role_id bigint identity(1,1) primary key,
    role_name varchar(50) not null,
    description nvarchar(255),
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint
);

-- ==========================================
-- 2. 조직 및 사용자
-- ==========================================
create table departments_51
(
    dept_id bigint identity(1,1) primary key,
    parent_dept_id bigint,
    leader_id bigint,
    dept_name nvarchar(100) not null,
    is_active char(1) default 'y' not null,
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint,
    foreign key (parent_dept_id) references departments_51(dept_id)
);

create table employees_51
(
    emp_id bigint primary key,
    dept_id bigint not null,
    position_id bigint not null,
    password varchar(255) not null,
    name nvarchar(50) not null,
    email varchar(255),
    phone varchar(50),
    status_code varchar(20) default 'employed',
    -- 재직/휴직/퇴사 상태 관리
    hire_date date,
    retire_date date,
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint,
    foreign key (dept_id) references departments_51(dept_id),
    foreign key (position_id) references positions_51(position_id)
);

-- departments_51.leader_id FK 추가 (employees_51 생성 이후)
alter table departments_51
    add constraint fk_dept_leader
    foreign key (leader_id) references employees_51(emp_id);


create table employee_roles_51
(
    emp_id bigint not null,
    role_id bigint not null,
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint,
    primary key (emp_id, role_id),
    foreign key (emp_id) references employees_51(emp_id),
    foreign key (role_id) references roles_51(role_id)
);

create table login_logs_51
(
    log_id bigint identity(1,1) primary key,
    emp_id bigint,
    login_input varchar(255) not null,
    result_code varchar(20) not null,
    is_failure char(1) default 'n',
    -- 로그인 실패 여부 (y/n)
    ip_address varchar(50),
    user_agent nvarchar(max),
    created_at datetime default getdate(),
    foreign key (emp_id) references employees_51(emp_id)
);

-- API 호출 이력 로그 (비즈니스 API 호출 추적용, login_logs_51과 역할 분리)
create table api_logs_51
(
    log_id bigint identity(1,1) primary key,
    api_url varchar(500) not null,
    http_method varchar(10) not null,
    request_content nvarchar(max),
    response_content nvarchar(max),
    result_code varchar(20),
    ip_address varchar(45),
    target_id bigint,
    trace_id varchar(50),
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint
);

create index idx_api_logs_created_at on api_logs_51(created_at desc);
create index idx_api_logs_target_id on api_logs_51(target_id);
create index idx_api_logs_created_by on api_logs_51(created_by);
create index idx_api_logs_ip on api_logs_51(ip_address);

-- ==========================================
-- 3. 평가 기준 및 매핑
-- ==========================================
create table evaluation_periods_51
(
    period_id bigint identity(1,1) primary key,
    period_year int not null,
    period_name nvarchar(100) not null,
    status_code varchar(50) not null,
    start_date date,
    end_date date,
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint
);

create table evaluation_elements_51
(
    element_id bigint identity(1,1) primary key,
    period_id bigint not null,
    dept_id bigint,
    element_type_code varchar(50) not null,
    element_name nvarchar(255) not null,
    max_score decimal(5,2) not null,
    weight decimal(5,2) not null,
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint,
    foreign key (period_id) references evaluation_periods_51(period_id),
    foreign key (dept_id) references departments_51(dept_id)
);

create table evaluation_type_weights_51
(
    weight_id bigint identity(1,1) primary key,
    period_id bigint not null,
    dept_id bigint,
    target_role_code varchar(50) not null,
    element_type_code varchar(50) not null,
    weight decimal(5,2) not null,
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint,
    foreign key (period_id) references evaluation_periods_51(period_id),
    foreign key (dept_id) references departments_51(dept_id)
);

create table evaluator_mappings_51
(
    mapping_id bigint identity(1,1) primary key,
    period_id bigint not null,
    evaluatee_id bigint not null,
    evaluator_id bigint not null,
    relation_type_code varchar(50) not null,
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint,
    foreign key (period_id) references evaluation_periods_51(period_id),
    foreign key (evaluatee_id) references employees_51(emp_id),
    foreign key (evaluator_id) references employees_51(emp_id)
);

-- ==========================================
-- 4. 평가 수행
-- ==========================================
create table evaluations_51
(
    eval_id bigint identity(1,1) primary key,
    mapping_id bigint not null,
    element_id bigint not null,
    score int,
    reason nvarchar(255),
    confirm_status_code varchar(50),
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint,
    foreign key (mapping_id) references evaluator_mappings_51(mapping_id),
    foreign key (element_id) references evaluation_elements_51(element_id)
);

create table interviews_51
(
    interview_id bigint identity(1,1) primary key,
    mapping_id bigint not null,
    content1 nvarchar(max),
    content2 nvarchar(max),
    content3 nvarchar(max),
    content4 nvarchar(max),
    status_code varchar(50),
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint,
    foreign key (mapping_id) references evaluator_mappings_51(mapping_id)
);

create table evidences_51
(
    evidence_id bigint identity(1,1) primary key,
    eval_id bigint not null,
    file_name nvarchar(255) not null,
    file_path nvarchar(500) not null,
    file_size bigint,
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint,
    foreign key (eval_id) references evaluations_51(eval_id)
);

-- ==========================================
-- 5. 최종 결과 관리
-- ==========================================
create table final_grades_51
(
    grade_id bigint identity(1,1) primary key,
    period_id bigint not null,
    emp_id bigint not null,
    total_score int,
    final_grade_code varchar(50),
    spatial_snapshot nvarchar(max), -- Immutable Audit 저장소 (JSON)
    is_deleted char(1) default 'n',
    version int default 0,
    created_at datetime default getdate(),
    created_by bigint,
    updated_at datetime,
    updated_by bigint,
    foreign key (period_id) references evaluation_periods_51(period_id),
    foreign key (emp_id) references employees_51(emp_id)
);

-- ==========================================
-- 6. Spatial EES (운영 분석 고도화)
-- ==========================================
-- 프랜차이즈 브랜드 정보 테이블 생성
CREATE TABLE brands_51 (
    brand_id    BIGINT IDENTITY(1,1) PRIMARY KEY,
    brand_name  NVARCHAR(100) NOT NULL,
    category    NVARCHAR(50),
    created_at  DATETIME DEFAULT GETDATE()
);

-- 지점 정보 테이블 생성 (공간 인덱스, 운영 상태 추적 및 UNIQUE 제약 조건 포함)
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
    region_type      VARCHAR(20)    DEFAULT 'GENERAL_CITY', -- 실무형 공간 밀도 평가 정책용 (URBAN_CORE, GENERAL_CITY, SUBURBAN)
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
    CONSTRAINT UK_BRANCH UNIQUE (brand_id, branch_name, address), -- 중복 삽입 방지 제약조건
    CONSTRAINT CK_BRANCH_REGION_TYPE CHECK (region_type IN ('URBAN_CORE', 'GENERAL_CITY', 'SUBURBAN')) -- 허용된 값 검증
);

-- 반경 검색 등 공간 쿼리의 성능 최적화를 위한 Spatial Index 생성
CREATE SPATIAL INDEX IDX_BRANCH_LOCATION ON branches_51(location);

-- 지점별 성과 테이블 생성
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

-- 지점-관리자 연결 브릿지 테이블 생성 (기존 employees_51 테이블 수정 없이 기능 확장)
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
    CONSTRAINT FK_mgr_employee FOREIGN KEY (emp_id) REFERENCES employees_51(emp_id)
);