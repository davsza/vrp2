import data.Data;
import data.Node;
import data.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Solver {

    private List<Data> dataList;
    private String logPath;

    public Solver(List<Data> dataList, String logPath) {
        this.dataList = dataList;
        this.logPath = logPath;
    }

    public void initGreedy(Data data) {

        // TODO: change it to some logger
        //System.out.println("solving " + data.getInfo());

        // init vehicles
        for(Vehicle vehicle : data.getFleet()) vehicle.initVehicle(data.getNodeListSize());
        Vehicle penaltyVehicle = new Vehicle();
        penaltyVehicle.initVehicle(data.getNodeListSize());
        penaltyVehicle.setPenaltyVehicle(true);
        data.getFleet().add(penaltyVehicle);

        // the first vehicle starts from the depot
        Node currentNode = data.getDepotNode();
        Node nextNode;
        Vehicle currentVehicle = data.getFleet().get(0);
        float travelDistance;

        // do this until all nodes have been visited
        while(data.hasMoreUnvisitedNodes()){
            // find the next visitable node
            nextNode = data.findNextNode(currentVehicle, currentNode);
            // if it's not a null node, which means is visitable (vehicle found a feasible solution for next node)
            if(!nextNode.isNullNode()) {
                // calculate the travel distance between the current node and the next to be visited
                travelDistance = data.getDistanceBetweenNode(currentNode, nextNode);
                if(!currentNode.isDepot()) {
                    currentVehicle.setCurrentTime(currentVehicle.getCurrentTime() + travelDistance + nextNode.getServiceTime());
                } else {
                    currentVehicle.setCurrentTime(currentVehicle.getCurrentTime() + nextNode.getServiceTime());
                }
                currentVehicle.setTravelTime(currentVehicle.getTravelTime() + travelDistance + nextNode.getServiceTime());
                currentVehicle.setCapacity(currentVehicle.getCapacity() + Math.round(nextNode.getQuantity()));
                currentVehicle.switchNode(currentVehicle.getFirstGhostNode(), nextNode);
                currentNode = nextNode;
                currentNode.setVisitedAt(currentVehicle.getCurrentTime() - currentNode.getServiceTime());
                currentNode.setVisited(true);
            } else {
                // if the vehicle can't visit a customer, it has to either go to a dumping site or back to the depot

                // if the vehicle is at a dumping site, it's route is over and has to go back to the depot
                if(currentNode.isDumpingSite()) {
                    // go to the depot
                    travelDistance = data.getDistanceBetweenNode(currentNode, data.getDepotNode());
                    currentVehicle.setCurrentTime(currentVehicle.getCurrentTime() + travelDistance);
                    currentVehicle.setTravelTime(currentVehicle.getTravelTime() + travelDistance);
                    currentNode = data.getDepotNode();
                    // switch vehicle
                    currentVehicle = data.getFleet().get(currentVehicle.getId() + 1);
                    continue;
                }
                // find the nearest dumping site, to there, set capacity to 0
                Node dumpingSize = data.getNearestDumpingSiteNode(currentNode);
                travelDistance = data.getDistanceBetweenNode(currentNode, dumpingSize);
                currentVehicle.setCurrentTime(currentVehicle.getCurrentTime() + travelDistance + dumpingSize.getServiceTime());
                currentVehicle.setTravelTime(currentVehicle.getTravelTime() + travelDistance + dumpingSize.getServiceTime());
                currentVehicle.setCapacity(0);
                currentNode = dumpingSize;
                // if the vehicle's travel time is close to it's maximum, send it back to the depot from the dumping site
                if(currentVehicle.getTravelTime() > currentVehicle.getMaximumTravelTime() * 0.8) {
                    travelDistance = data.getDistanceBetweenNode(currentNode, data.getDepotNode());
                    currentVehicle.setCurrentTime(currentVehicle.getCurrentTime() + travelDistance);
                    currentVehicle.setTravelTime(currentVehicle.getTravelTime() + travelDistance);
                    currentNode = data.getDepotNode();
                    currentVehicle = data.getFleet().get(currentVehicle.getId() + 1);
                }
            }
        }

        for(Vehicle vehicle : data.getFleet()) {
            if(vehicle.getCapacity() != 0) {
                currentNode = vehicle.getFirstCustomerNode();
                Node dumpingSize = data.getNearestDumpingSiteNode(currentNode);
                travelDistance = data.getDistanceBetweenNode(currentNode, dumpingSize);
                vehicle.setCurrentTime(vehicle.getCurrentTime() + travelDistance + dumpingSize.getServiceTime());
                vehicle.setTravelTime(vehicle.getTravelTime() + travelDistance + dumpingSize.getServiceTime());
                vehicle.setCapacity(0);
                currentNode = dumpingSize;
                travelDistance = data.getDistanceBetweenNode(currentNode, data.getDepotNode());
                vehicle.setCurrentTime(vehicle.getCurrentTime() + travelDistance);
                vehicle.setTravelTime(vehicle.getTravelTime() + travelDistance);
            }
        }

        boolean passed = checkSolution(data);
        System.out.println(!passed ? "failed" : "");

        System.out.println("---------------------");
        for(Vehicle vehicle : data.getFleet()) {
            System.out.print("Vehicle nr " + vehicle.getId() + ", capacity:" + vehicle.getCapacity() + ", time: " + vehicle.getCurrentTime() + ": ");
            for(Node node : vehicle.getRoute()) {
                if(!node.isGhostNode()) {
                    System.out.print(node.getId() + " ");
                }
            }
            System.out.println(", distance: " + vehicle.calculateTravelDistance(data, false));
            System.out.println();
        }

        float value = getDataValue(data, true);

        //System.out.println("---------------------");
        //for(Node node : data.getNodeList()) {
        //    System.out.println("Node " + node.getId() + ", time window starts at " + node.getTimeStart() + ", vehicle arrives at " + node.getVisitedAt() + ", time windows ends at " + node.getTimeEnd());
        //}
    }

    public void ALNS(Data data) {
        float T = 90;
        data.destroyInfo();
        Data bestData = new Data(data);
        float bestValue = getDataValue(bestData, true);
        Data currentData;
        float currentValue, newValue;
        List<Node> nodesToSwap;
        while(T > 0.1) {
            currentData = new Data(data);
            currentValue = getDataValue(currentData, false);
            nodesToSwap = new ArrayList<>();
            destroyNodes(currentData, 1, nodesToSwap);
            repairNodes(currentData, nodesToSwap);
            newValue = getDataValue(currentData, false);
            float delta = currentValue - newValue;
            if(delta > 0) {
                data = currentData;
            } else if (Math.exp(delta / T) > Math.random()) {
                data = currentData;
            }
            if(newValue < bestValue) {
                bestValue = newValue;
                bestData = new Data(data);
            }
            //update information about performance of destroy and repair methods
            T *= 0.99;
        }
    }

    private void repairNodes(Data data, List<Node> nodesToSwap) {
        greedyInsert(data, nodesToSwap);
    }

    private void greedyInsert(Data data, List<Node> nodesToSwap) {
        float bestValue = getDataValue(data, false);
        Vehicle vehicleToInsertInto = null;
        int indexToInsert = 0;
        boolean foundVehicleForNodeToInsert = false;
        while (nodesToSwap.size() > 0) {
            Node nodeToInsert = nodesToSwap.get(0);
            nodesToSwap.remove(nodeToInsert);
            for(Vehicle vehicle : data.getFleet()) {
                if (!vehicle.isPenaltyVehicle()) {
                    int firstGhostNodeIdx = vehicle.getFirstGhostNode().getId();
                    for (int i = 0; i < firstGhostNodeIdx + 1; i++) {
                        List<Node> copiedRoute = vehicle.copyRoute();
                        vehicle.getRoute().add(i, nodeToInsert);
                        for (int j = firstGhostNodeIdx + 1; j < vehicle.getRoute().size() - 1; j++) {
                            vehicle.getRoute().get(j).setId(vehicle.getRoute().get(j).getId() + 1);
                        }
                        vehicle.getRoute().remove(vehicle.getRoute().size() - 1);
                        boolean valid = checkForValidity(data, vehicle);
                        if (valid) {
                            float currentValue = getDataValue(data, true);
                            if (currentValue < bestValue) {
                                bestValue = currentValue;
                                vehicleToInsertInto = vehicle;
                                indexToInsert = i;
                                foundVehicleForNodeToInsert = true;
                            }
                        }
                        vehicle.setRoute(copiedRoute);
                    }
                }
            }
            if(foundVehicleForNodeToInsert) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
            } else {
                Vehicle penaltyVehicle = data.getPenaltyVehicle();
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
                int firstGhostNodeIdx = penaltyVehicle.getFirstGhostNode().getId();
                for(int j = firstGhostNodeIdx + 1; j < penaltyVehicle.getRoute().size() - 1; j++) {
                    penaltyVehicle.getRoute().get(j).setId(penaltyVehicle.getRoute().get(j).getId() + 1);
                }
                penaltyVehicle.getRoute().remove(penaltyVehicle.getRoute().size() - 1);
            }
        }
    }

    private boolean checkForValidity(Data data, Vehicle vehicle) {
        List<Node> route = vehicle.getRoute();
        if(route.get(0).isGhostNode()) {
            return true;
        }
        int currentIdx = 0;
        float travelDistance;
        Node currentNode = route.get(currentIdx);
        float currentTime = currentNode.getTimeStart();
        vehicle.setCapacity(0);
        Node dumpingSite;
        while (!currentNode.isGhostNode()) {
            Node nextNode = route.get(currentIdx + 1);
            if(!nextNode.isGhostNode()) {
                if(vehicle.getCapacity() + nextNode.getQuantity() <= vehicle.getMaximumCapacity()) {
                    travelDistance = data.getDistanceBetweenNode(currentNode, nextNode);
                    if(!data.timeWindowCheckWithWaitingTime(currentNode, nextNode, currentTime, travelDistance)) {
                        return false;
                    }
                    vehicle.setCapacity(vehicle.getCapacity() + Math.round(nextNode.getQuantity()));
                    currentTime = Math.max(currentTime + currentNode.getServiceTime() + travelDistance, nextNode.getTimeStart());
                    currentNode = nextNode;
                } else {
                    dumpingSite = data.getNearestDumpingSiteNode(currentNode);
                    travelDistance = data.getDistanceBetweenNode(currentNode, dumpingSite);
                    vehicle.setCapacity(0);
                    currentTime += travelDistance + dumpingSite.getServiceTime();
                    currentNode = dumpingSite;

                    nextNode = route.get(currentIdx + 1);
                    travelDistance = data.getDistanceBetweenNode(currentNode, nextNode);
                    if(!data.timeWindowCheckWithWaitingTime(currentNode, nextNode, currentTime, travelDistance)) {
                        return false;
                    }
                    vehicle.setCapacity(vehicle.getCapacity() + Math.round(nextNode.getQuantity()));
                    currentTime = Math.max(currentTime + currentNode.getServiceTime() + travelDistance, nextNode.getTimeStart());
                    currentNode = nextNode;
                }
                currentIdx++;
            } else {
                break;
            }
        }
        return true;
    }

    private void destroyNodes(Data data, int p, List<Node> nodesToSwap) {
        worstRemove(data, p, nodesToSwap);
    }

    private void worstRemove(Data data, int p, List<Node> nodesToSwap) {
        float bestValue = getDataValue(data, false);
        int removed = 0;
        //Data data = new Data(data);
        Node nodeToRemove = null;
        while (removed < p) {
            for(Vehicle vehicle : data.getFleet()) {
                for(int i = 0; i < vehicle.getRoute().size(); i++) {
                    Node node = vehicle.getRoute().get(i);
                    if(!node.getGhostNode()) {
                        List<Node> copiedRoute = vehicle.copyRoute();
                        vehicle.removeNode(node);
                        float currentValue = getDataValue(data, false);
                        if(currentValue < bestValue) {
                            bestValue = currentValue;
                            nodeToRemove = node;
                        }
                        vehicle.setRoute(copiedRoute);
                    } else {
                        break;
                    }
                }
            }
            nodesToSwap.add(nodeToRemove);
            removed++;
            for(Node node : nodesToSwap) {
                for(Vehicle vehicle : data.getFleet()) {
                    for(Node routeNode : vehicle.getRoute()) {
                        if(!routeNode.getGhostNode()) {
                            if (node.getId() == routeNode.getId()) {
                                vehicle.removeNode(routeNode);
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean checkSolution(Data data) {
        for(Node node : data.getNodeList()) {
            if(data.customerNode(node) && (!node.isVisited() || !data.timeWindowCheck(node.getVisitedAt(), node))) {
                return false;
            }
        }
        return true;
    }

    private float getDataValue(Data data, boolean timeWindow) {
        float overallDistance = 0;
        for(Vehicle vehicle : data.getFleet()) {
            float distance = vehicle.calculateTravelDistance(data, timeWindow);
            overallDistance += distance;
        }
        return overallDistance;
    }
}
