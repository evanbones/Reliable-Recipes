### Added

- `add_to_tag` action, which adds items or blocks to existing tags.

### Fixed

- Special recipes (suspicious stew, firework rockets, shulker box dyeing, etc.) that make a hidden item can no longer
  be crafted in crafting tables, crafters, or autocrafters, so they no longer eat the ingredients.
- `remove_recipe` now also removes recipes that other mods add while recipes load.
- Recipes that a script (e.g. KubeJS) adds back under a removed recipe's ID are no longer hidden from EMI and JEI
  (1.21.1 and older).