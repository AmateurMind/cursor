package com.example.expenses.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @PostMapping("/suggest-category")
    public ResponseEntity<Map<String, String>> suggestCategory(@RequestBody Map<String, Object> expense) {
        String title = asString(expense.get("title"));
        String notes = asString(expense.get("notes"));

        // Hard rules requested by user: prioritize these over AI
        String hard = specialCategoryFromTitle(title);
        if (StringUtils.hasText(hard)) {
            Map<String, String> body = new HashMap<>();
            body.put("category", hard);
            body.put("source", "rule-based");
            return ResponseEntity.ok(body);
        }

        String fallback = ruleBasedGuess(title + " " + notes);

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (!StringUtils.hasText(apiKey)) {
            String normalized = normalizeCategory(fallback, title + " " + notes);
            Map<String, String> body = new HashMap<>();
            body.put("category", normalized);
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
                String normalized = normalizeCategory(category, title + " " + notes);
                Map<String, String> body = new HashMap<>();
                body.put("category", normalized);
                body.put("source", "gemini");
                return ResponseEntity.ok(body);
            }
        } catch (HttpTimeoutException e) {
            // fall through to fallback
        } catch (Exception e) {
            // fall through to fallback
        }

        String normalized = normalizeCategory(fallback, title + " " + notes);
        Map<String, String> body = new HashMap<>();
        body.put("category", normalized);
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
        try {
            JsonNode root = MAPPER.readTree(json);
            // Preferred path per Gemini schema
            JsonNode node = root.at("/candidates/0/content/parts/0/text");
            if (node != null && node.isTextual() && StringUtils.hasText(node.asText())) {
                return sanitize(node.asText());
            }
            // Fallback: find first "text" field anywhere
            String found = findFirstText(root);
            return sanitize(found);
        } catch (Exception e) {
            return "";
        }
    }

    private static String findFirstText(JsonNode node) {
        if (node == null) return "";
        if (node.has("text") && node.get("text").isTextual()) {
            return node.get("text").asText();
        }
        // search arrays/objects recursively
        for (JsonNode child : node) {
            String v = findFirstText(child);
            if (StringUtils.hasText(v)) return v;
        }
        return "";
    }

    private static String sanitize(String s) {
        if (!StringUtils.hasText(s)) return "";
        String v = s.replace("\r", " ").replace("\n", " ").trim();
        // keep it concise: first 4 words maximum
        String[] parts = v.split("\\s+");
        if (parts.length <= 4) return v;
        return String.join(" ", parts[0], parts[1], parts[2], parts[3]);
    }

    private static String ruleBasedGuess(String text) {
        String t = (text == null ? "" : text).toLowerCase();
        // expanded heuristics to reduce "Misc"
        if (t.contains("banana") || t.contains("watermelon") || t.contains("apple") || t.contains("mango") || t.contains("orange") || t.contains("grape") || t.contains("fruit") || t.contains("milk") || t.contains("bread")) return "grocery";
        if (t.contains("chocolate") || t.contains("candy") || t.contains("snack") || t.contains("dessert")) return "treat";
        if (t.contains("metro") || t.contains("bus") || t.contains("train") || t.contains("subway") || t.contains("ticket") || t.contains("fare") || t.contains("uber") || t.contains("lyft") || t.contains("taxi")) return "transit";

        if (t.contains("rent") || t.contains("lease")) return "rent";
        if (t.contains("flight") || t.contains("air") || t.contains("hotel") || t.contains("travel")) return "travel";
        if (t.contains("grocery") || t.contains("supermarket") || t.contains("food") || t.contains("restaurant")) return "grocery";
        if (t.contains("electric") || t.contains("water") || t.contains("gas") || t.contains("utility") || t.contains("internet")) return "utilities";
        if (t.contains("medical") || t.contains("doctor") || t.contains("pharmacy")) return "health";
        if (t.contains("movie") || t.contains("netflix") || t.contains("entertainment")) return "entertainment";
        if (t.contains("amazon") || t.contains("shopping") || t.contains("store")) return "shopping";
        if (t.contains("education") || t.contains("tuition") || t.contains("course")) return "education";
        return "misc";
    }

    private static String normalizeCategory(String category, String text) {
        String c = (category == null ? "" : category).trim().toLowerCase();
        String t = (text == null ? "" : text).toLowerCase();
        // text-driven normalization first (handles vague AI outputs)
        if (t.contains("banana") || t.contains("watermelon") || t.contains("apple") || t.contains("mango") || t.contains("orange") || t.contains("grape") || t.contains("fruit") || t.contains("milk") || t.contains("bread")) c = "grocery";
        if (t.contains("chocolate") || t.contains("candy") || t.contains("snack") || t.contains("dessert")) c = "treat";
        if (t.contains("metro") || t.contains("bus") || t.contains("train") || t.contains("subway") || t.contains("ticket") || t.contains("fare") || t.contains("uber") || t.contains("lyft") || t.contains("taxi")) c = "transit";

        // category synonym normalization
        if (c.matches(".*\\btransport(ation)?\\b.*")) c = "transit";
        if (c.equals("food") || c.equals("groceries") || c.equals("supermarket")) c = "grocery";
        if (c.equals("healthcare")) c = "health";

        if (!StringUtils.hasText(c)) return "misc";
        return c;
    }

    private static String specialCategoryFromTitle(String title) {
        String t = (title == null ? "" : title).toLowerCase();
        if (t.contains("banana")) return "grocery";
        if (t.contains("chocolate")) return "treat";
        if (t.contains("metro") || t.contains("bus")) return "transit";
        return null;
    }
}
