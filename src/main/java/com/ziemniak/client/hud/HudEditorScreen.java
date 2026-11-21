package com.ziemniak.client.hud;

import com.ziemniak.client.config.ZiemniakConfig;
import com.ziemniak.client.config.ZiemniakConfig.HudPos;
import com.ziemniak.client.hud.HudManager.Module;
import com.ziemniak.client.sound.Sounds;
import com.ziemniak.client.theme.FemboyTheme;
import com.ziemniak.client.theme.Palettes.Colors;
import com.ziemniak.client.theme.Shapes;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Przeciaganie modulow HUD myszka. */
public class HudEditorScreen extends Screen {
	private final Screen parent;
	private Module dragging;
	private double offX, offY;

	public HudEditorScreen(Screen parent) {
		super(Text.literal("Edytor HUD"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe ♡"), b -> close())
				.dimensions(width / 2 - 104, height / 2 + 10, 100, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Reset pozycji"), b -> HudManager.resetPositions())
				.dimensions(width / 2 + 4, height / 2 + 10, 100, 20).build());
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		ctx.fill(0, 0, width, height, 0x40000000);
		Colors c = FemboyTheme.pal();
		for (int x = 0; x < width; x += 20) ctx.fill(x, 0, x + 1, height, FemboyTheme.argb(0.08f, c.light()));
		for (int y = 0; y < height; y += 20) ctx.fill(0, y, width, y + 1, FemboyTheme.argb(0.08f, c.light()));
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		Colors c = FemboyTheme.pal();
		float t = FemboyTheme.time();
		for (Module m : HudManager.MODULES) {
			HudPos p = HudManager.pos(m);
			if (!p.on) continue;
			HudManager.drawModule(ctx, client, m, true);
			int[] r = HudManager.rect(m, client);
			boolean hover = m == dragging || (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]);
			float pulse = (float) (Math.sin(t * 5.0) + 1.0) / 2f;
			Shapes.outlineRounded(ctx, r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1,
					FemboyTheme.argb(hover ? 0.7f + 0.3f * pulse : 0.45f, hover ? 0xFFFFFF : c.main()));
		}
		String hint = "Przeciagaj moduly myszka. Wlaczasz je w menu Ziemniak Mod -> HUD.";
		FemboyTheme.drawWaveText(ctx, textRenderer, "✿ Edytor HUD ✿", width / 2, height / 2 - 30, t, 1f);
		ctx.drawCenteredTextWithShadow(textRenderer, hint, width / 2, height / 2 - 14, 0xFFFFFF);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		for (int i = HudManager.MODULES.size() - 1; i >= 0; i--) {
			Module m = HudManager.MODULES.get(i);
			if (!HudManager.pos(m).on) continue;
			int[] r = HudManager.rect(m, client);
			if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
				dragging = m;
				Sounds.play(Sounds.Sfx.HUD_PICK);
				offX = mouseX - r[0];
				offY = mouseY - r[1];
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging == null) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		int[] r = HudManager.rect(dragging, client);
		double nx = mouseX - offX, ny = mouseY - offY;
		// przyciaganie do krawedzi
		if (nx < 4) nx = 0;
		if (ny < 4) ny = 0;
		if (nx + r[2] > width - 4) nx = width - r[2];
		if (ny + r[3] > height - 4) ny = height - r[3];
		HudPos p = HudManager.pos(dragging);
		p.x = (float) (nx / width);
		p.y = (float) (ny / height);
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging != null) {
			dragging = null;
			Sounds.play(Sounds.Sfx.HUD_DROP);
			ZiemniakConfig.save();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void close() {
		ZiemniakConfig.save();
		client.setScreen(parent);
	}
}
