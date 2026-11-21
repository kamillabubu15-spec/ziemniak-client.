package com.ziemniak.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ziemniak.client.theme.FemboyTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin extends ClickableWidget {
	public PressableWidgetMixin(int x, int y, int width, int height, Text message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void ziemniak$pastelButton(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!FemboyTheme.isPause(MinecraftClient.getInstance().currentScreen)) return;
		FemboyTheme.renderButton(ctx, this, this.alpha, this.isSelected(), this.active);
		ci.cancel();
	}
}
