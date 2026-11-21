package com.ziemniak.client.util;

import com.ziemniak.client.config.ZiemniakConfig;

import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/** Zoom pod klawiszem (domyslnie C), kolko myszy zmienia przyblizenie. */
public final class Zoom {
	private Zoom() {}

	public static KeyBinding key;
	private static boolean active;
	private static boolean savedSmooth;
	private static float extra = 1f;
	private static float current = 1f;
	private static long last = -1;

	public static void tick(MinecraftClient mc) {
		ZiemniakConfig cfg = ZiemniakConfig.get();
		boolean want = cfg.zoomEnabled && key != null && held(mc) && mc.currentScreen == null;
		if (want && !active) {
			active = true;
			extra = 1f;
			savedSmooth = mc.options.smoothCameraEnabled;
			if (cfg.zoomSmooth) mc.options.smoothCameraEnabled = true;
		} else if (!want && active) {
			active = false;
			mc.options.smoothCameraEnabled = savedSmooth;
		}
	}

	/** Stan klawisza czytany wprost (C koliduje w vanilli z "Zapisz pasek narzedzi"). */
	private static boolean held(MinecraftClient mc) {
		InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(key);
		if (k.getCategory() != InputUtil.Type.KEYSYM || k.getCode() < 0) return key.isPressed();
		return InputUtil.isKeyPressed(mc.getWindow().getHandle(), k.getCode());
	}

	public static boolean active() {
		return active;
	}

	/** true = przewijanie zuzyte przez zoom. */
	public static boolean onScroll(double amount) {
		if (!active) return false;
		extra = Math.max(0.5f, Math.min(5f, extra * (amount > 0 ? 1.2f : 1f / 1.2f)));
		return true;
	}

	/** Mnoznik dzielacy FOV, plynnie animowany. */
	public static float factor() {
		long now = System.currentTimeMillis();
		float dt = last < 0 ? 0f : Math.min(0.1f, (now - last) / 1000f);
		last = now;
		float target = active ? ZiemniakConfig.get().zoomLevel.mul * extra : 1f;
		current += (target - current) * Math.min(1f, dt * 12f);
		if (Math.abs(current - target) < 0.001f) current = target;
		return current;
	}
}
