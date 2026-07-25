# Reliable Recipes

<a href='https://files.minecraftforge.net'><img alt="forge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/forge_vector.svg"></a>
<a href='https://fabricmc.net'><img alt="fabric" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/fabric_vector.svg"></a>
<a href='https://neoforged.net/'><img alt="neoforge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg"></a>

A powerful, developer-friendly utility designed for manipulating recipes and tags through simple JSON configuration.
Reliable Recipes allows modpack creators to effortlessly add, remove, or modify recipes and tags using standard JSON
files, without the need for complex scripting (looking at you, KubeJS!).

---

## Getting Started

Reliable Recipes watches a specific folder in your Minecraft instance for JSON files:

* **Location:** Place your config files in `./config/reliable_recipes/` (e.g. `my_recipe_changes.json`).
* **Format:** Rules are defined within a single JSON array. Each rule must specify an `"action"`.

```json
[
  {
    "action": "remove_recipe",
    "mod": "examplemod",
    "type": "minecraft:crafting_shaped"
  }
]
```

---

## Recipe Actions

Once you've filtered which recipes to modify, you apply an action using the `"action"` property.

### 1. `remove_recipe` (or `remove`)

Completely removes the matching recipes from the game.

```json
{
  "action": "remove_recipe",
  "id": [
    "minecraft:wooden_pickaxe",
    "minecraft:wooden_hoe"
  ]
}
```

### 2. `replace_input`

Scans ingredients and replaces target items/tags with replacements.

* **`target`**: The item ID or tag (e.g. `#minecraft:logs`) to find.
* **`replacement`**: The item ID or tag to use instead. Can be a string or an array of items.

```json
{
  "action": "replace_input",
  "target": "minecraft:stick",
  "replacement": [
    "minecraft:bamboo",
    "minecraft:stick"
  ],
  "mod": "minecraft"
}
```

### 3. `replace_output`

Changes the result of matching recipes.

* **`replacement`**: The new item ID for the output.

```json
{
  "action": "replace_output",
  "replacement": "minecraft:golden_apple",
  "id": "minecraft:cake"
}
```

### 4. `prevent_repair`

Blocks specific items from being repaired across all standard repair methods.

```json
{
  "action": "prevent_repair",
  "target": "minecraft:diamond_pickaxe"
}
```

### 5. `set_repair_material`

Overrides the repair material required to repair a specific tool or weapon in the Anvil.

```json
{
  "action": "set_repair_material",
  "target": "minecraft:diamond_sword",
  "material": "minecraft:dirt"
}
```

---

## Filters

Filters determine which recipes are affected. A recipe must match **all** provided filter fields to be selected.

| Field    | Description                                     | Example                                        |
|----------|-------------------------------------------------|------------------------------------------------|
| `output` | The registry name of the item produced.         | `"minecraft:stone_pickaxe"`                    |
| `id`     | The specific ID of a recipe.                    | `"minecraft:black_bed_from_white_bed"`         |
| `mod`    | The mod ID that owns the recipe.                | `"farmersdelight"` or `["create", "mekanism"]` |
| `type`   | The recipe type.                                | `"minecraft:smoking"`                          |
| `input`  | Matches if the recipe contains this ingredient. | `"minecraft:stick"` or `"#minecraft:logs"`     |

## Tag Modifications

Strip tags from items or empty tags entirely. This helps clean up JEI/EMI displays or isolate items.

### 1. `remove_all_tags` (or `remove_tag`)

Removes all tag associations from the specified items.

```json
{
  "action": "remove_all_tags",
  "id": [
    "minecraft:stick",
    "minecraft:cake"
  ]
}
```

### 2. `remove_from_tag`

Removes specific items from a specific tag.

```json
{
  "action": "remove_from_tag",
  "tag": "minecraft:planks",
  "id": "minecraft:oak_planks"
}
```

### 3. `clear_tag`

Empties all items/blocks from the specified tags.

```json
{
  "action": "clear_tag",
  "tags": [
    "curios:artifact"
  ]
}
```

---

## Crafting Transmute backport

Reliable Recipes backports the `minecraft:crafting_transmute` crafting recipe type. This allows you to upgrade/convert
items in a crafting grid while preserving all of their item components (such as durability, enchantments, and custom
names).

### Recipe Fields

* **`input`** (Ingredient, required): The item being upgraded.
* **`material`** (Ingredient, required): The material item(s) needed.
* **`material_count`** (Int or min/max Bounds, optional, defaults to 1): How many materials are required (e.g. `1` or
  `{"min": 1, "max": 4}`).
* **`result`** (Item, required): The item output.
* **`add_material_count_to_result`** (Boolean, optional, default: false): If true, the quantity of materials placed in
  the grid will be added to the result count.

### Example JSON

```json
{
  "type": "minecraft:crafting_transmute",
  "category": "equipment",
  "input": {
    "item": "minecraft:iron_pickaxe"
  },
  "material": {
    "item": "minecraft:gold_ingot"
  },
  "material_count": 1,
  "result": {
    "id": "minecraft:golden_pickaxe"
  }
}
```

---

## Data-Driven Brewing Recipes

Reliable Recipes supports data-driven brewing stand recipes using the 26.3 `minecraft:brewing` recipe type format.

### Recipe Fields

* **`input`** (Object, required): Input container item or potion stack (`item`/`tag`, optional `potion_contents`).
* **`reagent`** (Ingredient, required): Ingredient item placed in the top slot of the brewing stand.
* **`output`** (ItemStack, required): Resulting item stack output with components.

### Example JSON

```json
{
  "type": "minecraft:brewing",
  "input": {
    "item": "minecraft:potion",
    "potion_contents": {
      "potions": "minecraft:awkward"
    }
  },
  "reagent": {
    "item": "minecraft:turtle_helmet"
  },
  "output": {
    "id": "minecraft:potion",
    "components": {
      "minecraft:potion_contents": {
        "potion": "minecraft:turtle_master"
      }
    }
  }
}
```

---

## Mod Compatibility

* **Reliable Removal:** Automatically removes recipe and tag listings for items hidden
  with [Reliable Removal](https://www.curseforge.com/minecraft/mc-mods/reliable-remover)'s blacklist.
* **EMI Integration:** Press the **delete key** while hovering over a recipe output in EMI (while in dev mode) to
  automatically append a removal rule to `./config/reliable_recipes/generated_removals.json`.

---

## Documentation & Wiki

For more advanced examples and details, see our wiki:
**[Modded Minecraft Wiki](https://moddedmc.wiki/en/project/reliable-recipes/latest/docs/reliable-recipes/features)**

---

## License

[![Code license (MIT)](https://img.shields.io/badge/code%20license-MIT-green.svg?style=flat-square)](https://github.com/evanbones/Reliable-Recipes/blob/1.20.1/LICENSE)