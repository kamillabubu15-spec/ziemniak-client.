package com.ziemniak.client.shader;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.GlDebugInfo;
import com.ziemniak.client.config.ZiemniakConfig;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * Shadery:
 * - wlasne (post-processing Minecrafta, dzialaja bez zadnych innych modow),
 * - z internetu (paczki z Modrinth do folderu shaderpacks, wymagaja Iris).
 */
public final class ShaderManager {
	private ShaderManager() {}

	// ================================================================ wlasne shadery

	public record OwnShader(String id, String label, String desc) {}

	public static final List<OwnShader> OWN = List.of(
			new OwnShader("brak", "Brak", "zwykly Minecraft"),
			new OwnShader("rozowy_sen", "Rozowy Sen", "rozowe niebo, lawendowe cienie"),
			new OwnShader("zlota_godzina", "Zlota Godzina", "zachod slonca, cieple swiatlo"),
			new OwnShader("anime", "Anime", "kontury jak w kreskowce"),
			new OwnShader("sny_sakury", "Sny Sakury", "spadajace platki wisni"),
			new OwnShader("neon_noc", "Neon Noc", "cyberpunk, gwiazdy, neon"),
			new OwnShader("retro_vhs", "Retro VHS", "stara kaseta wideo"),
			new OwnShader("akwarela", "Akwarela", "obraz malowany farbami"),
			new OwnShader("zimowy_poranek", "Zimowy Poranek", "chlodne, mrozne swiatlo"),
			new OwnShader("cukierkowy", "Cukierkowy", "mocne pastelowe kolory"),
			new OwnShader("noir", "Noir", "czarno-bialy film"),
			new OwnShader("stara_fotografia", "Stara Fotografia", "sepia, winieta"));

	public static Identifier postId(String id) {
		return Identifier.of("ziemniak", id);
	}

	/** Rysuje wybrany wlasny shader (wywolywane z GameRendererMixin, przed reka). */
	public static void renderOwn(MinecraftClient mc, ObjectAllocator pool) {
		String sel = ZiemniakConfig.get().shader;
		if (sel == null || sel.equals("brak") || OWN.stream().noneMatch(s -> s.id().equals(sel))) return;
		PostEffectProcessor p = mc.getShaderLoader().loadPostEffect(postId(sel), DefaultFramebufferSet.MAIN_ONLY);
		if (p == null) return;
		p.setUniforms("Time", (System.currentTimeMillis() % 3_600_000L) / 1000f * (float) com.ziemniak.client.theme.FemboyTheme.timeScale());
		p.render(mc.getFramebuffer(), pool);
		mc.getFramebuffer().beginWrite(true);
	}

	public static void select(String id) {
		ZiemniakConfig.get().shader = id;
		ZiemniakConfig.save();
	}

	// ================================================================ shadery z internetu

	public enum Tier {
		LEKKI("Lekkie", "slaby komp / laptop"),
		SREDNI("Srednie", "zwykly komp do gier"),
		MOCNY("Mocne", "mocna karta graficzna");

		public final String label;
		public final String hint;

		Tier(String label, String hint) {
			this.label = label;
			this.hint = hint;
		}
	}

	public record NetShader(String slug, String name, Tier tier, String desc) {}

	public static final List<NetShader> NET = List.of(
			new NetShader("makeup-ultra-fast-shaders", "MakeUp Ultra Fast", Tier.LEKKI, "bardzo szybki, ladne niebo"),
			new NetShader("vanilla-plus-shader", "Vanilla Plus", Tier.LEKKI, "jak vanilla, tylko ladniej"),
			new NetShader("super-duper-vanilla", "Super Duper Vanilla", Tier.LEKKI, "lekki, kolorowy"),
			new NetShader("bsl-shaders", "BSL Shaders", Tier.SREDNI, "klasyk, piekna woda"),
			new NetShader("complementary-reimagined", "Complementary Reimagined", Tier.SREDNI, "najpopularniejszy"),
			new NetShader("complementary-unbound", "Complementary Unbound", Tier.SREDNI, "realistyczny"),
			new NetShader("solas-shader", "Solas", Tier.MOCNY, "bajkowe swiatlo"),
			new NetShader("bliss-shader", "Bliss", Tier.MOCNY, "mgla i chmury wolumetryczne"),
			new NetShader("photon-shader", "Photon", Tier.MOCNY, "filmowy wyglad"),
			new NetShader("sildurs-vibrant-shaders", "Sildur's Vibrant", Tier.MOCNY, "wersja Extreme"));

