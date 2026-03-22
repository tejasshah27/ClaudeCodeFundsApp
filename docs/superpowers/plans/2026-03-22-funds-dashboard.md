# Funds Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a multi-role Fund Dashboard web app with an Angular frontend and Spring Boot backend, backed by in-memory mock data, supporting Editor, Approver, and Read Only roles.

**Architecture:** Spring Boot 3.5.10 (Java 21, Maven) exposes a REST API with session-based auth and role-aware DTO projection; Angular 17+ (standalone components, no Angular Material) consumes the API via an `/api/**` dev proxy and renders a fund table with a slide-in detail overlay. No database — all state lives in a `MockDataService` `ArrayList<Fund>` for the duration of the server session.

**Tech Stack:** Java 21, Spring Boot 3.5.10, Spring Security (session/CSRF-off), Lombok, JUnit 5 + MockMvc; Angular 17+ standalone, HttpClient (withCredentials), CSS custom properties, Inter font.

---

## File Map

### Backend (`funds-dashboard-backend/`)

| File | Responsibility |
|------|---------------|
| `pom.xml` | Maven deps: spring-boot-starter-web, spring-boot-starter-security, lombok, spring-boot-starter-test |
| `src/main/java/com/fundsdashboard/FundsDashboardApplication.java` | Main entry point |
| `model/Fund.java` | 20-field POJO (Lombok `@Data`) |
| `model/FundStatus.java` | Enum: DRAFT, SUBMITTED, APPROVED, REJECTED |
| `model/UserRole.java` | Enum: EDITOR, APPROVER, READ_ONLY |
| `dto/LoginRequest.java` | `{ username, password }` |
| `dto/LoginResponse.java` | `{ role }` |
| `dto/FundSummaryDto.java` | 5-field projection (name, isin, currency, nav, status) |
| `dto/FundDetailDto.java` | All 20-field projection |
| `dto/FundUpdateRequest.java` | `{ action: SAVE|SUBMIT, ...20 fields }` |
| `service/MockDataService.java` | In-memory `ArrayList<Fund>`, 25 funds seeded in `@PostConstruct` |
| `service/FundService.java` | Role-aware DTO projection, save/submit/approve/reject logic |
| `config/SecurityConfig.java` | InMemoryUserDetailsManager (3 users), STATEFUL session, CSRF off |
| `controller/AuthController.java` | POST /api/auth/login → `{ role }`, POST /api/auth/logout |
| `controller/FundController.java` | GET /api/funds, GET /api/funds/{id}, PUT /api/funds/{id}, POST /api/funds/{id}/approve|reject |
| `src/test/.../AuthControllerTest.java` | 4 integration tests: 3 valid logins + 1 invalid |
| `src/test/.../FundControllerTest.java` | 10 integration tests: column counts, actions, 403 checks |

### Frontend (`funds-dashboard-frontend/`)

| File | Responsibility |
|------|---------------|
| `proxy.conf.json` | `/api/**` → `http://localhost:8080` |
| `angular.json` | Registers proxy, standalone config |
| `src/styles.css` | Inter font, CSS vars, global resets, smooth scroll |
| `src/app/app.config.ts` | provideRouter, provideHttpClient(withCredentials) |
| `src/app/app.routes.ts` | `/login`, `/dashboard`, `/` → redirect |
| `core/models/fund.model.ts` | `FundSummary`, `FundDetail` TS interfaces |
| `core/models/user.model.ts` | `UserRole` type |
| `core/services/auth.service.ts` | login(), logout(), role signal/BehaviorSubject |
| `core/services/fund.service.ts` | getFunds(), getFund(id), save(), submit(), approve(), reject() |
| `core/guards/auth.guard.ts` | Redirect unauthenticated → /login |
| `features/login/login.component.{ts,html,css}` | Centered card, inline error |
| `features/dashboard/dashboard.component.{ts,html,css}` | Fund table, role-aware columns, row click → overlay |
| `features/fund-detail/fund-detail.component.{ts,html,css}` | Full-screen overlay, slide-in animation, role-aware buttons |

---

## Phase 1 — Backend Foundation

---

### Task 1: Bootstrap Spring Boot project

**Files:**
- Create: `funds-dashboard-backend/pom.xml`
- Create: `funds-dashboard-backend/src/main/java/com/fundsdashboard/FundsDashboardApplication.java`
- Create: `funds-dashboard-backend/src/main/resources/application.properties`

- [ ] **Step 1: Create Maven wrapper + pom.xml**

