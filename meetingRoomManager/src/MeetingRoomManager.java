import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

enum Equipment {
    PROJECTOR, VIDEO_CONF, WHITEBOARD, NONE
}

class Room {
    String id;
    int capacity;
    Set<Equipment> equipments;

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Room(String id, int capacity, Set<Equipment> equipments) {
        this.id = id;
        this.capacity = capacity;
        this.equipments = equipments;
    }

    public boolean canAccomodate(int requiredCapacity, Equipment equipment) {

        if (this.capacity < requiredCapacity) return false;
        if (equipment != null && !equipments.contains(equipment)) return false;
        return true;

    }
}

record Booking(String bookingId,
               String employeeId,
               long startTime,
               long endTime,
               String roomId) { }

public class MeetingRoomManager {
    private final List<Room> rooms;
    private final Map<String, List<Booking>> roomBookings;

    public MeetingRoomManager() {
        this.rooms = new ArrayList<>();
        this.roomBookings = new ConcurrentHashMap<>();
    }

    public void addRoom(String id, int capacity, Set<Equipment> equipmentSet) {
        rooms.add(new Room(id, capacity, equipmentSet));
        roomBookings.put(id, new ArrayList<>());
    }

    public String processBooking(
            String bookingId, String empId, long startTime, long endTime, int capacity, Equipment equipment) {

        // filter rooms based on physical reqt
        for (Room room: rooms) {
            if (room.canAccomodate(capacity, equipment)) {
                room.lock.writeLock().lock();
                try {
                    if (isRoomAvailable(room.id, startTime, endTime)) {
                        Booking newBooking = new Booking(bookingId, empId, startTime, endTime, room.id);
                        List<Booking> bookings = roomBookings.get(room.id);
                        bookings.add(newBooking);

                        // Keep list sorted for optimization
                        bookings.sort(Comparator.comparing(b -> b.startTime()));
                        System.out.println("Booking confirmed: " + bookingId + " in room " + room.id);
                        return room.id;
                    }
                } finally {
                    room.lock.writeLock().unlock();;
                }
            }
        }

        System.out.println("Booking Rejected: " + bookingId + " (No Room Available)");
        return null;

    }

    private boolean isRoomAvailable(String roomId, long start, long end) {
        List<Booking> bookings = roomBookings.get(roomId);
        for (Booking b: bookings) {
            if(start < b.startTime() && end > b.endTime()) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        MeetingRoomManager system = new MeetingRoomManager();

        // 1. Setup Inventory
        system.addRoom("Room-A", 10, Set.of(Equipment.PROJECTOR, Equipment.WHITEBOARD));
        system.addRoom("Room-B", 4, Set.of(Equipment.NONE));

        System.out.println("--- Processing Requests ---");

        // Scenario 1: Alice requests Room A (09:00 - 10:00)
        // Needs Projector, Cap 8
        system.processBooking("booking-101", "emp-alice",
                toMillis("2024-01-15T09:00:00"),
                toMillis("2024-01-15T10:00:00"),
                8, Equipment.PROJECTOR);

        // Scenario 2: Bob requests (09:30 - 10:30)
        // Needs Capacity 4, No Equipment.
        // Room A is occupied (Overlap). Room B is free and fits criteria.
        system.processBooking("booking-102", "emp-bob",
                toMillis("2024-01-15T09:30:00"),
                toMillis("2024-01-15T10:30:00"),
                4, Equipment.NONE);

        // Scenario 3: Charlie requests (10:00 - 11:00)
        // Needs Projector.
        // Note: Alice ends at 10:00. Charlie starts at 10:00.
        // Logic check: (10:00 < 10:00) is FALSE. No Overlap. Should Succeed.
        system.processBooking("booking-103", "emp-charlie",
                toMillis("2024-01-15T10:00:00"),
                toMillis("2024-01-15T11:00:00"),
                8, Equipment.PROJECTOR);

        // Scenario 4: Alice requests (09:15 - 10:15)
        // Needs Video Conf (Only Room A fits capacity, but A doesn't have Video Conf?)
        // Wait, Room A has {Projector, Whiteboard}. Request needs VIDEO_CONF.
        // Should Fail on Capability Check.
        system.processBooking("booking-104", "emp-alice",
                toMillis("2024-01-15T09:15:00"),
                toMillis("2024-01-15T10:15:00"),
                8, Equipment.VIDEO_CONF);
    }

    private static long toMillis(String isoDate) {
        return LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }
}