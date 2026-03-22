package com.fundsdashboard.service;

import com.fundsdashboard.dto.FundDetailDto;
import com.fundsdashboard.dto.FundSummaryDto;
import com.fundsdashboard.dto.FundUpdateRequest;
import com.fundsdashboard.model.Fund;
import com.fundsdashboard.model.FundStatus;
import com.fundsdashboard.model.UserRole;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FundService {

    private final MockDataService mockDataService;

    public FundService(MockDataService mockDataService) {
        this.mockDataService = mockDataService;
    }

    public List<?> getFundsForRole(UserRole role) {
        List<Fund> all = mockDataService.getAll();
        if (role == UserRole.APPROVER) {
            return all.stream().map(FundDetailDto::from).toList();
        }
        return all.stream().map(FundSummaryDto::from).toList();
    }

    public Optional<FundDetailDto> getFundDetail(String id) {
        return mockDataService.findById(id).map(FundDetailDto::from);
    }

    public boolean saveOrSubmit(String id, FundUpdateRequest req, UserRole role) {
        if (role != UserRole.EDITOR) return false;
        Optional<Fund> opt = mockDataService.findById(id);
        if (opt.isEmpty()) return false;
        Fund f = opt.get();
        applyUpdates(f, req);
        if ("SUBMIT".equalsIgnoreCase(req.getAction())) {
            f.setStatus(FundStatus.SUBMITTED);
        }
        return mockDataService.update(f);
    }

    public boolean approve(String id, UserRole role) {
        if (role != UserRole.APPROVER) return false;
        Optional<Fund> opt = mockDataService.findById(id);
        if (opt.isEmpty()) return false;
        Fund f = opt.get();
        f.setStatus(FundStatus.APPROVED);
        return mockDataService.update(f);
    }

    public boolean reject(String id, UserRole role) {
        if (role != UserRole.APPROVER) return false;
        Optional<Fund> opt = mockDataService.findById(id);
        if (opt.isEmpty()) return false;
        Fund f = opt.get();
        f.setStatus(FundStatus.REJECTED);
        return mockDataService.update(f);
    }

    private void applyUpdates(Fund f, FundUpdateRequest req) {
        if (req.getFundName() != null) f.setFundName(req.getFundName());
        if (req.getIsin() != null) f.setIsin(req.getIsin());
        if (req.getCurrency() != null) f.setCurrency(req.getCurrency());
        if (req.getNav() != null) f.setNav(req.getNav());
        if (req.getAssetClass() != null) f.setAssetClass(req.getAssetClass());
        if (req.getFundManager() != null) f.setFundManager(req.getFundManager());
        if (req.getInceptionDate() != null) f.setInceptionDate(req.getInceptionDate());
        if (req.getBenchmark() != null) f.setBenchmark(req.getBenchmark());
        if (req.getDomicile() != null) f.setDomicile(req.getDomicile());
        if (req.getRiskRating() != null) f.setRiskRating(req.getRiskRating());
        if (req.getAum() != null) f.setAum(req.getAum());
        if (req.getMinInvestment() != null) f.setMinInvestment(req.getMinInvestment());
        if (req.getManagementFee() != null) f.setManagementFee(req.getManagementFee());
        if (req.getPerformanceFee() != null) f.setPerformanceFee(req.getPerformanceFee());
        if (req.getDistributionFrequency() != null) f.setDistributionFrequency(req.getDistributionFrequency());
        if (req.getFundType() != null) f.setFundType(req.getFundType());
        if (req.getLegalStructure() != null) f.setLegalStructure(req.getLegalStructure());
        if (req.getBloombergTicker() != null) f.setBloombergTicker(req.getBloombergTicker());
        if (req.getBloombergId() != null) f.setBloombergId(req.getBloombergId());
    }
}
