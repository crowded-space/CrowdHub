# _samples — real responses the agent must use (no guessing)

The agent builds DTOs ONLY from these files. Replace each placeholder with a real response. Redact personal numbers if you like (keep the structure and field names!). Never include your API key (responses don't contain it; check anyway).

## How to get them
1. Open https://www.torn.com/api.html -> **API v2 Playground** (or the Swagger page there).
2. Paste your key (a Limited key). Pick the endpoint, run, copy the JSON body.
3. Paste it into the matching file here. If a selection only works on v1, save the v1 response in the same file and add a first line comment in a sibling `.note.txt` saying "v1".

## Files
| File | How to get it | Needed in |
|---|---|---|
| `user-basic.json` | user -> basic | 1A |
| `user-bars.json` | user -> bars (v1 if needed) | 1A |
| `user-cooldowns.json` | user -> cooldowns | 1A |
| `user-travel.json` | user -> travel (ideally while flying; also one while NOT traveling) | 1A |
| `user-money.json` | user -> money | 1A |
| `user-notifications.json` | user -> notifications (and events if separate) | 1A |
| `torn-stocks.json` | torn -> stocks | 1A |
| `login-page.html` | outer HTML of the torn.com login form (only the `<form>`; no secrets) | 1B |
| `faction-members.json` | faction/{id}/members for a faction that has members in **Traveling**, **Hospital**, **Abroad** and **Okay** | 2 |
| `faction-rankedwars.json` | faction/{id}/rankedwars | 2 |
| `user-profile-other.json` | user/{id}/profile for another player | 2 |
| `ffscouter-get-stats.json` | FFScouter get-stats for 3-5 IDs | 2 |
| `ffscouter-get-targets.json` | FFScouter get-targets | 2 |
| `hospital-page-rows.html` | outer HTML of 2-3 rows from the hospital list page | 2 |

## The 2-minute flight-data test (Phase 2 prerequisite)
In `faction-members.json`, find a member whose `status.state` is `Traveling`. Write down:
1. Is `status.until` a non-zero number? (If yes, the flight tracker can use it directly.)
2. What are the values of `status.plane_image_type` you see? (Collect several: standard / airstrip / private / business if possible.)
Put the answers in `flight-data-notes.md` in this folder.
