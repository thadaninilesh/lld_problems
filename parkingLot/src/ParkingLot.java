import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A parking lot or car park is a dedicated cleared area that is intended for parking vehicles.
 * In most countries where cars are a major mode of transportation, parking lots are a feature of every city and suburban area.
 * Shopping malls, sports stadiums, megachurches, and similar venues often feature parking lots over large areas.
 * System Requirements
 *  The parking lot should have multiple floors where customers can park their cars.
 *  Customers can collect a parking ticket from the entry points and can pay the parking fee at the exit points on their way out.
 *  The system should not allow more vehicles than the maximum capacity of the parking lot.
 *  If the parking is full, the system should be able to show a message at the entrance panel and on the parking display board on the ground floor.
 *  Each parking floor will have many parking spots.
 *  The system should support multiple types of parking spots such as Compact, Large, Handicapped, Motorcycle, etc.
 *  The Parking lot should have some parking spots specified for electric cars.
 *  These spots should have an electric panel through which customers can pay and charge their vehicles.
 *  Each parking floor should have a display board showing any free parking spot for each spot type.
 *  EXTRA:
 *  The system should support a per-hour parking fee model. For example, customers have to pay $4 for the first hour, $3.5 for the second and third hours, and $2.5 for all the remaining hours.
 */
class ParkingSpace {
    enum Type {
        COMPACT, LARGE, HANDICAPPED, MOTORCYCLE, ELECTRIC
    }

    private final String id;
    private final Type type;
    private boolean isOccupied;
    public ParkingSpace(String id, Type type) {
        this.id = id;
        this.type = type;
        this.isOccupied = false;
    }
    public boolean isOccupied() {
        return isOccupied;
    }
    public String getId() {
        return id;
    }
    public Type getType() {
        return type;
    }
    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }
}

record ParkingFloor(String floorId, List<ParkingSpace> parkingSpaces) { }

record Tickets(String ticketId, String vehicleId, long entryTime) { }
public class ParkingLot {
    private final String lotId;
    private final List<ParkingFloor> floors;
    private final int capacity;
    private final static BigDecimal parkingFeePerHour = BigDecimal.valueOf(5); // example fee per hour

    private final int currentOccupancy = 0;
    private final Map<String, ParkingSpace> occupiedSpots = new ConcurrentHashMap<>(); // vehicle ID to ParkingSpace mapping

    // store all the available parking slots so that we can quickly find a spot
    private final Map<ParkingSpace.Type, LinkedBlockingQueue<ParkingSpace>> availableSpots = new ConcurrentHashMap<>();

    // map ParkingSpace with Floor for easy lookup
    private final Map<ParkingSpace, ParkingFloor> parkingSpaceToFloorMap = new ConcurrentHashMap<>();

    // issued tickets
    private final Map<String, Tickets> vehicleTickets = new ConcurrentHashMap<>();

    public ParkingLot(String lotId, List<ParkingFloor> floors, int capacity) {
        this.lotId = lotId;
        this.floors = floors;
        this.capacity = capacity;
        for (ParkingFloor parkingFloor: floors) {
            for (ParkingSpace parkingSpace: parkingFloor.parkingSpaces()) {
                availableSpots.putIfAbsent(parkingSpace.getType(), new LinkedBlockingQueue<>());
                availableSpots.get(parkingSpace.getType()).add(parkingSpace);
                parkingSpaceToFloorMap.put(parkingSpace, parkingFloor);
            }
        }
    }

    public void displayAvailableSpots() {
        for (ParkingFloor floor : floors) {
            System.out.println("Floor: " + floor.floorId());
            for (ParkingSpace space : floor.parkingSpaces()) {
                if (!space.isOccupied()) {
                    System.out.println("Available Spot ID: " + space.getId() + ", Type: " + space.getType());
                }
            }
        }
    }

