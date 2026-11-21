package com.ziemniak.client.sound;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.ziemniak.client.config.Options;
import com.ziemniak.client.config.ZiemniakConfig;
import com.ziemniak.client.hud.HudEditorScreen;
import com.ziemniak.client.screen.ZiemniakMenuScreen;
import com.ziemniak.client.theme.FemboyTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * Dzwieki moda. Dwa zestawy: "Kawaii" (wlasne, syntezowane: assets/ziemniak/sounds)
 * i "Minecraft" (dzwieki z gry). Kazdy dzwiek nalezy do kategorii, ktora mozna wylaczyc.
 */
public final class Sounds {
	private Sounds() {}

	public enum Cat { UI, EKWIPUNEK, EFEKTY, GRA, SHADERY }

	public enum Sfx {
		HOVER("ui.hover", Cat.UI, 0.5f, SoundEvents.BLOCK_NOTE_BLOCK_HAT, 2.0f),
		CLICK("ui.click", Cat.UI, 0.8f, SoundEvents.UI_BUTTON_CLICK, 1.0f),
		TOGGLE_ON("ui.toggle_on", Cat.UI, 0.8f, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.6f),
		TOGGLE_OFF("ui.toggle_off", Cat.UI, 0.8f, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.8f),
		TAB("ui.tab", Cat.UI, 0.8f, SoundEvents.ITEM_BOOK_PAGE_TURN, 1.2f),
		OPEN("ui.open", Cat.UI, 0.7f, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f),
		CLOSE("ui.close", Cat.UI, 0.6f, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, 1.2f),
		RANDOM("ui.random", Cat.UI, 0.8f, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.4f),
		RESET("ui.reset", Cat.UI, 0.8f, SoundEvents.BLOCK_NOTE_BLOCK_BASS, 1.2f),
		ERROR("ui.error", Cat.UI, 0.8f, SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f),
		INV_OPEN("inv.open", Cat.EKWIPUNEK, 0.7f, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 1.3f),
		INV_CLOSE("inv.close", Cat.EKWIPUNEK, 0.6f, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 1.3f),
		SLOT("inv.slot", Cat.EKWIPUNEK, 0.45f, SoundEvents.BLOCK_NOTE_BLOCK_HAT, 2.0f),
		FX_HEARTS("fx.hearts", Cat.EFEKTY, 0.6f, SoundEvents.ENTITY_ITEM_PICKUP, 1.5f),
		FX_STARS("fx.stars", Cat.EFEKTY, 0.6f, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.6f),
		FX_WAVE("fx.wave", Cat.EFEKTY, 0.6f, SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1.0f),
		FX_CONFETTI("fx.confetti", Cat.EFEKTY, 0.6f, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1.6f),
		FX_FLOWERS("fx.flowers", Cat.EFEKTY, 0.6f, SoundEvents.BLOCK_CHERRY_LEAVES_PLACE, 1.4f),
		FX_BIG_HEART("fx.big_heart", Cat.EFEKTY, 0.6f, SoundEvents.ENTITY_SLIME_JUMP_SMALL, 1.5f),
		FX_SPARKS("fx.sparks", Cat.EFEKTY, 0.6f, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.6f),
		COMBO("game.combo", Cat.GRA, 0.6f, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f),
		HEARTBEAT("game.heartbeat", Cat.GRA, 0.9f, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.2f),
		ZOOM_IN("game.zoom_in", Cat.GRA, 0.6f, SoundEvents.ITEM_SPYGLASS_USE, 1.0f),
		ZOOM_OUT("game.zoom_out", Cat.GRA, 0.6f, SoundEvents.ITEM_SPYGLASS_STOP_USING, 1.0f),
		SHADER_APPLY("shader.apply", Cat.SHADERY, 0.7f, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.4f),
		SHADER_OFF("shader.off", Cat.SHADERY, 0.7f, SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.4f),
		DL_START("dl.start", Cat.SHADERY, 0.7f, SoundEvents.BLOCK_NOTE_BLOCK_BIT, 1.4f),
		DL_DONE("dl.done", Cat.SHADERY, 0.8f, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f),
		HUD_PICK("hud.pick", Cat.UI, 0.7f, SoundEvents.ENTITY_ITEM_PICKUP, 1.2f),
		HUD_DROP("hud.drop", Cat.UI, 0.7f, SoundEvents.BLOCK_WOOL_PLACE, 1.2f);

		public final SoundEvent own;
		public final Cat cat;
		final float volume;
		private final Object vanillaRef;
		final float vanillaPitch;

		Sfx(String id, Cat cat, float volume, Object vanilla, float vanillaPitch) {
			this.own = SoundEvent.of(Identifier.of("ziemniak", id));
			this.cat = cat;
			this.volume = volume;
			this.vanillaRef = vanilla;
			this.vanillaPitch = vanillaPitch;
		}

		@SuppressWarnings("unchecked")
		SoundEvent vanilla() {
			return vanillaRef instanceof RegistryEntry<?> r ? ((RegistryEntry<SoundEvent>) r).value() : (SoundEvent) vanillaRef;
		}
	}

