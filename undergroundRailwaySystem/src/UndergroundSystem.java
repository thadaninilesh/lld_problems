import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An underground railway system is keeping track of customer travel times between different stations.
 * They are using this data to calculate the average time it takes to travel from one station to another.
 * You may assume all calls to the checkIn and checkout methods are consistent:
 * If a customer checks in at time t1 then checks out at time t2, then t1 < t2.
 * All events happen in chronological order.
 * public class UndergroundSystem {
 *      Choose appropriate data structures to store the necessary information
 *      public UndergroundSystem {}
 *          Instantiate any necessary data structures here
 *          A customer with a card ID equal to id, checks in at the station stationName at time t.
 *          A customer can only be checked into one place at a time.
 *          Time t is a monotonously increasing integer value
 *      public void checkIn(int id, String stationName, int t) {}
 *           Implement logic to handle customer check-in
 *           A customer with a card ID equal to id, checks out from the station stationName at time t.
 *           Time t is a monotonously increasing integer value
 *      public double checkOut(int id, String stationName, int t) throws IllegalArgumentException {}
 *           Implement logic to handle customer check-out
 *           Returns the average time it takes to travel from startStation to endStation.
 *           The average time is computed from all the previous traveling times from startStation to endStation that happened directly, meaning a check in at startStation followed by a check out from endStation.
 *           The time it takes to travel from startStation to endStation may be different from the time it takes to travel from endStation to startStation.
 *           There will be at least one customer that has traveled from startStation to endStation before getAverageTime is called.
 *           The method should throw an IllegalArgumentException if the customer was not checked in at the time of check-out.
 *           If there are no travels between the stations, return -1.
 *      public double getAverageTime (String startStation, String endStation){}
 *           Implement logic to calculate the average travel time between startStation and endStation
 * }
 * class UndergroundSystemDriver {
 *      public static void main(String[]args) {
 *          UndergroundSystem undergroundSystem = new UndergroundSystem;
 *          undergroundSystem.checkIn(1,"A",3); undergroundSystem.checkIn(2,"A",8); undergroundSystem.checkOut(1,*"B",15);
 *          undergroundSystem.checkOut(2, "B", *, 20);
 *          double avgTimel = undergroundSystem.getAverageTime("A","B"); // return 12.0
 *          System.out println("Average time from A to B: "+avgTimel); undergroundSystem.checkIn(3, "A",10);
 *          undergroundSystem.checkOut(3,"B",25);
 *          double avgTime2 = undergroundSystem.getAverageTime("","B"); // return 13.33333
 *          System.out.println("Average time from A to B: "+avgTime2);
 *      }
 * }
 */

public class UndergroundSystem {

    private final Map<String, Map<String, AverageData>> completedTravels = new HashMap<>();

    private final Map<Integer, CustomerTaps> checkinDataMap = new HashMap<>();

    private record CustomerTaps(String stationName, int time) { }

    private static class AverageData {
        double totalTime = 0;
        int count = 0;

        synchronized void update(int time) {
            totalTime += time;
            count++;
        }

        double getAverage() {
            return count == 0 ? 0 : totalTime / count;
        }
    }

    public void checkin(int cardId, String stationName, int t) {
        checkinDataMap.put(cardId, new CustomerTaps(stationName, t));
    }

    public void checkout(int cardId, String stationName, int time) throws IllegalArgumentException {
        if (!checkinDataMap.containsKey(cardId)) {
            throw new IllegalArgumentException("Customer with card ID " + cardId + " was not checked in.");
        }

        CustomerTaps checkinData = checkinDataMap.get(cardId);

        String startStation = checkinData.stationName();
        int checkinTime = checkinData.time();

        int travelTime = time - checkinTime;

        System.out.println("Time taken from " + startStation + " to " + stationName + " is: " + travelTime);

        Map<String, AverageData> endStationTime = completedTravels.getOrDefault(startStation, new HashMap<>());
        AverageData averageData = endStationTime.getOrDefault(stationName, new AverageData());
        averageData.update(travelTime);
        endStationTime.put(stationName, averageData);

        completedTravels.put(startStation, endStationTime);

        checkinDataMap.remove(cardId);

    }

    public double getAverageTime(String startStation, String endStation) {
        if (!completedTravels.containsKey(startStation) ||
                !completedTravels.get(startStation).containsKey(endStation)) {
            return -1;
        }

        AverageData averageData = completedTravels.get(startStation).get(endStation);
        return averageData.getAverage();
    }

    public static void main(String[]args) {
        UndergroundSystem undergroundSystem = new UndergroundSystem();
        undergroundSystem.checkin(1,"A",3);
        undergroundSystem.checkin(2,"A",8);
        undergroundSystem.checkout(1,"B",15);
        undergroundSystem.checkout(2, "B", 20);
        double avgTime = undergroundSystem.getAverageTime("A", "B"); // return 12.0

        System.out.println("Average time from A to B: "+avgTime);

        undergroundSystem.checkin(3, "A",10);
        undergroundSystem.checkout(3,"B",25);
        double avgTime2 = undergroundSystem.getAverageTime("A","B");

        System.out.println("Average time from A to B: "+avgTime2); // return 13.0
    }

}