package net.imaginefun.stats;

import net.imaginefun.networking.ServerStatsPayload;
import net.imaginefun.networking.ServerStatsUpdatePayload;
import net.imaginefun.text.ComponentJson;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ServerStats {

    public record Category(String id, Component title) {
    }

    public record Stat(String id, String categoryId, Component title, ItemStack icon, long value, String displayValue) {

        private Stat withValue(long newValue, String newDisplayValue) {
            return new Stat(id, categoryId, title, icon, newValue, newDisplayValue);
        }
    }

    private static List<Category> categories = List.of();
    private static final Map<String, Stat> STATS_BY_ID = new LinkedHashMap<>();

    private ServerStats() {
    }

    public static boolean hasSnapshot() {
        return !categories.isEmpty();
    }

    public static List<Category> categories() {
        return categories;
    }

    public static List<Stat> statsIn(Category category) {
        return STATS_BY_ID.values().stream()
            .filter(stat -> stat.categoryId().equals(category.id()))
            .toList();
    }

    public static Stat latest(Stat stat) {
        return STATS_BY_ID.getOrDefault(stat.id(), stat);
    }

    public static void replace(ServerStatsPayload snapshot) {
        categories = snapshot.categories().stream()
            .map(category -> new Category(category.id(), ComponentJson.parse(category.titleJson())))
            .toList();
        STATS_BY_ID.clear();
        for (ServerStatsPayload.Stat stat : snapshot.stats()) {
            STATS_BY_ID.put(stat.id(), new Stat(
                stat.id(),
                stat.categoryId(),
                ComponentJson.parse(stat.titleJson()),
                stat.icon(),
                stat.value(),
                stat.displayValue()
            ));
        }
    }

    public static void apply(ServerStatsUpdatePayload payload) {
        for (ServerStatsUpdatePayload.Update update : payload.updates()) {
            STATS_BY_ID.computeIfPresent(update.id(), (id, stat) -> stat.withValue(update.value(), update.displayValue()));
        }
    }

    public static void clear() {
        categories = List.of();
        STATS_BY_ID.clear();
    }
}
