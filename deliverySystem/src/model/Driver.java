package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Driver {

    private String id;

    private List<Delivery> deliveries;

    private double rate;

    private long lastPayTime = 0;

    public long getLastPayTime() {
        return lastPayTime;
    }

    public void setLastPayTime(long lastPayTime) {
        this.lastPayTime = lastPayTime;
    }

    public Driver(String id, double rate) {
        this.id = id;
        this.rate = rate;
    }

    public List<Delivery> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<Delivery> deliveries) {
        this.deliveries = deliveries;
    }

    public void addDeliveries(Delivery delivery) {
        if (Objects.isNull(deliveries)) {
            deliveries = new ArrayList<>();
        }
        deliveries.add(delivery);
    }

    public double getRate() {
        return rate;
    }
}
