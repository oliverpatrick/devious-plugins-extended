package net.unethicalite.airorbs;

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

public class mAirOrbsOverlay extends OverlayPanel
{
	private final Client client;
	private final mAirOrbsPlugin plugin;
	private final mAirOrbsConfig config;

	@Inject
	private mAirOrbsOverlay(Client client, mAirOrbsPlugin plugin, mAirOrbsConfig config)
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
				.text("mAirOrbs")
				.color(Color.GREEN)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Running: " + plugin.getTimeRunning())
				.leftColor(Color.WHITE)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("State: " + plugin.getCurrentState())
				.leftColor(Color.WHITE)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Orbs charged: " + plugin.getOrbsCharged())
				.leftColor(Color.WHITE)
				.build());
		}
		return super.render(graphics);
	}
}
