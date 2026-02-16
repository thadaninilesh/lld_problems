import java.util.*;

public class CurrencyExchange {
    // Adjacency List: Source Currency -> (Target Currency -> Rate)
    private final Map<String, Map<String, Double>> graph = new HashMap<>();

    /**
     * Adds a rate to the system.
     * We add both directions (A -> B and B -> A) automatically.
     */
    public void addRate(String source, String target, double rate) {
        graph.computeIfAbsent(source, k -> new HashMap<>()).put(target, rate);
        graph.computeIfAbsent(target, k -> new HashMap<>()).put(source, 1.0 / rate);
    }

    /**
     * Finds the exchange rate using BFS.
     */
    public double getExchangeRate(String start, String end) {
        if (!graph.containsKey(start) || !graph.containsKey(end)) return -1.0;
        if (start.equals(end)) return 1.0;

        // Queue for BFS: stores pairs of (Current Currency, Cumulative Rate)
        Queue<Node> queue = new LinkedList<>();
        queue.add(new Node(start, 1.0));

        // Set to keep track of visited nodes to prevent infinite loops
        Set<String> visited = new HashSet<>();
        visited.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            String currentCurrency = current.symbol;
            double currentRate = current.rate;

            // Get all neighboring currencies
            Map<String, Double> neighbors = graph.get(currentCurrency);
            for (Map.Entry<String, Double> neighbor : neighbors.entrySet()) {
                String nextCurrency = neighbor.getKey();
                double rateToNext = neighbor.getValue();

                if (nextCurrency.equals(end)) {
                    return currentRate * rateToNext;
                }

                if (!visited.contains(nextCurrency)) {
                    visited.add(nextCurrency);
                    queue.add(new Node(nextCurrency, currentRate * rateToNext));
                }
            }
        }

        return -1.0; // No path found
    }

    // Helper class to store BFS state
    private static class Node {
        String symbol;
        double rate;

        Node(String symbol, double rate) {
            this.symbol = symbol;
            this.rate = rate;
        }
    }

    public static void main(String[] args) {
        CurrencyExchange ce = new CurrencyExchange();
        ce.addRate("USD", "JPY", 110.0);
        ce.addRate("USD", "EUR", 0.9);
        ce.addRate("EUR", "GBP", 0.8);

        System.out.println("USD to GBP: " + ce.getExchangeRate("USD", "GBP")); // 0.72
        System.out.println("JPY to GBP: " + ce.getExchangeRate("JPY", "GBP")); // ~0.0065
        System.out.println("USD to ABC: " + ce.getExchangeRate("USD", "ABC")); // -1.0
    }
}
