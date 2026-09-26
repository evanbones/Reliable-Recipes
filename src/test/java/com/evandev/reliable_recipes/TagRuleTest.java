package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.config.RecipeRuleParser;
import com.evandev.reliable_recipes.tag.TagRule;
import com.evandev.reliable_recipes.test.MinecraftTestBase;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TagRuleTest extends MinecraftTestBase {

    @Test
    @DisplayName("Parse remove_all_tags with id array")
    void testParseRemoveAllTags() {
        String json = """
        {
          "action": "remove_all_tags",
          "id": [
            "minecraft:stick",
            "minecraft:cake"
          ]
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        TagRule rule = RecipeRuleParser.parseTagRule(obj);

        assertNotNull(rule);
        assertEquals(TagRule.Action.REMOVE_ALL_TAGS, rule.action());
        assertTrue(rule.itemMatcher().test(Identifier.withDefaultNamespace("stick")));
        assertTrue(rule.itemMatcher().test(Identifier.withDefaultNamespace("cake")));
        assertFalse(rule.itemMatcher().test(Identifier.withDefaultNamespace("apple")));
    }

    @Test
    @DisplayName("Parse remove_from_tag for specific item and tag")
    void testParseRemoveFromTag() {
        String json = """
        {
          "action": "remove_from_tag",
          "tag": "minecraft:planks",
          "id": "minecraft:oak_planks"
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        TagRule rule = RecipeRuleParser.parseTagRule(obj);

        assertNotNull(rule);
        assertEquals(TagRule.Action.REMOVE_FROM_TAG, rule.action());
        assertTrue(rule.itemMatcher().test(Identifier.withDefaultNamespace("oak_planks")));
        assertFalse(rule.itemMatcher().test(Identifier.withDefaultNamespace("birch_planks")));

        assertTrue(rule.tagMatcher().test(Identifier.withDefaultNamespace("planks")));
        assertFalse(rule.tagMatcher().test(Identifier.withDefaultNamespace("logs")));
    }

    @Test
    @DisplayName("Parse clear_tag with array of tags")
    void testParseClearTagArray() {
        String json = """
        {
          "action": "clear_tag",
          "tags": [
            "createaddition:plant_foods",
            "caverns_and_chasms:experience_boost_items",
            "curios:artifact"
          ]
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        TagRule rule = RecipeRuleParser.parseTagRule(obj);

        assertNotNull(rule);
        assertEquals(TagRule.Action.CLEAR_TAG, rule.action());

        assertTrue(rule.tagMatcher().test(Identifier.fromNamespaceAndPath("createaddition", "plant_foods")));
        assertTrue(rule.tagMatcher().test(Identifier.fromNamespaceAndPath("caverns_and_chasms", "experience_boost_items")));
        assertTrue(rule.tagMatcher().test(Identifier.fromNamespaceAndPath("curios", "artifact")));
        assertFalse(rule.tagMatcher().test(Identifier.withDefaultNamespace("planks")));
    }

    @Test
    @DisplayName("Parse remove_from_tag with regex pattern")
    void testParseRemoveFromTagRegex() {
        String json = """
        {
          "action": "remove_from_tag",
          "tag": "c:foods",
          "id": "/.*:inedible_.*/"
        }
        """;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        TagRule rule = RecipeRuleParser.parseTagRule(obj);

        assertNotNull(rule);
        assertEquals(TagRule.Action.REMOVE_FROM_TAG, rule.action());
        assertTrue(rule.itemMatcher().test(Identifier.fromNamespaceAndPath("examplemod", "inedible_food")));
        assertTrue(rule.itemMatcher().test(Identifier.withDefaultNamespace("inedible_apple")));
        assertFalse(rule.itemMatcher().test(Identifier.withDefaultNamespace("apple")));
    }
}
