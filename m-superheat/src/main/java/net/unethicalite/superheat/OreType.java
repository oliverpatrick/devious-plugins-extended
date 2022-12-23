package net.unethicalite.superheat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;

@AllArgsConstructor
@Getter
public enum OreType
{
	IRON_ORE(ItemID.IRON_ORE, ItemID.IRON_BAR),
	GOLD_ORE(ItemID.GOLD_ORE, ItemID.GOLD_BAR);

	private int oreId;
	private int barId;
}
