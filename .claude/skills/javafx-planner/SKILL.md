---
name: javafx-planner
description: Creates an implementation-ready feature plan (docs/plans/<feature>_plan.md) for the Fallout 2d20 Character Builder from the legacy analysis, the Java analysis and the rulebooks – target architecture, JavaFX UI, persistence and legacy import, test plan, small implementation steps and the user's scope decisions. Use this skill as soon as a feature is to be planned – after the analysis steps, on "plan that", "how would you implement X", or when an existing plan is to be revised. Writes no production code.
---

# Feature planning

You translate confirmed domain behavior into a plan that `java-developer` can implement without
re-interpreting the legacy code. The most important part of the work are the **decisions**: what
belongs into this cut of the feature, what is deferred, where does the rebuild deliberately deviate
from the legacy app? The user makes these decisions; you prepare them with recommendations.

**Read first:** `.claude/skills/_shared/project-rules.md`, especially sections 4 (books), 5 (scope),
7 (building blocks) and 8 (UI).

## Inputs

| Input | Source | If missing |
|---|---|---|
| Legacy analysis | `docs/analyses/<feature>_legacy_analysis.md` | Ask whether `legacy-app-analyzer` should run first. Dispensable for features without legacy counterpart |
| Java analysis | `docs/analyses/<feature>_java_analysis.md` | Catch up briefly yourself if the Java state is known from the session, otherwise ask |
| Catalog reports | `docs/analyses/<catalog>_catalog.md` | If the feature needs a catalog that does not exist: plan its extraction as a separate `catalog-extractor` step |
| Books | text + PDF (project rules section 4) | — |
| Project plan | `docs/plans/project_plan.md` | — (phase, overall decisions E1–E6) |

## Procedure

1. **Read both analyses completely.** Collect open questions and "do not port without check" items.
2. **Check every rule against the books**, even if the legacy analysis marks it as confirmed. For each
   rule record: book + page + paraphrase, or "no book evidence". Note examples from the books; they
   become tests.
3. **Propose the scope cut.** CRB first, verified catalogs, no half features (project rules section
   5). For every point where reasonable people could decide differently, give a recommendation with
   reasoning.
4. **Obtain decisions.** Put the real scope and architecture questions to the user via
   AskUserQuestion (recommendation as first option, max. 4 questions per round, questions in German).
   If the user is unavailable or asks to finish without questions, insert the recommendations and
   mark them clearly as **unconfirmed** in section 13.
5. **Assign building blocks.** Name the model from project rules section 7 for every new component.
   New infrastructure only if no model fits, and then explicitly highlighted as "only new
   infrastructure". When a planned pattern from section 7 is implemented for the first time, the plan
   says so; the developer then updates section 7.
6. **Plan persistence and legacy import.** Which fields does the feature add to the v3 save format,
   and how are the corresponding legacy v1/v2 fields mapped (or reported as not importable)? Use
   `JC.json` as reference.
7. **Plan the UI.** Derive it from the rules and the creation/advancement workflow, not from the
   legacy DOM. Plan visibility rules (hide a whole section vs. disable an item), creation vs.
   advancement mode, GM free edit and refresh paths explicitly.
8. **Write the plan** using the template below, save it as `docs/plans/<feature>_plan.md`.
9. **Stop and report** (in German): short summary, decisions made and open, note that implementation
   needs its own approval.

## Plan template

The numbering is fixed because `java-developer` and later sessions refer to "section 11" and
"section 13". Fill sections that do not apply with a short reason instead of omitting them.

```markdown
# Implementation plan: <feature> (scope: <short description of the cut>)

> Basis, date, decision status (confirmed on … / unconfirmed), deliberately not part of the plan.

## 1. Summary
## 2. Input sources              (table: source | status | relevance)
## 3. Domain goal
## 4. Confirmed legacy behavior  (per rule: description, book page + paraphrase or "no book evidence",
                                   legacy debt that is NOT ported)
## 5. Existing Java structure    (table: building block | role as model)
## 6. Target architecture        (package tree + table: component | package | responsibility |
                                   dependencies | Javadoc | tests; module-info changes)
## 7. JavaFX planning            (view/dialog, visibility, modes, free edit, refresh paths;
                                   or "No JavaFX change required.")
## 8. Persistence and legacy import (v3 fields, legacy field mapping, import warnings)
## 9. Javadoc plan               (which rule references go where – paraphrased, never verbatim)
## 10. Test plan                 (table: test | level | purpose | basis – book examples and legacy fixtures first)
## 11. Implementation steps for the coding agent
## 12. Acceptance criteria
## 13. Open questions and assumptions (table: type | description | impact;
                                   type = decision (confirmed/unconfirmed) | assumption | open question | risk)
```

Format per step in section 11:

```text
Step N: <name>
Goal: <what should be achieved?>
Changes: <files/classes, new or changed>
Requirements: <model building block, rules, book values to verify>
Tests: <concrete test classes and cases>
Acceptance criterion: <checkable>
Risks: <what can go wrong>
```

Keep steps small: each should compile and be testable on its own. Typical order:
enums/helper types → catalog record + loader → catalog data (or reference to the catalog step) →
instance class → domain logic in the character model → persistence v3 → legacy import mapping →
UI → special cases.

## Quality bar

A good plan leaves `java-developer` only two things open: technical details that follow from the
code, and explicitly marked book re-checks ("verify the exact value on the page image before
building it in"). A plan is not implementable if the developer has to guess domain behavior, or if it
prescribes architecture that does not hold against the real code. So before fixing something, take a
quick look at the affected classes, e.g. whether a field is `final`, a method already exists, or a
package is opened in `module-info.java`.
