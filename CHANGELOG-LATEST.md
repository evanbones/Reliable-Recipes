### Added

- Custom recipes now respect Fabric and NeoForge load conditions (`fabric:load_conditions` and `neoforge:conditions`).

### Changed

- Logging improvements.
- Recipe files laid out like a data pack now override the recipe they mirror, instead of being added under the `reliable_recipes` namespace.

### Fixed

- Fixed recipes with a hidden input being removed before KubeJS (and similar mods) could replace that input on 1.21.1 and below.
- Fixed brewing recipes not reloading when KubeJS is installed on 1.21.1 and below.
- Fixed custom recipes that reuse an existing recipe's ID breaking recipe loading on 1.21.2+.
