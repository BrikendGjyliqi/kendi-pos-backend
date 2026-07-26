package com.kendi.pos.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AIAnalyticsService {

    private static final String CLAUDE_API = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-5-20250929";

    private static final String SCHEMA = """
            Ke keto tabela ne PostgreSQL per nje sistem POS te nje kafeje ne Kosove:

            TABLE: orders
            - id (VARCHAR) - UUID string
            - table_id (VARCHAR) - UUID string, referenca ne restaurant_tables (por si string)
            - staff_id (VARCHAR) - UUID string, referenca ne staff.id
            - status (VARCHAR) - 'open', 'closed', 'paid', 'cancelled'
            - total (INTEGER) - totali ne cents (pjesetohet me 100 per euros)
            - subtotal (INTEGER) - subtotali ne cents
            - discount (INTEGER) - zbritja ne cents
            - payment_method (VARCHAR) - 'cash' ose 'card'
            - tip_amount (INTEGER) - bakshishi ne cents
            - tip_percent (INTEGER) - perqindja e bakshishit
            - fiscal (BOOLEAN)
            - opened_at (BIGINT) - Unix timestamp ne millisekonda
            - paid_at (BIGINT) - Unix timestamp ne millisekonda
            - closed_at (BIGINT) - Unix timestamp ne millisekonda
            - cash_given (INTEGER)

            TABLE: order_items
            - id (VARCHAR)
            - order_id (VARCHAR) - referenca ne orders.id
            - product_id (VARCHAR) - referenca ne products.id
            - name (VARCHAR) - emri i produktit (snapshot)
            - price (INTEGER) - cmimi ne cents
            - quantity (INTEGER) - sasia
            - added_at (BIGINT)

            TABLE: products
            - id (VARCHAR) - UUID string
            - name (VARCHAR) - emri i produktit
            - category_id (VARCHAR)
            - price (INTEGER) - cmimi ne cents
            - stock_quantity (DOUBLE PRECISION) - stoku aktual
            - low_stock_threshold (DOUBLE PRECISION) - pragu low stock
            - track_stock (BOOLEAN)
            - auto_deduct_on_sale (BOOLEAN)
            - stock_unit (VARCHAR) - 'PIECE' ose 'KG'

            TABLE: categories
            - id (VARCHAR)
            - name (VARCHAR) - emri i kategorise
            - color (VARCHAR)

            TABLE: staff
            - id (VARCHAR) - UUID string
            - name (VARCHAR) - emri i punetorit
            - role (VARCHAR) - 'admin' ose 'cashier'
            - active (BOOLEAN)

            TABLE: restaurant_tables
            - id (VARCHAR) - UUID string (POR gjithashtu ka nje kolone "name" psh 'M1', 'T3', 'O5')
            - name (VARCHAR) - psh 'M1', 'T3', 'O5'
            - seat_count (INTEGER)
            - section (VARCHAR) - 'MAIN_DINING', 'TERRACE', 'OUTDOOR'
            - status (VARCHAR) - 'AVAILABLE', 'ON_DINE', 'RESERVED'

            TABLE: reservations
            - id (BIGINT)
            - table_id (BIGINT) - referenca ne restaurant_tables.id
            - guest_name (VARCHAR)
            - guest_phone (VARCHAR)
            - guest_count (INTEGER)
            - reservation_time (TIMESTAMP)
            - status (VARCHAR) - 'PENDING_REQUEST', 'CONFIRMED', 'ARRIVED', 'NO_SHOW', 'DECLINED', 'CANCELLED'
            - requested_by (VARCHAR)
            - confirmed_at (TIMESTAMP)
            - arrived_at (TIMESTAMP)
            - no_show_at (TIMESTAMP)
            - created_at (TIMESTAMP)
            - updated_at (TIMESTAMP)

            TABLE: deliveries
            - id (BIGINT)
            - supplier_id (BIGINT)
            - delivery_date (DATE)
            - total_amount (INTEGER) - ne cents
            - notes (TEXT)

            TABLE: suppliers
            - id (BIGINT)
            - name (VARCHAR)
            - contact_person (VARCHAR)
            - phone (VARCHAR)

            RREGULLA TE RENDESISHME:
            1. Te gjitha cmimet dhe totalet jane ne CENTS (integer). Pjesetoi me 100 per me marr euros.
            2. Timestamps (opened_at, paid_at, closed_at) jane BIGINT ne millisekonda. Perdor TO_TIMESTAMP(paid_at/1000) per me konvertu.
            3. Data e sotme: CURRENT_DATE. Kete jave: DATE_TRUNC('week', CURRENT_DATE). Kete muaj: DATE_TRUNC('month', CURRENT_DATE).
            4. Per fitim/te ardhura, perdor status='paid' (jo open ose cancelled).
            5. VAJT: table_id te orders eshte VARCHAR (UUID string), jo BIGINT. Perdor drejtperdrejt orders.table_id = restaurant_tables.id (te dyja VARCHAR).
            6. Kur pyetja permend "sot", perdor DATE(TO_TIMESTAMP(paid_at/1000)) = CURRENT_DATE per orders (paguar sot), jo opened_at.
            7. Kur pyetja permend "shitur", perdor status='paid' (porosia u perfundua me sukses).
            8. Per me numru sasi produktesh te shitura, perdor SUM(order_items.quantity) jo COUNT.
            9. Emrat e produkteve nuk jane gjithmone te sakte - psh 'Coca-Cola' mund te ruhet si 'Coca Cola 0.33l' ose 'Coca Cola 0/0.33l'. GJITHMONE perdor ILIKE '%emri%' per matching (psh WHERE name ILIKE '%cola%' NUK WHERE name = 'Coca-Cola'). Kjo osht KRITIKE.
            10. Kur perdoruesi pyet per nje produkt me emer te shkurter (cola, cappuccino, espresso), perdor ILIKE me wildcard.
            11. Emrat e staff mund te jene 'Admin', 'Brikend', 'Endrit', 'Enkel' - perdor ILIKE per te qene fleksibel.
            12. Per pyetje krahasimi (kete dite vs dite tjeter, sot vs dje, kete jave vs javen e kaluar), perdor CTE ose UNION ALL me labels te qarta. Psh:
                SELECT 'Sot' as periudha, COALESCE(SUM(total),0)/100.0 as fitim FROM orders WHERE status='paid' AND DATE(TO_TIMESTAMP(paid_at/1000)) = CURRENT_DATE
                UNION ALL
                SELECT 'Dje', COALESCE(SUM(total),0)/100.0 FROM orders WHERE status='paid' AND DATE(TO_TIMESTAMP(paid_at/1000)) = CURRENT_DATE - INTERVAL '1 day'
            13. Per grafik/chart data, kthe minimum 2 kolone: nje label (VARCHAR emri i dites/produktit/staffit/periudhes) dhe nje vlere numerike. Kolonat me emra te qarte psh 'produkt', 'shitje', 'dite', 'total_euro', 'periudha'.
            14. Kur pergjigja kerkon renditje (top 5, top 10, me shume), perdor ORDER BY DESC LIMIT.
            15. Per grafik trend ne kohe, perdor DATE_TRUNC('day', TO_TIMESTAMP(paid_at/1000)) per me grupu per dite, dhe TO_CHAR(...) per me formatu si string te lexueshem.
            """;

    private final JdbcTemplate jdbc;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient http = RestClient.create();

    public AIAnalyticsService(
            JdbcTemplate jdbc,
            @Value("${anthropic.api.key}") String apiKey
    ) {
        this.jdbc = jdbc;
        this.apiKey = apiKey;
    }

    public AIAnalyticsDtos.AnalyticsResponse answer(String question) {
        try {
            // Hapi 1: LLM gjeneron SQL
            String sql = generateSql(question);

            // Hapi 2: Siguria - vetem SELECT
            if (!isSafe(sql)) {
                return AIAnalyticsDtos.AnalyticsResponse.failure(
                        "Kjo pyetje kerkon operacione qe nuk lejohen"
                );
            }

            // Hapi 3: Ekzekuto SQL
            List<Map<String, Object>> rows = jdbc.queryForList(sql);

            // Hapi 4: Detekto nese data mund t'renderohet si chart
            String chartType = detectChartType(rows, question);

            // Hapi 5: LLM formaton pergjigje
            String answer = formatAnswer(question, sql, rows);

            // Nese ka chart, dergo edhe raw data
            List<Map<String, Object>> chartData = chartType != null ? rows : null;

            return AIAnalyticsDtos.AnalyticsResponse.success(answer, sql, chartData, chartType);

        } catch (Exception e) {
            return AIAnalyticsDtos.AnalyticsResponse.failure(
                    "Nuk mund t'i pergjigjem kesaj pyetjeje. Provo me nje formulim tjeter. (" + e.getMessage() + ")"
            );
        }
    }

    /**
     * Detekton nese rezultati mund te vizualizohet si chart.
     * Chart shfaqet nese ka >= 2 rreshta dhe pergjigja permban krahasim/renditje.
     */
    private String detectChartType(List<Map<String, Object>> rows, String question) {
        if (rows == null || rows.size() < 2) return null;

        String q = question.toLowerCase();

        // Bar chart per krahasime dhe renditje
        boolean isRanking = q.contains("me shume") || q.contains("me shum")
                || q.contains("top") || q.contains("cili")
                || q.contains("cilat") || q.contains("krahaso")
                || q.contains("me te shitura") || q.contains("me e");

        // Line chart per trends ne kohe
        boolean isTrend = q.contains("kete jave") || q.contains("kete muaj")
                || q.contains("cdo dite") || q.contains("dite pas dite")
                || q.contains("trend") || q.contains("evolimi");

        // Kontrollo nese ka kolone numerike ne rezultat
        Map<String, Object> firstRow = rows.get(0);
        boolean hasNumeric = firstRow.values().stream().anyMatch(v -> v instanceof Number);

        if (!hasNumeric) return null;

        if (isTrend) return "line";
        if (isRanking) return "bar";

        // Nese ka >=3 rreshta dhe kolone numerike, default bar
        if (rows.size() >= 3) return "bar";

        return null;
    }

    private String generateSql(String question) throws Exception {
        String prompt = SCHEMA + "\n\nPerdoruesi pyet: \"" + question + "\"\n\n" +
                "Gjenero VETEM SQL query per PostgreSQL. Mos shto shpjegime, mos shto ```sql```, vetem SQL.";

        String response = callClaude(prompt);

        // Pastro nese ka backticks
        response = response.replaceAll("```sql", "").replaceAll("```", "").trim();

        return response;
    }

    private String formatAnswer(String question, String sql, List<Map<String, Object>> rows) throws Exception {
        String prompt = "Perdoruesi pyeti: \"" + question + "\"\n\n" +
                "Query u ekzekutua dhe kthene keto rezultate:\n" +
                mapper.writeValueAsString(rows) + "\n\n" +
                "KUJDES: Vlerat ne kolonat total, price, subtotal, tip_amount jane ne CENTS. " +
                "Ne pergjigje, pjesetoi me 100 dhe shto € para (psh 2400 → €24.00). " +
                "Vlerat qe tashme jane pjesetuar (psh total_euro, fitim) mos i pjesetho me.\n\n" +
                "Formato pergjigje ne SHQIP natyror per pronarin e kafes. " +
                "Ji i shkurter dhe konkret. Perdor emoji nese ndihmon. " +
                "Nese lista ka disa elemente, perdor bullet points. " +
                "Nese ka krahasim, thuaj qarte se ka rritje/renie dhe perqindjen.";

        return callClaude(prompt);
    }

    private String callClaude(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", 1024,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        String responseJson = http.post()
                .uri(CLAUDE_API)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode node = mapper.readTree(responseJson);
        return node.get("content").get(0).get("text").asText();
    }

    private boolean isSafe(String sql) {
        String upper = sql.toUpperCase().trim();
        // Duhet me fillu me SELECT ose WITH
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            return false;
        }
        // Fjalet e ndaluara
        String[] forbidden = {"DELETE", "DROP", "INSERT", "UPDATE", "ALTER", "TRUNCATE", "GRANT", "REVOKE"};
        for (String word : forbidden) {
            if (upper.contains(word)) {
                return false;
            }
        }
        return true;
    }
}