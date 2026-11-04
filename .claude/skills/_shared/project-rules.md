# Shared project rules – Fallout 2d20 Character Builder

Applies to every workflow skill (`java-analyzer`, `javafx-planner`,
`java-developer`, `java-code-quality-reviewer`, `catalog-extractor`). Each skill reads this file
before it starts. Many rules were carried over from the Chummer rebuild, where they were learned the
hard way.

## Contents

1. Project in one paragraph
2. Workflow and approvals
3. Artifacts: where things live
4. Rulebooks as the source of truth
5. Scope policy
6. Code conventions
7. Proven building blocks
8. UI conventions
9. Verification
10. Follow-up after an implementation

---

## 1. Project in one paragraph

Started as a conceptual rebuild of an AI-generated TypeScript/Tauri character manager (v0.4.5) for **Fallout:
The Roleplaying Game (2d20)** as a **JavaFX** desktop application (Java 25, Gradle, fully modular
JPMS, Error Prone + NullAway, JUnit 5, Jackson, ControlsFX, MigLayout, Ikonli). The project has long outgrown
the legacy app: new features are not derived from it, the rulebooks decide; only its save files must keep
importing. Books in scope: Core Rulebook (`CRB`), Winter of
Atom (`WOA`), Wanderer's Guide (`WG`). Everything in the project is **English**; the project is public.
Architecture and build: see `CLAUDE.md`.

## 2. Workflow and approvals

A feature runs through four steps, each with its own skill:

```
java-analyzer → javafx-planner → java-developer → java-code-quality-reviewer
 (Java state)    (plan)           (implementation) (final review, diff mode)
```

`catalog-extractor` is a side workflow: it creates and checks catalog data (perks, weapons, ...)
from the books. A feature plan names the catalogs it needs; their extraction is its own approved step.

The last step runs **automatically at the end of every implementation**: after a green build,
`java-developer` starts a separate agent with fresh context that reviews the diff (focus: needless
complexity, over-abstraction, duplication). It only reports; the user decides which findings get
implemented. Skipped when the implementation itself consisted of review findings.

**Streamlined mode (user decision 2026-10-15, active until revoked):** to save tokens, one feature runs
as (1) **one combined document** `docs/plans/<feature>_plan.md` – Java state, book check, scope
decisions and implementation steps (the separate `_java_analysis.md` is skipped); the user approves it once; (2) **all implementation steps in one go**, `./gradlew build`
after every step, no stop in between; (3) **one** fresh-context review at the end of the feature instead
of one per step; the user picks the findings. Catalog extraction may run inside step (2) when the plan
names it. Domain decisions are still asked before or in the plan.

**Stop and report after every step** (outside streamlined mode). The user approves each step individually. A finished plan is
*not* an approval to implement; code is written only when the user explicitly says so ("leg los",
"setz das um", "implement step 3"). Not even as a "small fix" on the side.

**Open domain or scope decisions belong to the user.** Ask with AskUserQuestion, recommendation as
the first option. Small technical details that follow clearly from code or conventions: decide and
mention them in the result.

**Git is read-only.** Never commit, stage, stash, push or change branches; the developer alone does
writing git actions. When a commit makes sense, suggest a commit message to the user.

**Language of communication:** reports and questions to the user are in the language the user writes
in. All files (code, data, skills, docs) are in English.

## 3. Artifacts: where things live

| What | Path |
|---|---|
| Project plan (phases, decisions) | `docs/plans/project_plan.md` |
| Java analysis | `docs/analyses/<feature>_java_analysis.md` |
| Catalog report | `docs/analyses/<catalog>_catalog.md` |
| Feature plan | `docs/plans/<feature>_plan.md` |
| Roadmap (shared) | `README.md`, section "Roadmap" |
| Feature status, detailed | `docs/feature_checklist.md` |
| Rulebooks | `docs/rulebooks/` (PDF, `<book>.txt`, `<book>_layout.txt`, see section 4) |
| Legacy save examples | `src/test/resources/legacy/` |
| Java sources | `src/main/java/de/fallout2d20/charbuilder/...` |
| Catalog data (JSON) | `src/main/resources/de/fallout2d20/charbuilder/catalog/` |

