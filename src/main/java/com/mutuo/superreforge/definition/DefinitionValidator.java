package com.mutuo.superreforge.definition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 对跨文件引用和 Codec 无法表达的数值约束执行集中校验。 */
public final class DefinitionValidator {
    private DefinitionValidator() {}

    public static ValidationReport validate(DefinitionSnapshot snapshot) {
        List<String> errors = new ArrayList<>();

        snapshot.itemTypes().forEach((typeId, definition) -> {
            definition.include().stream()
                    .filter(ItemSelector::isEmpty)
                    .forEach(selector -> errors.add("物品类型 " + typeId + " 包含空 include selector"));
            definition.exclude().stream()
                    .filter(ItemSelector::isEmpty)
                    .forEach(selector -> errors.add("物品类型 " + typeId + " 包含空 exclude selector"));
        });

        snapshot.modifiers().forEach((modifierId, modifier) -> {
            if (!Double.isFinite(modifier.weight()) || modifier.weight() < 0.0) {
                errors.add("词条 " + modifierId + " 的 weight 必须是非负有限数");
            }
            if (!snapshot.levels().containsKey(modifier.level())) {
                errors.add("词条 " + modifierId + " 引用了未知等级 " + modifier.level());
            }
            for (var typeId : modifier.itemTypes()) {
                if (!snapshot.itemTypes().containsKey(typeId)) {
                    errors.add("词条 " + modifierId + " 引用了未知物品类型 " + typeId);
                }
            }

            Set<String> effectIds = new HashSet<>();
            for (AttributeEffectDefinition effect : modifier.attributes()) {
                if (effect.id().isBlank()) {
                    errors.add("词条 " + modifierId + " 包含空 Attribute effect id");
                } else if (!effectIds.add(effect.id())) {
                    errors.add("词条 " + modifierId + " 重复 Attribute effect id: " + effect.id());
                }
                validateAmount(modifierId.toString(), effect, errors);
                if (effect.slots().isEmpty()) {
                    errors.add("词条 " + modifierId + " 的 effect " + effect.id() + " 没有 slots");
                }
            }
        });

        snapshot.catalysts().forEach((catalystId, catalyst) -> {
            if (catalyst.ingredient().isEmpty()) {
                errors.add("媒介 " + catalystId + " 的 ingredient 为空");
            }
            if (catalyst.count() < 1) {
                errors.add("媒介 " + catalystId + " 的 count 必须至少为 1");
            }
            if (catalyst.experience() < 0) {
                errors.add("媒介 " + catalystId + " 的 experience 不能为负数");
            }
            for (WeightedLevel weighted : catalyst.levels()) {
                if (!Double.isFinite(weighted.weight()) || weighted.weight() < 0.0) {
                    errors.add("媒介 " + catalystId + " 的等级 weight 必须是非负有限数");
                }
                if (!snapshot.levels().containsKey(weighted.level())) {
                    errors.add("媒介 " + catalystId + " 引用了未知等级 " + weighted.level());
                }
            }
            validateKnownTypes(catalystId.toString(), catalyst.allowedItemTypes(), snapshot, errors);
            validateKnownTypes(catalystId.toString(), catalyst.deniedItemTypes(), snapshot, errors);
        });

        return new ValidationReport(errors);
    }

    private static void validateAmount(
            String modifierId, AttributeEffectDefinition effect, List<String> errors) {
        if (effect.amount() instanceof ValueDefinition.Fixed fixed) {
            if (!Double.isFinite(fixed.value())) {
                errors.add("词条 " + modifierId + " 的 effect " + effect.id() + " amount 必须有限");
            }
        } else if (effect.amount() instanceof ValueDefinition.Range range) {
            if (!Double.isFinite(range.min()) || !Double.isFinite(range.max()) || range.min() > range.max()) {
                errors.add("词条 " + modifierId + " 的 effect " + effect.id()
                        + " 要求有限数且 min <= max");
            }
        }
    }

    private static void validateKnownTypes(
            String owner,
            List<net.minecraft.resources.ResourceLocation> typeIds,
            DefinitionSnapshot snapshot,
            List<String> errors) {
        for (var typeId : typeIds) {
            if (!snapshot.itemTypes().containsKey(typeId)) {
                errors.add("媒介 " + owner + " 引用了未知物品类型 " + typeId);
            }
        }
    }
}
