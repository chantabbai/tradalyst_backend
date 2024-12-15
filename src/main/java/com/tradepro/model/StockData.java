package com.tradepro.model;

import lombok.Data;

@Data
public class StockData {
    private String symbol;
    private String name;
    private Double price;
    private Double change;
    private Double changePercent;
    private Long volume;
    private Double previousClose;
    private Double open;
    private Double high;
    private Double low;
    private Double marketCap;
    private Double peRatio;
    private Double dividendYield;
    private Double beta;
    private Double fiftyTwoWeekHigh;
    private Double fiftyTwoWeekLow;
} 