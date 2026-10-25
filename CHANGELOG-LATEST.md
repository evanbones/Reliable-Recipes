### Added

- `items` can now be used in place of `output` on `remove_recipe` rules.

### Fixed

- `remove_recipe` rules with nothing to filter by no longer remove every recipe. They are now skipped with a warning in
  the log.