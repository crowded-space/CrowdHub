# Audit Report Request (send after a phase build; also the template the agent must use)

```
Produce the AUDIT REPORT for phase <<PHASE>> now. Do not change code in this reply.

Format (exactly):
1. BUILD STATUS: compiles? (yes/no/unknown) — how you know. Runs in the emulator? (yes/no/unknown).
2. CHECKLIST: for EVERY ID in 07_checklists/phase-<<PHASE>>-checklist.md output one line:
   `<ID> | PASS|FAIL|NOT DONE|NA | evidence: <file path>#<symbol or line range> | note`
   Rules: PASS requires evidence (file + symbol). If you did not verify it, write NOT DONE or FAIL, never PASS.
3. GUARDRAIL SCAN: for each of A-01..G-03 write OK or VIOLATION (+ where). Explicitly answer:
   - Any API field/URL/selector invented? (list them, each marked VERIFY)
   - Any Torn API call that bypasses RateLimiter? (search results)
   - Any secret that could reach logs/UI/backups? (search results)
   - Any request to torn.com pages not triggered by the user? (search results)
   - Any dependency outside the allowed list? (list)
4. DEVIATIONS from the spec (what/why).
5. UNVERIFIED items (VERIFY list) and which `_samples` were missing.
6. KNOWN LIMITATIONS and bugs you are aware of.
7. BACKLOG_SUGGESTIONS added (list).
Be concise and factual. No marketing language.
```
