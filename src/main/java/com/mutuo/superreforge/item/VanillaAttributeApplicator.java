package com.mutuo.superreforge.item;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.definition.AttributeOperation;
import com.mutuo.superreforge.definition.SlotTarget;
import com.mutuo.superreforge.config.SuperReforgeConfig;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.client.event.GatherSkippedAttributeTooltipsEvent;

/** 把已解析 effect 注入原版 Attribute 查询；本模组自身不注册任何 Attribute。 */
public final class VanillaAttributeApplicator {
    private VanillaAttributeApplicator() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VanillaAttributeApplicator::onItemAttributes);
        NeoForge.EVENT_BUS.addListener(VanillaAttributeApplicator::onSkippedAttributeTooltips);
    }

    /** NeoForge 和 Curios 都读取此跳过事件，因此一套逻辑同时控制两类 Attribute 行。 */
    private static void onSkippedAttributeTooltips(GatherSkippedAttributeTooltipsEvent event) {
        ModifierResolver.resolveCurrent(event.getStack()).ifPresent(modifier ->
                hiddenEffectIds(modifier, SuperReforgeConfig.snapshot().showAttributeLines())
                        .forEach(event::skipId));
    }

    /** 纯策略函数：全局关闭时隐藏全部效果，否则只隐藏 show_in_tooltip=false 的效果。 */
    public static List<ResourceLocation> hiddenEffectIds(ResolvedModifier modifier, boolean globalDisplayEnabled) {
        return modifier.effects().stream()
                .filter(effect -> !globalDisplayEnabled || !effect.showInTooltip())
                .flatMap(effect -> effect.slots().stream()
                        .filter(slot -> slot != SlotTarget.CURIOS_ANY)
                        .map(slot -> stableModifierId(modifier.id(), effect.id(), slot)))
                .toList();
    }

    private static void onItemAttributes(ItemAttributeModifierEvent event) {
        ModifierResolver.resolveCurrent(event.getItemStack()).ifPresent(modifier -> {
            for (ResolvedEffect effect : modifier.effects()) {
                var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(effect.attribute());
                if (attribute.isEmpty()) {
                    SuperReforge.LOGGER.warn(
                            "Modifier {} references missing Attribute {}; effect {} is inactive",
                            modifier.id(), effect.attribute(), effect.id());
                    continue;
                }
                for (SlotTarget target : effect.slots()) {
                    if (target != SlotTarget.CURIOS_ANY) {
                        // 栏位属于 AttributeModifier 身份的一部分；否则主手与副手会彼此覆盖。
                        ResourceLocation effectModifierId = stableModifierId(modifier.id(), effect.id(), target);
                        AttributeModifier vanilla = new AttributeModifier(
                                effectModifierId, effect.amount(), operation(effect.operation()));
                        event.addModifier(attribute.orElseThrow(), vanilla, slot(target));
                    }
                }
            }
        });
    }

    /** 暴露纯映射供测试验证，避免 operation 接反造成数值灾难。 */
    public static AttributeModifier.Operation operation(AttributeOperation operation) {
        return switch (operation) {
            case ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }

    /** Curios 槽位由可选兼容层处理，不允许走这个原版槽位映射。 */
    public static EquipmentSlotGroup slot(SlotTarget slot) {
        return switch (slot) {
            case MAINHAND -> EquipmentSlotGroup.MAINHAND;
            case OFFHAND -> EquipmentSlotGroup.OFFHAND;
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
            case CURIOS_ANY -> throw new IllegalArgumentException("curios:any 必须由 Curios 兼容层处理");
        };
    }

    /**
     * 为每个“词条 ID + 效果 ID”生成稳定资源位置。
     *
     * <p>公开此纯函数是为了让 Curios 可选兼容层复用完全相同的去重规则；它不会注册 Attribute。
     */
    public static ResourceLocation stableModifierId(
            ResourceLocation modifierId, String effectId, SlotTarget slot) {
        return scopedModifierId(modifierId, effectId, slot.serializedName());
    }

    /** Curios 提供的 ID 已包含栏位类型与序号，用它可让两个戒指栏中的相同词条正确叠加。 */
    public static ResourceLocation curiosModifierId(
            ResourceLocation modifierId, String effectId, String identifier, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Curios slot index 不能为负数");
        }
        return scopedModifierId(
                modifierId,
                effectId,
                "curio/" + identifier + "/" + index);
    }

    private static ResourceLocation scopedModifierId(
            ResourceLocation modifierId, String effectId, String slotKey) {
        return ResourceLocation.fromNamespaceAndPath(
                SuperReforge.MOD_ID,
                "effect/" + modifierId.getNamespace() + "/" + modifierId.getPath() + "/" + effectId + "/" + slotKey);
    }
}
