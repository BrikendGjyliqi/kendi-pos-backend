package com.kendi.pos.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kendi.pos.product.Product;
import com.kendi.pos.product.ProductRepository;
import com.kendi.pos.supplier.Supplier;
import com.kendi.pos.supplier.SupplierRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/ai/invoice")
@CrossOrigin(origins = "*")
public class InvoiceScanController {

    private final AnthropicClient anthropic;
    private final ProductRepository productRepo;
    private final SupplierRepository supplierRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public InvoiceScanController(
            AnthropicClient anthropic,
            ProductRepository productRepo,
            SupplierRepository supplierRepo
    ) {
        this.anthropic = anthropic;
        this.productRepo = productRepo;
        this.supplierRepo = supplierRepo;
    }

    /**
     * POST /api/ai/invoice/scan
     * Pranon file (PDF ose imazh) dhe kthen delivery-n e ekstraktuar + product matches.
     */
    @PostMapping("/scan")
    public Map<String, Object> scan(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Konverto file ne base64
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String contentType = file.getContentType();
            if (contentType == null) contentType = "application/pdf";

            // 2. Merr listen e produkteve qe kane trackStock — per matching
            List<Product> stockProducts = productRepo.findAll().stream()
                    .filter(Product::isTrackStock)
                    .toList();

            // 3. Merr listen e furnitoreve
            List<Supplier> suppliers = supplierRepo.findAll();

            // 4. Ndertoje prompt-in
            String prompt = buildPrompt(stockProducts, suppliers);

            // 5. Therrase Claude Vision
            String aiResponse = anthropic.visionChat(prompt, base64, contentType);

            // 6. Parse JSON-in
            String cleanJson = extractJson(aiResponse);
            JsonNode parsed = mapper.readTree(cleanJson);

            return Map.of(
                    "success", true,
                    "extracted", parsed
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    private String buildPrompt(List<Product> products, List<Supplier> suppliers) {
        StringBuilder productsList = new StringBuilder();
        for (Product p : products) {
            productsList.append(String.format(
                    "  - id: %s | name: %s | unit: %s\n",
                    p.getId(),
                    p.getName(),
                    p.getStockUnit() != null ? p.getStockUnit() : "PIECE"
            ));
        }

        StringBuilder suppliersList = new StringBuilder();
        for (Supplier s : suppliers) {
            suppliersList.append(String.format(
                    "  - id: %s | name: %s\n",
                    s.getId(), s.getName()
            ));
        }

        return """
Je asistent qe lexon flete-dorezimi (invoice) nga furnitoret ne Kosove.
Nxirre keto te dhena nga dokumenti:

1. Emri i furnitorit dhe date-n e dorezimit
2. Numrin e dokumentit (nese ka)
3. Produktet e derguara me sasine, njesin, dhe qmimin per njesi

Ke dy lista referuese:

**Furnitoret ne sistem:**
%s

**Produktet qe gjurmohen ne stok:**
%s

Per çdo produkt ne fleten e dorezimit:
- Provo te gjesh match ne listen e produkteve me lart (fuzzy matching — psh "Coca Cola 0.33L" match-on me "Coca Cola 0.33l")
- Nese gjen match, kthej id-ne e produktit ne DB
- Nese nuk gjen match, kthej productId=null (menaxheri do t'a zgjedhe manualisht)

Per furnitorin:
- Provo te gjesh match ne listen me lart me emrin
- Nese gjen, kthej supplierId

Kthej PA COMMENTARY, VETEM JSON te paster me kete strukture (pa markdown, pa ```):

{
  "supplierId": "id ose null",
  "supplierName": "emri qe u lexua nga dokumenti",
  "deliveryDate": "YYYY-MM-DD",
  "documentRef": "numri i dokumentit ose null",
  "items": [
    {
      "productId": "id nga DB ose null nese s'ka match",
      "extractedName": "emri qe u lexua nga dokumenti",
      "quantity": 48.0,
      "unit": "PIECE ose KG",
      "unitPriceEur": 0.65,
      "matchConfidence": "high|medium|low|none"
    }
  ]
}

Cmimet ne EUR. Sasia si numer decimal. Kthej vetem JSON, asgje tjeter.
""".formatted(suppliersList.toString(), productsList.toString());
    }

    /**
     * Ndonjehere AI e kthen JSON-in ne markdown block ```json ... ```
     * Kete e pastrojme.
     */
    private String extractJson(String text) {
        text = text.trim();
        // Hiq ```json
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        // Hiq ``` fund
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}