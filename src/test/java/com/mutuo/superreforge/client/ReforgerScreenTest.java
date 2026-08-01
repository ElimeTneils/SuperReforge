package com.mutuo.superreforge.client;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mutuo.superreforge.network.ReforgePreviewPayload;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证原版重铸按钮只在服务端报价有效且客户端定义同步后启用。 */
final class ReforgerScreenTest {
    @Test
    void disablesButtonForMissingFailedStaleOrPendingQuotes() {
        long currentGeneration = 42L;
        ReforgePreviewPayload valid = preview(currentGeneration, -1);
        assertAll(
                () -> assertFalse(ReforgerScreen.canStart(true, valid, currentGeneration)),
                () -> assertFalse(ReforgerScreen.canStart(false, null, currentGeneration)),
                () -> assertFalse(ReforgerScreen.canStart(false, preview(41L, -1), currentGeneration)),
                () -> assertFalse(ReforgerScreen.canStart(false, preview(currentGeneration, 0), currentGeneration)),
                () -> assertTrue(ReforgerScreen.canStart(false, valid, currentGeneration)));
    }

    private static ReforgePreviewPayload preview(long generation, int failureOrdinal) {
        return new ReforgePreviewPayload(0, generation, failureOrdinal, 0, 0, false, List.of());
    }
}
