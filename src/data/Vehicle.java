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
    private Float currentTime;
    private Float travelTime;

    private Integer maximumCapacity;
    private Integer id;

    public Vehicle() {
        this.route = new ArrayList<>();
    }

    public Vehicle(Vehicle vehicle) {
        this.type = vehicle.getType();
        this.departureNode = vehicle.getDepartureNode();
        this.arrivalNode = vehicle.getArrivalNode();
        this.capacity = vehicle.getCapacity();
        this.maximumTravelTime = vehicle.getMaximumTravelTime();
        this.route = new ArrayList<>();
        for(Node node : vehicle.getRoute()) {
            Node copiedNode = new Node(node);
            this.route.add(copiedNode);
        }
        this.currentTime = vehicle.getCurrentTime();
        this.maximumCapacity = vehicle.getMaximumCapacity();
        this.id = vehicle.getId();
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

    public Float getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Float currentTime) {
        this.currentTime = currentTime;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public Float getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(Float travelTime) {
        this.travelTime = travelTime;
    }

    public void initVehicle(int size) {
        setCurrentTime((float) 0);
        setCapacity(0);
        setTravelTime((float)0);
        for(int i = 0; i < size; i++) {
            Node node = new Node();
            node.setGhostNode(true);
            node.setId(i);
            route.add(node);
        }
    }

    public void switchNode(Node fromNode, Node toNode) {
        this.route.set(fromNode.getId(), toNode);
    }

    public Node getFirstGhostNode() {
        for(Node node : route) {
            if(node.isGhostNode()) {
                return node;
            }
        }
        return null;
    }

    public Node getFirstCustomerNode() {
        for(Node node : route) {
            if(!node.isGhostNode()) {
                return node;
            }
        }
        return null;
    }

    public Float calculateTravelDistance(Data data, boolean timeWindow) {
        if(route.get(0).isGhostNode()) {
            return (float)0;
        }
        int currentIdx = 0;
        float travelDistance;
        Node currentNode = route.get(currentIdx);
        Node dumpingSite;
        setCapacity(Math.round(currentNode.getQuantity()));
        float distance = data.getDistanceBetweenNode(data.getDepotNode(), currentNode);
        while(!currentNode.isGhostNode()) {
            Node nextNode = route.get(currentIdx + 1);
            if(!nextNode.isGhostNode()) {
                float visitedAt = currentNode.getVisitedAt();
                float serviceTime = currentNode.getServiceTime();
                float travelTime = data.getDistanceBetweenNode(currentNode, nextNode);
                if(getCapacity() + nextNode.getQuantity() <= getMaximumCapacity()
                        && (timeWindow ? data.timeWindowCheck(visitedAt + serviceTime + travelTime, nextNode) : true)) {
                    travelDistance = data.getDistanceBetweenNode(currentNode, nextNode);
                    distance += travelDistance;
                    setCapacity(getCapacity() + Math.round(nextNode.getQuantity()));
                    currentNode = nextNode;
                } else {
                    dumpingSite = data.getNearestDumpingSiteNode(currentNode);
                    travelDistance = data.getDistanceBetweenNode(currentNode, dumpingSite);
                    distance += travelDistance;
                    setCapacity(0);
                    currentNode = dumpingSite;

                    nextNode = route.get(currentIdx + 1);
                    travelDistance = data.getDistanceBetweenNode(currentNode, nextNode);
                    distance += travelDistance;
                    setCapacity(getCapacity() + Math.round(nextNode.getQuantity()));
                    currentNode = nextNode;
                }
                currentIdx++;
            } else {
                dumpingSite = data.getNearestDumpingSiteNode(currentNode);
                distance += data.getDistanceBetweenNode(currentNode, dumpingSite);
                distance += data.getDistanceBetweenNode(dumpingSite, data.getDepotNode());
                currentNode = nextNode;
            }
        }
        return distance;
    }

    public void removeNode(Node nodeToRemove) {
        for(Node node : route) {
            if(node.equals(nodeToRemove)) {
                int currentIdx = route.indexOf(node);
                int firstGhostNodeIdx = route.indexOf(getFirstGhostNode());
                for(int i = currentIdx + 1; i < firstGhostNodeIdx; i++) {
                    route.set(i - 1, route.get(i));
                }
                route.set(firstGhostNodeIdx - 1, createGhostNode(firstGhostNodeIdx - 1));
            }
        }
    }

    private Node createGhostNode(int idx) {
        Node node = new Node();
        node.setGhostNode(true);
        node.setId(idx);
        return node;
    }

    public List<Node> copyRoute() {
        List<Node> route = new ArrayList<>();
        for(Node node : getRoute()) {
            Node copiedNode = new Node(node);
            route.add(copiedNode);
        }
        return route;
    }
}
