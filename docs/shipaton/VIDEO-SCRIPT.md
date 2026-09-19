# Demo video — 2 minutes, Next Gen Award

## Hard rules (from the official rules)

- **≤ 2:00.** Judges are not required to watch past two minutes, so the purchase flow and
  the premium payoff must both land before 2:00.
- Must show the app **running on the device it was built for** — a phone screen recording.
- Uploaded to **YouTube or Vimeo, public**, link pasted into the Devpost form.
- No third-party trademarks, no copyrighted music. Use silence, or a track you own/licensed.
- Do not use the Shipaton/RevenueCat logos or any influencer's name or likeness. (Showing a
  real screen recording of the RevenueCat dashboard is genuine product footage, not using
  their logo as branding — but do not paste a logo or wordmark on top of anything.)

Organizer's instruction for this category: *"make sure your demo video and public repo
clearly show how RevenueCat is integrated and how the purchase/monetization flow works."*
That means the purchase beat is not optional and must be legible: the real Test Store
purchase, plus **how** the SDK is wired (code call chain, or the dashboard transaction and
active `premium` entitlement). Do not let the offline demo paywall appear at all — the
"Demo mode" banner must be absent from the footage.

## Before recording

1. Build and install the debug build with the Test Store key in `local.properties`
   (see `SUBMISSION.md` §4 — Test Store is confirmed acceptable for Next Gen). Confirm a
   Test Store purchase unlocks premium — recording a broken purchase wastes a whole session.
   Check the "Demo mode" banner is gone on the paywall; if it is showing, the key did not
   reach the build and everything you record is worthless.
2. Give the journal history — the archive beats are worth more than an empty list:
   - Settings → *Developer → **Twenty years*** seeds ~8,700 notes across 20 years *and* the
     recent window, which is what makes the "jump to 2016" and "Ten years ago tonight" beats
     possible. Recruit someone to tap *Seed* a minute before you roll if you want the
     "emptied the table and refilled it" story on camera — but do not record on an empty
     journal.
   - *Sample journal → Fill* is the lighter option (~90 days). It leaves the archive memory at
     best "a month ago" and jump-to-month only a few months deep.
   - The **From the archive** card names whichever era it found first, and with a full
     twenty-year journal *every* era qualifies — so which one appears rotates with the date
     (`InsightEngine.anniversary` picks `hash(today) % erasAvailable`). Measured against a
     full journal: **19 Sep → "Five years ago tonight"**, 21 Sep → "Ten years ago tonight",
     23 Sep → "Three years", 29 Sep → "Ten years", 30 Sep → "Three months". The
     voice-over above is deliberately age-agnostic so it cannot contradict the screen — do
     not say "a decade ago" unless the card actually says *Ten*.
   - Do this *after* recording the empty first-run state if you want that beat; *Clear
     journal* empties the table again.
3. Settings → *Nightly reminder* → on, and set a custom time **2 minutes from now** if you
   want to capture the notification. Turn the notification off again afterwards.
4. Phone: portrait, Do Not Disturb on, brightness ~70%, clean status bar (charging, full
   signal), no personal notifications. Set system font size to default.
5. Record at the phone's native resolution with the built-in screen recorder. Keep the raw
   file; re-cutting from a fresh take is faster than re-recording.

## Shot list

