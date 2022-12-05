package data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Data {

    private String dataset;
    private String info;
    private Float[][] matrix;
    private List<Node> nodeList;
    private List<Vehicle> fleet;
    private List<Integer> dumpingSites;

    public Data() {
        this.nodeList = new ArrayList<>();
        this.fleet = new ArrayList<>();
        this.dumpingSites = new ArrayList<>();
    }

    public Data(Data data) {
        this.dataset = data.getDataset();
        this.info = data.getInfo();
        this.matrix = data.getMatrix();
        this.nodeList = copyNodeList(data.getNodeList());
        this.fleet = copyFleet(data.getFleet());
        this.dumpingSites = data.getDumpingSites();
    }

    private List<Vehicle> copyFleet(List<Vehicle> fleet) {
        List<Vehicle> vehicles = new ArrayList<>();
        for(Vehicle vehicle : fleet) {
            Vehicle copiedVehicle = new Vehicle(vehicle);
            vehicles.add(copiedVehicle);
        }
        return vehicles;
    }

    private List<Node> copyNodeList(List<Node> nodeList) {
        List<Node> nodes = new ArrayList<>();
        for(Node node : nodeList) {
            Node copiedNode = new Node(node);
            nodes.add(copiedNode);
        }
        return nodes;
    }

    public String getDataset() {
        return dataset;
    }

    public Float[][] getMatrix() {
        return matrix;
    }

    public List<Integer> getDumpingSites() {
        return dumpingSites;
    }

    public void setDumpingSites(List<Integer> dumpingSites) {
        this.dumpingSites = dumpingSites;
    }

    public void setNodeList(List<Node> nodeList) {
        this.nodeList = nodeList;
    }

    public void setFleet(List<Vehicle> fleet) {
        this.fleet = fleet;
    }

    public String getInfo() {
        return info;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void addNode(Node node)
    {
        this.nodeList.add(node);
    }

    public void addVehicle(Vehicle vehicle) { this.fleet.add(vehicle); }

    public int getNodeListSize() { return this.nodeList.size(); }

    public Node getNodeOnIndex(int idx) { return this.nodeList.get(idx); }

    public int getMatrixRowCount() { return this.matrix.length; }

    public void setMatrix(Float[][] matrix) {
        this.matrix = matrix;
    }


    public List<Node> getNodeList() {
        return this.nodeList;
    }

    public List<Vehicle> getFleet() {
        return fleet;
    }

    public boolean hasMoreUnvisitedNodes() {
        return nodeList.stream().anyMatch(node -> !node.isDepot() && !node.isDumpingSite() && !node.isVisited());
    }

    public Node getDepotNode() {
        return this.nodeList.get(0);
    }

    public Node findNextNode(Vehicle currentVehicle, Node currentNode) {
        float distance = Float.MAX_VALUE;
        Node nextNode = new Node();
        nextNode.setNullNode(true);
        Node nearestDump = getNearestDumpingSiteNode(currentVehicle, currentNode);
        float dumpDistance = getDistanceBetweenNode(currentNode, nearestDump);
        List<Node> feasibleNodes = nodeList.stream().filter(node -> !node.isDepot() && !node.isDumpingSite() && !node.isVisited()).collect(Collectors.toList());
        for(Node node : feasibleNodes) {
            float travelDistance = getDistanceBetweenNode(currentNode, node);
            if(currentNode.isDepot()
                    && currentNode.getTimeStart() + currentNode.getServiceTime() + travelDistance <= node.getTimeEnd()
                    && node.getTimeStart() <= currentNode.getTimeEnd() + travelDistance
                    && capacityCheck(currentVehicle, node)
                    && maximumNodesVisited(currentVehicle)) {
                distance = travelDistance;
                nextNode = node;
            } else if(travelDistance < distance
                    && capacityCheck(currentVehicle, node)
                    && maximumNodesVisited(currentVehicle)
                    && timeWindowCheck(currentVehicle.getCurrentTime() + currentNode.getServiceTime() + travelDistance, node)
                    && currentVehicle.getCurrentTime() + currentNode.getServiceTime() + travelDistance >= node.getTimeStart()
                    && checkForDepotTW(currentVehicle, currentNode)) {
                distance = travelDistance;
                nextNode = node;
            }
        }
        if(currentVehicle.getCapacity() >= currentVehicle.getMaximumCapacity() * 0.8 && dumpDistance < distance) {
            Node nullNode = new Node();
            nullNode.setNullNode(true);
            return nullNode;
        }
        return nextNode;
    }

    // TODO: fix needed, depot endtime reached
    private boolean checkForDepotTW(Vehicle currentVehicle, Node currentNode) {
        Node dumpingSite = getNearestDumpingSiteNode(currentVehicle, currentNode);
        float travelDistanceFromCurrentNodeToDumpingSite = getDistanceBetweenNode(currentNode, dumpingSite);
        float dumpingSiteServiceTime = dumpingSite.getServiceTime();
        float travelDistanceFromDumpingSiteToDepot = getDistanceBetweenNode(dumpingSite, getDepotNode());
        return timeWindowCheck(currentVehicle.getCurrentTime()
                + travelDistanceFromCurrentNodeToDumpingSite
                + dumpingSiteServiceTime
                + travelDistanceFromDumpingSiteToDepot, getDepotNode());
    }

    private boolean maximumNodesVisited(Vehicle currentVehicle) {
        return currentVehicle.getRoute().size() < currentVehicle.getMaximumNumberOfStopsToVisit();
    }

    private boolean capacityCheck(Vehicle vehicle, Node node) {
        return vehicle.getCapacity() + node.getQuantity() <= vehicle.getMaximumCapacity();
    }

    public Node getNearestDumpingSiteNode(Vehicle currentVehicle, Node currentNode) {
        float bestDistance = Float.MAX_VALUE;
        Node nearestDumpingSite = null;
        for(Node node : nodeList) {
            if(node.isDumpingSite()) {
                float travelDistance = getDistanceBetweenNode(currentNode, node);
                if(travelDistance < bestDistance
                        && timeWindowCheck(currentVehicle.getCurrentTime() + travelDistance, node)) {
                    bestDistance = travelDistance;
                    nearestDumpingSite = node;
                }
            }
        }
        return nearestDumpingSite;
    }

    public boolean timeWindowCheck(float arrivalTime, Node node) {
        //return arrivalTime >= node.getTimeStart() && arrivalTime <= node.getTimeEnd();
        return arrivalTime <= node.getTimeEnd();
    }

    public float getDistanceBetweenNode(Node nodeFrom, Node nodeTo) {
        return matrix[nodeFrom.getId()][nodeTo.getId()];
    }

    public void destroyInfo() {
        for(Vehicle vehicle : fleet) {
            vehicle.setCapacity((float)0);
            vehicle.setCurrentTime((float) 0);
            for(Node node : vehicle.getRoute().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).collect(Collectors.toList())) {
                node.setVisited(false);
                node.setVisitedAt((float) 0);
            }
        }
    }

    public Vehicle getPenaltyVehicle() {
        for(Vehicle vehicle : fleet) {
            if(vehicle.isPenaltyVehicle()) {
                return vehicle;
            }
        }
        return null;
    }

    public String dataToHash() {
        StringBuilder stringBuilder = new StringBuilder();
        for(Vehicle vehicle : fleet) {
            String hash = vehicle.routeHash();
            stringBuilder.append(hash);
        }
        return stringBuilder.toString();
    }

    public void calculateVisitingTime() {
        for(Vehicle vehicle : fleet) {
            vehicle.setCurrentTime((float)getDepotNode().getTimeStart());
            for(int i = 1; i < vehicle.getRoute().size(); i++) {
                float serviceTimeAtPreviousNode = vehicle.getRoute().get(i - 1).getServiceTime();
                float travelTimeBetweenPreviousAndCurrentNode = getDistanceBetweenNode(vehicle.getRoute().get(i - 1), vehicle.getRoute().get(i));
                float currentTime = vehicle.getCurrentTime() + serviceTimeAtPreviousNode + travelTimeBetweenPreviousAndCurrentNode;
                vehicle.getRoute().get(i).setVisitedAt(currentTime);
            }
        }
    }

    public float getMaximumTravelDistance() {
        float maxValue = 0;
        for (Float[] floats : matrix) {
            for (Float aFloat : floats) {
                if (aFloat > maxValue) {
                    maxValue = aFloat;
                }
            }
        }
        return maxValue;
    }
}
