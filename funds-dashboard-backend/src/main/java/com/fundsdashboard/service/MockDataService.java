package com.fundsdashboard.service;

import com.fundsdashboard.model.Fund;
import com.fundsdashboard.model.FundStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MockDataService {

    private final List<Fund> funds = new ArrayList<>();

    @PostConstruct
    public void initFunds() {
        String[] assetClasses = {"Equity", "Fixed Income", "Multi-Asset", "Alternatives", "Money Market"};
        String[] managers = {"Alice Chen", "Bob Smith", "Clara Davis", "David Lee", "Eva Martinez"};
        String[] benchmarks = {"MSCI World", "Bloomberg Agg", "S&P 500", "FTSE 100", "Eurostoxx 50"};
        String[] domiciles = {"Luxembourg", "Ireland", "Cayman Islands", "UK", "France"};
        String[] frequencies = {"Monthly", "Quarterly", "Annual", "Semi-Annual"};
        String[] fundTypes = {"UCITS", "AIF", "ETF", "Hedge Fund", "Mutual Fund"};
        String[] legalStructures = {"SICAV", "OEIC", "LP", "Trust", "SCA"};
        FundStatus[] statuses = {FundStatus.DRAFT, FundStatus.SUBMITTED, FundStatus.APPROVED, FundStatus.REJECTED};

        for (int i = 1; i <= 25; i++) {
            Fund f = new Fund();
            f.setId(String.format("FUND-%03d", i));
            f.setFundName("Fund " + String.format("%03d", i));
            f.setIsin(String.format("GB%010d", i));
            f.setCurrency(i % 3 == 0 ? "EUR" : i % 3 == 1 ? "USD" : "GBP");
            f.setNav(BigDecimal.valueOf(100 + i * 7.5));
            f.setStatus(statuses[(i - 1) % 4]);
            f.setAssetClass(assetClasses[(i - 1) % 5]);
            f.setFundManager(managers[(i - 1) % 5]);
            f.setInceptionDate(LocalDate.of(2015, 1, 1).plusMonths(i * 4L));
            f.setBenchmark(benchmarks[(i - 1) % 5]);
            f.setDomicile(domiciles[(i - 1) % 5]);
            f.setRiskRating((i % 7) + 1);
            f.setAum(BigDecimal.valueOf(50_000_000L + i * 10_000_000L));
            f.setMinInvestment(BigDecimal.valueOf(1000 + i * 500L));
            f.setManagementFee(BigDecimal.valueOf(0.5 + (i % 5) * 0.25));
            f.setPerformanceFee(BigDecimal.valueOf(i % 2 == 0 ? 20 : 0));
            f.setDistributionFrequency(frequencies[i % 4]);
            f.setFundType(fundTypes[(i - 1) % 5]);
            f.setLegalStructure(legalStructures[(i - 1) % 5]);
            f.setBloombergTicker("FUND" + i + " LN");
            f.setBloombergId("BBG00" + String.format("%07d", i));
            funds.add(f);
        }
    }

    public List<Fund> getAll() {
        return funds;
    }

    public Optional<Fund> findById(String id) {
        return funds.stream().filter(f -> f.getId().equals(id)).findFirst();
    }

    public boolean update(Fund updated) {
        for (int i = 0; i < funds.size(); i++) {
            if (funds.get(i).getId().equals(updated.getId())) {
                funds.set(i, updated);
                return true;
            }
        }
        return false;
    }
}
