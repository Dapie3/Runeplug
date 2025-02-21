package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Slf4j
@PluginDescriptor(
		name = "XP Vibe Plugin",
		description = "Vibrates a device when you gain XP, with customizable settings",
		tags = {"xp", "vibration", "haptic", "buttplug"}
)
public class XpVibePlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private XpVibeConfig config;

	private WebSocket buttplugClient;
	private Map<Skill, Integer> previousXp = new HashMap<>();
	private long lastVibrationTime = 0;

	@Override
	protected void startUp() throws Exception {
		connectToButtplug();
		log.info("XP Vibe Plugin Started!");
	}

	@Override
	protected void shutDown() throws Exception {
		if (buttplugClient != null) {
			buttplugClient.sendClose(WebSocket.NORMAL_CLOSURE, "Bye").join();  // Gracefully closing the WebSocket
		}
		log.info("XP Vibe Plugin Stopped!");
	}

	private void connectToButtplug() {
		HttpClient client = HttpClient.newHttpClient();
		client.newWebSocketBuilder()
				.buildAsync(URI.create(config.buttplugServerUrl()), new Listener() {
					@Override
					public void onOpen(WebSocket webSocket) {
						log.info("Connected to Buttplug Server!");
					}

					@Override
					public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
						log.info("Received message: " + data);
						return null;
					}

					@Override
					public void onError(WebSocket webSocket, Throwable error) {
						log.error("WebSocket Error", error);
					}

					@Override
					public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
						log.info("Disconnected from Buttplug Server: " + reason);
						return null;
					}
				})
				.thenAccept(webSocket -> {
					buttplugClient = webSocket;
				}).exceptionally(ex -> {
					log.error("Failed to connect to Buttplug Server", ex);
					return null;
				});
	}

	@Subscribe
	public void onStatChanged(StatChanged event) {
		Skill skill = event.getSkill();
		int newXp = client.getSkillExperience(skill);
		int oldXp = previousXp.getOrDefault(skill, newXp);
		int xpGained = newXp - oldXp;
		previousXp.put(skill, newXp);

		if (xpGained > 0) {
			log.info("XP Gained in " + skill.getName() + ": " + xpGained);

			double intensity = calculateIntensity(xpGained);
			long currentTime = System.currentTimeMillis();

			if (currentTime - lastVibrationTime >= config.vibrationCooldown()) {
				sendVibrationCommand(intensity);
				lastVibrationTime = currentTime;
			}
		}
	}

	private double calculateIntensity(int xpGained) {
		double normalizedXp = Math.min((double) xpGained / config.maxXpThreshold(), 1.0);
		return config.minIntensity() + (normalizedXp * (config.maxIntensity() - config.minIntensity()));
	}

	private void sendVibrationCommand(double intensity) {
		if (buttplugClient != null && !buttplugClient.isInputClosed()) {
			String command = String.format("{\"type\": \"VibrateCmd\", \"intensity\": %.2f}", intensity);
			buttplugClient.sendText(command, true);  // Send the message to the WebSocket server
			log.info("Sent Vibration Command: " + command);
		}
	}

	@Provides
	XpVibeConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(XpVibeConfig.class);
	}
}
