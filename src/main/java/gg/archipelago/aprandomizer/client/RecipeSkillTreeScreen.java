package gg.archipelago.aprandomizer.client;

import com.mojang.blaze3d.platform.InputConstants;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

import java.util.*;

public class RecipeSkillTreeScreen extends Screen {
    private final Screen parent;
    private int selectedTab = 0; // 0: Tools/Weapons, 1: Armor, 2: Utility, 3: All

    // Canvas Panning
    private double scrollX = 0;
    private double scrollY = 0;
    private boolean isDragging = false;
    private double lastDragMouseX = 0;
    private double lastDragMouseY = 0;

    // Window bounds
    private int windowX = 10;
    private int windowY = 20;
    private int windowWidth = 300;
    private int windowHeight = 200;
    private int canvasX = 14;
    private int canvasY = 56;
    private int canvasWidth = 292;
    private int canvasHeight = 160;

    // Node Dimensions in Tree
    private static final int NODE_SIZE = 26;
    private static final int SPACING_X = 54;
    private static final int SPACING_Y = 34;
    private static final int BRANCH_GAP = 16;

    // Tab Root Definitions
    private static final List<String> TOOLS_WEAPONS_ROOTS = List.of(
            "node_83", "node_89", "node_86", "node_12", "node_36", "node_47", "node_75", "node_109"
    );
    private static final List<String> ARMOR_ROOTS = List.of(
            "node_130", "node_150", "node_162", "node_169"
    );
    private static final List<String> UTILITY_ROOTS = List.of(
            "node_70", "node_17", "node_114", "node_81", "node_105", "node_107", "node_116", "node_74"
    );

    private final Map<Integer, Map<String, NodePos>> tabLayouts = new HashMap<>();
    private final Map<String, ItemStack> itemCache = new HashMap<>();

    public record NodePos(int x, int y) {}

    public RecipeSkillTreeScreen(Screen parent) {
        super(Component.literal("✦ Древо рецептов Archipelago ✦"));
        this.parent = parent;
        computeAllLayouts();
    }

    private void computeAllLayouts() {
        tabLayouts.put(0, computeLayoutForRoots(TOOLS_WEAPONS_ROOTS));
        tabLayouts.put(1, computeLayoutForRoots(ARMOR_ROOTS));
        tabLayouts.put(2, computeLayoutForRoots(UTILITY_ROOTS));

        List<String> allRoots = new ArrayList<>();
        allRoots.addAll(TOOLS_WEAPONS_ROOTS);
        allRoots.addAll(ARMOR_ROOTS);
        allRoots.addAll(UTILITY_ROOTS);
        tabLayouts.put(3, computeLayoutForRoots(allRoots));
    }

    private Map<String, NodePos> computeLayoutForRoots(List<String> rootIds) {
        Map<String, NodePos> positions = new HashMap<>();
        int[] currentY = new int[]{20};

        for (String rootId : rootIds) {
            layoutNodeRecursive(rootId, currentY, positions);
            currentY[0] += BRANCH_GAP;
        }

        return positions;
    }

