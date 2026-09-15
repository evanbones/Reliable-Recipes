package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.config.RecipeRuleParser;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.test.MinecraftTestBase;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RepairAndBrewingRuleTest extends MinecraftTestBase {

    @Test
    @DisplayName("Prevent repair targetsMatch works with item ID and regex")
    void testPreventRepairMatching() {
        String json1 = """
        {
          "action": "prevent_repair",
          "target": "minecraft:diamond_pickaxe"
        }
        """;
        RecipeRule rule1 = RecipeRuleParser.parseRule(JsonParser.parseString(json1).getAsJsonObject());
        assertNotNull(rule1);
        assertTrue(rule1.getTargetInput().test(new ItemStack(Items.DIAMOND_PICKAXE)));
        assertFalse(rule1.getTargetInput().test(new ItemStack(Items.IRON_PICKAXE)));

        String json2 = """
        {
          "action": "prevent_repair",
          "target": "/.*_pickaxe/"
        }
        """;
        RecipeRule rule2 = RecipeRuleParser.parseRule(JsonParser.parseString(json2).getAsJsonObject());
        assertNotNull(rule2);
        assertTrue(rule2.getTargetInput().test(new ItemStack(Items.DIAMOND_PICKAXE)));
        assertTrue(rule2.getTargetInput().test(new ItemStack(Items.IRON_PICKAXE)));
        assertTrue(rule2.getTargetInput().test(new ItemStack(Items.GOLDEN_PICKAXE)));
        assertFalse(rule2.getTargetInput().test(new ItemStack(Items.DIAMOND_SWORD)));
    }

    @Test
    @DisplayName("Set repair material target and material matching")
    void testSetRepairMaterialMatching() {
        String json = """
        {
          "action": "set_repair_material",
          "target": "minecraft:diamond_sword",
          "material": "minecraft:dirt"
        }
        """;
        RecipeRule rule = RecipeRuleParser.parseRule(JsonParser.parseString(json).getAsJsonObject());
        assertNotNull(rule);

        assertTrue(rule.getTargetInput().test(new ItemStack(Items.DIAMOND_SWORD)));
        assertFalse(rule.getTargetInput().test(new ItemStack(Items.NETHERITE_SWORD)));

        assertTrue(rule.getNewInput().test(new ItemStack(Items.DIRT)));
        assertFalse(rule.getNewInput().test(new ItemStack(Items.DIAMOND)));
    }

    @Test
    @DisplayName("Brewing reagent replacement rule matches target and replacement")
    void testBrewingReagentReplacementMatching() {
        String json = """
        {
          "action": "replace_input",
          "target": "minecraft:golden_carrot",
          "replacement": "minecraft:diamond"
        }
        """;
        RecipeRule rule = RecipeRuleParser.parseRule(JsonParser.parseString(json).getAsJsonObject());
        assertNotNull(rule);

        assertTrue(rule.targetsMatch(new ItemStack(Items.GOLDEN_CARROT)));
        assertFalse(rule.targetsMatch(new ItemStack(Items.DIAMOND)));

        assertTrue(rule.replacementMatches(new ItemStack(Items.DIAMOND)));
        assertFalse(rule.replacementMatches(new ItemStack(Items.GOLDEN_CARROT)));
    }
}
