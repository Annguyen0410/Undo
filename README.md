# Undo — Regret Journal

One question every night: *"Is there anything today you wish you'd done differently?"*
The app never judges — it quietly collects what you regret, then shows you
**where your regrets cluster** so you can actually change instead of journaling for fun.

There is no limit anywhere: as many notes a night as you want, for as many years as you
keep going. The journal is meant to be kept for **decades**, so nothing ever reads the
whole table — see [How it scales](#how-it-scales).

100% local-first MVP: no backend, no login, no AI. The "smart" insight sentence is
pure aggregation + category templates.

## Screens

| Screen | What it does |
| --- | --- |
| **Check-in** (home) | The nightly question + free text, one of 8 categories, intensity 1–3. Today's notes stack up (each with its own time, editable/deletable/undoable), the composer keeps an unsaved draft on disk, and a night from the archive is served back underneath (e.g. "Ten years ago tonight" — which era appears rotates with the date) |
| **Journal** | The archive: pinned search + category filters, 50 notes per page as you scroll, day headers with a per-day count, and **jump to any month that holds notes** |
| **Insights** | Streak, category donut chart, auto insight sentence, monthly trend. Free = last 7 days; Premium = full history + trend |
| **Premium & settings** | Paywall (RevenueCat), reminder time, archive size, Midnight Reflection theme, and CSV export/import of the whole journal (free, deliberately — see *Where the journal lives*) |

## Design system

One dark palette, one spacing scale, one panel — so three tabs can't drift apart.

| Token | File | Rule |
| --- | --- | --- |
| Colours | `ui/theme/Color.kt` | Three-step surface ladder (canvas → well → panel) plus a hairline. Every panel that matters carries a border: on a canvas this dark a fill alone is only a few values brighter than the background and reads as a smudge |
| Spacing | `ui/theme/Dimens.kt` | Six steps (4/8/12/16/24/32dp). No screen invents its own number |
| Type | `ui/theme/Type.kt` | Serif is for *content* (the nightly question, headings), sans for everything else. Uppercase section labels use `SectionLabelText` and nothing else does |
| Panels | `ui/components/Surfaces.kt` | `AppCard`, `SectionLabel`, `SegmentedControl`, `PrimaryActionButton`, `EmptyState`. Screens compose these instead of restyling `Card` locally |

Two rules the codebase is now held to: primary actions are **solid** (not gradient — a two-colour wash under a dark label was the lowest-contrast control on screen), and the same control looks the same everywhere (the range switch on Insights and the intensity picker on Check-in are the same `SegmentedControl`).

### Reviewing UI changes

The emulator is the only honest reviewer, so the loop is scripted:

```bash
python docs/shipaton/ui_drive.py dump                # list tappable nodes + their bounds
python docs/shipaton/ui_drive.py tap "Insights"      # tap by label, not by guessed pixel
python docs/shipaton/ui_drive.py shot 03-insights    # capture into docs/shipaton/shots/
python docs/shipaton/make_review.py                  # build ui-review.html from those shots
```

`make_review.py` inlines the screenshots as base64 so the whole review page is a single
file; `--only`/`--width` filter and size it for a close look at one screen.

## How it scales

A journal this app is actually for — twenty years of a few notes a night — is tens of
thousands of rows, so no screen ever holds the journal. Four rules do the work:

| Where | Rule |
| --- | --- |
| Journal | Reads **one page of 50 notes** at a time (`LIMIT/OFFSET` over the `(date, createdAt)` index) and extends the window as you scroll. Search and filters re-read only the window that is on screen |
| Journal navigation | A **ceiling** on the index (`date <= :ceiling`) instead of `OFFSET`-ing past the years in front. "Jump to month" is a range scan, not a scroll of ten thousand rows |
| Insights | Counts come from SQL (`GROUP BY category`, `GROUP BY substr(date,1,7)`) and the streak from the list of distinct dates — one short string per day. Ten of them or fifty thousand, the screen costs the same |
| Archive memory | Chooses a night from the **distinct dates** only, then loads just that day's notes |

Measured on the emulator (debug build, 7,185 notes across 20 years, `adb dumpsys gfxinfo`
for frames, `Log` timing around the queries for the rest):

| Operation | Warm |
| --- | --- |
| First page + exact count over the whole journal | **~40 ms** (37–48 ms) |
| Next page as you reach the end of the window | **~60 ms** |
| Cold process, very first query (Room open + first query on the emulator's virtual disk) | ~1.6 s, and the emulator itself reports ~100 ms frames for *any* scroll, including a static settings list |

You can reproduce it: Settings → *Developer → Twenty years* seeds ~8,700 notes over two
decades, then scroll the journal, jump to 2006, search, and open Insights.

## Stack

- Jetpack Compose (Material 3), single-activity
- Room (one table: `entries`) — local only
- WorkManager + notification channel for the nightly reminder
- Vico for the donut & column charts
- RevenueCat (`purchases` + `purchases-ui`, Paywalls v2) for subscriptions

## Build & run

```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Install on a device/emulator and allow notification permission (Android 13+).

A fresh clone has no `local.properties` (it is git-ignored), so Gradle has to find the
Android SDK through `ANDROID_HOME` / `ANDROID_SDK_ROOT`, or you create that file yourself.
Verified: `git clone` → `./gradlew :app:assembleDebug` builds from scratch.

## RevenueCat setup

Keys are **never committed**. They are read from `local.properties` (git-ignored) or
from the environment at build time, and selected per build type in
`app/build.gradle.kts`:

| Build type | `local.properties` key | Used for |
| --- | --- | --- |
| `debug` | `REVENUECAT_TEST_KEY` | RevenueCat **Test Store** — real offerings, purchases and entitlements with no store account |
| `release` | `REVENUECAT_GOOGLE_KEY` | Google Play public SDK key (`goog_…`) |

```properties
# local.properties
REVENUECAT_TEST_KEY=test_xxxxxxxxxxxx
REVENUECAT_GOOGLE_KEY=goog_xxxxxxxxxxxx
```

Dashboard side:

1. Create a project at [app.revenuecat.com](https://app.revenuecat.com).
2. **Test Store** — *Apps and providers → Test configuration* → create a Test Store and
   copy its API key into `REVENUECAT_TEST_KEY`.
3. **Product catalog** — create the subscription products, put them in an offering, and
   mark that offering **Current** — the app reads `offerings.current`, not a named offering.
4. The entitlement id must be **`premium`** (see `RevenueCatManager.ENTITLEMENT_ID`).
5. Google Play or Galaxy Store later — connect the store, add the products, and put the
   platform key into `REVENUECAT_GOOGLE_KEY`; release builds pick it up automatically.

Never ship a `test_…` key in a release build. Leave both keys empty to run fully
offline: the paywall falls back to a static plan list and the
**"Simulate premium (debug)"** switch in Settings unlocks premium features locally.

Test Store purchases show RevenueCat's own confirmation sheet and are reported as
sandbox data — they update `CustomerInfo`, activate the `premium` entitlement and
appear in the dashboard, exactly like real purchases.

## How RevenueCat is integrated

Five pieces of the codebase make up the entire purchase path, so the wiring can be
followed end to end without reverse-engineering it:

| Where | What it does |
| --- | --- |
| `UndoApplication.kt` | Calls `RevenueCatManager.configure()` once at startup, with the build-type key. |
| `purchase/RevenueCatManager.kt` | Where the SDK gets configured — `Purchases.configure()` with the build-type key, plus `getCustomerInfo()` to derive `isPremium` from the `premium` entitlement. Called once at startup, and again after a purchase or a restore. |
| `ui/paywall/PaywallViewModel.kt` | Reads `offerings.current`, turns its packages into UI state, runs `awaitPurchaseResult()` / `awaitRestoreResult()`, and falls back to a static plan list when no key is configured (offline demo). |
| `ui/paywall/PaywallScreen.kt` | Renders the real packages with their localised prices, plus purchase, restore and free-trial copy. |
| Premium gates | Full history + monthly trend, custom reminder time and the Midnight Reflection theme all read the same `isPremium` state. Export and import are deliberately outside that gate. |

No key is committed, so `REVENUECAT_API_KEY` is a `buildConfigField` that defaults to an
empty string — that is the offline demo path, and the paywall shows a "Demo mode" banner
when it is active. The recorded demo run must show that banner absent.

If you want to exercise the flow without setting up your own dashboard project, put your
own Test Store key in `REVENUECAT_TEST_KEY` — the app accepts any of them. (Publishing a
`test_…` key in the README would also work, since Test Store keys are client-side by
design and simulate payment with no money involved, but the release key must never be
exposed that way.)

## Premium vs free

| Feature | Free | Premium |
| --- | --- | --- |
| Unlimited check-ins | ✅ | ✅ |
| Timeline | ✅ | ✅ |
| Insights (last 7 days) | ✅ | ✅ |
| Export & import journal (CSV) | ✅ | ✅ |
| Full history + monthly trend | — | ✅ |
| Custom reminder time | — | ✅ |
| Midnight Reflection theme | — | ✅ |

## Where the journal lives

Nothing about your notes leaves the device unless you hand them over.

Android's cloud backup uploads an app's whole data directory by default, which for this app
would mean uploading every private note. `res/xml/data_extraction_rules.xml` (API 31+) and
`res/xml/backup_rules.xml` (older) replace that default with a decision instead of a
default: the note database and the in-progress draft are **excluded from cloud backup**,
while settings and RevenueCat's anonymous app user ID are still backed up — that ID carries
no personal content, and keeping it is what lets a paid entitlement survive a restore.
Device transfer is left wide open on purpose, so moving to a new phone carries the journal
straight across without a copy ever sitting on a server.

The trade-off is stated rather than hidden: lose the phone without exporting and the notes
are gone. That is exactly why CSV export *and* import are free features rather than Premium
ones — the portable file is the backup, and it is one the user holds. Import skips notes
already in the journal, so restoring the same file twice cannot duplicate a journal.

## Notes

- Debug builds only: Settings has four developer cards — *Simulate premium*, *Sample
  journal* (back-fills ~90 days of history for demos, screenshots and videos), *Twenty
  years* (seeds ~8,700 notes across 20 years to stress the paging) and *Clear journal*.
  All of them skip days that already hold a note, so they never overwrite your own
  writing, and none of them ship in a release build.
- Reminders: default 8:00 PM. WorkManager runs a 24h periodic job aligned to the
  chosen time; the worker tolerates Doze delay and notifies at most once per day — and
  only when today has not been written yet.
- The reminder toggle is free; choosing a *custom* time is premium.
- **No limit on notes per day**, and nothing is capped by age: the table has no unique
  constraint on `date`, and every read is a page or an aggregate. A day holding three
  notes is the normal case, not an upsert.
- **Drafts never lose text.** The composer writes every edit to its own preferences file
  (`DraftStore`), so a killed process comes back mid-sentence. A draft is restored
  whatever day it was written on (the label says which night it was), and a launch that
  only ever showed the question never wipes one.
- Deleting a note is immediate but undoable for a few seconds, and the undo re-inserts the
  row with its original id and timestamp.
- The launcher icon is generated, not hand-drawn: adaptive layers for API 26+, legacy
  mipmaps for below that, and a themed silhouette for Android 13+. Run
  `python docs/shipaton/make_icon.py` after touching the palette, so the app icon, the store
  icon and the demo video all show the same crescent.

## Shipaton 2026

This project is being submitted to the RevenueCat Shipaton 2026 **Next Gen Award**
(student category — video + open-source code instead of a store release).

Confirmed with the organizer: for Next Gen, a **RevenueCat Test Store** build counts as
the required "in-app purchase powered by the RevenueCat SDK" — no paid Apple or Google
developer account and no store listing are needed — provided the demo video and this public
repository clearly show how RevenueCat is integrated and how the purchase flow works. So
this README documents [the integration](#how-revenuecat-is-integrated) and the video walks
the app end to end, including the paywall loading the three real Test Store packages
through the SDK.

**Demo video:** <https://youtu.be/2b89ehOxyOQ> — under two minutes: the nightly check-in,
a twenty-year journal (7,185 notes), the *From the archive* card, and the paywall showing
the real offerings RevenueCat hands back.

Submission material lives in [`docs/shipaton/`](docs/shipaton):

- `SUBMISSION.md` — project description, requirements checklist and timeline
- `VIDEO-SCRIPT.md` — shot list for the two-minute demo video
- `undo-demo-v1.mp4` — the recorded demo take (the submitted copy is the YouTube upload
  linked above; this is the same cut, kept beside the shot list)
- `undo-icon-1024.png` — 1024×1024 submission icon
- `make_icon.py` — regenerates the submission icon **and** the launcher icon (adaptive
  layers + legacy mipmaps) from the same drawing, then checks the launcher assets by pixel
- `icon_preview.py` — opens the icon through the real launcher masks (circle, squircle)
- `ui_drive.py` / `make_review.py` — the emulator review loop described above
  (throwaway tooling: `ui-review.html` and `shots/` are git-ignored)

## License

MIT — see [LICENSE](LICENSE).
