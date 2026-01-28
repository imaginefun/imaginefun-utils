package net.imaginefun.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ImagineFunButton extends Button {

    private static final WidgetSprites SPRITES = new WidgetSprites(
        Identifier.withDefaultNamespace("widget/button"),
        Identifier.withDefaultNamespace("widget/button_disabled"),
        Identifier.withDefaultNamespace("widget/button_highlighted")
    );

    private static final int COLOR_FROM = 0xFFee609c;
    private static final int COLOR_TO = 0xFF4f88de;

    public ImagineFunButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.alpha <= 0.0f) {
            return;
        }
        Identifier sprite = SPRITES.get(this.active, this.isHovered());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        String text = this.getMessage().getString();

        int textWidth = font.width(text);
        int textHeight = font.lineHeight;

        float startX = this.getX() + (this.width - textWidth) / 2.0f;
        float startY = this.getY() + (this.height - textHeight) / 2.0f;

        float currentX = startX;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String charStr = String.valueOf(c);
            int charWidth = font.width(charStr);

            float gradientPos = (currentX - startX + charWidth / 2.0f) / textWidth;
            int color = interpolateColor(COLOR_FROM, COLOR_TO, gradientPos);

            graphics.drawString(font, charStr, (int) currentX, (int) startY, color, true);

            currentX += charWidth;
        }
    }

    private int interpolateColor(int from, int to, float t) {
        int fromR = (from >> 16) & 0xFF;
        int fromG = (from >> 8) & 0xFF;
        int fromB = from & 0xFF;

        int toR = (to >> 16) & 0xFF;
        int toG = (to >> 8) & 0xFF;
        int toB = to & 0xFF;

        int r = (int) (fromR + (toR - fromR) * t);
        int g = (int) (fromG + (toG - fromG) * t);
        int b = (int) (fromB + (toB - fromB) * t);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
