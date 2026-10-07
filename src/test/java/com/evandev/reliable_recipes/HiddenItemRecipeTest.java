package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.test.MinecraftTestBase;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class HiddenItemRecipeTest extends MinecraftTestBase {

    private static boolean hidden(String recipeJson, String... hiddenItems) {
        Set<String> items = Set.of(hiddenItems);
        JsonObject recipe = JsonParser.parseString(recipeJson).getAsJsonObject();
        return RecipeModifier.shouldHideRecipeJson(recipe, items::contains);
    }

    // From Reliable Remover issue #95
    private static final String TFC_COMPOSTER = """
            {
              "type": "minecraft:crafting_shaped",
              "pattern": ["X X", "XYX", "XYX"],
              "key": {
                "X": {"tag": "tfc:lumber"},
                "Y": {"tag": "minecraft:dirt"}
              },
              "result": {"item": "tfc:composter"}
            }
            """;

    @Test
    @DisplayName("A tag sharing its name with a hidden item doesn't remove the recipe")
    void testTagNamedLikeHiddenItem() {
        assertFalse(hidden(TFC_COMPOSTER, "minecraft:dirt"));
    }

    @Test
    @DisplayName("A tag sharing its name with a hidden item doesn't remove the recipe (1.21.2+ format)")
    void testTagNamedLikeHiddenItemStringFormat() {
        assertFalse(hidden("""
                {
                  "type": "minecraft:crafting_shaped",
                  "pattern": ["X X", "XYX", "XYX"],
                  "key": {"X": "#tfc:lumber", "Y": "#minecraft:dirt"},
                  "result": {"id": "tfc:composter"}
                }
                """, "minecraft:dirt"));
    }

    @Test
    @DisplayName("A hidden item in a shaped key removes the recipe")
    void testHiddenShapedItem() {
        String objectFormat = """
                {
                  "type": "minecraft:crafting_shaped",
                  "pattern": ["X", "#"],
                  "key": {"#": {"item": "minecraft:stick"}, "X": {"item": "minecraft:dirt"}},
                  "result": {"item": "minecraft:coarse_dirt"}
                }
                """;
        String stringFormat = """
                {
                  "type": "minecraft:crafting_shaped",
                  "pattern": ["X", "#"],
                  "key": {"#": "minecraft:stick", "X": "minecraft:dirt"},
                  "result": {"id": "minecraft:coarse_dirt"}
                }
                """;
        assertTrue(hidden(objectFormat, "minecraft:dirt"));
        assertTrue(hidden(stringFormat, "minecraft:dirt"));
        assertFalse(hidden(objectFormat, "minecraft:gravel"));
        assertFalse(hidden(stringFormat, "minecraft:gravel"));
    }

    @Test
    @DisplayName("A slot with alternatives is only hidden once every alternative is hidden")
    void testAlternatives() {
        String objectFormat = """
                {
                  "type": "minecraft:crafting_shaped",
                  "pattern": ["X"],
                  "key": {"X": [{"item": "minecraft:dirt"}, {"item": "minecraft:coarse_dirt"}]},
                  "result": {"item": "minecraft:stone"}
                }
                """;
        String stringFormat = """
                {
                  "type": "minecraft:crafting_shaped",
                  "pattern": ["X"],
                  "key": {"X": ["minecraft:dirt", "minecraft:coarse_dirt"]},
                  "result": {"id": "minecraft:stone"}
                }
                """;
        assertFalse(hidden(objectFormat, "minecraft:dirt"));
        assertFalse(hidden(stringFormat, "minecraft:dirt"));
        assertTrue(hidden(objectFormat, "minecraft:dirt", "minecraft:coarse_dirt"));
        assertTrue(hidden(stringFormat, "minecraft:dirt", "minecraft:coarse_dirt"));
    }

    @Test
    @DisplayName("A slot mixing a hidden item with a tag is kept")
    void testAlternativeWithTag() {
        assertFalse(hidden("""
                {
                  "type": "minecraft:crafting_shaped",
                  "pattern": ["X"],
                  "key": {"X": [{"item": "minecraft:dirt"}, {"tag": "minecraft:dirt"}]},
                  "result": {"item": "minecraft:stone"}
                }
                """, "minecraft:dirt"));
    }

    @Test
    @DisplayName("Shapeless ingredients are checked one slot at a time")
    void testShapeless() {
        String recipe = """
                {
                  "type": "minecraft:crafting_shapeless",
                  "ingredients": [
                    {"item": "minecraft:stick"},
                    [{"item": "minecraft:coal"}, {"item": "minecraft:charcoal"}]
                  ],
                  "result": {"item": "minecraft:torch", "count": 4}
                }
                """;
        assertTrue(hidden(recipe, "minecraft:stick"));
        assertFalse(hidden(recipe, "minecraft:coal"));
        assertTrue(hidden(recipe, "minecraft:coal", "minecraft:charcoal"));
        assertFalse(hidden(recipe, "minecraft:torch_flower"));
    }

    @Test
    @DisplayName("Shapeless tag ingredients are never hidden (1.21.2+ format)")
    void testShapelessTagString() {
        assertFalse(hidden("""
                {
                  "type": "minecraft:crafting_shapeless",
                  "ingredients": ["#minecraft:planks"],
                  "result": {"id": "minecraft:stick", "count": 4}
                }
                """, "minecraft:planks"));
    }

    @Test
    @DisplayName("A hidden output removes the recipe")
    void testHiddenOutput() {
        assertTrue(hidden(TFC_COMPOSTER, "tfc:composter"));
        assertTrue(hidden("""
                {"type": "minecraft:smelting", "ingredient": "minecraft:cobblestone", "result": {"id": "minecraft:stone"}}
                """, "minecraft:stone"));
        assertTrue(hidden("""
                {"type": "minecraft:smelting", "ingredient": {"item": "minecraft:cobblestone"}, "result": "minecraft:stone"}
                """, "minecraft:stone"));
    }

    @Test
    @DisplayName("Brewing input and reagent are checked as ingredients")
    void testBrewing() {
        String recipe = """
                {
                  "type": "reliable_recipes:brewing",
                  "input": {"item": "minecraft:potion"},
                  "reagent": [{"item": "minecraft:sugar"}, {"tag": "c:sweeteners"}],
                  "result": {"item": "minecraft:splash_potion"}
                }
                """;
        assertTrue(hidden(recipe, "minecraft:potion"));
        assertFalse(hidden(recipe, "minecraft:sugar"));
    }

    @Test
    @DisplayName("Strings outside item fields are not treated as item IDs")
    void testNonItemStrings() {
        // Forge NBT ingredient with a string that happens to be a hidden ID
        assertFalse(hidden("""
                {
                  "type": "minecraft:crafting_shapeless",
                  "ingredients": [
                    {"type": "forge:nbt", "item": "minecraft:potion", "nbt": {"Potion": "minecraft:water"}}
                  ],
                  "result": {"item": "minecraft:stone"}
                }
                """, "minecraft:water"));

        // Moonlight block type swap: "to" names a block type, not an ingredient
        String blockTypeSwap = """
                {
                  "type": "minecraft:crafting_shaped",
                  "pattern": ["A"],
                  "key": {
                    "A": {
                      "type": "moonlight:block_type_swap",
                      "ingredient": {"item": "minecraft:oak_log"},
                      "block_type": "minecraft:wood_type",
                      "from": "minecraft:oak",
                      "to": "bountifulfares:walnut"
                    }
                  },
                  "result": {"item": "bountifulfares:walnut_boards"}
                }
                """;
        assertFalse(hidden(blockTypeSwap, "bountifulfares:walnut"));
        assertTrue(hidden(blockTypeSwap, "minecraft:oak_log"));
    }

    @Test
    @DisplayName("Loader ingredient wrappers are unwrapped")
    void testLoaderIngredients() {
        assertTrue(hidden("""
                {
                  "type": "minecraft:crafting_shapeless",
                  "ingredients": [{"type": "neoforge:components", "items": "minecraft:dirt", "components": {}}],
                  "result": {"id": "minecraft:stone"}
                }
                """, "minecraft:dirt"));

        // Unknown custom ingredient types are kept instead of being guessed at
        assertFalse(hidden("""
                {
                  "type": "minecraft:crafting_shapeless",
                  "ingredients": [{"fabric:type": "fabric:any_of", "ingredients": [{"item": "minecraft:dirt"}, {"item": "minecraft:stone"}]}],
                  "result": {"item": "minecraft:gravel"}
                }
                """, "minecraft:dirt"));
    }

    @Test
    @DisplayName("Empty and malformed ingredients don't remove the recipe")
    void testEmptyAndMalformed() {
        assertFalse(hidden("""
                {"type": "minecraft:crafting_shapeless", "ingredients": [[]], "result": {"item": "minecraft:stone"}}
                """, "minecraft:dirt"));
        assertFalse(hidden("""
                {"type": "minecraft:crafting_shaped", "key": {"X": 5}, "result": {"item": "minecraft:stone"}}
                """, "minecraft:dirt"));
        assertFalse(hidden("""
                {"type": "minecraft:crafting_shaped", "key": {"X": null}, "result": {"item": "minecraft:stone"}}
                """, "minecraft:dirt"));
    }
}
