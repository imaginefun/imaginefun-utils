package net.imaginefun.advancements;

import java.util.List;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class ExpandedAdvancementsScreen extends AdvancementsScreen {

    private static final Identifier WINDOW_TEXTURE = Identifier.withDefaultNamespace("textures/gui/advancements/window.png");
    private static final Component EMPTY_LABEL = Component.translatable("advancements.empty");
    private static final Component SAD_LABEL = Component.translatable("advancements.sad_label");

    private static final int WINDOW_TEXTURE_SIZE = 256;
    private static final int FRAME_SIDE = 15;
    private static final int FRAME_TOP = 23;
    private static final int FRAME_BOTTOM = 15;
    private static final int FRAME_MIDDLE_WIDTH = WINDOW_WIDTH - FRAME_SIDE * 2;
    private static final int FRAME_MIDDLE_HEIGHT = WINDOW_HEIGHT - FRAME_TOP - FRAME_BOTTOM;
    private static final int VIEWPORT_OFFSET_X = 9;
    private static final int VIEWPORT_OFFSET_Y = 18;
    private static final int TITLE_OFFSET_X = 8;
    private static final int TITLE_OFFSET_Y = 6;
    private static final int TITLE_COLOR = 0xFF404040;
    private static final int EMPTY_BACKGROUND_COLOR = 0xFF000000;
    private static final int EMPTY_LABEL_SPACING = 16;

    private static final int EDGE_MARGIN = 8;
    private static final int TAB_OVERHANG = 28;
    private static final int VANILLA_TAB_SPACING = 4;
    private static final int TAB_SPACING = 2;
    private static final int TOP_MARGIN = 32;
    private static final int FOOTER_MARGIN = HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT;
    private static final double SCROLL_SPEED = 16;

    private int windowWidth = WINDOW_WIDTH;
    private int windowHeight = WINDOW_HEIGHT;

    public ExpandedAdvancementsScreen(ClientAdvancements advancements, @Nullable Screen lastScreen) {
        super(advancements, lastScreen);
    }

    public static ExpandedAdvancementsScreen replacing(AdvancementsScreen screen) {
        return new ExpandedAdvancementsScreen(screen.advancements, screen.lastScreen);
    }

    public int viewportWidth() {
        return windowWidth - (WINDOW_WIDTH - WINDOW_INSIDE_WIDTH);
    }

    public int viewportHeight() {
        return windowHeight - (WINDOW_HEIGHT - WINDOW_INSIDE_HEIGHT);
    }

    public int tabOriginX(AdvancementTab tab) {
        return switch (tab.getType()) {
            case ABOVE, BELOW -> leftPos - tab.getIndex() * (VANILLA_TAB_SPACING - TAB_SPACING);
            case LEFT -> leftPos;
            case RIGHT -> leftPos + windowWidth - WINDOW_WIDTH;
        };
    }

    public int tabOriginY(AdvancementTab tab) {
        return tab.getType() == AdvancementTabType.BELOW ? topPos + windowHeight - WINDOW_HEIGHT : topPos;
    }

    @Override
    protected void init() {
        super.init();
        List.copyOf(children()).stream()
            .filter(StringWidget.class::isInstance)
            .forEach(this::removeWidget);
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        layoutWindow();
    }

    private void layoutWindow() {
        int aboveTabs = AdvancementTabType.ABOVE.getMax();
        int aboveAndBelowTabs = aboveTabs + AdvancementTabType.BELOW.getMax();
        int sideMargin = tabs.size() > aboveAndBelowTabs ? TAB_OVERHANG : EDGE_MARGIN;
        int bottomMargin = tabs.size() > aboveTabs ? FOOTER_MARGIN + TAB_OVERHANG : FOOTER_MARGIN;

        windowWidth = Math.max(WINDOW_WIDTH, width - sideMargin * 2);
        windowHeight = Math.max(WINDOW_HEIGHT, height - TOP_MARGIN - bottomMargin);
        leftPos = (width - windowWidth) / 2;
        topPos = TOP_MARGIN;
    }

    @Override
    public void onAddAdvancementRoot(AdvancementNode root) {
        AdvancementTab vanillaTab = AdvancementTab.create(minecraft, this, tabs.size(), root);
        if (vanillaTab == null) {
            return;
        }
        tabs.put(root.holder(), new ExpandedAdvancementTab(minecraft, this, vanillaTab.getType(), vanillaTab.getIndex(), root, vanillaTab.getDisplay()));
        layoutWindow();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!(selectedTab instanceof ExpandedAdvancementTab tab)) {
            return false;
        }
        tab.scroll(scrollX * SCROLL_SPEED, 0);
        tab.zoomAt(x - leftPos - VIEWPORT_OFFSET_X, y - topPos - VIEWPORT_OFFSET_Y, scrollY);
        return true;
    }

    @Override
    public void extractWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (selectedTab == null) {
            extractEmptyViewport(graphics);
        }
        extractFrame(graphics);
        if (tabs.size() > 1) {
            for (AdvancementTab tab : tabs.values()) {
                tab.extractTab(graphics, tabOriginX(tab), tabOriginY(tab), mouseX, mouseY, tab == selectedTab);
            }
            for (AdvancementTab tab : tabs.values()) {
                tab.extractIcon(graphics, tabOriginX(tab), tabOriginY(tab));
            }
        }
        Component windowTitle = selectedTab != null ? selectedTab.getTitle() : title;
        graphics.text(font, windowTitle, leftPos + TITLE_OFFSET_X, topPos + TITLE_OFFSET_Y, TITLE_COLOR, false);
    }

    private void extractEmptyViewport(GuiGraphicsExtractor graphics) {
        int left = leftPos + VIEWPORT_OFFSET_X;
        int top = topPos + VIEWPORT_OFFSET_Y;
        graphics.fill(left, top, left + viewportWidth(), top + viewportHeight(), EMPTY_BACKGROUND_COLOR);

        int centerX = left + viewportWidth() / 2;
        int centerY = top + viewportHeight() / 2;
        graphics.centeredText(font, EMPTY_LABEL, centerX, centerY - font.lineHeight / 2, -1);
        graphics.centeredText(font, SAD_LABEL, centerX, centerY + EMPTY_LABEL_SPACING, -1);
    }

    private void extractFrame(GuiGraphicsExtractor graphics) {
        int right = leftPos + windowWidth;
        int bottom = topPos + windowHeight;
        int innerLeft = leftPos + FRAME_SIDE;
        int innerRight = right - FRAME_SIDE;
        int innerTop = topPos + FRAME_TOP;
        int innerBottom = bottom - FRAME_BOTTOM;
        int rightU = WINDOW_WIDTH - FRAME_SIDE;
        int bottomV = WINDOW_HEIGHT - FRAME_BOTTOM;

        blitWindow(graphics, leftPos, topPos, 0, 0, FRAME_SIDE, FRAME_TOP);
        blitWindow(graphics, innerRight, topPos, rightU, 0, FRAME_SIDE, FRAME_TOP);
        blitWindow(graphics, leftPos, innerBottom, 0, bottomV, FRAME_SIDE, FRAME_BOTTOM);
        blitWindow(graphics, innerRight, innerBottom, rightU, bottomV, FRAME_SIDE, FRAME_BOTTOM);

        for (int x = innerLeft; x < innerRight; x += FRAME_MIDDLE_WIDTH) {
            int sliceWidth = Math.min(FRAME_MIDDLE_WIDTH, innerRight - x);
            blitWindow(graphics, x, topPos, FRAME_SIDE, 0, sliceWidth, FRAME_TOP);
            blitWindow(graphics, x, innerBottom, FRAME_SIDE, bottomV, sliceWidth, FRAME_BOTTOM);
        }
        for (int y = innerTop; y < innerBottom; y += FRAME_MIDDLE_HEIGHT) {
            int sliceHeight = Math.min(FRAME_MIDDLE_HEIGHT, innerBottom - y);
            blitWindow(graphics, leftPos, y, 0, FRAME_TOP, FRAME_SIDE, sliceHeight);
            blitWindow(graphics, innerRight, y, rightU, FRAME_TOP, FRAME_SIDE, sliceHeight);
        }
    }

    private void blitWindow(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int sliceWidth, int sliceHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, x, y, u, v, sliceWidth, sliceHeight, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);
    }
}
