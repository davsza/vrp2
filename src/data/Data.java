package data;

import java.util.ArrayList;
import java.util.List;

public class Data {

    private String dataset;
    private String info;
    private Float[][] matrix;
    private List<Node> nodeList;
    private List<Vehicle> fleet;

    public Data() {
        this.nodeList = new ArrayList<>();
        this.fleet = new ArrayList<>();
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

    public void matrixPrint() {
        for(int i = 0; i < this.matrix.length; i++) {
            for(int j = 0; j < this.matrix.length; j++) {
                System.out.print(this.matrix[i][j] + " ");
            }
            System.out.println();
        }
    }

    public void setMatrix(Float[][] matrix) {
        this.matrix = matrix;
    }


    public List<Node> getNodeList() {
        return this.nodeList;
    }

    public List<Vehicle> getFleet() {
        return fleet;
    }

    public boolean unvisitedNode() {
        for(Node node : nodeList) {
            if(!node.getGhostNode() && !node.getDumpingSite() && !node.getDepot()) {
                if(!node.getVisited()) {
                    return true;
                }
            }
        }
        return false;
    }

    public Node getDepotNode() {
        return this.nodeList.get(0);
    }

    public Node findNextNode(Vehicle vehicle, Node currentNode) {
        int currentTime = vehicle.getCurrentTime();
        float bestDistance = Float.MAX_VALUE;
        Node bestNode = null;
        for(Node node : nodeList) {
            float distance = getDistanceBetweenNode(currentNode, node); //
            float arrivalTime = currentTime + distance;
            if(customerNodeAndNotVisitedYet(node)
                    && travelCheck(arrivalTime, node)
                    && maxTravelTimeCheck(vehicle, currentNode, node) && capacityCheck(vehicle, node)
                    && distance < bestDistance)
            {
                bestDistance = matrix[currentNode.getId()][node.getId()];
                bestNode = node;
            }
        }
        return bestNode;
    }

    private boolean capacityCheck(Vehicle vehicle, Node node) {
        return vehicle.getCapacity() + node.getQuantity() < vehicle.getMaximumCapacity();
    }

    private boolean maxTravelTimeCheck(Vehicle vehicle, Node currentNode, Node nextNode) {
        Node nearestDumpingSite = getNearestDumpingSiteNode(nextNode);
        return vehicle.getCurrentTime()
                + matrix[currentNode.getId()][nextNode.getId()]
                + matrix[nextNode.getId()][nearestDumpingSite.getId()]
                + matrix[nearestDumpingSite.getId()][getDepotNode().getId()]
                < vehicle.getMaximumTravelTime();
    }

    private Node getNearestDumpingSiteNode(Node nextNode) {
        Float bestDistance = Float.MAX_VALUE;
        Node nearestDumpingSite = null;
        for(Node node : nodeList) {
            if(node.getDumpingSite()) {
                Float distance = matrix[nextNode.getId()][node.getId()];
                if(distance < bestDistance) {
                    bestDistance = distance;
                    nearestDumpingSite = node;
                }
            }
        }
        return nearestDumpingSite;
    }

    private boolean travelCheck(float arrivalTime, Node node) {
        return arrivalTime > node.getTimeStart() && arrivalTime < node.getTimeEnd();
    }

    private boolean customerNode(Node node) {
        return !node.getGhostNode() && !node.getDepot() && !node.getDumpingSite();
    }

    private boolean customerNodeAndNotVisitedYet(Node node) {
        return customerNode(node) && !node.getVisited();
    }

    private float getDistanceBetweenNode(Node nodeFrom, Node nodeTo) {
        return matrix[nodeFrom.getId()][nodeTo.getId()];
    }
}
