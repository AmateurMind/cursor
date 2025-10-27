package com.example.expenses.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
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

        // Hard rules first (fast path)
        String hard = specialCategoryFromTitle(title);
        if (StringUtils.hasText(hard)) {
            Map<String, String> body = new HashMap<>();
            body.put("category", hard);
            body.put("source", "rule-based");
            return ResponseEntity.ok(body);
        }

        String fallback = ruleBasedGuess(title + " " + notes);

        String apiKey = System.getenv("OPENAI_API_KEY");
        String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1");
        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");
        if (!StringUtils.hasText(apiKey)) {
            String normalized = normalizeCategory(fallback, title + " " + notes);
            Map<String, String> body = new HashMap<>();
            body.put("category", normalized);
            body.put("source", "rule-based");
            return ResponseEntity.ok(body);
        }

        try {
            // Build OpenAI Chat Completions payload
            String instruction = "You are an expense category classifier. Suggest a concise, lowercase category (1-2 words) for the expense.\n" +
                    "Examples:\n" +
                    "'laptop' -> electronics\n" +
                    "'table' -> furniture\n" +
                    "'rent payment' -> housing\n" +
                    "'apple 1kg' -> grocery\n" +
                    "'chocolate' -> treats\n" +
                    "'bus ticket' -> transit\n" +
                    "Return ONLY the category name in lowercase.";
            String user = "Title: '" + safe(title) + "'\nNotes: '" + safe(notes) + "'";

            String payload = "{\n" +
                    "  \"model\": " + jsonString(model) + ",\n" +
                    "  \"messages\": [\n" +
                    "    {\"role\": \"system\", \"content\": " + jsonString(instruction) + "},\n" +
                    "    {\"role\": \"user\", \"content\": " + jsonString(user) + "}\n" +
                    "  ],\n" +
                    "  \"temperature\": 0,\n" +
                    "  \"max_tokens\": 4\n" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String category = extractOpenAIText(response.body());
                if (!StringUtils.hasText(category)) {
                    category = fallback;
                }
                String normalized = normalizeCategory(category, title + " " + notes);
                Map<String, String> body = new HashMap<>();
                body.put("category", normalized);
                body.put("source", "openai");
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

    private static String extractOpenAIText(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode node = root.at("/choices/0/message/content");
            if (node != null && node.isTextual() && StringUtils.hasText(node.asText())) {
                return sanitize(node.asText());
            }
            return "";
        } catch (Exception e) {
            return "";
        }
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
        // Electronics first, with Apple brand disambiguation
        if (t.contains("laptop") || t.contains("notebook") || t.contains("macbook") ||
                t.contains("iphone") || t.contains("android") || t.contains("smartphone") ||
                t.contains("mobile phone") || t.contains("mobile") || t.contains("tablet") ||
                t.contains("ipad") || t.contains("headphone") || t.contains("earbud") ||
                t.contains("charger") || t.contains("power bank") || t.contains("usb") ||
                t.contains("ssd") || t.contains("hard disk") || t.contains("monitor") ||
                t.contains("keyboard") || t.contains("mouse")) return "electronics";
        if ((t.contains("apple") && (t.contains("store") || t.contains("iphone") || t.contains("mac") || t.contains("ipad")))) return "electronics";

// Groceries and food
        if ((t.contains("banana") || t.contains("watermelon") || (t.contains("apple") && !(t.contains("store") || t.contains("iphone") || t.contains("mac") || t.contains("ipad"))) ||
                t.contains("mango") || t.contains("orange") || t.contains("grape") || t.contains("fruit") || t.contains("milk") || t.contains("bread") || t.contains("vegetable") || t.contains("pumpkin") || t.contains("tomato") || t.contains("potato") || t.contains("onion") || t.contains("carrot") || t.contains("grocery"))) return "grocery";

        // Treats / snacks
        if (t.contains("chocolate") || t.contains("candy") || t.contains("snack") || t.contains("dessert") || t.contains("ice cream") || t.contains("ice-cream")) return "treat";

        // Transit / transport
        if (t.contains("metro") || t.contains("bus") || t.contains("train") || t.contains("subway") || t.contains("ticket") || t.contains("fare") || t.contains("uber") || t.contains("lyft") || t.contains("taxi")) return "transit";

        if (t.contains("rent") || t.contains("lease")) return "rent";
        if (t.contains("flight") || t.contains("air") || t.contains("hotel") || t.contains("travel")) return "travel";
        if (t.contains("supermarket") || t.contains("restaurant") || t.contains("food")) return "grocery";
        if (t.contains("electric") || t.contains("electricity") || t.contains("water bill") || t.contains("gas") || t.contains("utility") || t.contains("internet") || t.contains("wifi")) return "utilities";
        if (t.contains("medical") || t.contains("doctor") || t.contains("pharmacy") || t.contains("hospital")) return "health";
        if (t.contains("movie") || t.contains("netflix") || t.contains("entertainment") || t.contains("cinema")) return "entertainment";
        if (t.contains("amazon") || t.contains("shopping") || t.contains("store") || t.contains("mall")) return "shopping";
        if (t.contains("education") || t.contains("tuition") || t.contains("course") || t.contains("school")) return "education";
        return "misc";
    }

    private static String normalizeCategory(String category, String text) {
        String c = (category == null ? "" : category).trim().toLowerCase();
        // Basic synonym normalization only
        if (c.equals("food") || c.equals("groceries")) c = "grocery";
        if (c.equals("transportation")) c = "transit";
        if (c.equals("healthcare") || c.equals("medicine")) c = "health";
        if (!StringUtils.hasText(c)) return "misc";
        return c;
    }

    private static String specialCategoryFromTitle(String title) {
        String t = (title == null ? "" : title).toLowerCase();
        // Common items per request
        if (t.contains("laptop") || t.contains("mobile") || t.contains("phone") || t.contains("iphone") || t.contains("macbook")) return "electronics";
if (t.contains("banana") || t.contains("milk") || t.contains("bread") || t.contains("pumpkin") || t.contains("tomato") || t.contains("potato") || t.contains("onion") || t.contains("carrot")) return "grocery";
        if (t.contains("chocolate")) return "treat";
        if (t.contains("metro") || t.contains("bus")) return "transit";
        return null;
    }
}
