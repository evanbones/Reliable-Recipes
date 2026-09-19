package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.tag.TagRule;
import com.evandev.reliable_recipes.test.MinecraftTestBase;
import com.evandev.reliable_recipes.test.TestPlatformHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RecipeConfigIOTest extends MinecraftTestBase {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Load rules, tag rules, and custom recipes from mixed config array")
    void testLoadFromMixedArray() throws IOException {
        Path reliableRecipesDir = tempDir.resolve("reliable_recipes");
        Files.createDirectories(reliableRecipesDir);
        TestPlatformHelper.customConfigDir = tempDir;

        String content = """
        [
          {
            "action": "remove_recipe",
            "mod": "examplemod",
            "type": "minecraft:crafting_shaped"
          },
          {
            "action": "replace_input",
            "target": "minecraft:stick",
            "replacement": [
              "minecraft:stick",
              "examplemod:reinforced_stick"
            ],
            "id": "examplemod:reinforced_sword"
          },
          {
            "action": "remove_from_tag",
            "tag": "c:foods",
            "id": [
              "examplemod:inedible_food"
            ]
          },
          {
            "action": "add_recipe",
            "id": "reliable_recipes:flint_from_gravel",
            "recipe": {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [
                { "item": "minecraft:gravel" },
                { "item": "minecraft:gravel" },
                { "item": "minecraft:gravel" }
              ],
              "result": {
                "count": 1,
                "id": "minecraft:flint"
              }
            }
          }
        ]
        """;
        Files.writeString(reliableRecipesDir.resolve("mixed_rules.json"), content);

        List<RecipeRule> rules = RecipeConfigIO.loadRules();
        assertEquals(2, rules.size());
        assertEquals(RecipeRule.Action.REMOVE, rules.get(0).getAction());
        assertEquals(RecipeRule.Action.REPLACE_INPUT, rules.get(1).getAction());

        List<TagRule> tagRules = RecipeConfigIO.loadTagRules();
        assertEquals(1, tagRules.size());
        assertEquals(TagRule.Action.REMOVE_FROM_TAG, tagRules.get(0).action());

        Map<ResourceLocation, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
        assertEquals(1, customRecipes.size());
        ResourceLocation expectedId = new ResourceLocation("reliable_recipes", "flint_from_gravel");
        assertTrue(customRecipes.containsKey(expectedId));
    }

    @Test
    @DisplayName("Load standalone recipe file and infer ID from path when omitted")
    void testLoadStandaloneRecipeInferredId() throws IOException {
        Path reliableRecipesDir = tempDir.resolve("reliable_recipes");
        Path subDir = reliableRecipesDir.resolve("crafting");
        Files.createDirectories(subDir);
        TestPlatformHelper.customConfigDir = tempDir;

        String recipeJson = """
        {
          "type": "minecraft:crafting_shaped",
          "category": "misc",
          "key": {
            "#": { "item": "minecraft:stick" }
          },
          "pattern": [
            "###",
            "###",
            "###"
          ],
          "result": {
            "count": 1,
            "id": "minecraft:diamond"
          }
        }
        """;
        Files.writeString(subDir.resolve("diamond_from_sticks.json"), recipeJson);

        Map<ResourceLocation, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
        assertEquals(1, customRecipes.size());

        ResourceLocation key = customRecipes.keySet().iterator().next();
        assertEquals("reliable_recipes", key.getNamespace());
        assertTrue(key.getPath().contains("diamond_from_sticks"));

        JsonObject recipe = customRecipes.get(key).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("minecraft:diamond", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    @DisplayName("Load direct add_recipe definition without wrapper")
    void testLoadDirectAddRecipe() throws IOException {
        Path reliableRecipesDir = tempDir.resolve("reliable_recipes");
        Files.createDirectories(reliableRecipesDir);
        TestPlatformHelper.customConfigDir = tempDir;

        String content = """
        [
          {
            "action": "add_recipe",
            "id": "reliable_recipes:dirt_to_stick",
            "type": "minecraft:crafting_shapeless",
            "ingredients": [
              { "item": "minecraft:dirt" }
            ],
            "result": {
              "count": 4,
              "id": "minecraft:stick"
            }
          }
        ]
        """;
        Files.writeString(reliableRecipesDir.resolve("direct_add.json"), content);

        Map<ResourceLocation, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
        assertEquals(1, customRecipes.size());

        ResourceLocation id = new ResourceLocation("reliable_recipes", "dirt_to_stick");
        assertTrue(customRecipes.containsKey(id));
        JsonObject recipe = customRecipes.get(id).getAsJsonObject();
        assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
        assertEquals("minecraft:stick", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(4, recipe.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    @DisplayName("Load multiple add_recipe entries in a single file")
    void testMultipleAddRecipesInSingleFile() throws IOException {
        Path reliableRecipesDir = tempDir.resolve("reliable_recipes");
        Files.createDirectories(reliableRecipesDir);
        TestPlatformHelper.customConfigDir = tempDir;

        String content = """
        [
          {
            "action": "add_recipe",
            "id": "reliable_recipes:recipe_one",
            "type": "minecraft:crafting_shapeless",
            "ingredients": [{ "item": "minecraft:dirt" }],
            "result": { "count": 1, "id": "minecraft:clay_ball" }
          },
          {
            "action": "add_recipe",
            "id": "reliable_recipes:recipe_two",
            "recipe": {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [{ "item": "minecraft:gravel" }],
              "result": { "count": 1, "id": "minecraft:flint" }
            }
          },
          {
            "action": "add_recipe",
            "type": "minecraft:crafting_shapeless",
            "ingredients": [{ "item": "minecraft:sand" }],
            "result": { "count": 1, "id": "minecraft:glass" }
          }
        ]
        """;
        Files.writeString(reliableRecipesDir.resolve("multiple_recipes.json"), content);

        Map<ResourceLocation, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
        assertEquals(3, customRecipes.size());

        ResourceLocation idOne = new ResourceLocation("reliable_recipes", "recipe_one");
        ResourceLocation idTwo = new ResourceLocation("reliable_recipes", "recipe_two");
        ResourceLocation idThreeInferred = new ResourceLocation("reliable_recipes", "multiple_recipes_2");

        assertTrue(customRecipes.containsKey(idOne));
        assertTrue(customRecipes.containsKey(idTwo));
        assertTrue(customRecipes.containsKey(idThreeInferred));
    }
}
