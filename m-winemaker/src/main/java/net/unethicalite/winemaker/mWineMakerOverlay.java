package net.unethicalite.winemaker;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class mWineMakerOverlay extends OverlayPanel
{
	private final Client client;
	private final mWineMakerPlugin plugin;
	private final mWineMakerConfig config;

	@Inject
	private mWineMakerOverlay(Client client, mWineMakerPlugin plugin, mWineMakerConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.BOTTOM_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.isScriptStarted() && config.overlayEnabled())
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("mWineMaker")
				.color(Color.WHITE)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Running: " + plugin.getTimeRunning())
				.leftColor(Color.GREEN)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("State: " + plugin.getCurrentState())
				.leftColor(Color.YELLOW)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Wines made: " + plugin.getTimesBanked())
				.leftColor(Color.GREEN)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Times banked: " + plugin.getTimesBanked())
				.leftColor(Color.GREEN)
				.build());
		}
		return super.render(graphics);
	}
}
