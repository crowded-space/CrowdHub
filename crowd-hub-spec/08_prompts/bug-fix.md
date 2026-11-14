# Bug Fix (constrained)

```
BUG FIX REQUEST (do not add features, do not refactor).
Phase context: <<PHASE>> (accepted code). Feature: <<F-ID>>.
Observed: <<what happens>>
Expected (per spec/AC): <<AC-ID and expected behavior>>
Steps/Evidence: <<steps, screenshot description, log excerpt with secrets removed>>

Process:
1. Explain the root cause in <= 5 lines (file + symbol).
2. Propose the SMALLEST change (list files/functions) and wait for "APPROVED" if it touches more than 3 files or any guardrail-relevant code (network, vault, alerts, WebView).
3. Apply the fix. Add/adjust a unit test if the code is pure logic.
4. Report: changed files, why it cannot affect other features, and re-run of the relevant checklist IDs.
```