`<feature>` is a short English snake_case slug (`perks`, `equipment_packs`, `legacy_import`). Each
skill reads its predecessors' artifacts from these paths and writes its own there, so the next step
can continue in a new session without loss of context. `docs/` is local to each developer and never
committed: create missing folders as needed. A missing plan, analysis or checklist of an earlier feature
is not an error; the code counts.

## 4. Rulebooks as the source of truth

**Setup:** everyone provides their own PDFs as `docs/rulebooks/core_rulebook.pdf`, `winter_of_atom.pdf`
and `wanderers_guide.pdf` (`<book>` below) and creates the text files with `pdftotext` (poppler-utils):
`pdftotext -enc UTF-8 <book>.pdf <book>.txt` and `pdftotext -enc UTF-8 -layout <book>.pdf <book>_layout.txt`.
If the books are missing, ask the user; never fill the gap from memory.

The books decide, **not** the legacy app. The legacy app was generated by an AI: its data and rules
contain errors, and its supplement content (Settler's Guide, Royal Flush, parts of Wanderer's Guide)
was never verified. Its README and code comments ("Core Rulebook pp. 59–73") are claims, not
evidence. Every rule and every catalog value is checked against the book before it counts as
confirmed.

**Search:** `grep -n "<term>" docs/rulebooks/<book>.txt`. Search all three books when the source is
unclear. Try spelling variants (`Boy/Girl`, plural forms, hyphenation) before concluding "not in the
book".

**Page:** PDF page = number of `\f` before the line + 1; printed page = PDF page − offset
(CRB 2, WOA 2, WG 4; other editions of a PDF may differ, compare with the page footer). Always cite the
printed page.

**Tables:** the reading-order `<book>.txt` scrambles table columns. Read tables from
`<book>_layout.txt`; if still ambiguous, render the page image
(`pdftoppm -f N -l N -r 110 -png <pdf> <scratchpad>/page`) and read it. The text layer is digital, so
names and numbers are reliable once the column assignment is clear.

**Cite:** rules in analyses, plans and Javadoc with book, page and a short paraphrase, e.g.
`CRB p. 52: a character gains one perk per level`. Javadoc and other tracked files never quote rules
text verbatim (copyright, public repository); `docs/` analyses may quote short passages.

**Invent nothing.** Catalog data only from a source that was actually read, never from memory.

## 5. Scope policy

