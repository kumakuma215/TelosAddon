package xyz.telosaddon.yuno.features;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import xyz.telosaddon.yuno.renderer.CircleRenderer;
import xyz.telosaddon.yuno.renderer.IRenderer;
import xyz.telosaddon.yuno.renderer.LineRenderer;
import xyz.telosaddon.yuno.utils.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ShowRangeFeature extends AbstractFeature {
	private ItemStack previousItem = null;
	private List<IRenderer> renderers;
	private final Function<PlayerInventory, ItemStack> itemGetter;
	private float radius = Float.NaN;


	public ShowRangeFeature(Config config, Function<PlayerInventory, ItemStack> itemGetter, String itemSlotName) {
		super(config, "Show" + itemSlotName + "RangeFeature");
		this.itemGetter = itemGetter;
		this.setRangeType(RangeViewType.valueOf(config.getString("Show" + itemSlotName + "RangeFeatureViewType")));
	}

	private float parseRadius(ItemStack itemStack) {
		var loreComponent = itemStack.getComponents().get(DataComponentTypes.LORE);
		if (loreComponent == null) return Float.NaN;
		for (var line : loreComponent.lines()) {
			if (!line.getString().contains("Range:")) continue;
			try {
				String numWithPossibleExt = line.getString().split("Range:")[1].trim();
				// Handle <num>(+3) case for EX
				String num = numWithPossibleExt.split("\\(")[0].trim();
				return Float.parseFloat(num);
			} catch (ArrayIndexOutOfBoundsException | NumberFormatException ignored) {
			}
		}
		return Float.NaN;
	}

	private void checkMainHand(MinecraftClient client) {
		assert client.player != null;
		var inventory = client.player.getInventory();
		if (inventory == null) return;
		ItemStack itemToCheck = this.itemGetter.apply(inventory);
		if (!itemToCheck.equals(this.previousItem)) {
			previousItem = itemToCheck;
			radius = parseRadius(itemToCheck);
			this.renderers.forEach(r -> r.setRadius(radius));
		}
	}

	public void tick() {
		if (!this.isEnabled()) return;
		var client = MinecraftClient.getInstance();
		if (client.player == null) return;
		checkMainHand(client);
	}

	public float getRadius() {
		return this.radius;
	}

	public void draw(float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, ClientPlayerEntity player, float dy) {
		if (!this.isEnabled()) return;
		this.renderers.forEach(r -> r.draw(tickDelta,
				matrices,
				vertexConsumers,
				player,
				this.getConfig().getInteger(this.getFeatureName() + "Color"),
				this.getConfig().getDouble(this.getFeatureName() + "Height").floatValue() + dy));

	}

	public void setRangeType(RangeViewType type) {
		this.renderers = switch (type){
			case CIRCLE -> List.of(new CircleRenderer());
			case LINE -> List.of(new LineRenderer());
			case BOTH -> List.of(new CircleRenderer(), new LineRenderer());
		};

		this.renderers.forEach(r -> r.setRadius(radius));
	}

	public enum RangeViewType {
		CIRCLE,
		LINE,
		BOTH;
	}
}
