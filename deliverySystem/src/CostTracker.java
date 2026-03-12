import model.Delivery;
import model.Driver;

import java.util.*;

public class CostTracker {

    private Map<String, Driver> driversInSystem = new HashMap<>();

    private Double totalCost = 0d;

    private Double totalPaidCost = 0d;

    private TreeMap<Long, Integer> timeline = new TreeMap<>();

    public void addDriver(String driverId, double rate) {
        driversInSystem.put(driverId, new Driver(driverId, rate));
    }

    public void addDelivery(String driverId, long startTime, long endTime) {
        Driver driver = driversInSystem.get(driverId);
        Delivery delivery = new Delivery(startTime, endTime);
        driver.addDeliveries(delivery);
        totalCost += delivery.getCost(startTime, endTime, driver.getRate());

        timeline.put(startTime, timeline.getOrDefault(startTime, 0) + 1);
        timeline.put(endTime, timeline.getOrDefault(endTime, 0) - 1);
    }

    public Double getTotalCost() {
        return totalCost;
    }

    public void getPayUptoTime(long uptoTime) {
        for (Driver d: driversInSystem.values()) {
            for (Delivery delivery: d.getDeliveries()) {
                if (delivery.getEndTime() <= uptoTime && delivery.getStartTime() >= d.getLastPayTime()) {
                    totalPaidCost += delivery.getCost(delivery.getStartTime(), delivery.getEndTime(), d.getRate());
                }
                d.setLastPayTime(Math.max(d.getLastPayTime(), uptoTime));
            }
        }

    }

    public Double getCostToBePaid() {
        return totalCost - totalPaidCost;
    }

    public int getMaxActiveDriversInLast24Hours(long currentTime) {
        long startTimeLimit = currentTime - (24 * 3600);
        int maxActive = 0;
        int currentActive = 0;

        NavigableMap<Long, Integer> last24Hours = timeline.subMap(startTimeLimit, true, currentTime, true);

        for (int change: last24Hours.values()) {
            currentActive += change;
            maxActive = Math.max(maxActive, currentActive);
        }

        return maxActive;
    }

    public static void main(String[] args) {
        CostTracker tracker = new CostTracker();

        System.out.println("--- Scenario 1: Basic Addition ---");
        tracker.addDriver("driver_1", 10);
        // 1 hour delivery (3600 seconds)
        tracker.addDelivery("driver_1", 1000, 4600);

        // At $20/hr, cost should be 20.0
        System.out.println("Total Incurred Cost: " + tracker.getTotalCost());
        System.out.println("Cost to be Paid: " + tracker.getCostToBePaid());

        System.out.println("\n--- Scenario 2: Payment Tracking ---");
        tracker.addDriver("driver_2", 20);
        // 2 hour delivery (7200 seconds)
        tracker.addDelivery("driver_2", 2000, 9200);

        System.out.println("Total Incurred (20 + 40): " + tracker.getTotalCost());

        // Pay up to time 5000.
        // driver_1 finished at 4600 (Paid)
        // driver_2 finished at 9200 (Not Paid yet)
        tracker.getPayUptoTime(5000);
        System.out.println("Cost to be Paid (Should be 40.0): " + tracker.getCostToBePaid());

        // Pay up to time 10000 (Covers driver_2)
        tracker.getPayUptoTime(10000);
        System.out.println("Cost to be Paid (Should be 0.0): " + tracker.getCostToBePaid());

        System.out.println("\n--- Scenario 3: 24-Hour Analytics ---");
        // We need to test overlapping drivers
        long now = 100000; // Let's pretend "now" is 100k seconds

        // Driver A: active 80k to 90k
        tracker.addDelivery("driver_1", 80000, 90000);
        // Driver B: active 85k to 95k (Overlaps A)
        tracker.addDelivery("driver_2", 85000, 95000);
        // Driver C: active 10k to 20k (Outside 24hr window: 100k - 86.4k = 13.6k)
        tracker.addDelivery("driver_1", 10000, 20000);

        // In the last 24 hours (since 13,600), only Driver A and B were active together.
        // Max concurrent should be 2.
        int maxActive = tracker.getMaxActiveDriversInLast24Hours(now);
        System.out.println("Max active drivers in last 24h: " + maxActive);
    }

}
