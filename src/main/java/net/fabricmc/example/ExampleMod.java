package net.fabricmc.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class ExampleMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (MinecraftClient.getInstance().player != null) {
                drawContext.drawCenteredTextWithShadow(
                    MinecraftClient.getInstance().textRenderer, 
                    "TEST OK", 
                    drawContext.getScaledWindowWidth() / 2, 
                    10, 
                    0xFFFFFF
                );
            }
        });
    }
}
