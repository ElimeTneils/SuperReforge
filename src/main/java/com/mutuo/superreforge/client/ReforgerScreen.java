package com.mutuo.superreforge.client;

import com.mutuo.superreforge.block.ReforgerLayout;
import com.mutuo.superreforge.block.ReforgerMenu;
import com.mutuo.superreforge.definition.DefinitionManager;
import com.mutuo.superreforge.network.ClientDefinitionSync;
import com.mutuo.superreforge.network.ClientPreviewState;
import com.mutuo.superreforge.network.ReforgePreviewPayload;
import com.mutuo.superreforge.reforge.ReforgeFailure;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * “熔核锻造日志”容器界面：左侧操作，右侧完整显示服务端候选概率并提供滚动和 Attribute 悬停详情。
 *
 * <p>界面只消费服务端报价与同步定义，不在客户端重算成本、候选池或随机结果。
 */
public final class ReforgerScreen extends AbstractContainerScreen<ReforgerMenu> {
    private static final int COLOR_BACKGROUND = 0xF0181418;
    private static final int COLOR_FRAME = 0xFF4B3432;
    private static final int COLOR_COPPER = 0xFFB56F48;
    private static final int COLOR_LOG = 0xFF211D27;
    private static final int COLOR_LOG_ALT = 0xFF28222D;

    private Button reforgeButton;
    private ReforgePreviewPayload renderedPreview;
    private ReforgerLogModel logModel = ReforgerLogModel.from(emptyPreview());
    private int scrollOffset;
    private boolean draggingScrollbar;
    private double scrollbarGrabOffset;
    private ResourceLocation hoveredModifier;
    private boolean hoveredPreviewWarning;

    public ReforgerScreen(ReforgerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ReforgerLayout.GUI_WIDTH;
        imageHeight = ReforgerLayout.GUI_HEIGHT;
        inventoryLabelX = ReforgerLayout.playerInventoryLeft();
        inventoryLabelY = 124;
    }

