package com.fundsdashboard.dto;

import com.fundsdashboard.model.Fund;
import com.fundsdashboard.model.FundStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundDetailDto {
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

    public static FundDetailDto from(Fund f) {
        FundDetailDto dto = new FundDetailDto();
        dto.id = f.getId();
        dto.fundName = f.getFundName();
        dto.isin = f.getIsin();
        dto.currency = f.getCurrency();
        dto.nav = f.getNav();
        dto.status = f.getStatus();
        dto.assetClass = f.getAssetClass();
        dto.fundManager = f.getFundManager();
        dto.inceptionDate = f.getInceptionDate();
        dto.benchmark = f.getBenchmark();
        dto.domicile = f.getDomicile();
        dto.riskRating = f.getRiskRating();
        dto.aum = f.getAum();
        dto.minInvestment = f.getMinInvestment();
        dto.managementFee = f.getManagementFee();
        dto.performanceFee = f.getPerformanceFee();
        dto.distributionFrequency = f.getDistributionFrequency();
        dto.fundType = f.getFundType();
        dto.legalStructure = f.getLegalStructure();
        dto.bloombergTicker = f.getBloombergTicker();
        dto.bloombergId = f.getBloombergId();
        return dto;
    }
}
