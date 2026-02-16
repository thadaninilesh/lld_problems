import java.util.*;

/**
 * Given a set of meetings with a start time and an end time, find out if any of them overlap.
 * public class Interval {
 *      long start; long end;
 *      public boolean overlap(Set<Interval> meetings) { }
 * }
 * Possible approaches
 * Naive solution
 * Iterate through the meetings and for each one of them, check it against all the other meetings by verifying that either the start time or the end time falls between another meeting's start and and times. This solution works but has a complexity of O(N^2).
 * Consider time to be discrete
 * If we assume that the meetings' start and end times are discrete (i.e. meetings can only start and end on the minute), we could iterate through all the possible times and check if they fall within more than one meeting. The complexity of this solution is OCT * N) where T is the number of possible start times of the meeting.
 * This solution has two main drawbacks: it only works if we consider time to be discrete, and is only an improvement over the naive solution if the number of meetings N is much larger than the number of times we are checking (an unlikely assumption).
 * Optimal solution - sort by start time.
 * Sort the meetings by start time. Iterate through the meetings, starting with the second one. If the previous meeting's end time is larger than the current one's start time, we have "
 * Sorting the meetings is ONLogN) and checking for overlaps afterwards is O(N).
 * Interval trees
 * A meeting is just an interval and so can be stored in an interval tree. Constructing the tree and querying for overlaps could be done at the same time, and since insertion in the tree is O(logN), the complexity of this solution is 0(NlogN).
 * Coding the interval tree data structure is relatively complex and it is expected that only exceptional candidates would be able to do it within the limited time of the interview.
 */

public class MeetingManager {

        static class Interval {
           private final long start;
           private final long end;


           public Interval(long start, long end) {
               this.start = start;
               this.end = end;
           }

        }

        public boolean overlap(Set<Interval> meetings) {
            if (meetings.size() < 2) {
                return false;
            }

            // Sort meetings by start time
            List<Interval> sortedMeetings = new ArrayList<>(meetings);
            Collections.sort(sortedMeetings, Comparator.comparing(m -> m.start));
            for (int i = 1; i < sortedMeetings.size(); i++) {
                Interval prevMeeting = sortedMeetings.get(i - 1);
                Interval currentMeeting = sortedMeetings.get(i);
                if (prevMeeting.end > currentMeeting.start) {
                        return true;
                }
            }

            return false; // No overlaps
        }

        public static void main(String[] args) {
                MeetingManager manager = new MeetingManager();
                System.out.println("--- Starting Tests ---");

                testNoOverlaps(manager);         // Should Pass
                testOverlapWithFirst(manager);   // Should Pass (Coincidentally)
                testOverlapDeepInList(manager);  // WILL FAIL due to the logic bug
                testTouchingIntervals(manager);  // Edge case check

                System.out.println("\n--- Tests Finished ---");
        }

        private static void testNoOverlaps(MeetingManager manager) {
                System.out.println("\nTest 1: Disjoint Meetings (No Overlap)");
                Set<Interval> set = new HashSet<>();
                set.add(new Interval(1, 5));
                set.add(new Interval(10, 15));
                set.add(new Interval(20, 25));

                boolean result = manager.overlap(set);
                assertResult(false, result, "1-5, 10-15, 20-25");
        }

        private static void testOverlapWithFirst(MeetingManager manager) {
                System.out.println("\nTest 2: Overlap involving the FIRST meeting");
                Set<Interval> set = new HashSet<>();
                set.add(new Interval(1, 100)); // Huge meeting covering everyone
                set.add(new Interval(10, 20));
                set.add(new Interval(30, 40));

                // This works because your code compares index 1 vs index 0.
                boolean result = manager.overlap(set);
                assertResult(true, result, "1-100 overlaps with 10-20");
        }

        private static void testOverlapDeepInList(MeetingManager manager) {
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

        private static void testTouchingIntervals(MeetingManager manager) {
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
