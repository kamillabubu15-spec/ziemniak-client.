package com.ziemniak.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ziemniak.client.hud.HudManager;
import com.ziemniak.client.util.Zoom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
public abstract class MouseMixin {
	@Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
	private void ziemniak$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (MinecraftClient.getInstance().currentScreen == null && vertical != 0 && Zoom.onScroll(vertical)) ci.cancel();
	}

	@Inject(method = "onMouseButton", at = @At("HEAD"))
	private void ziemniak$cps(long window, int button, int action, int mods, CallbackInfo ci) {
		if (action == 1 && MinecraftClient.getInstance().currentScreen == null) HudManager.onClick(button);
	}
}
