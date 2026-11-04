# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Character builder for the pen-and-paper RPG **Fallout 2d20** (*Fallout: The Roleplaying Game*) as a
JavaFX desktop application. It started as a conceptual rebuild of an AI-generated TypeScript app (v0.4.5,
not part of this repository) and has long outgrown it: new features are no longer derived from the old app.
Its save files must still import. The rulebooks are the source of truth.

Supported books: Core Rulebook (`CRB`), Winter of Atom (`WOA`), Wanderer's Guide (`WG`).

The roadmap is in `README.md`, the setup for new contributors in its "Contributing" section. Design decisions with
their reasons are in `DECISIONS.md`.

## Build & Run

```bash
./gradlew build        # Compile + test + checks (the verification step)
./gradlew test         # Run tests only
./gradlew run          # Start the application
./gradlew jlink        # Build a self-contained runtime image (build/image)
./gradlew jlinkZip     # Zip of the runtime image
```

- **Java 25** (toolchain; JavaFX 25 LTS line), Gradle 9 with version catalog `gradle/libs.versions.toml`.
- **Fully modular (JPMS):** module `de.fallout2d20.charbuilder`, main class
  `de.fallout2d20.charbuilder.App`. New packages with FXML controllers must be
  `opens ... to javafx.fxml`; packages deserialized by Jackson must be `opens ... to
  com.fasterxml.jackson.databind`. Tests run on the classpath (no test `module-info`).
- Error Prone + NullAway (error level) on main code; jspecify `@NonNull`/`@Nullable` written
  explicitly on every reference type in signatures, private ones included (see project rules,
  section 6);
  fields annotated with `@FXML` are excluded from NullAway.
- Libraries: JavaFX (controls, fxml), ControlsFX, MigLayout, Ikonli (FontAwesome 6), Jackson, JUnit 5.

## Language

**Everything is English**: UI texts, catalog data, identifiers, comments, Javadoc, skills. The project
is public. Names of game elements are spelled exactly as in the books.
(Reports and questions go to the user in the language they write in; that does not change the language
of project artifacts.)

## Local documentation (`docs/`, gitignored)

`docs/` is never committed and exists per developer. It contains copyrighted material and working notes.
Everyone provides their own rulebooks (README, "Contributing"); plans and analyses are created as needed:

| What | Path |
|---|---|
| Rulebook PDFs + text | `docs/rulebooks/` (`<book>.pdf`, `<book>.txt` for prose, `<book>_layout.txt` for tables, `\f` page breaks; page offsets and commands in project rules section 4) |
| Plans (of current work; finished ones go to `docs/plans/archive/`) | `docs/plans/` |
| Analyses / catalog reports (of current work; older ones in `docs/analyses/archive/`) | `docs/analyses/` |

**Never copy rules text verbatim into tracked files** (catalogs, Javadoc, UI). Use values, page
references (`CRB p. 61`) and short descriptions in your own words.

## Local setup

Personal settings go into `CLAUDE.local.md` (gitignored, loaded automatically). The fixtures in
`src/test/resources/legacy` cover the legacy import; the old app itself is not needed for development.

## Planned architecture

- Data-driven **JSON catalogs** (origins, traits, perks, skills, equipment packs, weapons, armor,
  items), each entry with stable `id` and `source` (book + printed page). Books can be toggled per
  character.
- Derived values (HP, initiative, defense, carry weight, melee bonus, DR) are **computed, never
  stored**.
- Save format: JSON with `format: "fallout2d20-character-builder"`, `formatVersion: 1` and the app versions
  `createdWith`/`savedWith` (`DECISIONS.md`, ADR-013); 1.0.0 test-build files
  (`schemaVersion: 3`) still load. Legacy saves (unversioned and `schemaVersion: 2`, examples in
  `src/test/resources/legacy`) **must** stay importable; unknown names are
  skipped and reported, never crash the import.
- Two modes: character creation (budgets, validation) and advancement (XP, level-up), plus GM free edit.
- Character model: `model.character.PlayerCharacter` (aggregate root) with the facets `abilities()`,
  `advancement()`, `gear()`, `startingGear()`, `session()`; built with `PlayerCharacter.builder()`.
- Views use the presets of `view.Layouts` (typed MigLayout constraints), no constraint strings.
- Packages by feature area (e.g. `model.character.perk`, `catalog`, `persistence.legacy`,
  `view.dialog`), not everything in the root package.

## Workflows

Shared rules for all skills: `.claude/skills/_shared/project-rules.md`.

Feature workflow (stop and report after each step, the user approves each one):

1. **java-analyzer** – what the Java code already has
2. **javafx-planner** – implementation plan from the books, with the user's decisions
3. **java-developer** – implementation after explicit approval, verified with `./gradlew build`
4. **java-code-quality-reviewer** (diff mode) – automatic final review by a fresh agent; reports only

Streamlined mode (user decision 2026-10-15, to save tokens): one combined analysis + plan document,
one approval, all implementation steps in one go, one review at the end of the feature. Details in
`.claude/skills/_shared/project-rules.md` section 2.

Side workflow: **catalog-extractor** transcribes catalogs from the books into JSON catalog files.
`java-code-quality-reviewer` can also be used on its own to review existing code.

## RULES FOR CLAUDE

1. Never implement code without being explicitly told to do so. A finished plan is not an approval.
2. Stop and report after each step; scope and domain decisions belong to the user.
3. Do not start the application (`./gradlew run`): the display is the user's real desktop.
4. Git is read-only for Claude: never commit, stage, push or change branches; suggest the command to
   the user instead.
