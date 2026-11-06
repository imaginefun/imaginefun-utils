package net.imaginefun.mixins.client;

import net.imaginefun.advancements.ExpandedAdvancementsScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public class GuiMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen imaginefunutils$expandAdvancementsScreen(Screen screen) {
        if (screen != null && screen.getClass() == AdvancementsScreen.class) {
            return ExpandedAdvancementsScreen.replacing((AdvancementsScreen) screen);
        }
        return screen;
    }
}
