package net.fabricmc.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ExampleMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && !client.options.hudHidden) {
                drawContext.drawCenteredTextWithShadow(client.textRenderer, 
                    "TEST OK", 
                    drawContext.getScaledWindowWidth() / 2, 
                    10, 
                    0xFFFFFF);
            }
        });
    }
}
