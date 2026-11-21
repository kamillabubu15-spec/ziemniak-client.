package com.ziemniak.client.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ziemniak.client.config.ZiemniakConfig;
import com.ziemniak.client.config.ZiemniakConfig.HudPos;
import com.ziemniak.client.theme.FemboyTheme;
import com.ziemniak.client.theme.Palettes.Colors;
import com.ziemniak.client.theme.Shapes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Moduly HUD w stylu motywu. Pozycje (ulamki ekranu) i wlaczenie zapisuja sie w configu. */
public final class HudManager {
	private HudManager() {}

	public abstract static class Module {
		public final String id;
		public final String name;
		final boolean defOn;
		final float defX, defY;

		Module(String id, String name, boolean defOn, float defX, float defY) {
			this.id = id;
			this.name = name;
			this.defOn = defOn;
			this.defX = defX;
			this.defY = defY;
		}

		/** Szerokosc i wysokosc w pikselach GUI (przed skala HUD). */
		abstract int[] size(MinecraftClient mc);

		abstract void draw(DrawContext ctx, MinecraftClient mc, int w, int h, Colors c);
	}

	/** Modul z linijkami tekstu. */
	private abstract static class TextModule extends Module {
		TextModule(String id, String name, boolean defOn, float x, float y) {
			super(id, name, defOn, x, y);
		}

		abstract List<String> lines(MinecraftClient mc);

		@Override
		int[] size(MinecraftClient mc) {
			int w = 0;
			List<String> ls = lines(mc);
			for (String l : ls) w = Math.max(w, mc.textRenderer.getWidth(l));
			return new int[]{w + 14, ls.size() * 10 + 5};
		}

		@Override
		void draw(DrawContext ctx, MinecraftClient mc, int w, int h, Colors c) {
			int y = 3;
			float t = FemboyTheme.time();
			Shapes.draw(ctx, Shapes.HEART, 5.5f, 7.5f, 0.8f + (float) Math.abs(Math.sin(t * 3)) * 0.15f, FemboyTheme.argb(1f, c.main()));
			for (String l : lines(mc)) {
				text(ctx, mc.textRenderer, l, 11, y, c);
				y += 10;
			}
		}
	}

	public static final List<Module> MODULES = new ArrayList<>();

	// stan do liczenia CPS, predkosci i dystansu
	private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<>();
	private static final Map<String, Float> KEY_ANIM = new LinkedHashMap<>();
	private static Vec3d lastPos;
	private static double speed;
	private static double distance;
	private static final long SESSION_START = Util.getMeasuringTimeMs();
	private static int combo;
	private static long lastHit;
	private static LivingEntity lastTarget;
	private static long lastTargetTime;

