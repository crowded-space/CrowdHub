# F11 — Flight Tracker (ETA estimator for OTHER players)
Phase: 2 | Depends on: F10 (MemberDiffer), flight-times.md | Package: `feature/flight`, `provider/torn/FlightTable`

## Purpose
Estimate when an enemy lands, for **preparation and calculation** (not pinpoint accuracy). Every ETA is a **window** with a **confidence** level.

## Inputs
- `MemberEvent.TravelStarted(playerId, direction, place, planeTypeApi?, prevPollAt, detectedAt)` from F10.
- `status.until` (if the API provides it for Traveling — VERIFY via `_samples/flight-data-notes.md`). **If `until` is present and in the future, use it as the ETA (confidence HIGH) and skip estimation.**
- `FlightTable` (`flight-times.md`), `PlaneTypeMap` (Room, user-editable), `PlayerFlightProfile` (book knowledge).

## Plane type mapping
`status.plane_image_type` (API string, VERIFY values) -> `PlaneCategory` {STANDARD, AIRSTRIP, WLT, BUSINESS, UNKNOWN} via `PlaneTypeMapEntity`. Defaults: EMPTY (everything UNKNOWN) until the user fills `_samples/flight-data-notes.md` and the Flight Debug screen. Do NOT guess the mapping.

## Algorithm (`FlightEstimator`, pure Kotlin, unit tested)
```
base      = FlightTable.baseMinutes(place, category)            // category known -> one value
bases     = all 4 categories                                     // category UNKNOWN -> min..max
variance  = 3%
bookFactor: hasBook == true -> 0.75 ; false -> 1.0 ; unknown -> {0.75 , 1.0}
durMin    = min(base) * min(bookFactor) * 0.97
durMax    = max(base) * max(bookFactor) * 1.03
departWindow = [prevPollAt, detectedAt]       // when the status flipped to Traveling between two polls
ETA window = [ departWindow.start + durMin , departWindow.end + durMax ]
```
- If departure was NOT observed (the player was already Traveling when first seen at `firstSeenAt`): the flight started at some unknown time before `firstSeenAt`, so the ETA window is `[now, firstSeenAt + durMax]` (it cannot land later than a flight that left exactly at `firstSeenAt`). Confidence is GRAY.
- Direction & place from `status.description` parsing (`torn-api-reference.md`).
- Overdue: if now > window.end + 60 s and still Traveling -> state DELAYED (a Detective Agency delay is possible): show "Delayed (+mm:ss)" and widen the window end by +10% of durMax every 5 min; never claim precision.

## Confidence
- **GREEN**: category known AND book known AND departure observed.
- **YELLOW**: departure observed but book OR category unknown (window shown as range).
- **GRAY**: departure not observed, place unknown, or `Hidden`.
Display: chip "Lands ~mm:ss–mm:ss" (or `~mm:ss` when window <= 2 min) + colored dot + "est." label. Chip tap -> sheet explaining the model and the uncertainty sources.

## Learning (per player)
- On landing (state leaves Traveling): `observed = landedWindow - departWindow` (needs observed departure). Compare against `durMin..durMax` for book=false and book=true for the known category; if exactly one fits -> set `hasBook` accordingly with `lastObservedAt`.
- Validity: `hasBook=true` known for 31 days; `hasBook=false` known for 7 days (the player may buy a book). Expired -> unknown.
- Persist `FlightObservationEntity` rows (keep last 50 per player; prune 30 days).
- If category is UNKNOWN but observed ratio matches exactly one category for a known place/book, DO NOT auto-fill the map; show the suggestion in Flight Debug only.

## Alerts
- "Lands in N min" scheduled at `window.start - lead` (default lead 2 min, "earliest" mode) or at the window midpoint (user option). If the window moves, re-schedule (F03 rules).
- After landing there is a **15 s grace period** before the player can be attacked: the row shows "Attackable in 0:15" countdown after the landing event. While Traveling the player cannot be attacked.

## Flight Debug screen (Settings > Developer > Flight Debug)
- Editable `PlaneTypeMap` (API value -> category) and a list of observed `plane_image_type` values with counts.
- Table view of `FlightTable` (read-only).
- Recent observations: place, direction, plane value, observed duration vs expected for each category/book combo, inferred book.

## Edge cases
- Return flights (`Returning to Torn from X`) use the same table; direction RETURN.
- Hospitalized abroad then released: not a flight.
- Unknown place string: log once to Flight Debug; GRAY.
- Player hides travel (10-star Theater): no events -> nothing shown.

## Acceptance criteria
- [ ] F11-AC-01 If `status.until` is provided and in the future it is used directly (HIGH) — gated by sample evidence, otherwise the estimator is used.
- [ ] F11-AC-02 `FlightEstimator` computes windows per the algorithm for all category/book combinations; unit tested against `flight-times.md` incl. variance and unknown book/category.
- [ ] F11-AC-03 Confidence (GREEN/YELLOW/GRAY) is computed per the rules and always displayed with "est.".
- [ ] F11-AC-04 First-seen-already-traveling yields GRAY with a bounded window; overdue flights show DELAYED and widen as specified.
- [ ] F11-AC-05 Landing observations update `PlayerFlightProfile.hasBook` with the stated validity periods (unit tested).
- [ ] F11-AC-06 `PlaneTypeMap` has no hard-coded guesses; the Flight Debug screen can edit it; unmapped values -> UNKNOWN category.
- [ ] F11-AC-07 "Lands in N min" alerts schedule from the window start (or midpoint option), re-schedule when the window changes.
- [ ] F11-AC-08 A 15 s post-landing "Attackable in" countdown is shown on the row.
- [ ] F11-AC-09 Flight Debug screen shows the plane-type map editor, observed values, table and recent observations.
- [ ] F11-AC-10 No estimation is presented as exact; the explanation sheet lists the uncertainty sources (poll interval, 3% variance, book, delays, hidden travel).

## Out of scope
Using FFScouter's premium flight data, predicting departures, flight planning for the user.
