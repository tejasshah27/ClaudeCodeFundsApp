package com.fundsdashboard.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class Fund {
    private String id;
    private String fundName;
    private String isin;
    private String currency;
    private BigDecimal nav;
    private FundStatus status;
    private String assetClass;
    private String fundManager;
    private LocalDate inceptionDate;
    private String benchmark;
    private String domicile;
    private Integer riskRating;
    private BigDecimal aum;
    private BigDecimal minInvestment;
    private BigDecimal managementFee;
    private BigDecimal performanceFee;
    private String distributionFrequency;
    private String fundType;
    private String legalStructure;
    private String bloombergTicker;
    private String bloombergId;
}
