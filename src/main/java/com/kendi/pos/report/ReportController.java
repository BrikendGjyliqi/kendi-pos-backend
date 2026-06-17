package com.kendi.pos.report;

import com.kendi.pos.order.Order;
import com.kendi.pos.order.OrderItem;
import com.kendi.pos.order.OrderRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final OrderRepository orderRepo;

    public ReportController(OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    // GET Z-Report ditor
    @GetMapping("/z-report")
    public Map<String, Object> zReport(@RequestParam(required = false) String date) {
        LocalDate target = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<Order> dayOrders = getPaidOrdersForDate(target);

        int totalRevenue = dayOrders.stream().mapToInt(Order::getTotal).sum();
        int cashTotal = dayOrders.stream()
                .filter(o -> "cash".equals(o.getPaymentMethod()))
                .mapToInt(Order::getTotal).sum();
        int cardTotal = dayOrders.stream()
                .filter(o -> "card".equals(o.getPaymentMethod()))
                .mapToInt(Order::getTotal).sum();
        int orderCount = dayOrders.size();
        int avgOrder = orderCount > 0 ? totalRevenue / orderCount : 0;

        // Top produktet
        Map<String, ProductStats> productMap = new HashMap<>();
        for (Order o : dayOrders) {
            for (OrderItem item : o.getItems()) {
                ProductStats ps = productMap.computeIfAbsent(item.getProductId(),
                        k -> new ProductStats(item.getName()));
                ps.qty += item.getQuantity();
                ps.revenue += item.getPrice() * item.getQuantity();
            }
        }
        List<ProductStats> topProducts = productMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.revenue, a.revenue))
                .limit(10)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("date", target.toString());
        result.put("totalRevenue", totalRevenue);
        result.put("cashTotal", cashTotal);
        result.put("cardTotal", cardTotal);
        result.put("orderCount", orderCount);
        result.put("avgOrder", avgOrder);
        result.put("topProducts", topProducts);
        result.put("orders", dayOrders);
        return result;
    }

    // GET raport per staff
    @GetMapping("/staff")
    public Map<String, Object> staffReport(
            @RequestParam String staffId,
            @RequestParam(required = false) String date
    ) {
        LocalDate target = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<Order> all = getPaidOrdersForDate(target);
        List<Order> staffOrders = all.stream()
                .filter(o -> staffId.equals(o.getStaffId()))
                .toList();

        int totalRevenue = staffOrders.stream().mapToInt(Order::getTotal).sum();
        int cashTotal = staffOrders.stream()
                .filter(o -> "cash".equals(o.getPaymentMethod()))
                .mapToInt(Order::getTotal).sum();
        int cardTotal = staffOrders.stream()
                .filter(o -> "card".equals(o.getPaymentMethod()))
                .mapToInt(Order::getTotal).sum();

        Map<String, ProductStats> productMap = new HashMap<>();
        for (Order o : staffOrders) {
            for (OrderItem item : o.getItems()) {
                ProductStats ps = productMap.computeIfAbsent(item.getProductId(),
                        k -> new ProductStats(item.getName()));
                ps.qty += item.getQuantity();
                ps.revenue += item.getPrice() * item.getQuantity();
            }
        }
        List<ProductStats> products = productMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.revenue, a.revenue))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("date", target.toString());
        result.put("staffId", staffId);
        result.put("totalRevenue", totalRevenue);
        result.put("cashTotal", cashTotal);
        result.put("cardTotal", cardTotal);
        result.put("orderCount", staffOrders.size());
        result.put("products", products);
        result.put("orders", staffOrders);
        return result;
    }

    // GET historia e plote (paid + cancelled)
    @GetMapping("/history")
    public List<Order> history() {
        return orderRepo.findAll().stream()
                .filter(o -> "paid".equals(o.getStatus()) || "cancelled".equals(o.getStatus()))
                .sorted((a, b) -> Long.compare(
                        b.getPaidAt() != null ? b.getPaidAt() : (b.getClosedAt() != null ? b.getClosedAt() : 0),
                        a.getPaidAt() != null ? a.getPaidAt() : (a.getClosedAt() != null ? a.getClosedAt() : 0)
                ))
                .toList();
    }

    private List<Order> getPaidOrdersForDate(LocalDate date) {
        long startMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMs = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        return orderRepo.findByStatus("paid").stream()
                .filter(o -> {
                    Long ts = o.getPaidAt() != null ? o.getPaidAt() : o.getClosedAt();
                    return ts != null && ts >= startMs && ts < endMs;
                })
                .toList();
    }

    // Helper inner class
    public static class ProductStats {
        public String name;
        public int qty = 0;
        public int revenue = 0;

        public ProductStats(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public int getQty() { return qty; }
        public int getRevenue() { return revenue; }
    }
}