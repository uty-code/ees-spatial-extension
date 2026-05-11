package com.ees.eval.service;

import com.ees.eval.dto.EvaluationPeriodDTO;
import com.ees.eval.dto.EvaluationResultDTO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 평가 결과 분석 리포트 생성을 담당하는 서비스 인터페이스입니다.
 */
public interface EvaluationReportService {

    /**
     * 전체 평가 결과를 포함하는 프리미엄 엑셀 리포트를 생성하여 응답 스트림에 씁니다.
     *
     * @param period    평가 차수
     * @param results   평가 결과 목록
     * @param response  HttpServletResponse
     * @throws IOException IO 예외
     */
    void generatePremiumReport(EvaluationPeriodDTO period, List<EvaluationResultDTO> results, HttpServletResponse response) throws IOException;
}
