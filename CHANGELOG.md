# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
