package net.imaginefun.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.imaginefun.config.ImaginefunUtilsConfig;
import net.imaginefun.gui.ImagineFunButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addImagineFunButton(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int x = this.width / 2 - 100;
        int y = this.height / 4 + 48 + 24;

        for (Renderable renderable : ((ScreenAccessor) this).imaginefunutils$getRenderables()) {
            if (renderable instanceof AbstractWidget widget) {
                if (widget.getY() >= y) {
                    widget.setY(widget.getY() + 24);
                }
            }
        }

        ImagineFunButton joinButton = new ImagineFunButton(
            x, y, buttonWidth, buttonHeight,
            Component.literal("Join " + ImaginefunUtilsConfig.serverName),
            button -> {
                ServerData serverData = new ServerData(
                    ImaginefunUtilsConfig.serverName,
                    ImaginefunUtilsConfig.serverAddress,
                    ServerData.Type.OTHER
                );
                serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                ServerAddress serverAddress = ServerAddress.parseString(ImaginefunUtilsConfig.serverAddress);
                ConnectScreen.startConnecting((TitleScreen) (Object) this, minecraft, serverAddress, serverData, false, null);
            }
        );

        this.addRenderableWidget(joinButton);
    }
}
