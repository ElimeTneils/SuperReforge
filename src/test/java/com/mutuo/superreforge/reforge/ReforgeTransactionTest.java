package com.mutuo.superreforge.reforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mutuo.superreforge.config.GlobalSettings;
import com.mutuo.superreforge.definition.CatalystDefinition;
import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.definition.ItemSelector;
import com.mutuo.superreforge.definition.ItemTypeDefinition;
import com.mutuo.superreforge.definition.LevelDefinition;
import com.mutuo.superreforge.definition.ModifierDefinition;
import com.mutuo.superreforge.definition.WeightedLevel;
import com.mutuo.superreforge.item.ReforgeData;
import com.mutuo.superreforge.registry.ModDataComponents;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

/** 验证输入检查与结果生成分离，失败不会产生半扣除状态。 */
final class ReforgeTransactionTest {
    private static final ResourceLocation TYPE = id("sword");
    private static final ResourceLocation LEVEL = id("tier_1");
    private static final ResourceLocation MODIFIER = id("steady");
    private static final ResourceLocation CATALYST = id("common");

    @Test
    void rejectsTargetStacksWhoseCountIsNotExactlyOne() {
        ItemStack target = new ItemStack(Items.DIAMOND_SWORD, 2);

        ReforgeQuoteResult result = ReforgeTransaction.quote(
                target, new ItemStack(Items.DIAMOND), snapshot(), GlobalSettings.DEFAULTS, Optional.empty());

        assertEquals(ReforgeFailure.TARGET_COUNT, result.failure().orElseThrow());
        assertEquals(2, target.getCount());
    }

    @Test
    void rejectsInsufficientCatalystWithoutChangingEitherInput() {
        ItemStack target = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack catalyst = new ItemStack(Items.DIAMOND, 1);

        ReforgeQuoteResult result = ReforgeTransaction.quote(
                target, catalyst, snapshot(), GlobalSettings.DEFAULTS, Optional.empty());

        assertEquals(ReforgeFailure.MATERIAL_COUNT, result.failure().orElseThrow());
        assertEquals(1, target.getCount());
        assertEquals(1, catalyst.getCount());
    }

    @Test
    void successfulPreparationReturnsResultAndRemainderWithoutMutatingInputs() {
        ItemStack target = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack catalyst = new ItemStack(Items.DIAMOND, 5);
        ReforgeQuote quote = ReforgeTransaction.quote(
                        target, catalyst, snapshot(), GlobalSettings.DEFAULTS, Optional.empty())
                .quote()
                .orElseThrow();

        PreparedReforge prepared = ReforgeTransaction.prepare(target, catalyst, quote, 1234L);

        assertEquals(1, target.getCount());
        assertEquals(5, catalyst.getCount());
        assertEquals(3, prepared.catalystRemainder().getCount());
        ReforgeData data = prepared.result().get(ModDataComponents.REFORGE_DATA.get());
        assertEquals(MODIFIER, data.modifierId());
        assertEquals(1234L, data.seed());
        assertFalse(prepared.result().isEmpty());
        assertTrue(quote.cost().experience() > 0);
    }

    private static DefinitionSnapshot snapshot() {
        ItemSelector sword = ItemSelector.item(ResourceLocation.withDefaultNamespace("diamond_sword"));
        CatalystDefinition catalyst = new CatalystDefinition(
                ItemSelector.item(ResourceLocation.withDefaultNamespace("diamond")),
                2,
                5,
                false,
                List.of(),
                List.of(),
                List.of(new WeightedLevel(LEVEL, 1)));
        return new DefinitionSnapshot(
                Map.of(LEVEL, new LevelDefinition(1, Component.literal("一级"))),
                Map.of(TYPE, new ItemTypeDefinition(List.of(sword), List.of())),
                Map.of(MODIFIER, new ModifierDefinition(
                        LEVEL, List.of(TYPE), Component.literal("稳定"), 1, List.of())),
                Map.of(CATALYST, catalyst));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
