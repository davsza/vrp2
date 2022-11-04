import data.*;

import java.util.*;
import java.util.stream.Collectors;

public class Solver {

    private List<Data> dataList;
    private String logPath;
    private HeuristicWeights heuristicWeights;
    private Random random;
    private List<String> hashes;

    public Solver(List<Data> dataList, String logPath) {
        this.dataList = dataList;
        this.logPath = logPath;
        this.heuristicWeights = new HeuristicWeights();
        this.random = new Random();
        this.hashes = new ArrayList<>();
    }

    public float initGreedy(Data data) {

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

        return basicInfo(false, data, true, "Greedy");
    }

    public float ALNS(Data data) {
        float T = 90;
        data.destroyInfo();
        Data bestData = new Data(data);
        float bestValue = getDataValue(bestData, false);
        Data currentData;
        float currentValue, newValue;
        List<Node> nodesToSwap;
        int iteration = 0;
        String hash;
        int score;
        while(T > 0.1) {
            score = 0;
            iteration++;
            currentData = new Data(data);
            currentValue = getDataValue(currentData, false);
            nodesToSwap = new ArrayList<>();
            destroyNodes(currentData, 2, nodesToSwap);
            repairNodes(currentData, nodesToSwap);
            newValue = getDataValue(currentData, false);
            float delta = currentValue - newValue;
            hash = currentData.dataToHash();
            if(delta > 0) {
                if(newValue >= bestValue) {
                    if(!hashes.contains(hash)) {
                        score = 2;
                        hashes.add(hash);
                    }
                }
                data = currentData;
            } else if (Math.exp(delta / T) > Math.random()) {
                if(!hashes.contains(hash)) {
                    score = 1;
                    hashes.add(hash);
                }
                data = currentData;
            }
            if(newValue < bestValue) {
                if(!hashes.contains(hash)) {
                    score = 5;
                    hashes.add(hash);
                };
                bestValue = newValue;
                bestData = new Data(data);
            }
            updateHeuristicInformation(heuristicWeights, score);
            //update information about performance of destroy and repair methods
            if(iteration % 100 == 0) {
                updateWeights(heuristicWeights, (float)0.5); //TODO: what is 'r'?
            }
            T *= 0.999;
        }

        return basicInfo(false, bestData, false, "ALNS");
    }

    private void updateWeights(HeuristicWeights heuristicWeights, float r) {
        float newRandomRemoveWeight = heuristicWeights.getRandomRemoveWeight() * (1 - r)
                + r * (heuristicWeights.getRandomRemoveScore() / (float)heuristicWeights.getTimesUsedRandomRemove());
        float newWorstRemoveWeight = heuristicWeights.getWorstRemoveWeight() * (1 - r)
                + r * (heuristicWeights.getWorstRemoveScore() / (float)heuristicWeights.getTimesUsedWorstRemove());
        float newRelatedRemoveWeight = heuristicWeights.getRelatedRemoveWeight() * (1 - r)
                + r * (heuristicWeights.getRelatedRemoveScore() / (float)heuristicWeights.getTimesUsedRelatedRemove());
        float newGreedyInsertWeight = heuristicWeights.getGreedyInsertWeight() * (1 - r)
                + r * (heuristicWeights.getGreedyInsertScore() / (float)heuristicWeights.getTimesUsedGreedyInsert());
        float newRegretInsertWeight = heuristicWeights.getRegretInsertWeight() * (1 - r)
                + r * (heuristicWeights.getRegretInsertScore() / (float)heuristicWeights.getTimesUsedRegretInsert());
        heuristicWeights.setRandomRemoveWeight(newRandomRemoveWeight);
        heuristicWeights.setWorstRemoveWeight(newWorstRemoveWeight);
        heuristicWeights.setRelatedRemoveWeight(newRelatedRemoveWeight);
        heuristicWeights.setGreedyInsertWeight(newGreedyInsertWeight);
        heuristicWeights.setRegretInsertWeight(newRegretInsertWeight);
    }

