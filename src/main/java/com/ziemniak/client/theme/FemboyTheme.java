package com.ziemniak.client.theme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;

/**
 * Pastelowy "femboy" motyw: rozowo-lawendowe tlo, unoszace sie serduszka,
 * blyszczace iskierki, zaokraglone przyciski z animacja najechania,
 * wjezdzajace przyciski w menu ESC i "wyskakujacy" ekwipunek.
 */
public final class FemboyTheme {
	public static boolean enabled = true;

	// Kolory (RGB, bez alfy)
	public static final int PINK = 0xFF8AD8;
	public static final int PINK_LIGHT = 0xFFC2EC;
	public static final int PINK_DARK = 0xC2449A;
	public static final int LAVENDER = 0xC9A7FF;
	public static final int BABY_BLUE = 0xA8E0FF;
	public static final int WHITE = 0xFFFFFF;

	private static final String[] HEART = {
			".XX.XX.",
			"XXXXXXX",
			"XXXXXXX",
			".XXXXX.",
			"..XXX..",
			"...X..."
	};
	private static final String[] BOW = {
			"XX.....XX",
			"XXX...XXX",
			"XXXXXXXXX",
			"XXX...XXX",
			"XX.....XX"
	};

	private static final Map<Screen, Long> OPENED = new WeakHashMap<>();
	private static final Map<ClickableWidget, HoverState> HOVER = new WeakHashMap<>();
	private static final List<Burst> BURSTS = new ArrayList<>();
	private static boolean popPushed = false;

	private record Burst(double x, double y, long start) {}

	private static final class HoverState {
		float progress;
		long last = now();
	}

	/** Mnoznik predkosci animacji (np. ZIEMNIAK_TIME_SCALE=0.2 do nagrywania w zwolnionym tempie). */
	private static final double TIME_SCALE = parseScale();

	private FemboyTheme() {}

	private static double parseScale() {
		String v = System.getProperty("ziemniak.timeScale", System.getenv("ZIEMNIAK_TIME_SCALE"));
		try {
			return v == null ? 1.0 : Double.parseDouble(v);
		} catch (NumberFormatException e) {
			return 1.0;
		}
	}

	/** Zegar animacji w milisekundach. */
	public static long now() {
		return (long) (Util.getMeasuringTimeMs() * TIME_SCALE);
	}

	// ------------------------------------------------------------ ktore ekrany

	public static boolean isPause(Screen s) {
		return enabled && s instanceof GameMenuScreen g && g.shouldShowMenu();
	}

	public static boolean isInventory(Screen s) {
		return enabled && s instanceof HandledScreen<?>;
	}

	public static boolean isThemed(Screen s) {
		return isPause(s) || isInventory(s);
	}

	// ------------------------------------------------------------ czas / easing

	public static void markOpened(Screen s) {
		OPENED.putIfAbsent(s, now());
	}

	/** Sekundy od otwarcia ekranu. */
	public static float sinceOpen(Screen s) {
		Long t = OPENED.get(s);
		return t == null ? 10f : (now() - t) / 1000f;
	}

	/** Globalny zegar animacji w sekundach. */
	public static float time() {
		return (now() % 3_600_000L) / 1000f;
	}

	public static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	public static float easeOutCubic(float t) {
		float u = 1f - clamp01(t);
		return 1f - u * u * u;
	}

	public static float easeOutBack(float t) {
		t = clamp01(t);
		float c1 = 1.70158f, c3 = c1 + 1f;
		float u = t - 1f;
		return 1f + c3 * u * u * u + c1 * u * u;
	}

