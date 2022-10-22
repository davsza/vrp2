package data;

import java.util.ArrayList;
import java.util.List;

public class Vehicle {

    private Integer type;
    private Node departureNode;
    private Node arrivalNode;
    private Integer capacity;
    private Float maximumTravelTime;
    private List<Node> route;
    private Integer currentNodeId;
    private Float currentTime;

    private Integer maximumCapacity;

    public Vehicle() {
        this.route = new ArrayList<>();
    }

    public List<Node> getRoute() {
        return route;
    }

    public Integer getMaximumCapacity() {
        return maximumCapacity;
    }

    public void setMaximumCapacity(Integer maximumCapacity) {
        this.maximumCapacity = maximumCapacity;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public void setDepartureNode(Node departureNode) {
        this.departureNode = departureNode;
    }

    public void setArrivalNode(Node arrivalNode) {
        this.arrivalNode = arrivalNode;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setMaximumTravelTime(Float maximumTravelTime) {
        this.maximumTravelTime = maximumTravelTime;
    }

    public Integer getCurrentNodeId() {
        return currentNodeId;
    }

    public Integer getType() {
        return type;
    }

    public Node getDepartureNode() {
        return departureNode;
    }

    public Node getArrivalNode() {
        return arrivalNode;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setRoute(List<Node> route) {
        this.route = route;
    }

    public Float getMaximumTravelTime() {
        return maximumTravelTime;
    }

    public void setCurrentNodeId(Integer currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public Float getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Float currentTime) {
        this.currentTime = currentTime;
    }

    public void initVehicle(int size) {
        setCurrentNodeId(0);
        setCurrentTime((float) 0);
        setCapacity(0);
        for(int i = 0; i < size; i++) {
            Node node = new Node();
            node.setGhostNode(true);
            route.add(node);
        }
    }

    public void switchNode(Node fromNode, Node toNode) {
        int index = fromNode.getId();
        this.route.set(index, toNode);
    }
}
