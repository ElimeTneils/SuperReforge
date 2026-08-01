package com.mutuo.superreforge.reforge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mutuo.superreforge.definition.DefinitionSnapshot;
import com.mutuo.superreforge.definition.ItemSelector;
import com.mutuo.superreforge.definition.ItemTypeDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

/** 验证精确 ID 可硬绑定物品，并允许一个物品同时属于多个自定义类型。 */
final class ItemTypeResolverTest {
    @Test
    void stickCanBelongToSwordAndAxeAtTheSameTime() {
        ResourceLocation stick = ResourceLocation.withDefaultNamespace("stick");
        ItemSelector exactStick = new ItemSelector(
                Optional.of(stick), List.of(), Optional.empty(), Optional.empty(), Optional.empty());
        DefinitionSnapshot snapshot = new DefinitionSnapshot(
                Map.of(),
                Map.of(
                        id("sword"), new ItemTypeDefinition(List.of(exactStick), List.of()),
                        id("axe"), new ItemTypeDefinition(List.of(exactStick), List.of())),
                Map.of(),
                Map.of());

        assertEquals(
                List.of(id("axe"), id("sword")),
                ItemTypeResolver.resolve(new ItemStack(Items.STICK), snapshot).stream().sorted().toList());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
