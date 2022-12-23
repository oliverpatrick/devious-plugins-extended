package net.unethicalite.superheat;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("msuperheat")
public interface mSuperheatConfig extends Config
{
	@ConfigItem(keyName = "Ore type", name = "Ore type", description = "The ore to use", position = 1)
	default OreType oreType()
	{
		return OreType.IRON_ORE;
	}

	@ConfigItem(keyName = "Overlay enabled", name = "Overlay enabled", description = "Enables overlay", position = 2)
	default boolean overlayEnabled()
	{
		return true;
	}

	@ConfigItem(keyName = "Start", name = "Start/Stop", description = "Start/Stop button", position = 3)
	default Button startStopButton()
	{
		return new Button();
	}
}
