package com.kendi.pos.ai;

import java.util.List;
import java.util.Map;

public class AIAnalyticsDtos {

    public record QuestionRequest(String question) {}

    public record AnalyticsResponse(
            String answer,
            String sql,
            List<Map<String, Object>> data,
            String chartType,
            boolean success,
            String error
    ) {
        public static AnalyticsResponse success(String answer, String sql, List<Map<String, Object>> data, String chartType) {
            return new AnalyticsResponse(answer, sql, data, chartType, true, null);
        }

        public static AnalyticsResponse failure(String error) {
            return new AnalyticsResponse(null, null, null, null, false, error);
        }
    }
}