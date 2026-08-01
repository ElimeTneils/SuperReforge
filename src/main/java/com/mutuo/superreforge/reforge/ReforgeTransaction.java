package com.mutuo.superreforge.reforge;

import com.mutuo.superreforge.config.GlobalSettings;
import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.item.ReforgeData;
import com.mutuo.superreforge.progress.ProgressStage;
import com.mutuo.superreforge.registry.ModDataComponents;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 服务端重铸事务的无副作用报价与准备阶段。 */
public final class ReforgeTransaction {
    private ReforgeTransaction() {}

    public static ReforgeQuoteResult quote(
            ItemStack target,
            ItemStack catalystStack,
            DefinitionSnapshot snapshot,
            GlobalSettings settings,
            Optional<ProgressStage> highestStage) {
        return quote(target, catalystStack, snapshot, settings, highestStage, true);
    }

    /**
     * paymentRequired=false 用于全局配置允许的创造模式免费重铸；仍要求放入一种有效媒介来选择概率池。
     */
    public static ReforgeQuoteResult quote(
            ItemStack target,
            ItemStack catalystStack,
            DefinitionSnapshot snapshot,
            GlobalSettings settings,
            Optional<ProgressStage> highestStage,
            boolean paymentRequired) {
        if (target.isEmpty()) {
            return ReforgeQuoteResult.failure(ReforgeFailure.TARGET_EMPTY);
        }
        if (target.getCount() != 1) {
            return ReforgeQuoteResult.failure(ReforgeFailure.TARGET_COUNT);
        }
        if (ItemTypeResolver.resolve(target, snapshot).isEmpty()) {
            return ReforgeQuoteResult.failure(ReforgeFailure.NO_ITEM_TYPE);
        }

        var catalystEntry = snapshot.catalysts().entrySet().stream()
                .filter(entry -> SelectorMatcher.matches(entry.getValue().ingredient(), catalystStack))
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .findFirst();
        if (catalystEntry.isEmpty()) {
            return ReforgeQuoteResult.failure(ReforgeFailure.CATALYST_MISSING);
        }

        var entry = catalystEntry.orElseThrow();
        Cost cost = paymentRequired
                ? CostService.quote(entry.getValue(), settings, highestStage)
                : new Cost(0, 0);
        if (paymentRequired && catalystStack.getCount() < cost.materialCount()) {
            return ReforgeQuoteResult.failure(ReforgeFailure.MATERIAL_COUNT);
        }
        ReforgeData existing = target.get(ModDataComponents.REFORGE_DATA.get());
        Optional<ResourceLocation> current = existing == null
                ? Optional.empty()
                : Optional.of(existing.modifierId());
        CandidatePool candidates = CandidatePool.build(target, entry.getValue(), snapshot, current);
        if (candidates.isEmpty()) {
            return ReforgeQuoteResult.failure(ReforgeFailure.NO_CANDIDATES);
        }
        return ReforgeQuoteResult.success(
                new ReforgeQuote(entry.getKey(), entry.getValue(), cost, candidates));
    }

    /**
     * 根据已经验证的 quote 构造提交内容，不修改传入栈。
     *
     * <p>方块实体必须在提交前再次确认输入仍与报价快照一致，并检查玩家经验。
     */
    public static PreparedReforge prepare(
            ItemStack target, ItemStack catalystStack, ReforgeQuote quote, long seed) {
        return prepare(target, catalystStack, quote, seed, true);
    }

    /**
     * 构造结果并按 consumeMaterial 决定是否扣媒介；创造免费配置通过该参数复用同一事务路径。
     */
    public static PreparedReforge prepare(
            ItemStack target,
            ItemStack catalystStack,
            ReforgeQuote quote,
            long seed,
            boolean consumeMaterial) {
        RollResult rolled = RollEngine.roll(quote.candidates(), seed);
        ItemStack result = target.copyWithCount(1);
        result.set(
                ModDataComponents.REFORGE_DATA.get(),
                new ReforgeData(rolled.modifierId(), rolled.seed(), ReforgeData.CURRENT_SCHEMA));
        ItemStack remainder = catalystStack.copy();
        if (consumeMaterial) {
            remainder.shrink(quote.cost().materialCount());
        }
        return new PreparedReforge(result, remainder);
    }
}