    private int layoutNodeRecursive(String nodeId, int[] currentY, Map<String, NodePos> positions) {
        RecipeTreeNode node = RecipeTreeManager.getNodeById(nodeId);
        if (node == null) return currentY[0];

        int x = (node.tier() - 1) * SPACING_X + 24;

        List<String> validChildren = new ArrayList<>();
        List<RecipeTreeNode> allNodes = RecipeTreeManager.getAllNodes();
        for (RecipeTreeNode candidate : allNodes) {
            if (nodeId.equals(candidate.parentId()) && candidate.tier() > 0 && !"minecraft:air".equals(candidate.item())) {
                validChildren.add(candidate.id());
            }
        }

        if (validChildren.isEmpty()) {
            int y = currentY[0];
            currentY[0] += SPACING_Y;
            positions.put(nodeId, new NodePos(x, y));
            return y;
        }

        List<Integer> childYs = new ArrayList<>();
        for (String chId : validChildren) {
            childYs.add(layoutNodeRecursive(chId, currentY, positions));
        }

        int avgY = childYs.stream().mapToInt(Integer::intValue).sum() / childYs.size();
        positions.put(nodeId, new NodePos(x, avgY));
        return avgY;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        this.windowWidth = Math.max(360, Math.min(this.width - 20, 560));
        this.windowHeight = Math.max(230, this.height - 36);
        this.windowX = (this.width - this.windowWidth) / 2;
        this.windowY = 16;

        this.canvasX = this.windowX + 8;
        this.canvasY = this.windowY + 44;
        this.canvasWidth = this.windowWidth - 16;
        this.canvasHeight = this.windowHeight - 72;

        // 1. Tab Buttons (⚔ Оружие, 🛡 Броня, ⚙ Механизмы, ✦ Все древо)
        int tabCount = 4;
        int tabGap = 4;
        int totalTabGaps = (tabCount - 1) * tabGap;
        int tabW = (this.canvasWidth - totalTabGaps) / tabCount;
        int tabsStartX = this.canvasX;
        int tabsY = this.windowY + 22;

        String[] tabLabels = new String[]{"⚔ Оружие", "🛡 Броня", "⚙ Быт", "✦ Все древо (77)"};
        String[] tabTooltips = new String[]{
                "Оружие и инструменты (Мечи, Кирки, Топоры, Луки, Мотыги)",
                "Доспехи (Шлемы, Нагрудники, Поножи, Ботинки)",
                "Металлургия и полезные механизмы (Печи, Наковальни, Зелья)",
                "Все 77 доступных рецептов на общем холсте"
        };

        for (int i = 0; i < tabCount; i++) {
            final int tabIdx = i;
            Button tabBtn = Button.builder(Component.literal(tabLabels[i]), b -> {
                this.selectedTab = tabIdx;
                centerView();
            }).bounds(tabsStartX + i * (tabW + tabGap), tabsY, tabW, 18).build();
            tabBtn.setTooltip(Tooltip.create(Component.literal(tabTooltips[i])));
            this.addRenderableWidget(tabBtn);
        }

        // 2. Bottom Toolbar Buttons: Center View & Close
        int btnW = 90;
        int btnH = 20;
        int bottomY = this.windowY + this.windowHeight - 24;

        this.addRenderableWidget(Button.builder(Component.literal("⌖ Центр"), b -> centerView())
                .bounds(this.canvasX, bottomY, btnW, btnH).build());

        this.addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> this.onClose())
                .bounds(this.canvasX + this.canvasWidth - btnW, bottomY, btnW, btnH).build());

        centerView();
    }

    private void centerView() {
        Map<String, NodePos> layout = tabLayouts.getOrDefault(selectedTab, Collections.emptyMap());
        if (layout.isEmpty()) {
            this.scrollX = 20;
            this.scrollY = 20;
            return;
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (NodePos pos : layout.values()) {
            minX = Math.min(minX, pos.x);
            maxX = Math.max(maxX, pos.x + NODE_SIZE);
            minY = Math.min(minY, pos.y);
            maxY = Math.max(maxY, pos.y + NODE_SIZE);
        }

        int treeW = maxX - minX;
        int treeH = maxY - minY;

        if (treeW < this.canvasWidth) {
            this.scrollX = (this.canvasWidth - treeW) / 2.0 - minX;
        } else {
            this.scrollX = 20 - minX;
        }

        if (treeH < this.canvasHeight) {
            this.scrollY = (this.canvasHeight - treeH) / 2.0 - minY;
        } else {
            this.scrollY = 20 - minY;
        }
    }

    private ItemStack getItemStack(RecipeTreeNode node) {
        return itemCache.computeIfAbsent(node.id(), id -> {
            try {
                if (node.item() != null && !node.item().isBlank()) {
                    Identifier itemIdent = Identifier.parse(node.item());
                    var item = BuiltInRegistries.ITEM.getValue(itemIdent);
                    if (item != null && item != Items.AIR) {
                        return new ItemStack(item);
                    }
                }
            } catch (Exception ignored) {}
            return new ItemStack(Items.CRAFTING_TABLE);
        });
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 1. Render Window Outer Frame
        int bgMain = 0xEE121212;
        int borderMain = 0xFF3E3E3E;

        // Window background
        graphics.fill(this.windowX, this.windowY, this.windowX + this.windowWidth, this.windowY + this.windowHeight, bgMain);
        // Window outer border
        graphics.fill(this.windowX, this.windowY, this.windowX + this.windowWidth, this.windowY + 1, borderMain);
        graphics.fill(this.windowX, this.windowY + this.windowHeight - 1, this.windowX + this.windowWidth, this.windowY + this.windowHeight, borderMain);
        graphics.fill(this.windowX, this.windowY + 1, this.windowX + 1, this.windowY + this.windowHeight, borderMain);
        graphics.fill(this.windowX + this.windowWidth - 1, this.windowY, this.windowX + this.windowWidth, this.windowY + this.windowHeight, borderMain);

        // Header Title
        graphics.centeredText(this.font, Component.literal("✦ Древо рецептов Archipelago ✦"), this.windowX + this.windowWidth / 2, this.windowY + 6, 0xFFFFA000);

        // 2. Tickets & Unlocked Counter
        int tickets = ClientRecipeManager.getTickets();
        boolean ticketMode = ClientRecipeManager.isTicketMode();
        Set<ResourceKey<Recipe<?>>> unlocked = ClientRecipeManager.getUnlockedRecipeKeys();

        List<RecipeTreeNode> allUnlockable = RecipeTreeManager.getUnlockableNodes();
        int unlockedCount = 0;
        for (RecipeTreeNode n : allUnlockable) {
            if (n.isUnlocked(unlocked)) unlockedCount++;
        }
        int totalCount = allUnlockable.size();
        int totalPct = totalCount > 0 ? (unlockedCount * 100 / totalCount) : 100;

        String ticketText = ticketMode ? ("🎟 Талоны: §e" + tickets) : "🎲 Случайный режим";
        String progressText = "✔ Открыто: §a" + unlockedCount + "/" + totalCount + " (" + totalPct + "%)";

        graphics.text(this.font, Component.literal(ticketText), this.canvasX + 6, this.windowY + 6, 0xFFFFFFFF);
        int progW = this.font.width(progressText);
        graphics.text(this.font, Component.literal(progressText), this.canvasX + this.canvasWidth - progW - 6, this.windowY + 6, 0xFFE0E0E0);

        // 3. Render Widgets (Buttons)
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // 4. Tab Active Underline
        int tabCount = 4;
        int tabGap = 4;
        int totalTabGaps = (tabCount - 1) * tabGap;
        int tabW = (this.canvasWidth - totalTabGaps) / tabCount;
        int tabsStartX = this.canvasX;
        int tabsY = this.windowY + 22;

        int activeX = tabsStartX + this.selectedTab * (tabW + tabGap);
        graphics.fill(activeX, tabsY + 16, activeX + tabW, tabsY + 18, 0xFFFFA000);

        // 5. Canvas Inner Box
        int canvasBg = 0xFF181818;
        graphics.fill(this.canvasX, this.canvasY, this.canvasX + this.canvasWidth, this.canvasY + this.canvasHeight, canvasBg);
        graphics.fill(this.canvasX, this.canvasY, this.canvasX + this.canvasWidth, this.canvasY + 1, 0xFF2A2A2A);
        graphics.fill(this.canvasX, this.canvasY + this.canvasHeight - 1, this.canvasX + this.canvasWidth, this.canvasY + this.canvasHeight, 0xFF2A2A2A);
        graphics.fill(this.canvasX, this.canvasY + 1, this.canvasX + 1, this.canvasY + this.canvasHeight, 0xFF2A2A2A);
        graphics.fill(this.canvasX + this.canvasWidth - 1, this.canvasY, this.canvasX + this.canvasWidth, this.canvasY + this.canvasHeight, 0xFF2A2A2A);

        // 6. Draw Tree inside Scissor
        Map<String, NodePos> currentLayout = tabLayouts.getOrDefault(this.selectedTab, Collections.emptyMap());
        RecipeTreeNode hoveredNode = null;
        int threshold = ClientRecipeManager.getTierThreshold();

        if (this.canvasHeight > 0 && !currentLayout.isEmpty()) {
            graphics.enableScissor(this.canvasX + 1, this.canvasY + 1, this.canvasX + this.canvasWidth - 1, this.canvasY + this.canvasHeight - 1);
            try {
                long time = System.currentTimeMillis();
                float pulse = (float) (Math.sin(time / 200.0) * 0.5 + 0.5);
                int pulseColor = 0xFF000000 | ((int) (220 + 35 * pulse) << 16) | ((int) (170 + 65 * pulse) << 8) | ((int) (30 + 40 * pulse));

                // 6a. Draw Connecting Lines first
                for (Map.Entry<String, NodePos> entry : currentLayout.entrySet()) {
                    RecipeTreeNode node = RecipeTreeManager.getNodeById(entry.getKey());
                    if (node == null) continue;

                    String parentId = node.parentId();
                    if (parentId != null && currentLayout.containsKey(parentId)) {
                        NodePos parentPos = currentLayout.get(parentId);
                        NodePos childPos = entry.getValue();

                        int px = this.canvasX + (int) this.scrollX + parentPos.x + NODE_SIZE;
                        int py = this.canvasY + (int) this.scrollY + parentPos.y + NODE_SIZE / 2;
                        int cx = this.canvasX + (int) this.scrollX + childPos.x;
                        int cy = this.canvasY + (int) this.scrollY + childPos.y + NODE_SIZE / 2;

                        RecipeTreeManager.UnlockStatus status = RecipeTreeManager.getUnlockStatus(node, unlocked, tickets, threshold);
                        int lineColor = switch (status) {
                            case UNLOCKED -> 0xFF388E3C;
                            case AVAILABLE -> pulseColor;
                            default -> 0xFF424242;
                        };

                        int midX = (px + cx) / 2;

                        // Drop Shadow (black)
                        drawOrthogonalLine(graphics, px + 1, py + 1, midX + 1, cy + 1, cx + 1, 0xAA000000);
                        // Colored Line
                        drawOrthogonalLine(graphics, px, py, midX, cy, cx, lineColor);
                    }
                }

                // 6b. Draw Node Boxes & Icons
                for (Map.Entry<String, NodePos> entry : currentLayout.entrySet()) {
                    RecipeTreeNode node = RecipeTreeManager.getNodeById(entry.getKey());
                    if (node == null) continue;

                    NodePos pos = entry.getValue();
                    int nx = this.canvasX + (int) this.scrollX + pos.x;
                    int ny = this.canvasY + (int) this.scrollY + pos.y;

                    // Culling
                    if (nx + NODE_SIZE < this.canvasX || nx > this.canvasX + this.canvasWidth ||
                            ny + NODE_SIZE < this.canvasY || ny > this.canvasY + this.canvasHeight) {
                        continue;
                    }

                    boolean isHovered = (mouseX >= nx - 2 && mouseX <= nx + NODE_SIZE + 2 &&
                            mouseY >= ny - 2 && mouseY <= ny + NODE_SIZE + 2 &&
                            mouseX >= this.canvasX && mouseX <= this.canvasX + this.canvasWidth &&
                            mouseY >= this.canvasY && mouseY <= this.canvasY + this.canvasHeight);

                    if (isHovered) {
                        hoveredNode = node;
                    }

                    RecipeTreeManager.UnlockStatus status = RecipeTreeManager.getUnlockStatus(node, unlocked, tickets, threshold);

                    // Box Background
                    int nodeBg;
                    if (status == RecipeTreeManager.UnlockStatus.UNLOCKED) {
                        nodeBg = isHovered ? 0xFF1B3822 : 0xFF142919;
                    } else {
                        nodeBg = isHovered ? 0xFF323232 : 0xFF202020;
                    }
                    graphics.fill(nx, ny, nx + NODE_SIZE, ny + NODE_SIZE, nodeBg);

                    // Border Color
                    int borderColor = switch (status) {
                        case UNLOCKED -> 0xFF00E676;
                        case AVAILABLE -> pulseColor;
                        case LOCKED_TREE -> 0xFF4E4E4E;
                        case LOCKED_TIER -> 0xFFC62828;
                        case NO_TICKETS -> 0xFFEF6C00;
                    };

                    if (status == RecipeTreeManager.UnlockStatus.UNLOCKED) {
                        // 2px Double thick Emerald Frame
                        graphics.fill(nx, ny, nx + NODE_SIZE, ny + 2, 0xFF00E676);
                        graphics.fill(nx, ny + NODE_SIZE - 2, nx + NODE_SIZE, ny + NODE_SIZE, 0xFF00E676);
                        graphics.fill(nx, ny + 2, nx + 2, ny + NODE_SIZE - 2, 0xFF00E676);
                        graphics.fill(nx + NODE_SIZE - 2, ny + 2, nx + NODE_SIZE, ny + NODE_SIZE - 2, 0xFF00E676);

                        // Outer subtle emerald glow
                        graphics.fill(nx - 1, ny - 1, nx + NODE_SIZE + 1, ny, 0x8800E676);
                        graphics.fill(nx - 1, ny + NODE_SIZE, nx + NODE_SIZE + 1, ny + NODE_SIZE + 1, 0x8800E676);
                        graphics.fill(nx - 1, ny, nx, ny + NODE_SIZE, 0x8800E676);
                        graphics.fill(nx + NODE_SIZE, ny, nx + NODE_SIZE + 1, ny + NODE_SIZE, 0x8800E676);
                    } else {
                        graphics.fill(nx, ny, nx + NODE_SIZE, ny + 1, borderColor);
                        graphics.fill(nx, ny + NODE_SIZE - 1, nx + NODE_SIZE, ny + NODE_SIZE, borderColor);
                        graphics.fill(nx, ny + 1, nx + 1, ny + NODE_SIZE, borderColor);
                        graphics.fill(nx + NODE_SIZE - 1, ny, nx + NODE_SIZE, ny + NODE_SIZE, borderColor);
                    }

                    if (status == RecipeTreeManager.UnlockStatus.AVAILABLE) {
                        // Extra outer glowing pixel on available
                        graphics.fill(nx - 1, ny - 1, nx + NODE_SIZE + 1, ny, pulseColor);
                        graphics.fill(nx - 1, ny + NODE_SIZE, nx + NODE_SIZE + 1, ny + NODE_SIZE + 1, pulseColor);
                        graphics.fill(nx - 1, ny, nx, ny + NODE_SIZE, pulseColor);
                        graphics.fill(nx + NODE_SIZE, ny, nx + NODE_SIZE + 1, ny + NODE_SIZE, pulseColor);
                    }

                    // Tier bottom stripe
                    int tierColor = RecipeTreeManager.getTierColor(node.tier());
                    graphics.fill(nx + 2, ny + NODE_SIZE - 3, nx + NODE_SIZE - 2, ny + NODE_SIZE - 1, tierColor);

                    // Item Icon
                    ItemStack stack = getItemStack(node);
                    graphics.fakeItem(stack, nx + 5, ny + 4);

                    // Unlocked checkmark indicator
                    if (status == RecipeTreeManager.UnlockStatus.UNLOCKED) {
                        int bx = nx + NODE_SIZE - 6;
                        int by = ny + 2;
                        graphics.fill(bx, by, bx + 4, by + 4, 0xFF00E676);
                        graphics.fill(bx + 1, by + 1, bx + 3, by + 3, 0xFFFFFFFF);
                    }
                }
            } finally {
                graphics.disableScissor();
            }
        }

        // 7. Navigation hint in bottom center
        String navHint = "§7Удерживайте ПКМ для перемещения | Колесо мыши для прокрутки";
        graphics.centeredText(this.font, Component.literal(navHint), this.windowX + this.windowWidth / 2, this.windowY + this.windowHeight - 20, 0xFF888888);

        // 8. Tooltip
        if (hoveredNode != null) {
            renderNodeTooltip(graphics, hoveredNode, unlocked, threshold, mouseX, mouseY);
        }
    }

    private void drawOrthogonalLine(GuiGraphicsExtractor graphics, int x1, int y1, int midX, int y2, int x2, int color) {
        // Segment 1: Horizontal from x1 to midX at y1
        fillLine(graphics, x1, y1, midX, y1, color);
        // Segment 2: Vertical from y1 to y2 at midX
        fillLine(graphics, midX, y1, midX, y2, color);
        // Segment 3: Horizontal from midX to x2 at y2
        fillLine(graphics, midX, y2, x2, y2, color);
    }

    private void fillLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        graphics.fill(minX, minY, maxX + 1, maxY + 1, color);
    }

    private void renderNodeTooltip(GuiGraphicsExtractor graphics, RecipeTreeNode node, Set<ResourceKey<Recipe<?>>> unlocked, int threshold, int mouseX, int mouseY) {
        List<Component> tip = new ArrayList<>();
        String tierName = switch (node.tier()) {
            case 1 -> "Тир 1 (Дерево)";
            case 2 -> "Тир 2 (Камень/Кожа)";
            case 3 -> "Тир 3 (Медь/Слитки)";
            case 4 -> "Тир 4 (Железо)";
            case 5 -> "Тир 5 (Золото)";
            case 6 -> "Тир 6 (Алмаз)";
            case 7 -> "Тир 7 (Незерит)";
            default -> "Тир " + node.tier();
        };

        tip.add(Component.literal("§6§l" + node.name()));
        tip.add(Component.literal("§e" + tierName + "   §7(Рецептов: §f" + node.recipes().size() + "§7)"));

        RecipeTreeManager.UnlockStatus status = RecipeTreeManager.getUnlockStatus(node, unlocked, ClientRecipeManager.getTickets(), threshold);

        if (node.parentId() != null && !node.parentId().isBlank() && !"node_11".equals(node.parentId())) {
            RecipeTreeNode parentNode = RecipeTreeManager.getNodeById(node.parentId());
            String pName = parentNode != null ? parentNode.name() : node.parentId();
            boolean pUnlocked = parentNode != null && parentNode.isUnlocked(unlocked);
            tip.add(Component.literal("§7Предок: " + (pUnlocked ? "§a✔ " : "§c✖ ") + pName));
        }

        if (node.tier() > 1 && threshold > 0) {
            int prevTier = node.tier() - 1;
            List<RecipeTreeNode> prevNodes = RecipeTreeManager.getNodesByTier(prevTier);
            int prevUnlocked = 0;
            for (RecipeTreeNode pn : prevNodes) {
                if (pn.isUnlocked(unlocked)) prevUnlocked++;
            }
            int required = (int) Math.ceil(prevNodes.size() * (threshold / 100.0));
            boolean tierMet = prevUnlocked >= required;
            tip.add(Component.literal("§7Порог Т" + prevTier + " (" + threshold + "%): "
                    + (tierMet ? "§a" : "§c") + prevUnlocked + "/" + required + " (всего " + prevNodes.size() + ")"));
        }

        tip.add(Component.empty());
        switch (status) {
            case UNLOCKED -> tip.add(Component.literal("§2✔ Рецепт уже разблокирован"));
            case AVAILABLE -> tip.add(Component.literal("§a► Нажмите ЛКМ, чтобы открыть за 1 талон!"));
            case NO_TICKETS -> tip.add(Component.literal("§6✖ Нет доступных талонов (нужен 1 талон)"));
            case LOCKED_TREE -> tip.add(Component.literal("§c✖ Сначала откройте предыдущий рецепт в древе"));
            case LOCKED_TIER -> tip.add(Component.literal("§c✖ Откройте больше рецептов предыдущего тира"));
        }

        graphics.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
    }

    private RecipeTreeNode getNodeAtMouse(double mouseX, double mouseY) {
        if (mouseX < this.canvasX || mouseX > this.canvasX + this.canvasWidth ||
                mouseY < this.canvasY || mouseY > this.canvasY + this.canvasHeight) {
            return null;
        }

        Map<String, NodePos> currentLayout = tabLayouts.getOrDefault(this.selectedTab, Collections.emptyMap());
        for (Map.Entry<String, NodePos> entry : currentLayout.entrySet()) {
            NodePos pos = entry.getValue();
            int nx = this.canvasX + (int) this.scrollX + pos.x;
            int ny = this.canvasY + (int) this.scrollY + pos.y;

            if (mouseX >= nx - 2 && mouseX <= nx + NODE_SIZE + 2 && mouseY >= ny - 2 && mouseY <= ny + NODE_SIZE + 2) {
                return RecipeTreeManager.getNodeById(entry.getKey());
            }
        }

        return null;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ClientKeyHandler.OPEN_RECIPE_TREE.matches(event)) {
            this.onClose();
            return true;
        }
        if (this.minecraft != null && this.minecraft.options != null && this.minecraft.options.keyInventory.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        if (event.x() >= this.canvasX && event.x() <= this.canvasX + this.canvasWidth &&
                event.y() >= this.canvasY && event.y() <= this.canvasY + this.canvasHeight) {

            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                RecipeTreeNode clicked = getNodeAtMouse(event.x(), event.y());
                if (clicked != null) {
                    RecipeTreeManager.UnlockStatus status = ClientRecipeManager.getUnlockStatus(clicked);
                    if (status == RecipeTreeManager.UnlockStatus.AVAILABLE) {
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2F));
                            if (this.minecraft.player != null) {
                                this.minecraft.player.sendSystemMessage(Component.literal("§a[Archipelago] Открывается рецепт: §e" + clicked.name() + "§a..."));
                            }
                        }
                        ClientRecipeManager.requestUnlock(clicked.id());
                        return true;
                    } else if (status == RecipeTreeManager.UnlockStatus.UNLOCKED) {
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            if (this.minecraft.player != null) {
                                this.minecraft.player.sendSystemMessage(Component.literal("§2✔ Рецепт «" + clicked.name() + "» уже разблокирован!"));
                            }
                        }
                        return true;
                    } else if (status == RecipeTreeManager.UnlockStatus.NO_TICKETS) {
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.6F));
                            if (this.minecraft.player != null) {
                                this.minecraft.player.sendSystemMessage(Component.literal("§6[Archipelago] Для открытия «" + clicked.name() + "» требуется 1 талон на рецепт!"));
                            }
                        }
                        return true;
                    } else if (status == RecipeTreeManager.UnlockStatus.LOCKED_TREE) {
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.4F));
                            if (this.minecraft.player != null) {
                                RecipeTreeNode parent = RecipeTreeManager.getNodeById(clicked.parentId());
                                String parentName = parent != null ? parent.name() : "предыдущий";
                                this.minecraft.player.sendSystemMessage(Component.literal("§c[Archipelago] Сначала откройте «" + parentName + "» в древе!"));
                            }
                        }
                        return true;
                    } else if (status == RecipeTreeManager.UnlockStatus.LOCKED_TIER) {
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.4F));
                            if (this.minecraft.player != null) {
                                int thresh = ClientRecipeManager.getTierThreshold();
                                this.minecraft.player.sendSystemMessage(Component.literal("§c[Archipelago] Откройте больше рецептов предыдущего тира (нужно " + thresh + "%)!"));
                            }
                        }
                        return true;
                    } else {
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
                        }
                        return true;
                    }
                }
                return true;
            } else if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
                // RMB: Start dragging canvas
                this.isDragging = true;
                this.lastDragMouseX = event.x();
                this.lastDragMouseY = event.y();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (super.mouseDragged(event, dragX, dragY)) {
            return true;
        }

        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && this.isDragging) {
            double dx = event.x() - this.lastDragMouseX;
            double dy = event.y() - this.lastDragMouseY;
            this.scrollX += dx;
            this.scrollY += dy;
            this.lastDragMouseX = event.x();
            this.lastDragMouseY = event.y();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        if (mouseX >= this.canvasX && mouseX <= this.canvasX + this.canvasWidth &&
                mouseY >= this.canvasY && mouseY <= this.canvasY + this.canvasHeight) {
            this.scrollY += verticalAmount * 24.0;
            this.scrollX += horizontalAmount * 24.0;
            return true;
        }

        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
