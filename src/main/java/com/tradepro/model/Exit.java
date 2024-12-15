package com.tradepro.model;

public class Exit {
    private String exitDate;
    private double exitPrice;
    private int exitQuantity;
    private double profit;
    private double profitPercentage;

    // Constructors
    public Exit() {}

    public Exit(String exitDate, double exitPrice, int exitQuantity, double profit, double profitPercentage) {
        this.exitDate = exitDate;
        this.exitPrice = exitPrice;
        this.exitQuantity = exitQuantity;
        this.profit = profit;
        this.profitPercentage = profitPercentage;
    }

    // Getters and setters
    public String getExitDate() { return exitDate; }
    public void setExitDate(String exitDate) { this.exitDate = exitDate; }
    public double getExitPrice() { return exitPrice; }
    public void setExitPrice(double exitPrice) { this.exitPrice = exitPrice; }
    public int getExitQuantity() { return exitQuantity; }
    public void setExitQuantity(int exitQuantity) { this.exitQuantity = exitQuantity; }
    public double getProfit() { return profit; }
    public void setProfit(double profit) { this.profit = profit; }
    public double getProfitPercentage() { return profitPercentage; }
    public void setProfitPercentage(double profitPercentage) { this.profitPercentage = profitPercentage; }
}
