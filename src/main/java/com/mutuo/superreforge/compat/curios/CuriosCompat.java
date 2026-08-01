package com.mutuo.superreforge.compat.curios;

import com.google.common.collect.HashMultimap;
import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.definition.SlotTarget;
import com.mutuo.superreforge.item.ModifierResolver;
import com.mutuo.superreforge.item.ModifierLifecycle;
import com.mutuo.superreforge.item.AttributeRefreshService;
import com.mutuo.superreforge.item.VanillaAttributeApplicator;
import com.mutuo.superreforge.config.SuperReforgeConfig;
import com.mutuo.superreforge.reforge.SelectorHooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;
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
        AttributeRefreshService.setOptionalEquipmentRefresher(CuriosCompat::refreshPlayer);
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
        ModifierResolver.resolveCurrent(event.getItemStack()).ifPresent(modifier -> {
            for (var effect : modifier.effects()) {
                if (!effect.slots().contains(SlotTarget.CURIOS_ANY)) {
                    continue;
                }
                // Curios 客户端用同一事件生成属性提示；隐藏时只跳过客户端展示，不影响服务端数值。
                boolean clientSide = event.getSlotContext().entity().level().isClientSide;
                if (clientSide && (!SuperReforgeConfig.snapshot().showAttributeLines() || !effect.showInTooltip())) {
                    continue;
                }
                var attribute = BuiltInRegistries.ATTRIBUTE.getHolder(effect.attribute());
                if (attribute.isEmpty()) {
                    SuperReforge.LOGGER.warn(
                            "Modifier {} references missing Curios Attribute {}; effect {} is inactive",
                            modifier.id(), effect.attribute(), effect.id());
                    continue;
                }
                var id = VanillaAttributeApplicator.curiosModifierId(
                        modifier.id(),
                        effect.id(),
                        event.getSlotContext().identifier(),
                        event.getSlotContext().index());
                var attributeModifier = new AttributeModifier(
                        id,
                        effect.amount(),
                        VanillaAttributeApplicator.operation(effect.operation()));
                event.addModifier(attribute.orElseThrow(), attributeModifier);
            }
        });
    }

    /**
     * 定义 reload 后直接重装所有已穿戴 Curio 的本模组属性。
     *
     * <p>不能只伪造一次穿戴变化：旧定义中已删除的 effect 已无法由当前定义算出，因此必须先按命名空间清理旧槽位 modifier。
     */
    private static void refreshPlayer(net.minecraft.server.level.ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            var oldSlotModifiers = HashMultimap.<String, AttributeModifier>create();
            handler.getModifiers().forEach((slot, modifier) -> {
                if (AttributeRefreshService.isOwnedModifier(modifier.id())) {
                    oldSlotModifiers.put(slot, modifier);
                }
            });
            handler.removeSlotModifiers(oldSlotModifiers);

            handler.getCurios().forEach((identifier, stacksHandler) -> {
                var stacks = stacksHandler.getStacks();
                for (int index = 0; index < stacks.getSlots(); index++) {
                    if (!handler.isSlotActive(identifier, index)) {
                        continue;
                    }
                    var stack = stacks.getStackInSlot(index);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    var context = new SlotContext(identifier, player, index, false, stacksHandler.isVisible());
                    var modifiers = CuriosApi.getAttributeModifiers(context, CuriosApi.getSlotId(context), stack);
                    var slotModifiers = HashMultimap.<String, AttributeModifier>create();
                    modifiers.forEach((attribute, modifier) -> {
                        if (!AttributeRefreshService.isOwnedModifier(modifier.id())) {
                            return;
                        }
                        if (attribute.value() instanceof SlotAttribute slotAttribute) {
                            slotModifiers.put(slotAttribute.getIdentifier(), modifier);
                            return;
                        }
                        var instance = player.getAttribute(attribute);
                        if (instance != null) {
                            instance.addOrUpdateTransientModifier(modifier);
                        }
                    });
                    handler.addTransientSlotModifiers(slotModifiers);
                }
            });
        });
    }
}