```xml
<!-- funds-dashboard-backend/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.10</version>
    <relativePath/>
  </parent>
  <groupId>com.fundsdashboard</groupId>
  <artifactId>funds-dashboard-backend</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>funds-dashboard-backend</name>
  <properties>
    <java.version>21</java.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: Create main application class**

```java
// src/main/java/com/fundsdashboard/FundsDashboardApplication.java
package com.fundsdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FundsDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(FundsDashboardApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.properties**

```properties
# src/main/resources/application.properties
server.port=8080
spring.security.user.name=disabled
spring.security.user.password=disabled
```

- [ ] **Step 4: Verify project compiles**

```bash
cd funds-dashboard-backend
./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add funds-dashboard-backend/
git commit -m "feat: bootstrap Spring Boot project"
```

---

### Task 2: Domain models

**Files:**
- Create: `src/main/java/com/fundsdashboard/model/FundStatus.java`
- Create: `src/main/java/com/fundsdashboard/model/UserRole.java`
- Create: `src/main/java/com/fundsdashboard/model/Fund.java`

- [ ] **Step 1: Create FundStatus enum**

```java
// model/FundStatus.java
package com.fundsdashboard.model;

public enum FundStatus {
    DRAFT, SUBMITTED, APPROVED, REJECTED
}
```

- [ ] **Step 2: Create UserRole enum**

```java
// model/UserRole.java
package com.fundsdashboard.model;

public enum UserRole {
    EDITOR, APPROVER, READ_ONLY
}
```

- [ ] **Step 3: Create Fund POJO (all 20 fields)**

```java
// model/Fund.java
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
```

- [ ] **Step 4: Verify compile**

```bash
./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add funds-dashboard-backend/src/main/java/com/fundsdashboard/model/
git commit -m "feat: add Fund domain model and enums"
```

---

### Task 3: DTOs

**Files:**
- Create: `src/main/java/com/fundsdashboard/dto/LoginRequest.java`
- Create: `src/main/java/com/fundsdashboard/dto/LoginResponse.java`
- Create: `src/main/java/com/fundsdashboard/dto/FundSummaryDto.java`
- Create: `src/main/java/com/fundsdashboard/dto/FundDetailDto.java`
- Create: `src/main/java/com/fundsdashboard/dto/FundUpdateRequest.java`

- [ ] **Step 1: Create LoginRequest and LoginResponse**

```java
// dto/LoginRequest.java
package com.fundsdashboard.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
```

```java
// dto/LoginResponse.java
package com.fundsdashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String role;
}
```

- [ ] **Step 2: Create FundSummaryDto (5 fields)**

```java
// dto/FundSummaryDto.java
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
```

- [ ] **Step 3: Create FundDetailDto (all 20 fields)**

```java
// dto/FundDetailDto.java
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
```

- [ ] **Step 4: Create FundUpdateRequest**

```java
// dto/FundUpdateRequest.java
package com.fundsdashboard.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundUpdateRequest {
    private String action; // "SAVE" or "SUBMIT"
    private String fundName;
    private String isin;
    private String currency;
    private BigDecimal nav;
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
```

- [ ] **Step 5: Verify compile**

```bash
./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add funds-dashboard-backend/src/main/java/com/fundsdashboard/dto/
git commit -m "feat: add DTOs for login and fund projection"
```

---

### Task 4: MockDataService — in-memory data store

**Files:**
- Create: `src/main/java/com/fundsdashboard/service/MockDataService.java`

- [ ] **Step 1: Create MockDataService with 25 seeded funds**

```java
// service/MockDataService.java
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
```

- [ ] **Step 2: Verify compile**

```bash
./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add funds-dashboard-backend/src/main/java/com/fundsdashboard/service/MockDataService.java
git commit -m "feat: add MockDataService with 25 seeded funds"
```

---

### Task 5: FundService — business logic and DTO projection

**Files:**
- Create: `src/main/java/com/fundsdashboard/service/FundService.java`

- [ ] **Step 1: Create FundService**

```java
// service/FundService.java
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
```

- [ ] **Step 2: Verify compile**

```bash
./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add funds-dashboard-backend/src/main/java/com/fundsdashboard/service/FundService.java
git commit -m "feat: add FundService with role-aware projection and business logic"
```

---

### Task 6: SecurityConfig — session-based auth, hardcoded users

**Files:**
- Create: `src/main/java/com/fundsdashboard/config/SecurityConfig.java`

- [ ] **Step 1: Create SecurityConfig**

```java
// config/SecurityConfig.java
package com.fundsdashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("editor").password(encoder.encode("password")).roles("EDITOR").build(),
            User.withUsername("approver").password(encoder.encode("password")).roles("APPROVER").build(),
            User.withUsername("readonly").password(encoder.encode("password")).roles("READ_ONLY").build()
        );
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, UserDetailsService userDetailsService, PasswordEncoder encoder) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
            .userDetailsService(userDetailsService)
            .passwordEncoder(encoder)
            .and()
            .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add funds-dashboard-backend/src/main/java/com/fundsdashboard/config/SecurityConfig.java
git commit -m "feat: add session-based SecurityConfig with 3 hardcoded users"
```

---

### Task 7: AuthController and FundController

**Files:**
- Create: `src/main/java/com/fundsdashboard/controller/AuthController.java`
- Create: `src/main/java/com/fundsdashboard/controller/FundController.java`

- [ ] **Step 1: Create AuthController**

```java
// controller/AuthController.java
package com.fundsdashboard.controller;

import com.fundsdashboard.dto.LoginRequest;
import com.fundsdashboard.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest httpReq) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            HttpSession session = httpReq.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

            String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst().orElse("UNKNOWN");

            return ResponseEntity.ok(new LoginResponse(role));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out");
    }
}
```

- [ ] **Step 2: Create FundController**

```java
// controller/FundController.java
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
```

- [ ] **Step 4: Verify compile**

```bash
./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Smoke-test startup**

```bash
./mvnw spring-boot:run &
sleep 5
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"editor","password":"password"}'
# Expected: {"role":"EDITOR"}
kill %1
```

- [ ] **Step 6: Commit**

```bash
git add funds-dashboard-backend/src/main/java/com/fundsdashboard/controller/
git add funds-dashboard-backend/src/main/java/com/fundsdashboard/config/SecurityConfig.java
git commit -m "feat: add AuthController and FundController"
```

---

## Phase 2 — Backend Tests

---

### Task 8: AuthControllerTest — 4 integration tests

**Files:**
- Create: `src/test/java/com/fundsdashboard/controller/AuthControllerTest.java`

- [ ] **Step 1: Write AuthControllerTest**

```java
// src/test/java/com/fundsdashboard/controller/AuthControllerTest.java
package com.fundsdashboard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginAsEditor_returns200AndEditorRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"editor\",\"password\":\"password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("EDITOR"));
    }

    @Test
    void loginAsApprover_returns200AndApproverRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"approver\",\"password\":\"password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("APPROVER"));
    }

    @Test
    void loginAsReadOnly_returns200AndReadOnlyRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"readonly\",\"password\":\"password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("READ_ONLY"));
    }

    @Test
    void loginWithInvalidCredentials_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"editor\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run tests and verify they pass**