- **CRB first.** WOA and WG come in their own phase, selectable per character via the book toggle.
  Content from other books (Settler's Guide, Royal Flush, ...) is out of scope: mark it "deferred,
  other book" and let the user decide.
- **Small, verified catalogs first** if a feature needs a catalog that does not exist yet; completing
  the catalog is a `catalog-extractor` task.
- **No half features.** Rather leave out a category entirely than offer it without its core mechanic.
- **Legacy saves must keep loading.** Unversioned and `schemaVersion: 2` files are imported by the
  legacy importer; every feature that touches saved data states how its legacy fields map. Unknown
  names are skipped and reported, never crash the import.
- **Own save format (since 2026-10-17, released with v1.0.0):** `format: "fallout2d20-character-builder"`,
  `formatVersion` (structure version, starts at 1), `createdWith`/`savedWith` (app versions); files of
  the 1.0.0 test builds (`schemaVersion: 3`) are read as format 1. Since the release every written
  format must stay readable: new optional fields are fine; an incompatible change raises
  `formatVersion` and reads the older versions (migration in `CharacterStore`/`CharacterMapper`).
  Mention every format change in the result.
- **Derived values are computed, never stored** (HP, initiative, defense, carry weight, melee bonus,
  DR). Stored legacy values are only compared on import.
- Open follow-ups and deferred items go into `docs/feature_checklist.md`.

## 6. Code conventions

- **One top-level type per file.** No nested/inner classes, records, enums or interfaces — not even
  `private`. Helper types get their own file, package-private if only used locally. (Standing user
  rule from the Chummer rebuild.)
- **Packages by feature area** (`model.character.perk`, `catalog`, `persistence`,
  `persistence.legacy`, `view.dialog`, ...). Never put a class into the root package if a feature
  package fits.
- **JPMS:** a new package with FXML controllers needs `opens <pkg> to javafx.fxml`; packages read or
  written by Jackson need `opens <pkg> to com.fasterxml.jackson.databind`. Exports only for what other
  modules really need.
  This includes packages of types Jackson only reads *inside* a record (enums such as `Skill`,
  nested records). Tests run on the classpath and cannot see a missing `opens`;
  `ModuleOpensTest` checks it from the Jackson roots – add new top-level Jackson types (new
  catalogs, new save roots) to its `JACKSON_ROOTS`.
- **Explicit nullness annotations (user preference):** every reference type in a signature is
  annotated explicitly with jspecify `@NonNull` or `@Nullable`, although non-null is NullAway's
  default, so the nullness is visible at a glance. Applies to parameters, return types, fields and
  record components of **all** methods and constructors, private ones included; type-use position (`public @NonNull String abbreviation()`,
  `record CatalogRef(@NonNull String id, @Nullable String legacyName)`). Not on primitives, `void`,
  local variables or lambda parameters. Generic type arguments only where they can be null
  (`List<@Nullable String>`). Nested types take the annotation after the outer type
  (`Map.@NonNull Entry<K, V>`), otherwise javac rejects it. Model: `model.character.CatalogRef`.
- **Null handling:** jspecify `@Nullable`, NullAway runs as an error. Check nullable fields
  **directly** (`if (field == null) return;`); no local helper variable just for NullAway. Only if a
  lambda captures the field and NullAway really fails the build, solve it structurally.
- **Javadoc** for public and domain-relevant APIs: purpose, rule reference (book + page +
  paraphrase), preconditions, deliberate deviations from the legacy app. Do not repeat the method
  name.
- **English everywhere:** identifiers, comments, Javadoc, UI texts, catalog data. Game terms exactly
  as in the books (`Tag skill`, `Luck Points`, `Damage Resistance`).
- **Domain logic in the model**, not in controllers. One central method per rule; no duplicates
  between model and view.
- **Modern Java (user preference 2026-10-16):** prefer the current idiom of the toolchain's Java version
  over older equivalents: `getFirst()`/`getLast()`/`reversed()` (sequenced collections) instead of
  `get(0)`/`get(size() - 1)`, `Stream.toList()`, `List.of`/`Map.of`, records, switch expressions and
  pattern matching (`instanceof Foo foo`, record and type patterns in `switch`), text blocks,
  `Math.clamp` only where its exception for min > max cannot occur. Third-party APIs without the new
  methods (e.g. Jackson's `JsonNode.get(0)`) stay as they are.
- **Decimal values** (e.g. item weights below 1 lb) with `BigDecimal` or a fixed integer unit, rounded
  at exactly one domain-defined place. No `double` sums.

## 7. Proven building blocks

This section grows with the project. Until a pattern exists in the code, the entries are the
**planned** shape; once a feature introduces it, replace "planned" with the real class as the model.

| Task | Model |
|---|---|
| Catalog entry | `catalog.origin.OriginData`: immutable record with `id`, `name`, `sources` (list of `catalog.Source` = `Book` + printed page) and structured effect fields; validated in the compact constructor; JSON fields marked `@JsonProperty(required = true)` |
| Catalog loader | `catalog.origin.OriginCatalog` (also `catalog.trait.TraitCatalog`, `catalog.perk.PerkCatalog`; `catalog.equipment.EquipmentCatalog` loads several files – packs, Tag skill items, trinkets – and checks their completeness): static class, the only place its catalog is loaded (static field, loaded on first use), lookups `all()`/`byId()`/`available(books)`; reads via `catalog.CatalogReader.readList` (fails loudly on broken data); new catalog package needs `opens … to com.fasterxml.jackson.databind` |
| Character instance | planned: class referencing its `XData` plus mutable state (rank, choices, equipped). Until a catalog exists, selections reference it via `CatalogRef` (id + legacy name), e.g. `PerkSelection`, `TraitSelection` |
| Character aggregate | `model.character.PlayerCharacter` (aggregate root: origin, traits, perks, books, modes; edits touching several parts) with its facets `Abilities` (S.P.E.C.I.A.L., skills, Tags; one computing method per value: `attributeValue`, `skillRank` – extend these, do not add parallel ones), `Advancement` (level, XP, staged level-up), `Gear` (weapons, ammunition, apparel, items, caps), `StartingGear` (pack selection, sync into the gear), `Session` (vitals). Each facet owns its state and is reached by `character.<facet>()`; no delegate methods on the root; mode guards (`requireCreation` …) are package-private on the root. Built from stored parts with `PlayerCharacter.builder()` (defaults = new empty character) |
| Standard layout | `view.Layouts`: presets `page()`, `fillingPage()`, `halves()`, `column()`, `row()`, `bare()`, `table(n)`, `tableWithGrowingLast(n)`, `wideTable(n)`, `details(extra)`, `dialogForm()`, cells `cell()`, `span(n)`, `top()`, `filling()`, `indented(n)`, `spaced()`, `sectionStart()`, spacing constants; typed MigLayout constraints (`LC`/`AC`/`CC`), no constraint strings in new views |
| Creation rules | `model.character.creation.OriginRules`, `TraitRules`, `AttributeRules`, `SkillRules`, `PerkRules`, `EquipmentRules`: `problems(character)` → `List<CreationProblem>` (soft hints, creation mode only, never block editing/saving); one rules class per area; `creation.CreationRules.problems(character)` collects them (plus the name hint) for the overview and the completion confirmation; `PlayerCharacter.completeCreation()` itself does not check them. Budgets as `…Left(character)` and `canRaise`/`canLower` for the UI (`AttributeRules`, `SkillRules`); rank cap in `Abilities.skillRankCap()`. Creation edits go through `PlayerCharacter` mutators that throw `IllegalStateException` once creation is complete |
| Editable overview section | `view.OriginSection`, `view.TraitSection`, `view.LevelSection`, `view.ItemDetails` (both modes; typed text → `onEdited`, picks → `onChanged`), `view.AttributeSection`, `view.SkillSection`, `view.PerkSection` (grid of `view.PerkCard`s) (−/+ via `view.StepButtons`: Ikonli icon buttons with wrapper tooltip naming the reason when disabled) (checkboxes; unusable ones disabled, tooltip on a wrapper because disabled controls show none): controls call the model, then `onChanged`; `MainWindowController.onChanged` marks dirty and calls `view.CharacterView.refresh()` via `Platform.runLater`. Since G1 step 1 the sections are placed by `CharacterView`: header bar `CharacterHeader` (name, origin, level, XP, create/advancement actions) and one class per tab (`StatsTab`, `StatusTab`, `PerksTab`, `WeaponsTab`, `ArmorTab`, `GearTab`, enum `CharacterTab`); rule hints carry a `creation.HintArea` and are shown in their tab, which gets a warning marker Choice controls use `view.DisplayConverter` |
| Staged advancement | after creation, level-up points are spent as staged ranks (`SkillState.stagedRanks`, `PerkSelection.stagedRanks`, saved): they count everywhere, can be taken back (`Advancement.stageSkillRank`, `PlayerCharacter.removePerkRank`, `Advancement.discard`) and become permanent with `Advancement.apply()`; budgets in `Advancement.skillRanksLeft/perksLeft`, hints and `canRaise`/`canLower`/`canApply` in `model.character.advancement.AdvancementRules`; UI `view.AdvancementSection`, `view.AdvancementSkillSection` |
| Catalog or custom entry | `model.character.weapon.CarriedWeapon`: exactly one of catalog ref or custom record (`CustomWeapon`, free-text effects), plus quantity and note; computed view per entry (`WeaponAttack.of`) handles both; unknown legacy names become custom entries with an info warning (`persistence.legacy.LegacyWeaponImport`). Same pattern for apparel: `model.character.apparel.WornApparel` / `CustomApparel`, values via `WornApparel.values()` (with mods), totals in `ArmorResistances.of` |
| Mods on an entry | weapons: `CarriedWeapon.mods` (`Map<ModSlot, CatalogRef>`), fitting rules `WeaponMods`, computed values `ModdedWeapon`; apparel: `WornApparel.mods` (`Map<ApparelModSlot, CatalogRef>`), `ApparelMods`, `ModdedApparel`. Unknown or misfitting stored mods are kept but have no effect; perks needed are information only. UI: one section per entry with a wrench button per slot (`view.WeaponModSection`, `view.ApparelModSection`) opening the generic `view.dialog.ModPickerDialog` (via `WeaponModDialog`/`ApparelModDialog`); changed values bold with the base value as tooltip |
| Add dialog | `view.catalog.CatalogPage<T>` (category combo box after the tab's tree groups, search, `TableView` sorted by name, header click re-sorts, row tooltip, double click/Enter adds) + `view.dialog.AddButtons` ("Add" keeps the dialog open, "Add & close", "Close"; each add goes to the model at once, status line); custom tab as an own form class (`CustomWeaponForm`, `CustomApparelForm`); the tab passes `categoryOf` + ordered group names (tree enums stay package-private) and an `onAdd` returning the entry's index (`Gear.addWeaponStacked` / `addItemStacked` join plain catalog stacks) |
| Persistence | `persistence.CharacterStore` + DTO records (`CharacterData` …) + `CharacterMapper` (only place knowing model and DTOs); new fields: DTO record field + mapper both ways + round-trip test in `CharacterStoreTest` |
| Derived values | `model.character.derived.DerivedStats.of(character)`: computed on every call, never stored; origin, trait and perk effects come from catalog effect fields (`OriginData`, `TraitDefinition`, `PerkDefinition`), applied in `DerivedStatsCalculator` |
| Overlay panel | `view.SlidePanel`: slides in over a base node from a floating button; a click outside only closes it |
| Rolling on book tables | `catalog.loot.LootCatalog` (tables with roll ranges, quantity formulas, links, rows sending to other tables; gaps fail on loading), `model.scavenging.Dice` (d20 sums, Combat Dice, quantity formulas), `LootRoller`, `ScavengeSearch` (rules, block reasons for the UI as text); tests with `FixedDice` |
| Legacy import | `persistence.legacy.LegacyCharacterImporter`: tree-model import, `ImportWarning` instead of silent drops, unported fields stay in `LegacyImport.payload`; a feature that ports a legacy field takes it out of the payload (own helper class for a larger area, e.g. `persistence.legacy.LegacyEquipmentImport`) |

Prefer existing patterns and extend them only when they do not fit. Keep changes to base classes
small and mention them in the result.

## 8. UI conventions

- Plan the JavaFX UI from the rules and the workflow of character creation and advancement.
- **Unusable items stay visible but disabled** in lists (e.g. perks whose requirements are not met),
  with the reason as tooltip. The user sees that they exist and why they are locked.
- **No `setDisable()` from live state in a build method** if the view is not rebuilt when that state
  changes. Check in the click handler instead and show a warning. (Was a real bug in Chummer.)
- **Hints/limits:** `Notifications.create()...showWarning()` (ControlsFX); soft warning instead of a
  hard block unless the rule clearly forbids it. GM free edit must be able to override validation.
- **Both modes** in every feature: character creation (budgets, validation) and advancement (XP,
  level-up). Free edit for GM corrections.
- Grids with MigLayout, icon buttons via Ikonli (FontAwesome 6) with the text as tooltip. New control
  types: check the stylesheet so they do not fall back to the default Modena look.

## 9. Verification

- **`./gradlew build`** (compile + tests + checks) is the verification. Report the number of tests
  and failures.
- **Do not start the app** (`./gradlew run`): the display is the user's real desktop and there is no
  click automation. The user checks UI behavior.
- No TestFX/UI test infrastructure without an explicit request. Build domain logic so it is testable
  without UI.
- **Book examples as unit tests** (e.g. a sample character's derived values), plus edge cases.
- **Legacy import tests** use real legacy files as fixtures (`src/test/resources/legacy`; they contain no
  rules text).
- Never delete or weaken tests to get green.

## 10. Follow-up after an implementation

- `README.md` roadmap: update it when an item is finished, added or dropped.
- `docs/feature_checklist.md` (if you keep one): update status and notes of the area, record deferred items.
- In the final report, name deviations from the plan, new book findings and open points clearly.
- Do not commit (Git is read-only); suggest a commit message instead.
