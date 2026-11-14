#!/usr/bin/env python3
"""Regenerates 07_checklists/phase-*-checklist.md and 09_all_in_one/*.md from the source docs.
Run from the repo root:  python3 tools/regenerate.py
Edit the feature files (04_features) -> run this -> commit.  Never edit the generated files by hand."""
import re, pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
rd = lambda p: (ROOT / p).read_text(encoding="utf-8")

PHASES = {
    "1A": dict(features=["F00-settings-privacy-about", "F01-profile-container-and-vault", "F02-api-manager",
                         "F03-alert-engine", "F06-dashboard", "F07-travel-tracker", "F08-stock-market"],
               providers=["torn-rules-and-compliance", "torn-api-reference"],
               samples=["user-basic.json", "user-bars.json", "user-cooldowns.json", "user-travel.json",
                        "user-money.json", "user-notifications.json", "torn-stocks.json"]),
    "1B": dict(features=["F04-in-app-browser-and-session", "F05-userscript-engine"],
               providers=["torn-rules-and-compliance", "torn-api-reference"],
               samples=["login-page.html"]),
    "2":  dict(features=["F09-ffscouter-connector", "F10-ranked-war-stakeout", "F11-flight-tracker",
                         "F12-chain-target-assist", "F13-attack-shortcut"],
               providers=["torn-rules-and-compliance", "torn-api-reference", "flight-times", "connector-ffscouter"],
               samples=["faction-members.json", "faction-rankedwars.json", "user-profile-other.json",
                        "ffscouter-get-stats.json", "ffscouter-get-targets.json", "hospital-page-rows.html",
                        "flight-data-notes.md"]),
}

def ac_lines(text):
    return [l.rstrip() for l in text.splitlines() if re.match(r"^- \[ \] [A-Z0-9]+-AC-\d+", l)]

def dod_lines():
    return [l.rstrip() for l in rd("07_checklists/definition-of-done.md").splitlines()
            if re.match(r"^- \[ \] DOD-\d+", l)]

GUARD_SPOT = [
    "- [ ] GR-01 A-01..A-09 process rules respected (only this phase, plan approved, no invented data, no renames).",
    "- [ ] GR-02 B-01..B-06 platform rules respected (Kotlin/Compose, single module, no Firebase/Gemini/analytics, allowed libs only).",
    "- [ ] GR-03 C-01..C-11 Torn compliance respected (API only or user-loaded page; no automation; limiter used).",
    "- [ ] GR-04 D-01..D-06 security/privacy respected (vault only, redaction, FLAG_SECURE, WebView hardening).",
    "- [ ] GR-05 E-01..E-09 code-quality rules respected (defensive parsing, Clock, states, no GlobalScope, tests for pure logic).",
    "- [ ] GR-06 F-01..F-05 UX rules respected (Crow palette, a11y, text+color status, staleness).",
]

def checklist(phase, cfg):
    out = [f"# Phase {phase} Checklist (GENERATED — do not edit; edit the feature files and run tools/regenerate.py)", "",
           "For each line the agent reports: `ID | PASS/FAIL/NOT DONE/NA | evidence: file#symbol | note`.",
           "The user verifies a sample of PASS items by reading the cited code and running `07_checklists/qa-manual-test-plan.md`.", ""]
    out.append("## Feature acceptance criteria")
    for f in cfg["features"]:
        t = rd(f"04_features/{f}.md")
        title = t.splitlines()[0].lstrip("# ").strip()
        out += ["", f"### {title}", *ac_lines(t)]
    out += ["", "## Definition of Done", *dod_lines(), "", "## Guardrail spot checks", *GUARD_SPOT, ""]
    (ROOT / f"07_checklists/phase-{phase}-checklist.md").write_text("\n".join(out), encoding="utf-8")

def all_in_one(phase, cfg):
    boot = rd("00_BOOTSTRAP_PROMPT.md").replace("<<PHASE>>", phase)
    parts = [f"# CROWD HUB — ALL-IN-ONE PROMPT FOR PHASE {phase} (GENERATED)",
             "Use this when the project files are NOT visible to the agent. Everything the agent needs is below.",
             "Paste the whole file into AI Studio (Android build). Then paste real sample JSON in the SAMPLES section at the end if you have not already.",
             "", "=" * 70, "## PART 0 — BOOTSTRAP PROMPT", "=" * 70, boot]
    def add(title, path):
        parts.extend(["", "=" * 70, f"## {title}  (`{path}`)", "=" * 70, rd(path)])
    add("PART 1 — GUARDRAILS (highest authority)", "06_guardrails/GUARDRAILS.md")
    add("PART 2 — ANTI SCOPE CREEP", "06_guardrails/anti-scope-creep.md")
    add("PART 3 — SECURITY & PRIVACY", "06_guardrails/security-and-privacy.md")
    add("PART 4 — PRODUCT BRIEF", "01_vision/product-brief.md")
    add("PART 5 — BRANDING", "01_vision/branding-crow-theme.md")
    add("PART 6 — ARCHITECTURE", "02_architecture/architecture-overview.md")
    add("PART 7 — PACKAGE STRUCTURE", "02_architecture/package-structure.md")
    add("PART 8 — DATA MODEL", "02_architecture/data-model.md")
    add("PART 9 — TECH CONSTRAINTS", "02_architecture/tech-constraints.md")
    n = 10
    for p in cfg["providers"]:
        add(f"PART {n} — PROVIDER DOC", f"03_providers/torn/{p}.md"); n += 1
    for f in cfg["features"]:
        add(f"PART {n} — FEATURE", f"04_features/{f}.md"); n += 1
    add(f"PART {n} — PHASE WORK ORDER", f"05_phases/phase-{phase}.md"); n += 1
    add(f"PART {n} — CHECKLIST", f"07_checklists/phase-{phase}-checklist.md"); n += 1
    add(f"PART {n} — AUDIT REPORT TEMPLATE", "08_prompts/audit-report-request.md"); n += 1
    parts += ["", "=" * 70, "## SAMPLES (paste real responses; do not let the agent guess fields)", "=" * 70]
    for s in cfg["samples"]:
        parts += [f"### {s}", "```", "PASTE_REAL_SAMPLE_HERE", "```", ""]
    parts += ["=" * 70, "## FINAL REMINDER",
              "Plan only (Step 0) -> wait for APPROVED -> build ONLY phase " + phase + " -> Audit Report with evidence. "
              "Never guess API fields. All Torn API calls via RateLimiter. No secrets in logs/UI. No automation of game actions. When in doubt: STOP AND ASK."]
    (ROOT / f"09_all_in_one/phase-{phase}-all-in-one.md").write_text("\n".join(parts), encoding="utf-8")

for ph, cfg in PHASES.items():
    checklist(ph, cfg)
for ph, cfg in PHASES.items():
    all_in_one(ph, cfg)
print("generated checklists + all-in-one for", ", ".join(PHASES))