```bash
./mvnw test -Dtest=AuthControllerTest
```
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 3: Commit**

```bash
git add funds-dashboard-backend/src/test/
git commit -m "test: add AuthControllerTest — 4 integration tests"
```

---

### Task 9: FundControllerTest — 10 integration tests

**Files:**
- Create: `src/test/java/com/fundsdashboard/controller/FundControllerTest.java`

- [ ] **Step 1: Write FundControllerTest**

```java
// src/test/java/com/fundsdashboard/controller/FundControllerTest.java
package com.fundsdashboard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession loginAs(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"password\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    @Test
    void editorGetFunds_returns5FieldsPerFund() throws Exception {
        MockHttpSession session = loginAs("editor");
        mockMvc.perform(get("/api/funds").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(25))
            .andExpect(jsonPath("$[0].fundName").exists())
            .andExpect(jsonPath("$[0].assetClass").doesNotExist());
    }

    @Test
    void readonlyGetFunds_returns5FieldsPerFund() throws Exception {
        MockHttpSession session = loginAs("readonly");
        mockMvc.perform(get("/api/funds").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(25))
            .andExpect(jsonPath("$[0].assetClass").doesNotExist());
    }

    @Test
    void approverGetFunds_returnsAllFieldsPerFund() throws Exception {
        MockHttpSession session = loginAs("approver");
        mockMvc.perform(get("/api/funds").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(25))
            .andExpect(jsonPath("$[0].assetClass").exists())
            .andExpect(jsonPath("$[0].bloombergId").exists());
    }

    @Test
    void getFundById_alwaysReturnsFullDetail() throws Exception {
        MockHttpSession session = loginAs("readonly");
        mockMvc.perform(get("/api/funds/FUND-001").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("FUND-001"))
            .andExpect(jsonPath("$.assetClass").exists());
    }

    @Test
    void editorCanSaveFund() throws Exception {
        MockHttpSession session = loginAs("editor");
        mockMvc.perform(put("/api/funds/FUND-001").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SAVE\",\"fundName\":\"Updated Fund\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/funds/FUND-001").session(session))
            .andExpect(jsonPath("$.fundName").value("Updated Fund"));
    }

    @Test
    void editorCanSubmitFund_changesStatusToSubmitted() throws Exception {
        MockHttpSession session = loginAs("editor");
        mockMvc.perform(put("/api/funds/FUND-001").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SUBMIT\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/funds/FUND-001").session(session))
            .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void approverCanApproveFund() throws Exception {
        MockHttpSession session = loginAs("approver");
        mockMvc.perform(post("/api/funds/FUND-001/approve").session(session))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/funds/FUND-001").session(session))
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approverCanRejectFund() throws Exception {
        MockHttpSession session = loginAs("approver");
        mockMvc.perform(post("/api/funds/FUND-001/reject").session(session))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/funds/FUND-001").session(session))
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void readonlyCannotSaveFund_returns403() throws Exception {
        MockHttpSession session = loginAs("readonly");
        mockMvc.perform(put("/api/funds/FUND-001").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SAVE\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void editorCannotApproveFund_returns403() throws Exception {
        MockHttpSession session = loginAs("editor");
        mockMvc.perform(post("/api/funds/FUND-001/approve").session(session))
            .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run all tests and verify all pass**

```bash
./mvnw test
```
Expected: `Tests run: 14, Failures: 0, Errors: 0`

- [ ] **Step 3: Commit**

```bash
git add funds-dashboard-backend/src/test/java/com/fundsdashboard/controller/FundControllerTest.java
git commit -m "test: add FundControllerTest — 10 integration tests"
```

---

## Phase 3 — Angular Bootstrap

---

### Task 10: Create Angular project with proxy config

**Files:**
- Create: `funds-dashboard-frontend/` (via `ng new`)
- Create: `funds-dashboard-frontend/proxy.conf.json`
- Modify: `funds-dashboard-frontend/angular.json`

- [ ] **Step 1: Scaffold Angular project**

```bash
cd ..
ng new funds-dashboard-frontend --standalone --style=css --routing=false --skip-git
```

- [ ] **Step 2: Create proxy config**

```json
// funds-dashboard-frontend/proxy.conf.json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

- [ ] **Step 3: Register proxy in angular.json**

In `angular.json`, under `projects.funds-dashboard-frontend.architect.serve.options`, add:

```json
"proxyConfig": "proxy.conf.json"
```

- [ ] **Step 4: Verify project serves**

```bash
cd funds-dashboard-frontend
npm install
ng build
```
Expected: `Build at: ... ✔`

- [ ] **Step 5: Commit**

```bash
git add funds-dashboard-frontend/
git commit -m "feat: scaffold Angular project with proxy config"
```

---

### Task 11: Global styles — Inter font, CSS variables

**Files:**
- Modify: `funds-dashboard-frontend/src/styles.css`

- [ ] **Step 1: Set up global styles**

