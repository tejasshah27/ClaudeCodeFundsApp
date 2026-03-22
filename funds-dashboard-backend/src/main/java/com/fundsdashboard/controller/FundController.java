package com.fundsdashboard.controller;

import com.fundsdashboard.dto.FundDetailDto;
import com.fundsdashboard.dto.FundUpdateRequest;
import com.fundsdashboard.model.UserRole;
import com.fundsdashboard.service.FundService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/funds")
public class FundController {

    private final FundService fundService;

    public FundController(FundService fundService) {
        this.fundService = fundService;
    }

    @GetMapping
    public ResponseEntity<List<?>> getFunds() {
        UserRole role = resolveRole();
        return ResponseEntity.ok(fundService.getFundsForRole(role));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FundDetailDto> getFund(@PathVariable String id) {
        Optional<FundDetailDto> dto = fundService.getFundDetail(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFund(@PathVariable String id, @RequestBody FundUpdateRequest req) {
        UserRole role = resolveRole();
        if (role != UserRole.EDITOR) return ResponseEntity.status(403).body("Forbidden");
        boolean ok = fundService.saveOrSubmit(id, req, role);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id) {
        UserRole role = resolveRole();
        if (role != UserRole.APPROVER) return ResponseEntity.status(403).body("Forbidden");
        boolean ok = fundService.approve(id, role);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id) {
        UserRole role = resolveRole();
        if (role != UserRole.APPROVER) return ResponseEntity.status(403).body("Forbidden");
        boolean ok = fundService.reject(id, role);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    private UserRole resolveRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("ROLE_"))
            .map(a -> a.substring(5))
            .map(UserRole::valueOf)
            .findFirst()
            .orElseThrow();
    }
}
