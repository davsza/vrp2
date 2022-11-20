package data;

import java.util.*;

public class NodeSwap {

    private Node node;
    private Vehicle vehicle;
    private float value;
    private int index;
    private boolean foundVehicleForNodeToInsert;
    private Set<Vehicle> vehicleSet;
    private List<Float> values;
    private List<NodeSwap> regretNodeSwapList;

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
        this.regretNodeSwapList = new ArrayList<>();
    }

    public NodeSwap() {
    }

    public int getNumberOfFeasibleVehiclesToInsertInto() {
        return regretNodeSwapList.size();
    }

    public List<NodeSwap> getRegretNodeSwapList() {
        return regretNodeSwapList;
    }

    public void setRegretNodeSwapList(List<NodeSwap> regretNodeSwapList) {
        this.regretNodeSwapList = regretNodeSwapList;
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

    public void sortRegretList() {
        regretNodeSwapList.sort(new Comparator<NodeSwap>() {
            @Override
            public int compare(NodeSwap o1, NodeSwap o2) {
                return (Float.compare(o1.getValue(), o2.getValue()));
            }
        });
    }

    public float getRegretSum(int index) {
        float sum = 0;
        float bestValue = regretNodeSwapList.get(0).getValue();
        for(int i = 1; i < index; i++) {
            sum += regretNodeSwapList.get(i).getValue() - bestValue;
        }
        return sum;
    }
}