| Time | On screen | Do this | Voice-over / caption |
| --- | --- | --- | --- |
| 0:00–0:10 | Check-in tab, top of screen | Slow scroll of the nightly question | "Every journal app asks you to write. Undo asks one question before bed — *is there anything today you wish you'd done differently?*" |
| 0:10–0:32 | Check-in composer | Type a real regret, tap a category chip, tap an intensity, save | "One line, one category, how much it weighs on you. That's the whole ritual — ten seconds, once a night." |
| 0:32–0:44 | Notes stack on Check-in | Scroll the saved notes (a night with two or three of them, each showing its own time), then tap *Add another note* | "It never judges — and a night can hold as many notes as it takes. Nothing here is capped: not the notes, not the years." |
| 0:44–1:00 | Journal tab (filled) | Scroll grouped days, type two words into search and clear it, then tap **Jump to month** and pick an old year | "Twenty years of notes is not a list you scroll. Search it, filter it — or jump straight to any month that has notes." |
| 1:00–1:16 | Insights tab, free state | Show streak, donut, the insight sentence; tap a range toggle | "Free users get the last seven days and one plain sentence that names the pattern: most of what you'd change this week sits in *communication*." |
| 1:16–1:34 | Paywall | Tap *See plans* → three real plans load → select annual → **purchase** completes | "Full history and the monthly trend live behind Premium. The paywall loads real offerings through the RevenueCat SDK — plans, localised prices and the purchase flow all come from RevenueCat, not from hard-coded strings." |
| 1:34–1:42 | Code inset (over a still of the app): `configure()` → `offerings.current` → `awaitPurchaseResult()` → `customerInfo.entitlements["premium"]` | Four short highlights, ~2s each, top to bottom of the chain | "Under the hood: configure at launch, read the current offering, purchase, then the `premium` entitlement comes back active on the customer info." |
| 1:42–1:52 | Premium unlocked | Monthly trend chart, then choose the Midnight Reflection theme and let it switch | "And once the entitlement is active, the whole trend opens up — months instead of moods." |
| 1:52–2:00 | Check-in, scrolled to **From the archive** | Show the card, then end on the nightly question | "Keep it long enough and it starts giving something back — a night from years ago, arriving on its own. Undo: one question a night." |

Total voice-over budget: ~270 words. Write it out, read it once against a stopwatch, cut
anything that does not fit.

**Instead of the code inset** you can show the RevenueCat dashboard — the new transaction and
the active `premium` entitlement — as a screen-recording inset over the same beat. Keep it
≤ 8 seconds and pick whichever you can hold steady; the dashboard is the more convincing of
the two *if* the build is legible at phone size. Screenshots of the same dashboard belong in
the Devpost description either way.

## Things to say explicitly (the judges are checking for them)

- The purchase is powered by the **RevenueCat SDK** — say the words "RevenueCat" once,
  ideally twice: when the paywall loads the offering, and when the entitlement unlocks.
- **How** it is wired, not just that it works: entitlement id `premium`, the `default`
  offering, purchase + restore, and `CustomerInfo` driving the unlock. Name these out loud or
  in captions — the organizer asked for exactly this, and a judge can then confirm it in the
  README section of the same name.
- The app is **local-first**: no account, no backend. It is a genuine differentiator and it
  answers the "where does my data go" question judges will have.
- The archive is **not capped**: no limit on notes per night, and it is built to hold
  decades. Say it while the journal scrolls or while you jump to an old month — it is a
  differentiator no judge will assume, and the repo explains how it stays fast
  (50-note pages, SQL counts, a date ceiling instead of scrolling — README *How it scales*).
- Premium is what the app *is*: full history, monthly trend, custom reminder time,
  Midnight Reflection theme. Export and import are free on purpose — worth one clause if the
  paywall or the Settings data card is on screen: a journal you cannot get out of is not a
  journal you own.

## Editing notes

- 1.0× speed, minimal zoom-ins, no fast whip transitions — the app's identity is calm.
- Captions for every line of voice-over: many judges watch muted.
- Cut any tap that misses, any waiting spinner, and the keyboard opening/closing where
  possible.
- Export 1080p, and wait for the upload to finish processing before pasting the link —
  a private or still-processing video fails the "publicly visible" requirement.

## Final checklist

- [ ] Duration ≤ 2:00 including the last frame
- [ ] Test Store purchase is visible and completes
- [ ] The integration itself is visible — code call chain or dashboard transaction (with the
      `premium` entitlement active), per the organizer's instruction
- [ ] No "Demo mode" banner anywhere in the footage
- [ ] The journal on screen has history in it (a night with more than one note, and a month
      old enough for the jump-to-month beat)
- [ ] Premium features are shown *after* the purchase
- [ ] Recorded on a phone, portrait, no device frame overlay added
- [ ] Video is public on YouTube/Vimeo and plays in an incognito window
- [ ] Link pasted into the Devpost form
