# Fallout 2d20 Character Builder

A desktop character builder for **Fallout: The Roleplaying Game** (Modiphius, 2d20 system). It guides you
through character creation by the book, levels characters up, and keeps track of gear and session values
at the table.

> Unofficial fan project – see [Disclaimer](#disclaimer).

## Features

- **Books:** Core Rulebook, Winter of Atom, and the Wanderer's Guide origins (more Wanderer's Guide content
  is planned).
- **Character creation** step by step: origin and origin options, traits, S.P.E.C.I.A.L., Tag skills and
  skill ranks, perks (perk grid with requirements), starting level, equipment packs. Budgets and rules are
  checked as soft hints – nothing blocks you.
- **Advancement:** add XP or milestone levels, spend skill ranks and perks, then apply them.
- **GM mode** for free corrections after creation.
- **Gear:** weapons with target numbers and damage, ammunition, apparel and armor with Damage Resistance
  per hit location (also for Mister Handy and Protectron), items, caps, materials and carry weight.
- **Session values:** current HP (rads lower the maximum), Luck points, Rad Sponge points, injuries,
  notes.
- **Character sheet** export as PDF for printing.
- **Import** of save files from the earlier web-based character manager.

Characters are saved as JSON files wherever you like.

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

Requires JDK 21.

```bash
./gradlew build      # compile and run the tests
./gradlew run        # start the application
./gradlew jpackage   # installer for the current OS (build/jpackage)
./gradlew jlinkZip   # portable zip for the current OS (build/portable)
```

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

## License

[MIT](LICENSE) © 2026 GrimReaper84
