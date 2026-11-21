package com.ziemniak.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.ziemniak.client.config.ZiemniakConfig;
import com.ziemniak.client.screen.ZiemniakMenuScreen;
import com.ziemniak.client.theme.FemboyTheme;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

public class ZiemniakClient implements ClientModInitializer {
	public static KeyBinding menuKey;

	@Override
	public void onInitializeClient() {
		ZiemniakConfig.load();

		menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.ziemniak.menu",
				InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.ziemniak"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (menuKey.wasPressed()) {
				if (client.currentScreen == null) client.setScreen(new ZiemniakMenuScreen(null));
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof GameMenuScreen gms && gms.shouldShowMenu()) addModButton(screen);
			if (!FemboyTheme.isThemed(screen)) return;
			FemboyTheme.markOpened(screen);

			ScreenMouseEvents.afterMouseClick(screen).register((s, mx, my, button) -> FemboyTheme.addBurst(mx, my));
			ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> FemboyTheme.drawOverlay(ctx, s, mx, my));

			if (FemboyTheme.isPause(screen)) {
				List<ClickableWidget> buttons = new ArrayList<>(Screens.getButtons(screen));
				int[] origX = new int[buttons.size()];
				for (int i = 0; i < origX.length; i++) origX[i] = buttons.get(i).getX();
				ScreenEvents.beforeRender(screen).register((s, ctx, mx, my, delta) -> FemboyTheme.animateButtons(s, buttons, origX));
			}
		});
	}

	/** Przycisk "Ziemniak Mod" w menu ESC - w miejscu "Give Feedback" + "Report Bugs" (albo pod menu). */
	private static void addModButton(Screen screen) {
		List<ClickableWidget> buttons = Screens.getButtons(screen);
		ClickableWidget feedback = find(buttons, "menu.sendFeedback");
		ClickableWidget bugs = find(buttons, "menu.reportBugs");
		ButtonWidget.PressAction open = b -> MinecraftClient.getInstance().setScreen(new ZiemniakMenuScreen(screen));
		Text label = Text.literal("✿ Ziemniak Mod ✿");

		int x, y, w;
		int index;
		if (feedback != null && bugs != null) {
			x = feedback.getX();
			y = feedback.getY();
			w = bugs.getX() + bugs.getWidth() - x;
			index = buttons.indexOf(feedback);
			buttons.remove(bugs);
			buttons.remove(feedback);
		} else {
			int bottom = 0, left = screen.width / 2 - 102;
			for (ClickableWidget b : buttons) {
				bottom = Math.max(bottom, b.getY() + b.getHeight());
			}
			x = left;
			y = bottom + 4;
			w = 204;
			index = buttons.size();
		}
		buttons.add(Math.min(index, buttons.size()), ButtonWidget.builder(label, open).dimensions(x, y, w, 20).build());
	}

	private static ClickableWidget find(List<ClickableWidget> buttons, String key) {
		for (ClickableWidget b : buttons) {
			if (b.getMessage().getContent() instanceof TranslatableTextContent t && t.getKey().equals(key)) return b;
		}
		return null;
	}
}
