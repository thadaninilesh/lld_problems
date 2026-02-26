import java.util.*;

public class Meetings {

    static class Interval {
        long start;
        long end;

        public Interval(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }

    public boolean overlap(Set<Interval> meetings) {
        if (meetings.size() < 2) {
            return false;
        }
        List<Interval> sorted = new ArrayList<>(meetings);
        sorted.sort(Comparator.comparing(m -> m.start));
        for (int i = 1; i < meetings.size(); i++) {
            Interval prev = sorted.get(i-1);
            Interval curr = sorted.get(i);
            if (curr.start < prev.end) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        Meetings manager = new Meetings();
        System.out.println("--- Starting Tests ---");

        testNoOverlaps(manager);         // Should Pass
        testOverlapWithFirst(manager);   // Should Pass (Coincidentally)
        testOverlapDeepInList(manager);  // WILL FAIL due to the logic bug
        testTouchingIntervals(manager);  // Edge case check

        System.out.println("\n--- Tests Finished ---");
    }

    private static void testNoOverlaps(Meetings manager) {
        System.out.println("\nTest 1: Disjoint Meetings (No Overlap)");
        Set<Interval> set = new HashSet<>();
        set.add(new Interval(1, 5));
        set.add(new Interval(10, 15));
        set.add(new Interval(20, 25));

        boolean result = manager.overlap(set);
        assertResult(false, result, "1-5, 10-15, 20-25");
    }

    private static void testOverlapWithFirst(Meetings manager) {
        System.out.println("\nTest 2: Overlap involving the FIRST meeting");
        Set<Interval> set = new HashSet<>();
        set.add(new Interval(1, 100)); // Huge meeting covering everyone
        set.add(new Interval(10, 20));
        set.add(new Interval(30, 40));

        // This works because your code compares index 1 vs index 0.
        boolean result = manager.overlap(set);
        assertResult(true, result, "1-100 overlaps with 10-20");
    }

    private static void testOverlapDeepInList(Meetings manager) {
        System.out.println("\nTest 3: Overlap NOT involving the first meeting (The Bug Catcher)");
        Set<Interval> set = new HashSet<>();
        set.add(new Interval(1, 5));   // Meeting A (Index 0)
        set.add(new Interval(10, 20)); // Meeting B (Index 1)
        set.add(new Interval(15, 25)); // Meeting C (Index 2) - Overlaps with B!

        // Your Logic Trace:
        // i=1: Compare B(10-20) with A(1-5). No overlap.
        // i=2: Compare C(15-25) with A(1-5). No overlap.
        // Returns FALSE. (Should be TRUE).

        boolean result = manager.overlap(set);
        assertResult(true, result, "10-20 and 15-25 overlap, ignoring 1-5");
    }

    private static void testTouchingIntervals(Meetings manager) {
        System.out.println("\nTest 4: Touching Intervals (Edge Case)");
        Set<Interval> set = new HashSet<>();
        set.add(new Interval(1, 5));
        set.add(new Interval(5, 10)); // Starts exactly when previous ends

        // Usually start == end is NOT considered an overlap in meeting logic
        boolean result = manager.overlap(set);
        assertResult(false, result, "1-5 and 5-10 touch but don't overlap");
    }

    private static void assertResult(boolean expected, boolean actual, String message) {
        if (expected == actual) {
            System.out.println("PASS: " + message);
        } else {
            System.err.println("FAIL: " + message + " (Expected " + expected + ", Got " + actual + ")");
        }
    }

}
