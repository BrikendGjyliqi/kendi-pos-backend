package com.kendi.pos.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class AnthropicClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.model}")
    private String model;

    @Value("${anthropic.api.max-tokens:4096}")
    private int maxTokens;

    public AnthropicClient() {
        this.http = RestClient.builder().build();
    }

    /**
     * Test i thjeshte — dergon nje prompt te vogel dhe kthen tekstin e pergjigjes.
     */
    public String simpleChat(String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "user", "content", userMessage)
                )
        );

        return call(body);
    }

    /**
     * Vision + text — dergon nje imazh (base64) me nje prompt.
     * mediaType: "image/png", "image/jpeg", "image/webp", "image/gif"
     * ose per PDF: "application/pdf"
     */
    public String visionChat(String prompt, String base64Data, String mediaType) {
        // Per PDF perdorim document type, per imazhe image type
        String contentType = mediaType.equals("application/pdf") ? "document" : "image";

        Map<String, Object> fileBlock = Map.of(
                "type", contentType,
                "source", Map.of(
                        "type", "base64",
                        "media_type", mediaType,
                        "data", base64Data
                )
        );

        Map<String, Object> textBlock = Map.of(
                "type", "text",
                "text", prompt
        );

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(fileBlock, textBlock)
                        )
                )
        );

        return call(body);
    }

    private String call(Map<String, Object> body) {
        try {
            String responseJson = http.post()
                    .uri(API_URL)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(responseJson);
            JsonNode content = root.path("content");
            if (content.isArray() && content.size() > 0) {
                // Merr text-in nga pjesa e pare qe osht text
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText())) {
                        return block.path("text").asText();
                    }
                }
            }
            return "";
        } catch (Exception e) {
            throw new RuntimeException("Anthropic API call failed: " + e.getMessage(), e);
        }
    }
}