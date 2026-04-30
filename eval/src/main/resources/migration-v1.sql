-- 면담 기록 관리 시스템 고도화 (4개 문항 분리) 마이그레이션 스크립트
-- 실행 대상: interviews_51 테이블

-- 1. 새로운 4개 컬럼 추가
ALTER TABLE interviews_51 ADD content1 NVARCHAR(MAX);
ALTER TABLE interviews_51 ADD content2 NVARCHAR(MAX);
ALTER TABLE interviews_51 ADD content3 NVARCHAR(MAX);
ALTER TABLE interviews_51 ADD content4 NVARCHAR(MAX);

-- 2. 기존 데이터가 있다면 content1으로 이동 (선택사항)
-- UPDATE interviews_51 SET content1 = content;

-- 3. 기존 단일 content 컬럼 삭제 (필요 시)
-- ALTER TABLE interviews_51 DROP COLUMN content;
