package com.mutuo.superreforge.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

/** 验证 datapack 公共格式能被生产 Codec 真实解码。 */
final class DefinitionCodecTest {
    @Test
    void decodesStyledLevelName() {
        var json = JsonParser.parseString("""
                {"rank":4,"name":{"text":"传说","color":"gold","bold":true}}
                """);

        LevelDefinition level = LevelDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(4, level.rank());
        assertEquals("传说", level.name().getString());
        assertTrue(level.name().getStyle().isBold());
    }

    @Test
    void decodesModifierWithFixedAndRandomAttributes() {
        var json = JsonParser.parseString("""
                {
                  "level":"example:legendary",
                  "item_types":["example:sword"],
                  "name":{"translate":"modifier.example.legendary","fallback":"传说","color":"gold"},
                  "weight":20,
                  "attributes":[
                    {"id":"damage","attribute":"minecraft:generic.attack_damage","amount":0.04,
                     "operation":"add_multiplied_base","slots":["mainhand"],"show_in_tooltip":true},
                    {"id":"speed","attribute":"minecraft:generic.attack_speed","amount":{"min":0.06,"max":0.10},
                     "operation":"add_multiplied_total","slots":["mainhand"],"show_in_tooltip":false}
                  ]
                }
                """);

        ModifierDefinition modifier =
                ModifierDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(20.0, modifier.weight());
        assertEquals(2, modifier.attributes().size());
        assertTrue(modifier.attributes().getFirst().amount() instanceof ValueDefinition.Fixed);
        assertTrue(modifier.attributes().getLast().amount() instanceof ValueDefinition.Range);
        assertFalse(modifier.attributes().getLast().showInTooltip());
    }

    @Test
    void missingModifierWeightDefaultsToOne() {
        var json = JsonParser.parseString("""
                {"level":"superreforge:tier_1","item_types":["superreforge:sword"],
                 "name":{"text":"坚固"},"attributes":[]}
                """);

        ModifierDefinition modifier =
                ModifierDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(1.0, modifier.weight());
    }

    @Test
    void decodesItemTypeSelectorsAndCatalystWeights() {
        var typeJson = JsonParser.parseString("""
                {"include":[{"tag":"example:reforgeable/swords"},{"items":["minecraft:stick"]}],
                 "exclude":[{"item":"example:forbidden_sword"}]}
                """);
        var catalystJson = JsonParser.parseString("""
                {"ingredient":{"item":"superreforge:common_reforge_stone"},"count":2,"experience":5,
                 "allow_same_modifier":false,"allowed_item_types":[],"denied_item_types":[],
                 "levels":[{"level":"superreforge:tier_1","weight":35},
                           {"level":"superreforge:tier_4","weight":20}]}
                """);

        ItemTypeDefinition type =
                ItemTypeDefinition.CODEC.parse(JsonOps.INSTANCE, typeJson).getOrThrow();
        CatalystDefinition catalyst =
                CatalystDefinition.CODEC.parse(JsonOps.INSTANCE, catalystJson).getOrThrow();

        assertEquals(2, type.include().size());
        assertEquals(1, type.exclude().size());
        assertEquals(2, catalyst.count());
        assertEquals(35.0, catalyst.levels().getFirst().weight());
        assertFalse(catalyst.allowSameModifier());
    }
}
