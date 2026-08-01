package com.mutuo.superreforge.item;

import com.mutuo.superreforge.SuperReforge;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * 在定义 reload 后移除并重新安装本模组的瞬时 Attribute modifier。
 *
 * <p>物品只保存词条 ID 与种子，定义变化不会自然改变 ItemStack；因此在线且已穿戴的装备必须由服务端主动刷新。
 */
public final class AttributeRefreshService {
    private static volatile Consumer<ServerPlayer> optionalEquipmentRefresher = player -> {};

    private AttributeRefreshService() {}

    /** Curios 等可选兼容层在真正加载后注入自己的已穿戴物刷新器。 */
    public static void setOptionalEquipmentRefresher(Consumer<ServerPlayer> refresher) {
        optionalEquipmentRefresher = refresher;
    }

    /** 清除所有旧 Super Reforge 数值，再按当前定义重读原版装备和可选饰品。 */
    public static void refresh(ServerPlayer player) {
        BuiltInRegistries.ATTRIBUTE.holders().forEach(attribute -> {
            var instance = player.getAttribute(attribute);
            if (instance == null) {
                return;
            }
            // 复制后再删除，避免遍历 AttributeInstance 的实时集合时并发修改。
            new ArrayList<>(instance.getModifiers()).stream()
                    .filter(modifier -> isOwnedModifier(modifier.id()))
                    .forEach(instance::removeModifier);
        });

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            player.getItemBySlot(slot).forEachModifier(slot, (attribute, modifier) -> {
                if (!isOwnedModifier(modifier.id())) {
                    return;
                }
                var instance = player.getAttribute(attribute);
                if (instance != null) {
                    instance.addOrUpdateTransientModifier(modifier);
                }
            });
        }
        optionalEquipmentRefresher.accept(player);
    }

    /** 所有本模组动态 modifier 都位于固定命名空间和 effect/ 路径下。 */
    public static boolean isOwnedModifier(ResourceLocation id) {
        return SuperReforge.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("effect/");
    }
}
