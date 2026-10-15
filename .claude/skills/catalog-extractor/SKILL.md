---
name: catalog-extractor
description: Transcribes a catalog (perks, traits, origins, skills, equipment packs, weapons, armor, apparel, items, ...) from the Fallout 2d20 rulebook texts into the project's JSON catalog files, with source book and page for every entry, table verification against the page image, and a diff against the legacy v0.4.5 data. Writes a report to docs/analyses/<catalog>_catalog.md. Use this skill whenever catalog data has to be created, completed or checked against a book ("extract the Core perks", "add the Winter of Atom weapons", "check the equipment packs against the book", "are the legacy perk requirements correct"), even if the skill is not named explicitly. Does not write Java code.
---

# Catalog extraction

You turn rulebook content into catalog data the application loads at runtime. The rulebooks are the
source of truth. The legacy app (v0.4.5) is only a cross-check: its data was AI-generated, partly
wrong, and its supplement entries were never verified.

Why this is its own step: Fallout 2d20 is dominated by catalogs (≈100 Core perks, dozens of weapons,
armor pieces and packs per book). Getting values exactly right and traceable to a page is the
largest part of the work, and it is different from planning or implementing a feature.

**Read first:** `.claude/skills/_shared/project-rules.md` (paths, book codes, conventions),
especially section 4 (rulebooks) and 5 (scope).

## Inputs

| Input | Location |
|---|---|
| Catalog + scope | from the user, e.g. "Core perks", "Winter of Atom weapons, Small Guns only" |
| Rulebook text | `docs/rulebooks/<book>.txt` (reading order) and `<book>_layout.txt` (tables), pages separated by `\f` |
| Rulebook PDF | `docs/rulebooks/<book>.pdf` (for page images) |
| Page offset per book | `docs/rulebooks/README.md` |
| Legacy data | `/home/luca/Documents/Fallout 2d20/v0.4.5/src/data/*.ts` |
| Existing catalog | `src/main/resources/de/fallout2d20/charbuilder/catalog/<catalog>.json` |

Book codes: `CRB` (Core Rulebook), `WOA` (Winter of Atom), `WG` (Wanderer's Guide).

## Procedure

1. **Locate the pages.** Search the text (`grep -n`) for the chapter heading and a few entry names.
   Compute the PDF page (`\f` count before the line + 1) and convert it to the printed page using the
   offset from `docs/rulebooks/README.md`. Citations always use the **printed** page.
2. **Read the full section**, not just the hits. Perks and items often continue across pages, and
   sidebars contain exceptions ("robots cannot take this perk").
3. **Tables:** the reading-order `<book>.txt` scrambles table columns. Read tables from
   `<book>_layout.txt` (same page breaks). If a row is still ambiguous, render the page
   (`pdftoppm -f <N> -l <N> -r 110 -png <pdf> <scratchpad>/page`) and read the image. Never guess a value.
4. **Check the schema.** If the catalog file already exists, follow its structure exactly. If it does
   not, propose a schema in the report (fields, types, example entry) and **stop and ask the user**
   before writing data. A schema is hard to change once Java code and save files depend on it.
5. **Write the entries** (conventions below). Keep the book's order unless the schema says otherwise.
6. **Diff against the legacy data:** entries missing on either side, differing values (ranks,
   requirements, damage, ...), differing names. Record each difference and which side the book
   supports. Legacy names that differ from the book go into `legacyNames` so old saves still import.
7. **Validate:** the file parses (`python3 -m json.tool <file> > /dev/null`), ids are unique, every
   entry has at least one `sources` item. If a catalog test exists, run `./gradlew test`.
8. **Write the report** to `docs/analyses/<catalog>_catalog.md`, then stop and report briefly:
   number of entries, open questions, notable legacy errors (report to the user in German).

## Entry conventions

- `id`: stable kebab-case identifier derived from the printed name (`adamantium-skeleton`). Save files
  reference ids, so **never rename an existing id**. If the printed name changes, change `name` only.
- `name`: exactly as printed in the book (spelling, capitalization, slashes such as
  `Action Boy/Girl`).
- `sources`: list of `{ "book": "CRB", "page": 61 }`, the printed page where the entry starts, one
  item per book that describes the entry (e.g. Child of Atom in WOA and WG). Model: `origins.json`.
- Mechanical values as structured fields, not as prose: requirements as objects
  (`{ "attribute": "END", "min": 7 }`, `{ "level": 1 }`), ranks as numbers, damage as numbers plus
  effect list. Requirements that cannot be expressed structurally go into `requirementNote` and are
  listed in the report.
- `summary`: a short description **in your own words** (one or two sentences, enough to choose at the
  character creation screen). **Never copy rules text verbatim.** The repository will be public, and
  the book text must stay in the local `docs/` folder only.
- `legacyNames`: optional list of names used by the legacy app or legacy save files.
- No invented entries, no values from memory. Anything not found in the book stays out of the data
  and goes into the report as an open question.

## Report template

```markdown
# Catalog: <catalog> (<book codes>, pages <from>–<to>)

> Date, scope, schema status (existing / proposed, confirmed on ...).

## 1. Summary            (entries written, open questions, legacy errors found)
## 2. Sources            (book | printed pages | PDF pages | verified by image: yes/no per table)
## 3. Schema             (only if new or changed: fields, types, example)
## 4. Legacy diff        (table: entry | field | legacy | book | page)
## 5. Not representable  (requirements/effects kept as notes, with reason)
## 6. Open questions     (table: entry | question | impact)
```

## Scope

- One catalog, or one clearly bounded part of it, per run. Large catalogs (Core weapons) can be split
  by category.
- Effects of perks/traits are **not** modeled here. Only data. Whether and how an effect is computed
  (HP bonus, carry weight, DR) is decided in the feature plan; the catalog only records it in a
  structured field once the plan defines one.
- Do not touch Java code, tests or other catalogs. Git is read-only: never commit or stage.
