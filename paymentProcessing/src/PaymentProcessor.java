import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentProcessor {
    private final Map<String, Account> accountStore = new ConcurrentHashMap<>();

    private static final int PARTITION_COUNT =  4;
    private final List<ExecutorService> partitions;

    public PaymentProcessor() {
        this.partitions = new ArrayList<>();

        for (int i = 0; i < PARTITION_COUNT; i++) {
            partitions.add(Executors.newSingleThreadExecutor());
        }
    }

    public void processFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line);
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } finally {
            shutdownAndAwait();
        }
    }

    public void processLine(String line) {
        String[] parts = line.split(",");
        if (parts.length != 2) {
            System.err.println("Invalid line format: " + line);
        } else {
            String accountId = parts[0].trim();
            String amountStr = parts[1].trim();

            try {
                BigDecimal amount = new BigDecimal(amountStr);

                int partitionIndex = Math.abs(accountId.hashCode()) % PARTITION_COUNT;

                partitions.get(partitionIndex).submit(() -> processTransaction(accountId, amount));
            } catch (NumberFormatException e) {
                System.err.println("Invalid amount: " + amountStr + " in line: " + line);
            }
        }
    }

    public void processTransaction(String accountId, BigDecimal amount) {
        accountStore.compute(accountId, (key, account) -> {
            if (account == null) {
                account = new Account(accountId);
            }
            account.applyTransaction(amount);
            return account;
        });
    }

    public void shutdownAndAwait() {
        for (ExecutorService executor : partitions) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }


    static class Account {
        private final String id;
        private BigDecimal balance;

        public Account(String id) {
            this.id = id;
            this.balance = BigDecimal.ZERO;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void applyTransaction(BigDecimal amount) {
            if(amount.signum() > 0) {
                balance = balance.add(amount);
                System.out.println("Deposited " + amount + " to account " + id + ". New balance: " + balance);
            } else {
                BigDecimal newBalance = balance.add(amount);
                if (newBalance.signum() < 0) {
                    System.err.println("Insufficient funds for account " + id + ". Transaction amount: " + amount + ", Current balance: " + balance);
                } else {
                    balance = newBalance;
                    System.out.println("Withdrew " + amount.abs() + " from account " + id + ". New balance: " + balance);
                }
            }
        }
    }

    public void printBalances() {
        for (Map.Entry<String, Account> entry : accountStore.entrySet()) {
            System.out.println("Account ID: " + entry.getKey() + ", Balance: " + entry.getValue().getBalance());
        }
    }

    public static void main(String[] args) {
        PaymentProcessor processor = new PaymentProcessor();
        processor.processFile("transactions.csv");
        System.out.println("Final Balances:");
        processor.printBalances();
    }
}