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
    private float targetHp = 20.0f;
    private float maxTargetHp = 20.0f; // Для ограничения регена
    private String lastTargetName = "НЕТ";
    private int comboCrits = 0;
    private long lastHitTime = 0;
    private long lastRegenTick = 0;

    private static final Map<String, Float> HP_BASES = new HashMap<>();
    static {
        // Значения: База (20) + Доп. сердца сферы
        HP_BASES.put("хаос", 16.0f);    
        HP_BASES.put("арес", 18.0f);    
        HP_BASES.put("пал", 32.0f);     // 20 + 12
        HP_BASES.put("чарка", 44.0f);   // 20 + 24 (если с чаркой)
        HP_BASES.put("дефолт", 20.0f);
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hp")
                .then(ClientCommandManager.argument("preset", StringArgumentType.string())
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                .executes(context -> {
                    String preset = StringArgumentType.getString(context, "preset").toLowerCase();
                    this.lastTargetName = StringArgumentType.getString(context, "name");
                    
                    this.targetHp = HP_BASES.getOrDefault(preset, 20.0f);
                    this.maxTargetHp = this.targetHp; // Запоминаем максимум
                    this.comboCrits = 0;
                    
                    context.getSource().getClient().player.sendMessage(Text.literal("§a[FT] Цель: §6" + lastTargetName + " §e(" + targetHp + " HP)"), false);
                    return 1;
                }))));
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && entity instanceof LivingEntity target) {
                long now = System.currentTimeMillis();
                
                // Сброс комбо (8 сек)
                if (now - lastHitTime > 8000) comboCrits = 0;
                lastHitTime = now;
                comboCrits++;

                // РАСЧЕТ УРОНА (Z5 80% срез)
                float myBonus = getOffhandDmg(player.getStackInHand(Hand.OFF_HAND).getName().getString().toLowerCase());
                
                // Сила (для 1.21.x)
                if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
                    var effect = player.getStatusEffect(StatusEffects.STRENGTH);
                    if (effect != null) myBonus += (effect.getAmplifier() + 1) * 3.0f;
                }

                // Крит множитель 1.5х, срез брони 0.2
                float hitDamage = (8.0f + myBonus) * 1.5f * 0.2f;
                targetHp -= hitDamage;
                
                if (targetHp < 0) targetHp = 0;
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.options.hudHidden) return;

            long now = System.currentTimeMillis();

            // ИМИТАЦИЯ РЕГЕНЕРАЦИИ (Реген 2 восстанавливает ~0.4 HP в сек)
            if (now - lastRegenTick > 1000) { // Раз в секунду
                if (targetHp < maxTargetHp) {
                    targetHp += 0.4f; // Реген 2
                    if (targetHp > maxTargetHp) targetHp = maxTargetHp;
                }
                lastRegenTick = now;
            }

            // Отрисовка HUD
            if (now - lastHitTime < 15000) {
                int x = 10; int y = 50;
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal("§6§l[FT] Z5 TRACKER"), x, y, 0xFFFFFF);
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal("§fЦЕЛЬ: §d" + lastTargetName), x, y + 12, 0xFFFFFF);
                
                // Цвет HP меняется от состояния
                int hpColor = targetHp < 10 ? 0xFF5555 : 0x55FF55;
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal("§fHP: "), x, y + 24, 0xFFFFFF);
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal(String.format("%.1f", targetHp)), x + 25, y + 24, hpColor);
                
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal("§fСЕРИЯ: §e" + comboCrits), x, y + 36, 0xFFFFFF);
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
