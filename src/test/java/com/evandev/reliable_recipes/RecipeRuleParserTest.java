package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.config.RecipeRuleParser;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.test.MinecraftTestBase;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecipeRuleParserTest extends MinecraftTestBase {

    @Test
    @DisplayName("Parse remove_recipe action with id array filter")
    void testParseRemoveRecipe() {
        String json = """
        {
          "action": "remove_recipe",
          "id": [
            "minecraft:wooden_pickaxe",
            "minecraft:wooden_hoe"
          ]
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);
        assertEquals(RecipeRule.Action.REMOVE, rule.getAction());

        JsonObject dummyRecipe = new JsonObject();
        dummyRecipe.addProperty("type", "minecraft:crafting_shaped");

        assertTrue(rule.testJson(Identifier.withDefaultNamespace("wooden_pickaxe"), dummyRecipe));
        assertTrue(rule.testJson(Identifier.withDefaultNamespace("wooden_hoe"), dummyRecipe));
        assertFalse(rule.testJson(Identifier.withDefaultNamespace("wooden_sword"), dummyRecipe));
    }

    @Test
    @DisplayName("Parse remove_recipe with brewing type and input filter")
    void testParseRemoveBrewingRecipe() {
        String json = """
        {
          "action": "remove_recipe",
          "type": "minecraft:brewing",
          "input": "minecraft:golden_carrot"
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);
        assertEquals(RecipeRule.Action.REMOVE, rule.getAction());

        JsonObject matchingRecipe = JsonParser.parseString("""
        {
          "type": "minecraft:brewing",
          "input": { "item": "minecraft:golden_carrot" }
        }
        """).getAsJsonObject();

        JsonObject nonMatchingType = JsonParser.parseString("""
        {
          "type": "minecraft:crafting_shapeless",
          "input": { "item": "minecraft:golden_carrot" }
        }
        """).getAsJsonObject();

        JsonObject nonMatchingInput = JsonParser.parseString("""
        {
          "type": "minecraft:brewing",
          "input": { "item": "minecraft:nether_wart" }
        }
        """).getAsJsonObject();

        Identifier id = Identifier.withDefaultNamespace("test_brewing");
        assertTrue(rule.testJson(id, matchingRecipe));
        assertFalse(rule.testJson(id, nonMatchingType));
        assertFalse(rule.testJson(id, nonMatchingInput));
    }

    @Test
    @DisplayName("Parse replace_input action")
    void testParseReplaceInput() {
        String json = """
        {
          "action": "replace_input",
          "target": "minecraft:stick",
          "replacement": [
            "minecraft:bamboo",
            "minecraft:stick"
          ],
          "mod": "minecraft"
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);
        assertEquals(RecipeRule.Action.REPLACE_INPUT, rule.getAction());
        assertEquals(List.of("minecraft:stick"), rule.getRawTargets());
        assertTrue(rule.getRawReplacement().isJsonArray());

        JsonObject dummyRecipe = new JsonObject();
        assertTrue(rule.testJson(Identifier.withDefaultNamespace("stick"), dummyRecipe));
        assertFalse(rule.testJson(Identifier.fromNamespaceAndPath("othermod", "stick"), dummyRecipe));
    }

    @Test
    @DisplayName("Parse replace_output action")
    void testParseReplaceOutput() {
        String json = """
        {
          "action": "replace_output",
          "replacement": "minecraft:golden_apple",
          "id": "minecraft:cake"
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);
        assertEquals(RecipeRule.Action.REPLACE_OUTPUT, rule.getAction());
        assertEquals("minecraft:golden_apple", rule.getRawReplacement().getAsString());

        JsonObject dummyRecipe = new JsonObject();
        assertTrue(rule.testJson(Identifier.withDefaultNamespace("cake"), dummyRecipe));
        assertFalse(rule.testJson(Identifier.withDefaultNamespace("apple"), dummyRecipe));
    }

    @Test
    @DisplayName("Parse prevent_repair action")
    void testParsePreventRepair() {
        String json = """
        {
          "action": "prevent_repair",
          "target": "minecraft:diamond_pickaxe"
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);
        assertEquals(RecipeRule.Action.PREVENT_REPAIR, rule.getAction());
        assertTrue(rule.getTargetInput().test(new ItemStack(Items.DIAMOND_PICKAXE)));
        assertFalse(rule.getTargetInput().test(new ItemStack(Items.IRON_PICKAXE)));
    }

    @Test
    @DisplayName("Parse set_repair_material action with single material")
    void testParseSetRepairMaterialSingle() {
        String json = """
        {
          "action": "set_repair_material",
          "target": "minecraft:diamond_sword",
          "material": "minecraft:dirt"
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);
        assertEquals(RecipeRule.Action.SET_REPAIR_MATERIAL, rule.getAction());
        assertTrue(rule.getTargetInput().test(new ItemStack(Items.DIAMOND_SWORD)));
        assertFalse(rule.getTargetInput().test(new ItemStack(Items.IRON_SWORD)));
        assertTrue(rule.getNewInput().test(new ItemStack(Items.DIRT)));
        assertFalse(rule.getNewInput().test(new ItemStack(Items.DIAMOND)));
    }

    @Test
    @DisplayName("Parse set_repair_material action with array and regex")
    void testParseSetRepairMaterialRegex() {
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
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);
        assertEquals(RecipeRule.Action.SET_REPAIR_MATERIAL, rule.getAction());
        assertTrue(rule.getTargetInput().test(new ItemStack(Items.DIAMOND_SWORD)));
        assertTrue(rule.getTargetInput().test(new ItemStack(Items.IRON_SWORD)));
        assertFalse(rule.getTargetInput().test(new ItemStack(Items.BOW)));

        assertTrue(rule.getNewInput().test(new ItemStack(Items.IRON_INGOT)));
        assertTrue(rule.getNewInput().test(new ItemStack(Items.COPPER_INGOT)));
        assertTrue(rule.getNewInput().test(new ItemStack(Items.DIRT)));
        assertFalse(rule.getNewInput().test(new ItemStack(Items.STICK)));
    }

    @Test
    @DisplayName("Parse and evaluate regex filter on output and mod")
    void testParseRegexFilter() {
        String json = """
        {
          "action": "remove_recipe",
          "output": "/.*gold.*/",
          "mod": "minecraft"
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);

        JsonObject matchingRecipe = JsonParser.parseString("""
        {
          "result": { "id": "minecraft:golden_sword" }
        }
        """).getAsJsonObject();

        JsonObject nonMatchingOutput = JsonParser.parseString("""
        {
          "result": { "id": "minecraft:iron_sword" }
        }
        """).getAsJsonObject();

        assertTrue(rule.testJson(Identifier.withDefaultNamespace("golden_sword"), matchingRecipe));
        assertFalse(rule.testJson(Identifier.withDefaultNamespace("iron_sword"), nonMatchingOutput));
        assertFalse(rule.testJson(Identifier.fromNamespaceAndPath("othermod", "golden_sword"), matchingRecipe));
    }

    @Test
    @DisplayName("Parse and evaluate logical not combinator")
    void testParseNotCombinator() {
        String json = """
        {
          "action": "remove_recipe",
          "output": "/.*gold.*/",
          "not": {
            "mod": "minecraft"
          }
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);

        JsonObject goldRecipe = JsonParser.parseString("""
        {
          "result": { "id": "somemod:golden_dagger" }
        }
        """).getAsJsonObject();

        assertFalse(rule.testJson(Identifier.withDefaultNamespace("golden_dagger"), goldRecipe));
        assertTrue(rule.testJson(Identifier.fromNamespaceAndPath("somemod", "golden_dagger"), goldRecipe));
    }

    @Test
    @DisplayName("Parse and evaluate logical and/or combinators")
    void testParseAndOrCombinators() {
        String json = """
        {
          "action": "remove_recipe",
          "or": [
            { "id": "minecraft:stick" },
            {
              "and": [
                { "mod": "minecraft" },
                { "type": "minecraft:blasting" }
              ]
            }
          ]
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);

        JsonObject blastingRecipe = new JsonObject();
        blastingRecipe.addProperty("type", "minecraft:blasting");

        JsonObject craftingRecipe = new JsonObject();
        craftingRecipe.addProperty("type", "minecraft:crafting_shaped");

        assertTrue(rule.testJson(Identifier.withDefaultNamespace("stick"), craftingRecipe));
        assertTrue(rule.testJson(Identifier.withDefaultNamespace("iron_ingot"), blastingRecipe));
        assertFalse(rule.testJson(Identifier.fromNamespaceAndPath("othermod", "iron_ingot"), blastingRecipe));
        assertFalse(rule.testJson(Identifier.withDefaultNamespace("iron_pickaxe"), craftingRecipe));
    }

    @Test
    @DisplayName("Parse input tag filter with shorthand +# expansion")
    void testInputTagFilterShorthand() {
        String json = """
        {
          "action": "remove_recipe",
          "input": "+#minecraft:wooden_trapdoors"
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);

        JsonObject literalTagRecipe = JsonParser.parseString("""
        {
          "ingredients": [
            { "tag": "minecraft:wooden_trapdoors" }
          ]
        }
        """).getAsJsonObject();

        JsonObject nonMatchingRecipe = JsonParser.parseString("""
        {
          "ingredients": [
            { "item": "minecraft:stone" }
          ]
        }
        """).getAsJsonObject();

        Identifier id = Identifier.withDefaultNamespace("test");
        assertTrue(rule.testJson(id, literalTagRecipe));
        assertFalse(rule.testJson(id, nonMatchingRecipe));
    }

    @Test
    @DisplayName("Parse input tag filter with object expand syntax")
    void testInputTagFilterObjectSyntax() {
        String json = """
        {
          "action": "remove_recipe",
          "input": {
            "tag": "#minecraft:wooden_trapdoors",
            "expand": true
          }
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        RecipeRule rule = RecipeRuleParser.parseRule(obj);

        assertNotNull(rule);

        JsonObject literalTagRecipe = JsonParser.parseString("""
        {
          "ingredients": [
            { "tag": "minecraft:wooden_trapdoors" }
          ]
        }
        """).getAsJsonObject();

        Identifier id = Identifier.withDefaultNamespace("test");
        assertTrue(rule.testJson(id, literalTagRecipe));
    }

    @Test
    @DisplayName("Parse ingredients with various notations")
    void testIngredientParsing() {
        Ingredient itemIng = RecipeRuleParser.parseIngredientString("minecraft:diamond");
        assertFalse(itemIng.isEmpty());
        assertTrue(itemIng.test(new ItemStack(Items.DIAMOND)));
        assertFalse(itemIng.test(new ItemStack(Items.IRON_INGOT)));

        Ingredient itemPrefixed = RecipeRuleParser.parseIngredientString("item:minecraft:emerald");
        assertFalse(itemPrefixed.isEmpty());
        assertTrue(itemPrefixed.test(new ItemStack(Items.EMERALD)));

        Ingredient regexIng = RecipeRuleParser.parseIngredientString("/minecraft:.*_pickaxe/");
        assertFalse(regexIng.isEmpty());
        assertTrue(regexIng.test(new ItemStack(Items.DIAMOND_PICKAXE)));
        assertTrue(regexIng.test(new ItemStack(Items.IRON_PICKAXE)));
        assertFalse(regexIng.test(new ItemStack(Items.DIAMOND_SWORD)));
    }
}
