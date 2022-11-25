package net.unethicalite.wintertodt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;

@AllArgsConstructor
@Getter
public enum FoodType
{
	TUNA("Tuna", ItemID.TUNA, "Eat"),
	;

	private String name;
	private int id;
	private String action;
}
