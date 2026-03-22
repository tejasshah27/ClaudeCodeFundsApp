package com.fundsdashboard.dto;

import com.fundsdashboard.model.Fund;
import com.fundsdashboard.model.FundStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FundSummaryDto {
    private String id;
    private String fundName;
    private String isin;
    private String currency;
    private BigDecimal nav;
    private FundStatus status;

    public static FundSummaryDto from(Fund f) {
        FundSummaryDto dto = new FundSummaryDto();
        dto.id = f.getId();
        dto.fundName = f.getFundName();
        dto.isin = f.getIsin();
        dto.currency = f.getCurrency();
        dto.nav = f.getNav();
        dto.status = f.getStatus();
        return dto;
    }
}
