package com.mutuo.superreforge.compat.curios;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;

/** 将 Curios 的实体上下文归类，避免创意物品栏的合成 tooltip 上下文被误当作服务端实体。 */
public final class CuriosContextPolicy {
    public enum Kind {
        SYNTHETIC_CLIENT,
        CLIENT_ENTITY,
        SERVER_ENTITY
    }

    private CuriosContextPolicy() {}

    public static Kind classify(@Nullable LivingEntity entity) {
        // Curios 在创意物品栏和 JEI 建索引时会传入 null；它只能生成 tooltip，绝不能改写物品数据。
        if (entity == null) {
            return Kind.SYNTHETIC_CLIENT;
        }
        return entity.level().isClientSide ? Kind.CLIENT_ENTITY : Kind.SERVER_ENTITY;
    }
}
