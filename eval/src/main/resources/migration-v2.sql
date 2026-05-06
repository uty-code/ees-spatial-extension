-- 면담 관리 성능 최적화를 위한 인덱스 추가
-- 1. mapping_id 조회 성능 향상 및 Key Lookup 제거를 위한 커버링 인덱스
-- 2. is_deleted = 'n' 조건만 포함하는 필터링된 인덱스 (SARG 최적화)
-- 3. 시스템 표준에 따라 비관적 락 대신 version 컬럼을 이용한 낙관적 락(Optimistic Locking)을 활용함

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'ix_interviews_mapping_id_51' AND object_id = OBJECT_ID('interviews_51'))
BEGIN
    CREATE NONCLUSTERED INDEX ix_interviews_mapping_id_51 
    ON interviews_51 (mapping_id) 
    INCLUDE (status_code, updated_at) 
    WHERE is_deleted = 'n';
    
    PRINT 'Index ix_interviews_mapping_id_51 created successfully.';
END
ELSE
BEGIN
    PRINT 'Index ix_interviews_mapping_id_51 already exists.';
END
