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
		NOCNA("Nocna"), TECZOWA("Teczowa");

		private final String label;
		Palette(String l) { label = l; }
		public String label() { return label; }
	}

	public enum Background implements Named {
		GRADIENT("Gradient"), ZORZA("Zorza polarna"), TECZA("Tecza"), GWIAZDY("Gwiezdne niebo"), KRATKA("Kratka"), BRAK("Brak");

		private final String label;
		Background(String l) { label = l; }
		public String label() { return label; }
	}

	public enum ParticleKind implements Named {
		SERDUSZKA("Serduszka"), GWIAZDKI("Gwiazdki"), SAKURA("Platki sakury"), BABELKI("Babelki"),
		MOTYLKI("Motylki"), SNIEG("Sniezynki"), NUTKI("Nutki"), LAPKI("Kocie lapki"), MIX("Mix wszystkiego");

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
		SERDUSZKA("Serduszka"), GWIAZDKI("Gwiazdki"), FALA("Fala"), KONFETTI("Konfetti"), KWIATKI("Kwiatki"), BRAK("Brak");

		private final String label;
		ClickEffect(String l) { label = l; }
		public String label() { return label; }
	}

	public enum CursorTrail implements Named {
		BRAK("Brak"), SERDUSZKA("Serduszka"), ISKIERKI("Iskierki"), TECZA("Tecza");

		private final String label;
		CursorTrail(String l) { label = l; }
		public String label() { return label; }
	}

	public static <E extends Enum<E>> E next(E value) {
		E[] all = value.getDeclaringClass().getEnumConstants();
		return all[(value.ordinal() + 1) % all.length];
	}
}
