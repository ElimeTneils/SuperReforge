package com.mutuo.superreforge.item;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.definition.AttributeOperation;
import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.definition.SlotTarget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/** 把已解析 effect 注入原版 Attribute 查询；本模组自身不注册任何 Attribute。 */
public final class VanillaAttributeApplicator {
    private VanillaAttributeApplicator() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VanillaAttributeApplicator::onItemAttributes);
    }

    private static void onItemAttributes(ItemAttributeModifierEvent event) {
        ModifierResolver.resolve(event.getItemStack(), DefinitionManager.snapshot()).ifPresent(modifier -> {
            for (ResolvedEffect effect : modifier.effects()) {
                var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(effect.attribute());
                if (attribute.isEmpty()) {
                    SuperReforge.LOGGER.warn(
                            "Modifier {} references missing Attribute {}; effect {} is inactive",
                            modifier.id(), effect.attribute(), effect.id());
                    continue;
                }
                ResourceLocation effectModifierId = stableModifierId(modifier.id(), effect.id());
                AttributeModifier vanilla =
                        new AttributeModifier(effectModifierId, effect.amount(), operation(effect.operation()));
                for (SlotTarget target : effect.slots()) {
                    if (target != SlotTarget.CURIOS_ANY) {
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
    public static ResourceLocation stableModifierId(ResourceLocation modifierId, String effectId) {
        String safeEffect = effectId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        return ResourceLocation.fromNamespaceAndPath(
                SuperReforge.MOD_ID,
                "effect/" + modifierId.getNamespace() + "/" + modifierId.getPath() + "/" + safeEffect);
    }
}
