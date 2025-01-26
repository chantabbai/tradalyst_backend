package com.tradepro.controller;

import com.tradepro.dto.BrokerTradeRecord;
import com.tradepro.dto.CsvTradeRecord;
import com.tradepro.exception.CsvImportException;
import com.tradepro.model.Exit;
import com.tradepro.model.Trade;
import com.tradepro.service.CsvImportService;
import com.tradepro.service.TradeService;
import com.tradepro.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for handling trade-related operations.
 * Provides endpoints for managing trades in the trading journal application.
 */
@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private static final Logger logger = LoggerFactory.getLogger(TradeController.class);

    @Autowired
    private TradeService tradeService;

    @Autowired
    private UserService userService;

    @Autowired
    private CsvImportService csvImportService;

    /**
     * Adds a new trade to the system.
     * @param trade The trade object to be added
     * @param token Authorization token for user identification
     * @return ResponseEntity containing the saved trade or error message
     */
    @PostMapping
    public ResponseEntity<?> addTrade(@RequestBody Trade trade, @RequestHeader("Authorization") String token) {
        try {
            logger.info("Received request to add trade: {}", trade);
            String userId = extractUserIdFromToken(token);
            logger.info("Extracted userId: {}", userId);
            
            // Ensure entryDate is set
            if (trade.getEntryDate() == null) {
                return ResponseEntity.badRequest().body("Entry date is required");
            }
            
            trade.setUserId(userId);
            Trade newTrade = tradeService.addTrade(trade);
            logger.info("Trade added successfully: {}", newTrade);
            return ResponseEntity.ok(newTrade);
        } catch (Exception e) {
            logger.error("Error adding trade", e);
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Retrieves all trades for a specific user.
     * @param userId The ID of the user whose trades are to be retrieved
     * @return List of trades belonging to the user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Trade>> getTradesByUserId(@PathVariable String userId) {
        List<Trade> trades = tradeService.getTradesByUserId(userId);
        return ResponseEntity.ok(trades);
    }

    /**
     * Updates an existing trade.
     * @param id The ID of the trade to update
     * @param updatedTrade The trade object containing updated information
     * @return ResponseEntity containing the updated trade
     */
    @PutMapping("/{id}")
    public ResponseEntity<Trade> updateTrade(@PathVariable String id, @RequestBody Trade updatedTrade) {
        logger.info("Received update request for trade id: {} with data: {}", id, updatedTrade);
        logger.info("Entry date from request: {}", updatedTrade.getEntryDate());
        try {
            Trade existingTrade = tradeService.findById(id);
            if (existingTrade == null) {
                throw new RuntimeException("Trade not found with id: " + id);
            }

            // Update fields while preserving the entryDate if it exists in the request
            existingTrade.setSymbol(updatedTrade.getSymbol());
            existingTrade.setAction(updatedTrade.getAction());
            existingTrade.setQuantity(updatedTrade.getQuantity());
            existingTrade.setPrice(updatedTrade.getPrice());
            existingTrade.setType(updatedTrade.getType());
            existingTrade.setOptionType(updatedTrade.getOptionType());
            existingTrade.setStrategy(updatedTrade.getStrategy());
            existingTrade.setNotes(updatedTrade.getNotes());
            
            // Only update entryDate if it's provided in the request
            if (updatedTrade.getEntryDate() != null) {
                existingTrade.setEntryDate(updatedTrade.getEntryDate());
            }

            Trade savedTrade = tradeService.save(existingTrade);
            logger.info("Trade updated successfully: {}", savedTrade);
            return ResponseEntity.ok(savedTrade);
        } catch (Exception e) {
            logger.error("Error updating trade: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Records an exit for an existing trade.
     * @param id The ID of the trade to exit
     * @param request Object containing exit details (date, price, quantity)
     * @return ResponseEntity containing the updated trade with exit information
     */
    @PostMapping("/{id}/exit")
    public ResponseEntity<?> exitTrade(@PathVariable String id, @RequestBody ExitTradeRequest request) {
        try {
            logger.info("Received exit trade request for trade id: {}, exitDate: {}, exitPrice: {}, exitQuantity: {}", 
                        id, request.getExitDate(), request.getExitPrice(), request.getExitQuantity());
            Trade exitedTrade = tradeService.exitTrade(id, request.getExitDate(), request.getExitPrice(), request.getExitQuantity());
            logger.info("Trade exited successfully: {}", exitedTrade);
            return ResponseEntity.ok(exitedTrade);
        } catch (Exception e) {
            logger.error("Error exiting trade", e);
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Retrieves trade statistics including counts, win ratio, and profit metrics.
     * @param token Authorization token for user identification
     * @return ResponseEntity containing trade statistics
     */
    @GetMapping("/count")
    public ResponseEntity<?> getTradesCounts(@RequestHeader("Authorization") String token) {
        try {
            String userId = extractUserIdFromToken(token);
            logger.info("Fetching trade counts for userId: {}", userId);
            List<Trade> allTrades = tradeService.getTradesByUserId(userId);
            
            // Calculate counts
            long totalTrades = allTrades.size();
            long openTrades = allTrades.stream()
                .filter(trade -> "OPEN".equals(trade.getStatus()))
                .count();
            long closedTrades = allTrades.stream()
                .filter(trade -> "CLOSED".equals(trade.getStatus()) || "PARTIALLY_CLOSED".equals(trade.getStatus()))
                .count();

            // Calculate win ratio
            double winRatio = 0.0;
            if (closedTrades > 0) {
                long winningTrades = allTrades.stream()
                    .filter(trade -> 
                        ("CLOSED".equals(trade.getStatus()) || "PARTIALLY_CLOSED".equals(trade.getStatus())) 
                        && trade.getTotalProfit() != null 
                        && trade.getTotalProfit() > 0)
                    .count();
                winRatio = (double) winningTrades / closedTrades;
            }

            // Calculate average profit for closed trades
            double avgProfit = 0.0;
            if (closedTrades > 0) {
                avgProfit = allTrades.stream()
                    .filter(trade -> 
                        ("CLOSED".equals(trade.getStatus()) || "PARTIALLY_CLOSED".equals(trade.getStatus())) 
                        && trade.getTotalProfit() != null)
                    .mapToDouble(Trade::getTotalProfit)
                    .average()
                    .orElse(0.0);
            }

            // Calculate biggest win and loss
            double biggestWin = allTrades.stream()
                .filter(trade -> 
                    ("CLOSED".equals(trade.getStatus()) || "PARTIALLY_CLOSED".equals(trade.getStatus())) 
                    && trade.getTotalProfit() != null
                    && trade.getTotalProfit() > 0)
                .mapToDouble(Trade::getTotalProfit)
                .max()
                .orElse(0.0);

            double biggestLoss = allTrades.stream()
                .filter(trade -> 
                    ("CLOSED".equals(trade.getStatus()) || "PARTIALLY_CLOSED".equals(trade.getStatus())) 
                    && trade.getTotalProfit() != null
                    && trade.getTotalProfit() < 0)
                .mapToDouble(Trade::getTotalProfit)
                .min()
                .orElse(0.0);

            Map<String, Object> response = new HashMap<>();
            response.put("total", totalTrades);
            response.put("open", openTrades);
            response.put("closed", closedTrades);
            response.put("winRatio", winRatio);
            response.put("avgProfit", avgProfit);
            response.put("biggestWin", biggestWin > 0 ? biggestWin : null);
            response.put("biggestLoss", biggestLoss < 0 ? biggestLoss : null);

            logger.info("Trade statistics retrieved successfully: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching trade statistics", e);
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Retrieves profit/loss chart data for a specific time frame.
     * @param token Authorization token for user identification
     * @param timeFrame Time period for which to retrieve data (1W, 1M, 3M, 6M, 1Y, ALL)
     * @return ResponseEntity containing chart data points
     */
    @GetMapping("/pnl-chart")
    public ResponseEntity<?> getPnLChartData(
            @RequestHeader("Authorization") String token,
            @RequestParam String timeFrame) {
        try {
            String userId = extractUserIdFromToken(token);
            logger.info("Fetching P/L chart data for userId: {} and timeFrame: {}", userId, timeFrame);
            
            // Calculate start date based on timeFrame
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = switch (timeFrame) {
                case "1W" -> endDate.minusWeeks(1);
                case "1M" -> endDate.minusMonths(1);
                case "3M" -> endDate.minusMonths(3);
                case "6M" -> endDate.minusMonths(6);
                case "YTD" -> LocalDate.of(endDate.getYear(), 1, 1); // Start from January 1st of current year
                default -> LocalDate.parse("2000-01-01"); // For "ALL" time frame
            };

            logger.info("Date range: {} to {}", startDate, endDate);

            // Get all closed trades
            List<Trade> allTrades = tradeService.getTradesByUserId(userId);
            List<Trade> closedTrades = allTrades.stream()
                .filter(trade -> 
                    ("CLOSED".equals(trade.getStatus()) || "PARTIALLY_CLOSED".equals(trade.getStatus())) 
                    && trade.getTotalProfit() != null
                    && !trade.getExits().isEmpty()
                )
                .toList();

            logger.info("Found {} closed trades", closedTrades.size());

            // Filter and transform trades into chart data
            Map<String, Double> dailyPnL = new HashMap<>();

            for (Trade trade : closedTrades) {
                try {
                    Exit lastExit = trade.getExits().get(trade.getExits().size() - 1);
                    String exitDateStr = lastExit.getExitDate();
                    logger.debug("Processing trade {} with exit date {}", trade.getId(), exitDateStr);

                    LocalDate exitDate = LocalDate.parse(exitDateStr.split("T")[0]);
                    
                    if (!exitDate.isBefore(startDate) && !exitDate.isAfter(endDate)) {
                        String dateKey = exitDate.toString();
                        dailyPnL.merge(dateKey, trade.getTotalProfit(), Double::sum);
                        logger.debug("Added P/L {} for date {}", trade.getTotalProfit(), dateKey);
                    } else {
                        logger.debug("Trade date {} outside range {} to {}", exitDate, startDate, endDate);
                    }
                } catch (Exception e) {
                    logger.warn("Error processing trade {}: {}", trade.getId(), e.getMessage());
                }
            }

            List<Map<String, Object>> chartData = dailyPnL.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", entry.getKey());
                    point.put("pnl", entry.getValue());
                    return point;
                })
                .sorted((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")))
                .collect(Collectors.toList());

            logger.info("Generated {} chart data points", chartData.size());
            logger.debug("Chart data: {}", chartData);

            return ResponseEntity.ok(chartData);
        } catch (Exception e) {
            logger.error("Error fetching P/L chart data", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "An unexpected error occurred",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Retrieves profit/loss data grouped by strategy.
     * @param token Authorization token for user identification
     * @return ResponseEntity containing strategy-wise P/L data
     */
    @GetMapping("/strategy-pnl")
    public ResponseEntity<?> getStrategyPnL(@RequestHeader("Authorization") String token) {
        try {
            String userId = extractUserIdFromToken(token);
            logger.info("Fetching strategy P/L for userId: {}", userId);
            
            List<Trade> allTrades = tradeService.getTradesByUserId(userId);
            
            // Group trades by strategy and calculate total P/L
            Map<String, Double> strategyPnL = allTrades.stream()
                .filter(trade -> 
                    ("CLOSED".equals(trade.getStatus()) || "PARTIALLY_CLOSED".equals(trade.getStatus())) 
                    && trade.getTotalProfit() != null
                    && trade.getStrategy() != null
                    && !trade.getStrategy().isEmpty()
                )
                .collect(Collectors.groupingBy(
                    Trade::getStrategy,
                    Collectors.summingDouble(Trade::getTotalProfit)
                ));

            // Calculate additional metrics for each strategy
            List<Map<String, Object>> strategyMetrics = strategyPnL.entrySet().stream()
                .map(entry -> {
                    String strategy = entry.getKey();
                    double totalPnL = entry.getValue();
                    
                    // Count trades for this strategy
                    long totalTrades = allTrades.stream()
                        .filter(t -> strategy.equals(t.getStrategy()) 
                            && ("CLOSED".equals(t.getStatus()) || "PARTIALLY_CLOSED".equals(t.getStatus())))
                        .count();
                    
                    // Calculate win ratio
                    long winningTrades = allTrades.stream()
                        .filter(t -> strategy.equals(t.getStrategy()) 
                            && ("CLOSED".equals(t.getStatus()) || "PARTIALLY_CLOSED".equals(t.getStatus()))
                            && t.getTotalProfit() != null 
                            && t.getTotalProfit() > 0)
                        .count();
                    
                    double winRatio = totalTrades > 0 ? (double) winningTrades / totalTrades : 0;

                    Map<String, Object> metrics = new HashMap<>();
                    metrics.put("strategy", strategy);
                    metrics.put("totalPnL", totalPnL);
                    metrics.put("tradeCount", totalTrades);
                    metrics.put("winRatio", winRatio);
                    metrics.put("avgPnL", totalTrades > 0 ? totalPnL / totalTrades : 0);
                    
                    return metrics;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("totalPnL"), (Double) a.get("totalPnL")))
                .collect(Collectors.toList());

            logger.info("Strategy P/L calculated successfully: {}", strategyMetrics);
            return ResponseEntity.ok(strategyMetrics);
        } catch (Exception e) {
            logger.error("Error calculating strategy P/L", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "An unexpected error occurred",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Extracts user ID from the authorization token.
     * @param token The authorization token
     * @return The extracted user ID
     */
    private String extractUserIdFromToken(String token) {
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return userService.getUserIdFromToken(token);
    }

    /**
     * Retrieves yearly profit/loss total.
     * @param token Authorization token for user identification
     * @param year The year for which to calculate total P/L
     * @return ResponseEntity containing yearly P/L data
     */
    @GetMapping("/yearly-pnl")
    public ResponseEntity<?> getYearlyPnL(
            @RequestHeader("Authorization") String token,
            @RequestParam Integer year) {
        try {
            String userId = extractUserIdFromToken(token);
            logger.info("Fetching yearly P/L for userId: {} and year: {}", userId, year);
            
            List<Trade> allTrades = tradeService.getTradesByUserId(userId);
            
            // Calculate total P/L only for trades that were closed in the specified year
            double yearlyTotal = allTrades.stream()
                .filter(trade -> 
                    ("CLOSED".equals(trade.getStatus()) || "PARTIALLY_CLOSED".equals(trade.getStatus())) 
                    && trade.getTotalProfit() != null
                    && !trade.getExits().isEmpty()
                    && trade.getExits().stream()
                        .anyMatch(exit -> {
                            String exitYear = exit.getExitDate().substring(0, 4);
                            return exitYear.equals(String.valueOf(year));
                        })
                )
                .mapToDouble(trade -> {
                    // Only count profits from exits in the specified year
                    return trade.getExits().stream()
                        .filter(exit -> exit.getExitDate().startsWith(String.valueOf(year)))
                        .mapToDouble(Exit::getProfit)
                        .sum();
                })
                .sum();

            Map<String, Object> response = new HashMap<>();
            response.put("year", year);
            response.put("totalPnL", yearlyTotal);

            logger.info("Yearly P/L calculated successfully for {}: {}", year, yearlyTotal);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error calculating yearly P/L", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "An unexpected error occurred",
                "message", e.getMessage()
            ));
        }
    }

    // Update the profit metrics endpoint
    @GetMapping("/profit-metrics")
    public ResponseEntity<Map<String, Object>> getProfitMetrics(@RequestHeader("Authorization") String token) {
        try {
            String userId = extractUserIdFromToken(token);
            List<Trade> userTrades = tradeService.getTradesByUserId(userId);
            
            // Filter only closed trades
            List<Trade> closedTrades = userTrades.stream()
                .filter(t -> "CLOSED".equals(t.getStatus()) || "PARTIALLY_CLOSED".equals(t.getStatus()))
                .toList();
            
            // Calculate gross profits and losses
            double grossProfits = closedTrades.stream()
                .filter(t -> t.getTotalProfit() != null && t.getTotalProfit() > 0)
                .mapToDouble(Trade::getTotalProfit)
                .sum();
                
            double grossLosses = Math.abs(closedTrades.stream()
                .filter(t -> t.getTotalProfit() != null && t.getTotalProfit() < 0)
                .mapToDouble(Trade::getTotalProfit)
                .sum());
                
            // Calculate profit factor
            double profitFactor = grossLosses > 0 ? grossProfits / grossLosses : grossProfits;

            // Calculate Maximum Drawdown
            double maxDrawdown = 0.0;
            double peak = 0.0;
            double currentEquity = 0.0;

            // Sort trades by date
            List<Trade> sortedTrades = closedTrades.stream()
                .filter(t -> t.getTotalProfit() != null && !t.getExits().isEmpty())
                .sorted((t1, t2) -> {
                    String date1 = t1.getExits().get(t1.getExits().size() - 1).getExitDate();
                    String date2 = t2.getExits().get(t2.getExits().size() - 1).getExitDate();
                    return date1.compareTo(date2);
                })
                .toList();

            // Calculate running equity and track maximum drawdown
            for (Trade trade : sortedTrades) {
                currentEquity += trade.getTotalProfit();
                if (currentEquity > peak) {
                    peak = currentEquity;
                }
                double drawdown = peak - currentEquity;
                if (drawdown > maxDrawdown) {
                    maxDrawdown = drawdown;
                }
            }
            
            // Calculate Trading Consistency Score (0-100)
            double winRatio = closedTrades.stream()
                .filter(t -> t.getTotalProfit() != null && t.getTotalProfit() > 0)
                .count() / (double) closedTrades.size();
                
            double avgProfit = closedTrades.stream()
                .filter(t -> t.getTotalProfit() != null)
                .mapToDouble(Trade::getTotalProfit)
                .average()
                .orElse(0.0);
                
            // Calculate trade frequency score (more regular trading = higher score)
            double tradeFrequencyScore = Math.min(closedTrades.size() / 20.0, 1.0); // Normalize to max 1.0
            
            // Combine factors for consistency score
            double consistencyScore = (
                (winRatio * 40) +                    // Win ratio contributes 40%
                (profitFactor / 3.0 * 30) +          // Profit factor contributes 30% (normalized to max ~3.0)
                (tradeFrequencyScore * 30)           // Trade frequency contributes 30%
            );
            
            // Ensure score is between 0 and 100
            consistencyScore = Math.min(Math.max(consistencyScore, 0), 100);
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("profitFactor", profitFactor);
            metrics.put("grossProfits", grossProfits);
            metrics.put("grossLosses", grossLosses);
            metrics.put("maxDrawdown", maxDrawdown);
            metrics.put("consistencyScore", consistencyScore);
            
            logger.info("Profit metrics calculated - Factor: {}, Profits: {}, Losses: {}, MaxDrawdown: {}, Consistency Score: {}", 
                       profitFactor, grossProfits, grossLosses, maxDrawdown, consistencyScore);
            
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error calculating profit metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/duration-metrics")
    public ResponseEntity<Map<String, Object>> getTradeDurationMetrics(@RequestHeader("Authorization") String token) {
        try {
            String userId = extractUserIdFromToken(token);
            List<Trade> userTrades = tradeService.getTradesByUserId(userId);
            
            // Filter closed trades
            List<Trade> closedTrades = userTrades.stream()
                .filter(t -> ("CLOSED".equals(t.getStatus()) || "PARTIALLY_CLOSED".equals(t.getStatus()))
                    && t.getEntryDate() != null 
                    && !t.getExits().isEmpty())
                .toList();

            // Calculate duration for each trade
            List<Map<String, Object>> tradeDurations = closedTrades.stream()
                .map(trade -> {
                    LocalDate entryDate = LocalDate.parse(trade.getEntryDate().split("T")[0]);
                    LocalDate exitDate = LocalDate.parse(
                        trade.getExits().get(trade.getExits().size() - 1)
                            .getExitDate().split("T")[0]
                    );
                    long daysHeld = ChronoUnit.DAYS.between(entryDate, exitDate);

                    Map<String, Object> tradeInfo = new HashMap<>();
                    tradeInfo.put("tradeId", trade.getId());
                    tradeInfo.put("symbol", trade.getSymbol());
                    tradeInfo.put("daysHeld", daysHeld);
                    tradeInfo.put("entryDate", entryDate);
                    tradeInfo.put("exitDate", exitDate);
                    return tradeInfo;
                })
                .collect(Collectors.toList());

            // Calculate average duration
            double averageDuration = tradeDurations.stream()
                .mapToLong(trade -> (Long) trade.get("daysHeld"))
                .average()
                .orElse(0.0);

            Map<String, Object> response = new HashMap<>();
            response.put("averageDuration", averageDuration);
            response.put("tradeDurations", tradeDurations);
            
            logger.info("Trade duration metrics calculated - Average: {} days", averageDuration);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error calculating trade duration metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/import-csv")
    public ResponseEntity<?> importCsvTrades(
            @RequestBody List<CsvTradeRecord> csvRecords,
            @RequestHeader("Authorization") String token) {
        try {
            String userId = extractUserIdFromToken(token);
            logger.info("Processing CSV import for userId: {} with {} records", userId, csvRecords.size());

            List<Trade> processedTrades = csvImportService.processCsvRecords(csvRecords, userId);
            
            // Save all processed trades
            List<Trade> savedTrades = new ArrayList<>();
            for (Trade trade : processedTrades) {
                savedTrades.add(tradeService.save(trade));
            }

            logger.info("Successfully imported {} trades", savedTrades.size());
            return ResponseEntity.ok(Map.of(
                "message", "Successfully imported trades",
                "count", savedTrades.size(),
                "trades", savedTrades
            ));
        } catch (Exception e) {
            logger.error("Error importing CSV trades", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to import trades",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importTrades(@RequestBody Map<String, String> payload) {
        try {
            String content = payload.get("content");
            String userId = payload.get("userId");
            String fileName = payload.get("fileName");
            
            logger.info("Processing import for file: {}", fileName);
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity
                    .badRequest()
                    .body(new ApiErrorResponse("No content provided"));
            }
            
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity
                    .badRequest()
                    .body(new ApiErrorResponse("User ID is required"));
            }

            try {
                // Parse and process the trades
                List<BrokerTradeRecord> brokerRecords = csvImportService.detectAndParseBrokerFormat(content);
                List<Trade> importedTrades = csvImportService.processBrokerRecords(brokerRecords, userId);
                
                // Save each trade to the database and collect results
                List<Trade> savedTrades = new ArrayList<>();
                for (Trade trade : importedTrades) {
                    try {
                        // Ensure trade has userId
                        trade.setUserId(userId);
                        
                        // Save using tradeService
                        Trade savedTrade = tradeService.addTrade(trade);
                        if (savedTrade != null) {
                            savedTrades.add(savedTrade);
                            logger.debug("Successfully saved trade: {} for symbol: {}", 
                                savedTrade.getId(), savedTrade.getSymbol());
                        } else {
                            logger.error("Failed to save trade for symbol: {}", trade.getSymbol());
                        }
                    } catch (Exception e) {
                        logger.error("Error saving trade for symbol {}: {}", 
                            trade.getSymbol(), e.getMessage());
                    }
                }
                
                logger.info("Successfully processed and saved {} out of {} trades from file: {}", 
                    savedTrades.size(), importedTrades.size(), fileName);
                
                if (savedTrades.isEmpty()) {
                    throw new RuntimeException("No trades were saved to the database");
                }
                
                // Return the saved trades
                return ResponseEntity.ok(Map.of(
                    "message", String.format("Successfully imported %d trades", savedTrades.size()),
                    "count", savedTrades.size(),
                    "trades", savedTrades
                ));
                    
            } catch (CsvImportException e) {
                logger.error("CSV import error for file {}: {}", fileName, e.getMessage());
                return ResponseEntity
                    .badRequest()
                    .body(new ApiErrorResponse(e.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Unexpected error during import: ", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("Failed to process file: " + e.getMessage()));
        }
    }

    @PostMapping("/import/debug")
    public ResponseEntity<?> debugCsvImport(@RequestBody Map<String, String> payload) {
        try {
            String content = payload.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiErrorResponse("No content provided"));
            }

            // Get first few lines and headers
            String[] lines = content.split("\n");
            String firstLine = lines[0];
            String delimiter = firstLine.contains("\t") ? "TAB" : "COMMA";
            String[] headers = firstLine.split(delimiter.equals("TAB") ? "\t" : ",");

            Map<String, Object> debug = new HashMap<>();
            debug.put("delimiter", delimiter);
            debug.put("headers", Arrays.asList(headers));
            debug.put("sampleLines", Arrays.asList(Arrays.copyOfRange(lines, 1, Math.min(lines.length, 4))));

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            logger.error("Error in debug endpoint: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("Debug error: " + e.getMessage()));
        }
    }
}

/**
 * Request object for trade exit operations.
 * Contains necessary information for recording a trade exit.
 */
class ExitTradeRequest {
    private String exitDate;
    private double exitPrice;
    private int exitQuantity;

    // Getters and setters
    public String getExitDate() { return exitDate; }
    public void setExitDate(String exitDate) { this.exitDate = exitDate; }
    public double getExitPrice() { return exitPrice; }
    public void setExitPrice(double exitPrice) { this.exitPrice = exitPrice; }
    public int getExitQuantity() { return exitQuantity; }
    public void setExitQuantity(int exitQuantity) { this.exitQuantity = exitQuantity; }
}

class ApiErrorResponse {
    private String message;

    public ApiErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
