package com.ziemniak.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ziemniak.client.sound.Sounds;
import com.ziemniak.client.theme.FemboyTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;

/** Na ekranach z motywem przyciski klikaja dzwiekiem moda zamiast zwyklego "klik". */
@Mixin(ClickableWidget.class)
public abstract class ClickableWidgetMixin {
	@Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
	private void ziemniak$clickSound(SoundManager soundManager, CallbackInfo ci) {
		if (!FemboyTheme.stylesButtons(MinecraftClient.getInstance().currentScreen) || !Sounds.enabled(Sounds.Cat.UI)) return;
		ci.cancel();
		ClickableWidget self = (ClickableWidget) (Object) this;
		if (!Sounds.isSilent(self)) Sounds.play(Sounds.Sfx.CLICK);
	}
}
