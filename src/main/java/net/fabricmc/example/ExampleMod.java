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
    private float maxTargetHp = 24.0f;
    private String lastTargetName = "НЕТ";
    private int comboCrits = 0;
    private long lastHitTime = 0;
    private long lastRegenTick = 0;

    private static final Map<String, Float> HP_BASES = new HashMap<>();
    static {
        putHP("круша", 24.0f); putHP("гидра", 24.0f); putHP("бестия", 24.0f);
        putHP("икар", 22.0f); putHP("раздор", 22.0f); putHP("вихрь", 22.0f);
        putHP("демон", 22.0f); putHP("мрак", 21.5f); putHP("арес", 18.0f);
        putHP("хаос", 16.0f); putHP("ярость", 16.0f); putHP("каратель", 16.0f);
        putHP("пал", 32.0f); putHP("чарка", 44.0f); putHP("дефолт", 20.0f);
    }

    private static void putHP(String key, float val) {
        HP_BASES.put(key, val);
        HP_BASES.put(key + "а", val);
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hp")
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                    .then(ClientCommandManager.argument("value", StringArgumentType.string())
                        .executes(c -> {
                            this.lastTargetName = StringArgumentType.getString(c, "name");
                            setTargetData(StringArgumentType.getString(c, "value").toLowerCase());
                            return 1;
                        })))
                .then(ClientCommandManager.argument("preset", StringArgumentType.string())
                    .executes(c -> {
                        setTargetData(StringArgumentType.getString(c, "preset").toLowerCase());
                        return 1;
                    })));
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && entity instanceof LivingEntity target) {
                long now = System.currentTimeMillis();
                String name = target.getName().getString();
                if (name.equals("Player") || name.isEmpty()) name = "Инвизник #" + target.getId();

                if (!name.equals(lastTargetName) && (now - lastHitTime > 15000)) {
                    lastTargetName = name;
                    this.targetHp = 24.0f;
                    this.maxTargetHp = 24.0f;
                    this.comboCrits = 0;
                }

                lastHitTime = now;
                comboCrits++;

                // СЧИТАЕМ ТВОЙ УРОН
                float myBonus = 0;
                var offStack = player.getStackInHand(Hand.OFF_HAND);
                if (offStack != null && !offStack.isEmpty()) {
                    myBonus = getOffhandDmg(offStack.getName().getString().toLowerCase());
                }
                
                // ПОДДЕРЖКА СИЛЫ (ЛЮБОЙ УРОВЕНЬ: 1, 2, 3, 4, 5)
                if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
                    var s = player.getStatusEffect(StatusEffects.STRENGTH);
                    if (s != null) {
                        // Уровень эффекта начинается с 0 (Сила 1 = 0, Сила 5 = 4)
                        myBonus += (s.getAmplifier() + 1) * 3.0f; 
                    }
                }

                // ВЫЧИТАЕМ: (База 8 + Бонус) * Крит 1.5 * Срез Z5 0.15
                float finalDamage = (8.0f + myBonus) * 1.5f * 0.15f;
                this.targetHp -= finalDamage;
                if (this.targetHp < 0) this.targetHp = 0;
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.options.hudHidden) return;

            long now = System.currentTimeMillis();
            if (now - lastRegenTick > 1000) {
                if (targetHp < maxTargetHp) {
                    // Учитываем, что на Чарках/Силе 5 враги тоже жестко регенятся
                    float regenAmount = (now - lastHitTime > 3000) ? 1.8f : 0.4f; 
                    targetHp += regenAmount;
                    if (targetHp > maxTargetHp) targetHp = maxTargetHp;
                }
                lastRegenTick = now;
            }

            if (now - lastHitTime < 15000) {
                int x = 10; int y = 60;
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal("§6§lFT Z5 TRACKER"), x, y, 0xFFFFFF);
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal("§fЦЕЛЬ: §d" + lastTargetName), x, y + 12, 0xFFFFFF);
                int color = targetHp < 8 ? 0xFF5555 : 0x55FF55;
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal("§fHP: "), x, y + 24, 0xFFFFFF);
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal(String.format("%.1f", targetHp)), x + 25, y + 24, color);
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal("§fСЕРИЯ: §e" + comboCrits), x, y + 36, 0xFFFFFF);
            }
        });
    }

    private void setTargetData(String input) {
        try { this.targetHp = Float.parseFloat(input); } 
        catch (Exception e) { this.targetHp = HP_BASES.getOrDefault(input, 24.0f); }
        this.maxTargetHp = this.targetHp;
        this.comboCrits = 0;
    }

    private float getOffhandDmg(String n) {
        if (n.contains("каратель")) return 7.0f;
        if (n.contains("арес")) return 6.0f;
        if (n.contains("ярость")) return 5.0f;
        if (n.contains("раздор")) return 4.0f;
        if (n.contains("круша") || n.contains("хаос")) return 3.0f;
        if (n.contains("тиран") || n.contains("сатир") || n.contains("икар")) return 2.0f;
        if (n.contains("мрак")) return 1.5f;
        return 0;
    }
}
