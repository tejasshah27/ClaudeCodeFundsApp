# How Skills Load in Claude Code

## Context

This document captures a Q&A conversation about how the superpowers plugin skills are loaded and invoked in Claude Code.

---

## Q: Why don't I see superpowers plugin files in my project `.claude` folder?

Plugin files don't get copied into `.claude/`. They stay in the global plugin cache and are loaded from there at runtime:

```
~/.claude/plugins/cache/claude-plugins-official/superpowers/5.0.5/skills/
```

The `.claude/` folder only contains **your own** files:
- `~/.claude/commands/` — your personal slash commands
- `~/.claude/settings.json` — your settings
- `~/.claude/plugins/` — plugin metadata + cache (skills live here, under `cache/`)

Skills from plugins are never surfaced as files in `.claude/` directly — Claude Code reads them from the plugin cache at startup and injects them into the session context.

---

## Q: How does `/superpowers:brainstorm` redirect to the brainstorming skill?

It doesn't redirect automatically. The command file (`brainstorm.md`) contains:

```markdown
---
description: "Deprecated - use the superpowers:brainstorming skill instead"
---

Tell your human partner that this command is deprecated and will be removed in the next
major release. They should ask you to use the "superpowers brainstorming" skill instead.
```

When `/superpowers:brainstorm` is invoked, Claude receives that markdown as a prompt and responds telling you it's deprecated. There's no programmatic redirect — it's just a message to the human to use the new skill name.

---

## Q: How does Claude know what to do when a skill is invoked?

When a slash command like `/brainstorming` is triggered, Claude Code loads the `SKILL.md` content and injects it into Claude's context as a prompt. Claude reads the instructions and follows them — it's just markdown text acting as a recipe.

References to other files within a skill (e.g. `spec-document-reviewer-prompt.md`, `visual-companion.md`) are plain text paths written in the markdown. Claude sees them and uses the `Read` tool to fetch them when the instructions say to.

**No code runs for this decision.** Claude is both the runtime and the decision-maker. Skill files are purely natural language instructions — the skill author trusts Claude to follow the steps and fetch referenced files at the right time.

- `SKILL.md` = main instructions loaded upfront
- Other `.md` files in the folder = loaded lazily, only when the main instructions reference them

---

## Q: When a skill is NOT explicitly invoked, who decides which skill to load?

It's a two-layer mechanism:

### Layer 1 — Session-start hook injects `using-superpowers`

The `hooks/hooks.json` session-start hook fires when Claude Code starts. It injects the full `using-superpowers` SKILL.md into Claude's context automatically. That skill contains:

> "If you think there is even a 1% chance a skill might apply to what you are doing, you ABSOLUTELY MUST invoke the skill."

This makes Claude start every session already knowing the rule: *check for skills before doing anything.*

### Layer 2 — System-reminder lists all skill names + descriptions

Every message includes a system-reminder with entries like:

```
- brainstorming: You MUST use this before any creative work...
- systematic-debugging: Use when encountering any bug...
- writing-plans: Use when you have a spec or requirements...
```

Claude reads these on every turn and pattern-matches them against your request.

### Decision flow without an explicit command

```
You type a message
    ↓
Claude sees skill list in system-reminder (name + one-line description)
    ↓
Claude pattern-matches your intent against descriptions
    ↓
Claude calls Skill tool → full SKILL.md loaded into context
    ↓
Claude follows the skill instructions
```

The `using-superpowers` hook is the enforcer — without it Claude might just answer directly. With it, skipping the skill check feels like violating an explicit rule given at session start.

---

## Q: Is the pattern matching done by the model or by Claude Code before reaching the LLM?

It's done entirely by the **LLM (Sonnet/Haiku/Opus)** — not by Claude Code.

### Exact sequence

```
You type a message
    ↓
Claude Code assembles the full prompt:
  - System prompt
  - Session-start hook output (using-superpowers content)
  - System-reminder (skill list with descriptions)
  - Your message
  - Conversation history
    ↓
This entire bundle is sent to the LLM (e.g. Sonnet)
    ↓
LLM reads everything together and decides:
  "This looks like debugging → I should call the Skill tool
   with skill='systematic-debugging'"
    ↓
LLM outputs a tool call: Skill("systematic-debugging")
    ↓
Claude Code executes it → fetches SKILL.md → injects into next LLM call
```

Claude Code is just a **transport layer** — it assembles the prompt, sends it to the LLM, and executes whatever tool calls the LLM returns.

The **pattern matching is pure LLM reasoning** happening inside the model. No keyword matching, no regex, no routing logic in Claude Code. The model reads the skill description and semantically matches it against your message — the same way it understands anything else.

**Claude Code has no idea what skill is relevant. It just runs what the model tells it to run.**
