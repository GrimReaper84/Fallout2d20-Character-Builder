# Fallout 2d20 Character Builder

A desktop character builder for **Fallout: The Roleplaying Game** (Modiphius, 2d20 system). It guides you
through character creation by the book, levels characters up, and keeps track of gear and session values
at the table.

> Unofficial fan project – see [Disclaimer](#disclaimer).

## Features

- **Books:** Core Rulebook, Winter of Atom and Wanderer's Guide (what is still missing: see
  [Roadmap](#roadmap)).
- **Character creation** step by step: origin and origin options, traits, S.P.E.C.I.A.L., Tag skills and
  skill ranks, perks (perk grid with requirements), starting level, equipment packs (entered into the
  inventory automatically). Budgets and rules are
  checked as soft hints – nothing blocks you.
- **Advancement:** add XP or milestone levels, spend skill ranks and perks, then apply them.
- **GM free edit** for free corrections after creation.
- **Rules** (Character ▸ Rules…): switch the Wanderer's Guide and Winter of Atom on or off per character and choose
  house rules (armor helmets count as headgear); both are saved with the character.
- **Gear:** weapons with target numbers, damage and weapon mods (perk bonuses such as Commando or Iron Fist
  included, situational ones listed per weapon), ammunition, apparel and armor with Damage
  Resistance per hit location (also for Mister Handy and Protectron), items and holotapes, caps, materials
  and carry weight.
- **Session values:** current HP (rads lower the maximum), Luck points, Rad Sponge points, injuries,
  notes.
- **Character sheet** export as PDF for printing.

Characters are saved as JSON files wherever you like.

## Roadmap

As of November 2026. The order can still change; ideas and wishes are welcome.

**Next release (1.5.0, finished, not released yet)**

- Perk bonuses computed into the weapon values (situational perks as hints per weapon)
- Rules window: books per character, house rules (first one: armor helmets count as headgear)
- Several characters open at once, each in its own tab
- GM compendium: equipment catalogs, character rules, roll tables and a party overview with its own party file
- Initiative lowered by carry load everywhere

**Planned next**

- Custom weapons that use Athletics and AGI (bows)
- Check the Tag skill items of robots (an Assaultron currently gets Casual Clothing through Athletics)
- Show book hints for weapons in the Weapons tab (today they appear in the Gear tab)
- **Power Armor**, including the Wanderer's Guide suits (Excavator, Hellcat, T-65)
- Developer documentation: architecture overview and flow diagrams

**Maintenance**

- Remaining static analysis findings
- Jackson 3 migration, once Jackson 2 is no longer maintained

**To be decided**

- Text export next to the PDF sheet
- Wanderer's Guide vehicles, crafting and GM creature rules (do they belong in a character builder?)

**Nice to have**

- Equipment sets: named loadouts to switch between (e.g. "combat", "town")
- Picture of the weapon type with its mod slots around it
- Compendium: add entries to a character, NPC stat blocks, export

**Not planned:** other books such as Settler's Guide or Royal Flush (the legacy import still accepts their
names and reports what it skips).

## Download and installation

Get the latest version from the [Releases](https://github.com/GrimReaper84/Fallout2d20-Character-Builder/releases)
page. Every package contains its own Java runtime – nothing else needs to be installed.

| System | Installer | Portable |
|---|---|---|
| Windows | `.msi` | `…-win.zip` |
| Linux (Debian, Ubuntu, Mint, …) | `.deb` | `…-linux.zip` |
| macOS (Apple Silicon) | `.dmg` | `…-mac-aarch64.zip` |

The packages are not code-signed, so your system may warn you the first time:

- **Windows:** SmartScreen shows "Windows protected your PC" → *More info* → *Run anyway*. The installer
  needs no administrator rights.
- **macOS:** the app is blocked at first start → right-click the app → *Open*, or allow it under
  *System Settings → Privacy & Security*.
- **Linux:** install with `sudo apt install ./fallout2d20-character-builder_<version>_amd64.deb`; the app
  appears in the menu under *Games*.

Portable zip: unpack it anywhere and start `bin/Fallout2d20CharacterBuilder` (`.bat` on Windows).

## Building from source

Requires JDK 25.

```bash
./gradlew build      # compile and run the tests
./gradlew run        # start the application
./gradlew jpackage   # installer for the current OS (build/jpackage)
./gradlew jlinkZip   # portable zip for the current OS (build/portable)
```

Development runs (`./gradlew run`) show an extra **Beta** menu that opens test characters from
`src/test/resources/samples` (and the legacy fixtures from `src/test/resources/legacy`) as unsaved copies.
Release builds never show it. To get it in an IDE run configuration, add
`-Dcharbuilder.beta=src/test/resources`. `./gradlew generateSamples` recreates the generated samples
after a save format change; other `.json` files in the folder are left alone.

## Contributing

The project is developed with [Claude Code](https://claude.com/claude-code), and everyone works from the same
rules, which are part of this repository:

- [`CLAUDE.md`](CLAUDE.md): project overview, loaded automatically
- [`.claude/skills/_shared/project-rules.md`](.claude/skills/_shared/project-rules.md): workflow, code
  conventions, building blocks, UI conventions
- [`.claude/skills/`](.claude/skills): one skill per workflow step (analysis, plan, implementation, review) and
  catalog extraction

Local setup:

1. Install JDK 25 and check that `./gradlew build` passes.
2. The rulebooks are the source of truth, and they stay out of the repository for copyright reasons. Put your
   own PDFs into `docs/rulebooks/` as `core_rulebook.pdf`, `winter_of_atom.pdf` and `wanderers_guide.pdf`, then
   create the text files with `pdftotext` (poppler-utils) in that folder:

   ```bash
   for book in core_rulebook winter_of_atom wanderers_guide; do
     pdftotext -enc UTF-8 "$book.pdf" "$book.txt"
     pdftotext -enc UTF-8 -layout "$book.pdf" "${book}_layout.txt"
   done
   ```

3. Optional: put personal notes for Claude into `CLAUDE.local.md` (gitignored).

`docs/` is gitignored; plans and analyses are created there per developer. Never copy rules text into tracked
files: only values, page references and short descriptions in your own words.

## Releasing

1. Set the version in `gradle.properties` (e.g. `version=1.1.0`) and commit. Use plain numbers
   (`MAJOR.MINOR.PATCH`, no `-SNAPSHOT`): MSI and macOS installers require a numeric version, and a test
   checks it.
2. Create and push the tag `v1.1.0` (`git tag v1.1.0`, then `git push origin v1.1.0`).
3. GitHub Actions builds the packages on Windows, Linux and macOS and creates a **draft** release.
   Review it and publish.

The release workflow can also be started by hand (*Actions → Release → Run workflow*); it then only builds
the packages as workflow artifacts.

## How this project was made

This application was written with extensive AI assistance (Claude Opus 5.5 via Claude Code), guided,
reviewed and tested by the author.

## Disclaimer

This is an unofficial fan project. It is not affiliated with, endorsed, sponsored or approved by Bethesda
Softworks, ZeniMax Media or Modiphius Entertainment. Fallout is a trademark of Bethesda Softworks LLC, a
ZeniMax Media company. Fallout: The Roleplaying Game is published by Modiphius Entertainment.

The program contains no rules text from the books – only game values, page references and short
summaries in our own words. You need the rulebooks to play.

## Credits

The application icon is the "radiation" icon of [Font Awesome Free](https://fontawesome.com) by Fonticons, Inc.,
licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/); it was recolored and placed on a dark
background. The buttons of the program use the Font Awesome Free icon font (font files under the
[SIL OFL 1.1](https://openfontlicense.org/), icons under CC BY 4.0) via [Ikonli](https://kordamp.org/ikonli/).

## License

[MIT](LICENSE) © 2026 GrimReaper84
