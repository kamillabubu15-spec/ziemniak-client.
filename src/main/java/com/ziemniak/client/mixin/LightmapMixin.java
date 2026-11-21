package com.ziemniak.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.ziemniak.client.config.ZiemniakConfig;

import net.minecraft.client.render.LightmapTextureManager;

/** Fullbright: podbija gamme tylko przy liczeniu oswietlenia (opcje gry zostaja nietkniete). */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapMixin {
	@ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
	private float ziemniak$fullbright(float gamma) {
		return ZiemniakConfig.get().fullbright ? 15f : gamma;
	}
}