    public void registerEntry(String vehicleId, ParkingSpace.Type requiredType) {
        // Logic to register vehicle entry, assign parking spot, issue ticket, etc.
        if (currentOccupancy >= capacity) {
            System.out.println("Parking Lot Full. Cannot accommodate more vehicles.");
            return;
        }
        LinkedBlockingQueue<ParkingSpace> spots = availableSpots.get(requiredType);
        if (spots == null || spots.isEmpty()) {
            System.out.println("No available spots of type: " + requiredType);
            return;
        }
        ParkingSpace assignedSpot = spots.poll();
        assignedSpot.setOccupied(true);
        occupiedSpots.put(vehicleId, assignedSpot);
        Tickets ticket = new Tickets(UUID.randomUUID().toString(), vehicleId, System.currentTimeMillis());
        vehicleTickets.put(vehicleId, ticket);

        System.out.println("Vehicle " + vehicleId + " assigned to spot " + assignedSpot.getId());

    }

    public void registerExit(String vehicleId) {
        // Logic to register vehicle exit, free parking spot, calculate fee, etc.
        ParkingSpace parkingSpace = occupiedSpots.get(vehicleId);
        if (parkingSpace == null) {
            System.out.println("No vehicle found with ID: " + vehicleId);
            return;
        }
        parkingSpace.setOccupied(false);
        occupiedSpots.remove(vehicleId);

        LinkedBlockingQueue<ParkingSpace> availableSpotsByType = availableSpots.getOrDefault(parkingSpace.getType(), new LinkedBlockingQueue<>());
        availableSpotsByType.add(parkingSpace);
        availableSpots.put(parkingSpace.getType(), availableSpotsByType);

        Tickets parkingTicket = vehicleTickets.get(vehicleId);
        long entryTime = parkingTicket.entryTime();
        long exitTime = System.currentTimeMillis();
        long durationMillis = exitTime - entryTime;
        long durationHours = (durationMillis / (1000 * 60 * 60)) + 1; // rounding up to next hour

        BigDecimal totalFee = parkingFeePerHour.multiply(BigDecimal.valueOf(durationHours));
        System.out.println("Total parking fee for vehicle " + vehicleId + " is: $" + totalFee);

        System.out.println("Vehicle " + vehicleId + " has exited from spot " + parkingSpace.getId());
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- 1. Setup Parking Lot ---");
        // Create 2 spots: 1 Compact, 1 Large
        ParkingSpace s1 = new ParkingSpace("C1", ParkingSpace.Type.COMPACT);
        ParkingSpace s2 = new ParkingSpace("L1", ParkingSpace.Type.LARGE);

        List<ParkingSpace> spaces = new ArrayList<>();
        spaces.add(s1);
        spaces.add(s2);

        ParkingFloor f1 = new ParkingFloor("Floor1", spaces);
        List<ParkingFloor> floors = new ArrayList<>();
        floors.add(f1);

        // Capacity is 2
        ParkingLot lot = new ParkingLot("Downtown_Lot", floors, 2);

        System.out.println("\n--- 2. Test Happy Path (Park & Exit) ---");
        lot.registerEntry("Car_A", ParkingSpace.Type.COMPACT); // Should Succeed
        Thread.sleep(10); // Sleep briefly so exit time > entry time
        lot.registerExit("Car_A"); // Should charge $5 (1 hour min)

        System.out.println("\n--- 3. Test Unavailable Type ---");
        // We only have COMPACT and LARGE. Asking for ELECTRIC should fail.
        lot.registerEntry("Tesla_X", ParkingSpace.Type.ELECTRIC);

        System.out.println("\n--- 4. Test Capacity / Full Lot ---");
        lot.registerEntry("Car_B", ParkingSpace.Type.COMPACT); // Takes C1
        lot.registerEntry("Truck_A", ParkingSpace.Type.LARGE);   // Takes L1

        // Lot is now Full (2/2). Next car should fail even if spot type matches?
        // Actually, capacity check runs first.
        lot.registerEntry("Car_C", ParkingSpace.Type.COMPACT); // Should fail (Full)

        System.out.println("\n--- 5. Test Exit Non-Existent Vehicle ---");
        lot.registerExit("Ghost_Car"); // Should fail

        System.out.println("\n--- 6. Test Spot Reuse ---");
        // Truck_A leaves L1. L1 becomes free.
        lot.registerExit("Truck_A");
        // New Truck should be able to park now
        lot.registerEntry("Truck_B", ParkingSpace.Type.LARGE); // Should Succeed
    }
}