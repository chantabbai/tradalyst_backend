package com.tradepro.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.ArrayList;

@Document(collection = "trades")
public class Trade {
    @Id
    private String id;
    private String userId;
    private String entryDate;
    private String exitDate;  // This should be null for open trades
    private String symbol;
    private String action;
    private int quantity;
    private double price;
    private String type;
    private String optionType;
    private String strategy;
    private String notes;
    private List<Exit> exits = new ArrayList<>();
    private String status = "OPEN";
    private Double totalProfit;
    private Double totalProfitPercentage;
    private int remainingQuantity;
    private String fullSymbol;
    private Double strikePrice;
    private String expirationDate;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEntryDate() { return entryDate; }
    public void setEntryDate(String entryDate) { this.entryDate = entryDate; }
    public String getExitDate() { return exitDate; }
    public void setExitDate(String exitDate) { this.exitDate = exitDate; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getOptionType() { return optionType; }
    public void setOptionType(String optionType) { this.optionType = optionType; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<Exit> getExits() { return exits; }
    public void setExits(List<Exit> exits) { this.exits = exits; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotalProfit() { return totalProfit; }
    public void setTotalProfit(Double totalProfit) { this.totalProfit = totalProfit; }
    public Double getTotalProfitPercentage() { return totalProfitPercentage; }
    public void setTotalProfitPercentage(Double totalProfitPercentage) { this.totalProfitPercentage = totalProfitPercentage; }
    public int getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(int remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public String getFullSymbol() { return fullSymbol; }
    public void setFullSymbol(String fullSymbol) { this.fullSymbol = fullSymbol; }
    public Double getStrikePrice() { return strikePrice; }
    public void setStrikePrice(Double strikePrice) { this.strikePrice = strikePrice; }
    public String getExpirationDate() { return expirationDate; }
    public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }

    // Add this method to Trade.java
    public void addToQuantity(int additionalQuantity) {
        this.quantity += additionalQuantity;
        this.remainingQuantity += additionalQuantity;
    }
}
