package gg.archipelago.aprandomizer.client;

import gg.archipelago.aprandomizer.managers.chest.SharedChestMenu;
import gg.archipelago.aprandomizer.network.RecipientEntry;
import gg.archipelago.aprandomizer.network.SendChestItemPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SharedChestScreen extends AbstractContainerScreen<SharedChestMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    private static List<RecipientEntry> cachedRecipients = new ArrayList<>();

    private RecipientComboBox comboBox;
    private Button sendButton;

    public SharedChestScreen(SharedChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 193);
        this.inventoryLabelY = 99;
    }

    public static void updateClientRecipients(List<RecipientEntry> recipients) {
        cachedRecipients = new ArrayList<>(recipients);
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof SharedChestScreen screen && screen.comboBox != null) {
            screen.comboBox.setOptions(recipients);
        }
    }

    @Override
    protected void init() {
        super.init();

        this.comboBox = new RecipientComboBox(this.leftPos + 30, this.topPos + 72, 86, 20);
        this.comboBox.setOptions(cachedRecipients);

        this.sendButton = Button.builder(Component.translatable("aprandomizer.chest.button_send"), b -> onSendClicked())
                .bounds(this.leftPos + 120, this.topPos + 72, 48, 20)
                .build();
        this.sendButton.active = false;
        this.addRenderableWidget(sendButton);
    }

    private void onSendClicked() {
        if (comboBox != null) {
            int targetSlotId = comboBox.getSelectedSlotId();
            if (targetSlotId != -1) {
                ClientPacketDistributor.sendToServer(new SendChestItemPayload(targetSlotId));
            }
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (sendButton != null && menu != null) {
            boolean hasItem = !menu.getSlot(SharedChestMenu.SEND_SLOT).getItem().isEmpty();
            RecipientEntry recipient = comboBox != null ? comboBox.getSelectedRecipient() : null;
            boolean canSend = hasItem && recipient != null && recipient.isOnline();
            sendButton.active = canSend;
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (comboBox != null && comboBox.mouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (comboBox != null && comboBox.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
        graphics.fill(x + 6, y + 13, x + this.imageWidth - 6, y + 14, 0xFF2A2A2A);

        // Receiving Inventory (2 rows x 9 columns)
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 7 + col * 18;
                int sy = y + 19 + row * 18;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, sx, sy, 18, 18);
            }
        }

        // Send section separator line
        graphics.fill(x + 6, y + 59, x + this.imageWidth - 6, y + 60, 0xFF2A2A2A);

        // Send slot (slot 18)
        int sendSlotX = x + 7;
        int sendSlotY = y + 72;
        graphics.fill(sendSlotX - 2, sendSlotY - 2, sendSlotX + 20, sendSlotY + 20, 0xFF9C27B0);
        graphics.fill(sendSlotX - 1, sendSlotY - 1, sendSlotX + 19, sendSlotY + 19, 0xFF121212);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, sendSlotX, sendSlotY, 18, 18);

        // Render Combobox main box
        if (comboBox != null) {
            comboBox.setPosition(x + 30, y + 72);
            comboBox.renderMainBox(graphics, mouseX, mouseY);
        }

        // Player Inventory separator
        graphics.fill(x + 6, y + 97, x + this.imageWidth - 6, y + 98, 0xFF2A2A2A);

        // Player 3x9 inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 7 + col * 18;
                int sy = y + 110 + row * 18;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, sx, sy, 18, 18);
            }
        }

        // Hotbar separator line
        graphics.fill(x + 6, y + 166, x + this.imageWidth - 6, y + 167, 0xFF2A2A2A);

        // Player Hotbar 9 slots
        for (int col = 0; col < 9; ++col) {
            int sx = x + 7 + col * 18;
            int sy = y + 168;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, sx, sy, 18, 18);
        }

        // Render dropdown popup on top of background
        if (comboBox != null && comboBox.isExpanded()) {
            comboBox.renderDropdownPopup(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component titleText = Component.translatable("aprandomizer.chest.screen_title");
        int titleW = this.font.width(titleText);
        graphics.text(this.font, titleText, (this.imageWidth - titleW) / 2, 3, 0xFFFFA000, false);

        graphics.text(this.font, Component.translatable("aprandomizer.chest.inbox"), 8, 14, 0xFF80D8FF, false);
        graphics.text(this.font, Component.translatable("aprandomizer.chest.outbox"), 8, 61, 0xFFFFD54F, false);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFCCCCCC, false);
    }
}