```css
/* src/styles.css */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap');

:root {
  --color-bg: #f5f7fa;
  --color-surface: #ffffff;
  --color-border: #e2e8f0;
  --color-text-primary: #1a202c;
  --color-text-secondary: #718096;
  --color-accent: #4a6cf7;
  --color-accent-hover: #3a5ce4;
  --color-danger: #e53e3e;
  --color-success: #38a169;
  --color-warning: #d69e2e;
  --color-info: #3182ce;

  --status-draft-bg: #edf2f7;
  --status-draft-text: #4a5568;
  --status-submitted-bg: #ebf8ff;
  --status-submitted-text: #2b6cb0;
  --status-approved-bg: #f0fff4;
  --status-approved-text: #276749;
  --status-rejected-bg: #fff5f5;
  --status-rejected-text: #c53030;

  --shadow-sm: 0 1px 3px rgba(0,0,0,0.08);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.12);
  --shadow-lg: 0 8px 32px rgba(0,0,0,0.16);
  --radius: 8px;
}

*, *::before, *::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html {
  scroll-behavior: smooth;
}

body {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  color: var(--color-text-primary);
  background-color: var(--color-bg);
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
}

button {
  font-family: inherit;
  cursor: pointer;
}

input {
  font-family: inherit;
}
```

- [ ] **Step 2: Verify build still succeeds**

```bash
ng build
```

- [ ] **Step 3: Commit**

```bash
git add funds-dashboard-frontend/src/styles.css
git commit -m "feat: add global styles with Inter font and CSS variables"
```

---

### Task 12: Core models, services, and auth guard

**Files:**
- Create: `src/app/core/models/fund.model.ts`
- Create: `src/app/core/models/user.model.ts`
- Create: `src/app/core/services/auth.service.ts`
- Create: `src/app/core/services/fund.service.ts`
- Create: `src/app/core/guards/auth.guard.ts`
- Modify: `src/app/app.config.ts`
- Modify: `src/app/app.routes.ts`

- [ ] **Step 1: Create TypeScript models**

```typescript
// core/models/user.model.ts
export type UserRole = 'EDITOR' | 'APPROVER' | 'READ_ONLY';
```

```typescript
// core/models/fund.model.ts
export interface FundSummary {
  id: string;
  fundName: string;
  isin: string;
  currency: string;
  nav: number;
  status: FundStatus;
}

export interface FundDetail extends FundSummary {
  assetClass: string;
  fundManager: string;
  inceptionDate: string;
  benchmark: string;
  domicile: string;
  riskRating: number;
  aum: number;
  minInvestment: number;
  managementFee: number;
  performanceFee: number;
  distributionFrequency: string;
  fundType: string;
  legalStructure: string;
  bloombergTicker: string;
  bloombergId: string;
}

export type FundStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
```

- [ ] **Step 2: Create AuthService**

```typescript
// core/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { UserRole } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private roleSubject = new BehaviorSubject<UserRole | null>(null);
  role$ = this.roleSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  get role(): UserRole | null {
    return this.roleSubject.value;
  }

  login(username: string, password: string): Observable<{ role: UserRole }> {
    return this.http.post<{ role: UserRole }>('/api/auth/login', { username, password }, { withCredentials: true })
      .pipe(tap(res => this.roleSubject.next(res.role)));
  }

  logout(): Observable<unknown> {
    return this.http.post('/api/auth/logout', {}, { withCredentials: true })
      .pipe(tap(() => {
        this.roleSubject.next(null);
        this.router.navigate(['/login']);
      }));
  }

  isAuthenticated(): boolean {
    return this.roleSubject.value !== null;
  }
}
```

- [ ] **Step 3: Create FundService**

```typescript
// core/services/fund.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FundDetail, FundSummary } from '../models/fund.model';

@Injectable({ providedIn: 'root' })
export class FundService {
  constructor(private http: HttpClient) {}

  getFunds(): Observable<(FundSummary | FundDetail)[]> {
    return this.http.get<(FundSummary | FundDetail)[]>('/api/funds', { withCredentials: true });
  }

  getFund(id: string): Observable<FundDetail> {
    return this.http.get<FundDetail>(`/api/funds/${id}`, { withCredentials: true });
  }

  save(id: string, data: Partial<FundDetail>): Observable<void> {
    return this.http.put<void>(`/api/funds/${id}`, { action: 'SAVE', ...data }, { withCredentials: true });
  }

  submit(id: string, data: Partial<FundDetail>): Observable<void> {
    return this.http.put<void>(`/api/funds/${id}`, { action: 'SUBMIT', ...data }, { withCredentials: true });
  }

  approve(id: string): Observable<void> {
    return this.http.post<void>(`/api/funds/${id}/approve`, {}, { withCredentials: true });
  }

  reject(id: string): Observable<void> {
    return this.http.post<void>(`/api/funds/${id}/reject`, {}, { withCredentials: true });
  }
}
```

- [ ] **Step 4: Create AuthGuard**

```typescript
// core/guards/auth.guard.ts
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) return true;
  return router.createUrlTree(['/login']);
};
```

- [ ] **Step 5: Configure app.config.ts**

```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withCredentials, withFetch } from '@angular/common/http';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withCredentials(), withFetch()),
  ]
};
```

- [ ] **Step 6: Set up routes placeholder**

```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/login/login.component').then(m => m.LoginComponent) },
  { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] },
];
```

- [ ] **Step 7: Verify build**

```bash
ng build
```
Expected: `Build at: ... ✔`

- [ ] **Step 8: Commit**

```bash
git add funds-dashboard-frontend/src/app/core/ funds-dashboard-frontend/src/app/app.config.ts funds-dashboard-frontend/src/app/app.routes.ts
git commit -m "feat: add core models, AuthService, FundService, AuthGuard, and routing"
```

---

