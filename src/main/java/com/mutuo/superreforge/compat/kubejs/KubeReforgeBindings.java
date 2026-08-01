package com.mutuo.superreforge.compat.kubejs;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mutuo.superreforge.api.ScriptDefinitionCollector;
import com.mutuo.superreforge.definition.CatalystDefinition;
import com.mutuo.superreforge.definition.ItemTypeDefinition;
import com.mutuo.superreforge.definition.LevelDefinition;
import com.mutuo.superreforge.definition.ModifierDefinition;
import com.mutuo.superreforge.progress.ProgressService;
import com.mutuo.superreforge.progress.ProgressStage;
import com.mutuo.superreforge.reforge.CostModifier;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.Context;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

/**
 * `server_scripts` 中的全局 `SuperReforge` 对象。
 *
 * <p>四种定义沿用 datapack 完全相同的 JSON 结构，因此整合包作者只需把 JSON 对象直接写进 JS。
 * 进度阶段则提供明确参数，分别配置材料与经验的乘数、加数。
 */
public final class KubeReforgeBindings {
    private volatile ScriptDefinitionCollector activeCollector;

    /** 由插件在一次 SERVER reload 开始前安装临时收集器，不暴露给脚本。 */
    void begin(ScriptDefinitionCollector collector) {
        activeCollector = Objects.requireNonNull(collector, "collector");
    }

    /** reload 成功或失败后都断开临时收集器，防止运行期误改定义。 */
    void end() {
        activeCollector = null;
    }

    public void addLevel(Context context, String id, Object definition) {
        collector().addLevel(id(id), decode(context, LevelDefinition.CODEC, definition, "level " + id));
    }

    public void addItemType(Context context, String id, Object definition) {
        collector().addItemType(
                id(id), decode(context, ItemTypeDefinition.CODEC, definition, "item type " + id));
    }

    public void addModifier(Context context, String id, Object definition) {
        collector().addModifier(
                id(id), decode(context, ModifierDefinition.CODEC, definition, "modifier " + id));
    }

    public void addCatalyst(Context context, String id, Object definition) {
        collector().addCatalyst(
                id(id), decode(context, CatalystDefinition.CODEC, definition, "catalyst " + id));
    }

    /** 注册供 item type 的 kubejs_predicate 字段引用的物品判断函数。 */
    public void addPredicate(String id, Predicate<ItemStack> predicate) {
        collector().addPredicate(id, Objects.requireNonNull(predicate, "predicate"));
    }

    /**
     * 定义一个服务器共享进度阶段；只会采用已激活阶段中 priority 最高的一项。
     * 成本公式均为 floor(原成本 × multiplier) + addition。
     */
    public void addStage(
            String id,
            int priority,
            double materialMultiplier,
            int materialAddition,
            double experienceMultiplier,
            int experienceAddition) {
        collector().addStage(new ProgressStage(
                id,
                priority,
                new CostModifier(materialMultiplier, materialAddition),
                new CostModifier(experienceMultiplier, experienceAddition)));
    }

    /** 在 ServerEvents.loaded、战利品事件或击杀事件中持久化激活/停用一个全服阶段。 */
    public void setStageActive(MinecraftServer server, String id, boolean active) {
        ProgressService.setActive(Objects.requireNonNull(server, "server"), id, active);
    }

    /** 返回当前最高优先级阶段 ID；没有激活阶段时返回空字符串，便于 JS 直接比较。 */
    public String getActiveStage(MinecraftServer server) {
        return ProgressService.highestActive(Objects.requireNonNull(server, "server"))
                .map(ProgressStage::id)
                .orElse("");
    }

    private ScriptDefinitionCollector collector() {
        ScriptDefinitionCollector collector = activeCollector;
        if (collector == null) {
            throw new IllegalStateException("SuperReforge.add* 定义 API 只能在 KubeJS server_scripts 加载期间调用");
        }
        return collector;
    }

    private static ResourceLocation id(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException("无效资源 ID: " + value);
        }
        return id;
    }

    private static <T> T decode(Context context, Codec<T> codec, Object raw, String owner) {
        JsonElement json = JsonUtils.of(context, raw);
        return codec.parse(JsonOps.INSTANCE, json)
                .getOrThrow(error -> new IllegalArgumentException(owner + " 的 JSON 无效：" + error));
    }
}