    @Override
    protected void init() {
        super.init();
        ClientPreviewState.clear(menu.containerId);
        renderedPreview = null;
        logModel = ReforgerLogModel.from(emptyPreview());
        scrollOffset = 0;
        draggingScrollbar = false;
        reforgeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.superreforge.reforge"),
                        button -> {
                            if (minecraft != null && minecraft.gameMode != null) {
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId, ReforgerMenu.START_BUTTON);
                            }
                        })
                .bounds(leftPos + 66, topPos + 91, 76, 20)
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ReforgePreviewPayload preview = ClientPreviewState.get(menu.containerId).orElse(null);
        updateLogModel(preview);
        if (reforgeButton != null) {
            boolean synchronizedDefinitions = preview != null
                    && preview.generation() == ClientDefinitionSync.completedGeneration();
            reforgeButton.active = !menu.pending() && synchronizedDefinitions;
            reforgeButton.setMessage(Component.translatable(
                    menu.pending() ? "gui.superreforge.forging" : "gui.superreforge.reforge"));
        }
    }

    @Override
    public void removed() {
        ClientPreviewState.clear(menu.containerId);
        super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        hoveredModifier = null;
        hoveredPreviewWarning = false;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (hoveredModifier != null) {
            List<Component> lines = ModifierTooltipFormatter.format(
                    hoveredModifier, DefinitionManager.clientModifiers().get(hoveredModifier));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (hoveredPreviewWarning) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(Component.translatable("gui.superreforge.preview_truncated")),
                    mouseX,
                    mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, COLOR_BACKGROUND);
        graphics.renderOutline(left, top, imageWidth, imageHeight, COLOR_FRAME);
        graphics.fill(left + 2, top + 16, left + imageWidth - 2, top + 18, COLOR_COPPER);

        // 左侧锻造操作区、右侧日志区和下方背包区使用不同层次，保持像素风但提高可读性。
        graphics.fill(left + 5, top + 20, left + 149, top + 120, 0xFF2A2222);
        graphics.fill(left + 151, top + 20, left + 281, top + 120, COLOR_LOG);
        graphics.fill(left + 50, top + 128, left + 236, top + 214, 0xFF211C20);
        graphics.renderOutline(left + 5, top + 20, 144, 100, COLOR_COPPER);
        graphics.renderOutline(left + 151, top + 20, 130, 100, 0xFF765B7B);
        graphics.renderOutline(left + 50, top + 128, 186, 86, 0xFF59464E);

        drawSlotFrame(graphics, left + ReforgerLayout.TARGET_X, top + ReforgerLayout.TARGET_Y);
        drawSlotFrame(graphics, left + ReforgerLayout.CATALYST_X, top + ReforgerLayout.CATALYST_Y);
        drawForgeStage(graphics, partialTick);
        drawScrollbar(graphics);
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0E0C0D);
        graphics.renderOutline(x - 2, y - 2, 20, 20, 0xFFB98754);
        graphics.fill(x - 1, y - 1, x + 17, y, 0xFF6A4233);
    }

    private void drawForgeStage(GuiGraphics graphics, float partialTick) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left + 101, top + 45, left + 127, top + 69, 0xFF171216);
        graphics.renderOutline(left + 99, top + 43, 30, 28, 0xFF694338);
        int glow = 88;
        int hammerY = top + 28;
        if (menu.pending()) {
            ReforgerRenderState state = ReforgerRenderState.fromTicks(
                    menu.totalTicks(), menu.remainingTicks(), partialTick);
            glow = (int) (80 + 175 * state.coreIntensity());
            hammerY = top + 22 + (int) ((1.2F - state.hammerHeight()) * 26.0F);
        }
        int color = 0xFF000000 | (Math.min(255, glow) << 16) | (Math.min(255, glow / 2) << 8);
        graphics.fill(left + 104, top + 53, left + 124, top + 67, color);
        graphics.fill(left + 98, hammerY, left + 130, hammerY + 7, 0xFFC7B7AE);
        graphics.fill(left + 111, hammerY + 7, left + 117, hammerY + 23, 0xFF704735);
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int x = leftPos + ReforgerLayout.SCROLLBAR_X;
        int y = topPos + ReforgerLayout.LOG_TOP;
        graphics.fill(x, y, x + ReforgerLayout.SCROLLBAR_WIDTH, y + ReforgerLayout.LOG_HEIGHT, 0xFF130F16);
        int thumbHeight = logModel.thumbHeight(ReforgerLayout.LOG_HEIGHT, ReforgerLayout.LOG_HEIGHT);
        int thumbTop = scrollbarThumbTop(thumbHeight);
        int thumbColor = draggingScrollbar ? 0xFFE0A164 : 0xFF9E6A52;
        graphics.fill(x + 1, y + thumbTop, x + ReforgerLayout.SCROLLBAR_WIDTH - 1, y + thumbTop + thumbHeight, thumbColor);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFF1D4AE, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFCDB9B1, false);
        graphics.drawString(font, Component.translatable("gui.superreforge.target"), 58, 48, 0xFFE0C9B5, false);
        graphics.drawString(font, Component.translatable("gui.superreforge.catalyst"), 58, 84, 0xFFE0C9B5, false);
        graphics.drawString(font, Component.translatable("gui.superreforge.log"), 156, 24, 0xFFE3CDEA, false);

        ReforgePreviewPayload preview = ClientPreviewState.get(menu.containerId).orElse(null);
        updateLogModel(preview);
        if (preview == null) {
            graphics.drawString(font, Component.translatable("gui.superreforge.waiting"), 156, 38, 0xFF999199, false);
            return;
        }
        if (preview.failureOrdinal() >= 0) {
            drawFailure(graphics, preview.failureOrdinal());
            return;
        }

        graphics.drawString(
                font,
                Component.translatable("gui.superreforge.cost", preview.materialCost(), preview.experienceCost()),
                58,
                68,
                0xFFFFC46B,
                false);
        if (preview.truncated()) {
            graphics.drawString(font, "!", 263, 24, 0xFFFF9C54, false);
            hoveredPreviewWarning = ReforgerLayout.insidePreviewWarning(
                    mouseX - leftPos, mouseY - topPos);
        }
        drawLogRows(graphics, mouseX - leftPos, mouseY - topPos);
    }

    private void drawFailure(GuiGraphics graphics, int failureOrdinal) {
        ReforgeFailure[] failures = ReforgeFailure.values();
        String id = failureOrdinal < failures.length
                ? failures[failureOrdinal].name().toLowerCase(Locale.ROOT)
                : "stale_state";
        Component message = Component.translatable("gui.superreforge.failure." + id);
        graphics.drawWordWrap(font, message, ReforgerLayout.LOG_LEFT + 2, ReforgerLayout.LOG_TOP + 2,
                ReforgerLayout.LOG_WIDTH - 6, 0xFFE57373);
    }

    private void drawLogRows(GuiGraphics graphics, int relativeMouseX, int relativeMouseY) {
        int absoluteLeft = leftPos + ReforgerLayout.LOG_LEFT;
        int absoluteTop = topPos + ReforgerLayout.LOG_TOP;
        graphics.enableScissor(
                absoluteLeft,
                absoluteTop,
                absoluteLeft + ReforgerLayout.LOG_WIDTH,
                absoluteTop + ReforgerLayout.LOG_HEIGHT);
        int rowIndex = 0;
        for (ReforgerLogModel.Row row : logModel.rows()) {
            int y = ReforgerLayout.LOG_TOP + row.top() - scrollOffset;
            if (y + row.height() <= ReforgerLayout.LOG_TOP
                    || y >= ReforgerLayout.LOG_TOP + ReforgerLayout.LOG_HEIGHT) {
                rowIndex++;
                continue;
            }
            if ((rowIndex & 1) != 0) {
                graphics.fill(
                        ReforgerLayout.LOG_LEFT,
                        y,
                        ReforgerLayout.LOG_LEFT + ReforgerLayout.LOG_WIDTH,
                        y + row.height(),
                        COLOR_LOG_ALT);
            }
            int nameX = ReforgerLayout.LOG_LEFT + (row.kind() == ReforgerLogModel.Kind.MODIFIER ? 7 : 2);
            int color = row.kind() == ReforgerLogModel.Kind.LEVEL ? 0xFFFFD79B : 0xFFC8BECF;
            graphics.drawString(font, row.name(), nameX, y + 1, color, false);
            String probability = percent(row.probability());
            int probabilityX = ReforgerLayout.LOG_LEFT + ReforgerLayout.LOG_WIDTH - 3 - font.width(probability);
            graphics.fill(probabilityX - 2, y, ReforgerLayout.LOG_LEFT + ReforgerLayout.LOG_WIDTH, y + row.height(), COLOR_LOG);
            graphics.drawString(font, probability, probabilityX, y + 1, color, false);
            if (row.kind() == ReforgerLogModel.Kind.MODIFIER
                    && ReforgerLayout.insideLog(relativeMouseX, relativeMouseY)
                    && relativeMouseY >= y
                    && relativeMouseY < y + row.height()) {
                hoveredModifier = row.id();
            }
            rowIndex++;
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;
        if (!ReforgerLayout.insideLog(relativeX, relativeY)
                && !ReforgerLayout.insideScrollbar(relativeX, relativeY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = logModel.scrollBy(scrollOffset, scrollY, ReforgerLayout.LOG_HEIGHT);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;
        if (button == 0 && ReforgerLayout.insideScrollbar(relativeX, relativeY)) {
            int thumbHeight = logModel.thumbHeight(ReforgerLayout.LOG_HEIGHT, ReforgerLayout.LOG_HEIGHT);
            int thumbTop = scrollbarThumbTop(thumbHeight);
            double trackY = relativeY - ReforgerLayout.LOG_TOP;
            if (trackY >= thumbTop && trackY < thumbTop + thumbHeight) {
                scrollbarGrabOffset = trackY - thumbTop;
            } else {
                scrollbarGrabOffset = thumbHeight / 2.0;
                scrollOffset = logModel.scrollFromThumb(
                        trackY - scrollbarGrabOffset,
                        ReforgerLayout.LOG_HEIGHT,
                        thumbHeight,
                        ReforgerLayout.LOG_HEIGHT);
            }
            draggingScrollbar = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            int thumbHeight = logModel.thumbHeight(ReforgerLayout.LOG_HEIGHT, ReforgerLayout.LOG_HEIGHT);
            double relativeTop = mouseY - topPos - ReforgerLayout.LOG_TOP - scrollbarGrabOffset;
            scrollOffset = logModel.scrollFromThumb(
                    relativeTop,
                    ReforgerLayout.LOG_HEIGHT,
                    thumbHeight,
                    ReforgerLayout.LOG_HEIGHT);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateLogModel(ReforgePreviewPayload preview) {
        if (preview == renderedPreview || (preview != null && preview.equals(renderedPreview))) {
            return;
        }
        renderedPreview = preview;
        logModel = ReforgerLogModel.from(preview == null ? emptyPreview() : preview);
        scrollOffset = logModel.clampScroll(scrollOffset, ReforgerLayout.LOG_HEIGHT);
    }

    private int scrollbarThumbTop(int thumbHeight) {
        int maximum = Math.max(0, logModel.contentHeight() - ReforgerLayout.LOG_HEIGHT);
        int travel = Math.max(0, ReforgerLayout.LOG_HEIGHT - thumbHeight);
        return maximum == 0 ? 0 : (int) Math.round((double) scrollOffset / maximum * travel);
    }

    private static String percent(double probability) {
        return String.format(Locale.ROOT, "%.1f%%", probability * 100.0);
    }

    private static ReforgePreviewPayload emptyPreview() {
        return new ReforgePreviewPayload(0, 0L, -1, 0, 0, false, List.of());
    }
}
