package net.notcoded.antiiframes;

import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.notcoded.antiiframes.config.AntiIFramesConfig;
import net.notcoded.antiiframes.interfaces.EntityHurtCallback;
import net.notcoded.antiiframes.interfaces.EntityKnockbackCallback;
import net.notcoded.antiiframes.interfaces.PlayerAttackCallback;

public class Main implements ModInitializer {

    public static AntiIFramesConfig CONFIG;

    @Override
    public void onInitialize() {
        System.out.println("AntiIFrames v0.2 for Minecraft 1.16 Fabric Edition is starting.");
        AntiIFramesConfig.preInit();
        registerHandlers();
        System.out.println("AntiIFrames: Loading completed. This mod is powered by FabricMC and SnakeYAML");
    }

    /**
     * Registers all the handlers in LivingEntityMixin.
     */
    private void registerHandlers() {
        // Core module
        EntityHurtCallback.EVENT.register((entity, source, amount) -> {
            if (entity.getEntityWorld().isClient) {
                return ActionResult.PASS;
            }
            if (AntiIFramesConfig.debugMode && entity instanceof PlayerEntity) {
                String debugSource;
                Entity trueSource = source.getAttacker();
                if (trueSource == null || EntityType.getId(trueSource.getType()) == null) {
                    debugSource = "null";
                } else {
                    debugSource = EntityType.getId(trueSource.getType()).toString();
                }
                String message = String.format("Type of damage received: %s\nAmount: %.3f\nTrue Source (mob id): %s\n",
                        source.getName(), amount, debugSource);
                ((PlayerEntity) entity).sendMessage(new LiteralText(message), false);

            }
            if (AntiIFramesConfig.excludePlayers && entity instanceof PlayerEntity) {
                return ActionResult.PASS;
            }
            if (AntiIFramesConfig.excludeAllMobs && !(entity instanceof PlayerEntity)) {
                return ActionResult.PASS;
            }
            for (String id : AntiIFramesConfig.dmgReceiveExcludedEntities) {
                Identifier loc = EntityType.getId(entity.getType());
                if (loc == null)
                    break;
                int starIndex = id.indexOf('*');
                if (starIndex != -1) {
                    if (loc.toString().contains(id.substring(0, starIndex))) {
                        return ActionResult.PASS;
                    }
                } else if (loc.toString().equals(id)) {
                    return ActionResult.PASS;
                }
            }

            // May have more DoTs missing in this list
            // Not anymore (/s)
            // Damage SOURCES that needs to be put in check
            for (String dmgType : AntiIFramesConfig.damageSrcWhitelist) {
                if (source.getName().equals(dmgType)) {
                    return ActionResult.PASS;
                }
            }

            // THINGS, MOBS that needs to apply i-frames when attacking
            for (String id : AntiIFramesConfig.attackExcludedEntities) {
                Entity attacker = source.getAttacker();
                if (attacker == null)
                    break;
                Identifier loc = EntityType.getId(attacker.getType());
                if (loc == null)
                    break;
                int starIndex = id.indexOf('*');
                if (starIndex != -1) {
                    if (loc.toString().contains(id.substring(0, starIndex))) {
                        return ActionResult.PASS;
                    }
                } else if (loc.toString().equals(id)) {
                    return ActionResult.PASS;
                }

            }

            entity.timeUntilRegen = AntiIFramesConfig.iFrameInterval;
            return ActionResult.PASS;
        });

        // Knockback module part 1
        EntityKnockbackCallback.EVENT.register((entity, source, amp, dx, dz) -> {
            if (entity.getEntityWorld().isClient) {
                return ActionResult.PASS;
            }
            if (source != null) {
                // IT'S ONLY MAGIC
                if (source instanceof PlayerEntity && ((PlayerEntity) source).hurtTime == -1) {
                    ((PlayerEntity) source).hurtTime = 0;
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        // Attack + knockback cancel module
        PlayerAttackCallback.EVENT.register((player, target) -> {
            if (player.getEntityWorld().isClient) {
                return ActionResult.PASS;
            }

            if (AntiIFramesConfig.debugMode) {
                String message = String.format("Entity attacked: %s",
                        EntityType.getId(target.getType()));
                player.sendMessage(new LiteralText(message),false);
            }

            float str = player.getAttackCooldownProgress(0);
            if (str <= AntiIFramesConfig.attackCancelThreshold) {
                return ActionResult.FAIL;
            }
            if (str <= AntiIFramesConfig.knockbackCancelThreshold) {
                // Don't worry, it's only magic
                player.hurtTime = -1;
            }

            return ActionResult.PASS;

        });
    }

}