    private void updateHeuristicInformation(HeuristicWeights heuristicWeights, int score) {
        int destroyHeuristic = heuristicWeights.getCurrentRemove();
        int repairHeuristic = heuristicWeights.getCurrentInsert();

        switch (destroyHeuristic){
            case 1:
                heuristicWeights.setRandomRemoveScore(heuristicWeights.getRandomRemoveScore() + score);
                heuristicWeights.setTimesUsedRandomRemove(heuristicWeights.getTimesUsedRandomRemove() + 1);
            case 2:
                heuristicWeights.setWorstRemoveScore(heuristicWeights.getWorstRemoveScore() + score);
                heuristicWeights.setTimesUsedWorstRemove(heuristicWeights.getTimesUsedWorstRemove() + 1);
            case 3:
                heuristicWeights.setRelatedRemoveScore(heuristicWeights.getRelatedRemoveScore() + score);
                heuristicWeights.setTimesUsedRelatedRemove(heuristicWeights.getTimesUsedRelatedRemove() + 1);
            default:
                break;
        }

        switch (repairHeuristic) {
            case 1:
                heuristicWeights.setGreedyInsertScore(heuristicWeights.getGreedyInsertScore() + score);
                heuristicWeights.setTimesUsedGreedyInsert(heuristicWeights.getTimesUsedGreedyInsert() + 1);
            case 2:
                heuristicWeights.setRegretInsertScore(heuristicWeights.getRegretInsertScore() + score);
                heuristicWeights.setTimesUsedRegretInsert(heuristicWeights.getTimesUsedRegretInsert() + 1);
            default:
                break;
        }

    }

    private void repairNodes(Data data, List<Node> nodesToSwap) {
        greedyInsert(data, nodesToSwap);
        //regretInsert(data, nodesToSwap, 3);
        /*
        float sumOf = heuristicWeights.sumOfRepair();
        float greedyWeight = heuristicWeights.getGreedyInsertWeight() / sumOf;
        float regretWeight = heuristicWeights.getRegretInsertWeight() / sumOf;
        double randomValue = random.nextDouble();

        if(randomValue < greedyWeight) {
            heuristicWeights.setCurrentInsert(1);
            greedyInsert(data, nodesToSwap);
        } else if (randomValue < greedyWeight + regretWeight) {
            heuristicWeights.setCurrentRemove(2);
            regretInsert(data, nodesToSwap);
        }

         */

    }

