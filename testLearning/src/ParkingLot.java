import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

enum Type {
    COMPACT, EV, LARGE, HANDICAPPED, MOTORCYCLE
}

class ParkingSpace {
    private final String id; //spaceId

    private final Type type;

    private boolean isOccupied = false;

    public ParkingSpace(String id, Type type) {
        this.id = id;
        this.type = type;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }
}

record ParkingFloor(List<ParkingSpace> parkingSpaces, String floorId) {}

record Ticket(String ticketId, String vehicle, long entryTime) {}


public class ParkingLot {

    private final String lotId;

    private final List<ParkingFloor> floors;

    private final int capacity;

    private final static BigDecimal parkingFeePerHour = BigDecimal.valueOf(5);

    private BigDecimal firstTwoHoursFlatRate = new BigDecimal("10.00"); // $x flat fee for up to 2 hrs
    private BigDecimal subsequentHourlyRate = new BigDecimal("5.00");

    private final int currentOccupancy = 0;

    // vehicleId vs parkingSpace
    private final Map<String, ParkingSpace> occupiedSpots = new ConcurrentHashMap<>();

    // parking type vs space
    private final Map<Type, LinkedBlockingQueue<ParkingSpace>> availableSpace = new ConcurrentHashMap<>();

    // space with floor
    private final Map<ParkingSpace, ParkingFloor> parkingSpaceParkingFloorMap = new ConcurrentHashMap<>();

    // vehicleId with tickets
    private final Map<String, Ticket> vehicleTickets = new ConcurrentHashMap<>();

    public ParkingLot(String lotId, List<ParkingFloor> floors, int capacity) {
        this.lotId = lotId;
        this.floors = floors;
        this.capacity = capacity;

        for (ParkingFloor floor: floors) {
            for (ParkingSpace space: floor.parkingSpaces()) {
                availableSpace.putIfAbsent(space.getType(), new LinkedBlockingQueue<>());
                availableSpace.get(space.getType()).add(space);
                parkingSpaceParkingFloorMap.putIfAbsent(space, floor);
            }
        }
    }

    public void displayAvailableSpots() {
        for (ParkingFloor floor: floors) {
            System.out.println("Floor: " + floor.floorId());
            for (ParkingSpace space: floor.parkingSpaces()) {
                if (!space.isOccupied()) {
                    System.out.println("Available spot id: " + space.getId() + ", Type: " + space.getType());
                }
            }
        }
    }

    public void registerEntry(String vehicleId, Type requiredType) {
        if (currentOccupancy >= capacity) {
            System.out.println("Parking Lot is full");
            return;
        }

        LinkedBlockingQueue<ParkingSpace> availableSpacesByType = availableSpace.get(requiredType);
        if (availableSpacesByType == null || availableSpacesByType.isEmpty()) {
            System.out.println("No parking available for this type");
            return;
        }
        ParkingSpace space = availableSpacesByType.poll(); // poll removes it from available space
        space.setOccupied(true);
        occupiedSpots.put(vehicleId, space);
        Ticket ticket = new Ticket(UUID.randomUUID().toString(), vehicleId, System.currentTimeMillis());
        vehicleTickets.put(vehicleId, ticket);

        System.out.println("Ticket issued to vehicle: " + vehicleId);
    }

    public void registerExit(String vehicleId) {
        ParkingSpace space = occupiedSpots.get(vehicleId);
        if (space == null) {
            System.out.println("No entry found for this vehicle");
            return;
        }

        Ticket ticket = vehicleTickets.get(vehicleId);
        long entryTime = ticket.entryTime();
        long exitTime = System.currentTimeMillis();

        space.setOccupied(false);
        occupiedSpots.remove(vehicleId);
        LinkedBlockingQueue<ParkingSpace> availableSpaces = availableSpace.getOrDefault(space.getType(), new LinkedBlockingQueue<>());
        availableSpaces.add(space);
        availableSpace.put(space.getType(), availableSpaces);
//        long hoursParked = (exitTime - entryTime) / (1000 * 60 * 60);
//        BigDecimal totalFee;
//
//        if (hoursParked < 2) {
//            totalFee = firstTwoHoursFlatRate;
//        } else {
//            BigDecimal extraHours = BigDecimal.valueOf(hoursParked - 2);
//            BigDecimal extraFee = subsequentHourlyRate.multiply(extraHours);
//            totalFee = firstTwoHoursFlatRate.add(extraFee);
//        }
        BigDecimal totalFee = parkingFeePerHour.multiply(BigDecimal.valueOf((exitTime - entryTime) / (1000 * 60 * 60) + 1));


        System.out.println("Total fee to be paid: " + totalFee);

    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- 1. Setup Parking Lot ---");
        // Create 2 spots: 1 Compact, 1 Large
        ParkingSpace s1 = new ParkingSpace("C1", Type.COMPACT);
        ParkingSpace s2 = new ParkingSpace("L1", Type.LARGE);

        List<ParkingSpace> spaces = new ArrayList<>();
        spaces.add(s1);
        spaces.add(s2);

        ParkingFloor f1 = new ParkingFloor(spaces, "Floor1");
        List<ParkingFloor> floors = new ArrayList<>();
        floors.add(f1);

        // Capacity is 2
        ParkingLot lot = new ParkingLot("Downtown_Lot", floors, 2);

        System.out.println("\n--- 2. Test Happy Path (Park & Exit) ---");
        lot.registerEntry("Car_A", Type.COMPACT); // Should Succeed
        Thread.sleep(10); // Sleep briefly so exit time > entry time
        lot.registerExit("Car_A"); // Should charge $5 (1 hour min)

        System.out.println("\n--- 3. Test Unavailable Type ---");
        // We only have COMPACT and LARGE. Asking for ELECTRIC should fail.
        lot.registerEntry("Tesla_X", Type.EV);

        System.out.println("\n--- 4. Test Capacity / Full Lot ---");
        lot.registerEntry("Car_B", Type.COMPACT); // Takes C1
        lot.registerEntry("Truck_A", Type.LARGE);   // Takes L1

        // Lot is now Full (2/2). Next car should fail even if spot type matches?
        // Actually, capacity check runs first.
        lot.registerEntry("Car_C", Type.COMPACT); // Should fail (Full)

        System.out.println("\n--- 5. Test Exit Non-Existent Vehicle ---");
        lot.registerExit("Ghost_Car"); // Should fail

        System.out.println("\n--- 6. Test Spot Reuse ---");
        // Truck_A leaves L1. L1 becomes free.
        lot.registerExit("Truck_A");
        // New Truck should be able to park now
        lot.registerEntry("Truck_B", Type.LARGE); // Should Succeed
    }
}
