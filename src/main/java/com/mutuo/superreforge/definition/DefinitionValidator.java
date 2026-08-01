package com.mutuo.superreforge.definition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 对跨文件引用和 Codec 无法表达的数值约束执行集中校验。 */
public final class DefinitionValidator {
    private static final java.util.regex.Pattern EFFECT_ID = java.util.regex.Pattern.compile("[a-z0-9/._-]+");
    private static final int MAX_SYNCED_MODIFIERS = 8192;
    private static final int MAX_ATTRIBUTES_PER_MODIFIER = 64;
    private DefinitionValidator() {}

    public static ValidationReport validate(DefinitionSnapshot snapshot) {
        List<String> errors = new ArrayList<>();

        if (snapshot.modifiers().size() > MAX_SYNCED_MODIFIERS) {
            errors.add("词条总数不能超过客户端同步上限 " + MAX_SYNCED_MODIFIERS);
        }

        snapshot.itemTypes().forEach((typeId, definition) -> {
            definition.include().stream()
                    .filter(ItemSelector::isEmpty)
                    .forEach(selector -> errors.add("物品类型 " + typeId + " 包含空 include selector"));
            definition.exclude().stream()
                    .filter(ItemSelector::isEmpty)
                    .forEach(selector -> errors.add("物品类型 " + typeId + " 包含空 exclude selector"));
            definition.include().forEach(selector -> validateSelector("物品类型 " + typeId, selector, errors));
            definition.exclude().forEach(selector -> validateSelector("物品类型 " + typeId, selector, errors));
        });

        snapshot.modifiers().forEach((modifierId, modifier) -> {
            if (!Double.isFinite(modifier.weight()) || modifier.weight() < 0.0) {
                errors.add("词条 " + modifierId + " 的 weight 必须是非负有限数");
            }
            if (!snapshot.levels().containsKey(modifier.level())) {
                errors.add("词条 " + modifierId + " 引用了未知等级 " + modifier.level());
            }
            if (modifier.itemTypes().isEmpty()) {
                errors.add("词条 " + modifierId + " 的 item_types 不能为空");
            }
            if (modifier.attributes().size() > MAX_ATTRIBUTES_PER_MODIFIER) {
                errors.add("词条 " + modifierId + " 的 attributes 不能超过 " + MAX_ATTRIBUTES_PER_MODIFIER + " 条");
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
                } else if (!EFFECT_ID.matcher(effect.id()).matches()) {
                    errors.add("词条 " + modifierId + " 的 effect id 只能使用小写字母、数字、/、.、_、-");
                } else if (!effectIds.add(effect.id())) {
                    errors.add("词条 " + modifierId + " 重复 Attribute effect id: " + effect.id());
                }
                validateAmount(modifierId.toString(), effect, errors);
                if (effect.slots().isEmpty()) {
                    errors.add("词条 " + modifierId + " 的 effect " + effect.id() + " 没有 slots");
                } else if (new HashSet<>(effect.slots()).size() != effect.slots().size()) {
                    errors.add("词条 " + modifierId + " 的 effect " + effect.id() + " 包含重复 slots");
                }
            }
        });

        snapshot.catalysts().forEach((catalystId, catalyst) -> {
            validateSelector("媒介 " + catalystId + " ingredient", catalyst.ingredient(), errors);
            if (catalyst.ingredient().isEmpty()) {
                errors.add("媒介 " + catalystId + " 的 ingredient 为空");
            }
            if (catalyst.count() < 1) {
                errors.add("媒介 " + catalystId + " 的 count 必须至少为 1");
            }
            if (catalyst.experience() < 0) {
                errors.add("媒介 " + catalystId + " 的 experience 不能为负数");
            }
            if (catalyst.levels().isEmpty()) {
                errors.add("媒介 " + catalystId + " 的 levels 不能为空");
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

    /** 校验 Codec 可解析、但语义上无效的可选匹配器字段。 */
    private static void validateSelector(String owner, ItemSelector selector, List<String> errors) {
        selector.curios().filter(value -> !"any".equals(value)).ifPresent(value ->
                errors.add(owner + " 的 curios 目前只支持 \"any\"，实际为 " + value));
        selector.kubejsPredicate().filter(String::isBlank).ifPresent(value ->
                errors.add(owner + " 的 kubejs_predicate 不能为空白字符串"));
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
