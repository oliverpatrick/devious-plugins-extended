/*
 * Copyright (c) 2022, Melxin <https://github.com/melxin/>,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.unethicalite.winemaker;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.events.InventoryChanged;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Dialog;
import net.unethicalite.wintertodt.utils.TimeUtils;
import static net.unethicalite.api.commons.Time.sleep;
import static net.unethicalite.api.commons.Time.sleepUntil;

@Extension
@PluginDescriptor(
	name = "mWineMaker",
	description = "makes wines",
	enabledByDefault = false,
	tags =
		{
			"jug_of_wine",
			"grapes",
			"jug_of_water"
		}
)
@Slf4j
@Singleton
public class mWineMakerPlugin extends LoopedPlugin
{
	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private mWineMakerOverlay mWineMakerOverlay;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private mWineMakerConfig config;

	@Provides
	private mWineMakerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(mWineMakerConfig.class);
	}

	@Getter(AccessLevel.PACKAGE)
	private State currentState;

	@Getter(AccessLevel.PACKAGE)
	private int winesMade;

	@Getter(AccessLevel.PACKAGE)
	private int timesBanked;

	@Getter(AccessLevel.PACKAGE)
	private boolean scriptStarted;

	private Instant scriptStartTime;

	protected String getTimeRunning()
	{
		return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
	}

	@Override
	protected void startUp()
	{
		this.overlayManager.add(mWineMakerOverlay);
	}

	@Override
	protected void shutDown()
	{
		reset();
		this.overlayManager.remove(mWineMakerOverlay);
	}

	/**
	 * Reset/stop
	 */
	private void reset()
	{
		this.winesMade = 0;
		this.timesBanked = 0;
		this.currentState = null;
		this.scriptStartTime = null;
		this.scriptStarted = false;
	}

	@Subscribe
	public void onConfigButtonPressed(ConfigButtonClicked event)
	{
		if (!event.getGroup().contains("mwinemaker")
			|| !event.getKey().toLowerCase().contains("start"))
		{
			return;
		}

		if (scriptStarted)
		{
			reset();
		}
		else
		{
			this.scriptStartTime = Instant.now();
			this.scriptStarted = true;
		}
	}

	/**
	 * Broadcast a chat message
	 *
	 * @param message
	 */
	private void broadcastMessage(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.runeLiteFormattedMessage(
				new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("[mWineMaker] ")
					.append(ChatColorType.HIGHLIGHT)
					.append(message)
					.build()
			)
			.type(ChatMessageType.BROADCAST)
			.build());
	}

	@Subscribe
	public void onInventoryChanged(InventoryChanged event)
	{
		if (!scriptStarted)
		{
			return;
		}

		if (event.getItemId() == ItemID.UNFERMENTED_WINE
			&& event.getChangeType() == InventoryChanged.ChangeType.ITEM_ADDED)
		{
			winesMade++;
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!scriptStarted)
		{
			return;
		}

		// Continue widget
		Widget w = client.getWidget(WidgetInfo.MULTI_SKILL_MENU);
		if (w != null && w.isVisible())
		{
			Dialog.continueSpace();
		}
	}

	private enum State
	{
		BANK,
		MAKE_WINES
	}

	private State getState()
	{
		if (!Inventory.contains(ItemID.GRAPES)
			|| !Inventory.contains(ItemID.JUG_OF_WATER))
		{
			return State.BANK;
		}
		return State.MAKE_WINES;
	}

	@Override
	protected int loop()
	{
		if (!scriptStarted || client.getGameState() == GameState.LOGIN_SCREEN)
		{
			return -1;
		}

		final Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return -1;
		}

		currentState = getState();
		switch (currentState)
		{
			case BANK:
				TileObject bank = TileObjects.getFirstSurrounding(localPlayer.getWorldLocation(), 10, obj -> obj.hasAction("Bank") || obj.getName().startsWith("Collect") || obj.getName().startsWith("Bank"));
				if (Bank.isOpen())
				{
					sleep(1000);
					Bank.depositInventory();
					sleep(1500);

					if (!Bank.contains(ItemID.GRAPES) || !Bank.contains(ItemID.JUG_OF_WATER))
					{
						log.error("No grapes or jug of water was found in bank");
						broadcastMessage("No grapes or jug of water was found in bank");
						reset();
						return -1;
					}

					Bank.withdraw(ItemID.GRAPES, 14, Bank.WithdrawMode.ITEM);
					sleepUntil(() -> Inventory.getCount(ItemID.GRAPES) == 14, 2000);

					Bank.withdraw(ItemID.JUG_OF_WATER, 14, Bank.WithdrawMode.ITEM);
					sleepUntil(() -> Inventory.getCount(ItemID.JUG_OF_WATER) == 14, 2000);

					Bank.close();
					timesBanked++;
				}
				else if (bank != null)
				{
					bank.interact(action -> action != null && (action.contains("Bank") || action.contains("Use")));
					sleepUntil(() -> Bank.isOpen(), 2000);
				}
				return -1;

			case MAKE_WINES:
				if (!localPlayer.isAnimating())
				{
					Inventory.getFirst(ItemID.GRAPES).useOn(Inventory.getFirst(ItemID.JUG_OF_WATER));
					sleepUntil(() -> localPlayer.isAnimating(), 3000);
				}
				return -1;
		}
		return -1;
	}
}
