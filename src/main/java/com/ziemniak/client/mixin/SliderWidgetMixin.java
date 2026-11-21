package com.ziemniak.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ziemniak.client.theme.FemboyTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

/** Pastelowe suwaki (FOV, glosnosc itd.) na ekranach z motywem. */
@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin extends ClickableWidget {
	@Shadow protected double value;

	public SliderWidgetMixin(int x, int y, int width, int height, Text message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void ziemniak$pastelSlider(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!FemboyTheme.stylesButtons(MinecraftClient.getInstance().currentScreen)) return;
		FemboyTheme.renderSlider(ctx, this, this.alpha, this.isSelected(), this.active, this.value);
		ci.cancel();
	}
}
