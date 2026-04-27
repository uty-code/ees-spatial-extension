package com.ees.eval;

import com.ees.eval.dto.DepartmentDTO;
import com.ees.eval.dto.EvaluationElementDTO;
import com.ees.eval.dto.EvaluationTypeWeightDTO;
import com.ees.eval.service.DepartmentService;
import com.ees.eval.service.EvaluationElementService;
import com.ees.eval.service.EvaluationTypeWeightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DebugRunner implements CommandLineRunner {

    @Autowired
    private EvaluationTypeWeightService typeWeightService;
    
    @Autowired
    private DepartmentService departmentService;
    
    @Autowired
    private EvaluationElementService elementService;

    @Override
    public void run(String... args) throws Exception {
        Long periodId = 2L; // 테스트1 차수
        System.out.println("=== Debug Validation Logic ===");
        
        List<DepartmentDTO> allDepts = departmentService.getSimpleAllDepartments();
        for (DepartmentDTO dept : allDepts) {
            boolean isStaffValid = typeWeightService.isWeightSumValid(periodId, dept.deptId(), "STAFF");
            boolean isLeaderValid = typeWeightService.isWeightSumValid(periodId, dept.deptId(), "LEADER");
            System.out.println("Dept: " + dept.deptName() + " (ID: " + dept.deptId() + ") -> Staff Valid: " + isStaffValid + ", Leader Valid: " + isLeaderValid);
            
            List<EvaluationElementDTO> elements = elementService.getElementsByPeriodId(periodId, dept.deptId());
            System.out.println("  Explicit Elements Count: " + elements.size());
            for (EvaluationElementDTO el : elements) {
                System.out.println("    " + el.elementTypeCode() + " / " + el.elementName() + " = " + el.weight());
            }
        }
        System.out.println("==============================");
    }
}
