# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Character builder for the pen-and-paper RPG **Fallout 2d20** (*Fallout: The Roleplaying Game*) as a
JavaFX desktop application. It is a conceptual rebuild of an AI-generated TypeScript app (v0.4.5,
`/home/luca/Documents/Fallout 2d20/v0.4.5`), which serves as a **functional template only**, not as a
blueprint. The rulebooks are the source of truth.

Supported books: Core Rulebook (`CRB`), Winter of Atom (`WOA`), Wanderer's Guide (`WG`).

The project plan lives in `docs/plaene/projektplan.md` (local only, see below).

## Build & Run

```bash
./gradlew build        # Compile + test + checks (the verification step)
./gradlew test         # Run tests only
./gradlew run          # Start the application
./gradlew jlink        # Build a self-contained runtime image (build/image)
./gradlew jlinkZip     # Zip of the runtime image
```

- **Java 21** (toolchain), Gradle 9 with version catalog `gradle/libs.versions.toml`.
- **Fully modular (JPMS):** module `de.fallout2d20.charbuilder`, main class
  `de.fallout2d20.charbuilder.App`. New packages with FXML controllers must be
  `opens ... to javafx.fxml`; packages deserialized by Jackson must be `opens ... to
  com.fasterxml.jackson.databind`. Tests run on the classpath (no test `module-info`).
- Error Prone + NullAway (error level) on main code; jspecify `@Nullable` for nullable values;
  fields annotated with `@FXML` are excluded from NullAway.
- Libraries: JavaFX (controls, fxml), ControlsFX, MigLayout, Ikonli (FontAwesome 6), Jackson, JUnit 5.

## Language

**Everything is English**: UI texts, catalog data, identifiers, comments, Javadoc, skills. The project
will be published after completion. Names of game elements are spelled exactly as in the books.
(The user communicates in German; that does not change the language of project artifacts.)

## Local documentation (`docs/`, gitignored)

`docs/` is never committed. It contains copyrighted material and working notes:

| What | Path |
|---|---|
| Rulebook PDFs + text | `docs/rulebooks/` (`<book>.pdf`, `<book>.txt` for prose, `<book>_layout.txt` for tables, `\f` page breaks; `README.md` has page offsets and commands) |
| Plans | `docs/plaene/` |
| Analyses / catalog reports | `docs/analyses/` |

**Never copy rules text verbatim into tracked files** (catalogs, Javadoc, UI). Use values, page
references (`CRB p. 61`) and short descriptions in your own words.

## Planned architecture

- Data-driven **JSON catalogs** (origins, traits, perks, skills, equipment packs, weapons, armor,
  items), each entry with stable `id` and `source` (book + printed page). Books can be toggled per
  character.
- Derived values (HP, initiative, defense, carry weight, melee bonus, DR) are **computed, never
  stored**.
- Save format: JSON, `schemaVersion: 3`. Legacy saves (unversioned and `schemaVersion: 2`, example
  `/home/luca/Documents/Fallout 2d20/Characters/JC.json`) **must** stay importable; unknown names are
  skipped and reported, never crash the import.
- Two modes: character creation (budgets, validation) and advancement (XP, level-up), plus GM free edit.
- Packages by feature area (e.g. `model.character.perk`, `catalog`, `persistence.legacy`,
  `view.dialog`), not everything in the root package.

## Workflows

- **catalog-extractor**: transcribe catalogs from the rulebook texts into JSON catalog files.
- Feature workflow skills (adapted from the Chummer rebuild) follow in phase 2.

## RULES FOR CLAUDE

1. Never implement code without being explicitly told to do so. A finished plan is not an approval.
2. Stop and report after each step; scope and domain decisions belong to the user.
3. Do not start the application (`./gradlew run`): the display is the user's real desktop.
4. Do not commit unless asked.
