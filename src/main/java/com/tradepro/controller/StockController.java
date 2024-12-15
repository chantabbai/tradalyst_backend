package com.tradepro.controller;

import com.tradepro.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private static final Logger logger = LoggerFactory.getLogger(StockController.class);

    @Autowired
    private StockService stockService;

    @GetMapping("/quote/{symbol}")
    public ResponseEntity<?> getQuote(@PathVariable String symbol) {
        try {
            logger.info("Fetching quote for symbol: {}", symbol);
            Map<String, Object> quote = stockService.getQuote(symbol);
            return ResponseEntity.ok(quote);
        } catch (Exception e) {
            logger.error("Error fetching quote: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching quote: " + e.getMessage()));
        }
    }

    @GetMapping("/profile/{symbol}")
    public ResponseEntity<?> getProfile(@PathVariable String symbol) {
        try {
            logger.info("Fetching profile for symbol: {}", symbol);
            Map<String, Object> profile = stockService.getProfile(symbol);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Error fetching profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching profile: " + e.getMessage()));
        }
    }

    @GetMapping("/historical-price-full/{symbol}")
    public ResponseEntity<?> getHistoricalPrices(@PathVariable String symbol) {
        try {
            logger.info("Fetching historical prices for symbol: {}", symbol);
            Map<String, Object> history = stockService.getHistoricalPrices(symbol);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error fetching historical prices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching historical prices: " + e.getMessage()));
        }
    }

    @GetMapping("/financials/{symbol}")
    public ResponseEntity<?> getFinancialStatements(@PathVariable String symbol) {
        try {
            logger.info("Fetching financial statements for symbol: {}", symbol);
            Map<String, Object> financials = stockService.getFinancialStatements(symbol);
            
            if (financials == null || financials.isEmpty()) {
                logger.warn("No financial data found for symbol: {}", symbol);
                return ResponseEntity.ok(Collections.emptyMap());
            }
            
            logger.info("Successfully retrieved financial statements for {}", symbol);
            logger.debug("Financial statements response: {}", financials);
            return ResponseEntity.ok(financials);
        } catch (Exception e) {
            logger.error("Error fetching financial statements for {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Error fetching financial statements: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis(),
                    "symbol", symbol
                ));
        }
    }

    @GetMapping("/key-metrics/{symbol}")
    public ResponseEntity<?> getKeyMetrics(@PathVariable String symbol) {
        try {
            logger.info("Fetching key metrics for symbol: {}", symbol);
            Map<String, Object> metrics = stockService.getKeyMetrics(symbol);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error fetching key metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching key metrics: " + e.getMessage()));
        }
    }

    @GetMapping("/ratios/{symbol}")
    public ResponseEntity<?> getRatios(@PathVariable String symbol) {
        try {
            logger.info("Fetching ratios for symbol: {}", symbol);
            Map<String, Object> ratios = stockService.getRatios(symbol);
            return ResponseEntity.ok(ratios);
        } catch (Exception e) {
            logger.error("Error fetching ratios: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching ratios: " + e.getMessage()));
        }
    }

    @GetMapping("/enterprise-value/{symbol}")
    public ResponseEntity<?> getEnterpriseValue(@PathVariable String symbol) {
        try {
            logger.info("Fetching enterprise value for symbol: {}", symbol);
            Map<String, Object> value = stockService.getEnterpriseValue(symbol);
            return ResponseEntity.ok(value);
        } catch (Exception e) {
            logger.error("Error fetching enterprise value: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching enterprise value: " + e.getMessage()));
        }
    }

    @GetMapping("/growth/{symbol}")
    public ResponseEntity<?> getCompanyGrowth(@PathVariable String symbol) {
        try {
            logger.info("Fetching company growth for symbol: {}", symbol);
            Map<String, Object> growth = stockService.getCompanyGrowth(symbol);
            return ResponseEntity.ok(growth);
        } catch (Exception e) {
            logger.error("Error fetching company growth: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching company growth: " + e.getMessage()));
        }
    }

    @GetMapping("/dcf/{symbol}")
    public ResponseEntity<?> getDCF(@PathVariable String symbol) {
        try {
            logger.info("Fetching DCF for symbol: {}", symbol);
            Map<String, Object> dcf = stockService.getDCF(symbol);
            return ResponseEntity.ok(dcf);
        } catch (Exception e) {
            logger.error("Error fetching DCF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching DCF: " + e.getMessage()));
        }
    }

    @GetMapping("/rating/{symbol}")
    public ResponseEntity<?> getRating(@PathVariable String symbol) {
        try {
            logger.info("Fetching rating for symbol: {}", symbol);
            Map<String, Object> rating = stockService.getRating(symbol);
            return ResponseEntity.ok(rating);
        } catch (Exception e) {
            logger.error("Error fetching rating: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching rating: " + e.getMessage()));
        }
    }

    @GetMapping("/financials-full/{symbol}")
    public ResponseEntity<?> getFullFinancialStatements(@PathVariable String symbol) {
        try {
            logger.info("Fetching full financial statements for symbol: {}", symbol);
            Map<String, Object> financials = stockService.getFullFinancialStatements(symbol);
            
            if (financials == null || financials.isEmpty()) {
                logger.warn("No financial statement data found for symbol: {}", symbol);
                return ResponseEntity.ok(Collections.emptyMap());
            }
            
            logger.info("Successfully retrieved full financial statements for {}", symbol);
            return ResponseEntity.ok(financials);
        } catch (Exception e) {
            logger.error("Error fetching full financial statements for {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Error fetching full financial statements: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis(),
                    "symbol", symbol
                ));
        }
    }

    @GetMapping("/ratios-ttm/{symbol}")
    public ResponseEntity<?> getRatiosTTM(@PathVariable String symbol) {
        try {
            logger.info("Fetching TTM ratios for symbol: {}", symbol);
            Map<String, Object> ratios = stockService.getRatiosTTM(symbol);
            return ResponseEntity.ok(ratios);
        } catch (Exception e) {
            logger.error("Error fetching TTM ratios: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching TTM ratios: " + e.getMessage()));
        }
    }

    @GetMapping("/key-metrics-ttm/{symbol}")
    public ResponseEntity<?> getKeyMetricsTTM(@PathVariable String symbol) {
        try {
            logger.info("Fetching TTM key metrics for symbol: {}", symbol);
            Map<String, Object> metrics = stockService.getKeyMetricsTTM(symbol);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error fetching TTM key metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching TTM key metrics: " + e.getMessage()));
        }
    }

    @GetMapping("/dividends/{symbol}")
    public ResponseEntity<?> getDividendHistory(@PathVariable String symbol) {
        try {
            logger.info("Fetching dividend history for symbol: {}", symbol);
            Map<String, Object> dividends = stockService.getDividendHistory(symbol);
            return ResponseEntity.ok(dividends);
        } catch (Exception e) {
            logger.error("Error fetching dividend history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error fetching dividend history: " + e.getMessage()));
        }
    }

    @GetMapping("/graham-valuation/{symbol}")
    public ResponseEntity<?> getGrahamValuation(@PathVariable String symbol) {
        try {
            logger.info("Calculating Graham's valuation for symbol: {}", symbol);
            Map<String, Object> grahamValuation = stockService.calculateGrahamValuation(symbol);
            return ResponseEntity.ok(grahamValuation);
        } catch (Exception e) {
            logger.error("Error calculating Graham's valuation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Error calculating Graham's valuation: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis(),
                    "symbol", symbol
                ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchStocks(
            @RequestParam String query,
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        try {
            logger.info("Searching stocks with query: {}, exchange: {}, limit: {}", query, exchange, limit);
            var results = stockService.searchStocks(query, exchange, limit);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error searching stocks", e);
            return ResponseEntity.status(500).body("Error searching stocks: " + e.getMessage());
        }
    }
} 