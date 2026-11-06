package net.imaginefun.advancements;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementTabType;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class ExpandedAdvancementTab extends AdvancementTab {

    private static final int TILE_SIZE = AdvancementsScreen.BACKGROUND_TILE_WIDTH;
    private static final int ZOOM_LEVELS_PER_DOUBLING = 4;
    private static final int MAX_ZOOM_LEVEL = ZOOM_LEVELS_PER_DOUBLING;
    private static final int WIDGET_CENTER_X = 16;
    private static final int WIDGET_CENTER_Y = 13;
    private static final float HOVER_FADE_IN_STEP = 0.06f;
    private static final float HOVER_FADE_OUT_STEP = 0.12f;
    private static final float MAX_HOVER_FADE = 0.3f;

    private final ExpandedAdvancementsScreen screen;
    private double zoomLevel;

    public ExpandedAdvancementTab(
        Minecraft minecraft,
        ExpandedAdvancementsScreen screen,
        AdvancementTabType type,
        int index,
        AdvancementNode rootNode,
        DisplayInfo display
    ) {
        super(minecraft, screen, type, index, rootNode, display);
        this.screen = screen;
    }

    @Override
    public boolean isMouseOver(int xo, int yo, double mouseX, double mouseY) {
        return super.isMouseOver(screen.tabOriginX(this), screen.tabOriginY(this), mouseX, mouseY);
    }

    @Override
    public void tick(int viewportMouseX, int viewportMouseY) {
        AdvancementWidget widget = widgetAt(viewportMouseX, viewportMouseY);
        if (widget != null) {
            hovered = widget;
            fade = Mth.clamp(fade + HOVER_FADE_IN_STEP, 0.0f, MAX_HOVER_FADE);
        } else {
            hovered = null;
            fade = Mth.clamp(fade - HOVER_FADE_OUT_STEP, 0.0f, 1.0f);
        }
    }

    private @Nullable AdvancementWidget widgetAt(int viewportMouseX, int viewportMouseY) {
        boolean insideViewport = viewportMouseX > 0 && viewportMouseX < screen.viewportWidth()
            && viewportMouseY > 0 && viewportMouseY < screen.viewportHeight();
        if (!insideViewport) {
            return null;
        }
        int treeMouseX = Mth.floor(viewportMouseX / zoom());
        int treeMouseY = Mth.floor(viewportMouseY / zoom());
        for (AdvancementWidget widget : widgets.values()) {
            if (widget.isMouseOver(Mth.floor(scrollX), Mth.floor(scrollY), treeMouseX, treeMouseY)) {
                return widget;
            }
        }
        return null;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int viewportLeft, int viewportTop) {
        if (!centered) {
            scrollX = visibleTreeWidth() / 2 - (maxX + minX) / 2.0;
            scrollY = visibleTreeHeight() / 2 - (maxY + minY) / 2.0;
            centered = true;
        }
        clampScroll();

        int intScrollX = Mth.floor(scrollX);
        int intScrollY = Mth.floor(scrollY);

        graphics.enableScissor(viewportLeft, viewportTop, viewportLeft + screen.viewportWidth(), viewportTop + screen.viewportHeight());
        graphics.pose().pushMatrix();
        graphics.pose().translate(viewportLeft, viewportTop);
        extractBackground(graphics);
        graphics.pose().scale(zoom());

        root.extractConnectivity(graphics, intScrollX, intScrollY, true);
        root.extractConnectivity(graphics, intScrollX, intScrollY, false);
        root.extractRenderState(graphics, intScrollX, intScrollY);

        graphics.pose().popMatrix();
        graphics.disableScissor();
    }

    private void extractBackground(GuiGraphicsExtractor graphics) {
        Identifier background = getDisplay().getBackground()
            .map(ClientAsset.ResourceTexture::texturePath)
            .orElse(TextureManager.INTENTIONAL_MISSING_TEXTURE);
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(background);
        GpuSampler repeatingSampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);

        double tileSize = TILE_SIZE * Math.max(zoom(), 1.0f);
        double tileOriginX = scrollX * zoom();
        double tileOriginY = scrollY * zoom();
        int width = screen.viewportWidth();
        int height = screen.viewportHeight();

        graphics.blit(
            texture.getTextureView(), repeatingSampler, 0, 0, width, height,
            (float) (-tileOriginX / tileSize), (float) ((width - tileOriginX) / tileSize),
            (float) (-tileOriginY / tileSize), (float) ((height - tileOriginY) / tileSize)
        );
    }

    @Override
    public void extractTooltips(GuiGraphicsExtractor graphics, int windowLeft, int windowTop) {
        graphics.fill(0, 0, screen.viewportWidth(), screen.viewportHeight(), Mth.floor(fade * 255.0f) << 24);
        if (hovered != null) {
            hovered.extractHover(graphics, hoverOriginX(hovered), hoverOriginY(hovered), fade, windowLeft, windowTop);
        }
    }

    private int hoverOriginX(AdvancementWidget widget) {
        return Mth.floor((scrollX + widget.getX() + WIDGET_CENTER_X) * zoom()) - WIDGET_CENTER_X - widget.getX();
    }

    private int hoverOriginY(AdvancementWidget widget) {
        return Mth.floor((scrollY + widget.getY() + WIDGET_CENTER_Y) * zoom()) - WIDGET_CENTER_Y - widget.getY();
    }

    @Override
    public void scroll(double x, double y) {
        scrollX += x / zoom();
        scrollY += y / zoom();
        clampScroll();
    }

    public void zoomAt(double viewportX, double viewportY, double levels) {
        float zoomBefore = zoom();
        zoomLevel = Mth.clamp(zoomLevel + levels, -MAX_ZOOM_LEVEL, MAX_ZOOM_LEVEL);
        float zoomAfter = zoom();
        scrollX += viewportX / zoomAfter - viewportX / zoomBefore;
        scrollY += viewportY / zoomAfter - viewportY / zoomBefore;
        clampScroll();
    }

    @Override
    public boolean canScrollHorizontally() {
        return maxX - minX > visibleTreeWidth();
    }

    @Override
    public boolean canScrollVertically() {
        return maxY - minY > visibleTreeHeight();
    }

    private void clampScroll() {
        scrollX = clampedScroll(scrollX, minX, maxX, visibleTreeWidth());
        scrollY = clampedScroll(scrollY, minY, maxY, visibleTreeHeight());
    }

    private static double clampedScroll(double scroll, int treeMin, int treeMax, double visibleSize) {
        if (treeMax - treeMin <= visibleSize) {
            return visibleSize / 2 - (treeMax + treeMin) / 2.0;
        }
        return Mth.clamp(scroll, visibleSize - treeMax, -treeMin);
    }

    private double visibleTreeWidth() {
        return screen.viewportWidth() / (double) zoom();
    }

    private double visibleTreeHeight() {
        return screen.viewportHeight() / (double) zoom();
    }

    private float zoom() {
        return (float) Math.pow(2, zoomLevel / ZOOM_LEVELS_PER_DOUBLING);
    }
}
