package com.mutuo.superreforge.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mutuo.superreforge.definition.AttributeEffectDefinition;
import com.mutuo.superreforge.definition.AttributeOperation;
import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.definition.ItemSelector;
import com.mutuo.superreforge.definition.ItemTypeDefinition;
import com.mutuo.superreforge.definition.LevelDefinition;
import com.mutuo.superreforge.definition.ModifierDefinition;
import com.mutuo.superreforge.definition.ModifierDisplayDefinition;
import com.mutuo.superreforge.definition.SlotTarget;
import com.mutuo.superreforge.definition.ValueDefinition;
import com.mutuo.superreforge.registry.ModDataComponents;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

/** 验证物品仅凭保存 ID/seed 就能从最新快照解析完整效果。 */
final class ModifierResolverTest {
    @Test
    void resolvesLatestRangeFromStoredIdAndSeed() {
        ResourceLocation typeId = id("sword");
        ResourceLocation levelId = id("tier");
        ResourceLocation modifierId = id("legendary");
        ItemSelector diamondSword = new ItemSelector(
                Optional.of(ResourceLocation.withDefaultNamespace("diamond_sword")),
                List.of(), Optional.empty(), Optional.empty(), Optional.empty());
        ModifierDefinition modifier = new ModifierDefinition(
                levelId,
                List.of(typeId),
                Component.literal("传说"),
                1,
                List.of(new AttributeEffectDefinition(
                        "damage",
                        ResourceLocation.withDefaultNamespace("generic.attack_damage"),
                        new ValueDefinition.Range(0.04, 0.08),
                        AttributeOperation.ADD_MULTIPLIED_BASE,
                        List.of(SlotTarget.MAINHAND),
                        true)));
        DefinitionSnapshot snapshot = new DefinitionSnapshot(
                Map.of(levelId, new LevelDefinition(4, Component.literal("四级"))),
                Map.of(typeId, new ItemTypeDefinition(List.of(diamondSword), List.of())),
                Map.of(modifierId, modifier),
                Map.of());
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.set(ModDataComponents.REFORGE_DATA.get(), new ReforgeData(modifierId, 77L, 1));

        ResolvedModifier resolved = ModifierResolver.resolve(stack, snapshot).orElseThrow();

        assertEquals(modifierId, resolved.id());
        assertEquals(1, resolved.effects().size());
        assertTrue(resolved.effects().getFirst().amount() >= 0.04);
        assertTrue(resolved.effects().getFirst().amount() <= 0.08);
    }

    @Test
    void modifierBecomesInactiveWhenItemNoLongerMatchesItsType() {
        ItemStack stick = new ItemStack(Items.STICK);
        stick.set(ModDataComponents.REFORGE_DATA.get(), new ReforgeData(id("missing"), 1L, 1));

        assertTrue(ModifierResolver.resolve(stick, DefinitionSnapshot.EMPTY).isEmpty());
    }

    @Test
    void clientMirrorTrustsServerActiveFlagWithoutRunningServerOnlyKubePredicate() {
        ResourceLocation modifierId = id("scripted");
        ModifierDefinition scripted = new ModifierDefinition(
                id("tier"), List.of(id("kube_type")), Component.literal("脚本词条"), 1, List.of());
        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(ModDataComponents.REFORGE_DATA.get(), new ReforgeData(modifierId, 5L, 2, true));

        assertEquals(
                "脚本词条",
                ModifierResolver.resolveMirrored(stack, Map.of(modifierId, ModifierDisplayDefinition.from(scripted)))
                        .orElseThrow()
                        .name()
                        .getString());

        stack.set(ModDataComponents.REFORGE_DATA.get(), new ReforgeData(modifierId, 5L, 2, false));
        assertTrue(ModifierResolver.resolveMirrored(
                stack, Map.of(modifierId, ModifierDisplayDefinition.from(scripted))).isEmpty());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
