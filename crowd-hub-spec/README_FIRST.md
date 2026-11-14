# Crowd Hub — Spec Plan (untuk Google AI Studio, mode Build → Android)

Paket ini adalah "otak" proyek: spesifikasi, guardrail, checklist, dan prompt siap pakai.
File untuk agent ditulis dalam **bahasa Inggris** (Gemini lebih patuh); file ini untuk **kamu** (Indonesia).

## Cara pakai (disarankan, via GitHub)
1. Buat repo GitHub **private**, unzip isi folder ini ke root repo, push.
2. AI Studio → Build → pilih **Android** → tombol (+) di kotak prompt → **Import from GitHub** → pilih repo.
3. Buka `00_BOOTSTRAP_PROMPT.md`, ganti `<<PHASE>>` dengan `1A`, tempel ke kotak prompt, kirim.
4. Agent HARUS membalas **PLAN** dulu (Step 0) tanpa kode. Baca, koreksi kalau perlu, lalu balas persis: `APPROVED`.
5. Setelah build selesai, kirim isi `08_prompts/audit-report-request.md`. Cocokkan laporan agent dengan `07_checklists/phase-1A-checklist.md` (atau 1B / 2 sesuai fase). Jangan percaya PASS tanpa bukti (path file + nama simbol).
6. Kalau agent melenceng: pakai `08_prompts/drift-correction.md`. Kalau ada bug: `08_prompts/bug-fix.md`.
7. Fase lulus semua checklist → buka sesi/chat baru, pakai `08_prompts/session-start.md` untuk fase berikutnya.

## Alternatif (tanpa GitHub)
`09_all_in_one/` berisi satu file per fase (bootstrap + guardrail + arsitektur + fitur + checklist). Tempel isinya ke kotak prompt.
Dokumentasi AI Studio hanya menyebut dua jalur masuk: prompt biasa dan Import from GitHub; upload zip / file "rules" khusus tidak disebut. Karena itu paket ini dibuat redundan.

## Urutan fase
| Fase | Isi | Risiko |
|---|---|---|
| 1A | Profile container + vault, API manager, alert engine, dashboard, travel (milik sendiri), stock market + chart + alert, settings/about | Rendah-sedang |
| 1B | In-app browser, sesi login persisten, auto-login opsional, userscript engine (kompatibel PDA) | **Tinggi** |
| 2 | FFScouter connector, ranked war stakeout, flight tracker, chain target assist, attack shortcut | Sedang-tinggi |
| 3 | Backlog polish (`04_features/F14-phase3-backlog.md`) | — |

**Satu fase = satu sesi.** Jangan minta fase berikutnya sebelum checklist fase ini lulus.

## WAJIB sebelum Fase 1A: isi sample JSON asli
Agent tidak boleh menebak field API. Isi folder `03_providers/torn/_samples/` dengan respons asli
(API v2 Playground di https://www.torn.com/api.html, pakai key kamu sendiri). Respons tidak memuat key-mu,
tapi memuat data pribadi (uang, dll.) — silakan samarkan angkanya. Panduan lengkap: `03_providers/torn/_samples/README.md`.
Minimal untuk 1A: `user-basic`, `user-bars`, `user-cooldowns`, `user-travel`, `user-money`, `torn-stocks`.
Untuk Fase 2 tambahan: `faction-members` (pilih faction yang punya member **Traveling**), `faction-rankedwars`, `user-profile-other`, `ffscouter-*`, `hospital-page-rows.html`.
Untuk Fase 1B: `login-page.html` (potongan form login).

## Yang BELUM terverifikasi (ditandai `VERIFY` di dokumen)
1. Path/field persis API v2 vs v1 (sebagian selection masih jatuh ke v1) → diselesaikan lewat `_samples`.
2. Apakah `status.until` terisi untuk status Traveling, dan nilai apa saja di `plane_image_type` → tes 2 menit di Playground; fallback sudah didesain.
3. Endpoint, field, dan batas rate FFScouter → cek https://ffscouter.com/openapi.
4. Selector halaman login Torn dan DOM halaman hospital → butuh sample HTML.
5. Detail kompatibilitas PDA (`PDA_httpGet`, placeholder API key) → cek dokumentasi/wiki Torn PDA.
6. Apakah agent AI Studio otomatis membaca file spec dari repo → makanya ada jalur all-in-one.
7. Batas foreground service Android 15 untuk Watch Mode.
8. **Aturan Torn bisa berubah.** Baca ulang rules scripting/API Torn sebelum rilis (checklist ada di `07_checklists/definition-of-done.md`).

## Peta folder
- `00_BOOTSTRAP_PROMPT.md` — prompt utama yang ditempel
- `01_vision/` — brief produk, branding Crow
- `02_architecture/` — layer, struktur package, model data, batasan teknis
- `03_providers/torn/` — aturan & API Torn, tabel durasi terbang, FFScouter, `_samples/`
- `04_features/` — satu file per fitur (perilaku, edge case, acceptance criteria)
- `05_phases/` — work order per fase
- `06_guardrails/` — aturan keras (otoritas tertinggi)
- `07_checklists/` — checklist per fase + DoD + rencana QA manual
- `08_prompts/` — session-start, audit, drift-correction, bug-fix
- `09_all_in_one/` — satu file per fase (cadangan; ±65–85 KB, jadi pakai hanya kalau Import GitHub tidak jalan)
- `tools/regenerate.py` — membuat ulang checklist + all-in-one setelah kamu mengedit file fitur (`python3 tools/regenerate.py`). Jangan edit file hasil generate secara manual.

## Catatan hukum/etika
Crowd Hub adalah app **tidak resmi**, tidak berafiliasi dengan Torn. Kamu bertanggung jawab atas kepatuhan pada aturan Torn
(termasuk aturan multi-akun dan script). Spesifikasi ini memang membuat multi-profile "siap" secara arsitektur, tapi tidak mengaktifkannya di UI utama.

## Tips menjaga Gemini tetap patuh
- Selalu mulai dengan Step 0 (PLAN) dan baru balas `APPROVED` setelah kamu baca rencananya.
- Satu fase per sesi. Kalau percakapan sudah panjang dan agent mulai lupa aturan: mulai sesi baru dengan `08_prompts/session-start.md`.
- Jangan terima laporan "PASS" tanpa bukti file + simbol. Pakai `08_prompts/drift-correction.md` #7.
- Cek cepat tanpa baca kode: lakukan grep pada DOD-04..DOD-07 di `07_checklists/definition-of-done.md`.
- Kalau agent minta fitur di luar fase, tolak: masukkan ke `BACKLOG_SUGGESTIONS.md`.
