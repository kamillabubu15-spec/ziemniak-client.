package com.ziemniak.client.theme;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.ziemniak.client.config.Options;
import com.ziemniak.client.config.Options.ParticleKind;
import com.ziemniak.client.config.ZiemniakConfig;
import com.ziemniak.client.hud.HudEditorScreen;
import com.ziemniak.client.screen.ZiemniakMenuScreen;
import com.ziemniak.client.sound.Sounds;
import com.ziemniak.client.theme.Palettes.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;

/**
 * Pastelowy "femboy" motyw: tla, czasteczki, przyciski, animacje otwierania,
 * efekty klikniecia i slad kursora. Wszystko sterowane z {@link ZiemniakConfig}.
 */
public final class FemboyTheme {
	public static final int WHITE = 0xFFFFFF;

	private static final ParticleKind[] MIX_KINDS = {
			ParticleKind.SERDUSZKA, ParticleKind.GWIAZDKI, ParticleKind.SAKURA, ParticleKind.BABELKI,
			ParticleKind.MOTYLKI, ParticleKind.SNIEG, ParticleKind.NUTKI, ParticleKind.LAPKI,
			ParticleKind.CHMURKI, ParticleKind.TRUSKAWKI, ParticleKind.DIAMENTY, ParticleKind.KOTKI};

	private static final Map<Screen, Long> OPENED = new WeakHashMap<>();
	private static final Map<ClickableWidget, HoverState> HOVER = new WeakHashMap<>();
	private static final List<Burst> BURSTS = new ArrayList<>();
	private static final ArrayDeque<float[]> TRAIL = new ArrayDeque<>();
	private static boolean popPushed = false;

	/** Mnoznik predkosci (np. ZIEMNIAK_TIME_SCALE=0.2 do nagrywania w zwolnionym tempie). */
	private static final double TIME_SCALE = parseScale();
	private static long realLast = -1;
	private static double clock = 0;

	private record Burst(double x, double y, long start, Options.ClickEffect kind, int seed) {}

	private static final class HoverState {
		float progress;
		boolean was;
		long last = now();
	}

	private FemboyTheme() {}

	public static ZiemniakConfig cfg() {
		return ZiemniakConfig.get();
	}

	// ================================================================ ktore ekrany

	public static boolean isPause(Screen s) {
		return cfg().themeEnabled && s instanceof GameMenuScreen g && g.shouldShowMenu();
	}

	public static boolean isInventory(Screen s) {
		return cfg().themeEnabled && s instanceof HandledScreen<?>;
	}

	/** Pozostale menu gry (opcje, wybor swiata itd.), gdy wlaczony "motyw we wszystkich menu". */
	public static boolean isOtherMenu(Screen s) {
		ZiemniakConfig c = cfg();
		return s != null && c.themeEnabled && c.themeAllMenus && !(s instanceof HandledScreen<?>) && !(s instanceof GameMenuScreen)
				&& !(s instanceof ZiemniakMenuScreen) && !(s instanceof TitleScreen) && !(s instanceof HudEditorScreen)
				&& !(s instanceof ChatScreen);
	}

	public static boolean isTitle(Screen s) {
		return s instanceof TitleScreen && cfg().themeEnabled && cfg().themeTitleScreen;
	}

	public static boolean isThemed(Screen s) {
		return isPause(s) || isInventory(s) || s instanceof ZiemniakMenuScreen || isOtherMenu(s) || isTitle(s);
	}

	/** Ekrany, na ktorych przyciski dostaja pastelowy wyglad. */
	public static boolean stylesButtons(Screen s) {
		return isPause(s) || s instanceof ZiemniakMenuScreen || s instanceof HudEditorScreen || isOtherMenu(s) || isTitle(s);
	}

	// ================================================================ czas / easing

	private static double parseScale() {
		String v = System.getProperty("ziemniak.timeScale", System.getenv("ZIEMNIAK_TIME_SCALE"));
		try {
			return v == null ? 1.0 : Double.parseDouble(v);
		} catch (NumberFormatException e) {
			return 1.0;
		}
	}

	public static double timeScale() {
		return TIME_SCALE;
	}

	/** Zegar animacji w ms (uwzglednia predkosc z ustawien). */
	public static long now() {
		long real = Util.getMeasuringTimeMs();
		if (realLast < 0) realLast = real;
		clock += (real - realLast) * TIME_SCALE * cfg().speed.mul;
		realLast = real;
		return (long) clock;
	}

	public static void markOpened(Screen s) {
		OPENED.putIfAbsent(s, now());
	}

	public static void resetOpened(Screen s) {
		OPENED.put(s, now());
	}

	public static float sinceOpen(Screen s) {
		Long t = OPENED.get(s);
		return t == null ? 10f : (now() - t) / 1000f;
	}

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

