package com.kendi.pos.ai;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiTestController {

    private final AnthropicClient anthropic;

    public AiTestController(AnthropicClient anthropic) {
        this.anthropic = anthropic;
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        try {
            String response = anthropic.simpleChat(
                    "Ma thuaj ne shqip: 'Pershendetje, jam Claude. Sistemi Kendi POS osht i lidhur me mua!' " +
                            "Vetem kete pergjigje, asnje shpjegim shtese."
            );
            return Map.of(
                    "success", true,
                    "response", response
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }
}