## Phase 4 — Angular Features

---

### Task 13: LoginComponent

**Files:**
- Create: `src/app/features/login/login.component.ts`
- Create: `src/app/features/login/login.component.html`
- Create: `src/app/features/login/login.component.css`

- [ ] **Step 1: Create LoginComponent**

```typescript
// features/login/login.component.ts
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  username = '';
  password = '';
  error = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  onSubmit() {
    this.error = '';
    this.loading = true;
    this.auth.login(this.username, this.password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => {
        this.error = 'Invalid username or password.';
        this.loading = false;
      }
    });
  }
}
```

```html
<!-- features/login/login.component.html -->
<div class="login-page">
  <div class="login-card">
    <h1 class="login-title">Funds Dashboard</h1>
    <p class="login-subtitle">Sign in to continue</p>
    <form (ngSubmit)="onSubmit()">
      <div class="form-group">
        <label for="username">Username</label>
        <input id="username" type="text" [(ngModel)]="username" name="username" placeholder="Enter username" autocomplete="username" required />
      </div>
      <div class="form-group">
        <label for="password">Password</label>
        <input id="password" type="password" [(ngModel)]="password" name="password" placeholder="Enter password" autocomplete="current-password" required />
      </div>
      @if (error) { <p class="error-msg">{{ error }}</p> }
      <button type="submit" class="btn-primary" [disabled]="loading">
        {{ loading ? 'Signing in...' : 'Sign In' }}
      </button>
    </form>
  </div>
</div>
```

```css
/* features/login/login.component.css */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}

.login-card {
  background: var(--color-surface);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
  padding: 40px 36px;
  width: 100%;
  max-width: 400px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.login-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 28px;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.form-group input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-bg);
  transition: border-color 0.2s;
  outline: none;
}

.form-group input:focus {
  border-color: var(--color-accent);
}

.error-msg {
  color: var(--color-danger);
  font-size: 13px;
  margin-bottom: 12px;
}

.btn-primary {
  width: 100%;
  padding: 11px;
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: var(--radius);
  font-size: 14px;
  font-weight: 500;
  transition: background 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-accent-hover);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
```

- [ ] **Step 2: Verify build**

```bash
ng build
```
Expected: `Build at: ... ✔`

- [ ] **Step 3: Commit**

```bash
git add funds-dashboard-frontend/src/app/features/login/
git commit -m "feat: add LoginComponent"
```

---

### Task 14: DashboardComponent — fund table with role-aware columns

**Files:**
- Create: `src/app/features/dashboard/dashboard.component.ts`
- Create: `src/app/features/dashboard/dashboard.component.html`
- Create: `src/app/features/dashboard/dashboard.component.css`

- [ ] **Step 1: Create DashboardComponent**

```typescript
// features/dashboard/dashboard.component.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { FundService } from '../../core/services/fund.service';
import { FundDetail, FundSummary, FundStatus } from '../../core/models/fund.model';
import { UserRole } from '../../core/models/user.model';
import { DecimalPipe, DatePipe } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DecimalPipe, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  funds: (FundSummary | FundDetail)[] = [];
  selectedFundId: string | null = null;
  role: UserRole | null = null;
  loading = true;

  constructor(
    private fundService: FundService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.role = this.authService.role;
    this.loadFunds();
  }

  loadFunds() {
    this.loading = true;
    this.fundService.getFunds().subscribe({
      next: (funds) => { this.funds = funds; this.loading = false; },
      error: () => this.router.navigate(['/login'])
    });
  }

  openDetail(id: string) {
    this.selectedFundId = id;
  }

  closeDetail() {
    this.selectedFundId = null;
    this.loadFunds(); // refresh in case status changed
  }

  logout() {
    this.authService.logout().subscribe();
  }

  isApprover(): boolean { return this.role === 'APPROVER'; }

  asFundDetail(f: FundSummary | FundDetail): FundDetail {
    return f as FundDetail;
  }

  statusClass(status: FundStatus): string {
    return `badge badge-${status.toLowerCase()}`;
  }
}
```

```html
<!-- features/dashboard/dashboard.component.html -->
<div class="layout">
  <header class="navbar">
    <span class="navbar-title">Funds Dashboard</span>
    <div class="navbar-right">
      <span class="role-badge">{{ role }}</span>
      <button class="btn-logout" (click)="logout()">Logout</button>
    </div>
  </header>

  <main class="main">
    <div class="table-container">
      @if (loading) { <div class="loading">Loading funds...</div> }
      @if (!loading) { <table class="funds-table">
        <thead>
          <tr>
            <th>Fund Name</th>
            <th>ISIN</th>
            <th>Currency</th>
            <th>NAV</th>
            <th>Status</th>
            @if (isApprover()) {
              <th>Asset Class</th>
              <th>Fund Manager</th>
              <th>Inception Date</th>
              <th>Benchmark</th>
              <th>Domicile</th>
              <th>Risk Rating</th>
              <th>AUM</th>
              <th>Min Investment</th>
              <th>Mgmt Fee</th>
              <th>Perf Fee</th>
              <th>Distribution</th>
              <th>Fund Type</th>
              <th>Legal Structure</th>
              <th>Bloomberg Ticker</th>
              <th>Bloomberg ID</th>
            }
          </tr>
        </thead>
        <tbody>
          @for (fund of funds; track fund.id) {
            <tr class="fund-row" (click)="openDetail(fund.id)">
              <td>{{ fund.fundName }}</td>
              <td>{{ fund.isin }}</td>
              <td>{{ fund.currency }}</td>
              <td>{{ fund.nav | number:'1.2-2' }}</td>
              <td><span [class]="statusClass(fund.status)">{{ fund.status }}</span></td>
              @if (isApprover()) {
                <td>{{ asFundDetail(fund).assetClass }}</td>
                <td>{{ asFundDetail(fund).fundManager }}</td>
                <td>{{ asFundDetail(fund).inceptionDate | date:'mediumDate' }}</td>
                <td>{{ asFundDetail(fund).benchmark }}</td>
                <td>{{ asFundDetail(fund).domicile }}</td>
                <td>{{ asFundDetail(fund).riskRating }}</td>
                <td>{{ asFundDetail(fund).aum | number:'1.0-0' }}</td>
                <td>{{ asFundDetail(fund).minInvestment | number:'1.0-0' }}</td>
                <td>{{ asFundDetail(fund).managementFee }}%</td>
                <td>{{ asFundDetail(fund).performanceFee }}%</td>
                <td>{{ asFundDetail(fund).distributionFrequency }}</td>
                <td>{{ asFundDetail(fund).fundType }}</td>
                <td>{{ asFundDetail(fund).legalStructure }}</td>
                <td>{{ asFundDetail(fund).bloombergTicker }}</td>
                <td>{{ asFundDetail(fund).bloombergId }}</td>
              }
            </tr>
          }
        </tbody>
      </table> }
    </div>
  </main>
</div>

@if (selectedFundId) {
  <app-fund-detail
    [fundId]="selectedFundId"
    (closed)="closeDetail()">
  </app-fund-detail>
}
```

