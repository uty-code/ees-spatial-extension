
package com.ees.eval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("prod")
public class PerformanceTestManager {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void generateMassData() {
        System.out.println(">>> Generating Mass Data (approx 5,000 employees, 20,000 mappings)...");
        
        // 1. 사원 데이터 증폭 (기존 데이터를 복제하여 사번만 다르게 생성)
        jdbcTemplate.execute("INSERT INTO employees_51 (emp_id, dept_id, position_id, password, name, email, hire_date, status_code) " +
                           "SELECT (t1.n * 1000 + t2.n + 2000), 1, 1, 'pass', 'Name' + CAST(t1.n * 1000 + t2.n AS VARCHAR), " +
                           "'test' + CAST(t1.n * 1000 + t2.n AS VARCHAR) + '@test.com', '2024-01-01', 'EMPLOYED' " +
                           "FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) t1 " +
                           "CROSS JOIN (SELECT n FROM (VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9)) v(n)) t2");
        
        // 2. 매핑 데이터 증폭
        jdbcTemplate.execute("INSERT INTO evaluator_mappings_51 (period_id, evaluatee_id, evaluator_id, relation_type_code, is_deleted) " +
                           "SELECT 1, (t1.n + 2000), 1001, 'PEER', 'n' " +
                           "FROM (SELECT TOP 5000 ROW_NUMBER() OVER(ORDER BY (SELECT NULL)) n FROM sys.objects s1 CROSS JOIN sys.objects s2) t1");
                           
        System.out.println(">>> Mass Data Generation Complete!");
    }

    @Test
    public void applyIndexes() {
        System.out.println(">>> Applying Optimization Indexes...");
        try {
            jdbcTemplate.execute("CREATE INDEX idx_eval_mapping_period ON dbo.evaluator_mappings_51 (period_id, is_deleted)");
            jdbcTemplate.execute("CREATE INDEX idx_eval_mapping_evaluatee ON dbo.evaluator_mappings_51 (evaluatee_id)");
            jdbcTemplate.execute("CREATE INDEX idx_eval_mapping_evaluator ON dbo.evaluator_mappings_51 (evaluator_id)");
            jdbcTemplate.execute("CREATE INDEX idx_employee_dept_pos ON dbo.employees_51 (dept_id, position_id)");
            jdbcTemplate.execute("CREATE INDEX idx_evaluations_mapping ON dbo.evaluations_51 (mapping_id, is_deleted)");
            System.out.println(">>> Indexes Applied Successfully!");
        } catch (Exception e) {
            System.out.println("Note: Some indexes might already exist. Error: " + e.getMessage());
        }
    }
}
