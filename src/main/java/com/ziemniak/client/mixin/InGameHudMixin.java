package com.ziemniak.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ziemniak.client.config.Options;
import com.ziemniak.client.config.ZiemniakConfig;
import com.ziemniak.client.theme.FemboyTheme;
import com.ziemniak.client.theme.Palettes.Colors;
import com.ziemniak.client.theme.Shapes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;

/** Wlasne celowniki w kolorze motywu. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void ziemniak$crosshair(DrawContext ctx, RenderTickCounter tc, CallbackInfo ci) {
		Options.Crosshair style = ZiemniakConfig.get().crosshair;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (style == Options.Crosshair.VANILLA || mc.getDebugHud().shouldShowDebugHud()) return;
		ci.cancel();
		if (!mc.options.getPerspective().isFirstPerson()) return;
		Colors c = FemboyTheme.pal();
		float cx = ctx.getScaledWindowWidth() / 2f, cy = ctx.getScaledWindowHeight() / 2f;
		int main = FemboyTheme.argb(1f, c.main()), white = FemboyTheme.argb(1f, 0xFFFFFF), dark = FemboyTheme.argb(0.8f, c.dark());
		int x = Math.round(cx), y = Math.round(cy);
		switch (style) {
			case SERDUSZKO -> {
				Shapes.draw(ctx, Shapes.HEART, cx + 0.5f, cy + 0.5f, 1.25f, dark);
				Shapes.draw(ctx, Shapes.HEART, cx, cy, 1f, main);
			}
			case PLUS -> {
				ctx.fill(x - 5, y - 1, x + 6, y + 2, dark);
				ctx.fill(x - 1, y - 5, x + 2, y + 6, dark);
				ctx.fill(x - 4, y, x + 5, y + 1, main);
				ctx.fill(x, y - 4, x + 1, y + 5, main);
			}
			case KROPKA -> {
				ctx.fill(x - 2, y - 2, x + 3, y + 3, dark);
				ctx.fill(x - 1, y - 1, x + 2, y + 2, main);
			}
			case KOLKO -> {
				Shapes.ring(ctx, cx + 0.5f, cy + 0.5f, 4.5f, 20, 0.6f, main);
				ctx.fill(x, y, x + 1, y + 1, white);
			}
			case GWIAZDKA -> {
				Shapes.draw(ctx, Shapes.STAR, cx + 0.5f, cy + 0.5f, 1.2f, dark);
				Shapes.draw(ctx, Shapes.STAR, cx, cy, 1f, main);
			}
			case KRZYZYK -> {
				for (int i = -3; i <= 3; i++) {
					ctx.fill(x + i, y + i, x + i + 1, y + i + 1, main);
					ctx.fill(x + i, y - i, x + i + 1, y - i + 1, main);
				}
			}
			default -> { }
		}
	}
}
