package com.ziemniak.client;

import java.util.ArrayList;
import java.util.List;

import com.ziemniak.client.theme.FemboyTheme;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.widget.ClickableWidget;

public class ZiemniakClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
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
}
