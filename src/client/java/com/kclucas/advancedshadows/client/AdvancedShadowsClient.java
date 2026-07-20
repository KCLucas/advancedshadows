package com.kclucas.advancedshadows.client;

import com.kclucas.advancedshadows.AdvancedShadows;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AdvancedShadowsClient implements ClientModInitializer {

	public static boolean overlayEnabled = false;
	private static KeyBinding toggleKey;
	private static final KeyBinding.Category CATEGORY =
			KeyBinding.Category.create(Identifier.of("advancedshadows", "general"));

	@Override
	public void onInitializeClient() {
		ModConfig.load();

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.advancedshadows.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_F7,
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.wasPressed()) {
				overlayEnabled = !overlayEnabled;
				if (client.player != null) {
					client.player.sendMessage(
							Text.literal(overlayEnabled
									? "§aShadow Overlay: ON"
									: "§cShadow Overlay: OFF"),
							true
					);
				}
			}
		});

		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
			if (overlayEnabled) {
				ShadowOverlayRenderer.render(context);
			}
		});

		// Client Commands registrieren
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
					ClientCommandManager.literal("advancedshadows")
							.then(ClientCommandManager.literal("radius")
									.then(ClientCommandManager.argument("value", IntegerArgumentType.integer(1, 256))
											.executes(ctx -> {
												int val = IntegerArgumentType.getInteger(ctx, "value");
												ModConfig.get().renderRadius = val;
												ModConfig.save();
												ctx.getSource().sendFeedback(
														Text.literal("§aShadow Radius set to " + val)
												);
												return 1;
											})
									)
							)
							.then(ClientCommandManager.literal("yradius")
									.then(ClientCommandManager.argument("value", IntegerArgumentType.integer(1, 64))
											.executes(ctx -> {
												int val = IntegerArgumentType.getInteger(ctx, "value");
												ModConfig.get().renderYRadius = val;
												ModConfig.save();
												ctx.getSource().sendFeedback(
														Text.literal("§aShadow Y-Radius set to " + val)
												);
												return 1;
											})
									)
							)
							.then(ClientCommandManager.literal("status")
									.executes(ctx -> {
										ctx.getSource().sendFeedback(Text.literal(
												"§6AdvancedShadows Status:\n" +
														"§7Overlay: " + (overlayEnabled ? "§aON" : "§cOFF") + "\n" +
														"§7Radius: §f" + ModConfig.get().renderRadius + "\n" +
														"§7Y-Radius: §f" + ModConfig.get().renderYRadius
										));
										return 1;
									})
							)
			);
		});

		AdvancedShadows.LOGGER.info("AdvancedShadows client initialized. Press F7 to toggle.");
	}
}