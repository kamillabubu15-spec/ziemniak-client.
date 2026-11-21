package com.ziemniak.client.config;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Wszystkie ustawienia moda. Zapis: .minecraft/config/ziemniak-client.json */
public class ZiemniakConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ziemniak-client.json");
	private static ZiemniakConfig instance;

	// ---------------------------------------------------------------- motyw
	public boolean themeEnabled = true;
	public Options.Palette palette = Options.Palette.ROZOWA;
	public Options.Background background = Options.Background.GRADIENT;
	public Options.ParticleKind particles = Options.ParticleKind.SERDUSZKA;
	public Options.Density density = Options.Density.SREDNIO;
	public Options.ButtonStyle buttonStyle = Options.ButtonStyle.PASTEL;
	public Options.Speed speed = Options.Speed.X1;

	// ---------------------------------------------------------------- animacje
	public boolean bgParticles = true;
	public boolean sparkles = true;
	public boolean buttonSlideIn = true;
	public boolean buttonGlow = true;
	public boolean buttonShine = true;
	public boolean buttonHearts = true;
	public boolean buttonBounce = true;
	public boolean titleHearts = true;
	public boolean waveText = true;
	public boolean panelGlow = true;
	public boolean panelBow = true;
	public boolean panelCornerHearts = true;
	public boolean slotHover = true;
	public boolean shootingStars = true;
	public Options.InventoryOpen inventoryOpen = Options.InventoryOpen.POP;

	// ---------------------------------------------------------------- kursor i klik
	public Options.ClickEffect clickEffect = Options.ClickEffect.SERDUSZKA;
	public Options.CursorTrail cursorTrail = Options.CursorTrail.ISKIERKI;
	public boolean cursorHeart = true;

	// ---------------------------------------------------------------- shadery
	public String shader = "brak";

	public static ZiemniakConfig get() {
		if (instance == null) load();
		return instance;
	}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				try (Reader r = Files.newBufferedReader(FILE)) {
					instance = GSON.fromJson(r, ZiemniakConfig.class);
				}
			}
		} catch (Exception e) {
			instance = null;
		}
		if (instance == null) instance = new ZiemniakConfig();
		instance.fixNulls();
	}

	/** Przywraca domyslne ustawienia (zostawia wybrany shader). */
	public static void reset() {
		String shader = get().shader;
		instance = new ZiemniakConfig();
		instance.shader = shader;
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE)) {
				GSON.toJson(get(), w);
			}
		} catch (Exception ignored) {
		}
	}

	private void fixNulls() {
		if (palette == null) palette = Options.Palette.ROZOWA;
		if (background == null) background = Options.Background.GRADIENT;
		if (particles == null) particles = Options.ParticleKind.SERDUSZKA;
		if (density == null) density = Options.Density.SREDNIO;
		if (buttonStyle == null) buttonStyle = Options.ButtonStyle.PASTEL;
		if (speed == null) speed = Options.Speed.X1;
		if (inventoryOpen == null) inventoryOpen = Options.InventoryOpen.POP;
		if (clickEffect == null) clickEffect = Options.ClickEffect.SERDUSZKA;
		if (cursorTrail == null) cursorTrail = Options.CursorTrail.ISKIERKI;
		if (shader == null) shader = "brak";
	}
}
