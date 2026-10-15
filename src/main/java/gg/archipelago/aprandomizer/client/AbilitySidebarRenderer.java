package gg.archipelago.aprandomizer.client;

import com.mojang.blaze3d.platform.InputConstants;
import gg.archipelago.aprandomizer.managers.AbilityManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AbilitySidebarRenderer {

    public record ElementBounds(int type, int x, int y, int width, int height) {}

    public static final int TYPE_LIGHT_HOLDER = 0;
    public static final int TYPE_TRASH_CAN = 1;
    public static final int TYPE_MINING = 2;
    public static final int TYPE_TREE_FELLER = 3;
    public static final int TYPE_MAGNET = 4;
    public static final int TYPE_STEP_ASSIST = 5;

    public static List<ElementBounds> getVisibleElements(int leftPos, int topPos, int imageWidth) {
        List<ElementBounds> list = new ArrayList<>();
        if (!ClientAbilityManager.hasAnyAbility()) return list;

        int x = leftPos + imageWidth + 4;
        int currentY = topPos + 6;

        if (ClientAbilityManager.isLightHolderUnlocked()) {
            list.add(new ElementBounds(TYPE_LIGHT_HOLDER, x + 1, currentY, 18, 18));
            currentY += 22;
        }
        if (ClientAbilityManager.isTrashCanUnlocked()) {
            list.add(new ElementBounds(TYPE_TRASH_CAN, x + 1, currentY, 18, 18));
            currentY += 24;
        }
        if (ClientAbilityManager.getMiningTier() > 0) {
            list.add(new ElementBounds(TYPE_MINING, x, currentY, 20, 20));
            currentY += 23;
        }
        if (ClientAbilityManager.isTreeFellerUnlocked()) {
            list.add(new ElementBounds(TYPE_TREE_FELLER, x, currentY, 20, 20));
            currentY += 23;
        }
        if (ClientAbilityManager.isItemMagnetUnlocked()) {
            list.add(new ElementBounds(TYPE_MAGNET, x, currentY, 20, 20));
            currentY += 23;
        }
        if (ClientAbilityManager.isStepAssistUnlocked()) {
            list.add(new ElementBounds(TYPE_STEP_ASSIST, x, currentY, 20, 20));
            currentY += 23;
        }

        return list;
    }

    public static void render(GuiGraphicsExtractor graphics, InventoryScreen screen, int leftPos, int topPos, int imageWidth, int imageHeight, int mouseX, int mouseY) {
        List<ElementBounds> elements = getVisibleElements(leftPos, topPos, imageWidth);
        if (elements.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // Background panel box
        int panelX = leftPos + imageWidth + 2;
        int panelY = topPos + 4;
        int panelW = 24;
        int lastY = elements.get(elements.size() - 1).y() + elements.get(elements.size() - 1).height() + 2;
        int panelH = lastY - panelY;

        // Dark window frame
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE161616);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF3C3C3C);
        graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF3C3C3C);
        graphics.fill(panelX, panelY + 1, panelX + 1, panelY + panelH, 0xFF3C3C3C);
        graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF3C3C3C);

        ElementBounds hoveredElement = null;

        for (ElementBounds el : elements) {
            boolean hovered = mouseX >= el.x && mouseX < el.x + el.width && mouseY >= el.y && mouseY < el.y + el.height;
            if (hovered) hoveredElement = el;

            switch (el.type) {
                case TYPE_LIGHT_HOLDER -> renderLightHolder(graphics, el, hovered);
                case TYPE_TRASH_CAN -> renderTrashCan(graphics, el, hovered);
                case TYPE_MINING -> renderMiningButton(graphics, el, hovered);
                case TYPE_TREE_FELLER -> renderTreeFellerButton(graphics, el, hovered);
                case TYPE_MAGNET -> renderMagnetButton(graphics, el, hovered);
                case TYPE_STEP_ASSIST -> renderStepAssistButton(graphics, el, hovered);
            }
        }

        if (hoveredElement != null) {
            renderTooltip(graphics, font, hoveredElement, mouseX, mouseY);
        }
    }

    public static boolean isMouseOverSidebar(int leftPos, int topPos, int imageWidth, double mouseX, double mouseY) {
        List<ElementBounds> elements = getVisibleElements(leftPos, topPos, imageWidth);
        if (elements.isEmpty()) return false;
        int panelX = leftPos + imageWidth;
        int panelY = topPos;
        int panelW = 30;
        int lastY = elements.get(elements.size() - 1).y() + elements.get(elements.size() - 1).height() + 6;
        int panelH = lastY - panelY;
        return mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH;
    }

    private static void drawSlotBox(GuiGraphicsExtractor graphics, ElementBounds el) {
        // Standard vanilla-style slot beveling (18x18)
        // Outer dark shadow: top and left: 0xFF373737
        graphics.fill(el.x, el.y, el.x + 18, el.y + 1, 0xFF373737);
        graphics.fill(el.x, el.y, el.x + 1, el.y + 18, 0xFF373737);
        // Inner slot background: 0xFF8B8B8B
        graphics.fill(el.x + 1, el.y + 1, el.x + 17, el.y + 17, 0xFF8B8B8B);
        // Outer light highlight: bottom and right: 0xFFFFFFFF
        graphics.fill(el.x, el.y + 17, el.x + 18, el.y + 18, 0xFFFFFFFF);
        graphics.fill(el.x + 17, el.y, el.x + 18, el.y + 18, 0xFFFFFFFF);
    }

    private static void renderLightHolder(GuiGraphicsExtractor graphics, ElementBounds el, boolean hovered) {
        drawSlotBox(graphics, el);
        ItemStack item = ClientAbilityManager.getLightHolderItem();
        if (!item.isEmpty()) {
            graphics.fakeItem(item, el.x + 1, el.y + 1);
        } else {
            // Draw ghost torch icon
            graphics.fakeItem(new ItemStack(Items.TORCH), el.x + 1, el.y + 1);
            graphics.fill(el.x + 1, el.y + 1, el.x + 17, el.y + 17, 0x66000000);
        }
        if (hovered) {
            graphics.fill(el.x + 1, el.y + 1, el.x + 17, el.y + 17, 0x44FFFFFF);
        }
    }

    private static void renderTrashCan(GuiGraphicsExtractor graphics, ElementBounds el, boolean hovered) {
        drawSlotBox(graphics, el);
        ItemStack item = ClientAbilityManager.getTrashCanItem();
        if (!item.isEmpty()) {
            graphics.fakeItem(item, el.x + 1, el.y + 1);
        } else {
            // Draw trash can / hopper icon as background
            graphics.fakeItem(new ItemStack(Items.HOPPER), el.x + 1, el.y + 1);
            graphics.fill(el.x + 1, el.y + 1, el.x + 17, el.y + 17, 0x66000000);
        }
        if (hovered) {
            graphics.fill(el.x + 1, el.y + 1, el.x + 17, el.y + 17, 0x44FF3333);
        }
    }

    private static void renderMiningButton(GuiGraphicsExtractor graphics, ElementBounds el, boolean hovered) {
        int shape = ClientAbilityManager.getMiningShapeIndex();
        int bg = hovered ? 0xFF353535 : 0xFF222222;
        int border = (shape > 0) ? 0xFFFFA000 : (hovered ? 0xFF888888 : 0xFF444444);

        drawButtonBox(graphics, el, bg, border);

        // Draw miniature pickaxe
        graphics.fakeItem(new ItemStack(Items.DIAMOND_PICKAXE), el.x + 2, el.y + 2);

        // Draw isometric wireframe overlay in top-right corner of button
        drawIsometricWireframe(graphics, el.x + 11, el.y + 3, shape);
    }

    private static void drawIsometricWireframe(GuiGraphicsExtractor graphics, int ox, int oy, int shape) {
        // Dimensions (H, W, D):
        // 0: 1x1x1
        // 1: 2x1x1
        // 2: 2x3x1
        // 3: 3x3x1
        // 4: 3x3x2
        // 5: 3x3x3
        int color = (shape > 0) ? 0xFF00E676 : 0xFF888888;

        int h = switch (shape) {
            case 1, 2 -> 6;
            case 3, 4, 5 -> 8;
            default -> 4;
        };
        int w = switch (shape) {
            case 1 -> 4;
            case 2, 3, 4, 5 -> 7;
            default -> 4;
        };
        int d = switch (shape) {
            case 4 -> 3;
            case 5 -> 4;
            default -> 2;
        };

        // Front face rectangle
        graphics.fill(ox, oy + 8 - h, ox + w, oy + 8 - h + 1, color);
        graphics.fill(ox, oy + 8, ox + w, oy + 8 + 1, color);
        graphics.fill(ox, oy + 8 - h, ox + 1, oy + 8, color);
        graphics.fill(ox + w - 1, oy + 8 - h, ox + w, oy + 8, color);

        // Top depth line
        graphics.fill(ox + d, oy + 8 - h - d, ox + w + d, oy + 8 - h - d + 1, color);
        graphics.fill(ox, oy + 8 - h, ox + d, oy + 8 - h - d, color);
        graphics.fill(ox + w, oy + 8 - h, ox + w + d, oy + 8 - h - d, color);
        graphics.fill(ox + w + d - 1, oy + 8 - h - d, ox + w + d, oy + 8 - d, color);
    }

    private static void renderTreeFellerButton(GuiGraphicsExtractor graphics, ElementBounds el, boolean hovered) {
        boolean active = ClientAbilityManager.isTreeFellerActive();
        int bg = hovered ? 0xFF353535 : 0xFF222222;
        int border = active ? 0xFF00E676 : (hovered ? 0xFF888888 : 0xFF444444);

        drawButtonBox(graphics, el, bg, border);

        if (active) {
            // ON: Axe with tree (sapling)
            graphics.fakeItem(new ItemStack(Items.IRON_AXE), el.x + 1, el.y + 2);
            graphics.fakeItem(new ItemStack(Items.OAK_SAPLING), el.x + 5, el.y + 4);
        } else {
            // OFF: Axe with log
            graphics.fakeItem(new ItemStack(Items.IRON_AXE), el.x + 1, el.y + 2);
            graphics.fakeItem(new ItemStack(Items.OAK_LOG), el.x + 5, el.y + 4);
        }
    }

    private static void renderMagnetButton(GuiGraphicsExtractor graphics, ElementBounds el, boolean hovered) {
        boolean active = ClientAbilityManager.isItemMagnetActive();
        int bg = hovered ? 0xFF353535 : 0xFF222222;
        int border = active ? 0xFFFFD54F : (hovered ? 0xFF888888 : 0xFF444444);

        drawButtonBox(graphics, el, bg, border);

        // Draw Horseshoe Magnet in pixel art
        int mx = el.x + 5;
        int my = el.y + 4;
        int red = 0xFFE53935;
        int white = 0xFFEEEEEE;

        // Curved top arch (Red)
        graphics.fill(mx + 2, my, mx + 8, my + 2, red);
        // Left arm (Red)
        graphics.fill(mx, my + 1, mx + 3, my + 8, red);
        // Right arm (Red)
        graphics.fill(mx + 7, my + 1, mx + 10, my + 8, red);

        // Tips (White / Silver)
        graphics.fill(mx, my + 8, mx + 3, my + 11, white);
        graphics.fill(mx + 7, my + 8, mx + 10, my + 11, white);

        if (active) {
            // Active attraction sparks
            graphics.fill(mx + 4, my + 9, mx + 6, my + 11, 0xFFFFA000);
        }
    }

    private static void renderStepAssistButton(GuiGraphicsExtractor graphics, ElementBounds el, boolean hovered) {
        boolean active = ClientAbilityManager.isStepAssistActive();
        int bg = hovered ? 0xFF353535 : 0xFF222222;
        int border = active ? 0xFFFFD54F : (hovered ? 0xFF888888 : 0xFF444444);

        drawButtonBox(graphics, el, bg, border);

        // Draw Ladder icon
        graphics.fakeItem(new ItemStack(Items.LADDER), el.x + 2, el.y + 2);
    }

    private static void drawButtonBox(GuiGraphicsExtractor graphics, ElementBounds el, int bg, int border) {
        graphics.fill(el.x, el.y, el.x + el.width, el.y + el.height, bg);
        graphics.fill(el.x, el.y, el.x + el.width, el.y + 1, border);
        graphics.fill(el.x, el.y + el.height - 1, el.x + el.width, el.y + el.height, border);
        graphics.fill(el.x, el.y + 1, el.x + 1, el.y + el.height, border);
        graphics.fill(el.x + el.width - 1, el.y, el.x + el.width, el.y + el.height, border);
    }

    private static void renderTooltip(GuiGraphicsExtractor graphics, Font font, ElementBounds el, int mouseX, int mouseY) {
        List<Component> tip = new ArrayList<>();

        switch (el.type) {
            case TYPE_LIGHT_HOLDER -> {
                tip.add(Component.literal("§e§lДержатель света"));
                ItemStack item = ClientAbilityManager.getLightHolderItem();
                if (item.isEmpty()) {
                    tip.add(Component.literal("§7(Ячейка пуста)"));
                    tip.add(Component.literal("§8Поместите светящийся блок"));
                } else {
                    tip.add(Component.literal("§f" + item.getHoverName().getString()));
                    tip.add(Component.literal("§a✔ Активен на груди"));
                }
                tip.add(Component.literal("§7Освещает мир вокруг (LambDynamicLights)"));
            }
            case TYPE_TRASH_CAN -> {
                tip.add(Component.literal("§8§lМусорка"));
                ItemStack item = ClientAbilityManager.getTrashCanItem();
                if (!item.isEmpty()) {
                    tip.add(Component.literal("§7Внутри: §f" + item.getHoverName().getString() + " x" + item.getCount()));
                    tip.add(Component.literal("§cУничтожится при следующем удалении!"));
                } else {
                    tip.add(Component.literal("§7(Ячейка пуста)"));
                }
                tip.add(Component.literal("§eAlt + Клик §7по предмету для быстрой отправки в мусорку"));
            }
            case TYPE_MINING -> {
                int shape = ClientAbilityManager.getMiningShapeIndex();
                int tier = ClientAbilityManager.getMiningTier();
                String desc = getShapeDescription(shape);
                String maxDesc = getShapeDescription(tier);
                tip.add(Component.literal("§6§lПрогрессивное копание"));
                tip.add(Component.literal("§eТекущий режим: §f" + desc + " §7(В×Ш×Г)"));
                tip.add(Component.literal("§7ЛКМ: сменить режим (от 1×1×1 до " + maxDesc + ")"));
                tip.add(Component.literal("§8Сохраняет прочность инструмента (не ломает в 0)"));
            }
            case TYPE_TREE_FELLER -> {
                boolean active = ClientAbilityManager.isTreeFellerActive();
                tip.add(Component.literal("§a§lРубка деревьев"));
                if (active) {
                    tip.add(Component.literal("§7Режим: §aТопор с деревом (ВКЛ)"));
                    tip.add(Component.literal("§fСрубает всё дерево и листву сразу!"));
                } else {
                    tip.add(Component.literal("§7Режим: §cТопор с бревном (ВЫКЛ)"));
                    tip.add(Component.literal("§8Срубает только один блок"));
                }
                tip.add(Component.literal("§eЛКМ: переключить"));
            }
            case TYPE_MAGNET -> {
                boolean active = ClientAbilityManager.isItemMagnetActive();
                tip.add(Component.literal("§c§lМагнит предметов"));
                tip.add(Component.literal("§7Статус: " + (active ? "§aВКЛ" : "§cВЫКЛ")));
                tip.add(Component.literal("§7Притягивает предметы и сферы опыта (10 блоков)"));
                tip.add(Component.literal("§eЛКМ: переключить"));
            }
            case TYPE_STEP_ASSIST -> {
                boolean active = ClientAbilityManager.isStepAssistActive();
                tip.add(Component.literal("§e§lАвтоподъем на 1 блок"));
                tip.add(Component.literal("§7Статус: " + (active ? "§aВКЛ" : "§cВЫКЛ")));
                tip.add(Component.literal("§7Плавный шаг вверх без необходимости прыжка"));
                tip.add(Component.literal("§eЛКМ: переключить"));
            }
        }

        graphics.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
    }

    public static String getShapeDescription(int shape) {
        return switch (shape) {
            case 1 -> "2×1×1";
            case 2 -> "2×3×1";
            case 3 -> "3×3×1";
            case 4 -> "3×3×2";
            case 5 -> "3×3×3";
            default -> "1×1×1";
        };
    }

    public static boolean mouseClicked(InventoryScreen screen, MouseButtonEvent event, int leftPos, int topPos, int imageWidth, int imageHeight) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT && event.button() != 0) return false;

        List<ElementBounds> elements = getVisibleElements(leftPos, topPos, imageWidth);
        for (ElementBounds el : elements) {
            if (event.x() >= el.x - 2 && event.x() <= el.x + el.width + 2 && event.y() >= el.y - 2 && event.y() <= el.y + el.height + 2) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return false;

                switch (el.type) {
                    case TYPE_MINING -> {
                        ClientAbilityManager.cycleMining();
                        mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.2f);
                        return true;
                    }
                    case TYPE_TREE_FELLER -> {
                        ClientAbilityManager.toggleTreeFeller();
                        mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.1f);
                        return true;
                    }
                    case TYPE_MAGNET -> {
                        ClientAbilityManager.toggleMagnet();
                        mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.0f);
                        return true;
                    }
                    case TYPE_STEP_ASSIST -> {
                        ClientAbilityManager.toggleStepAssist();
                        mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.0f);
                        return true;
                    }
                    case TYPE_LIGHT_HOLDER -> {
                        ItemStack carried = screen.getMenu().getCarried();
                        ItemStack currentInSlot = ClientAbilityManager.getLightHolderItem();

                        if (!carried.isEmpty()) {
                            if (AbilityManager.isLightEmittingBlock(carried)) {
                                // Swap carried stack and light slot
                                ItemStack newSlot = carried.copy();
                                screen.getMenu().setCarried(currentInSlot);
                                ClientAbilityManager.setLightHolderItem(newSlot);
                                ClientAbilityManager.sendUpdateSlot(ClientAbilityManager.SLOT_LIGHT_HOLDER, newSlot);
                                mc.player.playSound(SoundEvents.ITEM_PICKUP, 0.4f, 1.1f);
                            } else {
                                mc.player.playSound(SoundEvents.DISPENSER_FAIL, 0.5f, 1.0f);
                            }
                        } else if (!currentInSlot.isEmpty()) {
                            // Pick up item into cursor
                            screen.getMenu().setCarried(currentInSlot.copy());
                            ClientAbilityManager.setLightHolderItem(ItemStack.EMPTY);
                            ClientAbilityManager.sendUpdateSlot(ClientAbilityManager.SLOT_LIGHT_HOLDER, ItemStack.EMPTY);
                            mc.player.playSound(SoundEvents.ITEM_PICKUP, 0.4f, 1.0f);
                        }
                        return true;
                    }
                    case TYPE_TRASH_CAN -> {
                        ItemStack carried = screen.getMenu().getCarried();
                        ItemStack currentTrash = ClientAbilityManager.getTrashCanItem();

                        if (!carried.isEmpty()) {
                            // Place into trash can (destroying previous, cursor becomes empty)
                            ItemStack newTrash = carried.copy();
                            screen.getMenu().setCarried(ItemStack.EMPTY);
                            ClientAbilityManager.setTrashCanItem(newTrash);
                            ClientAbilityManager.sendUpdateSlot(ClientAbilityManager.SLOT_TRASH_CAN, newTrash);
                            mc.player.playSound(SoundEvents.ITEM_PICKUP, 0.4f, 0.8f);
                        } else if (!currentTrash.isEmpty()) {
                            // Recover item from trash into cursor
                            screen.getMenu().setCarried(currentTrash.copy());
                            ClientAbilityManager.setTrashCanItem(ItemStack.EMPTY);
                            ClientAbilityManager.sendUpdateSlot(ClientAbilityManager.SLOT_TRASH_CAN, ItemStack.EMPTY);
                            mc.player.playSound(SoundEvents.ITEM_PICKUP, 0.4f, 1.0f);
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
