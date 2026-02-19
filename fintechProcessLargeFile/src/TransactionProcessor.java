import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TransactionProcessor {

    static class Account {
        final String id;
        BigDecimal balance;

        public Account(String id) {
            this.id = id;
            this.balance = BigDecimal.ZERO;
        }

        public synchronized void processTransaction(BigDecimal amount) {
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                // Deposit
                balance = balance.add(amount);
                System.out.println("✅ Deposited " + amount + " to " + id + ". New Balance: " + balance);
            } else {
                BigDecimal absAmt = amount.abs();
                if (balance.compareTo(absAmt) >= 0) {
                    balance = balance.subtract(absAmt);
                    System.out.println("✅ Withdrew " + absAmt + " from " + id + ". New Balance: " + balance);
                } else {
                    System.out.println("❌ Skipped: " + id + " (Insufficient Funds for " + amount + ". Balance: " + balance + ")");
                }
            }
        }
    }

    private final Map<String, Account> accountDatabase = new ConcurrentHashMap<>();

    // Array of Single Thread Executors for Sticky Routing
    private final ExecutorService[] workers;
    private final int numPartitions;

    public TransactionProcessor(int parallelism) {
        this.numPartitions = parallelism;
        this.workers = new ExecutorService[parallelism];
        for (int i = 0; i < parallelism; i++) {
            // Each partition gets exactly ONE thread, ensuring strict chronological ordering
            workers[i] = Executors.newSingleThreadExecutor();
        }
    }

    public void processFile(String filePath) {
        // 1. Streaming Read: Only keeps one line in memory at a time
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length != 2) continue;

                String accountId = parts[0].trim();
                BigDecimal amt = new BigDecimal(parts[1].trim());

                // 2. Sticky Routing Logic: Route by Account ID
                // Math.abs handles negative hash codes.
                int partitionId = Math.abs(accountId.hashCode()) % numPartitions;

                // 3. Dispatch to the dedicated worker for this account
                workers[partitionId].submit(() -> {
                    Account acc = accountDatabase.computeIfAbsent(accountId, Account::new);
                    acc.processTransaction(amt);
                });
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // to be called from main method during test
    public void shutdownAndWait() {
        for (ExecutorService worker: workers) {
            worker.shutdown();
        }
        try {
            for (ExecutorService worker: workers) {
                worker.awaitTermination(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        // Assuming 4 cores/partitions
        TransactionProcessor processor = new TransactionProcessor(4);

        // In an interview, you can write a dummy file or simulate the lines
        // For this example, let's assume "transactions.csv" exists.
        System.out.println("Starting processing...");
        processor.processFile("transactions.csv");

        // Wait for all async tasks to finish
        processor.shutdownAndWait();
        System.out.println("Processing complete.");
    }

}