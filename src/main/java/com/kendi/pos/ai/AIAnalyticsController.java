package com.kendi.pos.ai;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/analytics")
@CrossOrigin(origins = "*")
public class AIAnalyticsController {

    private final AIAnalyticsService service;

    public AIAnalyticsController(AIAnalyticsService service) {
        this.service = service;
    }

    @PostMapping
    public AIAnalyticsDtos.AnalyticsResponse ask(@RequestBody AIAnalyticsDtos.QuestionRequest req) {
        if (req.question() == null || req.question().isBlank()) {
            return AIAnalyticsDtos.AnalyticsResponse.failure("Pyetja eshte bosh");
        }
        return service.answer(req.question());
    }
}