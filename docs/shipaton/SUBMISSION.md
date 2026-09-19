# Shipaton 2026 submission — Undo (Next Gen Award)

Status: **not yet submittable** — this file tracks exactly what still has to be true
before we hit *Submit* on Devpost.

- Event: RevenueCat Shipaton 2026 (<https://revenuecat-shipaton-2026.devpost.com>)
- Category: **Next Gen Award** (student — video + open-source repo, no store listing)
- Deadline: **Sep 30, 2026 11:45 PM PDT** = **Oct 1, 2026 13:45 ICT (giờ Việt Nam)**
- Target submit date: **Sep 27–28, 2026** (leave 2 days of slack)

---

## 1. Why this category

| Path | Verdict |
| --- | --- |
| Apple App Store | Not possible — the app is Android-only, no macOS build machine and no iOS code. |
| Google Play | Blocked for a *new personal* Play Console account: 12 testers opted in **continuously for 14 days** are required before you can even apply for production access. Started Sep 17 → cannot finish by Sep 30. |
| Samsung Galaxy Store | Would work, and there is **no 12-tester / 14-day gate** here — beta testing is available and recommended (especially for IAP) but is *not* a required period before publication; only Samsung's normal review applies. Blocked in practice by the D-U-N-S number + Commercial Seller Status you need in order to sell, i.e. a registered business. |
| **Next Gen Award** | ✅ No store, no paid developer account. Requires an active student + an **academic email** + video + public open-source repo. |

`csu.fullerton.edu` is listed in JetBrains/swot (`lib/domains/edu/fullerton.txt` →
*California State University, Fullerton*), so `AnNguyen0410@csu.fullerton.edu` passes the
Next Gen domain check. A Gmail address does **not** pass it.

> **For Next Gen, no store is involved at all.** The required submission is the demo video
> plus a public, open-source GitHub repo containing the code and the instructions to run it.
> Attaching a sideloaded APK through itch.io is allowed as a bonus, but it is optional and
> does **not** replace the repo requirement. (For the store-published categories the rules
> only accept the Apple App Store, Google Play Store and Samsung Galaxy Store — that is where
> itch.io would be ineligible.)

Was the app already publicly released anywhere before Aug 1, 2026? No — nothing was ever
published in any form. There is therefore no risk of tripping the "first public version must
be released between Aug 1 and Sep 30" rule, and if you later publish to Play or the Galaxy
Store the window still covers it.

---

## 2. Next Gen requirements checklist

| Requirement | Status | How it is satisfied |
| --- | --- | --- |
| Working app that uses the RevenueCat SDK to power at least one in-app purchase | ✅ verified end-to-end on the emulator | `purchases` 10.19.1 + entitlement `premium`. Debug builds run against the RevenueCat **Test Store** — the mechanism the organizer confirmed is acceptable (§4). Logcat shows the real path, not a mock: `Using a Test Store API key` → `GET /v1/subscribers/.../offerings 200` → `Building offerings response with 3 products`, then a purchase that POSTs `/v1/receipts 200` and flips `premium` to active. It survives a force-stop because `refreshPremium()` re-reads CustomerInfo at launch. |
| Active student + academic email submitted on Devpost | ⬜ | `AnNguyen0410@csu.fullerton.edu`, entered in the **student email** field of the submission form (do not change the Devpost account email). Keep proof of enrolment handy. |
| Parent/guardian consent form | n/a / ⬜ | Only if under the age of majority where you live. Under-18 ⇒ <https://forms.gle/Gx2Cr4X8WPk9V1q77> must be completed before the window closes. |
| Video **and** repo make the RevenueCat integration, and the purchase flow, obvious | ⬜ | The organizer's explicit instruction (§4). README documents the wiring; the video shows the real purchase plus the code call chain (or the dashboard transaction). |
| Public, open-source code repository | ✅ | <https://github.com/Annguyen0410/Undo> — public, and MIT shows in the About section. `.gitignore` blocks `build/`, `.gradle`, `.kotlin`, `local.properties`, keystores, APKs, plus `.idea/` and `.freebuff/`, so no machine paths or secrets are published. |
| Repo contains all source, assets and instructions to be functional | ✅ verified on a clean clone | README covers architecture, the build command, the RevenueCat integration (files, ids, call chain) and the Test Store setup. `gradle-wrapper.jar` is committed, so `./gradlew` runs on a machine that has never seen this project; a fresh clone builds and runs **50 unit tests**. |
| Text description of features and functionality | ⬜ | Draft in §7. |
| Demo video ≤ 2 minutes, showing the app running on a device, public on YouTube/Vimeo | ⬜ | Shot list in [`VIDEO-SCRIPT.md`](VIDEO-SCRIPT.md). No third-party trademarks or copyrighted music. |
| 1024×1024 app icon | ✅ | `docs/shipaton/undo-icon-1024.png` (regenerate with `python docs/shipaton/make_icon.py`, which also writes the launcher icon from the same drawing). A 512×512 version is included for store listings. |
| Screenshot at exactly 1179×2556 px, no device frame | ✅ four committed | `docs/shipaton/undo-screenshot-1179x2556.png` (Journal), `-2` (Insights), `-3` (Check-in), `-4` (paywall). Captured on the emulator at 1080×2400 and converted by `make_screenshot.py`. §5 covers redoing them. |
| Free trial *or* promo code for judges | n/a | Only required for store-published categories. Next Gen is judged on the video and the code, and there is no judge-facing build to unlock — Test Store purchases cost nothing and need no account. |

---

## 3. What changed in the repo for eligibility

1. `app/build.gradle.kts` — the hard-coded placeholder key is gone. Debug and release builds
   read their key from `local.properties` / the environment (`REVENUECAT_TEST_KEY`,
   `REVENUECAT_GOOGLE_KEY`) and expose it as `BuildConfig.REVENUECAT_API_KEY`, so no secret is
   ever committed.
2. `RevenueCatManager` — reads the key from the new `BuildConfig` field and derives every
   premium decision from the `premium` entitlement, so a Test Store build behaves exactly
   like a store build.
3. `PaywallViewModel` — the offline demo no longer loses its "best value" flag; the fallback
   paywall picks the annual plan like the real one does.
4. `.gitignore` — `.kotlin/`, `*.jks`, `*.keystore`, `*.apk`, `*.aab` were genuinely missing;
   `build/` is now ignored at every level as belt-and-braces (`app/.gitignore` already covered
   the module). Verified before the first commit: `git check-ignore -v` confirms nothing under
   `app/build/` (which contains `kspCaches` with absolute machine paths) and no
   `local.properties` is staged.
5. `LICENSE` — MIT.
6. `README.md` — a **"How RevenueCat is integrated"** section (the five files that make up the
   purchase path, for anyone reading the code including judges), Test Store setup, Shipaton
   pointers, licence section, sample-journal note.
7. `devtools/SampleJournal.kt` + debug-only settings cards — a fresh journal is empty by
   design (notes are written by hand, and there is no per-day limit), which made the journal,
   donut and monthly trend impossible to demo, screenshot or record. *Sample journal*
   back-fills ~90 deterministic days and *Clear journal* empties the table again. Existing
   days are never overwritten. Both cards are hidden in release builds, like the
   *Simulate premium* switch.
8. Launcher icon — the app still shipped the default Android Studio green robot while the
   submission icon was the crescent moon, so the video, the screenshots and the store icon
   would all have shown a different mark from the app itself. `docs/shipaton/make_icon.py` now
   renders both from one drawing: the adaptive layers
   (`drawable-*dpi/ic_launcher_{background,foreground,monochrome}.png`), the legacy
   `mipmap-*dpi/ic_launcher.webp` + `ic_launcher_round.webp`, and `undo-icon-1024.png`. The
   themed-icon (Android 13+) silhouette is generated as a separate white layer, since a
   blurred bloom would tint into a grey smudge. Re-run it any time; it ends by checking pixel
   safety margins, and `python docs/shipaton/icon_preview.py` renders a page showing the icon
   through the circle/squircle masks — this machine has no working emulator.
9. Verified after the change: `:app:assembleDebug` succeeds, and all 15 adaptive layers ship
   byte-identical in the APK with all 10 legacy mipmaps at the right densities.
10. **Unlimited archive (Sep 18)** — the app was built around "one regret per day", which is a
    cap the product should not have. It now takes as many notes a night as you want and is
    meant to hold **decades**, so every read was rebuilt to never load the journal into
    memory:
    - `Entry` has no unique constraint on `date`; a day simply holds a list of notes, each
      with its own write time, editable and deletable with an undo.
    - The Journal reads **pages of 50** over the `(date, createdAt)` index, with a `date <=
      :ceiling` bound instead of `OFFSET`-ing past the years in front — that ceiling is what
      makes **"jump to any month that holds notes"** a range scan rather than archaeology.
    - Insights takes counts from SQL (`GROUP BY category`, `GROUP BY substr(date,1,7)`) and
      the streak from the list of distinct dates; nothing holds entries.
    - New *"From the archive"* card on Check-in (`InsightEngine.anniversary`) serves one
      evening from years ago back to the writer — chosen from the distinct dates and then
      loaded one day at a time — with *Open that night* deep-linking into the Journal at
      that date.
    - New `DraftStore`: every edit in the composer is written to its own preferences file, so
      a killed process comes back mid-sentence (verified: force-stop → relaunch → the text is
      still there, labelled with the night it was written).
    - Debug card *Twenty years* seeds ~8,700 notes across 20 years, to make the claim
      checkable instead of asserted.
11. Verified (Sep 18): `:app:assembleDebug` and `:app:assembleRelease` succeed, **36 unit
    tests** green (streak, percentages, trend, day grouping, anniversary selection, date
    ceilings — `InsightEngineTest`, `EntryGroupsTest`, `DatesTest`).
    On the emulator with 7,185 notes / 20 years: first page + exact count **~40 ms** warm,
    next page ~60 ms, jump to *Dec 2025* instant, the archive memory deep-links to
    *Sep 18, 2024*, a day with 3 notes renders as a list with per-note timestamps, and the
    draft survives a `force-stop`.
12. **UI rebuild (Sep 18)** — the app worked but looked like a prototype, which matters
    directly here: this category is judged on a video and a public repo. Rebuilt against one
    palette, one spacing scale and one panel component (`ui/components/Surfaces.kt`,
    `ui/theme/Dimens.kt`, documented in the README's *Design system* section).
    Two real bugs came out of reviewing it on the emulator rather than in the IDE:
    - the platform theme was `android:Theme.Material.Light.NoActionBar`, so with
      `enableEdgeToEdge()` the system painted **dark status-bar icons on the near-black
      canvas** — the clock and battery were effectively invisible in every capture;
    - `Scaffold(containerColor = Color.Transparent)` left `LocalContentColor` unset, so every
      `Text` that relied on the default colour rendered almost black: the Journal and
      Insights headings were unreadable on the device while looking fine in code.
    Also: the reminder no longer asks for notification permission on first launch (it asks
    when the reminder is switched on), and the monthly-trend labels are the chart's own bottom
    axis instead of a hand-laid Row that drifted out of alignment with the columns.

---

## 4. Turn RevenueCat on (the one blocking code task)

Right now `local.properties` has no RevenueCat key, so the app runs in offline demo mode and
**no purchase exists at all** — that alone fails the mandatory requirement.

1. Create a project at <https://app.revenuecat.com> (free).
2. *Apps and providers → Test configuration* → **create a Test Store** → copy the `test_…`
   API key.
3. *Product catalog → Products* → create Test Store products for **weekly**, **monthly** and
   **annual** (~$1.99 / $4.99 / $24.99 — Test Store prices are fixed at creation time and
   cannot be edited afterwards).
4. *Product catalog → Offerings* → attach all three products to the **`default`** offering as
   `$rc_weekly` / `$rc_monthly` / `$rc_annual` packages.
5. *Entitlements* → create an entitlement with id **`premium`** (exact spelling — it is
   `RevenueCatManager.ENTITLEMENT_ID`) and attach all three products to it. Without this,
   purchases succeed but premium stays locked.
6. Add the key to `local.properties` (git-ignored; **never** commit it):

   ```properties
   REVENUECAT_TEST_KEY=test_xxxxxxxxxxxxxxxxxxxx
   ```

7. Verify:

   ```bash
   ./gradlew :app:assembleDebug
   # install on a device/emulator, then:
   #  Insights → paywall shows three real plans (no "Demo mode" banner)
   #  → purchase → RevenueCat's Test Store sheet → confirm
   #  → premium unlocks: full history, monthly trend, custom reminder time, Midnight Reflection theme
   #  → the transaction and the active `premium` entitlement appear in the dashboard
   ```

   Screenshot the dashboard afterwards — it is evidence that the SDK is really powering a
   purchase, and it is the strongest thing you can show judges besides the video.

> ⚠️ Never ship a `test_…` key in a release build. Only `REVENUECAT_GOOGLE_KEY` (a real
> `goog_…` key) belongs in release builds.

### Settled ✅ — the Test Store counts (confirmed by the organizer, Sep 17, 2026)

> "Yes, for the Next Gen Award, using RevenueCat's Test Store to demonstrate a working
> RevenueCat integration is fine. You don't need a paid Apple or Google developer account or
> a live store release for Next Gen. Just make sure your demo video and public repo clearly
> show how RevenueCat is integrated and how the purchase/monetization flow works."

So the remaining risk is no longer *eligibility* — it is producing the two pieces of
**evidence** the reply asks for:

1. **In the video** — a real Test Store purchase completing, plus either a short code inset
   showing the SDK call chain or a RevenueCat dashboard inset showing the transaction and the
   active `premium` entitlement. See [`VIDEO-SCRIPT.md`](VIDEO-SCRIPT.md).
2. **In the repo** — the README names every integration point, so a judge can follow the
   purchase path without reverse-engineering it.

Two related answers settled at the same time:

- **Galaxy Store** has no 12-tester / 14-day gate — beta testing is recommended for IAP but
  not mandatory. It is still not the path, because *selling* there needs a D-U-N-S number and
  Commercial Seller Status.
- **itch.io** is optional, is allowed as an extra, and does not replace the public repo.

> The revenue-path requirement to demonstrate is "at least one in-app purchase powered by the
> RevenueCat SDK". A Test Store purchase satisfies it; a hard-coded fake paywall does not. The
> offline demo fallback in this app exists only so the build runs without a key — it must be
> clear in the video that the recorded purchase is the real SDK one, with the "Demo mode"
> banner absent.

---

## 5. Screenshot at 1179×2556

The form needs at least one screenshot at exactly 1179×2556 px with no device frame. Four are
committed, captured from the emulator at 1080×2400 and converted by `make_screenshot.py`:

| File | Screen |
| --- | --- |
| `undo-screenshot-1179x2556.png` | Journal — `7185 notes · since Jun 2006`, search, jump-to-month |
| `undo-screenshot-1179x2556-2.png` | Insights — stats, donut with real percentages, insight sentence |
| `undo-screenshot-1179x2556-3.png` | Check-in — composer, a saved note, *From the archive* |
| `undo-screenshot-1179x2556-4.png` | Paywall — three plans, real prices, 7-day-trial badge |

To redo them (the UI moves, so they will drift):

```bash
# 1. Give the screen content worth showing (Settings → Sample journal → Fill).
# 2. Capture — on the emulator:  adb exec-out screencap -p > shot.png
#    or on a real phone with Power + Volume Down.
# 3. Convert:
python docs/shipaton/make_screenshot.py shot-a.png shot-b.png ...
#    -> undo-screenshot-1179x2556.png, -2, -3, ...
```

The script scales uniformly to 1179 px wide and centre-crops the height, so a 1080×2400
capture becomes exactly 1179×2556 with **no stretching** (it refuses captures that would come
out shorter than 2556 px). A status bar in the shot is fine — "no device frame" forbids a
phone bezel graphic, not the system UI. Do not screenshot a build with the DEVELOPER cards
visible unless you want those in the submission: they are debug-only, and the Check-in screen
is above them.

One trap worth knowing: `adb exec-out screencap` taken too soon after `am start` returns the
Android 12 **splash screen** (the crescent on a plain background), not the app. Wait for the
first content frame and dump the UI to confirm before capturing.

---

## 6. Timeline (13 days)

The Play closed-testing gate no longer governs this plan — Next Gen needs no store release,
so the schedule has real slack. (If you ever want a Play listing too, that gate starts
whenever you open a new personal account: 12 testers opted in continuously for 14 days.)

| Dates | Task |
| --- | --- |
| Sep 17 ✅ | Repo swept, `LICENSE` (MIT), `.gitignore`, build-type RevenueCat keys, deterministic sample-journal seed, 1024×1024 icon — all done and committed; clean clone verified to build. |
| Sep 17 ✅ | Test Store question settled: accepted for Next Gen. |
| Sep 18 ✅ | UI rebuilt (one palette/spacing/panel system) and the archive rebuilt around unlimited notes: paged journal, jump-to-month, archive memory, draft autosave. 36 unit tests green; verified on the emulator against 20 years / 7,185 notes. |
| Sep 18 | Create the RevenueCat project, Test Store, three products, the `default` offering and the `premium` entitlement. Add `REVENUECAT_TEST_KEY` to `local.properties`. |
| Sep 18 | Build, make a real Test Store purchase, screenshot the dashboard (transaction + active entitlement). |
| Sep 18–19 | Push to GitHub **public**, confirm MIT shows in About, re-verify a clean clone, finish the README integration section. |
| Sep 19 ✅ | Icon (1024×1024 + launcher, one drawing) and four screenshots at 1179×2556 — done. Optional bonus: sideload an APK on itch.io. |
| Sep 20–23 | Record and edit the ≤2 min video (multiple takes). Upload to YouTube as **public**. |
| Sep 24–26 | Devpost description, video link, student email field. |
| Sep 27 | **Submit.** |
| Sep 28–29 | Slack — fix anything the form or a reviewer flags. |
| Sep 30 | Final re-read only. No new features, no commits that could break the repo. |

---

## 7. Description draft

**Title:** Undo — Regret Journal

**Tagline:** One question every night. See where your regrets actually cluster.

> **What it does**
>
> Undo asks a single question before bed: *"Is there anything today you wish you'd done
> differently?"* No streaks-shaming, no blank page, no AI. You write a line, tag it with one
> of eight categories (Work, Money, Health, Relationships, Time, Courage, Communication,
> Other) and rate how much it weighs on you — and you can write **as many notes in an evening
> as you need**. Nothing is capped: not the notes per night, not the age of the journal.
>
> That nightly habit produces something a normal journal never gives you: **a map of where
> your regrets cluster.** The Insights tab shows your streak, a category donut chart and one
> plain-language sentence that names the pattern — "most of what you'd change this week sits
> in Communication". Free users see the last 7 days; Premium unlocks the full history and a
> monthly trend so the pattern is visible across months, not moods.
>
> **Built to be kept for decades.** The journal is designed to hold twenty years of notes, so
> the app never loads it: the Journal loads 50 notes at a time as you scroll, with pinned
> search and category filters, and a *jump to month* control that reaches any month in the
> archive in one step. Insights counts in SQL rather than in memory. With years of history the
> app starts giving something back — a *From the archive* card on the home screen serves one
> evening from years ago back to you ("Ten years ago tonight", or whichever era the day
> selects), and opening it lands you on
> that exact night in the Journal.
>
> **Small things that matter nightly.** An unsaved draft is written to disk on every
> keystroke, so a killed app comes back mid-sentence. A note can be edited, deleted and
> undone. A reminder only fires on a night you have not written yet. The whole thing is
> offline: no account, no sync, no analytics.
>
> **How it works**
>
> Jetpack Compose + Material 3, single activity, three tabs. Room stores one table on-device —
> there is no backend, no account and no analytics. The note database is also excluded from
> Android cloud backup (settings and RevenueCat's anonymous app user ID still back up), so
> the journal is never uploaded anywhere the user did not put it; CSV export and import are
> free for that reason, because the portable file is the backup. WorkManager schedules the nightly reminder
> through a notification channel, tolerating Doze. Vico draws the donut and trend charts. The
> insight sentence is pure aggregation and category templates, computed locally.
>
> **RevenueCat integration**
>
> The paywall loads the current offering through the RevenueCat SDK, renders real store
> packages with their localised prices and lets the user purchase or restore. An active
> `premium` entitlement gates full history, the monthly trend, a custom reminder time and the
> Midnight Reflection theme; free users keep unlimited check-ins, the timeline, the 7-day
> insights, and the ability to take their journal out of the app and put it back. Purchases
> and restores each return an authoritative `CustomerInfo`, and premium state is derived from
> `entitlements["premium"]` alone.
>
> The app has not been released to a store yet, so the demo build runs against RevenueCat's
> **Test Store**: the same SDK, the same `default` offering and the same `CustomerInfo` flow,
> with sandbox transactions visible in the RevenueCat dashboard. The repository documents the
> full purchase path under *"How RevenueCat is integrated"* in the README.
>
> **Design notes**
>
> The screens should feel like a late-night room rather than a productivity dashboard: a
> "midnight observatory" palette of near-black plum, one desaturated lavender accent, a
> slowly drifting star field behind the content, solid high-contrast primary actions, and a
> serif pull-quote for the nightly question. One spacing scale and one panel component are
> shared by all three tabs. "Midnight Reflection" is the premium theme that pushes the same
> hues deeper and colder. Judges should look at the note stack on Check-in, the intensity
> control, the category donut, the jump-to-month list, and the transition between the two
> themes.
>
> **Built with** — Kotlin, Jetpack Compose, Material 3, Room, WorkManager, Vico, RevenueCat
> (`purchases` + `purchases-ui`), MIT-licensed and fully open source.

---

## 8. Copy-paste checklist for the Devpost form

- [ ] Project name: `Undo — Regret Journal`
- [ ] Tagline: `One question every night. See where your regrets actually cluster.`
- [ ] Category: **Next Gen Award**
- [ ] Student email: `AnNguyen0410@csu.fullerton.edu`
- [ ] Code repository URL: `https://github.com/<you>/<repo>` (public, MIT in About)
- [ ] Demo video URL: YouTube/Vimeo, **public**, ≤ 2:00
- [ ] Icon: `docs/shipaton/undo-icon-1024.png`
- [ ] Screenshot: `docs/shipaton/undo-screenshot-1179x2556.png` and `-2`, `-3`, `-4` (all exactly 1179×2556)
- [ ] Description: §7, mentioning the RevenueCat purchase flow explicitly
- [ ] Repo: README has the "How RevenueCat is integrated" section; the rc `default`
      offering and `premium` entitlement ids in the code match the dashboard
- [ ] Optional bonus (never a substitute for the repo): sideloaded APK on itch.io
- [ ] Guard: if under 18, the guardian consent form is submitted
