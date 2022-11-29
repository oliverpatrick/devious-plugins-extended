/*
 * Copyright (c) 2022, Melxin <https://github.com/melxin/>
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
package net.unethicalite.airorbs;

import com.google.inject.Provides;
import com.openosrs.client.game.WorldLocation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.airorbs.utils.TimeUtils;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.events.ExperienceGained;
import net.unethicalite.api.game.Combat;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.magic.Magic;
import net.unethicalite.api.magic.SpellBook;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.plugins.LoopedPlugin;
import org.pf4j.Extension;
import static net.unethicalite.api.commons.Time.sleep;
import static net.unethicalite.api.commons.Time.sleepUntil;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;

@Extension
@PluginDescriptor(
        name = "mAir orbs",
        description = "Does air orbs for you",
        enabledByDefault = false,
        tags =
                {
                        "air",
                        "orbs"
                }
)
@Slf4j
@Singleton
public class mAirOrbsPlugin extends LoopedPlugin
{
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private mAirOrbsOverlay mAirOrbsOverlay;

    @Inject
    private mAirOrbsConfig config;

    @Provides
    private mAirOrbsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(mAirOrbsConfig.class);
    }

    @Getter(AccessLevel.PACKAGE)
    private State currentState;

    @Getter(AccessLevel.PACKAGE)
    private boolean scriptStarted;

    private Instant scriptStartTime;
    protected String getTimeRunning()
    {
        return scriptStartTime != null ? TimeUtils.getTimeBetween(scriptStartTime, Instant.now()) : "";
    }

    private Instant lastActionTime;

    @Getter(AccessLevel.PACKAGE)
    private int orbsCharged;

    @Override
    protected void startUp()
    {
        this.overlayManager.add(mAirOrbsOverlay);
    }

    @Override
    protected void shutDown()
    {
        reset();
        this.overlayManager.remove(mAirOrbsOverlay);
    }

    /**
     * Reset/stop
     */
    private void reset()
    {
        this.lastActionTime = null;
        this.scriptStartTime = null;
        this.scriptStarted = false;
        this.orbsCharged = 0;
    }

    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("airorbs") || !event.getKey().toLowerCase().contains("start"))
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

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        if (!scriptStarted)
        {
            return;
        }

        Widget chargeWidget = client.getWidget(270, 14);
        if (chargeWidget != null)
        {
            chargeWidget.interact("Charge");
        }
    }

    @Subscribe
    public void onExperienceGained(ExperienceGained event)
    {
        if (event.getSkill() == Skill.MAGIC
                && event.getXpGained() == 76)
        {
            final Player localPlayer = client.getLocalPlayer();
            final int regionId = localPlayer != null ? localPlayer.getWorldLocation().getRegionID() : -1;
            if (regionId == 12343)
            {
                orbsCharged++;
            }
        }
    }

    private enum State
    {
        BANK,
        EAT_FOOD,
        DRINK_ENERGY_POTION,
        WALK_TO_OBELISK,
        CHARGE_ORBS
    }

    private State getState()
    {
        final Player localPlayer = client.getLocalPlayer();
        final int regionId = localPlayer != null ? localPlayer.getWorldLocation().getRegionID() : -1;

        if (Combat.getHealthPercent() <= 65)
        {
            if (Inventory.contains(ItemID.JUG_OF_WINE))
            {
                return State.EAT_FOOD;
            }
            else
            {
                return State.BANK;
            }
        }
        else if (client.getEnergy() < 80 && Inventory.contains(i -> i.getName().startsWith("Energy potion")))
        {
            return State.DRINK_ENERGY_POTION;
        }
        else if (Inventory.contains(ItemID.UNPOWERED_ORB) && Inventory.contains(ItemID.COSMIC_RUNE) && regionId != 12343)
        {
            return State.WALK_TO_OBELISK;
        }
        else if (Inventory.contains(ItemID.UNPOWERED_ORB) && Inventory.contains(ItemID.COSMIC_RUNE) && regionId == 12343)
        {
            return State.CHARGE_ORBS;
        }
        else
        {
            return State.BANK;
        }
    }

    @Override
    protected int loop()
    {
        if (!scriptStarted || client.getGameState() == GameState.LOGIN_SCREEN)
        {
            return -1;
        }

        final Player localPlayer = client.getLocalPlayer();
        final int regionId = localPlayer != null ? localPlayer.getWorldLocation().getRegionID() : -1;

        currentState = getState();
        switch (currentState)
        {
            case BANK:
                bank();
                return -1;

            case EAT_FOOD:
                if (Inventory.contains(ItemID.JUG_OF_WINE))
                {
                    Inventory.getFirst(ItemID.JUG_OF_WINE).interact("Drink");
                    sleep(500);
                }
                return -1;

            case DRINK_ENERGY_POTION:
                if (Inventory.contains(i -> i.getName().startsWith("Energy potion")))
                {
                    Inventory.getFirst(i -> i.getName().startsWith("Energy potion")).interact("Drink");
                    sleep(500);
                }
                return -1;

            case WALK_TO_OBELISK:
                if (regionId == 12342)
                {
                    WorldPoint trapDoorWorldPoint = new WorldPoint(3096, 3468, 0);
                    if (localPlayer.distanceTo(trapDoorWorldPoint) > 1)
                    {
                        Movement.walkTo(trapDoorWorldPoint);
                    }
                    else
                    {
                        TileObject trapDoor = TileObjects.getFirstSurrounding(localPlayer.getWorldLocation(), 10, obj -> obj.getName().startsWith("Trapdoor"));
                        if (trapDoor != null)
                        {
                            trapDoor.interact("Open", "Climb-down");
                            sleepUntil(() -> regionId == 12442, 2000);
                        }
                    }
                }

                if (regionId == 12442 || regionId == 12443)
                {
                    WorldPoint ladderWorldPoint = new WorldPoint(3089, 9971, 0);
                    if (localPlayer.distanceTo(ladderWorldPoint) > 1)
                    {
                        Movement.walkTo(ladderWorldPoint);
                    }
                    else
                    {
                        TileObject ladder = TileObjects.getFirstSurrounding(localPlayer.getWorldLocation(), 10, obj -> obj.hasAction("Climb-up") || obj.getName().startsWith("Ladder"));
                        if (ladder != null)
                        {
                            ladder.interact("Climb-up");
                            sleepUntil(() -> regionId == 12343, 2000);
                        }
                    }
                }
                return -1;

            case CHARGE_ORBS:
                if (regionId == 12343)
                {
                    TileObject obelisk = TileObjects.getFirstSurrounding(localPlayer.getWorldLocation(), 10, obj -> obj.getName().startsWith("Obelisk"));
                    if (obelisk != null)
                    {
                        if (!localPlayer.isAnimating())
                        {
                            if (lastActionTime == null || Duration.between(lastActionTime, Instant.now()).toMillis() >= 2500)
                            {
                                Magic.cast(SpellBook.Standard.CHARGE_AIR_ORB, obelisk);
                                sleepUntil(() -> localPlayer.isAnimating(), 3000);
                            }
                        }
                        else
                        {
                            lastActionTime = Instant.now();
                        }
                    }
                }
                return -1;
        }
        return -1;
    }

    private void bank()
    {
        TileObject bank = TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 10, obj -> obj.hasAction("Bank") || obj.getName().startsWith("Collect"));
        if (bank != null && !Bank.isOpen())
        {
            if (Equipment.contains(ItemID.AMULET_OF_GLORY))
            {
                Equipment.getFirst(ItemID.AMULET_OF_GLORY).interact("Remove");
            }

            if (Inventory.contains(i -> i.getName().startsWith("Energy potion")))
            {
                Inventory.getFirst(i -> i.getName().startsWith("Energy potion")).interact("Drink");
                sleepUntil(() -> !Inventory.contains(i -> i.getName().startsWith("Energy potion")), 800);
                return;
            }

            bank.interact("Bank");
            sleepUntil(() -> Bank.isOpen(), 1500);
        }
        else if (Bank.isOpen())
        {
            sleep(1000);

            if (Bank.getCount(true, ItemID.COSMIC_RUNE) < 100
                    || Bank.getCount(true, ItemID.UNPOWERED_ORB) < 25
                    || Bank.getCount(true, ItemID.JUG_OF_WINE) < 1
                    || Bank.getCount(true, ItemID.ENERGY_POTION4) < 1)
            {
                log.error("Out of supplies! stopping..");
                log.warn("Make sure your bank has atleast: 100 cosmic runes, 25 Unpowered orbs, 1 Jug of wine, 1 Energy potion(4), 1x Charged amulet of glory");
                reset();
                Bank.close();
                return;
            }

            if (!Bank.Inventory.getAll().isEmpty())
            {
                Bank.depositInventory();
                sleepUntil(() -> Bank.Inventory.getAll().isEmpty(), 2000);
            }

            if (!Equipment.contains(i -> i != null && i.getName().contains("Amulet of glory(")))
            {
                Bank.withdraw(i -> i != null && i.getName().contains("Amulet of glory("), 1, Bank.WithdrawMode.ITEM);
                sleep(500);
                Bank.close();
                sleep(500);
                Inventory.getFirst(i -> i != null && i.getName().contains("Amulet of glory(")).interact("Wear");
                sleepUntil(() -> Equipment.contains(i -> i != null && i.getName().contains("Amulet of glory(")), 3000);
                return;
            }

            Bank.withdraw(ItemID.JUG_OF_WINE, 1, Bank.WithdrawMode.ITEM);

            Bank.withdraw(ItemID.ENERGY_POTION4, 1, Bank.WithdrawMode.ITEM);

            Bank.withdraw(ItemID.COSMIC_RUNE, 100, Bank.WithdrawMode.ITEM);

            Bank.withdrawAll(ItemID.UNPOWERED_ORB, Bank.WithdrawMode.ITEM);

            if (Inventory.getCount(ItemID.COSMIC_RUNE) == 100
                    && Inventory.getCount(ItemID.UNPOWERED_ORB) == 25
                    && Inventory.contains(ItemID.JUG_OF_WINE)
                    && Inventory.contains(ItemID.ENERGY_POTION4)
                    && Bank.isOpen())
            {
                Bank.close();
            }
        }
        else
        {
            WorldPoint bankLoc = WorldLocation.EDGEVILLE_BANK.getWorldArea().toWorldPoint();
            if (client.getLocalPlayer().distanceTo(bankLoc) > 2)
            {
                Movement.walkTo(bankLoc);
            }
        }
    }
}