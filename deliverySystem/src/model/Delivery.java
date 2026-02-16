package model;

public class Delivery {

    private double hourlyRate;
    private long startTime;
    private long endTime;

    public Delivery(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public double getCost(long startTime, long endTime, double rate) {
        hourlyRate = rate;
        return ((endTime - startTime) / 3600) * rate;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
