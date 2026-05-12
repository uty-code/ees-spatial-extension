-- 1. 하위 로그 및 증빙 데이터 삭제
DELETE FROM login_logs_51;
DELETE FROM evidences_51;
DELETE FROM interviews_51;

-- 2. 평가 및 매핑 데이터 삭제
DELETE FROM final_grades_51;
DELETE FROM evaluations_51;
DELETE FROM evaluator_mappings_51;
DELETE FROM evaluation_elements_51;
DELETE FROM evaluation_periods_51;

-- 3. 지점 관련 공간 데이터 삭제
DELETE FROM branch_managers_51;
DELETE FROM branch_performance_51;
DELETE FROM branches_51;
DELETE FROM brands_51;

-- 4. 사원 및 조직 데이터 삭제 (순서 주의)
UPDATE departments_51 SET leader_id = NULL;
DELETE FROM employee_roles_51;
DELETE FROM employees_51;
DELETE FROM departments_51;
DELETE FROM positions_51;
DELETE FROM roles_51;

-- 5. Identity 초기화
DBCC CHECKIDENT('brands_51', RESEED, 0);
DBCC CHECKIDENT('branches_51', RESEED, 0);
DBCC CHECKIDENT('branch_performance_51', RESEED, 0);
DBCC CHECKIDENT('evaluation_periods_51', RESEED, 0);
DBCC CHECKIDENT('evaluation_elements_51', RESEED, 0);
DBCC CHECKIDENT('evaluator_mappings_51', RESEED, 0);
DBCC CHECKIDENT('evaluations_51', RESEED, 0);
DBCC CHECKIDENT('departments_51', RESEED, 0);
DBCC CHECKIDENT('positions_51', RESEED, 0);
DBCC CHECKIDENT('roles_51', RESEED, 0);

SELECT 'All data truncated safely' AS Result;
