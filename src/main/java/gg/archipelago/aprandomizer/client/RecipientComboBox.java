package gg.archipelago.aprandomizer.client;

import gg.archipelago.aprandomizer.network.RecipientEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class RecipientComboBox {
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean isExpanded = false;
    private int selectedIndex = -1;
    private List<RecipientEntry> options = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_ITEMS = 6;
    private static final int ITEM_HEIGHT = 15;

    public RecipientComboBox(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setOptions(List<RecipientEntry> newOptions) {
        int previousSelectedSlot = getSelectedSlotId();
        this.options = new ArrayList<>(newOptions);
        this.selectedIndex = -1;

        if (previousSelectedSlot != -1) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).slotId() == previousSelectedSlot) {
                    this.selectedIndex = i;
                    break;
                }
            }
        }

        // If no previous selection, select first online player if available
        if (this.selectedIndex == -1) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).isOnline()) {
                    this.selectedIndex = i;
                    break;
                }
            }
        }

        if (this.selectedIndex == -1 && !options.isEmpty()) {
            this.selectedIndex = 0;
        }
    }

    public int getSelectedSlotId() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            return options.get(selectedIndex).slotId();
        }
        return -1;
    }

    public RecipientEntry getSelectedRecipient() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            return options.get(selectedIndex);
        }
        return null;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        this.isExpanded = expanded;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return false;

        // Check click on main box
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            if (!options.isEmpty()) {
                isExpanded = !isExpanded;
                scrollOffset = 0;
            }
            return true;
        }

        // If expanded, check click on dropdown items
        if (isExpanded) {
            int visibleCount = Math.min(options.size(), MAX_VISIBLE_ITEMS);
            int popupHeight = visibleCount * ITEM_HEIGHT;
            int popupY = y + height;

            if (mouseX >= x && mouseX < x + width && mouseY >= popupY && mouseY < popupY + popupHeight) {
                int clickedIndex = scrollOffset + (int) ((mouseY - popupY) / ITEM_HEIGHT);
                if (clickedIndex >= 0 && clickedIndex < options.size()) {
                    this.selectedIndex = clickedIndex;
                    this.isExpanded = false;
                }
                return true;
            }

            // Clicked outside dropdown
            isExpanded = false;
            return false;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isExpanded) {
            int visibleCount = Math.min(options.size(), MAX_VISIBLE_ITEMS);
            int popupHeight = visibleCount * ITEM_HEIGHT;
            int popupY = y + height;

            if (mouseX >= x && mouseX < x + width && mouseY >= popupY && mouseY < popupY + popupHeight) {
                if (scrollY > 0 && scrollOffset > 0) {
                    scrollOffset--;
                    return true;
                } else if (scrollY < 0 && scrollOffset + visibleCount < options.size()) {
                    scrollOffset++;
                    return true;
                }
            }
        } else if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            // Cycle options on scroll
            if (options.size() > 1) {
                if (scrollY < 0) {
                    selectedIndex = (selectedIndex + 1) % options.size();
                } else if (scrollY > 0) {
                    selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
                }
                return true;
            }
        }
        return false;
    }

    public void renderMainBox(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

        int bgColor = hovered ? 0xFF2A2A2A : 0xFF181818;
        int borderColor = isExpanded ? 0xFFFFA000 : (hovered ? 0xFF888888 : 0xFF444444);

        // Background
        graphics.fill(x, y, x + width, y + height, bgColor);

        // Border
        graphics.fill(x, y, x + width, y + 1, borderColor);
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        graphics.fill(x, y, x + 1, y + height, borderColor);
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor);

        // Text
        if (options.isEmpty()) {
            graphics.text(font, Component.translatable("aprandomizer.chest.no_players"), x + 4, y + (height - 8) / 2, 0xFF888888, false);
        } else {
            RecipientEntry selected = getSelectedRecipient();
            if (selected != null) {
                String dot = selected.isOnline() ? "§a● " : "§7○ ";
                String name = selected.name();
                int maxNameW = width - 20;
                String display = dot + font.plainSubstrByWidth(name, maxNameW);
                int textColor = selected.isOnline() ? 0xFFFFFFFF : 0xFFAAAAAA;
                graphics.text(font, Component.literal(display), x + 4, y + (height - 8) / 2, textColor, false);
            }
            String arrow = isExpanded ? "▲" : "▼";
            graphics.text(font, Component.literal(arrow), x + width - 10, y + (height - 8) / 2, 0xFFFFA000, false);
        }
    }

    public void renderDropdownPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!isExpanded || options.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int visibleCount = Math.min(options.size(), MAX_VISIBLE_ITEMS);
        int popupHeight = visibleCount * ITEM_HEIGHT;
        int popupY = y + height;

        // Popup background
        graphics.fill(x, popupY, x + width, popupY + popupHeight, 0xF0101010);

        // Popup border
        graphics.fill(x, popupY, x + width, popupY + 1, 0xFFFFA000);
        graphics.fill(x, popupY + popupHeight - 1, x + width, popupY + popupHeight, 0xFFFFA000);
        graphics.fill(x, popupY, x + 1, popupY + popupHeight, 0xFFFFA000);
        graphics.fill(x + width - 1, popupY, x + width, popupY + popupHeight, 0xFFFFA000);

        // Options
        for (int i = 0; i < visibleCount; i++) {
            int optIdx = scrollOffset + i;
            if (optIdx >= options.size()) break;

            RecipientEntry entry = options.get(optIdx);
            int itemY = popupY + i * ITEM_HEIGHT;
            boolean itemHovered = mouseX >= x && mouseX < x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;

            if (itemHovered || optIdx == selectedIndex) {
                int itemBg = itemHovered ? 0xFF333333 : 0xFF222222;
                graphics.fill(x + 1, itemY, x + width - 1, itemY + ITEM_HEIGHT, itemBg);
            }

            String dot = entry.isOnline() ? "§a● " : "§7○ ";
            int maxNameW = width - 12;
            String text = dot + font.plainSubstrByWidth(entry.name(), maxNameW);
            int textColor = entry.isOnline() ? 0xFFFFFFFF : 0xFF888888;
            graphics.text(font, Component.literal(text), x + 4, itemY + (ITEM_HEIGHT - 8) / 2, textColor, false);
        }
    }
}
