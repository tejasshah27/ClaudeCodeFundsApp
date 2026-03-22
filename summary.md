# Session Summary

## Agent Tool — Learnings

### What it does
Launches isolated subprocesses with fresh context. Each agent call's full output (tool calls + results + report) is written back into the main conversation context.

### Cost in this session
- ~30+ agent calls across 16 tasks (implementers, spec reviewers, code quality reviewers)
- Total agent context usage: 40.8k tokens (20% of context window)

### Where agents were justified
| Task | Agent Role | Why justified |
|------|-----------|---------------|
| Task 6 | Spec review — SecurityConfig | Non-trivial: 4 beans, wiring dependencies, Spring Security 6 API |
| Task 7 | Spec review — Controllers | Non-trivial: endpoint mapping, role checks, status codes across 2 files |
| Task 12 | Spec review — Core services | Non-trivial: Angular 21 interceptor approach for withCredentials needed judgment |
| Task 15 | Spec review — FundDetailComponent | Non-trivial: animation keyframe values, 20-field count, 3-role button combinations |

### Where a direct Read/Grep would have been enough
| Task | Agent used | Could have been |
|------|-----------|-----------------|
| Task 2 | Spec review — 3 model files | Read: FundStatus.java, UserRole.java, Fund.java — tiny files, mechanical field check |
| Task 3 | Spec review — 5 DTO files | Read: all 5 DTOs — small files, just count fields and verify annotations |
| Task 4 | Spec review — MockDataService | Read: MockDataService.java — one file, verify loop count and method signatures |
| Task 5 | Spec review — FundService | Read: FundService.java — one file, verify method signatures and null checks |

### Rule of thumb
- **Use Agent** when verification requires judgment, cross-file reasoning, or non-obvious correctness checks
- **Use Read/Grep directly** when the check is "read file X, confirm these field names exist" — saves ~2-3k tokens per avoided agent call
