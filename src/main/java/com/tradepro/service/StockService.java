package com.tradepro.service;

import com.tradepro.model.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final String BASE_URL = "https://financialmodelingprep.com/api/v3";
    private static final Logger logger = LoggerFactory.getLogger(StockService.class);

    @Autowired
    public StockService(@Value("${fmp.api.key}") String apiKey, RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "stockSearch", key = "#symbol", unless = "#result == null")
    public Map<String, Object> searchStock(String symbol) {
        try {
            String url = BASE_URL + "/search?query={symbol}&apikey={apiKey}";
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );

            Map<String, Object> result = new HashMap<>();
            result.put("search", response.getBody());
            
            // Also fetch quote data for the exact symbol
            String quoteUrl = BASE_URL + "/quote/{symbol}?apikey={apiKey}";
            ResponseEntity<List<Map<String, Object>>> quoteResponse = restTemplate.exchange(
                quoteUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            result.put("quote", quoteResponse.getBody());
            return result;
            
        } catch (Exception e) {
            logger.error("Error searching stock: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search stock: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "stockDetails", key = "#symbol", unless = "#result == null")
    public Map<String, Object> getStockDetails(String symbol) {
        try {
            Map<String, Object> allData = new HashMap<>();
            
            // Fetch profile data
            String profileUrl = BASE_URL + "/profile/{symbol}?apikey={apiKey}";
            ResponseEntity<List<Map<String, Object>>> profileResponse = restTemplate.exchange(
                profileUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            allData.put("profile", profileResponse.getBody());

            // Fetch key metrics
            String metricsUrl = BASE_URL + "/key-metrics/{symbol}?apikey={apiKey}&limit=4";
            ResponseEntity<List<Map<String, Object>>> metricsResponse = restTemplate.exchange(
                metricsUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            allData.put("metrics", metricsResponse.getBody());

            return allData;
        } catch (Exception e) {
            logger.error("Error fetching stock details: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch stock details: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "priceHistory", key = "#symbol + #timeframe", unless = "#result == null")
    public List<Map<String, Object>> getPriceHistory(String symbol, String timeframe) {
        try {
            String url = BASE_URL + "/historical-price-full/{symbol}?apikey={apiKey}&timeseries=90";
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                symbol,
                apiKey
            );
            
            Map<String, Object> data = response.getBody();
            return (List<Map<String, Object>>) data.get("historical");
        } catch (Exception e) {
            logger.error("Error fetching price history: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch price history: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "stockQuote", key = "#symbol")
    public Map<String, Object> getQuote(String symbol) {
        try {
            String url = BASE_URL + "/quote/{symbol}?apikey={apiKey}";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            List<Map<String, Object>> quotes = response.getBody();
            return quotes != null && !quotes.isEmpty() ? quotes.get(0) : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching quote: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch quote: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "stockProfile", key = "#symbol")
    public Map<String, Object> getProfile(String symbol) {
        try {
            String url = BASE_URL + "/profile/{symbol}?apikey={apiKey}";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            List<Map<String, Object>> profiles = response.getBody();
            return profiles != null && !profiles.isEmpty() ? profiles.get(0) : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching profile: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch profile: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "historicalPrices", key = "#symbol")
    public Map<String, Object> getHistoricalPrices(String symbol) {
        try {
            String url = BASE_URL + "/historical-price-full/{symbol}?apikey={apiKey}&timeseries=90";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                symbol,
                apiKey
            );
            
            return response.getBody() != null ? response.getBody() : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching historical prices: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch historical prices: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "financialStatements", key = "#symbol")
    public Map<String, Object> getFinancialStatements(String symbol) {
        try {
            logger.info("Starting to fetch financial statements for symbol: {}", symbol);
            Map<String, Object> financials = new HashMap<>();
            
            // Fetch income statement
            String incomeUrl = BASE_URL + "/income-statement/{symbol}?period=annual&apikey={apiKey}";
            ResponseEntity<List<Map<String, Object>>> incomeResponse = restTemplate.exchange(
                incomeUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            financials.put("income", incomeResponse.getBody());
            logger.debug("Income Statement Response: {}", incomeResponse.getBody());

            // Fetch balance sheet
            String balanceUrl = BASE_URL + "/balance-sheet-statement/{symbol}?period=annual&apikey={apiKey}";
            ResponseEntity<List<Map<String, Object>>> balanceResponse = restTemplate.exchange(
                balanceUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            financials.put("balance", balanceResponse.getBody());
            logger.debug("Balance Sheet Response: {}", balanceResponse.getBody());

            // Fetch cash flow statement
            String cashFlowUrl = BASE_URL + "/cash-flow-statement/{symbol}?period=annual&apikey={apiKey}";
            ResponseEntity<List<Map<String, Object>>> cashFlowResponse = restTemplate.exchange(
                cashFlowUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            financials.put("cashFlow", cashFlowResponse.getBody());
            logger.debug("Cash Flow Response: {}", cashFlowResponse.getBody());

            // Process and combine data for ratios
            List<Map<String, Object>> processedData = new ArrayList<>();
            List<Map<String, Object>> incomeStatements = incomeResponse.getBody();
            List<Map<String, Object>> balanceSheets = balanceResponse.getBody();
            List<Map<String, Object>> cashFlows = cashFlowResponse.getBody();

            if (incomeStatements != null && balanceSheets != null && cashFlows != null) {
                for (int i = 0; i < Math.min(Math.min(incomeStatements.size(), balanceSheets.size()), cashFlows.size()); i++) {
                    Map<String, Object> income = incomeStatements.get(i);
                    Map<String, Object> balance = balanceSheets.get(i);
                    Map<String, Object> cashFlow = cashFlows.get(i);

                    Map<String, Object> combined = new HashMap<>();
                    combined.put("date", income.get("date"));
                    combined.put("revenue", income.get("revenue"));
                    combined.put("grossProfit", income.get("grossProfit"));
                    combined.put("operatingIncome", income.get("operatingIncome"));
                    combined.put("netIncome", income.get("netIncome"));
                    combined.put("totalAssets", balance.get("totalAssets"));
                    combined.put("totalStockholdersEquity", balance.get("totalStockholdersEquity"));
                    combined.put("operatingCashFlow", cashFlow.get("operatingCashFlow"));
                    combined.put("freeCashFlow", cashFlow.get("freeCashFlow"));

                    processedData.add(combined);
                }
            }

            financials.put("processedData", processedData);
            logger.info("Successfully processed financial statements for {}", symbol);
            return financials;

        } catch (Exception e) {
            logger.error("Error fetching financial statements for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Failed to fetch financial statements: " + e.getMessage(), e);
        }
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    @Cacheable(value = "keyMetrics", key = "#symbol")
    public Map<String, Object> getKeyMetrics(String symbol) {
        try {
            String url = BASE_URL + "/key-metrics/{symbol}?apikey={apiKey}&limit=4";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            List<Map<String, Object>> metrics = response.getBody();
            return metrics != null && !metrics.isEmpty() ? metrics.get(0) : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching key metrics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch key metrics: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "ratios", key = "#symbol")
    public Map<String, Object> getRatios(String symbol) {
        try {
            String url = BASE_URL + "/ratios/{symbol}?apikey={apiKey}&limit=4";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            List<Map<String, Object>> ratios = response.getBody();
            return ratios != null && !ratios.isEmpty() ? ratios.get(0) : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching ratios: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch ratios: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "enterpriseValue", key = "#symbol")
    public Map<String, Object> getEnterpriseValue(String symbol) {
        try {
            String url = BASE_URL + "/enterprise-values/{symbol}?apikey={apiKey}&limit=4";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            List<Map<String, Object>> values = response.getBody();
            return values != null && !values.isEmpty() ? values.get(0) : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching enterprise value: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch enterprise value: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "companyGrowth", key = "#symbol")
    public Map<String, Object> getCompanyGrowth(String symbol) {
        try {
            String url = BASE_URL + "/financial-growth/{symbol}?apikey={apiKey}&limit=4";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            List<Map<String, Object>> growth = response.getBody();
            return growth != null && !growth.isEmpty() ? growth.get(0) : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching company growth: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch company growth: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "dcf", key = "#symbol")
    public Map<String, Object> getDCF(String symbol) {
        try {
            String url = BASE_URL + "/discounted-cash-flow/{symbol}?apikey={apiKey}";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            List<Map<String, Object>> dcf = response.getBody();
            return dcf != null && !dcf.isEmpty() ? dcf.get(0) : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching DCF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch DCF: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "rating", key = "#symbol")
    public Map<String, Object> getRating(String symbol) {
        try {
            String url = BASE_URL + "/rating/{symbol}?apikey={apiKey}";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            List<Map<String, Object>> rating = response.getBody();
            return rating != null && !rating.isEmpty() ? rating.get(0) : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching rating: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch rating: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "financialStatementsFull", key = "#symbol")
    public Map<String, Object> getFullFinancialStatements(String symbol) {
        try {
            logger.info("Fetching full financial statements for symbol: {}", symbol);
            String url = BASE_URL + "/financial-statement-full-as-reported/{symbol}?period=annual&limit=50&apikey={apiKey}";
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );

            List<Map<String, Object>> statements = response.getBody();
            Map<String, Object> result = new HashMap<>();

            if (statements != null && !statements.isEmpty()) {
                // Get the most recent statement
                Map<String, Object> latestStatement = statements.get(0);
                
                // Extract key financial metrics
                Map<String, Object> keyMetrics = new HashMap<>();
                keyMetrics.put("revenue", latestStatement.get("revenuefromcontractwithcustomerexcludingassessedtax"));
                keyMetrics.put("grossProfit", latestStatement.get("grossprofit"));
                keyMetrics.put("operatingIncome", latestStatement.get("operatingincomeloss"));
                keyMetrics.put("netIncome", latestStatement.get("netincomeloss"));
                keyMetrics.put("eps", latestStatement.get("earningspersharediluted"));
                keyMetrics.put("totalAssets", latestStatement.get("assets"));
                keyMetrics.put("totalLiabilities", latestStatement.get("liabilities"));
                keyMetrics.put("stockholdersEquity", latestStatement.get("stockholdersequity"));
                keyMetrics.put("cashAndEquivalents", latestStatement.get("cashandcashequivalentsatcarryingvalue"));
                
                // Add historical data
                result.put("historical", statements);
                result.put("metrics", keyMetrics);
                result.put("latestStatement", latestStatement);
            }

            logger.debug("Full financial statements response: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error fetching full financial statements for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Failed to fetch full financial statements: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "advancedDCF", key = "#symbol")
    public Map<String, Object> getAdvancedDCF(String symbol) {
        try {
            logger.info("Fetching advanced DCF for symbol: {}", symbol);
            String url = BASE_URL + "/v4/advanced_discounted_cash_flow?symbol={symbol}&apikey={apiKey}";
            logger.debug("Making request to URL: {}", url.replace(apiKey, "API_KEY"));
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                symbol,
                apiKey
            );
            
            Map<String, Object> result = response.getBody();
            logger.debug("Advanced DCF response: {}", result);
            return result != null ? result : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching advanced DCF for {}: {}", symbol, e.getMessage());
            logger.error("Stack trace:", e);
            throw new RuntimeException("Failed to fetch advanced DCF: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "valuationMetrics", key = "#symbol")
    public Map<String, Object> getValuationMetrics(String symbol) {
        try {
            logger.info("Starting to fetch valuation metrics for symbol: {}", symbol);
            Map<String, Object> valuationData = new HashMap<>();
            
            // Fetch ratios TTM for valuation metrics
            String ratiosUrl = BASE_URL + "/ratios-ttm/{symbol}?apikey={apiKey}";
            ResponseEntity<Map<String, Object>> ratiosResponse = restTemplate.exchange(
                ratiosUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                symbol,
                apiKey
            );
            Map<String, Object> ratios = ratiosResponse.getBody();
            logger.debug("Ratios TTM Response: {}", ratios);

            if (ratios != null) {
                // Key Valuation Metrics
                Map<String, Object> valuationMetrics = new HashMap<>();
                valuationMetrics.put("peRatio", ratios.get("peRatioTTM"));
                valuationMetrics.put("evToEbitda", ratios.get("enterpriseValueOverEBITDATTM"));
                valuationMetrics.put("pbRatio", ratios.get("priceToBookRatioTTM"));
                valuationMetrics.put("evToSales", ratios.get("evToSalesTTM"));
                valuationData.put("metrics", valuationMetrics);

                // Growth & Returns
                Map<String, Object> growthMetrics = new HashMap<>();
                growthMetrics.put("roe", ratios.get("returnOnEquityTTM"));
                growthMetrics.put("roic", ratios.get("returnOnCapitalEmployedTTM"));
                growthMetrics.put("operatingMargin", ratios.get("operatingProfitMarginTTM"));
                growthMetrics.put("netMargin", ratios.get("netProfitMarginTTM"));
                valuationData.put("growth", growthMetrics);

                // Per Share Metrics
                Map<String, Object> perShareMetrics = new HashMap<>();
                perShareMetrics.put("revenuePerShare", ratios.get("revenuePerShareTTM"));
                perShareMetrics.put("fcfPerShare", ratios.get("freeCashFlowPerShareTTM"));
                perShareMetrics.put("bookValuePerShare", ratios.get("bookValuePerShareTTM"));
                perShareMetrics.put("cashPerShare", ratios.get("cashPerShareTTM"));
                valuationData.put("perShare", perShareMetrics);
            }

            logger.info("Successfully processed valuation metrics for {}", symbol);
            return valuationData;
        } catch (Exception e) {
            logger.error("Error fetching valuation metrics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch valuation metrics: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "keyMetricsTTM", key = "#symbol")
    public Map<String, Object> getKeyMetricsTTM(String symbol) {
        try {
            logger.info("Fetching TTM key metrics for {}", symbol);
            String url = BASE_URL + "/key-metrics-ttm/{symbol}?apikey={apiKey}";
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            List<Map<String, Object>> metrics = response.getBody();
            logger.debug("Key Metrics TTM Response: {}", metrics);
            
            // Return the first item in the array if available, otherwise empty map
            Map<String, Object> ttmMetrics = metrics != null && !metrics.isEmpty() ? metrics.get(0) : Collections.emptyMap();
            
            // Process and organize the metrics
            Map<String, Object> processedMetrics = new HashMap<>();
            
            // Per share metrics - using the correct TTM property names
            Map<String, Object> perShare = new HashMap<>();
            perShare.put("revenuePerShare", ttmMetrics.get("revenuePerShareTTM"));
            perShare.put("freeCashFlowPerShare", ttmMetrics.get("freeCashFlowPerShareTTM"));
            perShare.put("bookValuePerShare", ttmMetrics.get("bookValuePerShareTTM"));
            perShare.put("cashPerShare", ttmMetrics.get("cashPerShareTTM"));
            
            // Efficiency metrics
            Map<String, Object> efficiency = new HashMap<>();
            efficiency.put("receivablesTurnover", ttmMetrics.get("receivablesTurnoverTTM"));
            efficiency.put("inventoryTurnover", ttmMetrics.get("inventoryTurnoverTTM"));
            efficiency.put("assetTurnover", ttmMetrics.get("assetTurnoverTTM"));
            
            // Returns metrics
            Map<String, Object> returns = new HashMap<>();
            returns.put("roic", ttmMetrics.get("roicTTM"));
            returns.put("roe", ttmMetrics.get("roeTTM"));
            returns.put("roa", ttmMetrics.get("returnOnTangibleAssetsTTM"));
            
            // Valuation metrics
            Map<String, Object> valuation = new HashMap<>();
            valuation.put("peRatio", ttmMetrics.get("peRatioTTM"));
            valuation.put("pbRatio", ttmMetrics.get("pbRatioTTM"));
            valuation.put("evToSales", ttmMetrics.get("evToSalesTTM"));
            valuation.put("enterpriseValueOverEBITDA", ttmMetrics.get("enterpriseValueOverEBITDATTM"));
            
            Map<String, Object> result = new HashMap<>();
            result.put("perShare", perShare);
            result.put("efficiency", efficiency);
            result.put("returns", returns);
            result.put("valuation", valuation);
            result.put("raw", ttmMetrics); // Keep the raw data for reference
            
            logger.debug("Processed TTM Metrics - Per Share: {}", perShare);
            return result;
        } catch (Exception e) {
            logger.error("Error fetching TTM key metrics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch TTM key metrics: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "ratiosTTM", key = "#symbol")
    public Map<String, Object> getRatiosTTM(String symbol) {
        try {
            logger.info("Fetching TTM ratios for {}", symbol);
            String url = BASE_URL + "/ratios-ttm/{symbol}?apikey={apiKey}";
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                symbol,
                apiKey
            );
            
            List<Map<String, Object>> ratiosList = response.getBody();
            logger.debug("TTM Ratios Response: {}", ratiosList);
            
            // Return the first item in the array if available, otherwise empty map
            return ratiosList != null && !ratiosList.isEmpty() ? ratiosList.get(0) : Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error fetching TTM ratios: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch TTM ratios: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "dividendHistory", key = "#symbol")
    public Map<String, Object> getDividendHistory(String symbol) {
        try {
            logger.info("Fetching dividend history for {}", symbol);
            String url = BASE_URL + "/historical-price-full/stock_dividend/{symbol}?apikey={apiKey}";
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                symbol,
                apiKey
            );
            
            Map<String, Object> dividendData = response.getBody();
            logger.debug("Dividend History Response: {}", dividendData);
            
            // Process and organize the dividend data
            Map<String, Object> processedData = new HashMap<>();
            if (dividendData != null && dividendData.containsKey("historical")) {
                List<Map<String, Object>> historical = (List<Map<String, Object>>) dividendData.get("historical");
                processedData.put("historical", historical);
                
                // Calculate additional metrics
                if (!historical.isEmpty()) {
                    Map<String, Object> latestDividend = historical.get(0);
                    double currentDividend = Double.parseDouble(latestDividend.get("dividend").toString());
                    
                    // Calculate annual dividend
                    double annualDividend = currentDividend * 4; // Assuming quarterly payments
                    
                    // Calculate dividend growth
                    double previousYearDividend = historical.stream()
                        .filter(div -> {
                            String date = (String) div.get("date");
                            return date.startsWith(String.valueOf(LocalDate.now().getYear() - 1));
                        })
                        .mapToDouble(div -> Double.parseDouble(div.get("dividend").toString()))
                        .sum();
                    
                    double dividendGrowth = previousYearDividend > 0 ? 
                        ((annualDividend / previousYearDividend) - 1) * 100 : 0;
                    
                    Map<String, Object> metrics = new HashMap<>();
                    metrics.put("currentDividend", currentDividend);
                    metrics.put("annualDividend", annualDividend);
                    metrics.put("dividendGrowth", dividendGrowth);
                    metrics.put("frequency", "Quarterly");
                    metrics.put("latestPaymentDate", latestDividend.get("paymentDate"));
                    metrics.put("latestRecordDate", latestDividend.get("recordDate"));
                    metrics.put("latestDeclarationDate", latestDividend.get("declarationDate"));
                    
                    processedData.put("metrics", metrics);
                }
            }
            
            return processedData;
        } catch (Exception e) {
            logger.error("Error fetching dividend history: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch dividend history: " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "grahamValuation", key = "#symbol")
    public Map<String, Object> calculateGrahamValuation(String symbol) {
        try {
            logger.info("Starting valuations calculation for {}", symbol);
            Map<String, Object> result = new HashMap<>();

            // Fetch required data
            String url = BASE_URL + "/ratios-ttm/" + symbol + "?apikey=" + apiKey;
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> ratiosList = response.getBody();
            logger.debug("Raw API Response - ratiosList: {}", ratiosList);

            Map<String, Object> quote = getQuote(symbol);
            logger.debug("Quote Response: {}", quote);

            if (ratiosList != null && !ratiosList.isEmpty() && quote != null) {
                Map<String, Object> ratios = ratiosList.get(0);
                logger.debug("TTM Ratios: {}", ratios);
                
                // Get current price from quote
                double currentPrice = getDoubleValue(quote, "price");
                logger.debug("Current Price: {}", currentPrice);
                
                // Get TTM ratios for Buffett calculation
                double operatingCashFlowPerShare = getDoubleValue(ratios, "operatingCashFlowPerShareTTM");
                double freeCashFlowPerShare = getDoubleValue(ratios, "freeCashFlowPerShareTTM");
                double sharesOutstanding = getDoubleValue(quote, "sharesOutstanding");
                
                logger.debug("Buffett Calculation Inputs (Per Share):");
                logger.debug("Operating Cash Flow Per Share: {}", operatingCashFlowPerShare);
                logger.debug("Free Cash Flow Per Share: {}", freeCashFlowPerShare);
                logger.debug("Shares Outstanding: {}", sharesOutstanding);
                
                // Calculate EPS and Book Value using TTM ratios
                double eps = getDoubleValue(ratios, "priceEarningsRatioTTM") > 0 ? currentPrice / getDoubleValue(ratios, "priceEarningsRatioTTM") : 0.0;
                double bookValue = getDoubleValue(ratios, "priceToBookRatioTTM") > 0 ? currentPrice / getDoubleValue(ratios, "priceToBookRatioTTM") : 0.0;
                logger.debug("Calculated values - EPS: {}, Book Value: {}", eps, bookValue);
                
                // Calculate Graham Number
                double grahamNumber = 0.0;
                if (eps > 0 && bookValue > 0) {
                    grahamNumber = Math.sqrt(22.5 * eps * bookValue);
                    logger.debug("Graham Number calculation: sqrt(22.5 * {} * {}) = {}", eps, bookValue, grahamNumber);
                } else {
                    logger.warn("Cannot calculate Graham Number - EPS: {}, Book Value: {}", eps, bookValue);
                }

                // Calculate margin of safety
                double grahamMarginOfSafety = grahamNumber > 0 ? 
                    ((grahamNumber - currentPrice) / grahamNumber) * 100 : 0.0;
                logger.debug("Graham Margin of Safety: {}%", grahamMarginOfSafety);

                // Calculate Peter Lynch Fair Value using TTM values
                // Lynch's formula: Fair Value = EPS * (1 + Sustainable Growth Rate) * Base P/E
                double sustainableGrowthRate = getDoubleValue(ratios, "returnOnEquityTTM") / 100; // ROE as growth rate
                double lynchFairValue = eps * (1 + sustainableGrowthRate) * getDoubleValue(ratios, "priceEarningsRatioTTM");

                // Calculate Buffett Number using Owner Earnings (more accurate FCF calculation)
                logger.debug("Starting Buffett calculation");

                // Get TTM cash flow values
                logger.debug("Buffett Calculation Inputs:");
                logger.debug("Operating Cash Flow Per Share: {}", operatingCashFlowPerShare);
                logger.debug("Free Cash Flow Per Share: {}", freeCashFlowPerShare);
                logger.debug("Shares Outstanding: {}", sharesOutstanding);

                // Calculate owner earnings per share
                double maintenanceCapExPerShare = (operatingCashFlowPerShare - freeCashFlowPerShare) * 0.7;
                double ownerEarningsPerShare = operatingCashFlowPerShare - maintenanceCapExPerShare;

                // Calculate total values
                double ownerEarnings = ownerEarningsPerShare * sharesOutstanding;
                double buffettNumber = ownerEarnings * 12;  // 12x multiple for stable companies
                double buffettPerShareValue = ownerEarningsPerShare * 12;  // Per share value directly

                logger.debug("Buffett calculations:");
                logger.debug("Maintenance CapEx Per Share: {}", maintenanceCapExPerShare);
                logger.debug("Owner Earnings Per Share: {}", ownerEarningsPerShare);
                logger.debug("Total Owner Earnings: {}", ownerEarnings);
                logger.debug("Buffett Number: {}", buffettNumber);
                logger.debug("Buffett Per Share Value: {}", buffettPerShareValue);

                // Calculate metrics for all valuations
                double lynchMarginOfSafety = lynchFairValue > 0 ? 
                    ((lynchFairValue - currentPrice) / lynchFairValue) * 100 : 0.0;
                double buffettMarginOfSafety = buffettNumber > 0 ?
                    ((buffettNumber - currentPrice) / buffettNumber) * 100 : 0.0;
                
                // Prepare result
                result.put("grahamNumber", grahamNumber);
                result.put("lynchFairValue", lynchFairValue);
                result.put("buffettNumber", buffettNumber);
                result.put("buffettPerShareValue", buffettPerShareValue);
                result.put("currentPrice", currentPrice);
                result.put("eps", eps);
                result.put("bookValue", bookValue);
                result.put("grahamMarginOfSafety", grahamMarginOfSafety);
                result.put("lynchMarginOfSafety", lynchMarginOfSafety);
                result.put("buffettMarginOfSafety", buffettMarginOfSafety);
                result.put("isGrahamBuy", currentPrice < grahamNumber);
                result.put("isLynchBuy", currentPrice < lynchFairValue);
                result.put("isBuffettBuy", currentPrice < buffettNumber);
                result.put("peRatio", getDoubleValue(ratios, "priceEarningsRatioTTM"));
                result.put("dividendYield", getDoubleValue(ratios, "dividendYielTTM"));
                result.put("roe", getDoubleValue(ratios, "returnOnEquityTTM"));
                result.put("assumedGrowthRate", sustainableGrowthRate);
                result.put("ownerEarnings", ownerEarnings);
                
                logger.info("Successfully calculated valuations for {}: Buffett = {}, Graham = {}, Lynch = {}", 
                           symbol, buffettNumber, grahamNumber, lynchFairValue);
                return result;
            } else {
                logger.warn("Missing required data for valuation calculation for {}", symbol);
                return createEmptyValuation();
            }
        } catch (Exception e) {
            logger.error("Error calculating valuations: {}", e.getMessage(), e);
            return createEmptyValuation();
        }
    }

    private Map<String, Object> createEmptyValuation() {
        return new HashMap<String, Object>() {{
            put("grahamNumber", 0.0);
            put("lynchFairValue", 0.0);
            put("buffettNumber", 0.0);
            put("buffettPerShareValue", 0.0);
            put("currentPrice", 0.0);
            put("eps", 0.0);
            put("bookValue", 0.0);
            put("grahamMarginOfSafety", 0.0);
            put("lynchMarginOfSafety", 0.0);
            put("buffettMarginOfSafety", 0.0);
            put("isGrahamBuy", false);
            put("isLynchBuy", false);
            put("isBuffettBuy", false);
            put("peRatio", 0.0);
            put("dividendYield", 0.0);
            put("roe", 0.0);
            put("assumedGrowthRate", 0.0);
            put("ownerEarnings", 0.0);
        }};
    }

    @Cacheable(value = "stockSearch", key = "#query + #exchange + #limit")
    public Object searchStocks(String query, String exchange, Integer limit) {
        StringBuilder urlBuilder = new StringBuilder(BASE_URL + "/search?query=" + query);
        
        if (exchange != null && !exchange.isEmpty()) {
            urlBuilder.append("&exchange=").append(exchange);
        }
        if (limit != null) {
            urlBuilder.append("&limit=").append(limit);
        }
        urlBuilder.append("&apikey=").append(apiKey);

        String url = urlBuilder.toString();
        logger.info("Making API request to: {}", url.replace(apiKey, "HIDDEN"));

        return restTemplate.getForObject(url, Object.class);
    }
} 