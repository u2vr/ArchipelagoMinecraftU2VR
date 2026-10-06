package gg.archipelago.aprandomizer.client;

import gg.archipelago.aprandomizer.managers.chest.SharedChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SharedChestScreen extends AbstractContainerScreen<SharedChestMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    public SharedChestScreen(SharedChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 133);
        this.inventoryLabelY = 40;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;

        int bgMain = 0xEE141414;
        int borderMain = 0xFF3E3E3E;

        // Window background
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, bgMain);

        // Window outer borders
        graphics.fill(x, y, x + this.imageWidth, y + 1, borderMain);
        graphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, borderMain);
        graphics.fill(x, y + 1, x + 1, y + this.imageHeight, borderMain);
        graphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, borderMain);

        // Header separator line
        graphics.fill(x + 6, y + 14, x + this.imageWidth - 6, y + 15, 0xFF2A2A2A);

        // Shared 1-slot box
        int chestSlotX = x + 79;
        int chestSlotY = y + 19;
        graphics.fill(chestSlotX - 2, chestSlotY - 2, chestSlotX + 20, chestSlotY + 20, 0xFF9C27B0);
        graphics.fill(chestSlotX - 1, chestSlotY - 1, chestSlotX + 19, chestSlotY + 19, 0xFF121212);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, chestSlotX, chestSlotY, 18, 18);

        // Player Inventory separator
        graphics.fill(x + 6, y + 48, x + this.imageWidth - 6, y + 49, 0xFF2A2A2A);

        // Player 3x9 inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 7 + col * 18;
                int sy = y + 50 + row * 18;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, sx, sy, 18, 18);
            }
        }

        // Hotbar separator line
        graphics.fill(x + 6, y + 106, x + this.imageWidth - 6, y + 107, 0xFF2A2A2A);

        // Player Hotbar 9 slots
        for (int col = 0; col < 9; ++col) {
            int sx = x + 7 + col * 18;
            int sy = y + 108;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, sx, sy, 18, 18);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component titleText = Component.literal("✦ Общий сундук ✦");
        int titleW = this.font.width(titleText);
        graphics.text(this.font, titleText, (this.imageWidth - titleW) / 2, 4, 0xFFFFA000, false);

        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFCCCCCC, false);
    }
}
