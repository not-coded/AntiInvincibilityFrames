package net.notcoded.antiiframes.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.notcoded.antiiframes.interfaces.EntityHurtCallback;
import net.notcoded.antiiframes.interfaces.EntityKnockbackCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.ActionResult;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Shadow @NotNull private LivingEntity attacker;

    @Inject(at = @At("TAIL"), method = "applyDamage", cancellable = true)
    private void onEntityHurt(DamageSource source, float amount, CallbackInfo info) {
        ActionResult result = EntityHurtCallback.EVENT.invoker().hurtEntity((LivingEntity) (Object) this, source, amount);
        if (result == ActionResult.FAIL) {
            info.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "takeKnockback", cancellable = true)
    private void onTakingKnockback(float f, double d, double e, CallbackInfo ci) {
        ActionResult result = EntityKnockbackCallback.EVENT.invoker().takeKnockback((LivingEntity) (Object) this, attacker, f, d, e);
        if (result == ActionResult.FAIL) {
            ci.cancel();
        }

    }
}