package com.ziemniak.client.theme;

import com.ziemniak.client.config.Options.Palette;

/** Kolory kazdej palety. */
public final class Palettes {
	private Palettes() {}

	/**
	 * @param top1/top2 gora tla (plynnie zmienia sie miedzy nimi)
	 * @param bot1/bot2 dol tla
	 * @param light     jasny kolor przycisku (gora)
	 * @param main      glowny kolor przycisku (dol)
	 * @param dark      ramka / cien
	 * @param p1/p2/p3  kolory czasteczek
	 * @param night     czy paleta jest ciemna (mocniejsze tlo)
	 */
	public record Colors(int top1, int top2, int bot1, int bot2, int light, int main, int dark,
			int p1, int p2, int p3, boolean night) {}

	public static Colors of(Palette p, float time) {
		return switch (p) {
			case ROZOWA -> new Colors(0xFF9BDB, 0xD6B4FF, 0xC9A7FF, 0xFFB8E1, 0xFFB3E6, 0xF27CC6, 0xB33F8D, 0xFFFFFF, 0xFFC2EC, 0xC9A7FF, false);
			case LAWENDOWA -> new Colors(0xC7B2FF, 0xE3C8FF, 0xA99BFF, 0xD9B8FF, 0xD9C6FF, 0xA98BF5, 0x6D52C2, 0xFFFFFF, 0xE3D4FF, 0xFFC2EC, false);
			case MIETOWA -> new Colors(0xA8F0D8, 0xC9F7E8, 0x9EE6F0, 0xB5F2D0, 0xB8F5DF, 0x5FD3A8, 0x2E8F6E, 0xFFFFFF, 0xD6FFF0, 0xFFC2EC, false);
			case BRZOSKWINIOWA -> new Colors(0xFFC8A8, 0xFFD9B8, 0xFFB3C1, 0xFFC9A0, 0xFFD3BA, 0xFF9F80, 0xC25E44, 0xFFFFFF, 0xFFE3D1, 0xFFB3C7, false);
			case BLEKITNA -> new Colors(0xA8D8FF, 0xC8E6FF, 0xB8C6FF, 0xA8E0FF, 0xC2E4FF, 0x6FB6F5, 0x3473B8, 0xFFFFFF, 0xD8EEFF, 0xC9A7FF, false);
			case TRUSKAWKOWA -> new Colors(0xFF8FA3, 0xFFB3C1, 0xFF6F91, 0xFF9CB0, 0xFFB0C0, 0xF0506E, 0xA8243F, 0xFFFFFF, 0xFFD1DC, 0x8FE3A0, false);
			case BUBBLEGUM -> new Colors(0xFF9CE6, 0x9CF0FF, 0xB39CFF, 0xFFB8F0, 0xFFC2F0, 0xFF6FD8, 0xB0309A, 0xFFFFFF, 0x9CF0FF, 0xFFF59C, false);
			case SAKURA -> new Colors(0xFFD1E0, 0xFFE4EE, 0xF5B8CF, 0xFFD9E6, 0xFFE0EC, 0xF59AB8, 0xB85C7D, 0xFFFFFF, 0xFFC2D6, 0xF7E3A0, false);
			case NOCNA -> new Colors(0x2A1B4D, 0x3A2160, 0x6B3A8F, 0x4A2A7A, 0x9B7BE0, 0x6A4CC2, 0x2B1A5C, 0xFFFFFF, 0xFFC2EC, 0xA8E0FF, true);
			case CYTRYNOWA -> new Colors(0xFFF3A8, 0xFFF8C8, 0xFFE08A, 0xFFF0B0, 0xFFF3B8, 0xF5CE45, 0xA8821A, 0xFFFFFF, 0xFFF8D0, 0xB8F0A8, false);
			case MATCHA -> new Colors(0xC8E6B0, 0xDFF0C8, 0xA8D49A, 0xCDE8B8, 0xCDEBB8, 0x8CC47A, 0x4E7D3E, 0xFFFFFF, 0xE8F5D8, 0xFFC2D6, false);
			case KAWOWA -> new Colors(0xE8D2BC, 0xF2E2D0, 0xD4B89C, 0xE6CCB2, 0xE8D3BF, 0xC09A7A, 0x7A5640, 0xFFFFFF, 0xF5E6D6, 0xFFC2D6, false);
			case OCEAN -> new Colors(0x8FD8F0, 0xB0E8F8, 0x6FB8E8, 0x90D0F0, 0xA8E2F8, 0x3FA4D8, 0x1E5F8A, 0xFFFFFF, 0xC8F0FF, 0x9CF0D8, false);
			case KOSMICZNA -> new Colors(0x1A1440, 0x2A1A5A, 0x3A1E6A, 0x221848, 0x8F7BFF, 0x5A48D8, 0x1A1250, 0xFFFFFF, 0xB8A8FF, 0xFF9CE6, true);
			case ARBUZOWA -> new Colors(0xFF9CA8, 0xFFB8C0, 0xA8E8A0, 0xC8F0C0, 0xFFB0BC, 0xF2647A, 0x3E8A4A, 0xFFFFFF, 0xFFD0D8, 0x7AD48A, false);
			case TECZOWA -> {
				float h = time * 0.08f;
				yield new Colors(hsv(h, 0.38f, 1f), hsv(h + 0.15f, 0.38f, 1f), hsv(h + 0.4f, 0.38f, 1f), hsv(h + 0.55f, 0.38f, 1f),
						hsv(h, 0.3f, 1f), hsv(h, 0.55f, 0.97f), hsv(h, 0.75f, 0.6f),
						0xFFFFFF, hsv(h + 0.33f, 0.35f, 1f), hsv(h + 0.66f, 0.35f, 1f), false);
			}
		};
	}

	public static int hsv(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		int i = (int) (h * 6f);
		float f = h * 6f - i;
		float p = v * (1f - s), q = v * (1f - f * s), t = v * (1f - (1f - f) * s);
		float r, g, b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return ((int) (r * 255f) << 16) | ((int) (g * 255f) << 8) | (int) (b * 255f);
	}
}
