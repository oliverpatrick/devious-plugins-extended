package net.unethicalite.wintertodt;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("mwintertodt")
public interface mWintertodtConfig extends Config
{
    @ConfigItem(keyName = "Food", name = "Food", description = "The food to use", position = 1)
    default FoodType foodType()
    {
        return FoodType.TUNA;
    }

    @ConfigItem(keyName = "Food amount", name = "Food amount", description = "The food amount to take from bank", position = 2)
    default int foodAmount()
    {
        return 7;
    }

    @ConfigItem(keyName = "Brazier location", name = "Brazier location", description = "The brazier to use", position = 3)
    default BrazierLocation brazierLocation()
    {
        return BrazierLocation.EAST;
    }


    @ConfigItem(keyName = "Destination level", name = "Destination level", description = "Stop when level is reached", position = 4)
    default int destinationLevel()
    {
        return 99;
    }

    @ConfigItem(keyName = "Start", name = "Start/Stop", description = "Start/Stop button", position = 5)
    default Button startStopButton()
    {
        return new Button();
    }
}
