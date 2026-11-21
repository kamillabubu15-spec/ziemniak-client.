package com.ziemniak.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ziemniak.client.theme.FemboyTheme;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Shadow public int width;
	@Shadow public int height;

	@Inject(method = "renderBackground", at = @At("TAIL"))
	private void ziemniak$themedBackground(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		Screen self = (Screen) (Object) this;
		if (FemboyTheme.isPause(self) || FemboyTheme.isOtherMenu(self)) FemboyTheme.drawBackground(ctx, self, width, height);
	}
}
