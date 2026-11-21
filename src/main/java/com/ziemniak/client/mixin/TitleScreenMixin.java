package com.ziemniak.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ziemniak.client.theme.FemboyTheme;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;

/** Ekran glowny: tlo motywu nad panorama. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	protected TitleScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V",
			shift = At.Shift.AFTER))
	private void ziemniak$titleBackground(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (FemboyTheme.cfg().themeTitleScreen) FemboyTheme.drawBackground(ctx, this, width, height);
	}
}
