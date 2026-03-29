package net.imaginefun.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class ImagineFunButton extends Button {

    private static final int COLOR_FROM = 0xFFee609c;
    private static final int COLOR_TO = 0xFF4f88de;

    public ImagineFunButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);

        Font font = Minecraft.getInstance().font;
        String text = this.getMessage().getString();
        int textWidth = font.width(text);

        MutableComponent gradientText = Component.empty();
        float currentWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String charStr = String.valueOf(c);
            int charWidth = font.width(charStr);

            float gradientPos = textWidth > 0 ? (currentWidth + charWidth / 2.0f) / textWidth : 0;
            int color = interpolateColor(COLOR_FROM, COLOR_TO, gradientPos);

            gradientText.append(Component.literal(charStr).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF))));
            currentWidth += charWidth;
        }

        ActiveTextCollector textCollector = graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE);
        this.extractScrollingStringOverContents(textCollector, gradientText, 2);
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
