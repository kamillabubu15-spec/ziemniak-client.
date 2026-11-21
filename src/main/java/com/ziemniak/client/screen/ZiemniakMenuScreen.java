package com.ziemniak.client.screen;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.ziemniak.client.config.Options;
import com.ziemniak.client.config.ZiemniakConfig;
import com.ziemniak.client.hud.HudEditorScreen;
import com.ziemniak.client.hud.HudManager;
import com.ziemniak.client.shader.ShaderManager;
import com.ziemniak.client.shader.ShaderManager.NetShader;
import com.ziemniak.client.shader.ShaderManager.OwnShader;
import com.ziemniak.client.shader.ShaderManager.State;
import com.ziemniak.client.shader.ShaderManager.Status;
import com.ziemniak.client.shader.ShaderManager.Tier;
import com.ziemniak.client.theme.FemboyTheme;
import com.ziemniak.client.theme.Palettes.Colors;
import com.ziemniak.client.theme.Shapes;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/** Menu moda: Shadery / Motyw / Animacje / Kursor i klik / Info. */
public class ZiemniakMenuScreen extends Screen {
	private static final String[] TABS = {"Shadery", "Motyw", "Animacje", "Kursor i klik", "HUD", "Narzedzia", "Info"};
	private static int lastTab = 0;

	private final Screen parent;
	private int tab = lastTab;
	private final List<ButtonWidget> tabButtons = new ArrayList<>();
	private final List<Row> rows = new ArrayList<>();
	private final Map<ButtonWidget, Supplier<String>> labels = new LinkedHashMap<>();
	private float scroll, scrollTarget;
	private long tabChanged;
	private int areaX0, areaY0, areaX1, areaY1, contentW, contentHeight;

	private static final class Row {
		int y, h, ry, dx;
		float alpha = 1f;
		String header;
		Supplier<String> text;
		int textColor = 0xFFFFFF;
		final List<ButtonWidget> widgets = new ArrayList<>();
		Supplier<String> side;
	}

	public ZiemniakMenuScreen(Screen parent) {
		super(Text.literal("Ziemniak Mod"));
		this.parent = parent;
	}

	// ================================================================ uklad

