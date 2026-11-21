package com.ziemniak.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ziemniak.client.shader.ShaderManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Pool;

/** Wlasne shadery rysujemy zaraz po swiecie (przed czyszczeniem glebi i reka) - wtedy bufor glebi jest jeszcze pelny. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow @Final private MinecraftClient client;
	@Shadow @Final private Pool pool;

	@Inject(method = "renderWorld", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
			shift = At.Shift.AFTER))
	private void ziemniak$ownShader(RenderTickCounter tickCounter, CallbackInfo ci) {
		ShaderManager.renderOwn(client, pool);
	}
}
