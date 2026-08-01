package com.mutuo.superreforge.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mutuo.superreforge.SuperReforge;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** 从各命名空间下的四个 {@code superreforge} 数据目录读取并解析 datapack 定义。 */
public final class DefinitionReloadListener extends SimplePreparableReloadListener<DefinitionSnapshot> {
    @Override
    protected DefinitionSnapshot prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<String> errors = new ArrayList<>();
        Map<ResourceLocation, LevelDefinition> levels =
                readDirectory(resourceManager, "levels", LevelDefinition.CODEC, errors);
        Map<ResourceLocation, ItemTypeDefinition> itemTypes =
                readDirectory(resourceManager, "item_types", ItemTypeDefinition.CODEC, errors);
        Map<ResourceLocation, ModifierDefinition> modifiers =
                readDirectory(resourceManager, "modifiers", ModifierDefinition.CODEC, errors);
        Map<ResourceLocation, CatalystDefinition> catalysts =
                readDirectory(resourceManager, "catalysts", CatalystDefinition.CODEC, errors);

        if (!errors.isEmpty()) {
            throw new DefinitionValidationException(String.join(System.lineSeparator(), errors));
        }
        return new DefinitionSnapshot(levels, itemTypes, modifiers, catalysts);
    }

    @Override
    protected void apply(DefinitionSnapshot prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        if (DefinitionManager.publishDatapack(prepared)) {
            SuperReforge.LOGGER.info(
                    "Loaded Super Reforge definitions: {} levels, {} types, {} modifiers, {} catalysts",
                    prepared.levels().size(),
                    prepared.itemTypes().size(),
                    prepared.modifiers().size(),
                    prepared.catalysts().size());
        }
    }

    private static <T> Map<ResourceLocation, T> readDirectory(
            ResourceManager manager, String directory, Codec<T> codec, List<String> errors) {
        // 1.21.1 的 ResourceManager 明确拒绝带末尾斜杠的 listResources 目录参数。
        String resourceDirectory = "superreforge/" + directory;
        // 解析相对定义 ID 时仍需要斜杠边界，避免把相似目录名前缀一起截入。
        String relativePrefix = resourceDirectory + "/";
        Map<ResourceLocation, T> decoded = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> resources =
                manager.listResources(resourceDirectory, id -> id.getPath().endsWith(".json"));

        resources.forEach((fileId, resource) -> {
            ResourceLocation definitionId = toDefinitionId(fileId, relativePrefix);
            try (Reader reader = resource.openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                T value = codec.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(message -> new IllegalArgumentException(message));
                decoded.put(definitionId, value);
            } catch (IOException | RuntimeException exception) {
                errors.add(fileId + ": " + exception.getMessage());
            }
        });
        return decoded;
    }

    private static ResourceLocation toDefinitionId(ResourceLocation fileId, String prefix) {
        String relative = fileId.getPath().substring(prefix.length());
        relative = relative.substring(0, relative.length() - ".json".length());
        return ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), relative);
    }
}