    private void regretInsert(Data data, List<Node> nodesToSwap, int p) {
        Vehicle vehicleToInsertInto;
        Node nodeToInsert;
        int indexToInsert;
        while(nodesToSwap.size() > 0) {
            float initialValue = getDataValue(data, false);
            List<NodeInsertion> nodeInsertionList = new ArrayList<>();
            for(Node nodesToInsert : nodesToSwap) {
                float worstDiff = 0;
                NodeInsertion nodeInsertion = new NodeInsertion(nodesToInsert);
                for(Vehicle vehicle : data.getFleet()) {
                    if (!vehicle.isPenaltyVehicle()) {
                        int firstGhostNodeIdx = vehicle.getFirstGhostNode().getId();
                        for (int i = 0; i < firstGhostNodeIdx + 1; i++) {
                            List<Node> copiedRoute = vehicle.copyRoute();
                            vehicle.getRoute().add(i, nodesToInsert);
                            for (int j = firstGhostNodeIdx + 1; j < vehicle.getRoute().size() - 1; j++) {
                                vehicle.getRoute().get(j).setId(vehicle.getRoute().get(j).getId() + 1);
                            }
                            vehicle.getRoute().remove(vehicle.getRoute().size() - 1);
                            boolean valid = checkForValidity(data, vehicle);
                            if (valid) {
                                float currentValue = getDataValue(data, false);
                                nodeInsertion.getValues().add(currentValue);
                                nodeInsertion.getVehicleSet().add(vehicle);
                                float diff = currentValue - initialValue;
                                if(diff > worstDiff) {
                                    worstDiff = diff;
                                    nodeInsertion.setVehicle(vehicle);
                                    nodeInsertion.setIndex(i);
                                    nodeInsertion.setFoundVehicleForNodeToInsert(true);
                                }
                            }
                            vehicle.setRoute(copiedRoute);
                        }
                    }
                }
                nodeInsertionList.add(nodeInsertion);
            }
            Collections.sort(nodeInsertionList, new Comparator<NodeInsertion>() {
                @Override
                public int compare(NodeInsertion o1, NodeInsertion o2) {
                    return (Float.compare(o1.getVehicleSet().size(), o2.getVehicleSet().size()));
                }
            });
            int leastFeasibleVehicleInsert = nodeInsertionList.get(0).getVehicleSet().size();

            NodeInsertion bestInsertion = new NodeInsertion();

            if(leastFeasibleVehicleInsert < p) {
                List<NodeInsertion> feasibleNodeInsertions = nodeInsertionList
                        .stream()
                        .filter(e -> e.getVehicleSet().size() == leastFeasibleVehicleInsert)
                        .collect(Collectors.toList());
                float worst = 0;
                for(NodeInsertion nodeInsertion : feasibleNodeInsertions) {
                    Collections.sort(nodeInsertion.getValues(), new Comparator<Float>() {
                        @Override
                        public int compare(Float o1, Float o2) {
                            return (Float.compare(o1, o2));
                        }
                    });
                    float bestValue = nodeInsertion.getValues().get(0);
                    float worstValue = nodeInsertion.getValues().get(nodeInsertion.getValues().size() - 1);
                    float diff = worstValue - bestValue;
                    if(diff > worst) {
                        worst = diff;
                        bestInsertion = nodeInsertion;
                    }
                }
            } else {
                float worst = 0;
                for(NodeInsertion nodeInsertion : nodeInsertionList) {
                    Collections.sort(nodeInsertion.getValues(), new Comparator<Float>() {
                        @Override
                        public int compare(Float o1, Float o2) {
                            return (Float.compare(o1, o2));
                        }
                    });
                    float bestValue = nodeInsertion.getValues().get(0);
                    float worstValue = nodeInsertion.getValues().get(p - 1);
                    float diff = worstValue - bestValue;
                    if(diff > worst) {
                        worst = diff;
                        bestInsertion = nodeInsertion;
                    }
                }
            }

            vehicleToInsertInto = bestInsertion.getVehicle();
            nodeToInsert = bestInsertion.getNode();
            indexToInsert = bestInsertion.getIndex();
            if(bestInsertion.isFoundVehicleForNodeToInsert()) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
                int firstGhostNodeIdx = vehicleToInsertInto.getFirstGhostNode().getId();
                for(int j = firstGhostNodeIdx + 1; j < vehicleToInsertInto.getRoute().size() - 1; j++) {
                    vehicleToInsertInto.getRoute().get(j).setId(vehicleToInsertInto.getRoute().get(j).getId() + 1);
                }
                vehicleToInsertInto.getRoute().remove(vehicleToInsertInto.getRoute().size() - 1);
            } else {
                Vehicle penaltyVehicle = data.getPenaltyVehicle();
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
                int firstGhostNodeIdx = penaltyVehicle.getFirstGhostNode().getId();
                for(int j = firstGhostNodeIdx + 1; j < penaltyVehicle.getRoute().size() - 1; j++) {
                    penaltyVehicle.getRoute().get(j).setId(penaltyVehicle.getRoute().get(j).getId() + 1);
                }
                penaltyVehicle.getRoute().remove(penaltyVehicle.getRoute().size() - 1);
            }
            nodesToSwap.remove(nodeToInsert);
        }
    }

    private void greedyInsert(Data data, List<Node> nodesToSwap) {

        float bestDiff;
        Vehicle vehicleToInsertInto = null;
        int indexToInsert = 0;
        boolean foundVehicleForNodeToInsert = false;
        List<NodeInsertion> nodeInsertionList;
        NodeInsertion nodeInsertion;
        Node nodeToInsert;
        float initialValue = getDataValue(data, false);
        while(nodesToSwap.size() > 0) {
            nodeInsertionList = new ArrayList<>();
            for(Node nodesToInsert : nodesToSwap) {
                bestDiff = Float.MAX_VALUE;
                for(Vehicle vehicle : data.getFleet()) {
                    if (!vehicle.isPenaltyVehicle()) {
                        int firstGhostNodeIdx = vehicle.getFirstGhostNode().getId();
                        for (int i = 0; i < firstGhostNodeIdx + 1; i++) {
                            List<Node> copiedRoute = vehicle.copyRoute();
                            vehicle.getRoute().add(i, nodesToInsert);
                            for (int j = firstGhostNodeIdx + 1; j < vehicle.getRoute().size() - 1; j++) {
                                vehicle.getRoute().get(j).setId(vehicle.getRoute().get(j).getId() + 1);
                            }
                            vehicle.getRoute().remove(vehicle.getRoute().size() - 1);
                            boolean valid = checkForValidity(data, vehicle);
                            if (valid) {
                                float currentValue = getDataValue(data, false);
                                float diff = currentValue - initialValue;
                                if (diff <= bestDiff) {
                                    bestDiff = diff;
                                    vehicleToInsertInto = vehicle;
                                    indexToInsert = i;
                                    foundVehicleForNodeToInsert = true;
                                }
                            }
                            vehicle.setRoute(copiedRoute);
                        }
                    }
                }
                nodeInsertion = new NodeInsertion(nodesToInsert, vehicleToInsertInto, bestDiff, indexToInsert, foundVehicleForNodeToInsert);
                nodeInsertionList.add(nodeInsertion);
            }

            Collections.sort(nodeInsertionList, new Comparator<NodeInsertion>() {
                @Override
                public int compare(NodeInsertion o1, NodeInsertion o2) {
                    return (Float.compare(o1.getValue(), o2.getValue()));
                }
            });

            NodeInsertion bestInsertion = nodeInsertionList.get(0);
            vehicleToInsertInto = bestInsertion.getVehicle();
            nodeToInsert = bestInsertion.getNode();
            indexToInsert = bestInsertion.getIndex();
            nodeInsertionList.remove(0);
            if(bestInsertion.isFoundVehicleForNodeToInsert()) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
                int firstGhostNodeIdx = vehicleToInsertInto.getFirstGhostNode().getId();
                for(int j = firstGhostNodeIdx + 1; j < vehicleToInsertInto.getRoute().size() - 1; j++) {
                    vehicleToInsertInto.getRoute().get(j).setId(vehicleToInsertInto.getRoute().get(j).getId() + 1);
                }
                vehicleToInsertInto.getRoute().remove(vehicleToInsertInto.getRoute().size() - 1);
            } else {
                Vehicle penaltyVehicle = data.getPenaltyVehicle();
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
                int firstGhostNodeIdx = penaltyVehicle.getFirstGhostNode().getId();
                for(int j = firstGhostNodeIdx + 1; j < penaltyVehicle.getRoute().size() - 1; j++) {
                    penaltyVehicle.getRoute().get(j).setId(penaltyVehicle.getRoute().get(j).getId() + 1);
                }
                penaltyVehicle.getRoute().remove(penaltyVehicle.getRoute().size() - 1);
            }
            nodesToSwap.remove(nodeToInsert);
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
        //relatedRemove(data, p, nodesToSwap, (float)0.3, (float)0.3, (float)0.3);
        /*
        float sumOf = heuristicWeights.sumOfDestroy();
        float worstWeight = heuristicWeights.getWorstRemoveWeight() / sumOf;
        float randomWeight = heuristicWeights.getRandomRemoveWeight() / sumOf;
        float relatedWeight = heuristicWeights.getRelatedRemoveWeight() / sumOf;
        double randomValue = random.nextDouble();

        if(randomValue < worstWeight) {
            heuristicWeights.setCurrentRemove(1);
            worstRemove(data, p, nodesToSwap);
        } else if(randomValue < worstWeight + randomWeight) {
            heuristicWeights.setCurrentRemove(2);
            randomRemoval(data, p, nodesToSwap);
        } else if(randomValue < worstWeight + randomWeight + relatedWeight){
            heuristicWeights.setCurrentRemove(3);
            relatedRemove(data, p, nodesToSwap);
        }

         */

    }

    private void relatedRemove(Data data, int p, List<Node> nodesToSwap,
                               float phi, float chi, float psi) {
        randomRemoval(data, 1, nodesToSwap);
        while(nodesToSwap.size() < p) {
            int idx = nodesToSwap.size() == 0 ? 0 : random.nextInt(nodesToSwap.size());
            Node nodeToCompare = nodesToSwap.get(idx);
            List<Float> relatednessValues = new ArrayList<>();
            float bestRelatedness = Float.MAX_VALUE;
            Node nodeToRemove = null;
            Vehicle vehicleToRemoveFrom = null;
            for(Vehicle vehicle : data.getFleet()) {
                for(Node node : vehicle.getRoute()) {
                    if(!node.isGhostNode()) {
                        float relatedness = phi * data.getDistanceBetweenNode(nodeToCompare, node)
                                //+ chi * Math.abs(data.getTimeNodeVisitedAt(nodeToCompare) - data.getTimeNodeVisitedAt(node))
                                + psi * Math.abs(nodeToCompare.getQuantity() - node.getQuantity());
                        if(relatedness < bestRelatedness) {
                            bestRelatedness = relatedness;
                            nodeToRemove = node;
                            vehicleToRemoveFrom = vehicle;
                        }
                    }
                }
            }
            vehicleToRemoveFrom.removeNode(nodeToRemove);
            nodesToSwap.add(nodeToRemove);
        }
    }

    private void randomRemoval(Data data, int p, List<Node> nodesToSwap) {
        int nodeIdMax = data.getNodeListSize() - 1;
        List<Integer> dumpingSites = data.getDumpingSites();
        Set<Integer> indexesToRemove = new HashSet<>();
        while (indexesToRemove.size() < p) {
            int next = random.nextInt(nodeIdMax) + 1;
            if(!dumpingSites.contains(next)) {
                indexesToRemove.add(next);
            }
        }
        for(Vehicle vehicle : data.getFleet()) {
            for(int i = 0; i < vehicle.getRoute().size(); i++) {
                Node node = vehicle.getRoute().get(i);
                if(!node.isGhostNode() && indexesToRemove.contains(node.getId())) {
                    nodesToSwap.add(node);
                    vehicle.removeNode(node);
                    i--;
                }
            }
        }
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

    private float basicInfo(boolean print, Data bestData, boolean timeWindow, String name){
        if(print) {
            System.out.println("---------------------");
            System.out.println(name);
            for(Vehicle vehicle : bestData.getFleet()) {
                if(!vehicle.getRoute().get(0).isGhostNode()) {
                    System.out.print("Vehicle " + vehicle.getId() + ": ");
                    for(Node node : vehicle.getRoute()) {
                        if(!node.isGhostNode()) {
                            System.out.print(node.getId() + " ");
                        }
                    }
                    System.out.println(", distance: " + vehicle.calculateTravelDistance(bestData, timeWindow));
                    System.out.print(", valid: " + checkForValidity(bestData, vehicle));
                    System.out.println();
                }
            }
            float value = getDataValue(bestData, timeWindow);
            System.out.println();
            System.out.println("Total distance: " + value);
            System.out.println("---------------------");
            return value;
        }
        return getDataValue(bestData, timeWindow);
    }
}
