package com.mutuo.superreforge.reforge;

import com.mutuo.superreforge.config.ExperienceMode;
import net.minecraft.server.level.ServerPlayer;

/** 集中处理等级/精确点数两种经验支付，事务只调用 canPay 和 pay。 */
public final class ExperienceService {
    private ExperienceService() {}

    public static boolean canPay(ServerPlayer player, int amount, ExperienceMode mode) {
        // 创造模式是否免费由调用方的全局配置统一判断，不能在这里无条件绕过。
        if (amount <= 0) {
            return true;
        }
        return mode == ExperienceMode.LEVELS
                ? player.experienceLevel >= amount
                : player.totalExperience >= amount;
    }

    public static void pay(ServerPlayer player, int amount, ExperienceMode mode) {
        if (amount <= 0) {
            return;
        }
        if (mode == ExperienceMode.LEVELS) {
            player.giveExperienceLevels(-amount);
        } else {
            player.giveExperiencePoints(-amount);
        }
    }
}
