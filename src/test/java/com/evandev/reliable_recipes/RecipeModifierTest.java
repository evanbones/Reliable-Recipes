package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.recipe.RecipeJsonMutator;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.test.MinecraftTestBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

//? if <1.21.2 {
/*import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
*///?}

import static org.junit.jupiter.api.Assertions.*;

public class RecipeModifierTest extends MinecraftTestBase {

    @Test
    @DisplayName("Replace input in shaped recipe key mapping with array of items")
    void testReplaceInputShapedArray() {
        String recipeRaw = """
        {
          "type": "minecraft:crafting_shaped",
          "category": "equipment",
          "key": {
            "#": { "item": "minecraft:stick" },
            "X": { "item": "minecraft:iron_ingot" }
          },
          "pattern": [
            "X",
            "#"
          ],
          "result": {
            "count": 1,
            "id": "minecraft:iron_sword"
          }
        }
        """;
        JsonObject recipe = JsonParser.parseString(recipeRaw).getAsJsonObject();

        JsonArray replacement = new JsonArray();
        replacement.add("minecraft:bamboo");
        replacement.add("minecraft:stick");

        RecipeJsonMutator.applyReplacement(recipe, RecipeRule.Action.REPLACE_INPUT, List.of("minecraft:stick"), replacement, RecipeJsonMutator.IngredientFormat.OBJECT);

        JsonObject keyObj = recipe.getAsJsonObject("key");
        JsonElement stickKey = keyObj.get("#");
        assertTrue(stickKey.isJsonArray(), "Replaced key '#' should be a JsonArray when given an array of replacements");

        JsonArray arr = stickKey.getAsJsonArray();
        assertEquals(2, arr.size());
        assertEquals("minecraft:bamboo", arr.get(0).getAsJsonObject().get("item").getAsString());
        assertEquals("minecraft:stick", arr.get(1).getAsJsonObject().get("item").getAsString());
        assertEquals("minecraft:iron_ingot", keyObj.get("X").getAsJsonObject().get("item").getAsString());
    }

    @Test
    @DisplayName("Replace input in shapeless recipe ingredients array")
    void testReplaceInputShapeless() {
        String recipeRaw = """
        {
          "type": "minecraft:crafting_shapeless",
          "ingredients": [
            { "item": "minecraft:stick" },
            { "item": "minecraft:coal" }
          ],
          "result": {
            "count": 4,
            "id": "minecraft:torch"
          }
        }
        """;
        JsonObject recipe = JsonParser.parseString(recipeRaw).getAsJsonObject();

        RecipeJsonMutator.applyReplacement(recipe, RecipeRule.Action.REPLACE_INPUT, List.of("minecraft:coal"), JsonParser.parseString("\"minecraft:charcoal\""), RecipeJsonMutator.IngredientFormat.OBJECT);

        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        assertEquals(2, ingredients.size());
        assertEquals("minecraft:stick", ingredients.get(0).getAsJsonObject().get("item").getAsString());
        assertEquals("minecraft:charcoal", ingredients.get(1).getAsJsonObject().get("item").getAsString());
    }

    @Test
    @DisplayName("Replace output item preserving structure and count")
    void testReplaceOutput() {
        String recipeRaw = """
        {
          "type": "minecraft:crafting_shaped",
          "result": {
            "count": 1,
            "id": "minecraft:cake"
          }
        }
        """;
        JsonObject recipe = JsonParser.parseString(recipeRaw).getAsJsonObject();

        RecipeJsonMutator.applyReplacement(recipe, RecipeRule.Action.REPLACE_OUTPUT, List.of(), JsonParser.parseString("\"minecraft:golden_apple\""), RecipeJsonMutator.IngredientFormat.OBJECT);

        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals("minecraft:golden_apple", result.get("id").getAsString());
        assertEquals(1, result.get("count").getAsInt());
    }