```css
/* features/dashboard/dashboard.component.css */
.layout { display: flex; flex-direction: column; min-height: 100vh; }

.navbar {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-badge {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-accent);
  background: #eef2ff;
  padding: 3px 10px;
  border-radius: 12px;
}

.btn-logout {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 5px 14px;
  transition: all 0.2s;
}

.btn-logout:hover { border-color: var(--color-accent); color: var(--color-accent); }

.main { flex: 1; padding: 24px; overflow: hidden; }

.table-container {
  background: var(--color-surface);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  overflow-x: auto;
  max-height: calc(100vh - 104px);
  overflow-y: auto;
}

.table-container::-webkit-scrollbar { width: 6px; height: 6px; }
.table-container::-webkit-scrollbar-track { background: transparent; }
.table-container::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 3px; }

.funds-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.funds-table thead {
  position: sticky;
  top: 0;
  background: var(--color-surface);
  z-index: 10;
}

.funds-table th {
  padding: 12px 16px;
  text-align: left;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-secondary);
  border-bottom: 2px solid var(--color-border);
  white-space: nowrap;
}

.funds-table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-primary);
  white-space: nowrap;
}

.fund-row { cursor: pointer; transition: background 0.15s; }
.fund-row:hover { background: #f7f9fc; }

.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.badge-draft { background: var(--status-draft-bg); color: var(--status-draft-text); }
.badge-submitted { background: var(--status-submitted-bg); color: var(--status-submitted-text); }
.badge-approved { background: var(--status-approved-bg); color: var(--status-approved-text); }
.badge-rejected { background: var(--status-rejected-bg); color: var(--status-rejected-text); }

.loading { padding: 32px; text-align: center; color: var(--color-text-secondary); }
```

