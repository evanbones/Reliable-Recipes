package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.RecipeRuleParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BrewingRecipe implements Recipe<Container> {

    public static final RecipeType<BrewingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "minecraft:brewing";
        }
    };

    public static final RecipeSerializer<BrewingRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public BrewingRecipe fromJson(ResourceLocation id, JsonObject json) {
            BrewingInputMatcher input = parseInputMatcher(json.get("input"));
            Ingredient reagent = RecipeRuleParser.parseIngredient(json.get("reagent"));
            ItemStack output = parseOutputStack(json.get("output"));
            return new BrewingRecipe(id, input, reagent, output);
        }

        @Override
        public BrewingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            BrewingInputMatcher input = BrewingInputMatcher.fromNetwork(buf);
            Ingredient reagent = Ingredient.fromNetwork(buf);
            ItemStack output = buf.readItem();
            return new BrewingRecipe(id, input, reagent, output);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, BrewingRecipe recipe) {
            recipe.input.toNetwork(buf);
            recipe.reagent.toNetwork(buf);
            buf.writeItem(recipe.output);
        }
    };

    private final ResourceLocation id;
    private final BrewingInputMatcher input;
    private final Ingredient reagent;
    private final ItemStack output;

    public BrewingRecipe(ResourceLocation id, BrewingInputMatcher input, Ingredient reagent, ItemStack output) {
        this.id = id != null ? id : new ResourceLocation(Constants.MOD_ID, "brewing_" + Math.abs(input.hashCode() ^ reagent.hashCode() ^ output.hashCode()));
        this.input = input;
        this.reagent = reagent;
        this.output = output;
    }

    public BrewingRecipe(BrewingInputMatcher input, Ingredient reagent, ItemStack output) {
        this(null, input, reagent, output);
    }

    private static BrewingInputMatcher parseInputMatcher(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return new BrewingInputMatcher(Ingredient.EMPTY, Optional.empty());
        }

        if (json.isJsonPrimitive()) {
            Ingredient ing = RecipeRuleParser.parseIngredientString(json.getAsString());
            return new BrewingInputMatcher(ing, Optional.empty());
        }

        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            Ingredient ing = Ingredient.EMPTY;

            if (obj.has("ingredient")) {
                ing = RecipeRuleParser.parseIngredient(obj.get("ingredient"));
            } else if (obj.has("item")) {
                ing = RecipeRuleParser.parseIngredient(obj.get("item"));
            } else if (obj.has("tag")) {
                ing = RecipeRuleParser.parseIngredientString("#" + obj.get("tag").getAsString());
            }

            Optional<List<ResourceLocation>> potionContents = parsePotionLocations(obj);
            return new BrewingInputMatcher(ing, potionContents);
        }

        return new BrewingInputMatcher(RecipeRuleParser.parseIngredient(json), Optional.empty());
    }

    private static Optional<List<ResourceLocation>> parsePotionLocations(JsonObject obj) {
        List<ResourceLocation> locations = new ArrayList<>();

        if (obj.has("potion_contents")) {
            JsonElement pc = obj.get("potion_contents");
            if (pc.isJsonPrimitive()) {
                ResourceLocation loc = ResourceLocation.tryParse(pc.getAsString());
                if (loc != null) locations.add(loc);
            } else if (pc.isJsonObject()) {
                JsonObject pcObj = pc.getAsJsonObject();
                if (pcObj.has("potions") && pcObj.get("potions").isJsonArray()) {
                    for (JsonElement el : pcObj.getAsJsonArray("potions")) {
                        if (el.isJsonPrimitive()) {
                            ResourceLocation loc = ResourceLocation.tryParse(el.getAsString());
                            if (loc != null) locations.add(loc);
                        }
                    }
                } else if (pcObj.has("potion") && pcObj.get("potion").isJsonPrimitive()) {
                    ResourceLocation loc = ResourceLocation.tryParse(pcObj.get("potion").getAsString());
                    if (loc != null) locations.add(loc);
                } else if (pcObj.has("potions") && pcObj.get("potions").isJsonPrimitive()) {
                    ResourceLocation loc = ResourceLocation.tryParse(pcObj.get("potions").getAsString());
                    if (loc != null) locations.add(loc);
                }
            }
        } else if (obj.has("potion") && obj.get("potion").isJsonPrimitive()) {
            ResourceLocation loc = ResourceLocation.tryParse(obj.get("potion").getAsString());
            if (loc != null) locations.add(loc);
        } else if (obj.has("potions") && obj.get("potions").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("potions")) {
                if (el.isJsonPrimitive()) {
                    ResourceLocation loc = ResourceLocation.tryParse(el.getAsString());
                    if (loc != null) locations.add(loc);
                }
            }
        }

        return locations.isEmpty() ? Optional.empty() : Optional.of(locations);
    }

    private static ItemStack parseOutputStack(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return ItemStack.EMPTY;
        }

        if (json.isJsonPrimitive()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(json.getAsString()));
            return item != Items.AIR ? new ItemStack(item) : ItemStack.EMPTY;
        }

        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            String itemId = null;
            if (obj.has("id") && obj.get("id").isJsonPrimitive()) {
                itemId = obj.get("id").getAsString();
            } else if (obj.has("item") && obj.get("item").isJsonPrimitive()) {
                itemId = obj.get("item").getAsString();
            }

            Item item = itemId != null ? BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemId)) : Items.AIR;
            int count = GsonHelper.getAsInt(obj, "count", 1);
            ItemStack stack = item != Items.AIR ? new ItemStack(item, count) : ItemStack.EMPTY;

            // Handle 26.3 components or direct potion property
            String potionId = null;
            if (obj.has("components") && obj.get("components").isJsonObject()) {
                JsonObject comp = obj.getAsJsonObject("components");
                if (comp.has("minecraft:potion_contents") && comp.get("minecraft:potion_contents").isJsonObject()) {
                    JsonObject pc = comp.getAsJsonObject("minecraft:potion_contents");
                    if (pc.has("potion") && pc.get("potion").isJsonPrimitive()) {
                        potionId = pc.get("potion").getAsString();
                    }
                }
            }
            if (potionId == null && obj.has("potion") && obj.get("potion").isJsonPrimitive()) {
                potionId = obj.get("potion").getAsString();
            }

            if (potionId != null && !stack.isEmpty()) {
                ResourceLocation pLoc = ResourceLocation.tryParse(potionId);
                if (pLoc != null && BuiltInRegistries.POTION.containsKey(pLoc)) {
                    Potion potion = BuiltInRegistries.POTION.get(pLoc);
                    PotionUtils.setPotion(stack, potion);
                }
            }

            if (obj.has("nbt") && !stack.isEmpty()) {
                try {
                    if (obj.get("nbt").isJsonObject()) {
                        stack.setTag(TagParser.parseTag(obj.get("nbt").toString()));
                    } else if (obj.get("nbt").isJsonPrimitive()) {
                        stack.setTag(TagParser.parseTag(obj.get("nbt").getAsString()));
                    }
                } catch (CommandSyntaxException ignored) {
                }
            }

            return stack;
        }

        return ItemStack.EMPTY;
    }

    public BrewingInputMatcher getInputMatcher() {
        return input;
    }

    public Ingredient getReagent() {
        return reagent;
    }

    public ItemStack getOutput() {
        return output;
    }

    public boolean matches(ItemStack inputStack, ItemStack reagentStack) {
        return this.input.matches(inputStack) && this.reagent.test(reagentStack);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return this.input.matches(container.getItem(0));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registries) {
        return this.output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return this.output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.input.ingredient());
        ingredients.add(this.reagent);
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    public record BrewingInputMatcher(Ingredient ingredient, Optional<List<ResourceLocation>> potionContents) {
        public static BrewingInputMatcher fromNetwork(FriendlyByteBuf buf) {
            Ingredient ing = Ingredient.fromNetwork(buf);
            boolean hasPotionContents = buf.readBoolean();
            Optional<List<ResourceLocation>> potionContents = Optional.empty();
            if (hasPotionContents) {
                int size = buf.readVarInt();
                List<ResourceLocation> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(buf.readResourceLocation());
                }
                potionContents = Optional.of(list);
            }
            return new BrewingInputMatcher(ing, potionContents);
        }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            if (!ingredient.isEmpty() && !ingredient.test(stack)) {
                return false;
            }
            if (potionContents.isPresent() && !potionContents.get().isEmpty()) {
                Potion potion = PotionUtils.getPotion(stack);
                ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potion);
                return potionContents.get().contains(potionId);
            }
            return true;
        }

        public void toNetwork(FriendlyByteBuf buf) {
            ingredient.toNetwork(buf);
            buf.writeBoolean(potionContents.isPresent());
            if (potionContents.isPresent()) {
                List<ResourceLocation> list = potionContents.get();
                buf.writeVarInt(list.size());
                for (ResourceLocation rl : list) {
                    buf.writeResourceLocation(rl);
                }
            }
        }
    }
}
