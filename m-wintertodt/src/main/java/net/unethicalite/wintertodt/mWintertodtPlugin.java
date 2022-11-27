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
package net.unethicalite.wintertodt;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.AnimationID;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.pathfinder.Walker;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import javax.inject.Inject;

@Extension
@PluginDescriptor(
        name = "mWintertodt",
        description = "does wintertodt",
        enabledByDefault = false,
        tags =
                {
                        "wintertodt",
                        "minigame",
                        "firemaking",
                        "woodcutting",
                        "fletching",
                        "smithing",
                        "rewards"
                }
)
@Slf4j
public class mWintertodtPlugin extends LoopedPlugin
{
    @Inject
    private Client client;

    @Inject
    private mWintertodtConfig config;

    @Provides
    private mWintertodtConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(mWintertodtConfig.class);
    }

    private boolean scriptStarted;

    @Override
    protected void startUp()
    {
    }

    @Override
    protected void shutDown()
    {
        reset();
    }

    /**
     * Reset/stop
     */
    private void reset()
    {
        this.scriptStarted = false;
    }

    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
            if (!event.getGroup().contains("mwintertodt") || !event.getKey().toLowerCase().contains("start"))
            {
                return;
            }

            if (scriptStarted)
            {
                reset();
            }
            else
            {
                this.scriptStarted = true;
            }
    }

    private boolean isInWintertodtRegion()
    {
        if (client.getLocalPlayer() != null)
        {
            return client.getLocalPlayer().getWorldLocation().getRegionID() == 6462;
        }
        return false;
    }

    private boolean isGameStarted()
    {
        if (isInWintertodtRegion())
        {
            return !Widgets.get(396, 3).getText().contains("The Wintertodt returns");
        }
        return false;
    }

    private enum State
    {
        BANK, ENTER_WINTERTODT, EAT_FOOD, CUT_TREE, FLETCH_LOGS, FIX_BRAZIER, LIT_BRAZIER, FEED_BRAZIER, LEAVE_WINTERTODT, SLEEP
    };

    private State getState()
    {
        if (client.getBoostedSkillLevel(Skill.HITPOINTS) <= 7)
        {
            if (Inventory.contains(config.foodType().getId()))
            {
                return State.EAT_FOOD;
            }
            else
            {
                if (isInWintertodtRegion())
                {
                    return State.LEAVE_WINTERTODT;
                }
            }
        }

        if (!isInWintertodtRegion())
        {
            if (Inventory.getCount(config.foodType().getId()) < config.foodAmount() || Inventory.contains(ItemID.SUPPLY_CRATE))
            {
                return State.BANK;
            }
            else
            {
                return State.ENTER_WINTERTODT;
            }
        }

        if (isInWintertodtRegion())
        {
            if (isGameStarted())
            {
                TileObject brokenBrazier = TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 10, obj -> obj.hasAction("Fix"));
                TileObject unlitBrazier = TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 10, obj -> obj.hasAction("Light"));
                TileObject burningBrazier = TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 10, obj -> obj.hasAction("Feed") || obj.getName().startsWith("Burning brazier"));
                if (brokenBrazier != null && client.getLocalPlayer().distanceTo(brokenBrazier) <= 4)
                {
                    return State.FIX_BRAZIER;
                }
                else if (unlitBrazier != null && client.getLocalPlayer().distanceTo(unlitBrazier) <= 4)
                {
                    return State.LIT_BRAZIER;
                }
                else if (Inventory.getCount(ItemID.BRUMA_KINDLING) >= 8 || Inventory.contains(ItemID.BRUMA_KINDLING) && burningBrazier != null && client.getLocalPlayer().distanceTo(burningBrazier.getWorldLocation()) <= 4)
                {
                        return State.FEED_BRAZIER;
                }
                else if (Inventory.contains(ItemID.BRUMA_ROOT))
                {
                    return State.FLETCH_LOGS;
                }
                else
                {
                    return State.CUT_TREE;
                }
            }
            else if (Inventory.getCount(config.foodType().getId()) < config.foodAmount() || Inventory.contains(ItemID.SUPPLY_CRATE))
            {
                return State.LEAVE_WINTERTODT;
            }
        }
        return State.SLEEP;
    }

    @Override
    protected int loop()
    {
        if (!scriptStarted)
        {
            return -1;
        }

        // Stop when on login screen or level is reached..
        if (client.getGameState() == GameState.LOGIN_SCREEN
                || client.getBoostedSkillLevel(Skill.FIREMAKING) >= config.destinationLevel())
        {
            if (scriptStarted)
            {
                reset();
            }
            return -1;
        }

        switch (getState())
        {
            case BANK:
                bank();
                break;
            case ENTER_WINTERTODT:
                enterWintertodt();
                break;
            case EAT_FOOD:
                FoodType foodType = config.foodType();
                Inventory.getFirst(foodType.getId()).interact(foodType.getAction());
                break;
            case CUT_TREE:
                cutTree();
                break;
            case FLETCH_LOGS:
                fletchLogs();
                break;
            case FIX_BRAZIER:
                TileObject brokenBrazier = TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 10, obj -> obj.hasAction("Fix"));
                if (brokenBrazier != null)
                {
                    brokenBrazier.interact("Fix");
                    sleepUntil(() -> brokenBrazier == null, 3000);
                }
                break;
            case LIT_BRAZIER:
                TileObject unlitBrazier = TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 10, obj -> obj.hasAction("Light"));
                if (unlitBrazier != null)
                {
                    unlitBrazier.interact("Light");
                    sleepUntil(() -> unlitBrazier == null, 3000);
                }
                break;
            case FEED_BRAZIER:
                feedBrazier();
                break;
            case LEAVE_WINTERTODT:
                leaveWintertodt();
                break;
            case SLEEP:
                return 1000;
        }
        return 50;
    }

    private void bank()
    {
        TileObject bank = TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 10, obj -> obj.hasAction("Bank") || obj.getName().startsWith("Collect"));

        if (Bank.isOpen())
        {
            sleep(1000);
            Bank.depositAllExcept(item -> item != null && item.getName().equals(config.foodType().getName()) || item.getName().endsWith("axe") || item.getName().equals("Knife") || item.getName().equals("Hammer") || item.getName().equals("Tinderbox"));
            sleep(1500);
            if (Bank.Inventory.getFirst(config.foodType().getId()) == null)
            {
                reset();
                return;
            }
            Bank.withdraw(config.foodType().getId(), config.foodAmount() - Inventory.getCount(config.foodType().getId()), Bank.WithdrawMode.ITEM);
            sleep(1000);
            sleepUntil(() -> Inventory.getCount(config.foodType().getId()) == config.foodAmount(), 3000);
            Bank.close();
        }
        else if (bank != null)
        {
            bank.interact("Bank");
            sleepUntil(() -> Bank.isOpen(), 1000);
        }
        else
        {
            Walker.walkTo(new WorldPoint(1640, 3944, 0));
            sleepUntil(() -> bank != null, 800);
        }
    }

    private void enterWintertodt()
    {
        if (!isInWintertodtRegion())
        {
            Walker.walkTo(new WorldPoint(1630, 3962, 0));
            TileObject door = TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 10, obj -> obj.getName().startsWith("Door") && obj.hasAction("Enter"));
            sleepUntil(() -> door != null, 1000);

            if (door != null)
            {
                door.interact("Enter");
                sleepUntil(this::isInWintertodtRegion, 5000);
            }
        }

        sleep(2000);
        Walker.walkTo(config.brazierLocation().getWorldPoint());
    }

    /**
     * Leave wintertodt area method
     */
    private void leaveWintertodt()
    {
        if (isInWintertodtRegion())
        {
            TileObject door = TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 10, obj -> obj.getName().startsWith("Door") && obj.hasAction("Enter"));
            if (door != null)
            {
                door.interact("Enter");
                Widget skipDialog =  Widgets.get(WidgetInfo.DIALOG_OPTION_OPTION1);
                sleepUntil(() -> !isInWintertodtRegion() || skipDialog != null && skipDialog.isVisible(), 5000);
                if (skipDialog != null && skipDialog.isVisible())
                {
                    skipDialog.interact(MenuAction.WIDGET_CONTINUE.getId());
                    sleepUntil(() -> !isInWintertodtRegion(), 1000);
                }
            }
            else
            {
                Walker.walkTo(new WorldPoint(631, 3969, 0));
                sleepUntil(() -> door != null, 800);
            }
        }
    }

    private void cutTree()
    {
        if (!client.getLocalPlayer().isAnimating() && isInWintertodtRegion())
        {
            TileObject tree = TileObjects.getFirstSurrounding(config.brazierLocation().getWorldPoint(), 10, obj -> obj.hasAction("Chop") || obj.getName().startsWith("Bruma roots"));
            if (tree != null)
            {
                tree.interact("Chop");
            }
            else
            {
                Walker.walkTo(config.brazierLocation().getWorldPoint());
                sleepUntil(() -> tree != null, 800);
            }
        }
    }

    private void fletchLogs()
    {
        int anim = client.getLocalPlayer().getAnimation();
        if (!client.getLocalPlayer().isAnimating() && isInWintertodtRegion()
                || Inventory.getCount(ItemID.BRUMA_ROOT) >= 8 && anim != AnimationID.FLETCHING_BOW_CUTTING)
        {
            Inventory.getFirst(ItemID.KNIFE).useOn(Inventory.getFirst(ItemID.BRUMA_ROOT));
            sleep(200);
            sleepUntil(() -> !Inventory.contains(ItemID.BRUMA_ROOT) || anim != AnimationID.FLETCHING_BOW_CUTTING, 5500);
        }
    }

    private void feedBrazier()
    {
        TileObject brazier = TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 10, obj -> obj.hasAction("Feed") || obj.getName().startsWith("Burning brazier"));
        if (!client.getLocalPlayer().isAnimating() && isInWintertodtRegion())
        {
            brazier.interact("Feed");
            sleep(200);
            int anim = client.getLocalPlayer().getAnimation();
            sleepUntil(() -> brazier == null || !Inventory.contains(ItemID.BRUMA_KINDLING) || anim != AnimationID.LOOKING_INTO, 5500);
        }
    }

    /**
     * Sleep until condition is met or timeout is reached
     *
     * @param condition, the condition to sleep for until becoming valid
     * @param timeout, the timeout in millis
     */
    private void sleepUntil(BooleanSupplier condition, long timeout)
    {
        Instant endTime = Instant.now().plusMillis(timeout);
        while (!condition.getAsBoolean() && Instant.now().isBefore(endTime))
        {
            continue;
        }
    }

    /**
     * Sleep with timeout
     *
     * @param timeout, the timeout in millis
     */
    private void sleep(long timeout)
    {
        Instant endTime = Instant.now().plusMillis(timeout);
        while (Instant.now().isBefore(endTime))
        {
            continue;
        }
    }
}
