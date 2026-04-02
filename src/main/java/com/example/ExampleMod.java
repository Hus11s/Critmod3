 package net.fabricmc.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.text.Text;

// ВАЖНО: Имя класса должно быть ExampleMod, если файл называется ExampleMod.java
public class ExampleMod implements ClientModInitializer {
    public static double targetHp = 20.0;
    public static boolean holyMode = false;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hp")
                .then(ClientCommandManager.literal("pal").executes(c -> {
                    targetHp = 32.0;
                    c.getSource().getClient().player.sendMessage(Text.literal("§dTarget: 32 HP (Paladin)"), false);
                    return 1;
                }))
                .then(ClientCommandManager.literal("holy").executes(c -> {
                    holyMode = !holyMode;
                    c.getSource().getClient().player.sendMessage(Text.literal("§bHoly Mode: " + holyMode), false);
                    return 1;
                })));
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) return;

            // Считаем урон (база + сферы + острота 7)
            double damage = client.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) + 3.5;
            double finalDmg = (damage * 1.5) * 0.15; // Усредненный срез незеритки Z5

            int crits = (int) Math.ceil((targetHp + (holyMode ? 15 : 0)) / finalDmg);

            int x = drawContext.getScaledWindowWidth() / 2;
            int y = drawContext.getScaledWindowHeight() / 2 + 10;
            int color = holyMode ? 0x00FFFF : (targetHp >= 32 ? 0xFF69B4 : 0xFFFFFF);

            drawContext.drawCenteredTextWithShadow(client.textRenderer, crits + " CRITS", x, y, color);
        });
    }
}
