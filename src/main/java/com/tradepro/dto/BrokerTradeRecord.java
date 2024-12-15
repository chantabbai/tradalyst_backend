package com.tradepro.dto;

import java.time.LocalDateTime;

public class BrokerTradeRecord {
    private String symbol;
    private String description;
    private LocalDateTime tradeDate;
    private String action;
    private String openClose;
    private Double quantity;
    private Double price;
    private Double commission;
    private Double fees;
    private Double amount;
    private String type;
    private String optionType;
    private LocalDateTime expirationDate;
    private Double strikePrice;
    private String broker;

    // Add getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDateTime tradeDate) { this.tradeDate = tradeDate; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getOpenClose() { return openClose; }
    public void setOpenClose(String openClose) { this.openClose = openClose; }
    
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public Double getCommission() { return commission; }
    public void setCommission(Double commission) { this.commission = commission; }
    
    public Double getFees() { return fees; }
    public void setFees(Double fees) { this.fees = fees; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getOptionType() { return optionType; }
    public void setOptionType(String optionType) { this.optionType = optionType; }
    
    public LocalDateTime getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDateTime expirationDate) { this.expirationDate = expirationDate; }
    
    public Double getStrikePrice() { return strikePrice; }
    public void setStrikePrice(Double strikePrice) { this.strikePrice = strikePrice; }
    
    public String getBroker() { return broker; }
    public void setBroker(String broker) { this.broker = broker; }
} 