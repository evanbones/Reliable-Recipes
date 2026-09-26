package com.evandev.reliable_recipes;

//? if >=1.21.2 {
import com.evandev.reliable_recipes.config.RecipeRuleParser;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.test.MinecraftTestBase;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 26.x applies rules to recipes that are already decoded, by round-tripping them through JSON.
 * These tests cover that round trip, which 1.21.1 doesn't need.
 */
public class RecipePipelineTest extends MinecraftTestBase {
    //? if <=26.2 {
    private static final Codec<Recipe<?>> RECIPE_CODEC = Recipe.CODEC;
    //?} else {
    /*private static final Codec<Recipe<?>> RECIPE_CODEC = Recipe.DIRECT_CODEC;
    *///?}

    private static final String IRON_SWORD = """
            {
              "type": "minecraft:crafting_shaped",
              "category": "equipment",
              "key": {
                "#": "minecraft:stick",
                "X": "minecraft:iron_ingot"
              },
              "pattern": ["X", "X", "#"],
              "result": { "id": "minecraft:iron_sword" }
            }
            """;

    private static RegistryOps<JsonElement> ops;

    @BeforeAll
    static void setupOps() {
        setupMinecraft();
        //? if <=26.2 {
        ops = VanillaRegistries.createLookup().createSerializationContext(JsonOps.INSTANCE);
        //?} else {
        /*ops = VanillaRegistries.createWorldLookup().createSerializationContext(JsonOps.INSTANCE);
        *///?}
    }

    private static RecipeHolder<?> recipe(String id, String json) {
        Recipe<?> recipe = RECIPE_CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow();
        return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.parse(id)), recipe);
    }

    private static RecipeHolder<?> process(RecipeHolder<?> holder, String... rules) {
        List<RecipeRule> parsed = java.util.Arrays.stream(rules)
                .map(rule -> RecipeRuleParser.parseRule(JsonParser.parseString(rule).getAsJsonObject()))
                .toList();
        return RecipeModifier.processRecipe(holder, parsed, Map.of(), ops);
    }

    private static JsonObject encode(RecipeHolder<?> holder) {
        return RECIPE_CODEC.encodeStart(ops, holder.value()).getOrThrow().getAsJsonObject();
    }

    @Test
    @DisplayName("Recipes no rule applies to are kept as the same instance")
    void testUnmatchedRecipeUntouched() {
        RecipeHolder<?> holder = recipe("minecraft:iron_sword", IRON_SWORD);
        assertSame(holder, process(holder, """
                {"action": "remove_recipe", "id": "minecraft:diamond_sword"}
                """));
    }

    @Test
    @DisplayName("replace_input swaps an item in a decoded recipe")
    void testReplaceInput() {
        RecipeHolder<?> processed = process(recipe("minecraft:iron_sword", IRON_SWORD), """
                {"action": "replace_input", "target": "minecraft:stick", "replacement": "minecraft:bamboo"}
                """);

        JsonObject key = encode(processed).getAsJsonObject("key");
        assertEquals("minecraft:bamboo", key.get("#").getAsString());
        assertEquals("minecraft:iron_ingot", key.get("X").getAsString());
    }

    @Test
    @DisplayName("replace_input with an array produces an ingredient accepting every replacement")
    void testReplaceInputWithArray() {
        RecipeHolder<?> processed = process(recipe("minecraft:iron_sword", IRON_SWORD), """
                {"action": "replace_input", "target": "minecraft:stick", "replacement": ["minecraft:bamboo", "minecraft:stick"]}
                """);

        JsonElement stickKey = encode(processed).getAsJsonObject("key").get("#");
        assertTrue(stickKey.isJsonArray());
        assertEquals(2, stickKey.getAsJsonArray().size());
    }

    @Test
    @DisplayName("replace_output swaps the result of a decoded recipe")
    void testReplaceOutput() {
        RecipeHolder<?> processed = process(recipe("minecraft:iron_sword", IRON_SWORD), """
                {"action": "replace_output", "filter": {"id": "minecraft:iron_sword"}, "replacement": "minecraft:golden_sword"}
                """);

        assertEquals("minecraft:golden_sword", encode(processed).getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    @DisplayName("Type filters match both the serializer and the recipe type")
    void testTypeFilters() {
        RecipeHolder<?> holder = recipe("minecraft:iron_sword", IRON_SWORD);
        assertNull(process(holder, """
                {"action": "remove_recipe", "type": "minecraft:crafting_shaped"}
                """));
        assertNull(process(holder, """
                {"action": "remove_recipe", "type": "minecraft:crafting"}
                """));
        assertSame(holder, process(holder, """
                {"action": "remove_recipe", "type": "minecraft:smelting"}
                """));
    }

    @Test
    @DisplayName("Input and output filters work on decoded recipes")
    void testInputOutputFilters() {
        RecipeHolder<?> holder = recipe("minecraft:iron_sword", IRON_SWORD);
        assertNull(process(holder, """
                {"action": "remove_output", "target": "minecraft:iron_sword"}
                """));
        assertNull(process(holder, """
                {"action": "remove_recipe", "input": "minecraft:iron_ingot"}
                """));
        assertSame(holder, process(holder, """
                {"action": "remove_recipe", "input": "minecraft:gold_ingot"}
                """));
    }
}
//?} else {
/*public class RecipePipelineTest {
}
*///?}
