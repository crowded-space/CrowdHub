# Flight Times (Torn Wiki "Travel" page, retrieved Nov 2026)

## Model
`duration = standard(dest) x typeMultiplier x bookFactor x (1 +/- 3% variance)`
- Types: STANDARD x1.00, AIRSTRIP x0.70, WLT x0.50 (shown as "Private" on the Travel Agent page), BUSINESS x0.30.
- Book "Mailing Yourself Abroad": additional -25% for 31 days -> `bookFactor = 0.75` (not visible in the API).
- Flight time has a **3% variance**. A Detective Agency "Watchlist" special can **extend** a flight; WLT 9M-share holders are immune to such delays (VERIFY).
- While Traveling a player **cannot be attacked**; on arrival there is a **15 s grace period** before they can be attacked.
- Players in a 10-star Theater hide their travel status/destination from others -> untrackable.

## Table (minutes, WITHOUT book) — source of truth for `FlightTable`
| Place | Standard | Airstrip | WLT | Business |
|---|---|---|---|---|
| MEXICO | 24 | 17 | 12 | 7 |
| CAYMAN_ISLANDS | 33 | 23 | 17 | 10 |
| CANADA | 39 | 27 | 19 | 12 |
| HAWAII | 127 | 89 | 63 | 38 |
| UNITED_KINGDOM | 151 | 106 | 75 | 45 |
| ARGENTINA | 158 | 111 | 79 | 47 |
| SWITZERLAND | 166 | 116 | 83 | 50 |
| JAPAN | 213 | 149 | 107 | 64 |
| CHINA | 229 | 160 | 114 | 69 |
| UAE | 257 | 180 | 128 | 77 |
| SOUTH_AFRICA | 282 | 197 | 141 | 85 |

## Reference: wiki table WITH book (minutes) — use ONLY for unit tests
`withBook ~= round(withoutBook x 0.75)` within +/-1 minute.
| Place | Standard | Airstrip | WLT | Business |
|---|---|---|---|---|
| MEXICO | 18 | 13 | 9 | 5 |
| CAYMAN_ISLANDS | 25 | 17 | 13 | 8 |
| CANADA | 29 | 20 | 15 | 9 |
| HAWAII | 95 | 67 | 48 | 29 |
| UNITED_KINGDOM | 113 | 80 | 57 | 34 |
| ARGENTINA | 119 | 83 | 59 | 35 |
| SWITZERLAND | 125 | **97 (wiki anomaly; expected ~87)** | 62 | 38 |
| JAPAN | 160 | 112 | 80 | 48 |
| CHINA | 172 | 120 | 86 | 52 |
| UAE | 193 | 135 | 97 | 58 |
| SOUTH_AFRICA | 212 | 148 | 106 | 64 |
The SWITZERLAND/Airstrip with-book value on the wiki looks like a typo; the estimator uses the multiplicative model, so it is unaffected. Skip that cell in the +/-1 test.

## Implementation notes
- `FlightTable` is a plain Kotlin object with a `Map<Place, IntArray(4)>` of the first table, plus `fun baseMinutes(place, category)`.
- Travel times have been changed by Torn in the past; keep the table in ONE file and expose it in the Flight Debug screen so it can be edited quickly.
- Standard ticket costs exist on the wiki but are irrelevant to Crowd Hub.