- [ ] **Step 2: Verify build (FundDetailComponent doesn't exist yet — expect a template error)**

This step is expected to fail until FundDetailComponent is created in Task 15. Move on.

- [ ] **Step 3: Commit**

```bash
git add funds-dashboard-frontend/src/app/features/dashboard/
git commit -m "feat: add DashboardComponent with role-aware fund table"
```

---

### Task 15: FundDetailComponent — slide-in overlay, role-aware actions

**Files:**
- Create: `src/app/features/fund-detail/fund-detail.component.ts`
- Create: `src/app/features/fund-detail/fund-detail.component.html`
- Create: `src/app/features/fund-detail/fund-detail.component.css`

- [ ] **Step 1: Create FundDetailComponent**

```typescript
// features/fund-detail/fund-detail.component.ts
import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { FundService } from '../../core/services/fund.service';
import { AuthService } from '../../core/services/auth.service';
import { FundDetail } from '../../core/models/fund.model';
import { UserRole } from '../../core/models/user.model';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
// Note: FundService and AuthService are provided in root — no imports[] entry needed

@Component({
  selector: 'app-fund-detail',
  standalone: true,
  imports: [FormsModule, DecimalPipe, DatePipe],
  templateUrl: './fund-detail.component.html',
  styleUrl: './fund-detail.component.css'
})
export class FundDetailComponent implements OnInit {
  @Input() fundId!: string;
  @Output() closed = new EventEmitter<void>();

  fund: FundDetail | null = null;
  editableFields: Partial<FundDetail> = {};
  loading = true;
  saving = false;
  message = '';
  role: UserRole | null = null;

  constructor(private fundService: FundService, private authService: AuthService) {}

  ngOnInit() {
    this.role = this.authService.role;
    this.fundService.getFund(this.fundId).subscribe({
      next: (fund) => {
        this.fund = fund;
        this.editableFields = { ...fund };
        this.loading = false;
      }
    });
  }

  isEditor(): boolean { return this.role === 'EDITOR'; }
  isApprover(): boolean { return this.role === 'APPROVER'; }

  save() {
    this.saving = true;
    this.fundService.save(this.fundId, this.editableFields).subscribe({
      next: () => { this.message = 'Saved successfully.'; this.saving = false; },
      error: () => { this.message = 'Save failed.'; this.saving = false; }
    });
  }

  submit() {
    this.saving = true;
    this.fundService.submit(this.fundId, this.editableFields).subscribe({
      next: () => { this.message = 'Submitted for approval.'; this.saving = false; this.close(); },
      error: () => { this.message = 'Submit failed.'; this.saving = false; }
    });
  }

  approve() {
    this.saving = true;
    this.fundService.approve(this.fundId).subscribe({
      next: () => { this.message = 'Fund approved.'; this.saving = false; this.close(); },
      error: () => { this.message = 'Approve failed.'; this.saving = false; }
    });
  }

  reject() {
    this.saving = true;
    this.fundService.reject(this.fundId).subscribe({
      next: () => { this.message = 'Fund rejected.'; this.saving = false; this.close(); },
      error: () => { this.message = 'Reject failed.'; this.saving = false; }
    });
  }

  close() { this.closed.emit(); }
}
```

```html
<!-- features/fund-detail/fund-detail.component.html -->
<div class="overlay" (click)="close()">
  <div class="panel" (click)="$event.stopPropagation()">
    <div class="panel-header">
      <div>
        <h2 class="panel-title">Fund Details</h2>
        @if (fund) { <p class="panel-subtitle">{{ fund.fundName }} · {{ fund.isin }}</p> }
      </div>
      <button class="btn-icon" (click)="close()">✕</button>
    </div>

    @if (!loading && fund) {
    <div class="panel-body">
      <div class="field-grid">
        <!-- Always-visible fields — editable for EDITOR -->
        <div class="field-group">
          <label>Fund Name</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.fundName" type="text" /> }
          @else { <span>{{ fund.fundName }}</span> }
        </div>
        <div class="field-group">
          <label>ISIN</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.isin" type="text" /> }
          @else { <span>{{ fund.isin }}</span> }
        </div>
        <div class="field-group">
          <label>Currency</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.currency" type="text" /> }
          @else { <span>{{ fund.currency }}</span> }
        </div>
        <div class="field-group">
          <label>NAV</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.nav" type="number" step="0.01" /> }
          @else { <span>{{ fund.nav | number:'1.2-2' }}</span> }
        </div>
        <div class="field-group">
          <label>Status</label>
          <span class="badge badge-{{ fund.status.toLowerCase() }}">{{ fund.status }}</span>
        </div>
        <div class="field-group">
          <label>Asset Class</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.assetClass" type="text" /> }
          @else { <span>{{ fund.assetClass }}</span> }
        </div>
        <div class="field-group">
          <label>Fund Manager</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.fundManager" type="text" /> }
          @else { <span>{{ fund.fundManager }}</span> }
        </div>
        <div class="field-group">
          <label>Inception Date</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.inceptionDate" type="date" /> }
          @else { <span>{{ fund.inceptionDate | date:'mediumDate' }}</span> }
        </div>
        <div class="field-group">
          <label>Benchmark</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.benchmark" type="text" /> }
          @else { <span>{{ fund.benchmark }}</span> }
        </div>
        <div class="field-group">
          <label>Domicile</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.domicile" type="text" /> }
          @else { <span>{{ fund.domicile }}</span> }
        </div>
        <div class="field-group">
          <label>Risk Rating</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.riskRating" type="number" min="1" max="7" /> }
          @else { <span>{{ fund.riskRating }}</span> }
        </div>
        <div class="field-group">
          <label>AUM</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.aum" type="number" /> }
          @else { <span>{{ fund.aum | number:'1.0-0' }}</span> }
        </div>
        <div class="field-group">
          <label>Min Investment</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.minInvestment" type="number" /> }
          @else { <span>{{ fund.minInvestment | number:'1.0-0' }}</span> }
        </div>
        <div class="field-group">
          <label>Management Fee (%)</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.managementFee" type="number" step="0.01" /> }
          @else { <span>{{ fund.managementFee }}%</span> }
        </div>
        <div class="field-group">
          <label>Performance Fee (%)</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.performanceFee" type="number" step="0.01" /> }
          @else { <span>{{ fund.performanceFee }}%</span> }
        </div>
        <div class="field-group">
          <label>Distribution Frequency</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.distributionFrequency" type="text" /> }
          @else { <span>{{ fund.distributionFrequency }}</span> }
        </div>
        <div class="field-group">
          <label>Fund Type</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.fundType" type="text" /> }
          @else { <span>{{ fund.fundType }}</span> }
        </div>
        <div class="field-group">
          <label>Legal Structure</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.legalStructure" type="text" /> }
          @else { <span>{{ fund.legalStructure }}</span> }
        </div>
        <div class="field-group">
          <label>Bloomberg Ticker</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.bloombergTicker" type="text" /> }
          @else { <span>{{ fund.bloombergTicker }}</span> }
        </div>
        <div class="field-group">
          <label>Bloomberg ID</label>
          @if (isEditor()) { <input [(ngModel)]="editableFields.bloombergId" type="text" /> }
          @else { <span>{{ fund.bloombergId }}</span> }
        </div>
      </div>

      @if (message) { <p class="message">{{ message }}</p> }
    </div>
    }

    <div class="panel-footer">
      @if (isEditor()) {
        <button class="btn btn-primary" (click)="save()" [disabled]="saving">Save</button>
        <button class="btn btn-secondary" (click)="submit()" [disabled]="saving">Submit</button>
      }
      @if (isApprover()) {
        <button class="btn btn-success" (click)="approve()" [disabled]="saving">Approve</button>
        <button class="btn btn-danger" (click)="reject()" [disabled]="saving">Reject</button>
      }
      <button class="btn btn-outline" (click)="close()">Close</button>
    </div>
  </div>
</div>
```

```css
/* features/fund-detail/fund-detail.component.css */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 200;
  display: flex;
  justify-content: flex-end;
}

.panel {
  background: var(--color-surface);
  width: min(680px, 95vw);
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-lg);
  animation: slideIn 300ms ease-out both;
}

@keyframes slideIn {
  from { transform: translateY(40px); opacity: 0; }
  to   { transform: translateY(0);    opacity: 1; }
}

.panel-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-shrink: 0;
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.panel-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.btn-icon {
  background: none;
  border: none;
  font-size: 16px;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.15s;
}
.btn-icon:hover { background: var(--color-bg); }

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.panel-body::-webkit-scrollbar { width: 6px; }
.panel-body::-webkit-scrollbar-track { background: transparent; }
.panel-body::-webkit-scrollbar-thumb { background: var(--color-border); border-radius: 3px; }

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 24px;
}

.field-group label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--color-text-secondary);
  margin-bottom: 5px;
}

.field-group span {
  display: block;
  font-size: 14px;
  color: var(--color-text-primary);
}

.field-group input {
  width: 100%;
  padding: 7px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-bg);
  outline: none;
  transition: border-color 0.2s;
}

.field-group input:focus { border-color: var(--color-accent); }

.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.badge-draft { background: var(--status-draft-bg); color: var(--status-draft-text); }
.badge-submitted { background: var(--status-submitted-bg); color: var(--status-submitted-text); }
.badge-approved { background: var(--status-approved-bg); color: var(--status-approved-text); }
.badge-rejected { background: var(--status-rejected-bg); color: var(--status-rejected-text); }

.message { margin-top: 16px; font-size: 13px; color: var(--color-accent); }

.panel-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--color-border);
  display: flex;
  gap: 10px;
  flex-shrink: 0;
  background: var(--color-surface);
}

.btn {
  padding: 9px 20px;
  border-radius: var(--radius);
  font-size: 13px;
  font-weight: 500;
  border: none;
  transition: all 0.2s;
}

.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-primary { background: var(--color-accent); color: white; }
.btn-primary:hover:not(:disabled) { background: var(--color-accent-hover); }
.btn-secondary { background: #edf2f7; color: var(--color-text-primary); }
.btn-secondary:hover:not(:disabled) { background: #e2e8f0; }
.btn-success { background: var(--color-success); color: white; }
.btn-success:hover:not(:disabled) { filter: brightness(1.1); }
.btn-danger { background: var(--color-danger); color: white; }
.btn-danger:hover:not(:disabled) { filter: brightness(1.1); }
.btn-outline { background: none; border: 1px solid var(--color-border); color: var(--color-text-secondary); }
.btn-outline:hover:not(:disabled) { border-color: var(--color-text-primary); color: var(--color-text-primary); }
```

- [ ] **Step 2: Import FundDetailComponent in DashboardComponent**

In `dashboard.component.ts`, add `FundDetailComponent` to imports:

```typescript
import { FundDetailComponent } from '../fund-detail/fund-detail.component';
// ...
imports: [DecimalPipe, DatePipe, FundDetailComponent],
```

- [ ] **Step 3: Verify full build passes**

```bash
ng build
```
Expected: `Build at: ... ✔`

- [ ] **Step 4: Commit**

```bash
git add funds-dashboard-frontend/src/app/features/fund-detail/ funds-dashboard-frontend/src/app/features/dashboard/dashboard.component.ts
git commit -m "feat: add FundDetailComponent with slide-in overlay and role-aware buttons"
```

---

## Phase 5 — Wire Up and Verify

---

### Task 16: End-to-end verification

- [ ] **Step 1: Start backend**

```bash
cd funds-dashboard-backend
./mvnw spring-boot:run
```
Expected: `Started FundsDashboardApplication on port 8080`

- [ ] **Step 2: Start frontend**

```bash
cd funds-dashboard-frontend
ng serve
```
Expected: `Application bundle generation complete. [4.xxx seconds]`

- [ ] **Step 3: Login verification**

Open `http://localhost:4200`.

| Action | Expected |
|--------|----------|
| Login as `editor / password` | Dashboard loads, 5 columns visible |
| Click a row | Detail overlay slides in |
| Edit a field → Save | `Saved successfully.` message |
| Reopen same fund | Changed value persists |
| Click Submit | Status → SUBMITTED, overlay closes |
| Login as `approver / password` | Dashboard loads with all 20 columns |
| Click a row | Approve / Reject / Close buttons visible |
| Click Approve | Status → APPROVED |
| Login as `readonly / password` | 5 columns, detail shows only Close |

- [ ] **Step 4: Run all backend tests**

```bash
cd funds-dashboard-backend
./mvnw test
```
Expected: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 5: Final commit**

```bash
git add .
git commit -m "feat: funds dashboard — full implementation complete"
```

---

## Verification Checklist

- [ ] Backend: `./mvnw test` → 14 tests pass, 0 failures
- [ ] Login page renders at `http://localhost:4200/login`
- [ ] Invalid credentials show inline error
- [ ] Editor: 5 columns, Save/Submit/Close in detail
- [ ] Approver: 20 columns, Approve/Reject/Close in detail
- [ ] Read Only: 5 columns, Close only in detail
- [ ] Submit changes status to SUBMITTED
- [ ] Approve changes status to APPROVED
- [ ] Reject changes status to REJECTED
- [ ] Save persists field edits (same session)
- [ ] ReadOnly PUT → 403
- [ ] Editor POST approve → 403
- [ ] Detail overlay slides in with animation
- [ ] Table header stays sticky during scroll
- [ ] Custom scrollbar on table and detail panel