	@Override
	protected void init() {
		tabButtons.clear();
		labels.clear();
		rows.clear();
		int sideW = Math.min(100, Math.max(70, width / 5));
		areaX0 = 10 + sideW + 8;
		areaY0 = 28;
		areaX1 = width - 10;
		areaY1 = height - 30;
		contentW = areaX1 - areaX0 - 8;

		for (int i = 0; i < TABS.length; i++) {
			final int idx = i;
			ButtonWidget b = ButtonWidget.builder(Text.literal(TABS[i]), btn -> switchTab(idx))
					.dimensions(10, areaY0 + i * 23, sideW, 19).build();
			tabButtons.add(addDrawableChild(b));
			labels.put(b, () -> (tab == idx ? "♥ " : "") + TABS[idx]);
		}
		addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe ♡"), b -> close())
				.dimensions(width / 2 - 60, height - 24, 120, 18).build());
		FemboyTheme.markOpened(this);
		buildTab();
	}

	private void switchTab(int idx) {
		if (idx == tab) return;
		tab = idx;
		lastTab = idx;
		buildTab();
	}

	private void buildTab() {
		for (Row r : rows) for (ButtonWidget w : r.widgets) {
			remove(w);
			labels.remove(w);
		}
		rows.clear();
		scroll = scrollTarget = 0f;
		tabChanged = FemboyTheme.now();
		switch (tab) {
			case 0 -> buildShaders();
			case 1 -> buildTheme();
			case 2 -> buildAnimations();
			case 3 -> buildCursor();
			case 4 -> buildHud();
			case 5 -> buildTools();
			default -> buildInfo();
		}
		layout();
	}

	private void layout() {
		int y = 4;
		for (Row r : rows) {
			r.y = y;
			if (r.header != null) r.h = 16;
			else if (r.widgets.isEmpty()) r.h = Math.max(1, wrap(r.text.get()).size()) * 10 + 3;
			else r.h = 23;
			y += r.h;
		}
		contentHeight = y + 4;
	}

	private List<OrderedText> wrap(String s) {
		return textRenderer.wrapLines(StringVisitable.plain(s), contentW);
	}

	// ================================================================ budowanie wierszy

	private Row header(String text) {
		Row r = new Row();
		r.header = text;
		rows.add(r);
		return r;
	}

	private Row text(Supplier<String> text, int color) {
		Row r = new Row();
		r.text = text;
		r.textColor = color;
		rows.add(r);
		return r;
	}

	private Row text(String text) {
		return text(() -> text, 0xFFFFFF);
	}

	private ButtonWidget button(Supplier<String> label, Runnable onPress) {
		ButtonWidget b = ButtonWidget.builder(Text.literal(label.get()), btn -> onPress.run()).dimensions(0, 0, 100, 19).build();
		labels.put(b, label);
		return b;
	}

	private Row buttons(ButtonWidget... ws) {
		Row r = new Row();
		for (ButtonWidget w : ws) r.widgets.add(addDrawableChild(w));
		rows.add(r);
		return r;
	}

	private Row buttonWithSide(ButtonWidget w, Supplier<String> side) {
		Row r = buttons(w);
		r.side = side;
		return r;
	}

	private ButtonWidget toggle(String label, Function<ZiemniakConfig, Boolean> get, BiConsumer<ZiemniakConfig, Boolean> set) {
		return button(() -> label + ": " + (get.apply(ZiemniakConfig.get()) ? "TAK" : "NIE"), () -> {
			ZiemniakConfig c = ZiemniakConfig.get();
			set.accept(c, !get.apply(c));
			ZiemniakConfig.save();
		});
	}

	private <E extends Enum<E> & Options.Named> ButtonWidget cycle(String label, Function<ZiemniakConfig, E> get,
			BiConsumer<ZiemniakConfig, E> set) {
		return button(() -> label + ": " + get.apply(ZiemniakConfig.get()).label(), () -> {
			ZiemniakConfig c = ZiemniakConfig.get();
			set.accept(c, Options.next(get.apply(c)));
			ZiemniakConfig.save();
			if (get.apply(c) instanceof Options.InventoryOpen) FemboyTheme.resetOpened(this);
		});
	}

	// ================================================================ zakladki

	private void buildShaders() {
		header("Twoj komputer");
		text(() -> "Karta: " + ShaderManager.gpuName(), 0xFFFFFF);
		text(() -> "FPS teraz: " + client.getCurrentFps() + "   Polecam: " + ShaderManager.recommend().label + " shadery", 0xFFF3A8);

		header("Moje shadery (dzialaja od razu, bez Iris)");
		for (OwnShader s : ShaderManager.OWN) {
			buttonWithSide(button(() -> (s.id().equals(ZiemniakConfig.get().shader) ? "♥ " : "") + s.label(),
					() -> ShaderManager.select(s.id())), s::desc);
		}
		text(() -> client.world == null ? "Wejdz do swiata, zeby zobaczyc shader." : "Shader dziala na swiat, menu zostaje bez zmian.", 0xFFE0F2);

		header("Shadery z internetu (Modrinth, przez Iris)");
		if (ShaderManager.irisLoaded()) {
			text(() -> {
				String a = ShaderManager.irisActivePack();
				return "Iris: zainstalowany ✔   Aktywny: " + (a == null ? "brak" : a);
			}, 0xC8FFD8);
			buttons(button(() -> "Menu Iris", () -> {
				Screen s = ShaderManager.irisScreen(this);
				if (s != null) client.setScreen(s);
			}), button(() -> "Wylacz shader Iris", ShaderManager::irisDisable));
		} else {
			Status iris = ShaderManager.status("iris");
			buttonWithSide(button(() -> switch (iris.state) {
				case POBIERANIE -> "Pobieranie " + Math.round(iris.progress * 100) + "%";
				case GOTOWE -> "Pobrano ✔";
				case BLAD -> "Blad - ponow";
				default -> ShaderManager.irisPendingRestart() ? "Pobrano ✔" : "Pobierz Iris + Sodium";
			}, () -> {
				if (iris.state != State.GOTOWE && !ShaderManager.irisPendingRestart()) ShaderManager.installIris();
			}), () -> switch (iris.state) {
				case GOTOWE -> "Zrestartuj gre, zeby wlaczyc Iris";
				case BLAD -> "Blad: " + iris.error;
				default -> ShaderManager.irisPendingRestart() ? "Zrestartuj gre, zeby wlaczyc Iris" : "Potrzebny do shaderow z netu";
			});
		}

		Tier rec = ShaderManager.recommend();
		for (Tier tier : Tier.values()) {
			header(tier.label + " - " + tier.hint + (tier == rec ? "  ★ dla Ciebie" : ""));
			for (NetShader s : ShaderManager.NET) {
				if (s.tier() != tier) continue;
				Status st = ShaderManager.shaderStatus(s);
				buttonWithSide(button(() -> switch (st.state) {
					case POBIERANIE -> Math.round(st.progress * 100) + "%";
					case GOTOWE -> !ShaderManager.irisLoaded() ? "Pobrano ✔"
							: (st.file != null && st.file.equals(ShaderManager.irisActivePack()) ? "♥ Wlaczony" : "Wlacz");
					case BLAD -> "Ponow";
					default -> "Pobierz";
				}, () -> {
					switch (st.state) {
						case BRAK, BLAD -> ShaderManager.downloadShader(s);
						case GOTOWE -> {
							if (ShaderManager.irisLoaded() && st.file != null) {
								ShaderManager.select("brak");
								ShaderManager.irisApply(st.file);
							}
						}
						default -> { }
					}
				}), () -> s.name() + " - " + (st.state == State.BLAD ? "blad: " + st.error : s.desc()));
			}
		}
		buttons(button(() -> "Otworz folder shaderpacks", () -> {
			try {
				Files.createDirectories(ShaderManager.shaderpacksDir());
			} catch (Exception ignored) {
			}
			Util.getOperatingSystem().open(ShaderManager.shaderpacksDir().toFile());
		}));
	}

	private void buildTheme() {
		buttons(toggle("Motyw ESC i ekwipunku", c -> c.themeEnabled, (c, v) -> c.themeEnabled = v));
		buttons(cycle("Paleta", c -> c.palette, (c, v) -> c.palette = v),
				cycle("Tlo", c -> c.background, (c, v) -> c.background = v));
		buttons(cycle("Czasteczki", c -> c.particles, (c, v) -> c.particles = v),
				cycle("Ilosc", c -> c.density, (c, v) -> c.density = v));
		buttons(cycle("Przyciski", c -> c.buttonStyle, (c, v) -> c.buttonStyle = v),
				cycle("Predkosc", c -> c.speed, (c, v) -> c.speed = v));
		buttons(cycle("Ekwipunek", c -> c.inventoryOpen, (c, v) -> c.inventoryOpen = v));
		buttons(toggle("Ekran glowny", c -> c.themeTitleScreen, (c, v) -> c.themeTitleScreen = v),
				toggle("Wszystkie menu", c -> c.themeAllMenus, (c, v) -> c.themeAllMenus = v));
		buttons(button(() -> "✨ Losuj wszystko!", this::randomize), button(() -> "Reset", () -> {
			ZiemniakConfig.reset();
			FemboyTheme.resetOpened(this);
		}));
		text("Zmiany widac od razu - to menu tez uzywa motywu. Ekwipunek: otworz E, zeby zobaczyc animacje.");
	}

	private void buildAnimations() {
		buttons(toggle("Czasteczki tla", c -> c.bgParticles, (c, v) -> c.bgParticles = v),
				toggle("Iskierki", c -> c.sparkles, (c, v) -> c.sparkles = v));
		buttons(toggle("Spadajace gwiazdy", c -> c.shootingStars, (c, v) -> c.shootingStars = v),
				toggle("Wjazd przyciskow", c -> c.buttonSlideIn, (c, v) -> c.buttonSlideIn = v));
		buttons(toggle("Poswiata przyc.", c -> c.buttonGlow, (c, v) -> c.buttonGlow = v),
				toggle("Blysk przyc.", c -> c.buttonShine, (c, v) -> c.buttonShine = v));
		buttons(toggle("Serca przy napisie", c -> c.buttonHearts, (c, v) -> c.buttonHearts = v),
				toggle("Podskok przyc.", c -> c.buttonBounce, (c, v) -> c.buttonBounce = v));
		buttons(toggle("Serca przy tytule", c -> c.titleHearts, (c, v) -> c.titleHearts = v),
				toggle("Falujacy napis", c -> c.waveText, (c, v) -> c.waveText = v));
		buttons(toggle("Poswiata panelu", c -> c.panelGlow, (c, v) -> c.panelGlow = v),
				toggle("Kokardka", c -> c.panelBow, (c, v) -> c.panelBow = v));
		buttons(toggle("Serca w rogach", c -> c.panelCornerHearts, (c, v) -> c.panelCornerHearts = v),
				toggle("Ramka slotu", c -> c.slotHover, (c, v) -> c.slotHover = v));
		buttons(button(() -> "Wszystko TAK", () -> setAllAnimations(true)),
				button(() -> "Wszystko NIE", () -> setAllAnimations(false)));
	}

	private void buildCursor() {
		buttons(cycle("Efekt klikniecia", c -> c.clickEffect, (c, v) -> c.clickEffect = v));
		buttons(cycle("Slad kursora", c -> c.cursorTrail, (c, v) -> c.cursorTrail = v));
		buttons(toggle("Serduszko przy kursorze", c -> c.cursorHeart, (c, v) -> c.cursorHeart = v));
		text("Kliknij gdziekolwiek i ruszaj myszka, zeby przetestowac.");
	}

	private void buildInfo() {
		header("Ziemniak Client 1.0.0");
		text("Minecraft 1.21.4 (Fabric). Menu otwierasz przyciskiem \"Ziemniak Mod\" w ESC albo klawiszem Prawy Shift. Zoom: C.");
		text("Ustawienia zapisuja sie same w .minecraft/config/ziemniak-client.json.");
		header("Co jest w srodku");
		text(Options.Palette.values().length + " palet kolorow, " + (Options.Background.values().length - 1) + " rodzajow tla, "
				+ (Options.ParticleKind.values().length - 1) + " rodzajow czasteczek + mix, " + Options.ButtonStyle.values().length + " style przyciskow, "
				+ (Options.InventoryOpen.values().length - 1) + " animacje otwierania ekwipunku, " + (Options.ClickEffect.values().length - 1)
				+ " efektow klikniecia, " + (Options.CursorTrail.values().length - 1) + " sladow kursora, 14 przelacznikow animacji.");
		text((ShaderManager.OWN.size() - 1) + " wlasnych shaderow + " + ShaderManager.NET.size() + " shaderow z internetu do pobrania jednym kliknieciem.");
		text(HudManager.MODULES.size() + " modulow HUD z edytorem przeciagania, ostrzezenie o niskim HP, zoom, fullbright, "
				+ (Options.Crosshair.values().length - 1) + " wlasnych celownikow.");
		header("Shadery z internetu");
		text("Pobierane sa z Modrinth do folderu shaderpacks. Do ich dzialania potrzebny jest Iris "
				+ "(przycisk \"Pobierz Iris + Sodium\" w zakladce Shadery, potem restart gry).");
	}

	private void buildHud() {
		buttons(toggle("HUD wlaczony", c -> c.hudEnabled, (c, v) -> c.hudEnabled = v),
				toggle("Tlo modulow", c -> c.hudBackground, (c, v) -> c.hudBackground = v));
		buttons(cycle("Skala HUD", c -> c.hudScale, (c, v) -> c.hudScale = v),
				toggle("Ostrzezenie HP", c -> c.lowHpWarning, (c, v) -> c.lowHpWarning = v));
		buttons(button(() -> "\u270E Edytuj polozenie (przeciaganie)", () -> client.setScreen(new HudEditorScreen(this))));
		header("Moduly");
		List<HudManager.Module> mods = HudManager.MODULES;
		for (int i = 0; i < mods.size(); i += 2) {
			HudManager.Module a = mods.get(i);
			if (i + 1 < mods.size()) {
				HudManager.Module b = mods.get(i + 1);
				buttons(hudToggle(a), hudToggle(b));
			} else {
				buttons(hudToggle(a));
			}
		}
	}

	private ButtonWidget hudToggle(HudManager.Module m) {
		return button(() -> m.name + ": " + (HudManager.pos(m).on ? "TAK" : "NIE"), () -> {
			HudManager.pos(m).on = !HudManager.pos(m).on;
			ZiemniakConfig.save();
		});
	}

	private void buildTools() {
		header("Zoom");
		buttons(toggle("Zoom (klawisz C)", c -> c.zoomEnabled, (c, v) -> c.zoomEnabled = v),
				cycle("Przyblizenie", c -> c.zoomLevel, (c, v) -> c.zoomLevel = v));
		buttons(toggle("Plynna kamera przy zoomie", c -> c.zoomSmooth, (c, v) -> c.zoomSmooth = v));
		text("Trzymaj C, kolko myszy zmienia przyblizenie. Klawisz zmienisz w Opcje -> Sterowanie.");
		header("Widocznosc");
		buttons(toggle("Fullbright", c -> c.fullbright, (c, v) -> c.fullbright = v),
				cycle("Celownik", c -> c.crosshair, (c, v) -> c.crosshair = v));
		header("Ruch (opcje Minecrafta)");
		buttons(button(() -> "Sprint: " + (client.options.getSprintToggled().getValue() ? "Przelacz" : "Trzymaj"), () -> {
			client.options.getSprintToggled().setValue(!client.options.getSprintToggled().getValue());
			client.options.write();
		}), button(() -> "Skradanie: " + (client.options.getSneakToggled().getValue() ? "Przelacz" : "Trzymaj"), () -> {
			client.options.getSneakToggled().setValue(!client.options.getSneakToggled().getValue());
			client.options.write();
		}));
	}

	private void setAllAnimations(boolean v) {
		ZiemniakConfig c = ZiemniakConfig.get();
		c.bgParticles = c.sparkles = c.shootingStars = c.buttonSlideIn = c.buttonGlow = c.buttonShine = v;
		c.buttonHearts = c.buttonBounce = c.titleHearts = c.waveText = c.panelGlow = c.panelBow = v;
		c.panelCornerHearts = c.slotHover = v;
		ZiemniakConfig.save();
	}

	private void randomize() {
		Random r = new Random();
		ZiemniakConfig c = ZiemniakConfig.get();
		c.palette = rnd(r, Options.Palette.values());
		c.background = rnd(r, java.util.Arrays.stream(Options.Background.values())
				.filter(b -> b != Options.Background.BRAK).toArray(Options.Background[]::new));
		c.particles = rnd(r, Options.ParticleKind.values());
		c.buttonStyle = rnd(r, Options.ButtonStyle.values());
		c.inventoryOpen = rnd(r, new Options.InventoryOpen[]{Options.InventoryOpen.POP, Options.InventoryOpen.WJAZD,
				Options.InventoryOpen.OBROT, Options.InventoryOpen.SPREZYNA});
		c.clickEffect = rnd(r, java.util.Arrays.stream(Options.ClickEffect.values())
				.filter(e -> e != Options.ClickEffect.BRAK).toArray(Options.ClickEffect[]::new));
		c.cursorTrail = rnd(r, java.util.Arrays.stream(Options.CursorTrail.values())
				.filter(e -> e != Options.CursorTrail.BRAK).toArray(Options.CursorTrail[]::new));
		ZiemniakConfig.save();
		FemboyTheme.resetOpened(this);
	}

	private static <T> T rnd(Random r, T[] arr) {
		return arr[r.nextInt(arr.length)];
	}

	// ================================================================ rysowanie

	private void positionRows() {
		int max = Math.max(0, contentHeight - (areaY1 - areaY0));
		scrollTarget = Math.max(0, Math.min(scrollTarget, max));
		scroll += (scrollTarget - scroll) * 0.35f;
		float since = (FemboyTheme.now() - tabChanged) / 1000f;
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			float p = FemboyTheme.clamp01((since - i * 0.03f) / 0.35f);
			r.dx = Math.round((1f - FemboyTheme.easeOutBack(p)) * 40f);
			r.alpha = FemboyTheme.clamp01(p * 1.6f);
			r.ry = areaY0 + r.y - Math.round(scroll);
			if (r.widgets.isEmpty()) continue;
			int x = areaX0 + 4 + r.dx;
			int n = r.widgets.size();
			int total = r.side != null ? Math.round(contentW * 0.4f) : contentW;
			int gap = 4;
			int w = (total - gap * (n - 1)) / n;
			boolean visible = r.ry >= areaY0 && r.ry + 19 <= areaY1;
			for (ButtonWidget b : r.widgets) {
				b.setX(x);
				b.setY(r.ry);
				b.setWidth(w);
				b.visible = visible;
				b.setAlpha(r.alpha);
				x += w + gap;
			}
		}
		for (Map.Entry<ButtonWidget, Supplier<String>> e : labels.entrySet()) {
			String s = e.getValue().get();
			if (!s.equals(e.getKey().getMessage().getString())) e.getKey().setMessage(Text.literal(s));
		}
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.renderBackground(ctx, mouseX, mouseY, delta);
		FemboyTheme.drawBackground(ctx, this, width, height);
		Colors c = FemboyTheme.pal();
		float open = FemboyTheme.easeOutCubic(FemboyTheme.sinceOpen(this) / 0.4f);
		int sideX1 = tabButtons.isEmpty() ? 100 : tabButtons.get(0).getX() + tabButtons.get(0).getWidth();
		Shapes.fillRounded(ctx, 6, areaY0 - 4, sideX1 + 4, areaY0 + TABS.length * 23 + 2, FemboyTheme.argb(0.22f * open, FemboyTheme.WHITE));
		Shapes.fillRounded(ctx, areaX0, areaY0 - 4, areaX1, areaY1 + 2, FemboyTheme.argb(0.2f * open, FemboyTheme.WHITE));
		Shapes.outlineRounded(ctx, areaX0, areaY0 - 4, areaX1, areaY1 + 2, FemboyTheme.argb(0.6f * open, c.light()));
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		positionRows();
		super.render(ctx, mouseX, mouseY, delta);

		TextRenderer tr = textRenderer;
		Colors c = FemboyTheme.pal();
		float t = FemboyTheme.time();
		ctx.enableScissor(areaX0, areaY0 - 2, areaX1, areaY1);
		for (Row r : rows) {
			if (r.ry + r.h < areaY0 - 2 || r.ry > areaY1) continue;
			int a = Math.max(4, Math.round(r.alpha * 255f)) << 24;
			int x = areaX0 + 4 + r.dx;
			if (r.header != null) {
				float beat = 1f + (float) Math.abs(Math.sin(t * 3f + r.y)) * 0.2f;
				Shapes.draw(ctx, Shapes.HEART, x + 4, r.ry + 7, beat, FemboyTheme.argb(r.alpha, c.main()));
				ctx.drawText(tr, r.header, x + 13, r.ry + 4, a | (c.dark() & 0xFFFFFF), false);
				ctx.drawText(tr, r.header, x + 12, r.ry + 3, a | 0xFFFFFF, false);
				ctx.fill(x + 12, r.ry + 13, x + 12 + tr.getWidth(r.header), r.ry + 14, FemboyTheme.argb(r.alpha * 0.6f, c.light()));
			} else if (r.widgets.isEmpty()) {
				int y = r.ry + 1;
				for (OrderedText line : wrap(r.text.get())) {
					ctx.drawText(tr, line, x + 1, y + 1, a | (c.dark() & 0xFFFFFF), false);
					ctx.drawText(tr, line, x, y, a | r.textColor, false);
					y += 10;
				}
			} else if (r.side != null) {
				int sx = x + Math.round(contentW * 0.4f) + 6;
				String s = tr.trimToWidth(r.side.get(), areaX1 - sx - 6);
				ctx.drawText(tr, s, sx + 1, r.ry + 6, a | (c.dark() & 0xFFFFFF), false);
				ctx.drawText(tr, s, sx, r.ry + 5, a | 0xFFFFFF, false);
			}
		}
		ctx.disableScissor();

		// pasek przewijania
		int view = areaY1 - areaY0;
		if (contentHeight > view) {
			int barH = Math.max(12, view * view / contentHeight);
			int barY = areaY0 + Math.round((view - barH) * (scroll / (contentHeight - view)));
			Shapes.fillRounded(ctx, areaX1 - 4, barY, areaX1 - 1, barY + barH, FemboyTheme.argb(0.8f, c.main()));
		}

		FemboyTheme.drawWaveText(ctx, tr, "✿ Ziemniak Mod ✿", width / 2, 10, t, 1f);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scrollTarget -= (float) verticalAmount * 24f;
		return true;
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}
}
