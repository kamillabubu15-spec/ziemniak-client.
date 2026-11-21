package com.ziemniak.client.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ziemniak.client.theme.FemboyTheme;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
	@Shadow protected int x;
	@Shadow protected int y;
	@Shadow protected int backgroundWidth;
	@Shadow protected int backgroundHeight;
	@Shadow @Nullable protected Slot focusedSlot;

	protected HandledScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "renderBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V"))
	private void ziemniak$background(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!FemboyTheme.isInventory(this)) return;
		FemboyTheme.drawBackground(ctx, this, width, height);
		FemboyTheme.beginPopIn(ctx, this, width, height);
	}

	@Inject(method = "renderBackground", at = @At("TAIL"))
	private void ziemniak$panel(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (FemboyTheme.isInventory(this)) FemboyTheme.drawPanel(ctx, x, y, backgroundWidth, backgroundHeight);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void ziemniak$slotHover(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!FemboyTheme.isInventory(this)) return;
		if (focusedSlot != null && focusedSlot.isEnabled()) {
			FemboyTheme.drawSlotHover(ctx, x + focusedSlot.x, y + focusedSlot.y);
		}
		FemboyTheme.endPopIn(ctx);
	}
}
