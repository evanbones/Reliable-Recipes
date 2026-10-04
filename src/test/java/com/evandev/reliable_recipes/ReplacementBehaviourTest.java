package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.config.RecipeRuleParser;
import com.evandev.reliable_recipes.recipe.RecipeJsonMutator;
import com.evandev.reliable_recipes.test.MinecraftTestBase;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReplacementBehaviourTest extends MinecraftTestBase {

    private static JsonObject objectFormat(String recipe, String rule) {
        return applyRule(recipe, rule, RecipeJsonMutator.IngredientFormat.OBJECT);
    }

    private static JsonObject stringFormat(String recipe, String rule) {
        return applyRule(recipe, rule, RecipeJsonMutator.IngredientFormat.STRING);
    }

    private static JsonObject applyRule(String recipe, String rule, RecipeJsonMutator.IngredientFormat format) {
        JsonObject json = JsonParser.parseString(recipe).getAsJsonObject();
        RecipeJsonMutator.applyRule(json, RecipeRuleParser.parseRule(JsonParser.parseString(rule).getAsJsonObject()), format);
        return json;
    }

    private static JsonObject objectFormatGlobal(String recipe, String from, String to) {
        return applyGlobal(recipe, from, to, RecipeJsonMutator.IngredientFormat.OBJECT);
    }

    private static JsonObject stringFormatGlobal(String recipe, String from, String to) {
        return applyGlobal(recipe, from, to, RecipeJsonMutator.IngredientFormat.STRING);
    }

    private static JsonObject applyGlobal(String recipe, String from, String to, RecipeJsonMutator.IngredientFormat format) {
        JsonObject json = JsonParser.parseString(recipe).getAsJsonObject();
        Map<String, JsonElement> replacements = Map.of(from, new JsonPrimitive(to));
        RecipeJsonMutator.mutateRecipe(json, replacements, replacements, format);
        return json;
    }

    private static void assertJson(String expected, JsonObject actual) {
        assertEquals(JsonParser.parseString(expected), actual);
    }

    @Nested
    @DisplayName("Object ingredient format (1.20.1 / 1.21.1)")
    class ObjectFormat {

        private static final String SHAPED = """
                {
                  "type": "minecraft:crafting_shaped",
                  "key": { "#": { "item": "minecraft:stick" }, "X": { "tag": "c:ingots/iron" } },
                  "pattern": ["X", "#"],
                  "result": { "count": 1, "id": "minecraft:stick" }
                }
                """;

        private static final String SHAPELESS = """
                {
                  "type": "minecraft:crafting_shapeless",
                  "ingredients": [ { "item": "minecraft:coal" }, { "item": "minecraft:stick" } ],
                  "result": { "count": 4, "id": "minecraft:torch" }
                }
                """;

        private static final String CUSTOM = """
                {
                  "type": "examplemod:infusing",
                  "catalysts": [ { "item": "minecraft:coal" } ],
                  "input": { "item": "minecraft:coal" }
                }
                """;

        private static final String SMELTING_STRING_RESULT = """
                {
                  "type": "minecraft:smelting",
                  "ingredient": { "item": "minecraft:iron_ore" },
                  "result": "minecraft:iron_ingot",
                  "experience": 0.7,
                  "cookingtime": 200
                }
                """;

        private static final String CRUSHING = """
                {
                  "type": "create:crushing",
                  "ingredients": [ { "item": "minecraft:diamond_ore" } ],
                  "results": [
                    { "id": "minecraft:diamond" },
                    { "id": "minecraft:diamond", "chance": 0.75 },
                    { "id": "minecraft:cobblestone", "chance": 0.125 }
                  ],
                  "processingTime": 350
                }
                """;

        private static final String TYPED_INGREDIENTS = """
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
                    "B": { "item": "bountifulfares:walnut" },
                    "C": { "type": "neoforge:components", "items": "bountifulfares:walnut", "components": {} }
                  },
                  "pattern": ["ABC"],
                  "result": { "id": "minecraft:oak_planks" }
                }
                """;

        private static final String OUTPUTS_KEY = """
                {
                  "type": "examplemod:press",
                  "ingredients": [ { "item": "minecraft:stick" } ],
                  "outputs": [ { "id": "minecraft:stick", "count": 2 } ]
                }
                """;

        @Test
        @DisplayName("replace_input swaps an item ingredient and leaves a matching result alone")
        void replaceInputItem() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": { "item": "minecraft:bamboo" }, "X": { "tag": "c:ingots/iron" } },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:stick" }
                    }
                    """, objectFormat(SHAPED, """
                    {"action": "replace_input", "target": "minecraft:stick", "replacement": "minecraft:bamboo"}
                    """));
        }

        @Test
        @DisplayName("replace_input with an array turns a key entry into an array of ingredient objects")
        void replaceInputArrayInKey() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": [ { "item": "minecraft:bamboo" }, { "tag": "c:rods" } ], "X": { "tag": "c:ingots/iron" } },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:stick" }
                    }
                    """, objectFormat(SHAPED, """
                    {"action": "replace_input", "target": "minecraft:stick", "replacement": ["minecraft:bamboo", "#c:rods"]}
                    """));
        }

        @Test
        @DisplayName("replace_input with a tag swaps the item key for a tag key")
        void replaceInputWithTag() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": { "tag": "c:rods" }, "X": { "tag": "c:ingots/iron" } },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:stick" }
                    }
                    """, objectFormat(SHAPED, """
                    {"action": "replace_input", "target": "minecraft:stick", "replacement": "#c:rods"}
                    """));
        }

        @Test
        @DisplayName("replace_input targeting a tag swaps the tag key for an item key")
        void replaceInputTagTarget() {
            String expected = """
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": { "item": "minecraft:stick" }, "X": { "item": "minecraft:gold_ingot" } },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:stick" }
                    }
                    """;
            assertJson(expected, objectFormat(SHAPED, """
                    {"action": "replace_input", "target": "#c:ingots/iron", "replacement": "minecraft:gold_ingot"}
                    """));
            assertJson(expected, objectFormat(SHAPED, """
                    {"action": "replace_input", "target": "c:ingots/iron", "replacement": "minecraft:gold_ingot"}
                    """));
        }

        @Test
        @DisplayName("replace_output swaps a targeted result id and leaves matching ingredients alone")
        void replaceOutputTargeted() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": { "item": "minecraft:stick" }, "X": { "tag": "c:ingots/iron" } },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:bamboo" }
                    }
                    """, objectFormat(SHAPED, """
                    {"action": "replace_output", "target": "minecraft:stick", "replacement": "minecraft:bamboo"}
                    """));
        }

        @Test
        @DisplayName("replace_output without a target swaps the result id and keeps the count")
        void replaceOutputWildcard() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shapeless",
                      "ingredients": [ { "item": "minecraft:coal" }, { "item": "minecraft:stick" } ],
                      "result": { "count": 4, "id": "minecraft:soul_torch" }
                    }
                    """, objectFormat(SHAPELESS, """
                    {"action": "replace_output", "replacement": "minecraft:soul_torch"}
                    """));
        }

        @Test
        @DisplayName("replace_output with an object replaces the whole result")
        void replaceOutputObject() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shapeless",
                      "ingredients": [ { "item": "minecraft:coal" }, { "item": "minecraft:stick" } ],
                      "result": { "id": "minecraft:soul_torch", "count": 8 }
                    }
                    """, objectFormat(SHAPELESS, """
                    {"action": "replace_output", "result": { "id": "minecraft:soul_torch", "count": 8 }}
                    """));
        }

        @Test
        @DisplayName("replace_output with a tag writes a tag into the result")
        void replaceOutputWithTag() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shapeless",
                      "ingredients": [ { "item": "minecraft:coal" }, { "item": "minecraft:stick" } ],
                      "result": { "count": 4, "tag": "c:torches" }
                    }
                    """, objectFormat(SHAPELESS, """
                    {"action": "replace_output", "replacement": "#c:torches"}
                    """));
        }

        @Test
        @DisplayName("replace_input with an array inside an ingredient list nests the alternatives")
        void replaceInputArrayInIngredientList() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shapeless",
                      "ingredients": [ [ { "item": "minecraft:charcoal" }, { "item": "minecraft:coal" } ], { "item": "minecraft:stick" } ],
                      "result": { "count": 4, "id": "minecraft:torch" }
                    }
                    """, objectFormat(SHAPELESS, """
                    {"action": "replace_input", "target": "minecraft:coal", "replacement": ["minecraft:charcoal", "minecraft:coal"]}
                    """));
        }

        @Test
        @DisplayName("replace_input in an ingredient list swaps the item in place")
        void replaceInputInIngredientList() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shapeless",
                      "ingredients": [ { "item": "minecraft:charcoal" }, { "item": "minecraft:stick" } ],
                      "result": { "count": 4, "id": "minecraft:torch" }
                    }
                    """, objectFormat(SHAPELESS, """
                    {"action": "replace_input", "target": "minecraft:coal", "replacement": "minecraft:charcoal"}
                    """));
        }

        @Test
        @DisplayName("replace_input with an array flattens into other lists and wraps a single object")
        void replaceInputArrayOutsideIngredientList() {
            assertJson("""
                    {
                      "type": "examplemod:infusing",
                      "catalysts": [ { "item": "minecraft:charcoal" }, { "item": "minecraft:blaze_powder" } ],
                      "input": [ { "item": "minecraft:charcoal" }, { "item": "minecraft:blaze_powder" } ]
                    }
                    """, objectFormat(CUSTOM, """
                    {"action": "replace_input", "target": "minecraft:coal", "replacement": ["minecraft:charcoal", "minecraft:blaze_powder"]}
                    """));
        }

        @Test
        @DisplayName("replace_output swaps a plain string result")
        void replaceOutputStringResult() {
            assertJson("""
                    {
                      "type": "minecraft:smelting",
                      "ingredient": { "item": "minecraft:iron_ore" },
                      "result": "minecraft:gold_ingot",
                      "experience": 0.7,
                      "cookingtime": 200
                    }
                    """, objectFormat(SMELTING_STRING_RESULT, """
                    {"action": "replace_output", "target": "minecraft:iron_ingot", "replacement": "minecraft:gold_ingot"}
                    """));
        }

        @Test
        @DisplayName("replace_input leaves a plain string result alone")
        void replaceInputSkipsStringResult() {
            assertJson(SMELTING_STRING_RESULT, objectFormat(SMELTING_STRING_RESULT, """
                    {"action": "replace_input", "target": "minecraft:iron_ingot", "replacement": "minecraft:gold_ingot"}
                    """));
        }

        @Test
        @DisplayName("replace_output swaps every matching entry in a results list and keeps their extra fields")
        void replaceOutputResultsList() {
            assertJson("""
                    {
                      "type": "create:crushing",
                      "ingredients": [ { "item": "minecraft:diamond_ore" } ],
                      "results": [
                        { "id": "spelunkery:rough_diamond" },
                        { "id": "spelunkery:rough_diamond", "chance": 0.75 },
                        { "id": "minecraft:cobblestone", "chance": 0.125 }
                      ],
                      "processingTime": 350
                    }
                    """, objectFormat(CRUSHING, """
                    {"action": "replace_output", "target": "minecraft:diamond", "replacement": "spelunkery:rough_diamond"}
                    """));
        }

        @Test
        @DisplayName("replace_input only touches item fields of typed custom ingredients")
        void replaceInputTypedIngredients() {
            assertJson("""
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
                        "B": { "item": "nomansland:walnuts" },
                        "C": { "type": "neoforge:components", "items": "nomansland:walnuts", "components": {} }
                      },
                      "pattern": ["ABC"],
                      "result": { "id": "minecraft:oak_planks" }
                    }
                    """, objectFormat(TYPED_INGREDIENTS, """
                    {"action": "replace_input", "target": "bountifulfares:walnut", "replacement": "nomansland:walnuts"}
                    """));
        }

        @Test
        @DisplayName("replace_input reaches ingredients nested inside typed custom ingredients")
        void replaceInputInsideTypedIngredient() {
            JsonObject result = objectFormat(TYPED_INGREDIENTS, """
                    {"action": "replace_input", "target": "minecraft:oak_log", "replacement": "minecraft:birch_log"}
                    """);
            assertEquals("minecraft:birch_log", result.getAsJsonObject("key").getAsJsonObject("A")
                    .getAsJsonObject("ingredient").get("item").getAsString());
        }

        @Test
        @DisplayName("replace_input also rewrites entries under an outputs key")
        void replaceInputOutputsKey() {
            assertJson("""
                    {
                      "type": "examplemod:press",
                      "ingredients": [ { "item": "minecraft:bamboo" } ],
                      "outputs": [ { "id": "minecraft:bamboo", "count": 2 } ]
                    }
                    """, objectFormat(OUTPUTS_KEY, """
                    {"action": "replace_input", "target": "minecraft:stick", "replacement": "minecraft:bamboo"}
                    """));
        }

        @Test
        @DisplayName("Global replacements rewrite both ingredients and results")
        void globalReplacement() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": { "item": "minecraft:bamboo" }, "X": { "tag": "c:ingots/iron" } },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:bamboo" }
                    }
                    """, objectFormatGlobal(SHAPED, "minecraft:stick", "minecraft:bamboo"));
        }
    }

    @Nested
    @DisplayName("String ingredient format (1.21.2+)")
    class StringFormat {

        private static final String SHAPED = """
                {
                  "type": "minecraft:crafting_shaped",
                  "key": { "#": "minecraft:stick", "X": "#c:ingots/iron" },
                  "pattern": ["X", "#"],
                  "result": { "count": 1, "id": "minecraft:stick" }
                }
                """;

        private static final String SHAPELESS = """
                {
                  "type": "minecraft:crafting_shapeless",
                  "ingredients": [ "minecraft:coal", "minecraft:stick" ],
                  "result": { "count": 4, "id": "minecraft:torch" }
                }
                """;

        private static final String CRUSHING = """
                {
                  "type": "create:crushing",
                  "ingredients": [ "minecraft:diamond_ore" ],
                  "results": [
                    { "id": "minecraft:diamond" },
                    { "id": "minecraft:diamond", "chance": 0.75 },
                    { "id": "minecraft:cobblestone", "chance": 0.125 }
                  ],
                  "processingTime": 350
                }
                """;

        private static final String TYPED_INGREDIENTS = """
                {
                  "type": "minecraft:crafting_shaped",
                  "key": {
                    "A": {
                      "type": "moonlight:block_type_swap",
                      "ingredient": "minecraft:oak_log",
                      "block_type": "minecraft:wood_type",
                      "from": "minecraft:oak",
                      "to": "bountifulfares:walnut"
                    },
                    "B": "bountifulfares:walnut"
                  },
                  "pattern": ["AB"],
                  "result": { "id": "minecraft:oak_planks" }
                }
                """;

        private static final String OUTPUTS_KEY = """
                {
                  "type": "examplemod:press",
                  "ingredients": [ "minecraft:stick" ],
                  "outputs": [ { "id": "minecraft:stick", "count": 2 } ]
                }
                """;

        @Test
        @DisplayName("replace_input swaps an item ingredient and leaves a matching result alone")
        void replaceInputItem() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": "minecraft:bamboo", "X": "#c:ingots/iron" },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:stick" }
                    }
                    """, stringFormat(SHAPED, """
                    {"action": "replace_input", "target": "minecraft:stick", "replacement": "minecraft:bamboo"}
                    """));
        }

        @Test
        @DisplayName("replace_input with an array turns a key entry into a list of alternatives")
        void replaceInputArrayInKey() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": ["minecraft:bamboo", "#c:rods"], "X": "#c:ingots/iron" },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:stick" }
                    }
                    """, stringFormat(SHAPED, """
                    {"action": "replace_input", "target": "minecraft:stick", "replacement": ["minecraft:bamboo", "#c:rods"]}
                    """));
        }

        @Test
        @DisplayName("replace_input with a tag writes the tag string")
        void replaceInputWithTag() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": "#c:rods", "X": "#c:ingots/iron" },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:stick" }
                    }
                    """, stringFormat(SHAPED, """
                    {"action": "replace_input", "target": "minecraft:stick", "replacement": "#c:rods"}
                    """));
        }

        @Test
        @DisplayName("replace_input targeting a tag swaps it for an item")
        void replaceInputTagTarget() {
            String expected = """
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": "minecraft:stick", "X": "minecraft:gold_ingot" },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:stick" }
                    }
                    """;
            assertJson(expected, stringFormat(SHAPED, """
                    {"action": "replace_input", "target": "#c:ingots/iron", "replacement": "minecraft:gold_ingot"}
                    """));
            assertJson(expected, stringFormat(SHAPED, """
                    {"action": "replace_input", "target": "c:ingots/iron", "replacement": "minecraft:gold_ingot"}
                    """));
        }

        @Test
        @DisplayName("replace_output swaps a targeted result id and leaves matching ingredients alone")
        void replaceOutputTargeted() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": "minecraft:stick", "X": "#c:ingots/iron" },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:bamboo" }
                    }
                    """, stringFormat(SHAPED, """
                    {"action": "replace_output", "target": "minecraft:stick", "replacement": "minecraft:bamboo"}
                    """));
        }

        @Test
        @DisplayName("replace_output without a target swaps the result id and keeps the count")
        void replaceOutputWildcard() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shapeless",
                      "ingredients": [ "minecraft:coal", "minecraft:stick" ],
                      "result": { "count": 4, "id": "minecraft:soul_torch" }
                    }
                    """, stringFormat(SHAPELESS, """
                    {"action": "replace_output", "replacement": "minecraft:soul_torch"}
                    """));
        }

        @Test
        @DisplayName("replace_output with an object replaces the whole result")
        void replaceOutputObject() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shapeless",
                      "ingredients": [ "minecraft:coal", "minecraft:stick" ],
                      "result": { "id": "minecraft:soul_torch", "count": 8 }
                    }
                    """, stringFormat(SHAPELESS, """
                    {"action": "replace_output", "result": { "id": "minecraft:soul_torch", "count": 8 }}
                    """));
        }

        @Test
        @DisplayName("replace_output refuses to write a tag into the result")
        void replaceOutputWithTag() {
            assertJson(SHAPELESS, stringFormat(SHAPELESS, """
                    {"action": "replace_output", "replacement": "#c:torches"}
                    """));
        }

        @Test
        @DisplayName("replace_output with an array uses its first entry for the result id")
        void replaceOutputArray() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shapeless",
                      "ingredients": [ "minecraft:coal", "minecraft:stick" ],
                      "result": { "count": 4, "id": "minecraft:soul_torch" }
                    }
                    """, stringFormat(SHAPELESS, """
                    {"action": "replace_output", "replacement": ["minecraft:soul_torch", "minecraft:redstone_torch"]}
                    """));
        }

        @Test
        @DisplayName("replace_input with an array inside an ingredient list nests the alternatives")
        void replaceInputArrayInIngredientList() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shapeless",
                      "ingredients": [ ["minecraft:charcoal", "minecraft:coal"], "minecraft:stick" ],
                      "result": { "count": 4, "id": "minecraft:torch" }
                    }
                    """, stringFormat(SHAPELESS, """
                    {"action": "replace_input", "target": "minecraft:coal", "replacement": ["minecraft:charcoal", "minecraft:coal"]}
                    """));
        }

        @Test
        @DisplayName("replace_output swaps every matching entry in a results list and keeps their extra fields")
        void replaceOutputResultsList() {
            assertJson("""
                    {
                      "type": "create:crushing",
                      "ingredients": [ "minecraft:diamond_ore" ],
                      "results": [
                        { "id": "spelunkery:rough_diamond" },
                        { "id": "spelunkery:rough_diamond", "chance": 0.75 },
                        { "id": "minecraft:cobblestone", "chance": 0.125 }
                      ],
                      "processingTime": 350
                    }
                    """, stringFormat(CRUSHING, """
                    {"action": "replace_output", "target": "minecraft:diamond", "replacement": "spelunkery:rough_diamond"}
                    """));
        }

        @Test
        @DisplayName("replace_input only touches item fields of typed custom ingredients")
        void replaceInputTypedIngredients() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": {
                        "A": {
                          "type": "moonlight:block_type_swap",
                          "ingredient": "minecraft:oak_log",
                          "block_type": "minecraft:wood_type",
                          "from": "minecraft:oak",
                          "to": "bountifulfares:walnut"
                        },
                        "B": "nomansland:walnuts"
                      },
                      "pattern": ["AB"],
                      "result": { "id": "minecraft:oak_planks" }
                    }
                    """, stringFormat(TYPED_INGREDIENTS, """
                    {"action": "replace_input", "target": "bountifulfares:walnut", "replacement": "nomansland:walnuts"}
                    """));
        }

        @Test
        @DisplayName("replace_input reaches ingredients nested inside typed custom ingredients")
        void replaceInputInsideTypedIngredient() {
            JsonObject result = stringFormat(TYPED_INGREDIENTS, """
                    {"action": "replace_input", "target": "minecraft:oak_log", "replacement": "minecraft:birch_log"}
                    """);
            assertEquals("minecraft:birch_log", result.getAsJsonObject("key").getAsJsonObject("A")
                    .get("ingredient").getAsString());
        }

        @Test
        @DisplayName("replace_input leaves entries under an outputs key alone")
        void replaceInputOutputsKey() {
            assertJson("""
                    {
                      "type": "examplemod:press",
                      "ingredients": [ "minecraft:bamboo" ],
                      "outputs": [ { "id": "minecraft:stick", "count": 2 } ]
                    }
                    """, stringFormat(OUTPUTS_KEY, """
                    {"action": "replace_input", "target": "minecraft:stick", "replacement": "minecraft:bamboo"}
                    """));
        }

        @Test
        @DisplayName("Global replacements rewrite both ingredients and results")
        void globalReplacement() {
            assertJson("""
                    {
                      "type": "minecraft:crafting_shaped",
                      "key": { "#": "minecraft:bamboo", "X": "#c:ingots/iron" },
                      "pattern": ["X", "#"],
                      "result": { "count": 1, "id": "minecraft:bamboo" }
                    }
                    """, stringFormatGlobal(SHAPED, "minecraft:stick", "minecraft:bamboo"));
        }
    }
}
