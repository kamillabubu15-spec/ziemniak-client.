package com.ziemniak.client.config;

/** Listy wyborow w menu moda. Kazda wartosc ma polska nazwe wyswietlana na przycisku. */
public final class Options {
	private Options() {}

	public interface Named {
		String label();
	}

	public enum Palette implements Named {
		ROZOWA("Rozowa"), LAWENDOWA("Lawendowa"), MIETOWA("Mietowa"), BRZOSKWINIOWA("Brzoskwiniowa"),
		BLEKITNA("Blekitna"), TRUSKAWKOWA("Truskawkowa"), BUBBLEGUM("Bubblegum"), SAKURA("Sakura"),
		NOCNA("Nocna"), TECZOWA("Teczowa"), CYTRYNOWA("Cytrynowa"), MATCHA("Matcha"), KAWOWA("Mleczna kawa"),
		OCEAN("Ocean"), KOSMICZNA("Kosmiczna"), ARBUZOWA("Arbuzowa");

		private final String label;
		Palette(String l) { label = l; }
		public String label() { return label; }
	}

	public enum Background implements Named {
		GRADIENT("Gradient"), ZORZA("Zorza polarna"), TECZA("Tecza"), GWIAZDY("Gwiezdne niebo"), KRATKA("Kratka"),
		FALE("Fale"), BOKEH("Bokeh"), BRAK("Brak");

		private final String label;
		Background(String l) { label = l; }
		public String label() { return label; }
	}

	public enum ParticleKind implements Named {
		SERDUSZKA("Serduszka"), GWIAZDKI("Gwiazdki"), SAKURA("Platki sakury"), BABELKI("Babelki"),
		MOTYLKI("Motylki"), SNIEG("Sniezynki"), NUTKI("Nutki"), LAPKI("Kocie lapki"), CHMURKI("Chmurki"),
		TRUSKAWKI("Truskawki"), DIAMENTY("Diamenty"), KOTKI("Kotki"), MIX("Mix wszystkiego");

		private final String label;
		ParticleKind(String l) { label = l; }
		public String label() { return label; }
	}

	public enum Density implements Named {
		MALO("Malo", 0.5f), SREDNIO("Srednio", 1f), DUZO("Duzo", 1.7f), MEGA("MEGA", 2.6f);

		private final String label;
		public final float mul;
		Density(String l, float m) { label = l; mul = m; }
		public String label() { return label; }
	}

	public enum ButtonStyle implements Named {
		PASTEL("Pastel"), CUKIEREK("Cukierek"), SZKLO("Szklo"), OBRYS("Obrys");

		private final String label;
		ButtonStyle(String l) { label = l; }
		public String label() { return label; }
	}

	public enum Speed implements Named {
		X05("0.5x", 0.5f), X1("1x", 1f), X15("1.5x", 1.5f), X2("2x", 2f);

		private final String label;
		public final float mul;
		Speed(String l, float m) { label = l; mul = m; }
		public String label() { return label; }
	}

	public enum InventoryOpen implements Named {
		POP("Pop"), WJAZD("Wjazd z dolu"), OBROT("Obrot"), SPREZYNA("Sprezyna"), BRAK("Brak");

		private final String label;
		InventoryOpen(String l) { label = l; }
		public String label() { return label; }
	}

	public enum ClickEffect implements Named {
		SERDUSZKA("Serduszka"), GWIAZDKI("Gwiazdki"), FALA("Fala"), KONFETTI("Konfetti"), KWIATKI("Kwiatki"),
		DUZE_SERCE("Duze serce"), ISKRY("Iskry"), BRAK("Brak");

		private final String label;
		ClickEffect(String l) { label = l; }
		public String label() { return label; }
	}

	public enum CursorTrail implements Named {
		BRAK("Brak"), SERDUSZKA("Serduszka"), ISKIERKI("Iskierki"), TECZA("Tecza"), GWIAZDKI("Gwiazdki"), BABELKI("Babelki");

		private final String label;
		CursorTrail(String l) { label = l; }
		public String label() { return label; }
	}

	public enum Crosshair implements Named {
		VANILLA("Vanilla"), SERDUSZKO("Serduszko"), PLUS("Plus"), KROPKA("Kropka"), KOLKO("Kolko"),
		GWIAZDKA("Gwiazdka"), KRZYZYK("Krzyzyk");

		private final String label;
		Crosshair(String l) { label = l; }
		public String label() { return label; }
	}

	public enum HudScale implements Named {
		S075("75%", 0.75f), S100("100%", 1f), S125("125%", 1.25f), S150("150%", 1.5f);

		private final String label;
		public final float mul;
		HudScale(String l, float m) { label = l; mul = m; }
		public String label() { return label; }
	}

	public enum ZoomLevel implements Named {
		X2("2x", 2f), X4("4x", 4f), X6("6x", 6f), X10("10x", 10f);

		private final String label;
		public final float mul;
		ZoomLevel(String l, float m) { label = l; mul = m; }
		public String label() { return label; }
	}

	public static <E extends Enum<E>> E next(E value) {
		E[] all = value.getDeclaringClass().getEnumConstants();
		return all[(value.ordinal() + 1) % all.length];
	}
}
