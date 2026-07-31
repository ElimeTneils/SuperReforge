package com.mutuo.superreforge.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mutuo.superreforge.config.ExperienceMode;
import com.mutuo.superreforge.config.GlobalSettings;
import com.mutuo.superreforge.config.MissingDefinitionPolicy;
import com.mutuo.superreforge.definition.CatalystDefinition;
import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.definition.ItemSelector;
import com.mutuo.superreforge.definition.ItemTypeDefinition;
import com.mutuo.superreforge.definition.LevelDefinition;
import com.mutuo.superreforge.definition.ModifierDefinition;
import com.mutuo.superreforge.definition.WeightedLevel;
import com.mutuo.superreforge.registry.ModDataComponents;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

/** 验证转换后失配清理以及可选的首次自动词条都复用同一动态定义。 */
final class ModifierLifecycleTest {
    private static final ResourceLocation TYPE = id("sword");
    private static final ResourceLocation LEVEL = id("tier");
    private static final ResourceLocation MODIFIER = id("steady");
    private static final ResourceLocation CATALYST = id("auto");

    @Test
    void removePolicyDeletesAnInvalidSavedModifier() {
        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(ModDataComponents.REFORGE_DATA.get(), new ReforgeData(id("gone"), 4L, 1));

        boolean changed = ModifierLifecycle.reconcile(stack, DefinitionSnapshot.EMPTY, settings(false, MissingDefinitionPolicy.REMOVE), 8L);

        assertTrue(changed);
        assertNull(stack.get(ModDataComponents.REFORGE_DATA.get()));
    }

    @Test
    void keepInactivePolicyPreservesAnInvalidSavedModifier() {
        ItemStack stack = new ItemStack(Items.STICK);
        ReforgeData original = new ReforgeData(id("gone"), 4L, 1);
        stack.set(ModDataComponents.REFORGE_DATA.get(), original);

        boolean changed = ModifierLifecycle.reconcile(
                stack, DefinitionSnapshot.EMPTY, settings(false, MissingDefinitionPolicy.KEEP_INACTIVE), 8L);

        assertFalse(changed);
        assertEquals(original, stack.get(ModDataComponents.REFORGE_DATA.get()));
    }

    @Test
    void automaticModeUsesTheConfiguredCatalystPoolWithoutConsumingAnything() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);

        boolean changed = ModifierLifecycle.reconcile(
                stack, snapshot(), settings(true, MissingDefinitionPolicy.REMOVE), 123L);

        assertTrue(changed);
        ReforgeData data = stack.get(ModDataComponents.REFORGE_DATA.get());
        assertEquals(MODIFIER, data.modifierId());
        assertEquals(123L, data.seed());
    }

    private static GlobalSettings settings(boolean automatic, MissingDefinitionPolicy policy) {
        return new GlobalSettings(true, ExperienceMode.LEVELS, automatic, CATALYST, true, 20, false, policy);
    }

    private static DefinitionSnapshot snapshot() {
        return new DefinitionSnapshot(
                Map.of(LEVEL, new LevelDefinition(1, Component.literal("一级"))),
                Map.of(TYPE, new ItemTypeDefinition(
                        List.of(ItemSelector.item(ResourceLocation.withDefaultNamespace("diamond_sword"))), List.of())),
                Map.of(MODIFIER, new ModifierDefinition(
                        LEVEL, List.of(TYPE), Component.literal("稳定"), 1, List.of())),
                Map.of(CATALYST, new CatalystDefinition(
                        ItemSelector.item(ResourceLocation.withDefaultNamespace("diamond")),
                        1, 0, true, List.of(), List.of(), List.of(new WeightedLevel(LEVEL, 1)))));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
