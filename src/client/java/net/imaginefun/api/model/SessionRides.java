package net.imaginefun.api.model;

import java.util.Map;

public record SessionRides(Map<String, RideStats> rides, RideTotals overall, RideTotals weekly, RideTotals yearly) {
}
