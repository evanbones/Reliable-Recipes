### Added

- Backported `brewing` recipe type from 26.3.
- Added support for modifying brewing recipes using recipe rules.

### Changed

- Switched to YACL instead of Cloth Config.
- Invalid removals now default to unmatched (previously, invalid filters would match everything).
- Major backend cleanups.

### Fixed

- Fixed crash when attempting to delete certain invalid recipes.