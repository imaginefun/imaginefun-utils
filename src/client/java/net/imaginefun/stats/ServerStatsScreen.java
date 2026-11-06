package net.imaginefun.stats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class ServerStatsScreen extends Screen {

    private static final Component TITLE = Component.translatable("gui.stats");
    private static final int LIST_WIDTH = 280;
    private static final int ROW_HEIGHT = 20;
    private static final int ICON_SIZE = 16;
    private static final int TEXT_PADDING = 2;
    private static final int ICON_GAP = 4;
    private static final int ODD_ROW_COLOR = 0xFFBABABA;
    private static final int EVEN_ROW_COLOR = 0xFFFFFFFF;
    private static final int LIST_TOP = 33;

    private final @Nullable Screen lastScreen;
    private final TabManager tabManager = new TabManager(widget -> addRenderableWidget(widget), widget -> removeWidget(widget));
    private HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private @Nullable MenuTabBar tabBar;

    public ServerStatsScreen(@Nullable Screen lastScreen) {
        super(TITLE);
        this.lastScreen = lastScreen;
    }

    public void onSnapshotReplaced() {
        rebuildWidgets();
    }

    @Override
    protected void init() {
        layout = new HeaderAndFooterLayout(this);
        List<Tab> tabs = ServerStats.categories().stream().<Tab>map(CategoryTab::new).toList();
        tabBar = ServerStatsTabBar.create(tabManager, width, tabs);
        addRenderableWidget(tabBar);
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        layout.visitWidgets(widget -> {
            widget.setTabOrderGroup(1);
            addRenderableWidget(widget);
        });
        if (!tabs.isEmpty()) {
            tabBar.selectTab(0, false);
        }
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (tabBar == null) {
            return;
        }
        tabBar.arrangeElements(width);
        int tabAreaTop = tabBar.getRectangle().bottom();
        ScreenRectangle tabArea = new ScreenRectangle(0, tabAreaTop, width, height - layout.getFooterHeight() - tabAreaTop);
        tabBar.getTabs().forEach(tab -> tab.visitChildren(child -> child.setHeight(tabArea.height())));
        tabManager.setTabArea(tabArea);
        layout.setHeaderHeight(tabAreaTop);
        layout.arrangeElements();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return tabBar != null && tabBar.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, height - layout.getFooterHeight(), 0.0f, 0.0f, width, 2, 32, 2);
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0f, 0.0f, width, layout.getHeaderHeight(), 16, 16);
        extractMenuBackground(graphics, 0, layout.getHeaderHeight(), width, height);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(lastScreen);
    }

    private class CategoryTab extends GridLayoutTab {

        private final StatList list;

        private CategoryTab(ServerStats.Category category) {
            super(category.title());
            list = new StatList(minecraft, category);
            this.layout.addChild(list, 1, 1);
        }

        @Override
        public void doLayout(ScreenRectangle screenRectangle) {
            HeaderAndFooterLayout screenLayout = ServerStatsScreen.this.layout;
            list.updateSizeAndPosition(ServerStatsScreen.this.width, screenLayout.getContentHeight(), screenLayout.getHeaderHeight());
            super.doLayout(screenRectangle);
        }
    }

    private class StatList extends ObjectSelectionList<StatRow> {

        private StatList(Minecraft minecraft, ServerStats.Category category) {
            super(minecraft, ServerStatsScreen.this.width, ServerStatsScreen.this.layout.getContentHeight(), LIST_TOP, ROW_HEIGHT);
            ServerStats.statsIn(category).forEach(stat -> addEntry(new StatRow(this, stat)));
        }

        @Override
        public int getRowWidth() {
            return LIST_WIDTH;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {
        }
    }

    private class StatRow extends ObjectSelectionList.Entry<StatRow> {

        private final StatList list;
        private final ServerStats.Stat stat;

        private StatRow(StatList list, ServerStats.Stat stat) {
            this.list = list;
            this.stat = stat;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int textY = getContentYMiddle() - font.lineHeight / 2;
            int color = list.children().indexOf(this) % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR;
            int titleX = getContentX() + TEXT_PADDING;
            if (!stat.icon().isEmpty()) {
                graphics.fakeItem(stat.icon(), titleX, getContentYMiddle() - ICON_SIZE / 2);
                titleX += ICON_SIZE + ICON_GAP;
            }
            graphics.text(font, stat.title(), titleX, textY, color);
            String value = ServerStats.latest(stat).displayValue();
            graphics.text(font, value, getContentRight() - font.width(value) - TEXT_PADDING * 2, textY, color);
        }

        @Override
        public Component getNarration() {
            String value = ServerStats.latest(stat).displayValue();
            return Component.translatable("narrator.select", Component.empty().append(stat.title()).append(CommonComponents.SPACE).append(value));
        }
    }
}
