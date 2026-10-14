---
name: java-analyzer
description: Determines the current state of the Java code in the Fallout 2d20 Character Builder for a feature – what already exists (also unexpectedly), which building blocks can be reused, what is missing – compares it with the legacy analysis and writes docs/analyses/<feature>_java_analysis.md. Use this skill as the second workflow step after the legacy analysis, or whenever it should be clarified before planning how far a feature is already implemented ("what do we already have of that", "is X already in"). Changes no code.
---

# Java state analysis

You establish what **actually** exists in the Java rebuild for a feature and prepare it so that
`javafx-planner` can build on it.

Why this step matters: `docs/feature_checklist.md` and memory regularly lag behind the code. In the
Chummer rebuild, things were repeatedly considered open that had long been implemented and tested.
Only the code counts, not the documentation.

**Read first:** `.claude/skills/_shared/project-rules.md`, especially sections 3 (paths) and 7
(building blocks).

## Inputs

| Input | Source |
|---|---|
| Legacy analysis | `docs/analyses/<feature>_legacy_analysis.md`, especially the hand-off. If missing, analyze anyway and note the missing baseline |
| Java code | `src/main/java/de/fallout2d20/charbuilder/...`, `module-info.java`, tests under `src/test/...` |
| Catalog data | `src/main/resources/de/fallout2d20/charbuilder/catalog/*.json`, catalog reports in `docs/analyses/*_catalog.md` |

## Procedure

1. **Search top-down and bottom-up:** from the entry points (`App`, main window controller, character
   model, persistence/legacy importer) to the domain classes, and conversely by grep for domain terms,
   catalog ids, JSON field names and test names. Report "not found" only after both directions.
2. **Assign a status per legacy finding** (table below). The key question is whether Java fulfils the
   same *domain* responsibility, not whether a class has a similar name.
3. **Name reusable building blocks:** which existing classes are the model (project rules section 7),
   and where do they *not* fit? Example: a field is `final`, a dialog has no room for an extra choice,
   a package is not `opens` for Jackson. Exactly these spots become planning decisions later.
4. **Check the catalogs:** does the data the feature needs exist, is it complete for the scope, does a
   catalog report list open questions?
5. **Judge tests by the behavior they secure**, not just by existence.
6. **Record documentation drift:** if `docs/feature_checklist.md` does not match the code, say so; the
   correction is part of the result.
7. **Write the file** `docs/analyses/<feature>_java_analysis.md`, then stop and report briefly (in
   German). Most important finding first, especially if more exists than expected.

For small scope, or when the Java state is already known from the current session, the compact form
suffices (sections 1, 2, 5, 6 below).

## Status per legacy finding

| Status | Meaning |
|---|---|
| Present | covered in domain terms (evidence: class/method/test) |
| Partial | foundations exist, scope missing |
| Divergent | present, behaves relevantly differently (deliberate? book-compliant?) |
| Missing | searched specifically, nothing found (name the search terms) |
| Not relevant | legacy behavior is legacy debt or another book |

## Output: `docs/analyses/<feature>_java_analysis.md`

```markdown
# Java analysis: <feature>

## 1. Summary
(degree of implementation in one sentence; surprising findings first)

## 2. Reusable building blocks
(per block: class, what it is the model for, where it does not fit)

## 3. Relevant Java components
(table: component | path | responsibility | tests)

## 4. Comparison with the legacy analysis
(table: legacy finding | Java counterpart | status | planning implication)

## 5. What is missing
(numbered, phrased in domain terms, with the affected places)

## 6. Hand-off to javafx-planner
- Confirmed starting point
- Building blocks to use
- To implement / adapt (incl. catalogs, persistence, legacy import, module-info)
- To decide (domain/technical), with the code places that influence the decision
- Documentation drift
```

No code and no finished class designs here. Hints at signatures or structures only as far as the
planner needs them for a decision.