	private static float hash(int i) {
		int h = i * 0x9E3779B9;
		h ^= h >>> 15;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		return (h & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	// ------------------------------------------------------------ kolory

	public static int argb(float alpha, int rgb) {
		int a = Math.round(clamp01(alpha) * 255f);
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	public static int lerpRgb(float t, int a, int b) {
		t = clamp01(t);
		int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
		return (r << 16) | (g << 8) | bl;
	}

	// ------------------------------------------------------------ ksztalty

	private static void drawPixels(DrawContext ctx, String[] shape, float x, float y, float scale, int color) {
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(x, y, 0f);
		m.scale(scale, scale, 1f);
		for (int row = 0; row < shape.length; row++) {
			String line = shape[row];
			int start = -1;
			for (int col = 0; col <= line.length(); col++) {
				boolean on = col < line.length() && line.charAt(col) == 'X';
				if (on && start < 0) start = col;
				if (!on && start >= 0) {
					ctx.fill(start, row, col, row + 1, color);
					start = -1;
				}
			}
		}
		m.pop();
	}

	/** Serduszko 7x6 pikseli; (x, y) = srodek. */
	public static void drawHeart(DrawContext ctx, float x, float y, float scale, int color) {
		drawPixels(ctx, HEART, x - 3.5f * scale, y - 3f * scale, scale, color);
	}

	/** Kokardka 9x5 pikseli; (x, y) = srodek. */
	public static void drawBow(DrawContext ctx, float x, float y, float scale, int color) {
		drawPixels(ctx, BOW, x - 4.5f * scale, y - 2.5f * scale, scale, color);
	}

	/** Iskierka w ksztalcie plusa. */
	public static void drawSparkle(DrawContext ctx, float x, float y, float size, int color) {
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(x, y, 0f);
		m.scale(size, size, 1f);
		ctx.fill(-2, 0, 3, 1, color);
		ctx.fill(0, -2, 1, 3, color);
		m.pop();
	}

	public static void fillRounded(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
		ctx.fill(x1 + 1, y1, x2 - 1, y2, color);
		ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
		ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
	}

	public static void outlineRounded(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
		ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
		ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
		ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
		ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
	}

	// ------------------------------------------------------------ tlo

	/** Rozowo-lawendowy gradient + unoszace sie serduszka + iskierki. */
	public static void drawBackground(DrawContext ctx, Screen screen, int w, int h) {
		float open = easeOutCubic(sinceOpen(screen) / 0.35f);
		float t = time();
		float shift = (float) (Math.sin(t * 0.8) + 1.0) / 2f;
		int top = lerpRgb(shift, 0xFF9BDB, 0xD6B4FF);
		int bottom = lerpRgb(shift, 0xC9A7FF, 0xFFB8E1);
		float baseAlpha = isPause(screen) ? 0.62f : 0.5f;
		ctx.fillGradient(0, 0, w, h, argb(baseAlpha * open, top), argb(baseAlpha * open, bottom));

		// Serduszka plynace do gory
		int hearts = 22;
		for (int i = 0; i < hearts; i++) {
			float speed = 14f + hash(i * 3 + 1) * 22f;
			float scale = 0.9f + hash(i * 3 + 2) * 1.6f;
			float travel = h + 30f;
			float yy = h + 15f - ((t * speed + hash(i * 7) * travel) % travel);
			float xx = hash(i * 5 + 11) * w + (float) Math.sin(t * 1.3f + i) * 10f;
			float fadeTop = clamp01(yy / (h * 0.45f));
			int color = (i % 3 == 0) ? WHITE : (i % 3 == 1 ? PINK_LIGHT : LAVENDER);
			drawHeart(ctx, xx, yy, scale, argb(0.75f * fadeTop * open, color));
		}

		// Migajace iskierki
		int sparkles = 26;
		for (int i = 0; i < sparkles; i++) {
			float tw = (float) Math.sin(t * (2.0 + hash(i + 100) * 3.0) + i * 1.7);
			float a = tw > 0 ? tw * tw * tw : 0f;
			if (a < 0.03f) continue;
			float xx = hash(i * 13 + 5) * w;
			float yy = hash(i * 17 + 9) * h;
			drawSparkle(ctx, xx, yy, 0.8f + a * 0.7f, argb(a * 0.95f * open, i % 2 == 0 ? WHITE : BABY_BLUE));
		}
	}

	// ------------------------------------------------------------ ekwipunek

	/** Wywolywane po narysowaniu tla - skaluje panel ekwipunku przy otwarciu ("pop"). */
	public static void beginPopIn(DrawContext ctx, Screen screen, int w, int h) {
		float p = sinceOpen(screen) / 0.32f;
		if (p >= 1f) return;
		float s = 0.75f + 0.25f * easeOutBack(p);
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(w / 2f, h / 2f + (1f - easeOutCubic(p)) * 14f, 0f);
		m.scale(s, s, 1f);
		m.translate(-w / 2f, -h / 2f, 0f);
		popPushed = true;
	}

	public static void endPopIn(DrawContext ctx) {
		if (popPushed) {
			ctx.getMatrices().pop();
			popPushed = false;
		}
	}

	/** Rozowa poswiata wokol panelu, rozowy odcien, kokardka i serduszka w rogach. */
	public static void drawPanel(DrawContext ctx, int x, int y, int bw, int bh) {
		float t = time();
		float pulse = (float) (Math.sin(t * 3.0) + 1.0) / 2f;

		// rozowy odcien na teksturze panelu
		ctx.fill(x, y, x + bw, y + bh, argb(0.3f, PINK));

		// pulsujaca poswiata (3 warstwy)
		for (int i = 1; i <= 3; i++) {
			float a = (0.55f - i * 0.14f) * (0.6f + 0.4f * pulse);
			outlineRounded(ctx, x - i, y - i, x + bw + i, y + bh + i, argb(a, i == 1 ? WHITE : PINK));
		}

		// kokardka na gorze, delikatnie podskakuje
		float bob = (float) Math.sin(t * 4.0) * 1.5f;
		drawBow(ctx, x + bw / 2f, y - 1f + bob, 2f, argb(1f, PINK_DARK));
		drawBow(ctx, x + bw / 2f, y - 1.5f + bob, 1.6f, argb(1f, PINK));

		// serduszka w rogach - pulsuja na zmiane
		float[][] corners = {{x, y}, {x + bw, y}, {x, y + bh}, {x + bw, y + bh}};
		for (int i = 0; i < 4; i++) {
			float beat = (float) Math.abs(Math.sin(t * 2.5 + i * 0.8));
			drawHeart(ctx, corners[i][0], corners[i][1], 1.2f + beat * 0.6f, argb(1f, i % 2 == 0 ? PINK : LAVENDER));
		}
	}

	/** Animowana rozowa ramka + serduszko nad slotem pod myszka. */
	public static void drawSlotHover(DrawContext ctx, int sx, int sy) {
		float t = time();
		float pulse = (float) (Math.sin(t * 8.0) + 1.0) / 2f;
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(0f, 0f, 300f);
		ctx.fill(sx, sy, sx + 16, sy + 16, argb(0.25f + 0.15f * pulse, PINK));
		outlineRounded(ctx, sx - 1, sy - 1, sx + 17, sy + 17, argb(0.9f, PINK));
		outlineRounded(ctx, sx - 2, sy - 2, sx + 18, sy + 18, argb(0.35f + 0.4f * pulse, WHITE));
		drawHeart(ctx, sx + 15f, sy + 1f - pulse * 2f, 0.8f + pulse * 0.3f, argb(1f, PINK_DARK));
		m.pop();
	}

	// ------------------------------------------------------------ menu ESC

	/** Przyciski wjezdzaja na zmiane z lewej i prawej z odbiciem. */
	public static void animateButtons(Screen screen, List<ClickableWidget> buttons, int[] origX) {
		float since = sinceOpen(screen);
		for (int i = 0; i < buttons.size(); i++) {
			ClickableWidget b = buttons.get(i);
			float p = (since - 0.06f - i * 0.045f) / 0.42f;
			if (p >= 1f) {
				b.setX(origX[i]);
				b.setAlpha(1f);
				continue;
			}
			float e = easeOutBack(p);
			int dir = (i % 2 == 0) ? -1 : 1;
			b.setX(origX[i] + Math.round((1f - e) * 70f * dir));
			b.setAlpha(clamp01(p * 1.8f));
		}
	}

	/** Zaokraglony pastelowy przycisk z animacja najechania. */
	public static void renderButton(DrawContext ctx, ClickableWidget w, float alpha, boolean hovered, boolean active) {
		MinecraftClient mc = MinecraftClient.getInstance();
		TextRenderer tr = mc.textRenderer;

		HoverState st = HOVER.computeIfAbsent(w, k -> new HoverState());
		long now = now();
		float dt = Math.min(0.1f, (now - st.last) / 1000f);
		st.last = now;
		float target = (hovered && active) ? 1f : 0f;
		st.progress += (target - st.progress) * clamp01(dt * 14f);
		float hp = st.progress;

		int x1 = w.getX(), y1 = w.getY(), x2 = x1 + w.getWidth(), y2 = y1 + w.getHeight();
		float cx = (x1 + x2) / 2f, cy = (y1 + y2) / 2f;
		float t = time();

		MatrixStack m = ctx.getMatrices();
		m.push();
		float s = 1f + 0.05f * hp + (hp > 0.5f ? (float) Math.sin(t * 6.0) * 0.008f : 0f);
		m.translate(cx, cy, 0f);
		m.scale(s, s, 1f);
		m.translate(-cx, -cy, 0f);

		// poswiata przy najechaniu
		if (hp > 0.02f) {
			fillRounded(ctx, x1 - 3, y1 - 3, x2 + 3, y2 + 3, argb(alpha * 0.22f * hp, PINK));
			fillRounded(ctx, x1 - 2, y1 - 2, x2 + 2, y2 + 2, argb(alpha * 0.35f * hp, PINK_LIGHT));
		}

		int top, bottom, border;
		if (!active) {
			top = 0xE6CFE0;
			bottom = 0xCDB3C7;
			border = 0xA98AA0;
		} else {
			top = lerpRgb(hp, 0xFFB3E6, 0xFFD3F2);
			bottom = lerpRgb(hp, 0xF27CC6, 0xFF8FD6);
			border = lerpRgb(hp, 0xB33F8D, 0xFFFFFF);
		}
		// cien pod spodem
		fillRounded(ctx, x1, y1 + 1, x2, y2 + 1, argb(alpha * 0.45f, 0x7A2A63));
		// cialo przycisku
		ctx.fillGradient(x1 + 1, y1 + 1, x2 - 1, y2 - 1, argb(alpha, top), argb(alpha, bottom));
		outlineRounded(ctx, x1, y1, x2, y2, argb(alpha, border));
		// polysk u gory
		ctx.fill(x1 + 2, y1 + 1, x2 - 2, y1 + 2, argb(alpha * 0.7f, WHITE));

		// przesuwajacy sie blask przy najechaniu
		if (hp > 0.02f) {
			int bw = x2 - x1;
			int sx = x1 + (int) (((t * 0.9f) % 1f) * (bw + 30)) - 15;
			for (int k = 0; k < 6; k++) {
				int px = sx + k;
				if (px > x1 + 1 && px < x2 - 1) {
					ctx.fill(px, y1 + 2, px + 1, y2 - 2, argb(alpha * 0.25f * hp, WHITE));
				}
			}
		}

		// tekst z rozowym cieniem
		String msg = w.getMessage().getString();
		int tw = tr.getWidth(msg);
		int tx = Math.round(cx - tw / 2f);
		int ty = Math.round(cy - 4f);
		int textA = Math.max(4, Math.round(alpha * 255f));
		ctx.drawText(tr, msg, tx + 1, ty + 1, (textA << 24) | (active ? 0x9E2F78 : 0x7D6577), false);
		ctx.drawText(tr, msg, tx, ty, (textA << 24) | (active ? WHITE : 0xF3E6EF), false);

		// serduszka wjezdzajace po bokach napisu
		if (hp > 0.02f) {
			float off = 6f + (1f - hp) * 10f;
			float beat = 1f + (float) Math.abs(Math.sin(t * 5.0)) * 0.25f;
			drawHeart(ctx, tx - off, cy, beat, argb(alpha * hp, WHITE));
			drawHeart(ctx, tx + tw + off, cy, beat, argb(alpha * hp, WHITE));
		}
		m.pop();
	}

	/** Rzeczy rysowane na samym wierzchu: tytul, napis falujacy, wybuchy serduszek po kliknieciu. */
	public static void drawOverlay(DrawContext ctx, Screen screen, int mouseX, int mouseY) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float t = time();
		float open = easeOutCubic(sinceOpen(screen) / 0.5f);
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(0f, 0f, 450f);

		if (isPause(screen)) {
			int cx = screen.width / 2;
			int tw = tr.getWidth(screen.getTitle());
			for (int side = -1; side <= 1; side += 2) {
				float bob = (float) Math.sin(t * 3.0 + side) * 2f;
				float beat = 1.3f + (float) Math.abs(Math.sin(t * 2.6)) * 0.4f;
				drawHeart(ctx, cx + side * (tw / 2f + 12f), 44f + bob, beat, argb(open, PINK));
				drawSparkle(ctx, cx + side * (tw / 2f + 24f), 38f - bob, 0.9f, argb(open, WHITE));
			}
			drawWaveText(ctx, tr, "♡ ziemniak client ♡", cx, screen.height - 18, t, open);
		}

		// cursor: male serduszko kolo kursora
		float cb = (float) Math.sin(t * 5.0) * 1.5f;
		drawHeart(ctx, mouseX + 9f, mouseY + 11f + cb, 0.9f, argb(0.9f * open, PINK));

		// wybuchy serduszek
		long now = now();
		Iterator<Burst> it = BURSTS.iterator();
		while (it.hasNext()) {
			Burst b = it.next();
			float age = (now - b.start) / 1000f;
			if (age > 0.7f) {
				it.remove();
				continue;
			}
			float p = age / 0.7f;
			float dist = easeOutCubic(p) * 26f;
			for (int k = 0; k < 8; k++) {
				double ang = k * Math.PI / 4.0 + (b.start % 100) / 50.0;
				float hx = (float) (b.x + Math.cos(ang) * dist);
				float hy = (float) (b.y + Math.sin(ang) * dist) - p * 6f;
				int color = k % 2 == 0 ? PINK : (k % 4 == 1 ? LAVENDER : WHITE);
				drawHeart(ctx, hx, hy, 1.1f * (1f - p * 0.5f), argb(1f - p, color));
			}
		}
		m.pop();
	}

	public static void drawWaveText(DrawContext ctx, TextRenderer tr, String text, int cx, int y, float t, float alpha) {
		int total = tr.getWidth(text);
		float x = cx - total / 2f;
		int a = Math.max(4, Math.round(clamp01(alpha) * 255f));
		for (int i = 0; i < text.length(); i++) {
			String ch = String.valueOf(text.charAt(i));
			float yy = y + (float) Math.sin(t * 4.0 + i * 0.45) * 2.5f;
			int col = lerpRgb((float) (Math.sin(t * 2.0 + i * 0.3) + 1.0) / 2f, PINK, LAVENDER);
			ctx.drawText(tr, ch, Math.round(x) + 1, Math.round(yy) + 1, (a << 24) | 0x7A2A63, false);
			ctx.drawText(tr, ch, Math.round(x), Math.round(yy), (a << 24) | col, false);
			x += tr.getWidth(ch);
		}
	}

	public static void addBurst(double x, double y) {
		BURSTS.add(new Burst(x, y, now()));
		if (BURSTS.size() > 20) BURSTS.remove(0);
	}
}
