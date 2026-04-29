# [최종] API 감사 로그(Audit Log) 시스템 구축 계획서

## 1. 개요
사원 평가 시스템의 데이터 무결성과 추적성을 보장하기 위해, 주요 데이터 변경 작업의 전/후 상태를 기록하는 감사 로그 시스템을 구축합니다.

## 2. 핵심 아키텍처: Read-Before-Write
- **방식**: 데이터를 수정(UPDATE)하기 직전에 기존 데이터를 `SELECT`하여 백업.
- **보관**: `ThreadLocal` 기반의 `AuditContextHolder`를 사용하여 AOP까지 데이터를 전달.
- **로깅**: AOP에서 기존 데이터(Before)와 요청 데이터(After)를 조합하여 비동기로 저장.

## 3. 데이터베이스 설계 (api_audit_log)
- `emp_id`: 요청자 사번
- `action_name`: 작업 명칭 (어노테이션 기반)
- `api_url`, `http_method`, `client_ip`: 접속 정보 (X-Forwarded-For 대응)
- `previous_payload`: **변경 전 데이터 (JSON)**
- `request_payload`: **변경 요청 데이터 (JSON)**
- `response_status`: 성공/실패 여부
- `created_at`: 생성 일시

## 4. 상세 보완 사항 (Pro-Level)
1. **정확한 IP 추적**: Azure 프록시 환경을 고려하여 `X-Forwarded-For` 헤더 체크.
2. **트랜잭션 분리**: 로그 저장은 메인 비즈니스 로직의 성공/실패와 별개로 처리 (`REQUIRES_NEW`).
3. **보안 마스킹**: 비밀번호 등 민감 정보는 JSON 저장 시 자동 마스킹 처리.
4. **비동기 처리**: Java 21 가상 스레드를 활용하여 API 응답 속도에 영향 제로.

## 5. 구현 단계
1. `pom.xml` AOP 의존성 추가.
2. `api_audit_log` 테이블 생성.
3. `AuditLog` 어노테이션 및 `AuditContextHolder` 구현.
4. `AuditLogAspect` (AOP) 및 비동기 `AuditLogService` 구현.
5. 주요 서비스(평가 제출 등)에 적용 및 검증.
