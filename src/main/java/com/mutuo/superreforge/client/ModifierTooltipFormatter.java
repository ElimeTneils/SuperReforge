package com.mutuo.superreforge.client;

import com.mutuo.superreforge.definition.AttributeEffectDefinition;
import com.mutuo.superreforge.definition.AttributeOperation;
import com.mutuo.superreforge.definition.ModifierDisplayDefinition;
import com.mutuo.superreforge.definition.SlotTarget;
import com.mutuo.superreforge.definition.ValueDefinition;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/** 将客户端同步的词条定义格式化为候选行悬停说明，不提前抽取未来随机值。 */
public final class ModifierTooltipFormatter {
    private ModifierTooltipFormatter() {}

    public static List<Component> format(
            ResourceLocation modifierId, @Nullable ModifierDisplayDefinition definition) {
        List<Component> lines = new ArrayList<>();
        lines.add((definition == null ? Component.literal(modifierId.toString()) : definition.name().copy())
                .withStyle(ChatFormatting.GOLD));
        lines.add(labeled(
                        "gui.superreforge.tooltip.modifier_id",
                        Component.literal(modifierId.toString()))
                .withStyle(ChatFormatting.DARK_GRAY));
        if (definition == null) {
            lines.add(Component.translatable("gui.superreforge.tooltip.definition_unavailable")
                    .withStyle(ChatFormatting.RED));
            return List.copyOf(lines);
        }
        for (AttributeEffectDefinition effect : definition.attributes()) {
            appendEffect(lines, effect);
        }
        return List.copyOf(lines);
    }

    private static void appendEffect(List<Component> lines, AttributeEffectDefinition effect) {
        lines.add(labeled(
                        "gui.superreforge.tooltip.attribute",
                        Component.literal(effect.attribute().toString()))
                .withStyle(ChatFormatting.GRAY));
        lines.add(labeled(
                        "gui.superreforge.tooltip.amount",
                        Component.literal(formatAmount(effect.amount(), effect.operation())))
                .withStyle(ChatFormatting.AQUA));
        lines.add(labeled(
                        "gui.superreforge.tooltip.operation",
                        Component.translatable("gui.superreforge.tooltip.operation."
                                + effect.operation().serializedName()))
                .withStyle(ChatFormatting.GRAY));
        lines.add(labeled("gui.superreforge.tooltip.slots", formatSlots(effect.slots()))
                .withStyle(ChatFormatting.GRAY));
    }

    /** 标签和值分开拼接，未知语言包也不会吞掉调试所需的 ID 和数值。 */
    private static MutableComponent labeled(String translationKey, Component value) {
        return Component.translatable(translationKey).append(": ").append(value);
    }

    private static String formatAmount(ValueDefinition amount, AttributeOperation operation) {
        boolean percentage = operation != AttributeOperation.ADD_VALUE;
        if (amount instanceof ValueDefinition.Fixed fixed) {
            return number(fixed.value(), percentage);
        }
        ValueDefinition.Range range = (ValueDefinition.Range) amount;
        return number(range.min(), percentage) + " – " + number(range.max(), percentage);
    }

    private static String number(double value, boolean percentage) {
        double displayed = percentage ? value * 100.0 : value;
        String number = BigDecimal.valueOf(displayed).stripTrailingZeros().toPlainString();
        return percentage ? number + "%" : number;
    }

    private static Component formatSlots(List<SlotTarget> slots) {
        MutableComponent result = Component.empty();
        for (int index = 0; index < slots.size(); index++) {
            if (index > 0) {
                result.append(Component.literal(", "));
            }
            result.append(Component.translatable(
                    "gui.superreforge.tooltip.slot." + slotKey(slots.get(index))));
        }
        return result;
    }

    private static String slotKey(SlotTarget target) {
        return target == SlotTarget.CURIOS_ANY
                ? "curios_any"
                : target.serializedName().replace(':', '_');
    }
}
