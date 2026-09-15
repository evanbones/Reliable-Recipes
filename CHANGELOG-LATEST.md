### Added

- Added JSON recipe loading directly from the `reliable_recipes/` folder, with support for standalone recipe files in subdirectories.
- Added the `add_recipe` action.
- Added `remove_output` as an alias for removing recipes by output.

### Fixed

- Fixed `replace_output` not modifying recipes when `target` is omitted.
- Fixed rare service loading crash.