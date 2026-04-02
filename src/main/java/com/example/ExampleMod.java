package net.fabricmc.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.text.Text;

public class ExampleMod implements ClientModInitializer {
    public static double targetHp = 20.0;
    public static boolean holyMode = false;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hp")
                .then(ClientCommandManager.literal("pal").executes(c -> setHp(c.getSource().getClient(), 32.0)))
                .then(ClientCommandManager.literal("holy").executes(c -> {
                    holyMode = !holyMode;
                    c.getSource().getClient().player.sendMessage(Text.literal("Holy: " + holyMode), false);
                    return 1;
                })));
        });
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private int setHp(MinecraftClient client, double hp) {
        targetHp = hp;
        client.player.sendMessage(Text.literal("HP set to " + hp), false);
        return 1;
    }

    private void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        double damage = client.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) + 3.5;
        double finalDmg = (damage * 1.5) * 0.15; // Усредненный Z5
        int crits = (int) Math.ceil((targetHp + (holyMode ? 15 : 0)) / finalDmg);
        drawContext.drawCenteredTextWithShadow(client.textRenderer, crits + " CRITS", 
            drawContext.getScaledWindowWidth() / 2, drawContext.getScaledWindowHeight() / 2 + 10, 0xFFFFFF);
    }
} 
