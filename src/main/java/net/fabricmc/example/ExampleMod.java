package net.fabricmc.example;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.Map;

public class ExampleMod implements ClientModInitializer {
    private float targetHp = 24.0f;
    private String lastTargetName = "НЕТ";
    private int comboCrits = 0;
    private long lastHitTime = 0;

    // БАЗА ДАННЫХ СФЕР
    private static final Map<String, Float> HP_BASES = new HashMap<>();
    static {
        HP_BASES.put("хаос", 16.0f);    // 20 - 4
        HP_BASES.put("арес", 18.0f);    // 20 - 2
        HP_BASES.put("каратель", 16.0f); // 20 - 4
        HP_BASES.put("ярость", 16.0f);  // 20 - 4
        HP_BASES.put("гидра", 24.0f);   // 20 + 4
        HP_BASES.put("бестия", 24.0f);  // 20 + 4
        HP_BASES.put("круша", 24.0f);   // 20 + 4
        HP_BASES.put("пал", 32.0f);     // 20 + 12
        HP_BASES.put("чарка", 44.0f);   // 20 + 12 + 12
        HP_BASES.put("дефолт", 20.0f);
    }

    @Override
    public void onInitializeClient() {
        // КОМАНДА: /hp <тип/число> <ник>
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hp")
                .then(ClientCommandManager.argument("preset", StringArgumentType.string())
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                .executes(context -> {
                    String preset = StringArgumentType.getString(context, "preset").toLowerCase();
                    this.lastTargetName = StringArgumentType.getString(context, "name");
                    
                    // Если ввели число - ставим число, если слово - берем из базы
                    try {
                        this.targetHp = Float.parseFloat(preset);
                    } catch (NumberFormatException e) {
                        this.targetHp = HP_BASES.getOrDefault(preset, 24.0f);
                    }
                    
                    this.comboCrits = 0;
                    context.getSource().sendFeedback(Text.of("§aТрекинг §6" + lastTargetName + " §aначат с §e" + targetHp + " HP"));
                    return 1;
                }))));
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && entity instanceof LivingEntity target) {
                long now = System.currentTimeMillis();
                if (now - lastHitTime > 8000) comboCrits = 0;
                
                lastHitTime = now;
                lastTargetName = target.getName().getString();
                comboCrits++;

                // РАСЧЕТ УРОНА (Z5 80%)
                float myBonus = getOffhandDmg(player.getStackInHand(Hand.OFF_HAND).getName().getString().toLowerCase());
                if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
                    myBonus += (player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1) * 3.0f;
                }
                
                targetHp -= (8.0f + myBonus) * 0.2f; // 80% поглощения
                if (targetHp < 0) targetHp = 0;
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) return;

            int x = 10; int y = 40;
            drawContext.drawTextWithShadow(client.textRenderer, "§6§l[FT] Z5 SYSTEM", x, y, 0xFFFFFF);
            
            if (System.currentTimeMillis() - lastHitTime < 15000) {
                drawContext.drawTextWithShadow(client.textRenderer, "§fЦЕЛЬ: §d" + lastTargetName, x, y + 12, 0xFFFFFF);
                drawContext.drawTextWithShadow(client.textRenderer, "§fHP: §a" + String.format("%.1f", targetHp), x, y + 24, 0xFFFFFF);
                drawContext.drawTextWithShadow(client.textRenderer, "§fСЕРИЯ: §e" + comboCrits + " / 13", x, y + 36, 0xFFFFFF);
            }
        });
    }

    private float getOffhandDmg(String name) {
        if (name.contains("каратель")) return 7;
        if (name.contains("арес")) return 6;
        if (name.contains("ярости")) return 5;
        if (name.contains("хаос") || name.contains("круша")) return 3;
        return 0;
    }
}
