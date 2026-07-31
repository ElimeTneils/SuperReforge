package com.mutuo.superreforge.reforge;

import com.mutuo.superreforge.definition.CatalystDefinition;
import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.definition.ModifierDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 已移除空等级、0 权重和非法类型后的最终服务端候选池。 */
public record CandidatePool(List<CandidateLevel> levels) {
    public CandidatePool {
        levels = List.copyOf(levels);
    }

    public boolean isEmpty() {
        return levels.isEmpty();
    }

    public static CandidatePool build(
            ItemStack target,
            CatalystDefinition catalyst,
            DefinitionSnapshot snapshot,
            Optional<ResourceLocation> currentModifier) {
        Set<ResourceLocation> types = ItemTypeResolver.resolve(target, snapshot);
        if (types.isEmpty() || !typeRestrictionsAllow(types, catalyst)) {
            return new CandidatePool(List.of());
        }

        Map<ResourceLocation, ModifierDefinition> eligible = new LinkedHashMap<>();
        snapshot.modifiers().forEach((id, definition) -> {
            if (definition.weight() > 0.0
                    && definition.itemTypes().stream().anyMatch(types::contains)) {
                eligible.put(id, definition);
            }
        });
        if (!catalyst.allowSameModifier() && eligible.size() > 1) {
            currentModifier.ifPresent(eligible::remove);
        }

        List<CandidateLevel> levels = new ArrayList<>();
        catalyst.levels().forEach(weightedLevel -> {
            List<WeightedValue<ResourceLocation>> modifiers = eligible.entrySet().stream()
                    .filter(entry -> entry.getValue().level().equals(weightedLevel.level()))
                    .map(entry -> new WeightedValue<>(entry.getKey(), entry.getValue().weight()))
                    .toList();
            if (weightedLevel.weight() > 0.0 && !WeightNormalizer.normalize(modifiers).isEmpty()) {
                levels.add(new CandidateLevel(weightedLevel.level(), weightedLevel.weight(), modifiers));
            }
        });
        return new CandidatePool(levels);
    }

    private static boolean typeRestrictionsAllow(Set<ResourceLocation> types, CatalystDefinition catalyst) {
        if (catalyst.deniedItemTypes().stream().anyMatch(types::contains)) {
            return false;
        }
        return catalyst.allowedItemTypes().isEmpty()
                || catalyst.allowedItemTypes().stream().anyMatch(types::contains);
    }
}