	public static float hash(int i) {
		int h = i * 0x9E3779B9;
		h ^= h >>> 15;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		return (h & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	// ================================================================ kolory

	public static Colors pal() {
		return Palettes.of(cfg().palette, time());
	}

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

	private static int pick(Colors c, int i) {
		return switch (Math.floorMod(i, 3)) {
			case 0 -> c.p1();
			case 1 -> c.p2();
			default -> c.p3();
		};
	}

	// ================================================================ tlo

	/** Tlo motywu (gradient/zorza/tecza/gwiazdy/kratka) + czasteczki + iskierki + spadajace gwiazdy. */
	public static void drawBackground(DrawContext ctx, Screen screen, int w, int h) {
		ZiemniakConfig cfg = cfg();
		Colors c = pal();
		float open = easeOutCubic(sinceOpen(screen) / 0.35f);
		float t = time();
		float shift = (float) (Math.sin(t * 0.8) + 1.0) / 2f;
		int top = lerpRgb(shift, c.top1(), c.top2());
		int bottom = lerpRgb(shift, c.bot1(), c.bot2());
		float base = isPause(screen) ? 0.62f : (screen instanceof ZiemniakMenuScreen ? 0.72f : (screen instanceof TitleScreen ? 0.42f : 0.5f));
		if (c.night()) base += 0.18f;

		switch (cfg.background) {
			case GRADIENT -> ctx.fillGradient(0, 0, w, h, argb(base * open, top), argb(base * open, bottom));
			case ZORZA -> {
				ctx.fillGradient(0, 0, w, h, argb((base + 0.1f) * open, lerpRgb(0.55f, top, 0x1A1033)), argb(base * open, bottom));
				drawAurora(ctx, w, h, t, open, c);
			}
			case TECZA -> {
				ctx.fillGradient(0, 0, w, h, argb(base * 0.8f * open, top), argb(base * 0.8f * open, bottom));
				int bands = 7;
				float bh = h / 5f;
				for (int k = 0; k < bands; k++) {
					float y = ((k * bh + t * 18f) % (bands * bh)) - bh;
					int col = Palettes.hsv(k / (float) bands + t * 0.02f, 0.35f, 1f);
					ctx.fillGradient(0, Math.round(y), w, Math.round(y + bh), argb(0.0f, col), argb(0.32f * open, col));
				}
			}
			case GWIAZDY -> {
				ctx.fillGradient(0, 0, w, h, argb(0.82f * open, 0x140B2E), argb(0.78f * open, lerpRgb(0.5f, c.main(), 0x2A1650)));
				int stars = Math.round(70 * cfg.density.mul);
				for (int i = 0; i < stars; i++) {
					float tw = (float) (Math.sin(t * (1.5 + hash(i + 300) * 3.0) + i * 2.1) + 1.0) / 2f;
					float xx = hash(i * 31 + 7) * w, yy = hash(i * 37 + 3) * h;
					if (hash(i + 900) > 0.8f) Shapes.sparkle(ctx, xx, yy, 0.6f + tw * 0.5f, argb(open * (0.3f + 0.7f * tw), i % 3 == 0 ? c.p2() : WHITE));
					else Shapes.dot(ctx, xx, yy, 0.5f + tw * 0.3f, argb(open * (0.25f + 0.75f * tw), WHITE));
				}
			}
			case KRATKA -> {
				ctx.fillGradient(0, 0, w, h, argb(base * open, top), argb(base * open, bottom));
				int cell = 24;
				int off = (int) ((t * 10f) % (cell * 2));
				for (int gx = -2; gx <= w / cell + 2; gx++) {
					for (int gy = -2; gy <= h / cell + 2; gy++) {
						if (((gx + gy) & 1) != 0) continue;
						int x0 = gx * cell + off, y0 = gy * cell + off;
						ctx.fill(x0, y0, x0 + cell, y0 + cell, argb(0.14f * open, WHITE));
					}
				}
			}
			case FALE -> {
				ctx.fillGradient(0, 0, w, h, argb(base * open, top), argb(base * open, bottom));
				int[] cols = {c.p2(), c.main(), c.p3()};
				for (int b = 0; b < 3; b++) {
					for (int x = 0; x < w; x += 3) {
						float yy = h * (0.68f + 0.08f * b) + (float) Math.sin(x * 0.02f + t * (0.8f + 0.3f * b) + b * 2f) * 8f
								+ (float) Math.sin(x * 0.045f - t * 1.1f + b) * 3f;
						ctx.fill(x, Math.round(yy), x + 3, h, argb((0.22f + 0.06f * b) * open, cols[b]));
						ctx.fill(x, Math.round(yy), x + 3, Math.round(yy) + 1, argb(0.5f * open, WHITE));
					}
				}
			}
			case BOKEH -> {
				ctx.fillGradient(0, 0, w, h, argb(base * open, top), argb(base * open, bottom));
				int n = Math.round(14 * cfg.density.mul);
				for (int i = 0; i < n; i++) {
					float r = 10f + hash(i + 400) * 20f;
					float bx = (hash(i * 3 + 401) * (w + 60) + t * (3f + hash(i + 402) * 6f)) % (w + 60) - 30;
					float by = hash(i * 5 + 403) * h + (float) Math.sin(t * 0.5f + i) * 12f;
					float a = 0.1f + 0.1f * (float) (Math.sin(t * 1.2f + i) + 1.0) / 2f;
					Shapes.disc(ctx, bx, by, r, argb(a * open, pick(c, i)));
				}
			}
			case BRAK -> ctx.fillGradient(0, 0, w, h, argb(0.25f * open, top), argb(0.25f * open, bottom));
		}

		if (cfg.bgParticles) drawParticles(ctx, w, h, t, open, cfg.particles, cfg.density.mul, c);
		if (cfg.sparkles) drawSparkles(ctx, w, h, t, open, cfg.density.mul, c);
		if (cfg.shootingStars) drawShootingStars(ctx, w, h, t, open);
	}

	private static void drawAurora(DrawContext ctx, int w, int h, float t, float open, Colors c) {
		int[] cols = {c.p2(), c.main(), 0x9CF0D8};
		for (int b = 0; b < 3; b++) {
			for (int x = 0; x < w; x += 3) {
				float yc = h * (0.22f + 0.14f * b)
						+ (float) Math.sin(x * 0.02f + t * (0.6f + 0.2f * b) + b) * h * 0.08f
						+ (float) Math.sin(x * 0.007f - t * 0.4f) * h * 0.05f;
				float hh = 14f + 10f * (float) Math.sin(x * 0.03f + t + b);
				float a = (0.22f + 0.12f * (float) Math.sin(x * 0.05f + t * 1.3f + b)) * open;
				ctx.fillGradient(x, Math.round(yc - hh), x + 3, Math.round(yc), argb(0f, cols[b]), argb(a, cols[b]));
				ctx.fillGradient(x, Math.round(yc), x + 3, Math.round(yc + hh * 0.4f), argb(a, cols[b]), argb(0f, cols[b]));
			}
		}
	}

	private static void drawSparkles(DrawContext ctx, int w, int h, float t, float open, float mul, Colors c) {
		int n = Math.round(26 * mul);
		for (int i = 0; i < n; i++) {
			float tw = (float) Math.sin(t * (2.0 + hash(i + 100) * 3.0) + i * 1.7);
			float a = tw > 0 ? tw * tw * tw : 0f;
			if (a < 0.03f) continue;
			Shapes.sparkle(ctx, hash(i * 13 + 5) * w, hash(i * 17 + 9) * h, 0.8f + a * 0.7f,
					argb(a * 0.95f * open, i % 2 == 0 ? WHITE : c.p3()));
		}
	}

	private static void drawShootingStars(DrawContext ctx, int w, int h, float t, float open) {
		for (int s = 0; s < 2; s++) {
			float period = 4.5f + s * 2.3f;
			float local = (t + s * 1.7f) % period;
			if (local > 0.9f) continue;
			int id = (int) ((t + s * 1.7f) / period) * 7 + s;
			float p = local / 0.9f;
			float sx = hash(id) * w * 0.8f + w * 0.1f, sy = hash(id + 50) * h * 0.35f;
			float hx = sx + p * 120f, hy = sy + p * 55f;
			for (int k = 0; k < 14; k++) {
				float f = k / 14f;
				Shapes.dot(ctx, hx - f * 34f, hy - f * 15.6f, 1.1f - f * 0.8f, argb(open * (1f - f) * (1f - p * 0.6f), WHITE));
			}
		}
	}

	/** Czasteczki tla wybranego rodzaju. */
	public static void drawParticles(DrawContext ctx, int w, int h, float t, float open, ParticleKind kind, float mul, Colors c) {
		int n = Math.round(22 * mul);
		for (int i = 0; i < n; i++) {
			ParticleKind k = kind == ParticleKind.MIX ? MIX_KINDS[i % MIX_KINDS.length] : kind;
			float hs = hash(i * 3 + 1), hs2 = hash(i * 3 + 2), hx = hash(i * 5 + 11), hy = hash(i * 7);
			int col = pick(c, i);
			switch (k) {
				case SERDUSZKA -> {
					float travel = h + 30f;
					float yy = h + 15f - ((t * (14f + hs * 22f) + hy * travel) % travel);
					float xx = hx * w + (float) Math.sin(t * 1.3f + i) * 10f;
					float fade = clamp01(yy / (h * 0.45f));
					float beat = 1f + (float) Math.abs(Math.sin(t * 3 + i)) * 0.12f;
					Shapes.draw(ctx, Shapes.HEART, xx, yy, (0.9f + hs2 * 1.6f) * beat, argb(0.75f * fade * open, col));
				}
				case GWIAZDKI -> {
					float xx = (hx * (w + 40) + t * (4f + hs * 6f)) % (w + 40) - 20;
					float yy = hy * h + (float) Math.sin(t * 0.8f + i) * 6f;
					float tw = (float) (Math.sin(t * (2 + hs * 3) + i) + 1) / 2f;
					Shapes.draw(ctx, Shapes.STAR, xx, yy, 0.8f + hs2 * 0.9f + tw * 0.3f, argb((0.35f + 0.55f * tw) * open, i % 2 == 0 ? 0xFFF3A8 : col));
				}
				case SAKURA -> {
					float travel = h + 30f;
					float yy = ((t * (12f + hs * 14f) + hy * travel) % travel) - 15f;
					float xx = (hx * (w + 60) + t * 14f + (float) Math.sin(t * 1.6f + i) * 14f) % (w + 60) - 30;
					float flip = (float) Math.cos(t * (2f + hs * 2f) + i);
					float s = 1.1f + hs2 * 1.2f;
					Shapes.draw(ctx, Shapes.PETAL, xx, yy, s * flip, s, argb(0.85f * open, i % 2 == 0 ? 0xFFC2D6 : c.p2()), argb(0.85f * open, 0xFFE6EE));
				}
				case BABELKI -> {
					float travel = h + 30f;
					float yy = h + 15f - ((t * (10f + hs * 18f) + hy * travel) % travel);
					float xx = hx * w + (float) Math.sin(t * 2f + i) * 6f;
					float pop = clamp01((h * 0.15f - yy) / (h * 0.15f));
					float s = (1f + hs2 * 1.4f) * (1f + pop * 0.8f);
					Shapes.draw(ctx, Shapes.BUBBLE, xx, yy, s, s, argb(0.7f * (1f - pop) * open, i % 2 == 0 ? 0xBDEBFF : col), argb(0.9f * (1f - pop) * open, WHITE));
				}
				case MOTYLKI -> {
					float xx = w / 2f + (float) Math.sin(t * (0.25f + hs * 0.2f) + i) * w * 0.46f;
					float yy = h / 2f + (float) Math.sin(t * (0.37f + hs2 * 0.25f) + i * 2f) * h * 0.42f;
					boolean openWings = ((int) (t * 9f + i)) % 2 == 0;
					float s = 1.2f + hs2 * 0.8f;
					Shapes.draw(ctx, openWings ? Shapes.BUTTERFLY_OPEN : Shapes.BUTTERFLY_CLOSED, xx, yy, s, s,
							argb(0.85f * open, i % 2 == 0 ? c.main() : col), argb(0.9f * open, 0x5A2A4A));
				}
				case SNIEG -> {
					float travel = h + 30f;
					float yy = ((t * (8f + hs * 10f) + hy * travel) % travel) - 15f;
					float xx = hx * w + (float) Math.sin(t * 0.9f + i) * 16f;
					float s = 0.7f + hs2 * 1.0f;
					Shapes.draw(ctx, Shapes.SNOW, xx, yy, s, s, argb(0.8f * open, WHITE), argb(0.9f * open, c.p3()));
				}
				case NUTKI -> {
					float travel = h + 30f;
					float yy = h + 15f - ((t * (16f + hs * 16f) + hy * travel) % travel);
					float xx = hx * w + (float) Math.sin(t * 3f + i) * 8f;
					float fade = clamp01(yy / (h * 0.4f));
					float flip = ((int) (t * 2f + i)) % 2 == 0 ? 1f : -1f;
					float s = 1f + hs2;
					Shapes.draw(ctx, Shapes.NOTE, xx, yy, s * flip, s, argb(0.8f * fade * open, col), argb(0.8f * fade * open, col));
				}
				case LAPKI -> {
					if (i % 4 != 0) continue;
					float row = hy * h * 0.9f + h * 0.05f;
					int dir = hs > 0.5f ? 1 : -1;
					int step = (int) (t / 0.35f + hx * 100f);
					for (int s = 0; s < 6; s++) {
						int st = step - s;
						float xx = (st * 14f + hx * w) % (w + 40f) - 20f;
						if (dir < 0) xx = w - xx;
						float yy = row + ((st & 1) == 0 ? -5f : 5f);
						Shapes.draw(ctx, Shapes.PAW, xx, yy, 1.1f, argb(0.7f * (1f - s / 6f) * open, col));
					}
				}
				case CHMURKI -> {
					float s = 1.5f + hs2 * 1.8f;
					float xx = (hx * (w + 80) + t * (5f + hs * 8f)) % (w + 80) - 40;
					float yy = hy * h * 0.8f + (float) Math.sin(t * 0.6f + i) * 4f;
					Shapes.draw(ctx, Shapes.CLOUD, xx, yy, s, argb(0.55f * open, WHITE));
				}
				case TRUSKAWKI -> {
					float travel = h + 30f;
					float yy = ((t * (13f + hs * 12f) + hy * travel) % travel) - 15f;
					float xx = hx * w + (float) Math.sin(t * 1.4f + i) * 10f;
					float flip = (float) Math.cos(t * 1.5f + i);
					float s = 1f + hs2 * 0.9f;
					Shapes.draw(ctx, Shapes.STRAWBERRY, xx, yy, s * (flip >= 0 ? 1f : -1f), s, argb(0.85f * open, 0xFF5A7A), argb(0.9f * open, 0x6FCF6A));
				}
				case DIAMENTY -> {
					float xx = hx * w + (float) Math.sin(t * 0.7f + i) * 8f;
					float yy = hy * h + (float) Math.cos(t * 0.9f + i) * 6f;
					float tw = (float) (Math.sin(t * (2f + hs * 2f) + i * 1.3f) + 1f) / 2f;
					Shapes.draw(ctx, Shapes.DIAMOND, xx, yy, 1f + hs2 * 0.8f, 1f + hs2 * 0.8f, argb((0.45f + 0.45f * tw) * open, i % 2 == 0 ? 0x9CF0FF : c.p3()), argb(open, WHITE));
					if (tw > 0.85f) Shapes.sparkle(ctx, xx + 5f, yy - 5f, 0.8f, argb(open, WHITE));
				}
				case KOTKI -> {
					float travel = h + 40f;
					float yy = h + 20f - ((t * (8f + hs * 10f) + hy * travel) % travel);
					float xx = hx * w + (float) Math.sin(t * 1.1f + i) * 12f;
					boolean blink = ((t * 0.7f + hs * 5f) % 3f) < 0.15f;
					float look = ((int) (t * 0.5f + i)) % 2 == 0 ? 1f : -1f;
					float s = 1f + hs2 * 0.9f;
					int face = i % 2 == 0 ? 0xFFF2F7 : c.p2();
					Shapes.draw(ctx, Shapes.CAT, xx, yy, s * look, s, argb(0.85f * open, face), argb(0.95f * open, blink ? face : 0x3A2A40));
				}
				default -> { }
			}
		}
	}

	// ================================================================ ekwipunek

	/** Animacja otwierania panelu (pop / wjazd / obrot / sprezyna). */
	public static void beginPopIn(DrawContext ctx, Screen screen, int w, int h) {
		Options.InventoryOpen mode = cfg().inventoryOpen;
		if (mode == Options.InventoryOpen.BRAK) return;
		float dur = switch (mode) {
			case POP -> 0.32f;
			case SPREZYNA -> 0.7f;
			default -> 0.45f;
		};
		float p = sinceOpen(screen) / dur;
		if (p >= 1f) return;
		MatrixStack m = ctx.getMatrices();
		m.push();
		popPushed = true;
		m.translate(w / 2f, h / 2f, 0f);
		switch (mode) {
			case POP -> {
				float s = 0.75f + 0.25f * easeOutBack(p);
				m.translate(0f, (1f - easeOutCubic(p)) * 14f, 0f);
				m.scale(s, s, 1f);
			}
			case WJAZD -> m.translate(0f, (1f - easeOutBack(p)) * h * 0.7f, 0f);
			case OBROT -> {
				float e = easeOutBack(p);
				m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((1f - e) * -28f));
				float s = 0.55f + 0.45f * e;
				m.scale(s, s, 1f);
			}
			case SPREZYNA -> {
				float wob = (float) Math.sin(p * 20f) * (1f - p) * 0.28f;
				float s = 0.8f + 0.2f * easeOutCubic(p * 3f);
				m.scale(s * (1f + wob), s * (1f - wob), 1f);
			}
			default -> { }
		}
		m.translate(-w / 2f, -h / 2f, 0f);
	}

	public static void endPopIn(DrawContext ctx) {
		if (popPushed) {
			ctx.getMatrices().pop();
			popPushed = false;
		}
	}

	/** Odcien, poswiata, kokardka i serduszka w rogach panelu. */
	public static void drawPanel(DrawContext ctx, int x, int y, int bw, int bh) {
		ZiemniakConfig cfg = cfg();
		Colors c = pal();
		float t = time();
		float pulse = (float) (Math.sin(t * 3.0) + 1.0) / 2f;

		ctx.fill(x, y, x + bw, y + bh, argb(0.3f, c.main()));

		if (cfg.panelGlow) {
			for (int i = 1; i <= 3; i++) {
				float a = (0.55f - i * 0.14f) * (0.6f + 0.4f * pulse);
				Shapes.outlineRounded(ctx, x - i, y - i, x + bw + i, y + bh + i, argb(a, i == 1 ? WHITE : c.main()));
			}
		}
		if (cfg.panelBow) {
			float bob = (float) Math.sin(t * 4.0) * 1.5f;
			float wig = 1f + (float) Math.sin(t * 6.0) * 0.05f;
			Shapes.draw(ctx, Shapes.BOW, x + bw / 2f, y - 1f + bob, 2f * wig, 2f, argb(1f, c.dark()), argb(1f, c.dark()));
			Shapes.draw(ctx, Shapes.BOW, x + bw / 2f, y - 1.5f + bob, 1.6f * wig, 1.6f, argb(1f, c.main()), argb(1f, c.light()));
		}
		if (cfg.panelCornerHearts) {
			float[][] corners = {{x, y}, {x + bw, y}, {x, y + bh}, {x + bw, y + bh}};
			for (int i = 0; i < 4; i++) {
				float beat = (float) Math.abs(Math.sin(t * 2.5 + i * 0.8));
				Shapes.draw(ctx, Shapes.HEART, corners[i][0], corners[i][1], 1.2f + beat * 0.6f, argb(1f, i % 2 == 0 ? c.main() : c.p3()));
			}
		}
	}

	/** Ramka i serduszko nad slotem pod myszka. */
	public static void drawSlotHover(DrawContext ctx, int sx, int sy) {
		if (!cfg().slotHover) return;
		Colors c = pal();
		float t = time();
		float pulse = (float) (Math.sin(t * 8.0) + 1.0) / 2f;
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(0f, 0f, 300f);
		ctx.fill(sx, sy, sx + 16, sy + 16, argb(0.25f + 0.15f * pulse, c.main()));
		Shapes.outlineRounded(ctx, sx - 1, sy - 1, sx + 17, sy + 17, argb(0.9f, c.main()));
		Shapes.outlineRounded(ctx, sx - 2, sy - 2, sx + 18, sy + 18, argb(0.35f + 0.4f * pulse, WHITE));
		Shapes.draw(ctx, Shapes.HEART, sx + 15f, sy + 1f - pulse * 2f, 0.8f + pulse * 0.3f, argb(1f, c.dark()));
		m.pop();
	}

	// ================================================================ przyciski

	/** Przyciski wjezdzaja na zmiane z lewej i prawej z odbiciem. */
	public static void animateButtons(Screen screen, List<ClickableWidget> buttons, int[] origX) {
		float since = sinceOpen(screen);
		boolean anim = cfg().buttonSlideIn;
		for (int i = 0; i < buttons.size(); i++) {
			ClickableWidget b = buttons.get(i);
			float p = (since - 0.06f - i * 0.045f) / 0.42f;
			if (!anim || p >= 1f) {
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

	/** Przycisk w wybranym stylu (pastel / cukierek / szklo / obrys) z animacja najechania. */
	public static void renderButton(DrawContext ctx, ClickableWidget w, float alpha, boolean hovered, boolean active) {
		ZiemniakConfig cfg = cfg();
		Colors c = pal();
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		HoverState st = HOVER.computeIfAbsent(w, k -> new HoverState());
		long now = now();
		float dt = Math.min(0.1f, (now - st.last) / 1000f);
		st.last = now;
		float target = (hovered && active) ? 1f : 0f;
		if (target > 0f && !st.was) Sounds.play(Sounds.Sfx.HOVER);
		st.was = target > 0f;
		st.progress += (target - st.progress) * clamp01(dt * 14f);
		float hp = st.progress;

		int x1 = w.getX(), y1 = w.getY(), x2 = x1 + w.getWidth(), y2 = y1 + w.getHeight();
		float cx = (x1 + x2) / 2f, cy = (y1 + y2) / 2f;
		float t = time();

		MatrixStack m = ctx.getMatrices();
		m.push();
		float s = 1f;
		if (cfg.buttonBounce) s += 0.05f * hp + (hp > 0.5f ? (float) Math.sin(t * 6.0) * 0.008f : 0f);
		m.translate(cx, cy, 0f);
		m.scale(s, s, 1f);
		m.translate(-cx, -cy, 0f);

		if (cfg.buttonGlow && hp > 0.02f) {
			Shapes.fillRounded(ctx, x1 - 3, y1 - 3, x2 + 3, y2 + 3, argb(alpha * 0.22f * hp, c.main()));
			Shapes.fillRounded(ctx, x1 - 2, y1 - 2, x2 + 2, y2 + 2, argb(alpha * 0.35f * hp, c.light()));
		}

		int top = active ? lerpRgb(hp, c.light(), lerpRgb(0.5f, c.light(), WHITE)) : lerpRgb(0.7f, c.light(), 0xDCD6DA);
		int bottom = active ? lerpRgb(hp, c.main(), lerpRgb(0.25f, c.main(), WHITE)) : lerpRgb(0.7f, c.main(), 0xC4BCC2);
		int border = active ? lerpRgb(hp, c.dark(), WHITE) : lerpRgb(0.6f, c.dark(), 0x9A9298);
		int textColor = active ? WHITE : 0xF3E6EF;
		int shadow = active ? c.dark() : 0x7D6577;

		switch (cfg.buttonStyle) {
			case PASTEL, CUKIEREK -> {
				Shapes.fillRounded(ctx, x1, y1 + 1, x2, y2 + 1, argb(alpha * 0.45f, c.dark()));
				ctx.fillGradient(x1 + 1, y1 + 1, x2 - 1, y2 - 1, argb(alpha, top), argb(alpha, bottom));
				Shapes.outlineRounded(ctx, x1, y1, x2, y2, argb(alpha, border));
				ctx.fill(x1 + 2, y1 + 1, x2 - 2, y1 + 2, argb(alpha * 0.7f, WHITE));
				if (cfg.buttonStyle == Options.ButtonStyle.CUKIEREK) {
					int off = (int) ((t * 12f) % 10f);
					int bh = y2 - y1 - 2;
					for (int sx = x1 - bh; sx < x2; sx += 10) {
						for (int seg = 0; seg < 4; seg++) {
							int sy1 = y1 + 1 + seg * bh / 4, sy2 = y1 + 1 + (seg + 1) * bh / 4;
							int a1 = sx + off + (3 - seg) * (bh / 4), a2 = a1 + 4;
							a1 = Math.max(a1, x1 + 1);
							a2 = Math.min(a2, x2 - 1);
							if (a2 > a1) ctx.fill(a1, sy1, a2, sy2, argb(alpha * 0.22f, WHITE));
						}
					}
				}
			}
			case SZKLO -> {
				ctx.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, argb(alpha * (0.22f + 0.15f * hp), c.main()));
				ctx.fill(x1 + 1, y1 + 1, x2 - 1, (y1 + y2) / 2, argb(alpha * 0.22f, WHITE));
				Shapes.outlineRounded(ctx, x1, y1, x2, y2, argb(alpha * 0.85f, WHITE));
				shadow = 0x40304A;
			}
			case OBRYS -> {
				ctx.fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, argb(alpha * (0.12f + 0.4f * hp), c.main()));
				Shapes.outlineRounded(ctx, x1, y1, x2, y2, argb(alpha, c.main()));
				Shapes.outlineRounded(ctx, x1 + 1, y1 + 1, x2 - 1, y2 - 1, argb(alpha, c.main()));
			}
		}

		if (cfg.buttonShine && hp > 0.02f) {
			int sx = x1 + (int) (((t * 0.9f) % 1f) * (x2 - x1 + 30)) - 15;
			for (int k = 0; k < 6; k++) {
				int px = sx + k;
				if (px > x1 + 1 && px < x2 - 1) ctx.fill(px, y1 + 2, px + 1, y2 - 2, argb(alpha * 0.25f * hp, WHITE));
			}
		}

		String msg = w.getMessage().getString();
		int tw = tr.getWidth(msg);
		int maxW = w.getWidth() - 6;
		if (tw > maxW) {
			msg = tr.trimToWidth(msg, maxW - tr.getWidth("..")) + "..";
			tw = tr.getWidth(msg);
		}
		int tx = Math.round(cx - tw / 2f);
		int ty = Math.round(cy - 4f);
		int textA = Math.max(4, Math.round(alpha * 255f));
		ctx.drawText(tr, msg, tx + 1, ty + 1, (textA << 24) | (shadow & 0xFFFFFF), false);
		ctx.drawText(tr, msg, tx, ty, (textA << 24) | textColor, false);

		if (cfg.buttonHearts && hp > 0.02f) {
			float off = 6f + (1f - hp) * 10f;
			float beat = 1f + (float) Math.abs(Math.sin(t * 5.0)) * 0.25f;
			Shapes.draw(ctx, Shapes.HEART, tx - off, cy, beat, argb(alpha * hp, WHITE));
			Shapes.draw(ctx, Shapes.HEART, tx + tw + off, cy, beat, argb(alpha * hp, WHITE));
		}
		m.pop();
	}

	/** Suwak: tor z wypelnieniem do wartosci i uchwyt z serduszkiem. */
	public static void renderSlider(DrawContext ctx, ClickableWidget w, float alpha, boolean hovered, boolean active, double value) {
		Colors c = pal();
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		HoverState st = HOVER.computeIfAbsent(w, k -> new HoverState());
		long now = now();
		float dt = Math.min(0.1f, (now - st.last) / 1000f);
		st.last = now;
		if (hovered && active && !st.was) Sounds.play(Sounds.Sfx.HOVER);
		st.was = hovered && active;
		st.progress += ((hovered && active ? 1f : 0f) - st.progress) * clamp01(dt * 14f);
		float hp = st.progress;
		int x1 = w.getX(), y1 = w.getY(), x2 = x1 + w.getWidth(), y2 = y1 + w.getHeight();
		if (hp > 0.02f) Shapes.fillRounded(ctx, x1 - 2, y1 - 2, x2 + 2, y2 + 2, argb(alpha * 0.3f * hp, c.light()));
		Shapes.fillRounded(ctx, x1, y1, x2, y2, argb(alpha * 0.85f, lerpRgb(0.55f, c.dark(), 0x201020)));
		int fx = x1 + 1 + (int) Math.round((x2 - x1 - 2) * clamp01((float) value));
		ctx.fillGradient(x1 + 1, y1 + 1, fx, y2 - 1, argb(alpha, c.light()), argb(alpha, c.main()));
		Shapes.outlineRounded(ctx, x1, y1, x2, y2, argb(alpha, lerpRgb(hp, c.dark(), WHITE)));
		int hx = Math.max(x1 + 4, Math.min(x2 - 4, fx));
		Shapes.fillRounded(ctx, hx - 4, y1, hx + 4, y2, argb(alpha, WHITE));
		Shapes.draw(ctx, Shapes.HEART, hx, (y1 + y2) / 2f, 0.7f + 0.15f * hp, argb(alpha, c.main()));
		String msg = w.getMessage().getString();
		int tw = tr.getWidth(msg);
		int tx = (x1 + x2) / 2 - tw / 2, ty = (y1 + y2) / 2 - 4;
		int a = Math.max(4, Math.round(alpha * 255f)) << 24;
		ctx.drawText(tr, msg, tx + 1, ty + 1, a | (c.dark() & 0xFFFFFF), false);
		ctx.drawText(tr, msg, tx, ty, a | (active ? WHITE : 0xD8C8D4), false);
	}

	// ================================================================ wierzch: tytul, kursor, klik

	public static void drawOverlay(DrawContext ctx, Screen screen, int mouseX, int mouseY) {
		ZiemniakConfig cfg = cfg();
		Colors c = pal();
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		float t = time();
		float open = easeOutCubic(sinceOpen(screen) / 0.5f);
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(0f, 0f, 450f);

		if (isPause(screen)) {
			int cx = screen.width / 2;
			int tw = tr.getWidth(screen.getTitle());
			if (cfg.titleHearts) {
				for (int side = -1; side <= 1; side += 2) {
					float bob = (float) Math.sin(t * 3.0 + side) * 2f;
					float beat = 1.3f + (float) Math.abs(Math.sin(t * 2.6)) * 0.4f;
					Shapes.draw(ctx, Shapes.HEART, cx + side * (tw / 2f + 12f), 44f + bob, beat, argb(open, c.main()));
					Shapes.sparkle(ctx, cx + side * (tw / 2f + 24f), 38f - bob, 0.9f, argb(open, WHITE));
				}
			}
			if (cfg.waveText) drawWaveText(ctx, tr, "♡ ziemniak client ♡", cx, screen.height - 12, t, open);
		}

		drawTrail(ctx, mouseX, mouseY, t, c);

		if (cfg.cursorHeart) {
			float cb = (float) Math.sin(t * 5.0) * 1.5f;
			Shapes.draw(ctx, Shapes.HEART, mouseX + 9f, mouseY + 11f + cb, 0.9f, argb(0.9f * open, c.main()));
		}

		drawBursts(ctx, c);
		m.pop();
	}

	private static void drawTrail(DrawContext ctx, int mx, int my, float t, Colors c) {
		Options.CursorTrail mode = cfg().cursorTrail;
		long now = now();
		float[] last = TRAIL.peekLast();
		if (last == null || Math.abs(last[0] - mx) + Math.abs(last[1] - my) > 2f) {
			TRAIL.addLast(new float[]{mx, my, now});
		}
		while (!TRAIL.isEmpty() && now - (long) TRAIL.peekFirst()[2] > 500L) TRAIL.removeFirst();
		while (TRAIL.size() > 40) TRAIL.removeFirst();
		if (mode == Options.CursorTrail.BRAK) return;
		int i = 0;
		for (float[] p : TRAIL) {
			float age = (now - (long) p[2]) / 500f;
			float a = 1f - age;
			switch (mode) {
				case SERDUSZKA -> Shapes.draw(ctx, Shapes.HEART, p[0], p[1] - age * 6f, 0.4f + a * 0.6f, argb(a * 0.8f, i % 2 == 0 ? c.main() : c.p3()));
				case ISKIERKI -> Shapes.sparkle(ctx, p[0] + (hash(i + (int) p[2]) - 0.5f) * 6f, p[1] + age * 5f, 0.4f + a * 0.6f, argb(a, i % 2 == 0 ? WHITE : c.p2()));
				case TECZA -> Shapes.dot(ctx, p[0], p[1], 0.8f + a * 1.2f, argb(a * 0.85f, Palettes.hsv(i * 0.05f + t * 0.5f, 0.55f, 1f)));
				case GWIAZDKI -> Shapes.draw(ctx, Shapes.STAR, p[0], p[1] + age * 4f, 0.35f + a * 0.5f, argb(a, i % 2 == 0 ? 0xFFF3A8 : c.p2()));
				case BABELKI -> Shapes.draw(ctx, Shapes.BUBBLE, p[0] + (float) Math.sin(age * 9f + i) * 3f, p[1] - age * 14f, 0.5f + age * 0.5f, 0.5f + age * 0.5f,
						argb(a * 0.8f, 0xBDEBFF), argb(a, WHITE));
				default -> { }
			}
			i++;
		}
	}

	private static void drawBursts(DrawContext ctx, Colors c) {
		long now = now();
		Iterator<Burst> it = BURSTS.iterator();
		while (it.hasNext()) {
			Burst b = it.next();
			float dur = b.kind == Options.ClickEffect.KONFETTI ? 1.1f : 0.7f;
			float age = (now - b.start) / 1000f;
			if (age > dur || age < 0f) {
				it.remove();
				continue;
			}
			float p = age / dur;
			float dist = easeOutCubic(p) * 26f;
			switch (b.kind) {
				case SERDUSZKA, GWIAZDKI, KWIATKI -> {
					String[] shape = b.kind == Options.ClickEffect.SERDUSZKA ? Shapes.HEART
							: b.kind == Options.ClickEffect.GWIAZDKI ? Shapes.STAR : Shapes.FLOWER;
					int n = 8;
					for (int k = 0; k < n; k++) {
						double ang = k * Math.PI * 2 / n + b.seed * 0.37;
						float hx = (float) (b.x + Math.cos(ang) * dist);
						float hy = (float) (b.y + Math.sin(ang) * dist) - p * 6f;
						int col = k % 2 == 0 ? c.main() : (k % 4 == 1 ? c.p3() : WHITE);
						float sc = 1.1f * (1f - p * 0.5f);
						float flip = b.kind == Options.ClickEffect.KWIATKI ? (float) Math.cos(p * 9f + k) : 1f;
						int col2 = b.kind == Options.ClickEffect.KWIATKI ? 0xFFF3A8 : col;
						Shapes.draw(ctx, shape, hx, hy, sc * flip, sc, argb(1f - p, col), argb(1f - p, col2));
					}
				}
				case FALA -> {
					Shapes.ring(ctx, (float) b.x, (float) b.y, easeOutCubic(p) * 24f, 28, 0.9f, argb(1f - p, c.main()));
					float p2 = clamp01((age - 0.12f) / dur);
					if (p2 > 0f) Shapes.ring(ctx, (float) b.x, (float) b.y, easeOutCubic(p2) * 16f, 20, 0.7f, argb(1f - p2, WHITE));
				}
				case DUZE_SERCE -> {
					float sc = 1f + easeOutBack(p * 1.4f) * 2.6f;
					Shapes.draw(ctx, Shapes.HEART, (float) b.x, (float) b.y - p * 10f, sc + 0.4f, argb((1f - p) * 0.8f, WHITE));
					Shapes.draw(ctx, Shapes.HEART, (float) b.x, (float) b.y - p * 10f, sc, argb(1f - p, c.main()));
				}
				case ISKRY -> {
					for (int k = 0; k < 12; k++) {
						double ang = k * Math.PI / 6 + b.seed * 0.21;
						float v = 50f + hash(b.seed + k) * 30f;
						float sx = (float) (b.x + Math.cos(ang) * v * age);
						float sy = (float) (b.y + Math.sin(ang) * v * age + 60f * age * age);
						Shapes.sparkle(ctx, sx, sy, 0.9f * (1f - p) + 0.3f, argb(1f - p, k % 3 == 0 ? 0xFFF3A8 : (k % 3 == 1 ? WHITE : c.p2())));
					}
				}
				case KONFETTI -> {
					for (int k = 0; k < 16; k++) {
						float hk = hash(b.seed * 31 + k);
						double ang = -Math.PI / 2 + (hk - 0.5) * 2.4;
						float v = 60f + hash(b.seed * 17 + k) * 50f;
						float px = (float) (b.x + Math.cos(ang) * v * age);
						float py = (float) (b.y + Math.sin(ang) * v * age + 0.5f * 160f * age * age);
						float flip = (float) Math.cos(age * 14f + k);
						int col = Palettes.hsv(hk + 0.1f * k, 0.5f, 1f);
						MatrixStack m = ctx.getMatrices();
						m.push();
						m.translate(px, py, 0f);
						m.scale(flip, 1f, 1f);
						ctx.fill(-1, -2, 2, 1, argb(1f - p * p, col));
						m.pop();
					}
				}
				default -> { }
			}
		}
	}

	public static void drawWaveText(DrawContext ctx, TextRenderer tr, String text, int cx, int y, float t, float alpha) {
		Colors c = pal();
		int total = tr.getWidth(text);
		float x = cx - total / 2f;
		int a = Math.max(4, Math.round(clamp01(alpha) * 255f));
		for (int i = 0; i < text.length(); i++) {
			String ch = String.valueOf(text.charAt(i));
			float yy = y + (float) Math.sin(t * 4.0 + i * 0.45) * 2.5f;
			int col = lerpRgb((float) (Math.sin(t * 2.0 + i * 0.3) + 1.0) / 2f, c.main(), c.p3());
			ctx.drawText(tr, ch, Math.round(x) + 1, Math.round(yy) + 1, (a << 24) | (c.dark() & 0xFFFFFF), false);
			ctx.drawText(tr, ch, Math.round(x), Math.round(yy), (a << 24) | col, false);
			x += tr.getWidth(ch);
		}
	}

	public static void addBurst(double x, double y) {
		addBurst(x, y, true);
	}

	public static void addBurst(double x, double y, boolean sound) {
		Options.ClickEffect kind = cfg().clickEffect;
		if (kind == Options.ClickEffect.BRAK) return;
		long n = now();
		BURSTS.add(new Burst(x, y, n, kind, (int) (n & 0xFFFF)));
		if (BURSTS.size() > 20) BURSTS.remove(0);
		if (sound) Sounds.clickEffect(kind);
	}
}
