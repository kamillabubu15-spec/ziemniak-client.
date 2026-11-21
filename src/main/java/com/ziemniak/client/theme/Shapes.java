package com.ziemniak.client.theme;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

/** Ksztalty pixel-art. 'X' = kolor glowny, 'o' = kolor drugi. Rysowane od srodka. */
public final class Shapes {
	private Shapes() {}

	public static final String[] HEART = {
			".XX.XX.",
			"XXXXXXX",
			"XXXXXXX",
			".XXXXX.",
			"..XXX..",
			"...X..."};
	public static final String[] BOW = {
			"XX.....XX",
			"XXX...XXX",
			"XXXXoXXXX",
			"XXX...XXX",
			"XX.....XX"};
	public static final String[] STAR = {
			"...X...",
			"..XXX..",
			"XXXXXXX",
			".XXXXX.",
			"..XXX..",
			".XX.XX.",
			".X...X."};
	public static final String[] FLOWER = {
			"..XXX..",
			"..XXX..",
			"XXXoXXX",
			"XXoooXX",
			"XXXoXXX",
			"..XXX..",
			"..XXX.."};
	public static final String[] PETAL = {
			".XXX.",
			"XXXXX",
			"XXoXX",
			".XXX.",
			"..X.."};
	public static final String[] BUBBLE = {
			"..XXX..",
			".X...X.",
			"X.o...X",
			"X.....X",
			"X.....X",
			".X...X.",
			"..XXX.."};
	public static final String[] BUTTERFLY_OPEN = {
			"XX.....XX",
			"XXX.o.XXX",
			"XXXXoXXXX",
			".XXXoXXX.",
			"XXX.o.XXX",
			"XX.....XX"};
	public static final String[] BUTTERFLY_CLOSED = {
			"...X.X...",
			"...XoX...",
			"..XXoXX..",
			"...XoX...",
			"..XXoXX..",
			"...X.X..."};
	public static final String[] SNOW = {
			"X..X..X",
			".X.X.X.",
			"..XXX..",
			"XXXoXXX",
			"..XXX..",
			".X.X.X.",
			"X..X..X"};
	public static final String[] NOTE = {
			"...XXX",
			"...X.X",
			"...X.X",
			"...X..",
			"...X..",
			".XXX..",
			"XXXX..",
			".XX..."};
	public static final String[] PAW = {
			".X.X.X.",
			".......",
			"X.XXX.X",
			".XXXXX.",
			".XXXXX.",
			"..XXX.."};

	/** Rysuje ksztalt wysrodkowany w (x, y). scaleX moze byc ujemne (odbicie = "obrot"). */
	public static void draw(DrawContext ctx, String[] shape, float x, float y, float scaleX, float scaleY, int color, int color2) {
		if ((color >>> 24) < 3 || scaleY == 0f) return;
		int w = shape[0].length(), h = shape.length;
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(x, y, 0f);
		m.scale(scaleX, scaleY, 1f);
		m.translate(-w / 2f, -h / 2f, 0f);
		for (int row = 0; row < h; row++) {
			String line = shape[row];
			int start = -1;
			char cur = 0;
			for (int col = 0; col <= w; col++) {
				char c = col < w ? line.charAt(col) : '.';
				if (start >= 0 && c != cur) {
					ctx.fill(start, row, col, row + 1, cur == 'X' ? color : color2);
					start = -1;
				}
				if (start < 0 && c != '.') {
					start = col;
					cur = c;
				}
			}
		}
		m.pop();
	}

	public static void draw(DrawContext ctx, String[] shape, float x, float y, float scale, int color) {
		draw(ctx, shape, x, y, scale, scale, color, color);
	}

	/** Iskierka w ksztalcie plusa. */
	public static void sparkle(DrawContext ctx, float x, float y, float size, int color) {
		if ((color >>> 24) < 3) return;
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(x, y, 0f);
		m.scale(size, size, 1f);
		ctx.fill(-2, 0, 3, 1, color);
		ctx.fill(0, -2, 1, 3, color);
		m.pop();
	}

	/** Kwadracik o srodku (x, y). */
	public static void dot(DrawContext ctx, float x, float y, float size, int color) {
		if ((color >>> 24) < 3) return;
		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(x, y, 0f);
		m.scale(size, size, 1f);
		ctx.fill(-1, -1, 1, 1, color);
		m.pop();
	}

	/** Okrag z kropek. */
	public static void ring(DrawContext ctx, float cx, float cy, float r, int points, float size, int color) {
		for (int i = 0; i < points; i++) {
			double a = i * Math.PI * 2.0 / points;
			dot(ctx, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, size, color);
		}
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
}