	static {
		MODULES.add(new TextModule("fps", "FPS", true, 0.01f, 0.01f) {
			List<String> lines(MinecraftClient mc) {
				return List.of(mc.getCurrentFps() + " FPS");
			}
		});
		MODULES.add(new TextModule("ping", "Ping", false, 0.01f, 0.07f) {
			List<String> lines(MinecraftClient mc) {
				int ping = 0;
				if (mc.getNetworkHandler() != null && mc.player != null) {
					PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
					if (e != null) ping = e.getLatency();
				}
				return List.of("Ping: " + ping + " ms");
			}
		});
		MODULES.add(new TextModule("coords", "Koordynaty", true, 0.01f, 0.13f) {
			List<String> lines(MinecraftClient mc) {
				ClientPlayerEntity p = mc.player;
				if (p == null) return List.of("XYZ: -");
				BlockPos b = p.getBlockPos();
				String biome = mc.world == null ? "?" : mc.world.getBiome(b).getKey()
						.map(k -> k.getValue().getPath().replace('_', ' ')).orElse("?");
				return List.of("XYZ: " + b.getX() + " " + b.getY() + " " + b.getZ(),
						"Kierunek: " + direction(p.getYaw()), "Biom: " + biome);
			}
		});
		MODULES.add(new TextModule("clock", "Zegar", false, 1f, 0.01f) {
			List<String> lines(MinecraftClient mc) {
				String real = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
				if (mc.world == null) return List.of("Godzina: " + real);
				long tod = mc.world.getTimeOfDay();
				long day = tod / 24000L + 1;
				int hours = (int) ((tod % 24000L) / 1000L + 6) % 24;
				int mins = (int) ((tod % 1000L) * 60L / 1000L);
				return List.of("Godzina: " + real, String.format("Dzien %d, %02d:%02d", day, hours, mins));
			}
		});
		MODULES.add(new TextModule("speed", "Predkosc", false, 0.01f, 0.3f) {
			List<String> lines(MinecraftClient mc) {
				return List.of(String.format("Predkosc: %.1f b/s", speed));
			}
		});
		MODULES.add(new TextModule("session", "Sesja", false, 0.01f, 0.38f) {
			List<String> lines(MinecraftClient mc) {
				long s = (Util.getMeasuringTimeMs() - SESSION_START) / 1000L;
				return List.of(String.format("Sesja: %d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60),
						String.format("Dystans: %.0f m", distance));
			}
		});
		MODULES.add(new TextModule("effects", "Efekty mikstur", true, 1f, 0.16f) {
			List<String> lines(MinecraftClient mc) {
				List<String> out = new ArrayList<>();
				if (mc.player != null) {
					for (StatusEffectInstance e : mc.player.getStatusEffects()) {
						String name = e.getEffectType().value().getName().getString();
						String lvl = e.getAmplifier() > 0 ? " " + roman(e.getAmplifier() + 1) : "";
						String time = e.isInfinite() ? "**:**" : String.format("%d:%02d", e.getDuration() / 1200, (e.getDuration() / 20) % 60);
						out.add(name + lvl + "  " + time);
					}
				}
				if (out.isEmpty()) out.add("Brak efektow");
				return out;
			}
		});
		MODULES.add(new Module("keys", "Klawisze + CPS", true, 0.01f, 0.62f) {
			int[] size(MinecraftClient mc) {
				return new int[]{62, 76};
			}

			void draw(DrawContext ctx, MinecraftClient mc, int w, int h, Colors c) {
				var o = mc.options;
				key(ctx, mc, "W", o.forwardKey, 22, 2, 18, 18, c);
				key(ctx, mc, "A", o.leftKey, 2, 22, 18, 18, c);
				key(ctx, mc, "S", o.backKey, 22, 22, 18, 18, c);
				key(ctx, mc, "D", o.rightKey, 42, 22, 18, 18, c);
				key(ctx, mc, "LPM " + cps(LEFT_CLICKS), o.attackKey, 2, 42, 28, 18, c);
				key(ctx, mc, "PPM " + cps(RIGHT_CLICKS), o.useKey, 32, 42, 28, 18, c);
				key(ctx, mc, "♥", o.jumpKey, 2, 62, 58, 12, c);
			}
		});
		MODULES.add(new Module("armor", "Zbroja", true, 1f, 0.42f) {
			final EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
					EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

			int[] size(MinecraftClient mc) {
				return new int[]{58, 6 * 17 + 4};
			}

			void draw(DrawContext ctx, MinecraftClient mc, int w, int h, Colors c) {
				if (mc.player == null) return;
				int y = 2;
				for (EquipmentSlot s : slots) {
					ItemStack st = mc.player.getEquippedStack(s);
					if (!st.isEmpty()) {
						ctx.drawItem(st, 3, y);
						String txt;
						int col = 0xFFFFFF;
						if (st.isDamageable()) {
							float left = 1f - st.getDamage() / (float) st.getMaxDamage();
							txt = Math.round(left * 100) + "%";
							col = left > 0.5f ? 0xB8FFC8 : (left > 0.2f ? 0xFFF3A8 : 0xFF8A9A);
						} else {
							txt = "x" + st.getCount();
						}
						text(ctx, mc.textRenderer, txt, 22, y + 4, c, col);
					}
					y += 17;
				}
			}
		});
		MODULES.add(new Module("items", "Licznik przedmiotow", false, 0.38f, 0.7f) {
			final Item[] items = {Items.TOTEM_OF_UNDYING, Items.ENDER_PEARL, Items.GOLDEN_APPLE, Items.ARROW, Items.EXPERIENCE_BOTTLE};

			int[] size(MinecraftClient mc) {
				return new int[]{items.length * 28 + 2, 20};
			}

			void draw(DrawContext ctx, MinecraftClient mc, int w, int h, Colors c) {
				if (mc.player == null) return;
				int x = 2;
				for (Item it : items) {
					int n = mc.player.getInventory().count(it);
					if (it == Items.GOLDEN_APPLE) n += mc.player.getInventory().count(Items.ENCHANTED_GOLDEN_APPLE);
					ctx.drawItem(new ItemStack(it), x, 2);
					text(ctx, mc.textRenderer, String.valueOf(n), x + 16, 10, c, n > 0 ? 0xFFFFFF : 0x9A8A96);
					x += 28;
				}
			}
		});
		MODULES.add(new Module("target", "Cel (HP)", true, 0.42f, 0.09f) {
			int[] size(MinecraftClient mc) {
				LivingEntity e = target(mc);
				int w = e == null ? 90 : Math.max(90, mc.textRenderer.getWidth(e.getName().getString()) + 16);
				return new int[]{w, 24};
			}

			void draw(DrawContext ctx, MinecraftClient mc, int w, int h, Colors c) {
				LivingEntity e = target(mc);
				if (e == null) {
					text(ctx, mc.textRenderer, "Brak celu", 6, 8, c, 0xD8C8D4);
					return;
				}
				text(ctx, mc.textRenderer, e.getName().getString(), 6, 3, c);
				float hp = MathHelper.clamp(e.getHealth() / Math.max(1f, e.getMaxHealth()), 0f, 1f);
				Shapes.fillRounded(ctx, 5, 14, w - 5, 20, FemboyTheme.argb(0.7f, 0x201020));
				ctx.fillGradient(6, 15, 6 + Math.round((w - 12) * hp), 19, FemboyTheme.argb(1f, c.light()), FemboyTheme.argb(1f, c.main()));
				String hpTxt = String.format("%.1f\u2665", e.getHealth());
				text(ctx, mc.textRenderer, hpTxt, w - 6 - mc.textRenderer.getWidth(hpTxt), 3, c, 0xFFC2EC);
			}
		});
		MODULES.add(new TextModule("combo", "Combo", true, 0.45f, 0.2f) {
			List<String> lines(MinecraftClient mc) {
				if (Util.getMeasuringTimeMs() - lastHit > 2500L) combo = 0;
				return List.of("Combo: " + combo + (combo >= 5 ? " \u2665\u2665" : (combo >= 3 ? " \u2665" : "")));
			}
		});
		MODULES.add(new Module("compass", "Kompas", false, 0.37f, 0.005f) {
			int[] size(MinecraftClient mc) {
				return new int[]{160, 16};
			}

			void draw(DrawContext ctx, MinecraftClient mc, int w, int h, Colors c) {
				if (mc.player == null) return;
				float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
				String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
				ctx.enableScissor(0, 0, w, h);
				for (int i = -8; i <= 16; i++) {
					float ang = i * 45f;
					float dx = MathHelper.wrapDegrees(ang - yaw) * 1.6f;
					int x = Math.round(w / 2f + dx);
					if (x < -10 || x > w + 10) continue;
					String n = names[Math.floorMod(i, 8)];
					int col = n.equals("N") ? c.main() : 0xFFFFFF;
					text(ctx, mc.textRenderer, n, x - mc.textRenderer.getWidth(n) / 2, 4, c, col);
					for (int k = 1; k < 3; k++) {
						int tx = Math.round(w / 2f + dx + k * 24f);
						ctx.fill(tx, 6, tx + 1, 10, FemboyTheme.argb(0.6f, 0xFFFFFF));
					}
				}
				ctx.disableScissor();
				Shapes.draw(ctx, Shapes.HEART, w / 2f, 1.5f, 0.6f, FemboyTheme.argb(1f, c.main()));
			}
		});
	}

	// ================================================================ pomocnicze

	static void text(DrawContext ctx, TextRenderer tr, String s, int x, int y, Colors c) {
		text(ctx, tr, s, x, y, c, 0xFFFFFF);
	}

	static void text(DrawContext ctx, TextRenderer tr, String s, int x, int y, Colors c, int color) {
		ctx.drawText(tr, s, x + 1, y + 1, 0xFF000000 | (c.dark() & 0xFFFFFF), false);
		ctx.drawText(tr, s, x, y, 0xFF000000 | color, false);
	}

	private static void key(DrawContext ctx, MinecraftClient mc, String label, KeyBinding kb, int x, int y, int w, int h, Colors c) {
		float target = kb.isPressed() ? 1f : 0f;
		float cur = KEY_ANIM.getOrDefault(label.split(" ")[0], 0f);
		cur += (target - cur) * 0.35f;
		KEY_ANIM.put(label.split(" ")[0], cur);
		int bg = FemboyTheme.lerpRgb(cur, 0x2A1A30, c.main());
		Shapes.fillRounded(ctx, x, y, x + w, y + h, FemboyTheme.argb(0.55f + 0.4f * cur, bg));
		Shapes.outlineRounded(ctx, x, y, x + w, y + h, FemboyTheme.argb(0.5f + 0.5f * cur, cur > 0.5f ? 0xFFFFFF : c.light()));
		TextRenderer tr = mc.textRenderer;
		text(ctx, tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2, c);
	}

	private static String cps(ArrayDeque<Long> q) {
		long now = Util.getMeasuringTimeMs();
		while (!q.isEmpty() && now - q.peekFirst() > 1000L) q.removeFirst();
		return String.valueOf(q.size());
	}

	/** Uderzenie gracza (AttackEntityCallback): licznik combo i zapamietany cel. */
	public static void onAttack(Entity e) {
		long now = Util.getMeasuringTimeMs();
		combo = now - lastHit > 2500L ? 1 : combo + 1;
		lastHit = now;
		if (e instanceof LivingEntity le) {
			lastTarget = le;
			lastTargetTime = now;
		}
	}

	private static LivingEntity target(MinecraftClient mc) {
		if (mc.targetedEntity instanceof LivingEntity le) return le;
		if (lastTarget != null && lastTarget.isAlive() && Util.getMeasuringTimeMs() - lastTargetTime < 3000L) return lastTarget;
		return null;
	}

	public static void onClick(int button) {
		long now = Util.getMeasuringTimeMs();
		if (button == 0) LEFT_CLICKS.addLast(now);
		else if (button == 1) RIGHT_CLICKS.addLast(now);
	}

	private static String direction(float yaw) {
		String[] d = {"Poludnie (S)", "Pld-zach (SW)", "Zachod (W)", "Pln-zach (NW)", "Polnoc (N)", "Pln-wsch (NE)", "Wschod (E)", "Pld-wsch (SE)"};
		int i = Math.floorMod(Math.round(MathHelper.wrapDegrees(yaw) / 45f), 8);
		return d[i];
	}

	private static String roman(int n) {
		String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
		return n < r.length ? r[n] : String.valueOf(n);
	}

	public static void tick(MinecraftClient mc) {
		if (mc.player == null) {
			lastPos = null;
			return;
		}
		Vec3d pos = mc.player.getPos();
		if (lastPos != null) {
			double d = Math.sqrt(Math.pow(pos.x - lastPos.x, 2) + Math.pow(pos.z - lastPos.z, 2));
			if (d < 20) {
				speed = speed * 0.6 + d * 20.0 * 0.4;
				distance += d;
			}
		}
		lastPos = pos;
	}

	// ================================================================ pozycje

	public static HudPos pos(Module m) {
		return ZiemniakConfig.get().hud.computeIfAbsent(m.id, k -> new HudPos(m.defOn, m.defX, m.defY));
	}

	public static void resetPositions() {
		for (Module m : MODULES) {
			HudPos p = pos(m);
			p.x = m.defX;
			p.y = m.defY;
		}
		ZiemniakConfig.save();
	}

	/** Prostokat modulu na ekranie: x, y, w, h (juz ze skala). */
	public static int[] rect(Module m, MinecraftClient mc) {
		float scale = ZiemniakConfig.get().hudScale.mul;
		int[] s = m.size(mc);
		int w = Math.round(s[0] * scale), h = Math.round(s[1] * scale);
		int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
		HudPos p = pos(m);
		int x = MathHelper.clamp(Math.round(p.x * sw), 0, Math.max(0, sw - w));
		int y = MathHelper.clamp(Math.round(p.y * sh), 0, Math.max(0, sh - h));
		return new int[]{x, y, w, h};
	}

	public static void drawModule(DrawContext ctx, MinecraftClient mc, Module m, boolean editor) {
		ZiemniakConfig cfg = ZiemniakConfig.get();
		Colors c = FemboyTheme.pal();
		int[] r = rect(m, mc);
		float scale = cfg.hudScale.mul;
		int[] s = m.size(mc);
		var mat = ctx.getMatrices();
		mat.push();
		mat.translate(r[0], r[1], 0f);
		mat.scale(scale, scale, 1f);
		if (cfg.hudBackground || editor) {
			Shapes.fillRounded(ctx, 0, 0, s[0], s[1], FemboyTheme.argb(0.5f, FemboyTheme.lerpRgb(0.55f, c.dark(), 0x000000)));
			ctx.fill(1, 0, s[0] - 1, 1, FemboyTheme.argb(0.9f, c.main()));
		}
		m.draw(ctx, mc, s[0], s[1], c);
		mat.pop();
	}

	/** Rysowanie HUD w grze. */
	public static void render(DrawContext ctx) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ZiemniakConfig cfg = ZiemniakConfig.get();
		if (mc.options.hudHidden || mc.player == null) return;
		if (cfg.lowHpWarning) drawLowHp(ctx, mc);
		if (!cfg.hudEnabled || mc.getDebugHud().shouldShowDebugHud()) return;
		if (mc.currentScreen instanceof HudEditorScreen) return;
		for (Module m : MODULES) {
			if (pos(m).on) drawModule(ctx, mc, m, false);
		}
	}

