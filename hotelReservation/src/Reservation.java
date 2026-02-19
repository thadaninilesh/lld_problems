import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class Reservation {
    String reservationId;
    int partySize;
    long startTime;
    long endTime;
    String tableId;
    boolean isCancelled;

    public Reservation(String reservationId, int partySize, long startTime, long endTime, String tableId) {
        this.reservationId = reservationId;
        this.partySize = partySize;
        this.startTime = startTime;
        this.endTime = endTime;
        this.tableId = tableId;
        this.isCancelled = false;
    }
}

class Table {
    String id;
    int capacity;
    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Table(String id, int capacity) {
        this.id = id;
        this.capacity = capacity;
    }
}

class RestaurantSystem {
    private final Map<String, Table> tables;
    private final Map<String, List<Reservation>> tableReservation;

    private static final long CLEANUP_BUFFER_MS = 15 * 60 * 1000L;

    public RestaurantSystem() {
        this.tables = new ConcurrentHashMap<>();
        this.tableReservation = new ConcurrentHashMap<>();
    }

    public void addTable(String id, int capacity) {
        tables.put(id, new Table(id, capacity));
        tableReservation.put(id, new ArrayList<>());
    }

    public String processBooking(String bookingId, int partySize, long startTime, int durationInMins) {
        long durationInMs = durationInMins * 60 * 1000L;
        long endTime = startTime + durationInMs;

        // 1. OPTIMIZATION: Get capable tables and SORT by capacity (Best Fit)
        List<Table> possibleTables = tables.values().stream()
                .filter(t -> t.capacity >= partySize)
                .sorted(Comparator.comparingInt(t -> t.capacity))
                .collect(Collectors.toList());

        for (Table table: possibleTables) {
            table.lock.writeLock().lock();

            try {
                if (isTableAvailable(table.id, startTime, endTime)) {
                    Reservation reservation = new Reservation(bookingId, partySize, startTime, endTime, table.id);
                    List<Reservation> reservations = tableReservation.get(table.id);
                    reservations.add(reservation);

                    reservations.sort(Comparator.comparingLong(r -> r.startTime));
                    System.out.println("✅ Booking " + bookingId + " -> Table " + table.id);
                    return table.id;
                }
            } finally {
                table.lock.writeLock().unlock();;
            }
        }
        System.out.println("❌ Booking Rejected: " + bookingId + " (No tables available)");
        return null;
    }

    public boolean cancelBooking(String tableId, String bookingId) {
        Table table = tables.get(tableId);
        if (table == null) return false;

        table.lock.writeLock().lock();

        try {
            List<Reservation> reservations = tableReservation.get(tableId);
            for (Reservation r: reservations) {
                if (r.reservationId.equals(bookingId) && !r.isCancelled) {
                    r.isCancelled = true;
                    System.out.println("🗑️ Cancelled: " + bookingId);
                    return true;
                }
            }
            return false;
        } finally {
            table.lock.writeLock().unlock();
        }
    }

    private boolean isTableAvailable(String id, long start, long end) {
        List<Reservation> reservations = tableReservation.get(id);
        for (Reservation r: reservations) {
            if (r.isCancelled) continue;

            // The existing reservation actually occupies time from its start to its (end + cleanup)
            long existingBlockedUntil = r.endTime + CLEANUP_BUFFER_MS;

            if (start < existingBlockedUntil && end > r.startTime) {
                return false;
            }
        }
        return true;
    }

    private static long toMillis(String isoDate) {
        return LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    public static void main(String[] args) {
        RestaurantSystem system = new RestaurantSystem();

        // Inventory: 2-seaters, 4-seaters, and a large 6-seater
        system.addTable("T1-2P", 2);
        system.addTable("T2-4P", 4);
        system.addTable("T3-4P", 4);
        system.addTable("T4-6P", 6);

        // Scenario 1: Party of 4 at 19:00 for 90 mins
        // Should pick T2-4P (exact match) over T4-6P.
        system.processBooking("booking-001", 4,
                toMillis("2024-01-15T19:00:00"), 90);

        // Scenario 2: Party of 2 at 19:30 for 60 mins
        // Should pick T1-2P.
        system.processBooking("booking-002", 2,
                toMillis("2024-01-15T19:30:00"), 60);

        // Scenario 3: Party of 6 at 19:00 for 120 mins
        // Should pick T4-6P.
        system.processBooking("booking-003", 6,
                toMillis("2024-01-15T19:00:00"), 120);

        // Scenario 4: Party of 4 at 20:00 for 90 mins
        // T2-4P is booked until 20:30 (19:00 + 90m).
        // T3-4P is free, so it should take T3-4P.
        system.processBooking("booking-004", 4,
                toMillis("2024-01-15T20:00:00"), 90);

        // Scenario 5: Party of 2 at 20:35.
        // booking-002 ended at 20:30.
        // With 15 min cleanup, table is blocked until 20:45. Should fail or take larger table.
        // It will take a 4-seater (T2-4P, which was freed at 20:30, plus 15 min cleanup = 20:45. Wait, T2-4P is also blocked).
        system.processBooking("booking-005", 2,
                toMillis("2024-01-15T20:35:00"), 60);

        // Extension Test: Cancellation
        system.cancelBooking("T4-6P", "booking-003");
    }
}