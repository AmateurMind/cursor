package com.example.expenses.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    @PostMapping("/suggest-category")
    public ResponseEntity<Map<String, String>> suggestCategory(@RequestBody Map<String, Object> expense) {
        String title = asString(expense.get("title"));
        String notes = asString(expense.get("notes"));
        String fallback = ruleBasedGuess(title + " " + notes);

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (!StringUtils.hasText(apiKey)) {
            Map<String, String> body = new HashMap<>();
            body.put("category", fallback);
            body.put("source", "rule-based");
            return ResponseEntity.ok(body);
        }

        try {
            String model = System.getenv().getOrDefault("GEMINI_MODEL", "gemini-2.0-flash");
            String prompt = "Suggest a concise expense category (single or two words) for: '" +
                    safe(title) + "' notes: '" + safe(notes) + "'. Only return the category text.";

            String payload = "{\n" +
                    "  \"contents\": [\n" +
                    "    {\n" +
                    "      \"parts\": [\n" +
                    "        {\"text\": " + jsonString(prompt) + "}\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"generationConfig\": {\n" +
                    "    \"temperature\": 0.2,\n" +
                    "    \"maxOutputTokens\": 50\n" +
                    "  }\n" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String category = extractGeminiText(response.body());
                if (!StringUtils.hasText(category)) {
                    category = fallback;
                }
                Map<String, String> body = new HashMap<>();
                body.put("category", category.trim());
                body.put("source", "gemini");
                return ResponseEntity.ok(body);
            }
        } catch (HttpTimeoutException e) {
            // fall through to fallback
        } catch (Exception e) {
            // fall through to fallback
        }

        Map<String, String> body = new HashMap<>();
        body.put("category", fallback);
        body.put("source", "rule-based");
        return ResponseEntity.ok(body);
    }

    private static String asString(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").trim();
    }

    private static String jsonString(String s) {
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    private static String extractGeminiText(String json) {
        // extremely small, naive extraction to avoid adding JSON libs
        // looks for "text":"..." in Gemini response
        try {
            int idx = json.indexOf("\"text\":");
            if (idx == -1) return "";
            int start = json.indexOf('"', idx + 7);
            if (start == -1) return "";
            int end = json.indexOf('"', start + 1);
            if (end == -1) return "";
            String content = json.substring(start + 1, end);
            return content.replace("\\n", " ").trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String ruleBasedGuess(String text) {
        String t = (text == null ? "" : text).toLowerCase();
        if (t.contains("rent") || t.contains("lease")) return "Rent";
        if (t.contains("uber") || t.contains("lyft") || t.contains("taxi")) return "Transport";
        if (t.contains("flight") || t.contains("air") || t.contains("hotel") || t.contains("travel")) return "Travel";
        if (t.contains("grocery") || t.contains("supermarket") || t.contains("food") || t.contains("restaurant")) return "Food";
        if (t.contains("electric") || t.contains("water") || t.contains("gas") || t.contains("utility") || t.contains("internet")) return "Utilities";
        if (t.contains("medical") || t.contains("doctor") || t.contains("pharmacy")) return "Health";
        if (t.contains("movie") || t.contains("netflix") || t.contains("entertainment")) return "Entertainment";
        if (t.contains("amazon") || t.contains("shopping") || t.contains("store")) return "Shopping";
        if (t.contains("education") || t.contains("tuition") || t.contains("course")) return "Education";
        return "Misc";
    }
}



