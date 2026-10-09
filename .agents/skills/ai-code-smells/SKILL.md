---
name: ai-code-smells
description: >-
  Find and fix generic AI-generated code smells in this Kotlin/Spring Boot
  codebase — restating comments, decorative section dividers, swallowed
  exceptions, needless guards/wrappers, generic variable names, copy-pasted
  boilerplate. Use when asked to clean up code smells or review recently
  written/generated Kotlin code for these tells.
---

# AI Code Smells

Generic AI-generated code has recognizable tells in comments, error
handling, and structure. This skill finds them in this repo's Kotlin source
(`src/main/kotlin/com/fcfb/arceus/`) and fixes only confirmed instances,
preserving existing behavior.

## Workflow

**Phase 1 — Find.** Search for each checklist item below across the
changed/relevant files (or the whole `src/main/kotlin` tree if asked
broadly). Report every instance as `file:line` plus a short quote. For any
category with zero instances, explicitly write "none found."

**Phase 2 — Fix.** Only fix confirmed instances. Preserve existing
behavior and intent — this is a cleanup pass, not a refactor. Don't add new
abstractions, don't "improve" code that isn't actually exhibiting one of
these smells, and don't touch generated code (DTOs mirroring a schema,
Lombok/JPA boilerplate that's structurally required) just because it looks
repetitive.

**Phase 3 — Verify.** After fixing, run this project's build/lint/test
commands to confirm nothing broke:
- `./gradlew ktlintCheck` (lint)
- `./gradlew compileKotlin compileTestKotlin` (compile check)
- `./gradlew test` (test suite)

## Checklist

### Comments
- Comments that just restate the line directly below them
- Decorative section-divider comments (`// ═══ Section ═══`, `// ==== X
  ====`)
- Narrative "first we do X, then we do Y" walkthrough comments describing
  control flow instead of explaining a non-obvious why
- Boilerplate KDoc that repeats a trivial function's name/params with no
  real information (e.g. `/** Gets the user id. @return the user id */` on
  `fun getUserId()`)
- Stale comments describing behavior the code no longer has (check the
  comment against what the code actually does now)

### Error handling & control flow
- `catch` blocks that only log and swallow an error with no user feedback
  or fallback behavior
- `catch` blocks that log and rethrow the exact same exception unchanged
  (the log adds nothing beyond what an unhandled exception would already
  surface)
- Defensive null/guard checks for states that cannot actually occur given
  the real callers (verify by checking call sites — don't assume a check is
  needless without confirming)

### Structure & naming
- Needless single-use wrapper functions that just forward to another call
  with no added logic
- Generic variable names (`data`, `result`, `temp`, `item`) where a domain
  name is obvious from context (e.g. `game`, `team`, `scorebug`, `user`)
- The same boilerplate helper copy-pasted across multiple files instead of
  extracted to one shared/imported location
- Unnecessary `suspend`/coroutine usage with no actual asynchronous work
  inside
- Redundant boolean comparisons (`=== true`, `== false`) instead of the
  boolean itself or its negation
- Placeholder `TODO`/`FIXME` comments with zero context on what's
  incomplete or why
- Leftover debug `println`/`print` statements not gated behind a dev flag
  or proper logger

## Guardrails

- Don't invent findings. A category with nothing wrong gets "none found."
- Don't flag idiomatic Spring/JPA/Lombok patterns (e.g. thin
  controller-to-service delegation, `@Transactional` wrappers) as "needless
  wrappers" — that's framework convention, not an AI smell.
- Verify guard-check findings against actual call sites before removing
  them; a check that looks redundant in isolation may guard a real caller.
- Keep diffs minimal and behavior-preserving — this is a smell cleanup, not
  a refactor or redesign pass.
