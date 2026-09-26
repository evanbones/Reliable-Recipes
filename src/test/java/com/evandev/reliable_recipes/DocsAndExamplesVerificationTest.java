package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.config.RecipeRuleParser;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.tag.TagRule;
import com.evandev.reliable_recipes.test.MinecraftTestBase;
import com.evandev.reliable_recipes.platform.Services;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DocsAndExamplesVerificationTest extends MinecraftTestBase {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("docs/reliable-recipes/actions.mdx Examples")
    class ActionsDocs {

        @Test
        @DisplayName("Example: Remove specific recipes by ID")
        void testRemoveRecipesByIdExample() {
            String json = """
            [
              {
                "action": "remove_recipe",
                "id": [
                  "minecraft:wooden_pickaxe",
                  "minecraft:wooden_hoe"
                ]
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());

            assertNotNull(rule);
            assertEquals(RecipeRule.Action.REMOVE, rule.getAction());

            JsonObject dummy = new JsonObject();
            assertTrue(rule.testJson(Identifier.withDefaultNamespace("wooden_pickaxe"), dummy));
            assertTrue(rule.testJson(Identifier.withDefaultNamespace("wooden_hoe"), dummy));
            assertFalse(rule.testJson(Identifier.withDefaultNamespace("wooden_sword"), dummy));
        }

        @Test
        @DisplayName("Example: Remove brewing recipes by input")
        void testRemoveBrewingByInputExample() {
            String json = """
            [
              {
                "action": "remove_recipe",
                "type": "minecraft:brewing",
                "input": "minecraft:golden_carrot"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());

            assertNotNull(rule);
            JsonObject brewingRecipe = JsonParser.parseString("""
            {
              "type": "minecraft:brewing",
              "input": { "item": "minecraft:golden_carrot" }
            }
            """).getAsJsonObject();

            assertTrue(rule.testJson(Identifier.withDefaultNamespace( "night_vision"), brewingRecipe));
        }

        @Test
        @DisplayName("Example: Replace Sticks with Bamboo OR Sticks for vanilla recipes")
        void testReplaceSticksWithBambooOrSticksExample() {
            String json = """
            [
              {
                "action": "replace_input",
                "target": "minecraft:stick",
                "replacement": [
                  "minecraft:bamboo",
                  "minecraft:stick"
                ],
                "mod": "minecraft"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());

            assertNotNull(rule);
            assertEquals(RecipeRule.Action.REPLACE_INPUT, rule.getAction());

            JsonObject recipe = JsonParser.parseString("""
            {
              "type": "minecraft:crafting_shaped",
              "key": {
                "#": { "item": "minecraft:stick" }
              }
            }
            """).getAsJsonObject();

            assertTrue(rule.testJson(Identifier.withDefaultNamespace( "iron_pickaxe"), recipe));
            assertFalse(rule.testJson(Identifier.fromNamespaceAndPath("othermod", "iron_pickaxe"), recipe));

            RecipeModifier.mutateJsonRecursively(
                    recipe,
                    rule.getRawTargets(),
                    rule.getRawReplacement(),
                    rule.getAction(),
                    true
            );

            JsonElement replaced = recipe.getAsJsonObject("key").get("#");
            assertTrue(replaced.isJsonArray());
            JsonArray replacedArr = replaced.getAsJsonArray();
            assertEquals(2, replacedArr.size());
            assertEquals("minecraft:bamboo", replacedArr.get(0).getAsJsonObject().get("item").getAsString());
            assertEquals("minecraft:stick", replacedArr.get(1).getAsJsonObject().get("item").getAsString());
        }

        @Test
        @DisplayName("Example: Make the Cake recipe craft a Golden Apple instead")
        void testCakeToGoldenAppleExample() {
            String json = """
            [
              {
                "action": "replace_output",
                "replacement": "minecraft:golden_apple",
                "id": "minecraft:cake"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());

            assertNotNull(rule);
            assertEquals(RecipeRule.Action.REPLACE_OUTPUT, rule.getAction());

            JsonObject cakeRecipe = JsonParser.parseString("""
            {
              "type": "minecraft:crafting_shaped",
              "result": {
                "count": 1,
                "id": "minecraft:cake"
              }
            }
            """).getAsJsonObject();

            assertTrue(rule.testJson(Identifier.withDefaultNamespace("cake"), cakeRecipe));

            RecipeModifier.mutateJsonRecursively(
                    cakeRecipe,
                    rule.getRawTargets(),
                    rule.getRawReplacement(),
                    rule.getAction(),
                    true
            );

            assertEquals("minecraft:golden_apple", cakeRecipe.getAsJsonObject("result").get("id").getAsString());
        }

        @Test
        @DisplayName("Example: Block specific item repair (diamond pickaxe)")
        void testPreventRepairExample() {
            String json = """
            [
              {
                "action": "prevent_repair",
                "target": "minecraft:diamond_pickaxe"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());

            assertNotNull(rule);
            assertEquals(RecipeRule.Action.PREVENT_REPAIR, rule.getAction());
            assertTrue(rule.getTargetInput().test(new ItemStack(Items.DIAMOND_PICKAXE)));
            assertFalse(rule.getTargetInput().test(new ItemStack(Items.IRON_PICKAXE)));
        }

        @Test
        @DisplayName("Example 1: Change a repair material by item ID (diamond sword -> dirt)")
        void testSetRepairMaterialExample1() {
            String json = """
            {
              "action": "set_repair_material",
              "target": "minecraft:diamond_sword",
              "material": "minecraft:dirt"
            }
            """;
            RecipeRule rule = RecipeRuleParser.parseRule(JsonParser.parseString(json).getAsJsonObject());

            assertNotNull(rule);
            assertEquals(RecipeRule.Action.SET_REPAIR_MATERIAL, rule.getAction());
            assertTrue(rule.getTargetInput().test(new ItemStack(Items.DIAMOND_SWORD)));
            assertTrue(rule.getNewInput().test(new ItemStack(Items.DIRT)));
        }

        @Test
        @DisplayName("Example 2: Use tags and regex for targets and repair materials (durability -> diamond)")
        void testSetRepairMaterialExample2() {
            String json = """
            {
              "action": "set_repair_material",
              "target": "minecraft:enchantable/durability",
              "material": "minecraft:diamond"
            }
            """;
            RecipeRule rule = RecipeRuleParser.parseRule(JsonParser.parseString(json).getAsJsonObject());

            assertNotNull(rule);
            assertEquals(RecipeRule.Action.SET_REPAIR_MATERIAL, rule.getAction());
            assertTrue(rule.getNewInput().test(new ItemStack(Items.DIAMOND)));
        }

        @Test
        @DisplayName("Example 3: Use regex patterns or multiple repair materials (swords -> ingots or dirt)")
        void testSetRepairMaterialExample3() {
            String json = """
            {
              "action": "set_repair_material",
              "target": "/.*_sword/",
              "material": [
                "/.*_ingot/",
                "minecraft:dirt"
              ]
            }
            """;
            RecipeRule rule = RecipeRuleParser.parseRule(JsonParser.parseString(json).getAsJsonObject());

            assertNotNull(rule);
            assertEquals(RecipeRule.Action.SET_REPAIR_MATERIAL, rule.getAction());
            assertTrue(rule.getTargetInput().test(new ItemStack(Items.DIAMOND_SWORD)));
            assertTrue(rule.getTargetInput().test(new ItemStack(Items.IRON_SWORD)));
            assertTrue(rule.getNewInput().test(new ItemStack(Items.IRON_INGOT)));
            assertTrue(rule.getNewInput().test(new ItemStack(Items.GOLD_INGOT)));
            assertTrue(rule.getNewInput().test(new ItemStack(Items.DIRT)));
        }

        @Test
        @DisplayName("Example: Global Repair Removal (minecraft:repair_item)")
        void testGlobalRepairRemovalExample() {
            String json = """
            [
              {
                "action": "remove_recipe",
                "id": "minecraft:repair_item"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());

            assertNotNull(rule);
            assertTrue(rule.testJson(Identifier.withDefaultNamespace("repair_item"), new JsonObject()));
            assertFalse(rule.testJson(Identifier.withDefaultNamespace("stick"), new JsonObject()));
        }

        @Test
        @DisplayName("Example: add_recipe with recipe wrapper (flint from gravel)")
        void testAddRecipeWrapperExample() throws IOException {
            Path dir = tempDir.resolve("reliable_recipes");
            Files.createDirectories(dir);
            Services.configDirectoryOverride = tempDir;

            String json = """
            [
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
            Files.writeString(dir.resolve("flint.json"), json);

            Map<Identifier, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
            Identifier id = Identifier.fromNamespaceAndPath("reliable_recipes", "flint_from_gravel");
            assertTrue(customRecipes.containsKey(id));
            assertEquals("minecraft:crafting_shapeless", customRecipes.get(id).getAsJsonObject().get("type").getAsString());
        }

        @Test
        @DisplayName("Example: add_recipe direct definition (dirt to stick)")
        void testAddRecipeDirectExample() throws IOException {
            Path dir = tempDir.resolve("reliable_recipes");
            Files.createDirectories(dir);
            Services.configDirectoryOverride = tempDir;

            String json = """
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
            Files.writeString(dir.resolve("dirt.json"), json);

            Map<Identifier, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
            Identifier id = Identifier.fromNamespaceAndPath("reliable_recipes", "dirt_to_stick");
            assertTrue(customRecipes.containsKey(id));
            assertEquals("minecraft:crafting_shapeless", customRecipes.get(id).getAsJsonObject().get("type").getAsString());
            assertEquals(4, customRecipes.get(id).getAsJsonObject().getAsJsonObject("result").get("count").getAsInt());
        }
    }

    @Nested
    @DisplayName("docs/reliable-recipes/filters.mdx Examples")
    class FiltersDocs {

        @Test
        @DisplayName("Example: Tag expansion in output filter")
        void testOutputTagFilterExample() {
            String json = """
            [
              {
                "action": "remove_recipe",
                "output": "#minecraft:wooden_trapdoors"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());
            assertNotNull(rule);

            JsonObject literalTagRecipe = JsonParser.parseString("""
            {
              "result": { "id": "#minecraft:wooden_trapdoors" }
            }
            """).getAsJsonObject();

            assertTrue(rule.testJson(Identifier.withDefaultNamespace("trapdoor"), literalTagRecipe));
        }

        @Test
        @DisplayName("Example: Input Tag shorthand syntax (+#)")
        void testInputTagShorthandExample() {
            String json = """
            [
              {
                "action": "remove_recipe",
                "input": "+#minecraft:wooden_trapdoors"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());
            assertNotNull(rule);

            JsonObject tagRecipe = JsonParser.parseString("""
            {
              "ingredients": [
                { "tag": "minecraft:wooden_trapdoors" }
              ]
            }
            """).getAsJsonObject();

            assertTrue(rule.testJson(Identifier.withDefaultNamespace("test"), tagRecipe));
        }

        @Test
        @DisplayName("Example: Input Tag object syntax ({ tag, expand: true })")
        void testInputTagObjectSyntaxExample() {
            String json = """
            [
              {
                "action": "remove_recipe",
                "input": {
                  "tag": "#minecraft:wooden_trapdoors",
                  "expand": true
                }
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());
            assertNotNull(rule);

            JsonObject tagRecipe = JsonParser.parseString("""
            {
              "ingredients": [
                { "tag": "minecraft:wooden_trapdoors" }
              ]
            }
            """).getAsJsonObject();

            assertTrue(rule.testJson(Identifier.withDefaultNamespace("test"), tagRecipe));
        }

        @Test
        @DisplayName("Example: Regex & Logic (gold items not from minecraft)")
        void testRegexAndLogicExample() {
            String json = """
            [
              {
                "action": "remove_recipe",
                "output": "/.*gold.*/",
                "not": {
                  "mod": "minecraft"
                }
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());
            assertNotNull(rule);

            JsonObject goldRecipe = JsonParser.parseString("""
            {
              "result": { "id": "othermod:gold_ingot" }
            }
            """).getAsJsonObject();

            assertTrue(rule.testJson(Identifier.fromNamespaceAndPath("othermod", "gold_ingot"), goldRecipe));
            assertFalse(rule.testJson(Identifier.withDefaultNamespace("gold_ingot"), goldRecipe));
        }
    }

    @Nested
    @DisplayName("docs/reliable-recipes/brewing.mdx Examples")
    class BrewingDocs {

        @Test
        @DisplayName("Example: Disabling a specific brewing recipe")
        void testDisableSpecificBrewingExample() {
            String json = """
            [
              {
                "action": "remove_recipe",
                "type": "minecraft:brewing",
                "input": "minecraft:golden_carrot"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());
            assertNotNull(rule);

            JsonObject nightVision = JsonParser.parseString("""
            {
              "type": "minecraft:brewing",
              "input": { "item": "minecraft:golden_carrot" }
            }
            """).getAsJsonObject();

            assertTrue(rule.testJson(Identifier.withDefaultNamespace("night_vision"), nightVision));
        }

        @Test
        @DisplayName("Example: Disabling all brewing recipes")
        void testDisableAllBrewingExample() {
            String json = """
            [
              {
                "action": "remove_recipe",
                "type": "minecraft:brewing"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());
            assertNotNull(rule);

            JsonObject anyBrewing = JsonParser.parseString("""
            {
              "type": "minecraft:brewing"
            }
            """).getAsJsonObject();

            JsonObject crafting = JsonParser.parseString("""
            {
              "type": "minecraft:crafting_shaped"
            }
            """).getAsJsonObject();

            assertTrue(rule.testJson(Identifier.withDefaultNamespace("any_potion"), anyBrewing));
            assertFalse(rule.testJson(Identifier.withDefaultNamespace("sword"), crafting));
        }

        @Test
        @DisplayName("Example: Replacing a brewing reagent (golden carrot -> diamond)")
        void testReplacingBrewingReagentExample() {
            String json = """
            [
              {
                "action": "replace_input",
                "target": "minecraft:golden_carrot",
                "replacement": "minecraft:diamond"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            RecipeRule rule = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());
            assertNotNull(rule);

            assertTrue(rule.targetsMatch(new ItemStack(Items.GOLDEN_CARROT)));
            assertTrue(rule.replacementMatches(new ItemStack(Items.DIAMOND)));
        }
    }

    @Nested
    @DisplayName("docs/reliable-recipes/tags.mdx Examples")
    class TagsDocs {

        @Test
        @DisplayName("Example: remove_all_tags from items")
        void testRemoveAllTagsExample() {
            String json = """
            [
              {
                "action": "remove_all_tags",
                "id": [
                  "minecraft:stick",
                  "minecraft:cake"
                ]
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            TagRule rule = RecipeRuleParser.parseTagRule(array.get(0).getAsJsonObject());
            assertNotNull(rule);
            assertEquals(TagRule.Action.REMOVE_ALL_TAGS, rule.action());
            assertTrue(rule.itemMatcher().test(Identifier.withDefaultNamespace("stick")));
            assertTrue(rule.itemMatcher().test(Identifier.withDefaultNamespace("cake")));
            assertFalse(rule.itemMatcher().test(Identifier.withDefaultNamespace("stone")));
        }

        @Test
        @DisplayName("Example: remove_from_tag for specific item and tag")
        void testRemoveFromTagExample() {
            String json = """
            [
              {
                "action": "remove_from_tag",
                "tag": "minecraft:planks",
                "id": "minecraft:oak_planks"
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            TagRule rule = RecipeRuleParser.parseTagRule(array.get(0).getAsJsonObject());
            assertNotNull(rule);
            assertEquals(TagRule.Action.REMOVE_FROM_TAG, rule.action());
            assertTrue(rule.itemMatcher().test(Identifier.withDefaultNamespace("oak_planks")));
            assertTrue(rule.tagMatcher().test(Identifier.withDefaultNamespace("planks")));
        }

        @Test
        @DisplayName("Example: clear_tag for multiple tags")
        void testClearTagExample() {
            String json = """
            [
              {
                "action": "clear_tag",
                "tags": [
                  "createaddition:plant_foods",
                  "caverns_and_chasms:experience_boost_items",
                  "curios:artifact"
                ]
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            TagRule rule = RecipeRuleParser.parseTagRule(array.get(0).getAsJsonObject());
            assertNotNull(rule);
            assertEquals(TagRule.Action.CLEAR_TAG, rule.action());
            assertTrue(rule.tagMatcher().test(Identifier.fromNamespaceAndPath("createaddition", "plant_foods")));
            assertTrue(rule.tagMatcher().test(Identifier.fromNamespaceAndPath("caverns_and_chasms", "experience_boost_items")));
            assertTrue(rule.tagMatcher().test(Identifier.fromNamespaceAndPath("curios", "artifact")));
        }
    }

    @Nested
    @DisplayName("docs/reliable-recipes/usage.mdx Examples")
    class UsageDocs {

        @Test
        @DisplayName("Example: Multi-rule array in usage.mdx")
        void testUsageMixedArrayExample() throws IOException {
            Path dir = tempDir.resolve("reliable_recipes");
            Files.createDirectories(dir);
            Services.configDirectoryOverride = tempDir;

            String json = """
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
            Files.writeString(dir.resolve("usage_rules.json"), json);

            List<RecipeRule> rules = RecipeConfigIO.loadRules();
            assertEquals(2, rules.size());
            List<TagRule> tagRules = RecipeConfigIO.loadTagRules();
            assertEquals(1, tagRules.size());
            Map<Identifier, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
            assertEquals(1, customRecipes.size());
        }

        @Test
        @DisplayName("Example: Standalone recipe file in usage.mdx")
        void testUsageStandaloneRecipeFile() throws IOException {
            Path dir = tempDir.resolve("reliable_recipes");
            Files.createDirectories(dir);
            Services.configDirectoryOverride = tempDir;

            String json = """
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
            Files.writeString(dir.resolve("diamond_box.json"), json);

            Map<Identifier, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
            assertEquals(1, customRecipes.size());
            Identifier id = customRecipes.keySet().iterator().next();
            assertEquals("diamond_box", id.getPath());
        }
    }

    @Nested
    @DisplayName("fabric/run/config/reliable_recipes/recipe_example.json.disabled Verification")
    class ShippedExampleConfig {

        @Test
        @DisplayName("Parse and verify the recipe_example.json.disabled configuration")
        void testShippedExampleConfig() {
            String json = """
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
              }
            ]
            """;
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();

            RecipeRule rule1 = RecipeRuleParser.parseRule(array.get(0).getAsJsonObject());
            assertNotNull(rule1);
            assertEquals(RecipeRule.Action.REMOVE, rule1.getAction());

            RecipeRule rule2 = RecipeRuleParser.parseRule(array.get(1).getAsJsonObject());
            assertNotNull(rule2);
            assertEquals(RecipeRule.Action.REPLACE_INPUT, rule2.getAction());
            assertEquals(List.of("minecraft:stick"), rule2.getRawTargets());
            assertTrue(rule2.getRawReplacement().isJsonArray());

            TagRule rule3 = RecipeRuleParser.parseTagRule(array.get(2).getAsJsonObject());
            assertNotNull(rule3);
            assertEquals(TagRule.Action.REMOVE_FROM_TAG, rule3.action());
            assertTrue(rule3.tagMatcher().test(Identifier.fromNamespaceAndPath("c", "foods")));
            assertTrue(rule3.itemMatcher().test(Identifier.fromNamespaceAndPath("examplemod", "inedible_food")));
        }
    }
}
