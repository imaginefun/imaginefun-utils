package net.imaginefun.mixins.client;

import net.imaginefun.advancements.ExpandedAdvancementsScreen;
import net.imaginefun.stats.ServerStats;
import net.imaginefun.stats.ServerStatsScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public class GuiMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen imaginefunutils$replaceVanillaScreen(Screen screen) {
        if (screen == null) {
            return null;
        }
        if (screen.getClass() == AdvancementsScreen.class) {
            return ExpandedAdvancementsScreen.replacing((AdvancementsScreen) screen);
        }
        if (screen.getClass() == StatsScreen.class && ServerStats.hasSnapshot()) {
            return new ServerStatsScreen(((StatsScreen) screen).lastScreen);
        }
        return screen;
    }
}
