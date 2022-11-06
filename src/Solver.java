import data.*;

import java.util.*;
import java.util.stream.Collectors;

public class Solver {

    private List<Data> dataList;
    private String logPath;
    private HeuristicWeights heuristicWeights;
    private Random random;
    private List<String> hashes;
    private final Constants CONSTANTS;

    public Solver(List<Data> dataList, String logPath) {
        this.dataList = dataList;
        this.logPath = logPath;
        this.heuristicWeights = new HeuristicWeights();
        this.random = new Random();
        this.hashes = new ArrayList<>();
        this.CONSTANTS = new Constants();
    }

    public float initGreedyKim(Data data) {
        //init vehicles
        for(Vehicle vehicle : data.getFleet()) vehicle.initVehicleKim();
        Vehicle penaltyVehicle = new Vehicle();
        penaltyVehicle.initVehicleKim();
        penaltyVehicle.setPenaltyVehicle(true);
        data.getFleet().add(penaltyVehicle);

        //set the current node to the depot, set the current vehicle, and it's first node to the depot node
        Node currentNode = data.getDepotNode();
        Node nextNode;
        Vehicle currentVehicle = data.getFleet().get(0);
        float currentTime;
        float travelTime;
        float serviceTime;
        float quantity;
        Node dumpingSite;
        currentVehicle.getRoute().add(currentNode);
        currentVehicle.setCurrentTime((float)currentNode.getTimeStart());

        while(data.hasMoreUnvisitedNodes()){
            nextNode = data.findNextNodeKim(currentVehicle, currentNode);
            if(!nextNode.isNullNode()) {
                currentTime = currentVehicle.getCurrentTime();
                travelTime = data.getDistanceBetweenNode(currentNode, nextNode);
                serviceTime = nextNode.getServiceTime();
                quantity = nextNode.getQuantity();
                currentVehicle.setCurrentTime(Math.max(currentTime + serviceTime + travelTime, nextNode.getTimeStart()));
                currentVehicle.setCapacity(currentVehicle.getCapacity() + quantity);

                currentNode = nextNode;
                currentNode.setVisitedAt(currentVehicle.getCurrentTime() - currentNode.getServiceTime());
                currentNode.setVisited(true);
                currentVehicle.getRoute().add(currentNode);
            } else {
                if(currentNode.isDumpingSite()) {
                    Node depot = data.getDepotNode();
                    currentTime = currentVehicle.getCurrentTime();
                    travelTime = data.getDistanceBetweenNode(currentNode, depot);
                    currentVehicle.setCurrentTime(currentTime + travelTime);

                    currentNode = depot;
                    currentVehicle.getRoute().add(currentNode);
                    currentVehicle = data.getFleet().get(currentVehicle.getId() + 1);

                    currentVehicle.getRoute().add(data.getDepotNode());
                    currentVehicle.setCurrentTime((float)data.getDepotNode().getTimeStart());
                    continue;
                }
                dumpingSite = data.getNearestDumpingSiteNode(currentVehicle, currentNode);
                currentTime = currentVehicle.getCurrentTime();
                travelTime = data.getDistanceBetweenNode(currentNode, dumpingSite);
                serviceTime = dumpingSite.getServiceTime();
                currentVehicle.setCurrentTime(Math.max(currentTime + serviceTime + travelTime, dumpingSite.getTimeStart()));
                currentVehicle.setCapacity((float)0);

                currentNode = dumpingSite;
                currentVehicle.getRoute().add(currentNode);
            }
        }

        dumpingSite = data.getNearestDumpingSiteNode(currentVehicle, currentNode);
        currentVehicle.getRoute().add(dumpingSite);
        currentVehicle.getRoute().add(data.getDepotNode());

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() == 0).collect(Collectors.toList())) {
            vehicle.getRoute().add(data.getDepotNode());
            vehicle.getRoute().add(data.getNearestDumpingSiteNode(vehicle, data.getDepotNode()));
            vehicle.getRoute().add(data.getDepotNode());
        }

        return 0;
    }

    public Data ALNSkim(Data data) {
        data.destroyInfoKim();
        Data bestData = new Data(data);
        Data currentData;
        float T = calculateInitialTemperature(data, CONSTANTS.getW()), bestValue = getDataValueKim(bestData), currentValue, newValue, delta;
        List<Node> nodesToSwap;
        int numberOfSteps = 1, score = 0, numberOfNodesToSwap, upperLimit;
        String hashCode;
        System.out.println("Best value before ALNS: " + bestValue);
        while (numberOfSteps < 25000) {
            System.out.println(numberOfSteps);
            currentData = new Data(data);
            currentValue = getDataValueKim(currentData);
            nodesToSwap = new ArrayList<>();
            upperLimit = Math.min(100, (int) currentData.getNodeList().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count());
            numberOfNodesToSwap = random.nextInt(upperLimit - 4) + 4;
            System.out.println(numberOfNodesToSwap);
            long asd = System.currentTimeMillis();
            System.out.println("torles kezdodott");
            destroyNodesKim(currentData, numberOfNodesToSwap, nodesToSwap);
            System.out.println("torles vege");

            long asd2 = System.currentTimeMillis();
            System.out.println("repair kezdodott");

            repairNodesKim(currentData, nodesToSwap);
            System.out.println("repair vege");

            long asd3 = System.currentTimeMillis();
            System.out.println("destry: " + (asd2-asd));
            System.out.println("repair: " + (asd3-asd2));
            newValue = getDataValueKim(currentData);
            delta = newValue - currentValue;
            hashCode = currentData.dataToHash();

            if(delta < 0) {

                if(newValue >= bestValue) {
                    if(!hashes.contains(hashCode)) {
                        score = CONSTANTS.getSIGMA_2();
                        hashes.add(hashCode);
                    }
                }

                data = currentData;

            } else if (Math.exp(-1 * (delta) / T) > Math.random()) {

                if(!hashes.contains(hashCode)) {
                    score = CONSTANTS.getSIGMA_3();
                    hashes.add(hashCode);
                }

                data = currentData;

            }

            if(newValue < bestValue) {

                if(!hashes.contains(hashCode)) {
                    score = CONSTANTS.getSIGMA_1();
                    hashes.add(hashCode);
                }

                bestValue = newValue;
                bestData = new Data(currentData);

            }

            updateHeuristicInformation(heuristicWeights, score);
            //update information about performance of destroy and repair methods
            if(numberOfSteps % 100 == 0) {
                updateWeights(heuristicWeights, CONSTANTS.getR());
            }
            numberOfSteps++;
            T *= 0.995;
        }

        // TODO: do something with the best found solution (bestData)
        System.out.println("Best value after ALNS: " + bestValue);

        return bestData;
    }

    private float calculateInitialTemperature(Data data, float W) {
        float initialValue = getDataValueKim(data);
        return (float) (-1 * (W * initialValue) / Math.log(0.5));
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
        heuristicWeights.setRandomRemoveScore(0);
        heuristicWeights.setWorstRemoveScore(0);
        heuristicWeights.setRelatedRemoveScore(0);
        heuristicWeights.setGreedyInsertScore(0);
        heuristicWeights.setRegretInsertScore(0);
    }

    private void updateHeuristicInformation(HeuristicWeights heuristicWeights, int score) {
        int destroyHeuristic = heuristicWeights.getCurrentRemove();
        int repairHeuristic = heuristicWeights.getCurrentInsert();

        switch (destroyHeuristic){
            case 1:
                heuristicWeights.setWorstRemoveScore(heuristicWeights.getWorstRemoveScore() + score);
                heuristicWeights.setTimesUsedWorstRemove(heuristicWeights.getTimesUsedWorstRemove() + 1);
                break;
            case 2:
                heuristicWeights.setRandomRemoveScore(heuristicWeights.getRandomRemoveScore() + score);
                heuristicWeights.setTimesUsedRandomRemove(heuristicWeights.getTimesUsedRandomRemove() + 1);
                break;
            case 3:
                heuristicWeights.setRelatedRemoveScore(heuristicWeights.getRelatedRemoveScore() + score);
                heuristicWeights.setTimesUsedRelatedRemove(heuristicWeights.getTimesUsedRelatedRemove() + 1);
                break;
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

    private void repairNodesKim(Data data, List<Node> nodesToSwap) {
        // TODO: regret 2,3,k different

        float sumOf = heuristicWeights.sumOfRepair();
        float greedyWeight = heuristicWeights.getGreedyInsertWeight() / sumOf;
        float regretWeight = heuristicWeights.getRegretInsertWeight() / sumOf;
        double randomValue = random.nextDouble();
        if(randomValue < greedyWeight) {
            heuristicWeights.setCurrentInsert(1);
            greedyInsertKim(data, nodesToSwap);
        } else if (randomValue < greedyWeight + regretWeight) {
            heuristicWeights.setCurrentInsert(2);
            regretInsertKim(data, nodesToSwap, 3);
        }

    }

    private void regretInsertKim(Data data, List<Node> nodesToSwap, int p) {
        List<NodeSwap> nodeSwapList;
        float bestDiff, currentValue, diff, initialValue;
        Vehicle vehicleToInsertInto = null, penaltyVehicle = data.getPenaltyVehicle();
        int indexToInsert = 0;
        boolean foundVehicleForInsert = false;
        NodeSwap currentNodeSwap;
        Node nodeToInsert;
        while(nodesToSwap.size() > 0) {
            initialValue = getDataValueKim(data);
            nodeSwapList = new ArrayList<>();
            for(Node nodesToInsert : nodesToSwap) {
                bestDiff = Float.MAX_VALUE;
                currentNodeSwap = new NodeSwap(nodesToInsert);
                for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
                    for (int i = 1; i < vehicle.getRoute().size() - 1; i++) {
                        List<Node> copiedRoute = vehicle.copyRoute();
                        vehicle.getRoute().add(i, nodesToInsert);
                        boolean valid = checkForValidityKim(data, vehicle);
                        if (valid) {
                            currentValue = getDataValueKim(data);
                            currentNodeSwap.getValues().add(currentValue);
                            currentNodeSwap.getVehicleSet().add(vehicle);
                            diff = currentValue - initialValue;
                            if(diff < bestDiff) {
                                bestDiff = diff;
                                currentNodeSwap.setVehicle(vehicle);
                                currentNodeSwap.setIndex(i);
                                currentNodeSwap.setFoundVehicleForNodeToInsert(true);
                            }
                        }
                        vehicle.setRoute(copiedRoute);
                    }
                }
                nodeSwapList.add(currentNodeSwap);
            }
            nodeSwapList.sort(new Comparator<NodeSwap>() {
                @Override
                public int compare(NodeSwap o1, NodeSwap o2) {
                    return (Float.compare(o1.getVehicleSet().size(), o2.getVehicleSet().size()));
                }
            });
            int leastFeasibleVehicleInsert = nodeSwapList.get(0).getVehicleSet().size();

            NodeSwap bestNodeSwap = new NodeSwap();

            if(leastFeasibleVehicleInsert < p) {
                List<NodeSwap> feasibleNodeSwaps = nodeSwapList
                        .stream()
                        .filter(e -> e.getVehicleSet().size() == leastFeasibleVehicleInsert)
                        .collect(Collectors.toList());
                float worst = 0;
                float bestDataValue = Float.MAX_VALUE;
                for(NodeSwap nodeSwap : feasibleNodeSwaps) {
                    nodeSwap.getValues().sort(new Comparator<Float>() {
                        @Override
                        public int compare(Float o1, Float o2) {
                            return (Float.compare(o1, o2));
                        }
                    });
                    float bestValue = nodeSwap.getValues().get(0);
                    float worstValue = nodeSwap.getValues().get(nodeSwap.getValues().size() - 1);
                    diff = worstValue - bestValue;
                    if(diff > worst) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = nodeSwap.getValue();
                    } else if (diff == worst &&  nodeSwap.getValues().get(0) < bestDataValue) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = nodeSwap.getValue();
                    }
                }
            } else {
                float worst = 0;
                float bestDataValue = Float.MAX_VALUE;
                for(NodeSwap nodeSwap : nodeSwapList) {
                    nodeSwap.getValues().sort(new Comparator<Float>() {
                        @Override
                        public int compare(Float o1, Float o2) {
                            return (Float.compare(o1, o2));
                        }
                    });
                    float bestValue = nodeSwap.getValues().get(0);
                    float worstValue = nodeSwap.getValues().get(p - 1);
                    diff = worstValue - bestValue;
                    if(diff > worst) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = nodeSwap.getValue();
                    } else if (diff == worst && nodeSwap.getValue() < bestDataValue) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = nodeSwap.getValue();
                    }
                }
            }

            vehicleToInsertInto = bestNodeSwap.getVehicle();
            nodeToInsert = bestNodeSwap.getNode();
            indexToInsert = bestNodeSwap.getIndex();

            if(bestNodeSwap.isFoundVehicleForNodeToInsert()) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
            } else {
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
            }
            nodesToSwap.remove(nodeToInsert);
        }
    }

    private void greedyInsertKim(Data data, List<Node> nodesToSwap) {
        List<NodeSwap> nodeSwapList;
        float bestDiff, currentValue, diff, initialValue;
        Vehicle vehicleToInsertInto = null, penaltyVehicle = data.getPenaltyVehicle();
        int indexToInsert = 0;
        boolean foundVehicleForInsert = false;
        NodeSwap currentNodeSwap;
        Node nodeToInsert;
        while(nodesToSwap.size() > 0) {
            nodeSwapList = new ArrayList<>();
            initialValue = getDataValueKim(data);
            for(Node nodesToInsert : nodesToSwap) {
                bestDiff = Float.MAX_VALUE;
                for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 0 && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
                    for(int i = 1; i < vehicle.getRoute().size() - 1; i++) {
                        List<Node> copiedRoute = vehicle.copyRoute();
                        vehicle.getRoute().add(i, nodesToInsert);
                        boolean validSolution = checkForValidityKim(data, vehicle);
                        if(validSolution) {
                            currentValue = getDataValueKim(data);
                            diff = currentValue - initialValue;
                            if(diff < bestDiff) {
                                bestDiff = diff;
                                vehicleToInsertInto = vehicle;
                                indexToInsert = i;
                                foundVehicleForInsert = true;
                            }
                        }
                        vehicle.setRoute(copiedRoute);
                    }
                }
                currentNodeSwap = new NodeSwap(nodesToInsert, vehicleToInsertInto, bestDiff, indexToInsert, foundVehicleForInsert);
                nodeSwapList.add(currentNodeSwap);
            }
            nodeSwapList.sort(new Comparator<NodeSwap>() {
                @Override
                public int compare(NodeSwap o1, NodeSwap o2) {
                    return (Float.compare(o1.getValue(), o2.getValue()));
                }
            });

            NodeSwap bestNodeSwap = nodeSwapList.get(0);
            vehicleToInsertInto = bestNodeSwap.getVehicle();
            nodeToInsert = bestNodeSwap.getNode();
            indexToInsert = bestNodeSwap.getIndex();

            if(bestNodeSwap.isFoundVehicleForNodeToInsert()) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
            } else {
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
            }
            nodesToSwap.remove(nodeToInsert);
        }
    }

    private boolean checkForValidityKim(Data data, Vehicle vehicle) {
        List<Node> route = vehicle.getRoute();

        if(route.size() > vehicle.getMaximumNumberOfStopsToVisit()) {
            return false;
        }

        Node currentNode = route.get(0), previousNode;
        float currentTime = currentNode.getTimeStart(), quantity;
        vehicle.setCurrentTime(currentTime);
        vehicle.setCapacity((float)0);
        previousNode = currentNode;
        for(int i = 1; i < route.size(); i++) {
            currentNode = route.get(i);
            if(currentNode.isDumpingSite()) {
                vehicle.setCapacity((float)0);
            } else if(!currentNode.isDepot()) {
                quantity = currentNode.getQuantity();
                vehicle.setCapacity(vehicle.getCapacity() + quantity);
            }

            if(vehicle.getCapacity() > vehicle.getMaximumCapacity()) {
                return false;
            }

            float serviceTimeAtPreviousNode = previousNode.getServiceTime();
            float travelDistance = data.getDistanceBetweenNode(previousNode, currentNode);

            if(!data.timeWindowCheck(currentTime + serviceTimeAtPreviousNode + travelDistance, currentNode)) {
                return false;
            }
            currentTime = Math.max(currentTime + previousNode.getServiceTime() + travelDistance, currentNode.getTimeStart());
            vehicle.setCurrentTime(currentTime);
            previousNode = currentNode;
        }
        return true;
    }

    private void destroyNodesKim(Data data, int p, List<Node> nodesToSwap) {

        // TODO: check removal method runtimes

        float sumOf = heuristicWeights.sumOfDestroy();
        float worstWeight = heuristicWeights.getWorstRemoveWeight() / sumOf;
        float randomWeight = heuristicWeights.getRandomRemoveWeight() / sumOf;
        float relatedWeight = heuristicWeights.getRelatedRemoveWeight() / sumOf;
        double randomValue = random.nextDouble();

        if(randomValue < worstWeight) {
            heuristicWeights.setCurrentRemove(1);
            worstRemovalKim(data, p, nodesToSwap, CONSTANTS.getP_WORST());
        } else if(randomValue < worstWeight + randomWeight) {
            heuristicWeights.setCurrentRemove(2);
            randomRemovalKim(data, p, nodesToSwap);
        } else if(randomValue < worstWeight + randomWeight + relatedWeight){
            heuristicWeights.setCurrentRemove(3);
            relatedRemovalKim(data, p, nodesToSwap, CONSTANTS.getPHI(), CONSTANTS.getCHI(), CONSTANTS.getPSI(), CONSTANTS.getP());
        }

    }


    private void relatedRemovalKim(Data data, int p, List<Node> nodesToSwap,
                                   float phi, float chi, float psi, int P) {
        randomRemovalKim(data, 1, nodesToSwap);
        int randomIndex;
        List<NodeSwap> nodeSwapList;
        NodeSwap bestNodeSwap, currentNodeSwap;
        while(nodesToSwap.size() < p) {
            nodeSwapList = new ArrayList<>();
            randomIndex = nodesToSwap.size() == 0 ? 0 : random.nextInt(nodesToSwap.size());
            Node nodeToCompare = nodesToSwap.get(randomIndex);
            data.calculateVisitingTime();
            // TODO:
            for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 0).collect(Collectors.toList())) {
                for(Node node : vehicle.getRoute().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).collect(Collectors.toList())) {
                    currentNodeSwap = new NodeSwap();
                    float relatedness = phi * data.getDistanceBetweenNode(nodeToCompare, node)
                            + chi * Math.abs(nodeToCompare.getVisitedAt() - node.getVisitedAt())
                            + psi * Math.abs(nodeToCompare.getQuantity() - node.getQuantity());
                    currentNodeSwap.setNode(node);
                    currentNodeSwap.setValue(relatedness);
                    currentNodeSwap.setVehicle(vehicle);

                    nodeSwapList.add(currentNodeSwap);
                }
            }

            nodeSwapList.sort(new Comparator<NodeSwap>() {
                @Override
                public int compare(NodeSwap o1, NodeSwap o2) {
                    return (Float.compare(o1.getValue(), o2.getValue()));
                }
            });

            double y = random.nextDouble();
            int index = (int)(Math.pow(y, P) * nodeSwapList.size());

            bestNodeSwap = nodeSwapList.get(index);
            nodesToSwap.add(bestNodeSwap.getNode());
            bestNodeSwap.getVehicle().getRoute().remove(bestNodeSwap.getNode());
        }
    }

    private void randomRemovalKim(Data data, int p, List<Node> nodesToSwap) {
        List<Node> feasibleNodesToRemove;
        int numberOfFeasibleNodesToRemove, index;
        boolean found = false;
        while (nodesToSwap.size() < p) {
            feasibleNodesToRemove = data.getNodeList().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).collect(Collectors.toList());
            numberOfFeasibleNodesToRemove = feasibleNodesToRemove.size();
            index = random.nextInt(numberOfFeasibleNodesToRemove);
            Node nodeToRemove = feasibleNodesToRemove.get(index);
            for(Vehicle vehicle : data.getFleet()) {
                found = false;
                for(int i = 0; i < vehicle.getRoute().size(); i++) {
                    Node node = vehicle.getRoute().get(i);
                    if(node.getId() == nodeToRemove.getId()) {
                        nodesToSwap.add(node);
                        vehicle.getRoute().remove(node);
                        found = true;
                        break;
                    }
                }
                if(found) break;
            }
        }
    }

    private void worstRemovalKim(Data data, int p, List<Node> nodesToSwap, int p_worst) {
        float currentValue;
        NodeSwap bestNodeSwap;
        List<NodeSwap> nodeSwapList;
        while (nodesToSwap.size() < p) {
            nodeSwapList = new ArrayList<>();
            // TODO: hüyleésg a szűrés
            for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 0).collect(Collectors.toList())) {
                for(int i = 0; i < vehicle.getRoute().size(); i++) {
                    Node node = vehicle.getRoute().get(i);
                    NodeSwap currentNodeSwap = new NodeSwap();
                    if(!node.isDepot() && !node.isDumpingSite()) {
                        List<Node> copiedRoute = vehicle.copyRoute();
                        vehicle.getRoute().remove(node);
                        currentValue = getDataValueKim(data);

                        currentNodeSwap.setNode(node);
                        currentNodeSwap.setValue(currentValue);
                        currentNodeSwap.setIndex(i);
                        currentNodeSwap.setVehicle(vehicle);

                        nodeSwapList.add(currentNodeSwap);

                        vehicle.setRoute(copiedRoute);
                    }
                }
            }
            nodeSwapList.sort(new Comparator<NodeSwap>() {
                @Override
                public int compare(NodeSwap o1, NodeSwap o2) {
                    return (Float.compare(o1.getValue(), o2.getValue()));
                }
            });

            double y = random.nextDouble();
            int index = (int)(Math.pow(y, p_worst) * nodeSwapList.size());

            bestNodeSwap = nodeSwapList.get(index);
            bestNodeSwap.getVehicle().getRoute().remove(bestNodeSwap.getIndex());
            nodesToSwap.add(bestNodeSwap.getNode());
        }
    }

    private float getDataValueKim(Data data) {
        float overallDistance = 0;
        for(Vehicle vehicle : data.getFleet()) {
            float distance = vehicle.calculateTravelDistanceKim(data);
            overallDistance += distance;
        }
        return overallDistance;
    }
}
