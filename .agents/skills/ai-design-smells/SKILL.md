---
name: ai-design-smells
description: >-
  Find and fix generic "AI SaaS" design tells in this project's user-facing
  output — Discord embeds/messages, API error responses, controller-generated
  text, README/docs. Use when asked to clean up code smells, review generated
  content for AI-isms, or polish anything a user (Discord member, API
  consumer, README reader) actually sees.
---

# AI Design Smells

Generic AI-generated content has recognizable visual and writing tells. This
skill finds them in this repo's actual user-facing output — not in normal,
intentional design choices — and fixes only confirmed instances.

This repo's user-facing surfaces: Discord embeds/messages (
`src/main/kotlin/com/fcfb/arceus/service/discord/`), API responses/error
messages (`controllers/`, DTOs), emails (`service/email/`), README/HELP.md,
and scorebug images. Skip any category below that doesn't apply to a given
surface and say so explicitly in the report.

## Workflow

**Phase 1 — Find.** Search the relevant surfaces for each checklist item
below. Report every instance as `file:line` plus a short quote. For any
category with zero instances, explicitly write "none found" — do not skip
silently and do not stretch to manufacture a finding.

**Phase 2 — Fix.** Only fix instances confirmed in Phase 1. Preserve the
existing behavior and intent of the message/output. Don't redesign a
Discord embed, error format, or doc section that isn't actually exhibiting
one of these smells — matching an existing, deliberate house style is not a
smell. Don't introduce new abstractions to fix a one-off string.

## Checklist

### Visual
- Purple/violet gradients or other generic "AI SaaS" color choices (embed
  colors, hex values)
- Glowing/gradient-clipped text effects
- Overuse of a single generic icon/emoji (✨ 🚀 ⚡ 🎯) slapped onto messages,
  embeds, or headers where it isn't part of an established convention (e.g.
  this project already uses specific emoji for specific game events —
  that's normal, not a smell)
- Decorative box-drawing dividers in output (`═══`, `----`, `***` as visual
  separators in a Discord message or log)

### Writing / copy
- Em dashes (—) used as a stylistic tic in generated strings
- Middle dots (·) as separators
- "not just X, it's Y" / "whether you're X or Y" constructions
- Marketing buzzwords: unlock, unleash, elevate, leverage, seamless,
  streamline, supercharge, cutting-edge
- Abstract "solves a problem" phrasing instead of concrete, specific
  descriptions (e.g. a vague error message where the actual failure reason
  is known and could be stated directly)
- Formulaic rule-of-three lists that pad rather than inform
- Duplicated taglines/boilerplate copy-pasted across multiple
  messages/embeds/docs instead of stated once

## Guardrails

- Don't invent findings to fill out the checklist. A category with nothing
  wrong gets "none found."
- Don't flag stylistic choices that are clearly intentional and consistent
  house style (e.g. a consistent brand color used everywhere, a fixed emoji
  used consistently for one specific event type).
- Don't redesign working, non-smelly output while in the area.
- Keep fixes minimal: change the smelly string/color/format, not the
  surrounding structure.
