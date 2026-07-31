package com.mutuo.superreforge.client;

import com.mutuo.superreforge.block.ReforgerMenu;
import com.mutuo.superreforge.network.ClientPreviewState;
import com.mutuo.superreforge.network.ReforgePreviewPayload;
import com.mutuo.superreforge.reforge.ReforgeFailure;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * “锻造日志”界面：左侧操作与成本，右侧展示服务端归一化后的等级/词条概率。
 *
 * <p>界面不自行计算价格；远程服务器的 datapack/KubeJS 定义通过预览 payload 同步。
 */
public final class ReforgerScreen extends AbstractContainerScreen<ReforgerMenu> {
    private Button reforgeButton;

    public ReforgerScreen(ReforgerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 198;
        inventoryLabelY = 104;
    }

    @Override
    protected void init() {
        super.init();
        reforgeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.superreforge.reforge"),
                        button -> {
                            if (minecraft != null && minecraft.gameMode != null) {
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId, ReforgerMenu.START_BUTTON);
                            }
                        })
                .bounds(leftPos + 66, topPos + 74, 72, 20)
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (reforgeButton != null) {
            reforgeButton.active = !menu.pending();
            reforgeButton.setMessage(Component.translatable(
                    menu.pending() ? "gui.superreforge.forging" : "gui.superreforge.reforge"));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xEE171317);
        graphics.fill(left + 5, top + 18, left + 145, top + 101, 0xFF2A2222);
        graphics.fill(left + 149, top + 18, left + 251, top + 101, 0xFF211C24);
        graphics.renderOutline(left + 5, top + 18, 140, 83, 0xFF8B5A3C);
        graphics.renderOutline(left + 149, top + 18, 102, 83, 0xFF6A526E);
        graphics.fill(left + 34, top + 43, left + 54, top + 63, 0xFF0E0C0D);
        graphics.fill(left + 34, top + 79, left + 54, top + 99, 0xFF0E0C0D);
        graphics.renderOutline(left + 34, top + 43, 20, 20, 0xFFB98754);
        graphics.renderOutline(left + 34, top + 79, 20, 20, 0xFFB98754);

        if (menu.pending()) {
            ReforgerRenderState state = ReforgerRenderState.fromTicks(
                    menu.totalTicks(), menu.remainingTicks(), partialTick);
            int glow = (int) (80 + 175 * state.coreIntensity());
            int color = 0xFF000000 | (glow << 16) | (Math.min(255, glow / 2) << 8);
            graphics.fill(left + 91, top + 45, left + 113, top + 65, color);
            int hammerY = top + 20 + (int) ((1.2F - state.hammerHeight()) * 26.0F);
            graphics.fill(left + 87, hammerY, left + 117, hammerY + 7, 0xFFB8A39A);
            graphics.fill(left + 99, hammerY + 7, left + 105, hammerY + 22, 0xFF6B4231);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFF1D4AE, false);
        graphics.drawString(font, Component.translatable("gui.superreforge.target"), 58, 47, 0xFFE0C9B5, false);
        graphics.drawString(font, Component.translatable("gui.superreforge.catalyst"), 58, 83, 0xFFE0C9B5, false);
        graphics.drawString(font, Component.translatable("gui.superreforge.log"), 154, 23, 0xFFD8C5E2, false);

        ReforgePreviewPayload preview = ClientPreviewState.get(menu.containerId).orElse(null);
        if (preview == null) {
            graphics.drawString(font, Component.translatable("gui.superreforge.waiting"), 154, 37, 0xFF999199, false);
            return;
        }
        if (preview.failureOrdinal() >= 0) {
            ReforgeFailure[] failures = ReforgeFailure.values();
            String id = preview.failureOrdinal() < failures.length
                    ? failures[preview.failureOrdinal()].name().toLowerCase(java.util.Locale.ROOT)
                    : "stale_state";
            graphics.drawString(font, Component.translatable("gui.superreforge.failure." + id), 154, 37, 0xFFE57373, false);
            return;
        }

        graphics.drawString(
                font,
                Component.translatable("gui.superreforge.cost", preview.materialCost(), preview.experienceCost()),
                58,
                65,
                0xFFFFC46B,
                false);
        int y = 37;
        for (var level : preview.levels()) {
            String line = level.name().getString() + " " + percent(level.probability());
            graphics.drawString(font, font.plainSubstrByWidth(line, 92), 154, y, 0xFFFFD79B, false);
            y += 10;
            for (var modifier : level.modifiers()) {
                if (y > 93) {
                    break;
                }
                String modifierLine = "  " + modifier.name().getString() + " " + percent(modifier.probability());
                graphics.drawString(font, font.plainSubstrByWidth(modifierLine, 92), 154, y, 0xFFBDB4C2, false);
                y += 9;
            }
            if (y > 93) {
                break;
            }
        }
    }

    private static String percent(double probability) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", probability * 100.0);
    }
}