	public enum State { BRAK, POBIERANIE, GOTOWE, BLAD }

	public static final class Status {
		public volatile State state = State.BRAK;
		public volatile float progress;
		public volatile String file;
		public volatile String error;
	}

	private static final Map<String, Status> STATUS = new ConcurrentHashMap<>();
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(15))
			.build();
	private static final String UA = "ziemniak-client/1.0.0 (Minecraft mod)";

	public static Path shaderpacksDir() {
		return FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
	}

	public static Path modsDir() {
		return FabricLoader.getInstance().getGameDir().resolve("mods");
	}

	/** Status pobierania; sprawdza tez, czy plik juz lezy na dysku. */
	public static Status status(String key) {
		return STATUS.computeIfAbsent(key, k -> new Status());
	}

	public static Status shaderStatus(NetShader s) {
		Status st = status(s.slug());
		if (st.state == State.BRAK) {
			Optional<String> f = findExisting(shaderpacksDir(), s.slug());
			if (f.isPresent()) {
				st.file = f.get();
				st.state = State.GOTOWE;
			}
		}
		return st;
	}

	private static Optional<String> findExisting(Path dir, String slug) {
		Path marker = dir.resolve(".ziemniak-" + slug);
		try {
			if (Files.exists(marker)) {
				String name = Files.readString(marker).trim();
				if (!name.isEmpty() && Files.exists(dir.resolve(name))) return Optional.of(name);
			}
		} catch (Exception ignored) {
		}
		return Optional.empty();
	}

	public static void downloadShader(NetShader s) {
		download(s.slug(), s.slug(), "iris", shaderpacksDir(), true);
	}

	public static boolean irisLoaded() {
		return FabricLoader.getInstance().isModLoaded("iris");
	}

	public static boolean sodiumLoaded() {
		return FabricLoader.getInstance().isModLoaded("sodium");
	}

	/** Iris (i Sodium) pobrane do mods, ale gra jeszcze nie zrestartowana. */
	public static boolean irisPendingRestart() {
		return !irisLoaded() && findExisting(modsDir(), "iris").isPresent();
	}

	public static void installIris() {
		download("iris", "iris", "fabric", modsDir(), false);
		if (!sodiumLoaded()) download("sodium", "sodium", "fabric", modsDir(), false);
	}

	private static void download(String key, String slug, String loader, Path dir, boolean anyGameVersion) {
		Status st = status(key);
		if (st.state == State.POBIERANIE) return;
		st.state = State.POBIERANIE;
		st.progress = 0f;
		st.error = null;
		CompletableFuture.runAsync(() -> {
			try {
				JsonObject version = findVersion(slug, loader, true);
				if (version == null && anyGameVersion) version = findVersion(slug, loader, false);
				if (version == null) throw new IllegalStateException("brak wersji dla 1.21.4");

				JsonObject file = null;
				for (JsonElement e : version.getAsJsonArray("files")) {
					JsonObject f = e.getAsJsonObject();
					if (file == null || (f.has("primary") && f.get("primary").getAsBoolean())) file = f;
				}
				if (file == null) throw new IllegalStateException("brak pliku");
				String name = file.get("filename").getAsString();
				String url = file.get("url").getAsString();

				Files.createDirectories(dir);
				Path target = dir.resolve(name).normalize();
				if (!target.getParent().equals(dir.normalize()) || name.contains("/") || name.contains("\\")) {
					throw new IllegalStateException("zla nazwa pliku");
				}
				Path part = dir.resolve(name + ".part");
				HttpResponse<InputStream> resp = HTTP.send(HttpRequest.newBuilder(URI.create(url))
						.header("User-Agent", UA).timeout(Duration.ofMinutes(5)).build(),
						HttpResponse.BodyHandlers.ofInputStream());
				if (resp.statusCode() != 200) throw new IllegalStateException("HTTP " + resp.statusCode());
				long total = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
				try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(part)) {
					byte[] buf = new byte[64 * 1024];
					long done = 0;
					int n;
					while ((n = in.read(buf)) > 0) {
						out.write(buf, 0, n);
						done += n;
						if (total > 0) st.progress = done / (float) total;
					}
				}
				Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
				Files.writeString(dir.resolve(".ziemniak-" + key), name);
				st.file = name;
				st.progress = 1f;
				st.state = State.GOTOWE;
			} catch (Exception e) {
				st.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
				st.state = State.BLAD;
			}
		}, Util.getDownloadWorkerExecutor());
	}

	private static JsonObject findVersion(String slug, String loader, boolean onlyThisMc) throws Exception {
		StringBuilder url = new StringBuilder("https://api.modrinth.com/v2/project/").append(slug).append("/version?loaders=")
				.append(URLEncoder.encode("[\"" + loader + "\"]", StandardCharsets.UTF_8));
		if (onlyThisMc) url.append("&game_versions=").append(URLEncoder.encode("[\"1.21.4\"]", StandardCharsets.UTF_8));
		HttpResponse<String> resp = HTTP.send(HttpRequest.newBuilder(URI.create(url.toString()))
				.header("User-Agent", UA).timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
		if (resp.statusCode() != 200) throw new IllegalStateException("Modrinth HTTP " + resp.statusCode());
		JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
		for (JsonElement e : arr) {
			JsonObject v = e.getAsJsonObject();
			String type = v.has("version_type") ? v.get("version_type").getAsString() : "release";
			if (type.equals("release")) return v;
		}
		return arr.isEmpty() ? null : arr.get(0).getAsJsonObject();
	}

	// ================================================================ Iris (przez refleksje - Iris jest opcjonalny)

	/** Wlacza paczke shaderow w Iris i przeladowuje shadery. */
	public static boolean irisApply(String packFile) {
		try {
			Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
			Object cfg = iris.getMethod("getIrisConfig").invoke(null);
			cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, packFile);
			cfg.getClass().getMethod("setShadersEnabled", boolean.class).invoke(cfg, true);
			cfg.getClass().getMethod("save").invoke(cfg);
			iris.getMethod("reload").invoke(null);
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	public static boolean irisDisable() {
		try {
			Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
			Object cfg = iris.getMethod("getIrisConfig").invoke(null);
			cfg.getClass().getMethod("setShadersEnabled", boolean.class).invoke(cfg, false);
			cfg.getClass().getMethod("save").invoke(cfg);
			iris.getMethod("reload").invoke(null);
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	/** Nazwa aktywnej paczki Iris albo null. */
	public static String irisActivePack() {
		try {
			Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
			Object cfg = iris.getMethod("getIrisConfig").invoke(null);
			boolean on = (boolean) cfg.getClass().getMethod("areShadersEnabled").invoke(cfg);
			if (!on) return null;
			Optional<?> name = (Optional<?>) cfg.getClass().getMethod("getShaderPackName").invoke(cfg);
			return name.map(Object::toString).orElse(null);
		} catch (Throwable t) {
			return null;
		}
	}

	public static Screen irisScreen(Screen parent) {
		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Object inst = api.getMethod("getInstance").invoke(null);
			Method m = api.getMethod("openMainIrisScreenObj", Object.class);
			return (Screen) m.invoke(inst, parent);
		} catch (Throwable t) {
			return null;
		}
	}

	// ================================================================ "co moj komp uciagnie"

	public static String gpuName() {
		try {
			String r = GlDebugInfo.getRenderer();
			return r == null ? "nieznana" : r;
		} catch (Throwable t) {
			return "nieznana";
		}
	}

	/** Prosta ocena: nazwa karty + aktualne FPS. */
	public static Tier recommend() {
		String g = gpuName().toLowerCase(Locale.ROOT);
		int fps = MinecraftClient.getInstance().getCurrentFps();
		int score;
		if (g.matches(".*(rtx|rx ?6[6-9]\\d\\d|rx ?7\\d\\d\\d|rx ?9\\d\\d\\d|arc a7|radeon pro w7).*")) score = 2;
		else if (g.matches(".*(gtx ?1[0-9]{3}|gtx ?9[6-8]0|rx ?5[5-9]\\d|rx ?5[67]00|rx ?6[45]\\d\\d|arc).*")) score = 1;
		else if (g.matches(".*(intel|uhd|iris|vega|radeon\\(tm\\) graphics|radeon graphics|llvmpipe|mali|adreno|apple|gt ?[0-9]{3}).*")) score = 0;
		else score = 1;
		if (fps > 0 && fps < 45) score = Math.min(score, 0);
		else if (fps > 0 && fps < 90) score = Math.min(score, 1);
		return Tier.values()[score];
	}
}
