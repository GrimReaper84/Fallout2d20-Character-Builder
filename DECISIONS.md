# Design decisions

Lightweight architecture decision records (ADRs): what was decided and why, where the code alone does not tell. Details
of a feature stay in the code, its Javadoc and the tests.

**Rules for this file**
- Add an entry when a decision is hard to see from the code, was a real choice, or fills a gap in the books. Skip routine
  UI details.
- Never rewrite an old decision: mark it superseded and add a new entry.
- No verbatim rules text; values, page references and own words only.

**Why (r)** marks a reason reconstructed later from the context, because the original plan only recorded the decision.
Correct it if you remember better. Entries before 2026-11-04 were collected from the feature plans and read through by the
project owner on 2026-11-04.

## Project and process

### ADR-001: The rulebooks decide, not the legacy app (2026-10-14)
The app is a rebuild of an AI-generated TypeScript app; its data is checked against CRB, WOA and WG before it counts.
**Why:** the old data and rules contain errors, and its supplement content was never verified.

### ADR-002: Only CRB, WOA and WG are in scope (2026-10-15)
Other books (Settler's Guide, Royal Flush, ...) are not supported; the legacy import skips and reports them.
**Why:** only rules checked against a book that is available count.

### ADR-003: Everything is English (2026-10-14)
UI, data, code and docs; game terms as printed.
**Why:** the project goes public, and no translation is needed.

### ADR-004: `docs/` stays local (2026-10-14)
Rulebooks, plans and analyses are gitignored and exist per developer. Tracked files hold no rules text.
**Why:** not needed for publishing, and copyright.

### ADR-005: Streamlined feature workflow (2026-10-15)
One combined plan, one approval, all steps in one go, one fresh-context review per feature.
**Why:** per-step reviews and separate analysis documents were the largest token cost.

### ADR-006: Skills and project rules are shared in the repository (2026-11-03)
`CLAUDE.md` and `.claude/` are tracked; personal settings go into `CLAUDE.local.md`. Supersedes the removal of these files
before v1.0 (2026-10-16).
**Why:** a friend joins development and needs the same workflow.

### ADR-007: Legacy analysis dropped from the workflow (2026-11-03)
New features no longer derive from the old app; importing its save files stays mandatory.
**Why:** the Java app has far outgrown it.

### ADR-008: Public repository, MIT license, AI assistance named (2026-10-16)
Copyright holder GrimReaper84; the README mentions the AI assistance.
**Why:** an open hobby project for the group and others, honest about how it was made.

## Architecture

### ADR-009: Modular JavaFX app on the Java 25 LTS line (2026-10-14, 2026-10-19)
Fully JPMS with jlink/jpackage; JavaFX 25.0.4 matches the JDK, deliberately not the newest release.
**Why:** Gradle supports modules natively with manageable friction, and jlink serves distribution. The LTS line stays
supported.

### ADR-010: Stay on Jackson 2 for now (2026-10-19)
The move to Jackson 3 waits (roadmap).
**Why:** 2.x is still released; nothing forces the migration yet.

### ADR-011: Data-driven JSON catalogs, one file each, loaded once (2026-10-14, 2026-10-15)
Game content is JSON with id and source page, loaded at one static place and not passed around. Only attributes and skills
are Java enums. Passive perk effects are catalog fields.
**Why:** readable, the same format as the saves, only Jackson needed; content changes need no code.

### ADR-012: Derived values are computed, never stored (2026-10-14)
HP, Initiative, Defense, carry weight and DR are computed on demand. On legacy import, computed values win; a mismatch is
an info warning.
**Why:** storing them was a known defect of the old app: stale values.

### ADR-013: Own save format beside the legacy import (2026-10-14, 2026-10-17)
`format` plus `formatVersion` from 1, `createdWith` kept and `savedWith` updated. `schemaVersion: 3` test files stay
loadable. Files keep the `.json` extension. A legacy file is never overwritten: it opens without a path, so saving asks.
**Why:** a file's origin stays recognizable, and the structure version is independent of the app version.

### ADR-014: Keep attribute allocation, not final values (2026-10-14)
Legacy characters keep their values as a baseline; imported perks and traits are marked.
**Why:** the rules work on allocation, and already baked effects of old files must not count twice.

### ADR-015: `PlayerCharacter` aggregate with facets and a builder (2026-10-19)
The former 1547-line `Character` became an aggregate with `abilities`, `advancement`, `gear`, `startingGear`, `session`.
**Why:** two constructors with 15 and 24 parameters and clear method groups made it hard to change.

### ADR-016: Rule violations are soft hints (2026-10-15, 2026-10-16)
Budgets, requirements and clashes give hints and never block editing or saving. GM free edit removes budgets; book caps stay.
**Why:** the table decides; a builder must not stand between the GM and a ruling.

### ADR-017: No undo of Create Character (2026-10-15)
Completing is one-way, with a confirmation that lists open hints. Corrections go through GM free edit.
**Why:** starting values freeze here (Tag skills, starting level); undoing would need to reverse them.

### ADR-018: Beta samples only in development (2026-10-17)
The Beta menu needs a system property set only by the Gradle `run` task. Samples open as unsaved copies.
**Why:** testers must never overwrite fixtures, and release builds show nothing of it.

## Character rules: choices where the book is silent or unclear

### ADR-019: Ghoul Survival is an additional fourth Tag skill (2026-10-15)
Our reading of CRB p. 52. **Why:** recorded as a reading; revisit on a bug report.

### ADR-020: Super Mutant and Nightkin may lower STR/END from 7 to 6 (2026-10-15)
No book evidence for base 7. **Why:** carries the general rule of lowering an attribute by one for a point (CRB p. 50).

### ADR-021: Creation budgets must be spent completely (2026-10-15)
SPECIAL, Tags, skill points and perks; soft hints both ways. INT for skill points includes Gifted and Intense Training.
**Why:** matches the book's process while staying a hint (ADR-016).

### ADR-022: Origin, trait and level changes adjust skills automatically (2026-10-15)
Wasted Tags are freed and ranks trimmed to the cap. Perks that become invalid stay, with a hint.
**Why:** silent budget errors are worse than an adjustment; perks are deliberate choices the player should review.

### ADR-023: Tag! is its own Tag source (2026-10-15)
Allowed on any untagged skill, rank bonus capped at the rank cap, as in the book and unlike the legacy app.
**Why:** the book is the source (ADR-001).

### ADR-024: Starting level without an upper limit (2026-10-15)
XP is the minimum of the level. Above 20: previous caps + level × 50, any rarity (reading of the CRB p. 81 pattern).
**Why:** CRB p. 49 continues beyond 21.

### ADR-025: Life Giver uses current END (2026-10-15)
**Why:** the book is silent; the simplest reading.

### ADR-026: Level-up raises current HP too (2026-10-16)
Damage taken stays. A house rule: the book only names max HP.
**Why:** a level gain should not read as new damage.

### ADR-027: Staged advancement with Apply (2026-10-16)
XP adds an amount (the legacy app set the total). Ranks and perks are staged, saved, and committed by Apply, which is blocked
while advancement hints exist. Tag skills freeze at completion for starting items.
**Why:** players can try combinations and take them back until they commit.

### ADR-028: Protectron and Assaultron use humanoid hit locations (2026-10-16, 2026-10-26)
Robot armor is mapped: Optics to Head, Main Body to Torso, Arm to one arm; a robot leg piece covers one chosen leg.
Plating "All" covers all six. The book says nothing here, so this is a house rule. Assaultron plating does not count where
robot armor sits (WG p. 18); Mister Handy and Protectron keep adding.
**Why:** a player has such a character, and the book gives no table.

### ADR-029: Armor over an outfit is only a hint (2026-10-16)
Values are still computed; per location the higher armor value counts. Innate DR adds on every location.
**Why:** soft hints (ADR-016); perks say "all locations".

### ADR-030: Armor helmets as headgear is a switchable house rule (2026-10-22, 2026-11-02)
Saved in the character file and named on the PDF sheet. With it on, an armor helmet is the one head item, also against hats.
Hard-wired house rules stay fixed for now.
**Why:** the book's layers exclude it, but the group wanted the option.

### ADR-031: Books per character; switching off removes nothing (2026-11-02)
CRB is always on; gear of a disabled book gets a hint and stays. Supersedes "all characters have all books" (2026-10-16).
**Why:** a toggle must not destroy data.

### ADR-032: Handedness follows the Two-Handed quality, mods included (2026-10-31)
Perk bonuses rely on that alone. The Lever-Action Rifle therefore counts as one-handed for Gunslinger.
**Why:** one clear rule that also covers mods.

### ADR-033: Situational perks are hints, fixed ones computed (2026-10-31)
Only unconditional bonuses enter the weapon values and the sheet. Nerd Rage, Radicool and Overly Generous stay hints, not derived
from current HP or rads. Supersedes "no perk damage bonuses" (2026-10-16).
**Why:** the app cannot know the situation at the table.

### ADR-034: Mod perks are information only (2026-10-17, 2026-10-22)
Missing perks never block a mod; installation is rolled at the table. Fixed effects are computed, conditional ones are text.
**Why:** like ADR-016; the app records the result, not the crafting.

### ADR-035: Torso armor mods count double, Ballistic Weave does not (2026-10-22)
The doubling sentence stands only in the armor sections; Legion torso mods are assumed to double.
**Why:** the wording of the book.

### ADR-036: Left out or simplified on purpose
- No Vault Boy figure on the Armor tab: Bethesda artwork.
- No Caesar's Outfit and Nuka-Girl Rocketsuit: no table values; custom apparel covers them.
- Tribal NCR trait list: other book.
- Ammunition per type, no magazines (CRB p. 91); legacy magazine and pool are added to the type.
- Junk as items with component text, no salvaging.
- Consumables with effect text and quantity, no use logic.
- Tables with missing results are rolled again (legendary head 19–20, ranged 10 and 19).

## User interface

### ADR-037: Second-level UI through tabs (2026-10-16, 2026-11-02)
Each open character is a document tab with its own tabs below; the GM compendium is one more document tab. The GM toggle was
renamed "GM free edit".
**Why:** to avoid confusing it with the new compendium.

### ADR-038: GM compendium works without characters (2026-11-02)
Party rows are the GM's own entries in their own file (`fallout2d20-party`). Values can be copied once from an open character and
never write back. The player name lives only in the party.
**Why:** the GM should not need to maintain character files.

### ADR-039: Own PDF sheet, modelled on the CRB layout (2026-10-16)
Generated with PDFBox, with no official artwork. Current HP, Luck points, ammo quantity and caps are left empty on purpose.
**Why:** usable at the table, and no copyrighted template.

### ADR-040: Application icon (2026-10-19)
Font Awesome `radiation` in `#14FF14` on a near-black rounded square.
**Why:** readable down to 16 px, in the style of a Pip-Boy screen.

### ADR-041: Status on a slide panel, tab optional (2026-11-04)
A floating button opens the panel; Settings ▸ Status as Tab restores the tab, at once and without a restart. The button can be
dragged along the right edge; one shared position is remembered.
**Why:** the status stays at hand from every tab. Live switching costs little because the view rebuilds anyway.

### ADR-042: A transparent scrim closes the slide panel (2026-11-04)
A click beside the panel only closes it. The scrim covers the character view, so the menu bar and other tabs stay usable and leave
the panel open. Replaces a first version with scene event filters.
**Why:** a hand test confirmed that the filters let clicks through and could change a Tag; the scrim cannot.

## Scavenging

### ADR-043: Scavenging follows the book, not the script (2026-11-03)
Tables come from the books; `Loot-Gen-3.py` only gave the idea, flow and scale lists. Minimum rolls and AP are by the book, Luck per find.
Roll to maximum and free rolls exist on top and are marked.
**Why:** the script has wrong rows and always rolls the maximum.

### ADR-044: Scale lists are suggestions (2026-11-03)
Editable and labeled as a house rule. **Why:** the book leaves the item list to the GM (CRB p. 195).

### ADR-045: Finds are not transferred, nothing is saved (2026-11-03)
Result list with links and copy as text. Scrounger and Fortune Finder stay notes.
**Why:** the compendium knows no character; a smaller scope.

### ADR-046: Readings of the loot rules (2026-11-04)
- A detailed junk roll gives one item, not 2d20.
- Animal Products 12–20 rolls on Random Food: the book points to a list without rolls.
- Armor sets and Power Armor pieces roll a location; frames, dog armor and Vault-Tec Armor do not.
- The ammunition footnote's "Scavenger" is read as Scrounger.
**Why:** the books are unclear; confirmed by the user.

### ADR-047: Loot uses the loot table's quantities (2026-11-04)
Also for the ammunition found with a weapon, so .45 is 9+4 CD (CRB p. 200) although CRB p. 91 says 8+4. The ammunition catalog keeps 8+4.
Junk Jet comes without ammunition.
**Why:** the same find always rolls the same, with one formula syntax.

### ADR-048: Group AP is a typed field, not tracked (2026-11-04)
It adds to the test's AP; leftovers are carried by hand, with no hard cap of 6.
**Why:** the app has no session state for the group, and effects can raise the maximum.
