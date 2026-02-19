import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

record Reservation (String orderId,
                    String productId,
                    String warehouseId,
                    int quantity,
                    long timestamp){ }

class ProductInventory {
    int availableQuantity;
    int totalCapacity;

    final LinkedHashMap<String, Reservation> activeReservation = new LinkedHashMap<>();
    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ProductInventory(int initialQuantity) {
        this.availableQuantity = initialQuantity;
        this.totalCapacity = initialQuantity;
    }
}

class InventorySystem {

//    warehouse vs product vs inventory
    private final Map<String, Map<String, ProductInventory>> warehouses = new ConcurrentHashMap<>();

    private final long RESERVATION_TTL_MS = 15*60*1000;

    public void initializeInventory(String warehouseId, String productId, int quantity) {
        warehouses.putIfAbsent(warehouseId, new ConcurrentHashMap<>());
        warehouses.get(warehouseId).put(productId, new ProductInventory(quantity));
    }

    public boolean reserve(String orderId, String productId, String warehouseId, int quantity, long requestTime) {
        Map<String, ProductInventory> warehouse = warehouses.get(warehouseId);
        if (warehouse == null || !warehouse.containsKey(productId)) {
            System.out.println("❌ Rejected: " + orderId + " (Unknown Product/Warehouse)");
            return false;
        }

        ProductInventory inventory = warehouse.get(productId);
        inventory.lock.writeLock().lock();

        try {
            // 1. LAzy Expiration: Clean up expired reservation before checking availability
            expireOldReservation(inventory, requestTime);

            // 2. Check capacity
            if (inventory.availableQuantity >= quantity) {
                // 3. Execute Reservation
                inventory.availableQuantity -= quantity;
                Reservation res = new Reservation(orderId, productId, warehouseId, quantity, requestTime);
                inventory.activeReservation.put(orderId, res);
                System.out.println("✅ Reserved: " + orderId + " (" + quantity + "x " + productId + ")");
                return true;
            } else {
                System.out.println("❌ Rejected: " + orderId + " (Insufficient Inventory. Available: " + inventory.availableQuantity + ")");
                return false;
            }
        } finally {
            inventory.lock.writeLock().unlock();
        }
    }

    public void cancelReservation(String orderId, String productId, String warehouseId) {
        ProductInventory inventory = warehouses.get(warehouseId).get(productId);
        if (inventory == null) return;
        inventory.lock.writeLock().lock();
        try {
            Reservation res = inventory.activeReservation.remove(orderId);
            if (res != null) {
                inventory.availableQuantity += res.quantity();
                System.out.println("🚫 Cancelled: " + orderId + " (Restored " + res.quantity() + ")");
            }
        } finally {
            inventory.lock.writeLock().unlock();
        }
    }

    // Helper: Removes reservations older than the TTL
    private void expireOldReservation(ProductInventory productInventory, long currentTime) {
        Iterator<Map.Entry<String, Reservation>> iterator = productInventory.activeReservation.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Reservation> entry = iterator.next();
            Reservation res = entry.getValue();

            // Because it's a LinkedHashMap, the oldest are first.
            // If we hit one that isn't expired, we can stop checking the rest.
            if (currentTime - res.timestamp() > RESERVATION_TTL_MS) {
                System.out.println("⏳ Expiring: " + res.orderId() + " (Restoring " + res.quantity() + " items)");
                productInventory.availableQuantity += res.quantity();
                iterator.remove();
            } else {
                break;
            }
        }
    }

    public static void main(String[] args) {
        InventorySystem system = new InventorySystem();

        // 1. Initialize Configuration
        system.initializeInventory("warehouse-A", "product-456", 10);
        system.initializeInventory("warehouse-B", "product-789", 5);

        System.out.println("--- Processing Stream ---");

        // Request 1: Valid
        system.reserve("order-123", "product-456", "warehouse-A", 5,
                Instant.parse("2024-01-15T10:00:00Z").toEpochMilli());

        // Request 2: Valid (5 left)
        system.reserve("order-124", "product-456", "warehouse-A", 3,
                Instant.parse("2024-01-15T10:01:00Z").toEpochMilli());

        // Request 3: Different Warehouse
        system.reserve("order-123", "product-789", "warehouse-B", 2,
                Instant.parse("2024-01-15T10:02:00Z").toEpochMilli());

        // Request 4: Fails due to limits (Only 2 left in A, needs 10)
        system.reserve("order-125", "product-456", "warehouse-A", 10,
                Instant.parse("2024-01-15T10:03:00Z").toEpochMilli());

        // Request 5: The "Time Jump" (Triggering Expiration)
        // 20 minutes later. order-123 and order-124 should expire, freeing 8 items.
        System.out.println("\n--- 20 Minutes Later ---");
        system.reserve("order-126", "product-456", "warehouse-A", 10,
                Instant.parse("2024-01-15T10:25:00Z").toEpochMilli());
    }
}