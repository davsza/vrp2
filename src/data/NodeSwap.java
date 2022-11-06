package data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NodeSwap {

    private Node node;
    private Vehicle vehicle;
    private float value;
    private int index;
    private boolean foundVehicleForNodeToInsert;
    private Set<Vehicle> vehicleSet;
    private List<Float> values;

    public NodeSwap(Node node, Vehicle vehicle, float value, int index, boolean foundVehicleForNodeToInsert) {
        this.node = node;
        this.vehicle = vehicle;
        this.value = value;
        this.index = index;
        this.foundVehicleForNodeToInsert = foundVehicleForNodeToInsert;
    }

    public NodeSwap(Node node) {
        this.node = node;
        this.vehicleSet = new HashSet<>();
        this.values = new ArrayList<>();
    }

    public NodeSwap() {

    }

    public Set<Vehicle> getVehicleSet() {
        return vehicleSet;
    }

    public void setVehicleSet(Set<Vehicle> vehicleSet) {
        this.vehicleSet = vehicleSet;
    }

    public List<Float> getValues() {
        return values;
    }

    public void setValues(List<Float> values) {
        this.values = values;
    }

    public boolean isFoundVehicleForNodeToInsert() {
        return foundVehicleForNodeToInsert;
    }

    public void setFoundVehicleForNodeToInsert(boolean foundVehicleForNodeToInsert) {
        this.foundVehicleForNodeToInsert = foundVehicleForNodeToInsert;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
