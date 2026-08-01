package com.mutuo.superreforge.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mutuo.superreforge.registry.ModBlocks;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** 锁定熔核锻台的水平朝向状态，避免以后把有正面的 3D 模型退化成无方向方块。 */
final class ReforgerBlockTest {
    @Test
    void defaultStateFacesNorthAndExposesHorizontalFacing() {
        // NeoForge 单元测试启动时注册表已经冻结，应检查模组真实注册实例而不是临时注册第二个 Block。
        ReforgerBlock block = ModBlocks.REFORGER.get();

        assertTrue(block.defaultBlockState().hasProperty(ReforgerBlock.FACING));
        assertEquals(Direction.NORTH, block.defaultBlockState().getValue(ReforgerBlock.FACING));
        assertEquals(4, ReforgerBlock.FACING.getPossibleValues().size());
    }
}
