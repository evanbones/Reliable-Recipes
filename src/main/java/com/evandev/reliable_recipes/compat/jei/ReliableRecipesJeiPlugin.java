package com.evandev.reliable_recipes.compat.jei;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.util.CompatUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.*;

//? if <=26.2 {
import com.evandev.reliable_recipes.recipe.BrewingRecipe;
import com.evandev.reliable_recipes.recipe.BrewingRecipeManager;
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe;
//?}
//? if <1.21 {
/*import net.minecraft.world.item.alchemy.PotionUtils;
*///?} else if <=26.2 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.alchemy.PotionContents;
//?}

@JeiPlugin
public class ReliableRecipesJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "jei_plugin");
    private static final JsonObject EMPTY_RECIPE_JSON = new JsonObject();
    private static final List<Runnable> RULE_HIDDEN = new ArrayList<>();
    private static final Map<Identifier, Runnable> DELETED = new HashMap<>();
    private static IJeiRuntime runtime;
    private static List<ItemStack> allItems = List.of();

    private static void runLater(Runnable task) {
        //? if >=26.1 {
        Minecraft.getInstance().schedule(task);
        //?} else {
        /*Minecraft.getInstance().tell(task);
         *///?}
    }

    /**
     * Re-evaluates which recipes the current rules hide.
     */
    public static void refresh() {
        IJeiRuntime jeiRuntime = runtime;
        if (jeiRuntime == null) return;

        RULE_HIDDEN.forEach(Runnable::run);
        RULE_HIDDEN.clear();
        hideRemovedRecipes(jeiRuntime);
    }

    /**
     * Hides every recipe with the given id.
     *
     * @return the first output of a hidden recipe, for display in the removal toast, or an empty stack.
     */
    public static ItemStack hideDeletedRecipe(Identifier recipeId) {
        IJeiRuntime jeiRuntime = runtime;
        if (jeiRuntime == null) return ItemStack.EMPTY;

        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
        IFocusGroup focuses = jeiRuntime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        List<Runnable> undo = new ArrayList<>();
        ItemStack icon = ItemStack.EMPTY;
        for (IRecipeCategory<?> category : recipeManager.createRecipeCategoryLookup().get().toList()) {
            ItemStack output = hideById(recipeManager, category, recipeId, focuses, undo);
            if (icon.isEmpty()) icon = output;
        }

        if (!undo.isEmpty()) {
            Runnable previous = DELETED.put(recipeId, () -> undo.forEach(Runnable::run));
            if (previous != null) previous.run();
        }
        return icon;
    }

    public static void restoreDeletedRecipe(Identifier recipeId) {
        Runnable undo = DELETED.remove(recipeId);
        if (undo != null && runtime != null) undo.run();
    }

    private static <T> ItemStack hideById(IRecipeManager recipeManager, IRecipeCategory<T> category, Identifier recipeId, IFocusGroup focuses, List<Runnable> undo) {
        List<T> matches = new ArrayList<>();
        recipeManager.createRecipeLookup(category.getRecipeType()).get().forEach(recipe -> {
            try {
                if (recipeId.equals(getRecipeId(category, recipe))) matches.add(recipe);
            } catch (Exception ignored) {
            }
        });
        if (matches.isEmpty()) return ItemStack.EMPTY;

        recipeManager.hideRecipes(category.getRecipeType(), matches);
        undo.add(() -> recipeManager.unhideRecipes(category.getRecipeType(), matches));

        return recipeManager.createRecipeLayoutDrawable(category, matches.get(0), focuses)
                .flatMap(layout -> layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                        .flatMap(IRecipeSlotView::getItemStacks)
                        .filter(stack -> !stack.isEmpty())
                        .findFirst())
                .orElse(ItemStack.EMPTY);
    }

    static <T> Identifier getRecipeId(IRecipeCategory<T> category, T recipe) {
        //? if >=26.1 {
        return category.getIdentifier(recipe);
        //?} else {
        /*return category.getRegistryName(recipe);
         *///?}
    }

    private static void hideRemovedRecipes(IJeiRuntime jeiRuntime) {
        List<RecipeRule> rules = RecipeConfigIO.loadRules().stream()
                .filter(rule -> rule.getAction() == RecipeRule.Action.REMOVE)
                .toList();
        boolean needsContents = rules.stream().anyMatch(RecipeRule::needsRecipeJson);

        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
        IFocusFactory focusFactory = jeiRuntime.getJeiHelpers().getFocusFactory();
        List<IFocus<?>> hiddenFocuses = getHiddenItemFocuses(focusFactory);
        IFocusGroup noFocus = focusFactory.getEmptyFocusGroup();
        for (IRecipeCategory<?> category : recipeManager.createRecipeCategoryLookup().get().toList()) {
            try {
                hideInCategory(recipeManager, category, noFocus, rules, needsContents, hiddenFocuses);
            } catch (Exception e) {
                Constants.LOG.warn("Failed to filter JEI recipe category {}", category.getRecipeType().getUid(), e);
            }
        }
    }

    /**
     * Focuses on every hidden JEI ingredient, so JEI's own index can find the recipes that use them.
     */
    private static List<IFocus<?>> getHiddenItemFocuses(IFocusFactory focusFactory) {
        if (!ReliableRecipesAPI.hasItemHidingCapabilities()) return List.of();

        List<IFocus<?>> focuses = new ArrayList<>();
        for (ItemStack stack : allItems) {
            if (!ReliableRecipesAPI.isItemHidden(stack)) continue;
            focuses.add(focusFactory.createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, stack));
            focuses.add(focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
        }
        return focuses;
    }

    /**
     * Only reads a recipe's contents when something could need them, since building a JEI layout for every recipe is slow in big packs.
     */
    private static <T> void hideInCategory(IRecipeManager recipeManager, IRecipeCategory<T> category, IFocusGroup noFocus,
                                           List<RecipeRule> rules, boolean needsContents, List<IFocus<?>> hiddenFocuses) {
        var recipeType = category.getRecipeType();
        Identifier typeId = recipeType.getUid();
        boolean inspectAll = needsContents || isRepairCategory(typeId.getPath());

        Set<T> withHiddenItems = new LinkedHashSet<>();
        if (!inspectAll && !hiddenFocuses.isEmpty()) {
            recipeManager.createRecipeLookup(recipeType).limitFocus(hiddenFocuses).get().forEach(withHiddenItems::add);
        }
        if (!inspectAll && rules.isEmpty() && withHiddenItems.isEmpty()) return;

        List<T> toHide = new ArrayList<>();
        for (T recipe : recipeManager.createRecipeLookup(recipeType).get().toList()) {
            try {
                Identifier id = getRecipeId(category, recipe);
                if (inspectAll || withHiddenItems.contains(recipe)) {
                    Optional<RecipeView> view = viewOf(recipeManager, category, recipe, id, noFocus);
                    if (view.isPresent() && shouldRemoveRecipe(view.get(), rules)) toHide.add(recipe);
                } else if (matchesUserRemovalRule(new RecipeView(typeId, id, List.of(), List.of(), List.of()), rules)) {
                    toHide.add(recipe);
                }
            } catch (Exception ignored) {
            }
        }

        if (toHide.isEmpty()) return;
        recipeManager.hideRecipes(recipeType, toHide);
        RULE_HIDDEN.add(() -> recipeManager.unhideRecipes(recipeType, toHide));
    }

    private static <T> Optional<RecipeView> viewOf(IRecipeManager recipeManager, IRecipeCategory<T> category, T recipe, Identifier id, IFocusGroup noFocus) {
        Identifier typeId = category.getRecipeType().getUid();
        // Anvil recipes can be read directly, and there are a lot of them
        if (recipe instanceof IJeiAnvilRecipe anvil) {
            return Optional.of(new RecipeView(typeId, id, List.of(anvil.getLeftInputs(), anvil.getRightInputs()), List.of(anvil.getOutputs()), List.of()));
        }
        return recipeManager.createRecipeLayoutDrawable(category, recipe, noFocus)
                .map(layout -> RecipeView.of(typeId, id, layout.getRecipeSlotsView()));
    }

    private static boolean isRepairCategory(String path) {
        return path.contains("anvil") || path.contains("grindstone") || path.contains("grinding") || path.contains("repair");
    }

    private static boolean shouldRemoveRecipe(RecipeView recipe, List<RecipeRule> rules) {
        if (matchesUserRemovalRule(recipe, rules)) {
            return true;
        }

        if (isReplacedAnvilRepair(recipe)) {
            return true;
        }

        boolean isRepairRecipe = isRepairRecipe(recipe);
        if (isRepairRecipe && isBlockedRepair(recipe)) {
            return true;
        }

        List<ItemStack> outputs = flatten(recipe.outputs());
        if (outputs.isEmpty()) return false;

        if (hasHiddenOutput(outputs, flatten(recipe.inputs()), flatten(recipe.catalysts()), isRepairRecipe)) {
            return true;
        }

        return hasHiddenInput(recipe.inputs(), isRepairRecipe) || hasHiddenInput(recipe.catalysts(), isRepairRecipe);
    }

    private static boolean matchesUserRemovalRule(RecipeView recipe, List<RecipeRule> rules) {
        if (rules.isEmpty()) return false;

        Identifier recipeId = recipe.id() != null ? recipe.id() : Identifier.withDefaultNamespace("jei_recipe");
        JsonObject recipeJson = null;

        for (RecipeRule rule : rules) {
            if (rule.getAction() == RecipeRule.Action.REMOVE) {
                if (recipeJson == null && rule.needsRecipeJson()) recipeJson = createRecipeJson(recipe);
                if (rule.testJson(recipeId, recipeJson != null ? recipeJson : EMPTY_RECIPE_JSON)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static JsonObject createRecipeJson(RecipeView recipe) {
        JsonObject json = new JsonObject();
        json.addProperty("type", recipe.typeId().toString());

        JsonArray outputArr = new JsonArray();
        for (ItemStack out : flatten(recipe.outputs())) {
            outputArr.add(stackToJson(out));
        }
        if (outputArr.size() == 1) {
            json.add("result", outputArr.get(0));
            json.add("output", outputArr.get(0));
        } else if (outputArr.size() > 1) {
            json.add("results", outputArr);
            json.add("output", outputArr);
        }

        JsonArray inputsArr = new JsonArray();
        for (List<ItemStack> slot : recipe.inputs()) {
            if (slot.isEmpty()) continue;
            JsonArray ingArr = new JsonArray();
            for (ItemStack stack : slot) {
                ingArr.add(stackToJson(stack));
            }
            inputsArr.add(ingArr.size() == 1 ? ingArr.get(0) : ingArr);
        }

        if (inputsArr.size() >= 2) {
            json.add("input", inputsArr.get(0));
            json.add("reagent", inputsArr.get(1));
        } else if (inputsArr.size() == 1) {
            json.add("input", inputsArr.get(0));
        }
        json.add("ingredients", inputsArr);

        return json;
    }

    private static JsonObject stackToJson(ItemStack stack) {
        //? if <=26.2 {
        return BrewingRecipeManager.stackToJson(stack);
        //?} else {
        /*JsonObject obj = new JsonObject();
        String itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        obj.addProperty("item", itemKey);
        obj.addProperty("id", itemKey);
        return obj;
        *///?}
    }

    private static boolean isReplacedAnvilRepair(RecipeView recipe) {
        if (recipe.isOwn() || !recipe.typePath().contains("anvil")) return false;

        List<List<ItemStack>> inputs = recipe.inputs();
        if (inputs.size() < 2) return false;

        for (ItemStack baseStack : inputs.get(0)) {
            Ingredient customMaterial = ReliableRecipesAPI.getCustomRepairMaterial(baseStack.getItem());
            if (customMaterial == null || customMaterial.isEmpty()) continue;

            for (ItemStack additionStack : inputs.get(1)) {
                if (additionStack.getItem() == Items.ENCHANTED_BOOK) continue;
                if (ItemStack.isSameItem(baseStack, additionStack)) continue;
                if (customMaterial.test(additionStack)) continue;
                return true;
            }
        }
        return false;
    }

    private static boolean isRepairRecipe(RecipeView recipe) {
        String path = recipe.typePath();
        if (path.contains("anvil") && isBookEnchanting(recipe)) return false;
        if (path.contains("grindstone") || path.contains("grinding")) {
            // Disenchanting only uses one of the two grindstone slots
            return recipe.inputs().stream().filter(slot -> !slot.isEmpty()).count() >= 2;
        }
        return path.contains("anvil") || path.contains("repair");
    }

    /**
     * Enchanting with a book isn't a repair, so prevent_repair doesn't block it.
     */
    private static boolean isBookEnchanting(RecipeView recipe) {
        List<List<ItemStack>> inputs = recipe.inputs();
        return inputs.size() > 1 && inputs.get(1).stream().anyMatch(stack -> stack.getItem() == Items.ENCHANTED_BOOK);
    }

    private static boolean isBlockedRepair(RecipeView recipe) {
        List<List<ItemStack>> inputs = recipe.inputs();
        if (inputs.isEmpty()) return false;

        boolean isAnvil = recipe.typePath().contains("anvil");
        return hasHiddenInput(isAnvil ? List.of(inputs.get(0)) : inputs, true);
    }

    private static boolean hasHiddenOutput(List<ItemStack> outputs, List<ItemStack> allInputStacks, List<ItemStack> allCatalystStacks, boolean isRepairRecipe) {
        boolean hasAnyRealOutput = false;
        boolean hasValidOutput = false;

        for (ItemStack outputStack : outputs) {
            boolean isReturnedTool = isSameItemInList(outputStack, allInputStacks) || isSameItemInList(outputStack, allCatalystStacks);

            if (!isReturnedTool) {
                hasAnyRealOutput = true;
                if (!ReliableRecipesAPI.isItemHidden(outputStack) && !(isRepairRecipe && ReliableRecipesAPI.isRepairBlocked(outputStack))) {
                    hasValidOutput = true;
                }
            }
        }

        return hasAnyRealOutput && !hasValidOutput;
    }

    private static boolean hasHiddenInput(List<List<ItemStack>> slots, boolean isRepairRecipe) {
        for (List<ItemStack> stacks : slots) {
            if (stacks.isEmpty()) continue;

            boolean allHidden = true;
            boolean allRepairBlocked = true;
            for (ItemStack stack : stacks) {
                if (!ReliableRecipesAPI.isItemHidden(stack)) {
                    allHidden = false;
                }
                if (!isRepairRecipe || !ReliableRecipesAPI.isRepairBlocked(stack)) {
                    allRepairBlocked = false;
                }
            }

            if (allHidden) return true;
            if (isRepairRecipe && allRepairBlocked) return true;
        }
        return false;
    }

    private static boolean isSameItemInList(ItemStack targetStack, List<ItemStack> stackList) {
        for (ItemStack stack : stackList) {
            if (ItemStack.isSameItem(stack, targetStack)) {
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack> flatten(List<List<ItemStack>> slots) {
        List<ItemStack> stacks = new ArrayList<>();
        slots.forEach(stacks::addAll);
        return stacks;
    }

    private static List<ItemStack> ingredientStacks(Ingredient ingredient) {
        //? if <1.21.2 {
        /*return List.of(ingredient.getItems());
         *///?} else {
        return CompatUtil.ingredientItems(ingredient).stream().map(ItemStack::new).toList();
        //?}
    }

    //? if <=26.2 {
    private static List<IJeiBrewingRecipe> getCustomBrewingRecipes(IVanillaRecipeFactory factory) {
        List<IJeiBrewingRecipe> recipes = new ArrayList<>();
        int counter = 0;
        for (BrewingRecipe brewingRecipe : BrewingRecipeManager.getBrewingRecipes()) {
            try {
                List<ItemStack> reagents = ingredientStacks(brewingRecipe.getReagent());
                if (reagents.isEmpty()) continue;

                ItemStack output = brewingRecipe.getOutput();
                String outputPath = BuiltInRegistries.ITEM.getKey(output.getItem()).getPath();
                for (ItemStack input : getBrewingInputs(brewingRecipe.getInputMatcher())) {
                    Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "brewing/" + outputPath + "_" + counter++);
                    recipes.add(factory.createBrewingRecipe(reagents, input, output.copy(), id));
                }
            } catch (Exception ignored) {
            }
        }
        return recipes;
    }

    private static List<ItemStack> getBrewingInputs(BrewingRecipe.BrewingInputMatcher matcher) {
        //? if <1.21.2 {
        /*List<Item> items = CompatUtil.ingredientItems(matcher.ingredient());
         *///?} else {
        List<Item> items = matcher.ingredient().map(CompatUtil::ingredientItems)
                .orElseGet(() -> List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION));
        //?}
        List<Identifier> potions = matcher.potionContents().orElse(List.of());

        List<ItemStack> inputs = new ArrayList<>();
        for (Item item : items) {
            if (potions.isEmpty()) {
                inputs.add(new ItemStack(item));
                continue;
            }
            for (Identifier potionId : potions) {
                withPotion(item, potionId).ifPresent(inputs::add);
            }
        }
        return inputs;
    }

    private static Optional<ItemStack> withPotion(Item item, Identifier potionId) {
        //? if <1.21 {
        /*return BuiltInRegistries.POTION.getOptional(potionId).map(potion -> PotionUtils.setPotion(new ItemStack(item), potion));
         *///?} else if <1.21.2 {
        /*return BuiltInRegistries.POTION.getHolder(ResourceKey.create(Registries.POTION, potionId)).map(potion -> PotionContents.createItemStack(item, potion));
         *///?} else {
        return BuiltInRegistries.POTION.get(ResourceKey.create(Registries.POTION, potionId)).map(potion -> PotionContents.createItemStack(item, potion));
        //?}
    }

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    //? if <1.21.2 {
    /*private static List<IJeiAnvilRecipe> getCustomAnvilRepairRecipes(IVanillaRecipeFactory factory) {
        List<IJeiAnvilRecipe> recipes = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Ingredient customMat = ReliableRecipesAPI.getCustomRepairMaterial(item);
            if (customMat == null || customMat.isEmpty()) continue;

            ItemStack damaged = new ItemStack(item);
            if (!damaged.isDamageableItem()) continue;
            damaged.setDamageValue(damaged.getMaxDamage());

            ItemStack repaired = damaged.copy();
            repaired.setDamageValue(damaged.getMaxDamage() - damaged.getMaxDamage() / 4);

            Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "anvil_repair/" + BuiltInRegistries.ITEM.getKey(item).getPath());
            recipes.add(factory.createAnvilRecipe(damaged, ingredientStacks(customMat), List.of(repaired), id));
        }
        return recipes;
    }
    *///?}

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();
        //? if <=26.2 {
        registration.addRecipes(RecipeTypes.BREWING, getCustomBrewingRecipes(factory));
        //?}
        //? if <1.21.2 {
        /*
        registration.addRecipes(RecipeTypes.ANVIL, getCustomAnvilRepairRecipes(factory));
        *///?}
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        allItems = List.copyOf(jeiRuntime.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK));
        RULE_HIDDEN.clear();
        DELETED.clear();
        runLater(() -> {
            if (runtime == jeiRuntime) hideRemovedRecipes(jeiRuntime);
        });
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        allItems = List.of();
        RULE_HIDDEN.clear();
        DELETED.clear();
    }

    /**
     * The item contents of a JEI recipe, one list per slot
     */
    private record RecipeView(Identifier typeId, Identifier id, List<List<ItemStack>> inputs,
                              List<List<ItemStack>> outputs, List<List<ItemStack>> catalysts) {
        static RecipeView of(Identifier typeId, Identifier id, IRecipeSlotsView slots) {
            //? if <26.1 {
            /*RecipeIngredientRole catalystRole = RecipeIngredientRole.CATALYST;
             *///?} else {
            RecipeIngredientRole catalystRole = RecipeIngredientRole.CRAFTING_STATION;
            //?}
            return new RecipeView(typeId, id, slotStacks(slots, RecipeIngredientRole.INPUT), slotStacks(slots, RecipeIngredientRole.OUTPUT), slotStacks(slots, catalystRole));
        }

        private static List<List<ItemStack>> slotStacks(IRecipeSlotsView slots, RecipeIngredientRole role) {
            List<List<ItemStack>> result = new ArrayList<>();
            for (IRecipeSlotView slot : slots.getSlotViews(role)) {
                result.add(slot.getItemStacks().filter(stack -> !stack.isEmpty()).toList());
            }
            return result;
        }

        boolean isOwn() {
            return id != null && id.getNamespace().equals(Constants.MOD_ID);
        }

        String typePath() {
            return typeId.getPath();
        }
    }
    //?}
}
