package net.unethicalite.winemaker;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("mwinemaker")
public interface mWineMakerConfig extends Config
{
	@ConfigItem(keyName = "Overlay enabled", name = "Overlay enabled", description = "Enables overlay", position = 1)
	default boolean overlayEnabled()
	{
		return true;
	}

	@ConfigItem(keyName = "Start", name = "Start/Stop", description = "Start/Stop button", position = 2)
	default Button startStopButton()
	{
		return new Button();
	}
}
