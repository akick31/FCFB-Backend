# FCFB-Backend handoff (written 2026-11-03)

Branch `add-animations-to-plays`. Working tree clean as of writing; last commit `7d2e487a merge main`. Andrew commits everything himself; never commit, push or tag, and never add AI attribution.

## Working rules
- No inline `//` comments; thin controllers; one class per file; reuse shared helpers; never weaken a failing test.
- Checks: `./gradlew ktlintFormat ktlintCheck test`. If the compiler runs out of memory: `./gradlew --stop`, then add `-Dorg.gradle.jvmargs=-Xmx3g -Pkotlin.daemon.jvmargs=-Xmx3g`.
- Local backend boots against the shared dev DB `arceus_dev`, and Flyway migrations apply on boot. Never touch prod `arceus`. Never edit a migration that may have booted (V67 checksum is -592088876); add a new one.
- Concept renders come from the real renderer (a throwaway test copied into `src/test`, then deleted), never hand-drawn mockups.

## Done and committed or merged
- Win probability fix (per-play win chance after the play, WPA from the previous play), `GET /history/weeks` and `/history/week` with biggest swing.
- Stadium roadmap Phase 2: M1 attendance, M2 surface and mowing (per team field), M3a long-kick perspective and tilt, M3b domes (`DomeStyle`: NONE, SUITES, FABRIC, WINDOWED), M4 `GET /appearance-preview/game-field?gameId=` (`GameFieldPreviewService`, cached, `X-Field-Width/Height/Margin` headers).
- Migrations V66 (bowl stadiums), V67 (surface), V68 (dome_style), V69 (moves surface and mowing to team_field/game_field).
- `/error` is permitted in `WebConfig` so the real exception shows instead of a masked 401.
- Merge of `main`, ported by hand into the refactored layout: final-play two-point fix (`GameConditions`, `PlayUpdateService`), game reopen (`GameReopenService`, `POST /game-finalization/reopen?gameId=`, privileged), win/loss revert (`TeamService`/`UserService`), `ScheduleService.markGameAsUnfinished`, `InvalidGameReopenException`. 1912 tests passed.

## Do these first after the Mac restarts
1. Restart the backend so V68 and V69 apply, then check the dome, surface and crowd-switch renders live.
2. Admin `POST /win-probability/calculate/all` once, to recompute stored WPA. Until then biggest swings read 99-100%.
3. `/history/week?season=12&week=2` returned "Authentication required". Andrew is investigating. With `/error` now permitted, the real exception should show in the backend console. Around FCFB on the scoreboard and the History page depend on it.
4. First load of `game-field` took about a minute. Probably logo fetching on first render; unmeasured. Check whether the navbar ticker's slow load shares a cause.

## Backlog
- **M5 matchup previews:** `GET /appearance-preview/matchup?scheduleId=&view=FIELD|KICK`, built from a transient Game made from the schedule row; a Preview modal on upcoming schedule rows and in the Matchup Previewer. Plan: `~/.claude/plans/misty-wobbling-brook.md`.
- **Phase 3:** automated time slots when a week ends (rankings, elo; weekday games primetime; networks follow conference TV deals); slots drive day/dusk/night lighting; climate-based weather with an admin override on the game page (week 1 is Labor Day week; domes skip weather).
- Reopen is not a full undo: it does not revert bowl results, the week advance, record-setting from `recordService`, or the stored game look and field snapshots. Decide whether to fix.
- `/stadium/merge` endpoint still exists although its UI was removed; decide whether to delete it.
- Coach stats are empty on dev until Stats Management "generate all" runs (needs Andrew's OK to write to dev).
- Jenkins `BOT_SERVICE_KEY` must be set and the bot must send `X-Service-Key`: a hard deploy blocker.
- `PlayAnimationService.scrimmageOnly` is on, so play replays 404 for non-scrimmage games.
- Data quirks: Navy shows "AAC" and Arkansas State "SBC" as division; the Colonial conference logo is missing.
- A later `git merge main` will still conflict on the old pre-refactor paths; keep this branch's side, since the logic is already ported.

## Merging the 2026-11-14 main hotfix into add-animations-to-plays
Main got chart colors, forgone PAT (kneel/spike) and coach-stats sub games. When merging main in:
- Charts: main's `service/fcfb/chart/ChartColor.selectMatchup` ports fcfb-website's `pickMatchupColors` (hue family, neutral spread 40, RGB distance 90, white then accent fallbacks, default team colors #004260/#888888). Replace the branch's `rendering/chart/ChartColor.selectPair` (ColorSimilarity-based) with it, keep `ChartRendererBase.selectMatchupColors` used by both renderers, and move `ChartColorTest` to `rendering/chart`, replacing the branch's version.
- Forgone PAT: `GameRules.isForgonePointAfter`/`isPointAfterAttempt`, `PointAfterPlayProcessor.forgoPointAfter`, `PlayService` SPIKE/KNEEL split, `PlayProcessingUtils.updatePlayValues` uses `game.currentPlayType != PAT`, `GameService` updatePlayType/updateClockStopped/rollbackPlay, `GameStatsCalculator` spike pass attempts, `WinProbabilityService` forgone PAT branch. Recorded as playCall KNEEL/SPIKE, result Scenario.KNEEL/SPIKE, actualResult NO_GOOD. Re-apply in the refactored GameService split (`PlayUpdateService` etc.).
- Coach stats: `CoachStatsService.getCoachStats(coach, includeSubGames)`, `GameRepository.findNonScrimmageGamesByCoachDiscordId`, `/coach-stats?includeSubGames=`.
