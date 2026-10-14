---
name: java-developer
description: Implements an approved implementation plan (docs/plans/<feature>_plan.md) of the Fallout 2d20 Character Builder in Java/JavaFX code – model, catalog loading, persistence and legacy import, UI and tests – and verifies with ./gradlew build. Use this skill whenever the user explicitly approves implementation after a finished plan ("leg los", "setz das um", "implement step 3") or orders concrete code changes for features, review findings or bug fixes in this project. Not for pure analysis or planning.
---

# Java implementation

You turn a plan approved by the user into code. The plan is the leading basis: it already contains
the user's scope decisions, and they must not be silently renegotiated.

**Read first:** `.claude/skills/_shared/project-rules.md` (workflow, book check, conventions,
building blocks, verification). The rules there apply in full.

## Precondition

Implement only with an explicit order from the user. If there is only a plan but no "go", do not
start; ask.

## Inputs

| Input | Source |
|---|---|
| Plan (leading) | `docs/plans/<feature>_plan.md` – especially section 11 (steps), 12 (acceptance), 13 (user decisions) |
| Background | `docs/analyses/<feature>_legacy_analysis.md`, `..._java_analysis.md`, catalog reports (for unclear points in the plan) |
| Order | scope according to the user (all steps or individual ones) |

For review findings instead of a plan, the implementation orders of `java-code-quality-reviewer`
(allowed/forbidden changes, behavior to keep) act as the plan.

## Procedure

1. **Read the plan completely**, including section 13. The user decisions there are binding.
2. **Read the affected code** before writing: the model classes named in the plan, the character
   model, persistence and legacy importer, `module-info.java`, existing tests of the area. Goal:
   continue in the style of the code base, not introduce a new architecture.
3. **Redeem the book reservations of the plan.** Plans often say "verify the exact value against the
   book before building it in" (catalog values, names, requirements). Do it now, with page images for
   tables (project rules section 4). If the book differs from the plan, the book wins; report the
   deviation.
4. **Implement step by step** in the plan's order: data structures → domain logic → persistence and
   legacy import → UI. Compile after larger steps (`./gradlew compileJava -q`) to catch errors early.
   New packages: update `module-info.java` (`opens` for FXML/Jackson).
5. **Write the tests from the test plan.** Recompute book examples exactly, cover edge cases, test
   domain logic with synthetic data (create records directly), test loaders against the real catalog
   JSON, test the legacy import against real legacy files.
6. **Verify:** `./gradlew build`. Determine test count and result from `build/test-results`. Do not
   start the app.
7. **Follow-up** according to project rules section 10 (`docs/feature_checklist.md`). If this
   implementation introduced a pattern that section 7 of the project rules lists as "planned",
   replace the entry with the real class.
8. **Final review in fresh context.** After a green build, start a separate agent (Agent tool,
   `general-purpose`, without the context of this session) that applies the skill
   `java-code-quality-reviewer` in **diff mode** to the changes of this implementation. The order to
   the agent names: the plan (`docs/plans/<feature>_plan.md`), the base commit or "uncommitted
   changes", the instruction to read `.claude/skills/_shared/project-rules.md` and the reviewer
   SKILL.md first, and that it must **change nothing** and only report. Reason: whoever just wrote the
   code hardly questions their own design decisions; a fresh context is more critical. The findings
   are **not** implemented on your own initiative but presented in the final report; the user
   approves them. **Skipped** if the order itself was implementing review findings (otherwise an
   endless loop) or the user explicitly waives it.

## Handling plan deviations

Plans sometimes contain assumptions that turn out to be untenable against the real code. Then:

- Choose the smallest deviation that achieves the plan's **domain goal**.
- Justify it in the code (Javadoc) and in the final report.
- If the deviation affects a **user decision** from section 13 or the domain scope, ask first instead
  of deciding yourself.
- Additional book rules that come up during implementation and are clearly documented may be
  implemented along with it if they are small. Highlight them in the report.

## Do not

- No refactorings outside the feature, no global reformatting, no new dependencies without a plan.
- No deleting or weakening of tests.
- No writing git actions (no commit, add, stash, ...). Git is read-only; suggest a commit message.
- Change existing catalog values only if the book proves it. Name the consequences for saves and the
  legacy import in the report.
- No verbatim rules text in code, Javadoc, catalog data or UI.

## Final report

Short and readable for the user, **in German**, in this order:

1. **Result in one sentence** and build status (tests total/new, failures).
2. **What works now:** phrased in domain terms, with book pages of the implemented rules.
3. **Deviations from the plan** with reasons (new book findings, technical constraints, renamed data
   and their consequences). "None" if none.
4. **Open points:** what was deliberately not done or still needs clarification.
5. **Final review (step 8):** the review agent's findings briefly with severity, each with your own
   assessment (agree/disagree, briefly justified), and the question which ones to implement.
6. Note that nothing was committed (with a suggested commit message) and that the UI was only
   compiled, not looked at.

No file list of every changed file unless the user asks for it. The important new classes may be
named in the text.
