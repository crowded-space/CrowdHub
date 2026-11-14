# Drift Correction (use when the agent goes off-spec)

Pick the matching block, fill the <<>> parts, send.

## 1) Built things outside the phase
```
STOP. You violated A-01/anti-scope-creep: you added <<what>> which is not in phase <<PHASE>>.
Revert it completely (files, routes, strings, dependencies) and confirm with a list of removed files/symbols.
Put the idea in BACKLOG_SUGGESTIONS.md. Do not add anything else. Reply with the list only.
```

## 2) Invented API data / guessed something
```
STOP. You violated A-03: <<what you invented>> is not in the spec or _samples.
Do this: (1) list every place it is used; (2) make the code defensive (nullable, no crash) or remove the feature part; (3) mark it VERIFY in the code comments and in your report; (4) ask me for the missing sample instead of guessing.
```

## 3) Forbidden library / Firebase / Gemini / DI framework
```
STOP. You violated B-02/B-03/B-04 by adding <<library/service>>. Remove it and any code depending on it; use the approved approach from 02_architecture/tech-constraints.md. Confirm the build files changed.
```

## 4) Security/privacy leak
```
STOP. You violated D-01/D-02: <<where a secret can reach logs/UI/storage>>. Fix it by routing through SecretVault/CrowLog redaction. Then show the grep/search results proving no other leak exists (key, password, Authorization, key= query).
```

## 5) Torn compliance
```
STOP. You violated C-0x: <<what>>. Remove any automatic page load/click/refresh or non-API request on torn.com that the user did not trigger. Show the search results proving none remain.
```

## 6) Refactoring/renaming without being asked
```
STOP. You violated A-04/A-05. Restore the original names/structure from 02_architecture/package-structure.md and undo unrelated refactors. List what you restored.
```

## 7) Claimed PASS without evidence
```
Your audit marked <<IDs>> as PASS without evidence. For each: give file path + symbol proving it, or change it to FAIL/NOT DONE. Then fix the FAIL items in the smallest possible change.
```

## 8) Agent stops early / leaves stubs
```
Continue the build. Items still NOT DONE: <<IDs>>. No TODO/placeholder logic reachable by the user (E-08). Implement them completely, then re-issue the audit report for those IDs only.
```
