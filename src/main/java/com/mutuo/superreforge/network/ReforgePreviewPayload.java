package com.mutuo.superreforge.network;

import com.mutuo.superreforge.SuperReforge;
import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.reforge.ReforgeFailure;
import com.mutuo.superreforge.reforge.ReforgeQuoteResult;
import com.mutuo.superreforge.reforge.WeightNormalizer;
import com.mutuo.superreforge.reforge.WeightedValue;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端向当前菜单同步的只读真实报价；客户端不能把其中任何数值发回执行。 */
public record ReforgePreviewPayload(
        int containerId,
        long generation,
        int failureOrdinal,
        int materialCost,
        int experienceCost,
        List<PreviewLevel> levels) implements CustomPacketPayload {
    private static final int MAX_LEVELS = 64;
    private static final int MAX_MODIFIERS_PER_LEVEL = 256;

    public static final Type<ReforgePreviewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SuperReforge.MOD_ID, "reforge_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReforgePreviewPayload> STREAM_CODEC =
            CustomPacketPayload.codec(ReforgePreviewPayload::write, ReforgePreviewPayload::decode);

    public ReforgePreviewPayload {
        if (generation < 0) {
            throw new IllegalArgumentException("预览定义代次不能为负数");
        }
        levels = List.copyOf(levels);
        if (levels.size() > MAX_LEVELS) {
            throw new IllegalArgumentException("预览等级数量超过网络上限 " + MAX_LEVELS);
        }
    }

    public static ReforgePreviewPayload from(
            int containerId, ReforgeQuoteResult result, DefinitionSnapshot snapshot) {
        if (result.quote().isEmpty()) {
            int failure = result.failure().map(Enum::ordinal).orElse(ReforgeFailure.STALE_STATE.ordinal());
            return new ReforgePreviewPayload(
                    containerId, com.mutuo.superreforge.definition.DefinitionManager.generation(),
                    failure, 0, 0, List.of());
        }
        var quote = result.quote().orElseThrow();
        var normalizedLevels = WeightNormalizer.normalize(quote.candidates().levels().stream()
                .map(level -> new WeightedValue<>(level, level.weight()))
                .toList());
        List<PreviewLevel> levels = new ArrayList<>();
        for (var weightedLevel : normalizedLevels.stream().limit(MAX_LEVELS).toList()) {
            var candidateLevel = weightedLevel.value();
            Component levelName = snapshot.levels().containsKey(candidateLevel.level())
                    ? snapshot.levels().get(candidateLevel.level()).name()
                    : Component.literal(candidateLevel.level().toString());
            var normalizedModifiers = WeightNormalizer.normalize(candidateLevel.modifiers());
            List<PreviewModifier> modifiers = normalizedModifiers.stream()
                    .limit(MAX_MODIFIERS_PER_LEVEL)
                    .map(weighted -> {
                        Component name = snapshot.modifiers().containsKey(weighted.value())
                                ? snapshot.modifiers().get(weighted.value()).name()
                                : Component.literal(weighted.value().toString());
                        return new PreviewModifier(
                                weighted.value(), name, weightedLevel.probability() * weighted.probability());
                    })
                    .toList();
            levels.add(new PreviewLevel(
                    candidateLevel.level(), levelName, weightedLevel.probability(), modifiers));
        }
        return new ReforgePreviewPayload(
                containerId,
                com.mutuo.superreforge.definition.DefinitionManager.generation(),
                -1,
                quote.cost().materialCount(),
                quote.cost().experience(),
                levels);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeVarLong(generation);
        buffer.writeVarInt(failureOrdinal + 1);
        buffer.writeVarInt(materialCost);
        buffer.writeVarInt(experienceCost);
        buffer.writeVarInt(levels.size());
        for (PreviewLevel level : levels) {
            buffer.writeResourceLocation(level.id());
            ComponentSerialization.STREAM_CODEC.encode(buffer, level.name());
            buffer.writeDouble(level.probability());
            buffer.writeVarInt(level.modifiers().size());
            for (PreviewModifier modifier : level.modifiers()) {
                buffer.writeResourceLocation(modifier.id());
                ComponentSerialization.STREAM_CODEC.encode(buffer, modifier.name());
                buffer.writeDouble(modifier.probability());
            }
        }
    }

    private static ReforgePreviewPayload decode(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        long generation = buffer.readVarLong();
        if (generation < 0) {
            throw new IllegalArgumentException("非法预览定义代次: " + generation);
        }
        int failure = buffer.readVarInt() - 1;
        int material = buffer.readVarInt();
        int experience = buffer.readVarInt();
        int levelCount = buffer.readVarInt();
        if (levelCount < 0 || levelCount > MAX_LEVELS) {
            throw new IllegalArgumentException("非法预览等级数量: " + levelCount);
        }
        List<PreviewLevel> levels = new ArrayList<>(levelCount);
        for (int levelIndex = 0; levelIndex < levelCount; levelIndex++) {
            ResourceLocation levelId = buffer.readResourceLocation();
            Component levelName = ComponentSerialization.STREAM_CODEC.decode(buffer);
            double levelProbability = buffer.readDouble();
            int modifierCount = buffer.readVarInt();
            if (modifierCount < 0 || modifierCount > MAX_MODIFIERS_PER_LEVEL) {
                throw new IllegalArgumentException("非法预览词条数量: " + modifierCount);
            }
            List<PreviewModifier> modifiers = new ArrayList<>(modifierCount);
            for (int modifierIndex = 0; modifierIndex < modifierCount; modifierIndex++) {
                modifiers.add(new PreviewModifier(
                        buffer.readResourceLocation(),
                        ComponentSerialization.STREAM_CODEC.decode(buffer),
                        buffer.readDouble()));
            }
            levels.add(new PreviewLevel(levelId, levelName, levelProbability, modifiers));
        }
        return new ReforgePreviewPayload(containerId, generation, failure, material, experience, levels);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
