package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface XpVibeConfig extends Config
{
	@ConfigItem(
		keyName = "buttplugServerUrl",
		name = "Server URL",
		description = "The message to show to the user when they login"
	)
	default String buttplugServerUrl()
	{
		return "Hello";
	}

	@ConfigItem(
			keyName = "vibrationCooldown",
			name = "Vibration Cooldown",
			description = "The message to show to the user when they login"
	)
	default int vibrationCooldown()
	{
		return 1;
	}

	@ConfigItem(
			keyName = "maxXpThreshold",
			name = "Max XP Threshold",
			description = "The message to show to the user when they login"
	)
	default double maxXpThreshold()
	{
		return 10000000000.00;
	}

	@ConfigItem(
			keyName = "minIntensity",
			name = "Minimum Intensity",
			description = "The message to show to the user when they login"
	)
	default double minIntensity()
	{
		return 0.00;
	}

	@ConfigItem(
			keyName = "maxIntensity",
			name = "Maximum Intensity",
			description = "The message to show to the user when they login"
	)
	default double maxIntensity()
	{
		return 100.00;
	}
}
