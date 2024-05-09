package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.item.Item;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Arrays;
import java.util.List;

public class AutoCraft extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items you want to get crafted.")
        .defaultValue(Arrays.asList())
        .build()
    );

    private final Setting<Boolean> antiDesync = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-desync")
            .description("Try to prevent inventory desync.")
            .defaultValue(false)
            .build()
    );
    
    private final Setting<Boolean> craftAll = sgGeneral.add(new BoolSetting.Builder()
            .name("craft-all")
            .description("Crafts maximum possible amount amount per craft (shift-clicking)")
            .defaultValue(false)
            .build()
    );
    
    private final Setting<Boolean> drop = sgGeneral.add(new BoolSetting.Builder()
            .name("drop")
            .description("Automatically drops crafted items (useful for when not enough inventory space)")
            .defaultValue(false)
            .build()
    );

    public AutoCraft() {
        super(MeteorRejectsAddon.CATEGORY, "auto-craft", "Automatically crafts items.");
    }
    private final long craftingDelay = 1000; // Adjust this value to change the crafting delay in milliseconds
    private long lastCraftingTime = 0;    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.interactionManager == null) return;
        if (items.get().isEmpty()) return;

        if (!(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) return;

        if (antiDesync.get())
            mc.player.getInventory().updateItems();

        // Danke schön GhostTypes
        // https://github.com/GhostTypes/orion/blob/main/src/main/java/me/ghosttypes/orion/modules/main/AutoBedCraft.java
        CraftingScreenHandler currentScreenHandler = (CraftingScreenHandler) mc.player.currentScreenHandler;
        List<Item> itemList = items.get();
        List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getOrderedResults();

        // Check if enough time has passed since the last crafting action
        if (System.currentTimeMillis() - lastCraftingTime < craftingDelay) return;

        // Iterate over recipes to perform crafting
        for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
            for (RecipeEntry<?> recipe : recipeResultCollection.getRecipes(true)) {
                if (!itemList.contains(recipe.value().getResult(mc.world.getRegistryManager()).getItem())) continue;
                mc.interactionManager.clickRecipe(currentScreenHandler.syncId, recipe, craftAll.get());
                mc.interactionManager.clickSlot(currentScreenHandler.syncId, 0, 1,
                        drop.get() ? SlotActionType.THROW : SlotActionType.QUICK_MOVE, mc.player);
                
                // Update last crafting time
                lastCraftingTime = System.currentTimeMillis();
                return; // Exit the method after performing one crafting action
            }
        }
    }
}