	public static final SoundEvent MUSIC_BOX = SoundEvent.of(Identifier.of("ziemniak", "music.musicbox"));

	/** Przyciski, ktore graja wlasny dzwiek (przelacznik, zakladka) zamiast zwyklego klikniecia. */
	private static final Set<ClickableWidget> SILENT = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<Sfx, Long> LAST = new java.util.EnumMap<>(Sfx.class);

	public static void silence(ClickableWidget w) {
		SILENT.add(w);
	}

	public static boolean isSilent(ClickableWidget w) {
		return SILENT.contains(w);
	}

	public static boolean enabled(Cat cat) {
		ZiemniakConfig c = ZiemniakConfig.get();
		if (!c.soundsEnabled) return false;
		return switch (cat) {
			case UI -> c.soundUi;
			case EKWIPUNEK -> c.soundInventory;
			case EFEKTY -> c.soundEffects;
			case GRA -> c.soundGame;
			case SHADERY -> c.soundShaders;
		};
	}

	public static void play(Sfx s) {
		play(s, 1f);
	}

	public static void play(Sfx s, float pitch) {
		if (!enabled(s.cat)) return;
		long now = Util.getMeasuringTimeMs();
		Long last = LAST.get(s);
		if (last != null && now - last < 30L) return; // bez "karabinu" przy szybkich zdarzeniach
		LAST.put(s, now);
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!mc.isOnThread()) {
			mc.execute(() -> play(s, pitch));
			return;
		}
		ZiemniakConfig c = ZiemniakConfig.get();
		float vol = s.volume * c.soundVolume.mul;
		boolean vanilla = c.soundPack == Options.SoundPack.MINECRAFT;
		SoundEvent ev = vanilla ? s.vanilla() : s.own;
		float p = pitch * (vanilla ? s.vanillaPitch : 1f);
		mc.getSoundManager().play(PositionedSoundInstance.master(ev, p, vol));
	}

	public static void clickEffect(Options.ClickEffect kind) {
		switch (kind) {
			case SERDUSZKA -> play(Sfx.FX_HEARTS);
			case GWIAZDKI -> play(Sfx.FX_STARS);
			case FALA -> play(Sfx.FX_WAVE);
			case KONFETTI -> play(Sfx.FX_CONFETTI);
			case KWIATKI -> play(Sfx.FX_FLOWERS);
			case DUZE_SERCE -> play(Sfx.FX_BIG_HEART);
			case ISKRY -> play(Sfx.FX_SPARKS);
			default -> { }
		}
	}

	// ================================================================ przejscia miedzy ekranami

	private static Screen lastScreen;

	/** Wolane co tick: dzwiek otwarcia / zamkniecia menu i ekwipunku. */
	public static void tick(MinecraftClient mc) {
		Screen cur = mc.currentScreen;
		if (cur != lastScreen) {
			Screen prev = lastScreen;
			lastScreen = cur;
			onScreenChange(prev, cur);
		}
		tickMusic(mc);
	}

	private static void onScreenChange(Screen prev, Screen cur) {
		boolean prevMenu = prev instanceof GameMenuScreen || prev instanceof ZiemniakMenuScreen;
		boolean curMenu = cur instanceof GameMenuScreen || cur instanceof ZiemniakMenuScreen;
		if (cur instanceof HandledScreen<?> && !(prev instanceof HandledScreen<?>)) play(Sfx.INV_OPEN);
		else if (prev instanceof HandledScreen<?> && !(cur instanceof HandledScreen<?>)) play(Sfx.INV_CLOSE);
		else if (cur instanceof ZiemniakMenuScreen && !(prev instanceof ZiemniakMenuScreen)) play(Sfx.OPEN);
		else if (curMenu && !prevMenu && prev == null) play(Sfx.OPEN);
		else if (prevMenu && cur == null) play(Sfx.CLOSE);
		else if (cur instanceof HudEditorScreen) play(Sfx.TAB);
	}

	// ================================================================ pozytywka w menu

	private static final class MusicBox extends AbstractSoundInstance {
		MusicBox(float volume) {
			super(MUSIC_BOX, SoundCategory.MUSIC, SoundInstance.createRandom());
			this.repeat = true;
			this.repeatDelay = 0;
			this.relative = true;
			this.attenuationType = SoundInstance.AttenuationType.NONE;
			this.volume = volume;
		}
	}

	private static MusicBox music;

	private static void tickMusic(MinecraftClient mc) {
		ZiemniakConfig c = ZiemniakConfig.get();
		Screen s = mc.currentScreen;
		boolean want = c.soundsEnabled && c.menuMusic && s != null && FemboyTheme.stylesButtons(s);
		var sm = mc.getSoundManager();
		if (want) {
			if (music == null || !sm.isPlaying(music)) {
				music = new MusicBox(0.6f * c.soundVolume.mul);
				sm.play(music);
			}
			mc.getMusicTracker().stop();
		} else if (music != null) {
			sm.stop(music);
			music = null;
		}
	}

	/** Po zmianie glosnosci / zestawu: zrestartuj pozytywke. */
	public static void restartMusic() {
		if (music != null) {
			MinecraftClient.getInstance().getSoundManager().stop(music);
			music = null;
		}
	}
}