    @Test
    @DisplayName("Replace input with tag replacement starting with #")
    void testReplaceInputWithTag() {
        String recipeRaw = """
        {
          "type": "minecraft:crafting_shaped",
          "key": {
            "#": { "item": "minecraft:oak_planks" }
          }
        }
        """;
        JsonObject recipe = JsonParser.parseString(recipeRaw).getAsJsonObject();

        RecipeJsonMutator.applyReplacement(recipe, RecipeRule.Action.REPLACE_INPUT, List.of("minecraft:oak_planks"), JsonParser.parseString("\"#minecraft:planks\""), RecipeJsonMutator.IngredientFormat.OBJECT);

        JsonObject keyObj = recipe.getAsJsonObject("key");
        JsonObject stickKey = keyObj.get("#").getAsJsonObject();
        assertFalse(stickKey.has("item"), "Old item key should be removed when replaced with tag");
        assertTrue(stickKey.has("tag"), "New tag key should be added");
        assertEquals("minecraft:planks", stickKey.get("tag").getAsString());
    }

    //? if <1.21.2 {
    /*@Test
    @DisplayName("Global API replacements via ReliableRecipesAPI")
    void testGlobalApiReplacements() {
        ReliableRecipesAPI.registerItemReplacement("minecraft:dirt", "minecraft:diamond_block");

        Map<Identifier, JsonElement> map = new HashMap<>();
        JsonObject recipe = JsonParser.parseString("""
        {
          "type": "minecraft:crafting_shapeless",
          "ingredients": [
            { "item": "minecraft:dirt" }
          ],
          "result": {
            "count": 1,
            "id": "minecraft:coarse_dirt"
          }
        }
        """).getAsJsonObject();

        Identifier id = Identifier.withDefaultNamespace("test_dirt");
        map.put(id, recipe);

        RecipeModifier.modifyRecipesJson(map);

        JsonObject modified = map.get(id).getAsJsonObject();
        JsonArray ingredients = modified.getAsJsonArray("ingredients");
        assertEquals("minecraft:diamond_block", ingredients.get(0).getAsJsonObject().get("item").getAsString());
    }
    *///?}

    private static final String BLOCK_TYPE_SWAP_RECIPE = """
        {
          "type": "minecraft:crafting_shaped",
          "key": {
            "A": {
              "type": "moonlight:block_type_swap",
              "ingredient": { "item": "minecraft:oak_log" },
              "block_type": "minecraft:wood_type",
              "from": "minecraft:oak",
              "to": "bountifulfares:walnut"
            },
            "B": { "item": "bountifulfares:walnut" }
          },
          "pattern": ["AB"],
          "result": { "count": 1, "id": "bountifulfares:walnut_boards" }
        }
        """;

    @Test
    @DisplayName("Replace input ignores non-item fields of typed custom ingredients")
    void testReplaceInputSkipsTypedIngredientFields() {
        JsonObject recipe = JsonParser.parseString(BLOCK_TYPE_SWAP_RECIPE).getAsJsonObject();

        RecipeJsonMutator.applyReplacement(recipe, RecipeRule.Action.REPLACE_INPUT, List.of("bountifulfares:walnut"), JsonParser.parseString("\"nomansland:walnuts\""), RecipeJsonMutator.IngredientFormat.OBJECT);

        JsonObject keyObj = recipe.getAsJsonObject("key");
        assertEquals("bountifulfares:walnut", keyObj.getAsJsonObject("A").get("to").getAsString());
        assertEquals("nomansland:walnuts", keyObj.getAsJsonObject("B").get("item").getAsString());
    }

    @Test
    @DisplayName("JSON mutator ignores non-item fields of typed custom ingredients")
    void testJsonMutatorSkipsTypedIngredientFields() {
        JsonObject recipe = JsonParser.parseString(BLOCK_TYPE_SWAP_RECIPE).getAsJsonObject();

        boolean changed = RecipeJsonMutator.mutateRecipe(
                recipe,
                Map.of("bountifulfares:walnut", JsonParser.parseString("\"nomansland:walnuts\"")),
                Map.of(),
                RecipeJsonMutator.IngredientFormat.STRING
        );

        assertTrue(changed);
        JsonObject keyObj = recipe.getAsJsonObject("key");
        assertEquals("bountifulfares:walnut", keyObj.getAsJsonObject("A").get("to").getAsString());
        assertEquals("nomansland:walnuts", keyObj.getAsJsonObject("B").get("item").getAsString());
    }
}
