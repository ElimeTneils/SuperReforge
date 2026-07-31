package com.mutuo.superreforge.compat.curios;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.definition.SlotTarget;
import com.mutuo.superreforge.item.ModifierResolver;
import com.mutuo.superreforge.item.ModifierLifecycle;
import com.mutuo.superreforge.item.VanillaAttributeApplicator;
import com.mutuo.superreforge.config.SuperReforgeConfig;
import com.mutuo.superreforge.reforge.SelectorHooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * Curios 9.x 的隔离兼容层。
 *
 * <p>该类故意是唯一导入 Curios 类型的生产类，也不使用自动扫描注解。只有主入口确认 Curios 已加载后，
 * 才会通过反射调用 {@link #initialize()}；因此删除 Curios 不会影响武器重铸或 datapack 加载。
 */
public final class CuriosCompat {
    private static boolean initialized;

    private CuriosCompat() {}

    /** 注册任意饰品匹配器和 Curios 自己的动态 Attribute 事件。 */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        SelectorHooks.setCuriosMatcher(stack -> CuriosApi.getCurio(stack).isPresent());
        NeoForge.EVENT_BUS.addListener(CuriosCompat::onCurioAttributes);
    }

    /**
     * Curios 会在服务端穿脱饰品和客户端生成属性提示时触发同一事件。
     * 这里只处理功能栏位；外观栏位不应给玩家实体提供属性。
     */
    private static void onCurioAttributes(CurioAttributeModifierEvent event) {
        if (event.getSlotContext().cosmetic()) {
            return;
        }
        if (!event.getSlotContext().entity().level().isClientSide) {
            ModifierLifecycle.reconcile(
                    event.getItemStack(),
                    DefinitionManager.snapshot(),
                    SuperReforgeConfig.snapshot(),
                    event.getSlotContext().entity().getRandom().nextLong());
        }
        ModifierResolver.resolve(event.getItemStack(), DefinitionManager.snapshot()).ifPresent(modifier -> {
            for (var effect : modifier.effects()) {
                if (!effect.slots().contains(SlotTarget.CURIOS_ANY)) {
                    continue;
                }
                var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(effect.attribute());
                if (attribute.isEmpty()) {
                    SuperReforge.LOGGER.warn(
                            "Modifier {} references missing Curios Attribute {}; effect {} is inactive",
                            modifier.id(), effect.attribute(), effect.id());
                    continue;
                }
                var id = VanillaAttributeApplicator.stableModifierId(modifier.id(), effect.id());
                var attributeModifier = new AttributeModifier(
                        id,
                        effect.amount(),
                        VanillaAttributeApplicator.operation(effect.operation()));
                event.addModifier(attribute.orElseThrow(), attributeModifier);
            }
        });
    }
}
