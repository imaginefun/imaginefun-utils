package net.imaginefun.stats;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.util.Mth;

import java.util.List;

public class ServerStatsTabBar extends MenuTabBar {

    private static final int HEIGHT = 24;
    private static final int MARGIN = 14;
    private static final int MIN_TABS_WIDTH = 372;
    private static final int PREFERRED_TAB_WIDTH = 90;

    private ServerStatsTabBar(int width, TabManager tabManager, ImmutableList<TabButton> tabButtons, ImmutableList<Tab> tabs) {
        super(0, 0, width, HEIGHT, tabManager, tabButtons, tabs);
    }

    public static ServerStatsTabBar create(TabManager tabManager, int width, List<Tab> tabs) {
        ImmutableList<TabButton> tabButtons = tabs.stream()
            .<TabButton>map(tab -> new MenuTabButton(tabManager, tab, 0, HEIGHT))
            .collect(ImmutableList.toImmutableList());
        return new ServerStatsTabBar(width, tabManager, tabButtons, ImmutableList.copyOf(tabs));
    }

    @Override
    public void arrangeElements(int width) {
        this.width = width;
        int availableWidth = width - MARGIN * 2;
        int tabsWidth = Math.min(availableWidth, Math.max(MIN_TABS_WIDTH, tabs.size() * PREFERRED_TAB_WIDTH));
        int tabWidth = Mth.roundToward(tabsWidth / tabs.size(), 2);
        tabButtons.forEach(button -> button.setWidth(tabWidth));
        layout.arrangeElements();
        layout.setX(Mth.roundToward((width - tabsWidth) / 2, 2));
        layout.setY(0);
    }
}
