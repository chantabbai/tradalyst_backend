package com.tradepro.service;

import com.tradepro.model.Trade;
import com.tradepro.model.Exit;
import com.tradepro.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class TradeService {

    private static final Logger logger = LoggerFactory.getLogger(TradeService.class);

    @Autowired
    private TradeRepository tradeRepository;

    public Trade addTrade(Trade trade) {
        try {
            logger.debug("Saving trade: {}", trade);
            return tradeRepository.save(trade);
        } catch (Exception e) {
            logger.error("Error saving trade: {}", e.getMessage());
            throw new RuntimeException("Failed to save trade: " + e.getMessage());
        }
    }

    public List<Trade> getTradesByUserId(String userId) {
        return tradeRepository.findByUserId(userId);
    }

    public Trade updateTrade(String id, Trade updatedTrade) {
        Trade existingTrade = tradeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Trade not found"));
        
        // Update fields
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

        return tradeRepository.save(existingTrade);
    }

    public Trade exitTrade(String id, String exitDate, double exitPrice, int exitQuantity) {
        logger.info("Exiting trade with id: {}, exitDate: {}, exitPrice: {}, exitQuantity: {}", 
                    id, exitDate, exitPrice, exitQuantity);
        Trade trade = tradeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Trade not found with id: " + id));
        
        logger.info("Found trade: {}", trade);
        
        int quantity = trade.getQuantity();
        logger.info("Remaining quantity: {}", quantity);
        if (exitQuantity > quantity) {
            logger.error("Exit quantity {} exceeds remaining quantity {}", exitQuantity, quantity);
            throw new IllegalArgumentException("Exit quantity cannot exceed remaining quantity");
        }
        
        Exit exit = new Exit();
        exit.setExitDate(exitDate);
        exit.setExitPrice(exitPrice);
        exit.setExitQuantity(exitQuantity);
        
        double entryPrice = trade.getPrice();
        double profit = (exitPrice - entryPrice) * exitQuantity;
        double profitPercentage = ((exitPrice - entryPrice) / entryPrice) * 100;
        
        exit.setProfit(profit);
        exit.setProfitPercentage(profitPercentage);
        
        trade.getExits().add(exit);
        
        updateTradeStatus(trade);
        
        logger.info("Updated trade before saving: {}", trade);
        Trade savedTrade = tradeRepository.save(trade);
        logger.info("Trade saved after exit: {}", savedTrade);
        
        return savedTrade;
    }
    
    private void updateTradeStatus(Trade trade) {
        logger.info("Updating trade status for trade: {}", trade);
        int totalExitQuantity = trade.getExits().stream().mapToInt(Exit::getExitQuantity).sum();
        double totalProfit = trade.getExits().stream().mapToDouble(Exit::getProfit).sum();
        
        trade.setTotalProfit(totalProfit);
        trade.setTotalProfitPercentage((totalProfit / (trade.getPrice() * trade.getQuantity())) * 100);
        
        int remainingQuantity = trade.getQuantity() - totalExitQuantity;
        trade.setRemainingQuantity(remainingQuantity);
        
        if (remainingQuantity == 0) {
            trade.setStatus("CLOSED");
        } else if (remainingQuantity < trade.getQuantity()) {
            trade.setStatus("PARTIALLY_CLOSED");
        } else {
            trade.setStatus("OPEN");
        }
        logger.info("Updated trade status: {}", trade.getStatus());
    }

    public Trade findById(String id) {
        return tradeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Trade not found with id: " + id));
    }

    public Trade save(Trade trade) {
        return tradeRepository.save(trade);
    }

    public void deleteTrade(String id) {
        logger.info("Deleting trade with id: {}", id);
        Trade trade = findById(id);
        tradeRepository.delete(trade);
        logger.info("Trade deleted successfully");
    }
}
