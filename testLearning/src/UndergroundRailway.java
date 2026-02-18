import java.util.HashMap;
import java.util.Map;

public class UndergroundRailway {

    static class AverageData {
        double totalTime = 0;
        int count = 0;

        synchronized void update(int time) {
            totalTime += time;
            count++;
        }

        double getAvg() {
            return count == 0 ? 0 : totalTime / count;
        }
    }

    record CustomerTaps(String stationName, int time) {}
    private final Map<String, Map<String, AverageData>> completedTravels = new HashMap<>();
    private final Map<Integer, CustomerTaps> checkinData = new HashMap<>();

    public void checkIn(int id, String stationName, int t) {
        checkinData.put(id, new CustomerTaps(stationName, t));
    }

    public void checkOut(int id, String stationName, int t) {
        if (!checkinData.containsKey(id)) {
            throw new IllegalArgumentException();
        }
        CustomerTaps checkin = checkinData.get(id);
        String startStation = checkin.stationName();
        int startTime = checkin.time();

        int travelTime = t - startTime;
        System.out.println("Time taken from " + startStation + " to " + stationName + " is: " + travelTime);

        Map<String, AverageData> toStationTravels = completedTravels.getOrDefault(startStation, new HashMap<>());

        AverageData avgData = toStationTravels.getOrDefault(stationName, new AverageData());
        avgData.update(travelTime);
        toStationTravels.put(stationName, avgData);
        completedTravels.put(startStation, toStationTravels);
        checkinData.remove(id);


    }

    public double getAvgTime(String stationName, String endStation) {
        if(!completedTravels.containsKey(stationName) || !completedTravels.get(stationName).containsKey(endStation)) {
            return -1;
        }
        AverageData avgData = completedTravels.get(stationName).get(endStation);
        return avgData.getAvg();
    }

    public static void main(String[]args) {
        UndergroundRailway undergroundSystem = new UndergroundRailway();
        undergroundSystem.checkIn(1,"A",3);
        undergroundSystem.checkIn(2,"A",8);
        undergroundSystem.checkOut(1,"B",15);
        undergroundSystem.checkOut(2, "B", 20);
        double avgTime = undergroundSystem.getAvgTime("A", "B"); // return 12.0

        System.out.println("Average time from A to B: "+avgTime);

        undergroundSystem.checkIn(3, "A",10);
        undergroundSystem.checkOut(3,"B",25);
        double avgTime2 = undergroundSystem.getAvgTime("A","B");

        System.out.println("Average time from A to B: "+avgTime2); // return 13.0
    }
}