	private static void drawLowHp(DrawContext ctx, MinecraftClient mc) {
		float hp = mc.player.getHealth() / Math.max(1f, mc.player.getMaxHealth());
		if (hp > 0.3f || mc.player.isCreative() || mc.player.isSpectator()) return;
		float t = FemboyTheme.time();
		float beat = (float) Math.pow(Math.abs(Math.sin(t * 4.0)), 3.0);
		float a = (0.25f + 0.35f * beat) * (1f - hp / 0.3f * 0.5f);
		int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
		int col = 0xFF4F7A;
		int band = Math.max(20, h / 6);
		ctx.fillGradient(0, 0, w, band, FemboyTheme.argb(a, col), FemboyTheme.argb(0f, col));
		ctx.fillGradient(0, h - band, w, h, FemboyTheme.argb(0f, col), FemboyTheme.argb(a, col));
		for (int i = 0; i < band; i += 2) {
			float f = a * (1f - i / (float) band);
			ctx.fill(i, 0, i + 2, h, FemboyTheme.argb(f, col));
			ctx.fill(w - i - 2, 0, w - i, h, FemboyTheme.argb(f, col));
		}
		Shapes.draw(ctx, Shapes.HEART, w / 2f, h / 2f - 22f, 1.6f + beat * 0.6f, FemboyTheme.argb(0.5f + 0.5f * beat, col));
	}
}
