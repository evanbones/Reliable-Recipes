# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.0] - 2026-06-04

### Added

- Added additional safety checks for modded recipes.
- Added additional logging for failed recipe mutations.

## [2.0.0] - 2026-04-25

### Changed

- (26.1) Now requires latest version of RRV for the item viewer integration.

## [1.12.0] - 2026-04-23

### Fixed

- Fixed block tag removals not syncing properly.
- Improved tag modification performance.

## [1.11.0] - 2026-04-22

### Changed

- All rules now properly support regular expressions when escaped with forward slashes (`/ /`).

## [1.10.1] - 2026-04-21

### Fixed

- Performance improvements with the EMI plugin.

## [1.10.0] - 2026-04-19

### Added

- Added support for item tags in `input` and `output` filters.

## [1.9.6] - 2026-04-12

### Fixed

- Performance improvements.

## [1.9.5] - 2026-04-12

### Fixed

- Fixed replacement arrays not working on 26.1.

## [1.9.4] - 2026-04-11

### Changed

- Adjust dependencies on 26.1.

## [1.9.3] - 2026-04-10

### Changed

- Updated API to closer match pre-26.1 functionality.

## [1.9.2] - 2026-04-10

### Fixed

- Fixed recipe removal using dev mode and RRV in 26.1.

## [1.9.1] - 2026-04-09

### Fixed

- Fixed TPS issues with certain mods (like Overgeared).

## [1.9.0] - 2026-04-08

### Changed

- Reverted experimental recipe output stripping.
- Improved recipe removal logic.

## [1.8.4] - 2026-04-06

### Changed

- Fixed repair blocking not working on 1.21.

## [1.8.3] - 2026-04-06

### Changed

- Final tweaks for Create recipe matching.

## [1.8.2] - 2026-04-06

### Changed

- (Hopefully) fix Create recipe matching.

## [1.8.1] - 2026-04-06

### Changed

- More experimental fixes for recipe matching.

## [1.8.0] - 2026-04-06

### Changed

- Improved recipe matching with modded recipes and updated API for Reliable Remover.

## [1.7.0] - 2026-04-03

### Added

- Added item replacement to the API.

## [1.6.2] - 2026-03-15

### Added

- Added tag whitelist config option.

## [1.6.1] - 2026-03-15

### Added

- Expanded API options for selective tag removals.

## [1.6.0] - 2026-03-09

### Changed

- Flattened `recipe` and `tag` rules out of nested blocks.
    - Existing rules will be automatically converted, but refer to the wiki in the future for the new syntax.

### Fixed

- Fixed issues with nested tags.
- Fixed EMI recipe removal being too aggressive in certain cases.

## [1.5.0] - 2026-02-28

### Added

- Added `prevent_repair` action to prevent items from being repaired in Anvils, Crafting, etc.

### Fixed

- Fixed boolean logic in filters.

## [1.4.5] - 2026-02-28

### Fixed

- Improved recipe type parsing.

## [1.4.4] - 2026-02-27

### Fixed

- Rollback 1.4.3 changes.

## [1.4.3] - 2026-02-27

### Fixed

- Fixed log spam with EMI.

## [1.4.2] - 2026-02-27

### Changed

- Remove Item Obliterator capabilities.

### Fixed

- Fix Reliable Remover integration.

## [1.4.1] - 2026-02-27

### Changed

- Attempt to fix recipe hot reloading.

## [1.4.0] - 2026-02-27

### Changed

- Internal refactors/API changes.

## [1.3.10] - 2026-02-19

### Fixed

- Removed items are now removed from EMI grindstone and repair recipes.

## [1.3.9] - 2026-02-18

### Changed

- Changed `/reliable_recipes_undo` to `/rrecipes_undo`.

## [1.3.8] - 2026-02-18

### Fixed

- Fix possible concurrency issues causing log spam.
- Safely fail recipe matches for dynamic registries.

## [1.3.7i] - 2026-02-09

### Fixed

- Fix tags not applying until running /reload.
- Fix possible issue with missing item IDs.

## [1.3.7h] - 2026-02-09

### Fixed

- Fix tag display in EMI.

## [1.3.7] - 2026-02-09

### Fixed

- Fix incompatibility with Immersive Engineering.
