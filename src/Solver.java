import data.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Solver {

    private List<Data> dataList;
    private HeuristicWeights heuristicWeights;
    private Random random;
    private List<String> hashes;
    private final Constants CONSTANTS;

    public Solver(List<Data> dataList) {
        this.dataList = dataList;
        this.heuristicWeights = new HeuristicWeights();
        this.random = new Random();
        this.hashes = new ArrayList<>();
        this.CONSTANTS = new Constants();
    }

    public float initGreedy(Data data, Logger logger) {

        LocalTime startGreedy = LocalTime.now();
        long startGreedyNano = System.nanoTime();

        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        logger.log(CONSTANTS.getDividerString());
        logger.emptyLine();
        logger.log("Solving " + data.getInfo() + " with greedy at " + date + " " + time);
        logger.emptyLine();
        logger.log("Greedy initialization started at " + startGreedy);

        //init vehicles
        for(Vehicle vehicle : data.getFleet()) vehicle.initVehicle();
        Vehicle penaltyVehicle = new Vehicle();
        penaltyVehicle.initVehicle();
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
        StringBuilder currentVehicleRouteStringBuilder;
        Node dumpingSite;
        currentVehicle.getRoute().add(currentNode);
        currentVehicle.setCurrentTime((float)currentNode.getTimeStart());

        while(data.hasMoreUnvisitedNodes()){
            nextNode = data.findNextNode(currentVehicle, currentNode);
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

        LocalTime endGreedy = LocalTime.now();
        long endGreedyNano = System.nanoTime();

        logger.log("Greedy initialization ended at " + endGreedy);
        logger.log("Greedy took " + ((endGreedyNano - startGreedyNano) * 1e-9) + " seconds.");
        logger.emptyLine();

        float travelDistance, sumTravelDistance = 0;
        int numberOfCustomers;

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList())) {
            travelDistance = vehicle.calculateTravelDistance(data);
            sumTravelDistance += travelDistance;
            numberOfCustomers = (int)vehicle.getRoute().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count();
            currentVehicleRouteStringBuilder = new StringBuilder("Vehicle " + vehicle.getId() + "'s service time: "
                    + travelDistance + " with " + numberOfCustomers + " customers.");
            logger.log(currentVehicleRouteStringBuilder.toString());
        }
        logger.log("Total travel distance: " + sumTravelDistance);
        logger.emptyLine();

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList())) {
            currentVehicleRouteStringBuilder = new StringBuilder("Vehicle " + vehicle.getId() + "'s route: ");
            for(Node node : vehicle.getRoute()) {
                String str;
                if(node.isDepot()) {
                    str = "DP0";
                } else if (node.isDumpingSite()) {
                    str = "DS" + node.getId();
                } else {
                    str = node.getId().toString();
                }
                currentVehicleRouteStringBuilder.append(str).append(" ");
            }
            logger.log(currentVehicleRouteStringBuilder.toString());
        }

        logger.emptyLine();
        logger.log(CONSTANTS.getDividerString());

        return 0;
    }

    public Data ALNS(Data data, Logger logger) {

        LocalTime startALNS = LocalTime.now();
        long startALNSNano = System.nanoTime();

        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        logger.log(CONSTANTS.getDividerString());
        logger.emptyLine();
        logger.log("Solving " + data.getInfo() + " with greedy at " + date + " " + time);
        logger.emptyLine();
        logger.log("ALNS initialization started at " + startALNS);

        logger.emptyLine();

        data.destroyInfo();
        Data bestData = new Data(data);
        Data currentData;
        float T = calculateInitialTemperature(data, CONSTANTS.getW()), bestValue = getDataValue(bestData), currentValue, newValue, delta;
        List<Node> nodesToSwap;
        int numberOfSteps = 1, score = 0, numberOfNodesToSwap, upperLimit;
        String hashCode;
        StringBuilder currentVehicleRouteStringBuilder;
        while (numberOfSteps < 1000) {
            logger.log("Iteration " + numberOfSteps);
            System.out.println(numberOfSteps);
            currentData = new Data(data);
            currentValue = getDataValue(currentData);
            logger.log("Current data value: " + currentValue);
            nodesToSwap = new ArrayList<>();
            upperLimit = Math.min(100, (int) currentData.getNodeList().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count());
            numberOfNodesToSwap = random.nextInt(upperLimit - 4) + 4;
            logger.log("Number of nodes to swap: " + numberOfNodesToSwap);
            destroyNodes(currentData, numberOfNodesToSwap, nodesToSwap, logger);
            repairNodes(currentData, nodesToSwap, logger);
            newValue = getDataValue(currentData);
            logger.log("New data value: " + newValue);
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
                logger.log("Solution accepted by default");

            } else if (Math.exp(-1 * (delta) / T) > Math.random()) {

                if(!hashes.contains(hashCode)) {
                    score = CONSTANTS.getSIGMA_3();
                    hashes.add(hashCode);
                }

                data = currentData;
                logger.log("Solution accepted by chance");

            }

            if(newValue < bestValue) {

                if(!hashes.contains(hashCode)) {
                    score = CONSTANTS.getSIGMA_1();
                    hashes.add(hashCode);
                }

                bestValue = newValue;
                bestData = new Data(currentData);
                logger.log("New best solution found");

            }

            updateHeuristicInformation(heuristicWeights, score, logger);
            //update information about performance of destroy and repair methods
            if(numberOfSteps % 100 == 0) {
                updateWeights(heuristicWeights, CONSTANTS.getR());
            }
            numberOfSteps++;
            T *= 0.995;
            logger.emptyLine();
        }

        // TODO: do something with the best found solution (bestData)

        LocalTime endALNS = LocalTime.now();
        long endALNSNano = System.nanoTime();

        logger.emptyLine();
        logger.log("ALNS ended at " + endALNS);
        logger.log("ALNS took " + ((endALNSNano - startALNSNano) * 1e-9) + " seconds.");
        logger.emptyLine();

        float travelDistance, sumTravelDistance = 0;
        int numberOfCustomers;

        for(Vehicle vehicle : bestData.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList())) {
            travelDistance = vehicle.calculateTravelDistance(bestData);
            sumTravelDistance += travelDistance;
            numberOfCustomers = (int)vehicle.getRoute().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count();
            currentVehicleRouteStringBuilder = new StringBuilder("Vehicle " + vehicle.getId() + "'s service time: "
                    + travelDistance + " with " + numberOfCustomers + " customers.");
            logger.log(currentVehicleRouteStringBuilder.toString());
        }
        logger.log("Total travel distance: " + sumTravelDistance);
        logger.emptyLine();

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList())) {
            currentVehicleRouteStringBuilder = new StringBuilder("Vehicle " + vehicle.getId() + "'s route: ");
            for(Node node : vehicle.getRoute()) {
                String str;
                if(node.isDepot()) {
                    str = "DP0";
                } else if (node.isDumpingSite()) {
                    str = "DS" + node.getId();
                } else {
                    str = node.getId().toString();
                }
                currentVehicleRouteStringBuilder.append(str).append(" ");
            }
            logger.log(currentVehicleRouteStringBuilder.toString());
        }

        logger.emptyLine();
        logger.log(CONSTANTS.getDividerString());

        return bestData;
    }

    private float calculateInitialTemperature(Data data, float W) {
        float initialValue = getDataValue(data);
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
        float newRegret_2_InsertWeight = heuristicWeights.getRegret_2_InsertWeight() * (1 - r)
                + r * (heuristicWeights.getRegret_2_InsertScore() / (float)heuristicWeights.getTimesUsedRegret_2_Insert());
        float newRegret_3_InsertWeight = heuristicWeights.getRegret_3_InsertWeight() * (1 - r)
                + r * (heuristicWeights.getRegret_3_InsertScore() / (float)heuristicWeights.getTimesUsedRegret_3_Insert());
        float newRegret_K_InsertWeight = heuristicWeights.getRegret_K_InsertWeight() * (1 - r)
                + r * (heuristicWeights.getRegret_K_InsertScore() / (float)heuristicWeights.getTimesUsedRegret_K_Insert());
        heuristicWeights.setRandomRemoveWeight(newRandomRemoveWeight);
        heuristicWeights.setWorstRemoveWeight(newWorstRemoveWeight);
        heuristicWeights.setRelatedRemoveWeight(newRelatedRemoveWeight);
        heuristicWeights.setGreedyInsertWeight(newGreedyInsertWeight);
        heuristicWeights.setRegret_2_InsertWeight(newRegret_2_InsertWeight);
        heuristicWeights.setRegret_3_InsertWeight(newRegret_3_InsertWeight);
        heuristicWeights.setRegret_K_InsertWeight(newRegret_K_InsertWeight);
        heuristicWeights.setRandomRemoveScore(0);
        heuristicWeights.setWorstRemoveScore(0);
        heuristicWeights.setRelatedRemoveScore(0);
        heuristicWeights.setGreedyInsertScore(0);
        heuristicWeights.setRegret_2_InsertScore(0);
        heuristicWeights.setRegret_3_InsertScore(0);
        heuristicWeights.setRegret_K_InsertScore(0);
    }

    private void updateHeuristicInformation(HeuristicWeights heuristicWeights, int score, Logger logger) {

        LocalTime startTime = LocalTime.now();
        logger.log("Updating heuristic information started at: " + startTime);

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
                heuristicWeights.setRegret_2_InsertScore(heuristicWeights.getRegret_2_InsertScore() + score);
                heuristicWeights.setTimesUsedRegret_2_Insert(heuristicWeights.getTimesUsedRegret_2_Insert() + 1);
            case 3:
                heuristicWeights.setRegret_3_InsertScore(heuristicWeights.getRegret_3_InsertScore() + score);
                heuristicWeights.setTimesUsedRegret_3_Insert(heuristicWeights.getTimesUsedRegret_3_Insert() + 1);
            case 4:
                heuristicWeights.setRegret_K_InsertScore(heuristicWeights.getRegret_K_InsertScore() + score);
                heuristicWeights.setTimesUsedRegret_K_Insert(heuristicWeights.getTimesUsedRegret_K_Insert() + 1);
            default:
                break;
        }

        LocalTime endTime = LocalTime.now();
        logger.log("Updating heuristic information ended at: " + endTime + ", took " + startTime.until(endTime, ChronoUnit.SECONDS) + " seconds");

    }

    private void repairNodes(Data data, List<Node> nodesToSwap, Logger logger) {

        LocalTime startTime = LocalTime.now();
        logger.log("Repairing nodes started at: " + startTime);
        logger.log("Inserting " + nodesToSwap.size() + " nodes");

        float sumOf = heuristicWeights.sumOfRepair();
        float greedyWeight = heuristicWeights.getGreedyInsertWeight() / sumOf;
        float regret_2_Weight = heuristicWeights.getRegret_2_InsertWeight() / sumOf;
        float regret_3_Weight = heuristicWeights.getRegret_3_InsertWeight() / sumOf;
        float regret_K_Weight = heuristicWeights.getRegret_K_InsertWeight() / sumOf;
        double randomValue = random.nextDouble();
        if(randomValue < greedyWeight) {
            heuristicWeights.setCurrentInsert(1);
            logger.log("Insert method: greedyInsert");
            greedyInsert(data, nodesToSwap, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight) {
            heuristicWeights.setCurrentInsert(2);
            logger.log("Insert method: regretInsert_2");
            regretInsert(data, nodesToSwap, 2, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight + regret_3_Weight) {
            heuristicWeights.setCurrentInsert(3);
            logger.log("Insert method: regretInsert_2");
            regretInsert(data, nodesToSwap, 3, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight + regret_3_Weight + regret_K_Weight) {
            heuristicWeights.setCurrentInsert(4);
            logger.log("Insert method: regretInsert_k");
            int customerNodeCount = (int) data.getNodeList().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count();
            regretInsert(data, nodesToSwap, customerNodeCount, logger);
        }

        LocalTime endTime = LocalTime.now();
        logger.log("Repairing nodes ended at: " + endTime + ", took " + startTime.until(endTime, ChronoUnit.SECONDS) + " seconds");
    }

    private void regretInsert(Data data, List<Node> nodesToSwap, int p, Logger logger) {
        LocalTime startTime = LocalTime.now();
        logger.log("regretInsert_" + (p == 2 || p == 3 ? p : "k") + " started at: " + startTime);
        List<NodeSwap> nodeSwapList;
        float bestDiff, currentValue, diff, initialValue;
        Vehicle vehicleToInsertInto = null, penaltyVehicle = data.getPenaltyVehicle();
        int indexToInsert = 0;
        boolean foundVehicleForInsert = false;
        NodeSwap currentNodeSwap;
        Node nodeToInsert;
        long totalNanoSeconds = 0, startNano, endNano;
        while(nodesToSwap.size() > 0) {
            LocalTime removeStartTime = LocalTime.now();
            logger.log((nodesToSwap.size() + 1) + "node to insert left, started at: " + removeStartTime);
            initialValue = getDataValue(data);
            nodeSwapList = new ArrayList<>();
            for(Node nodesToInsert : nodesToSwap) {
                bestDiff = Float.MAX_VALUE;
                currentNodeSwap = new NodeSwap(nodesToInsert);
                for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
                    for (int i = 1; i < vehicle.getRoute().size() - 1; i++) {
                        List<Node> copiedRoute = vehicle.copyRoute();
                        vehicle.getRoute().add(i, nodesToInsert);
                        startNano = System.nanoTime();
                        boolean valid = checkForValidity(data, vehicle);
                        endNano = System.nanoTime();
                        totalNanoSeconds += endNano - startNano;
                        if (valid) {
                            currentValue = getDataValue(data);
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

            LocalTime removeEndTime = LocalTime.now();
            logger.log("Node insert ended at: " + removeEndTime + ", took " + removeStartTime.until(removeEndTime, ChronoUnit.SECONDS) + " seconds, " +
                    "validating data took " + (totalNanoSeconds * 1e9) + " seconds");

        }
        LocalTime endTime = LocalTime.now();
        logger.log("regretInsert ended at: " + endTime + ", took " + startTime.until(endTime, ChronoUnit.SECONDS) + " seconds");
    }

    private void greedyInsert(Data data, List<Node> nodesToSwap, Logger logger) {
        LocalTime startTime = LocalTime.now();
        logger.log("greedyInsert started at: " + startTime);
        List<NodeSwap> nodeSwapList;
        float bestDiff, currentValue, diff, initialValue;
        Vehicle vehicleToInsertInto = null, penaltyVehicle = data.getPenaltyVehicle();
        int indexToInsert = 0;
        boolean foundVehicleForInsert = false;
        NodeSwap currentNodeSwap;
        Node nodeToInsert;
        long totalNanoSeconds = 0, startNano, endNano;
        while(nodesToSwap.size() > 0) {
            LocalTime removeStartTime = LocalTime.now();
            logger.log((nodesToSwap.size() + 1) + "node to insert left, started at: " + removeStartTime);
            nodeSwapList = new ArrayList<>();
            initialValue = getDataValue(data);
            for(Node nodesToInsert : nodesToSwap) {
                bestDiff = Float.MAX_VALUE;
                for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 0 && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
                    for(int i = 1; i < vehicle.getRoute().size() - 1; i++) {
                        List<Node> copiedRoute = vehicle.copyRoute();
                        vehicle.getRoute().add(i, nodesToInsert);
                        startNano = System.nanoTime();
                        boolean validSolution = checkForValidity(data, vehicle);
                        endNano = System.nanoTime();
                        totalNanoSeconds += endNano - startNano;
                        if(validSolution) {
                            currentValue = getDataValue(data);
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

            LocalTime removeEndTime = LocalTime.now();
            logger.log("Node insert ended at: " + removeEndTime + ", took " + removeStartTime.until(removeEndTime, ChronoUnit.SECONDS) + " seconds, " +
                    "validating data took " + (totalNanoSeconds * 1e9) + " seconds");
        }
        LocalTime endTime = LocalTime.now();
        logger.log("greedyInsert ended at: " + endTime + ", took " + startTime.until(endTime, ChronoUnit.SECONDS) + " seconds");
    }

    private boolean checkForValidity(Data data, Vehicle vehicle) {
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

    private void destroyNodes(Data data, int p, List<Node> nodesToSwap, Logger logger) {

        LocalTime startTime = LocalTime.now();
        logger.log("Destroying nodes started at: " + startTime);
        logger.log("Removing " + p + " nodes");

        float sumOf = heuristicWeights.sumOfDestroy();
        float worstWeight = heuristicWeights.getWorstRemoveWeight() / sumOf;
        float randomWeight = heuristicWeights.getRandomRemoveWeight() / sumOf;
        float relatedWeight = heuristicWeights.getRelatedRemoveWeight() / sumOf;
        double randomValue = random.nextDouble();

        if(randomValue < worstWeight) {
            heuristicWeights.setCurrentRemove(1);
            logger.log("Destroy method: worstRemoval");
            worstRemoval(data, p, nodesToSwap, CONSTANTS.getP_WORST(), logger);
        } else if(randomValue < worstWeight + randomWeight) {
            heuristicWeights.setCurrentRemove(2);
            logger.log("Destroy method: randomRemoval");
            randomRemoval(data, p, nodesToSwap, logger);
        } else if(randomValue < worstWeight + randomWeight + relatedWeight){
            heuristicWeights.setCurrentRemove(3);
            logger.log("Destroy method: relatedRemoval");
            relatedRemoval(data, p, nodesToSwap, CONSTANTS.getPHI(), CONSTANTS.getCHI(), CONSTANTS.getPSI(), CONSTANTS.getP(), logger);
        }

        LocalTime endTime = LocalTime.now();
        logger.log("Destroying nodes ended at: " + endTime + ", took " + startTime.until(endTime, ChronoUnit.SECONDS) + " seconds");
    }


    private void relatedRemoval(Data data, int p, List<Node> nodesToSwap,
                                float phi, float chi, float psi, int P, Logger logger) {
        LocalTime startTime = LocalTime.now();
        logger.log("relatedRemoval started at: " + startTime);
        randomRemoval(data, 1, nodesToSwap, logger);
        int randomIndex;
        List<NodeSwap> nodeSwapList;
        NodeSwap bestNodeSwap, currentNodeSwap;
        while(nodesToSwap.size() < p) {
            LocalTime removeStartTime = LocalTime.now();
            logger.log((nodesToSwap.size() + 1) + "th node removal started at: " + removeStartTime);
            nodeSwapList = new ArrayList<>();
            randomIndex = nodesToSwap.size() == 0 ? 0 : random.nextInt(nodesToSwap.size());
            Node nodeToCompare = nodesToSwap.get(randomIndex);
            data.calculateVisitingTime();
            // TODO: szűrés feltétele?
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

            LocalTime removeEndTime = LocalTime.now();
            logger.log((nodesToSwap.size() + 1) + "th node removal ended at: " + removeEndTime + ", took " + removeStartTime.until(removeEndTime, ChronoUnit.SECONDS) + " seconds");
        }
        LocalTime endTime = LocalTime.now();
        logger.log("relatedRemoval ended at: " + endTime + ", took " + startTime.until(endTime, ChronoUnit.SECONDS) + " seconds");
    }

    private void randomRemoval(Data data, int p, List<Node> nodesToSwap, Logger logger) {
        LocalTime startTime = LocalTime.now();
        logger.log("randomRemoval started at: " + startTime);
        List<Node> feasibleNodesToRemove;
        int numberOfFeasibleNodesToRemove, index;
        boolean found;
        while (nodesToSwap.size() < p) {
            LocalTime removeStartTime = LocalTime.now();
            logger.log((nodesToSwap.size() + 1) + "th node removal started at: " + removeStartTime);
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
            LocalTime removeEndTime = LocalTime.now();
            logger.log((nodesToSwap.size() + 1) + "th node removal ended at: " + removeEndTime + ", took " + removeStartTime.until(removeEndTime, ChronoUnit.SECONDS) + " seconds");
        }
        LocalTime endTime = LocalTime.now();
        logger.log("randomRemoval ended at: " + endTime + ", took " + startTime.until(endTime, ChronoUnit.SECONDS) + " seconds");
    }

    private void worstRemoval(Data data, int p, List<Node> nodesToSwap, int p_worst, Logger logger) {
        LocalTime startTime = LocalTime.now();
        logger.log("worstRemoval started at: " + startTime);
        float currentValue;
        NodeSwap bestNodeSwap;
        List<NodeSwap> nodeSwapList;
        while (nodesToSwap.size() < p) {
            LocalTime removeStartTime = LocalTime.now();
            logger.log((nodesToSwap.size() + 1) + "th node removal started at: " + removeStartTime);
            nodeSwapList = new ArrayList<>();
            // TODO: szűrés feltétele?
            for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 0).collect(Collectors.toList())) {
                for(int i = 0; i < vehicle.getRoute().size(); i++) {
                    Node node = vehicle.getRoute().get(i);
                    NodeSwap currentNodeSwap = new NodeSwap();
                    if(!node.isDepot() && !node.isDumpingSite()) {
                        List<Node> copiedRoute = vehicle.copyRoute();
                        vehicle.getRoute().remove(node);
                        currentValue = getDataValue(data);

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

            LocalTime removeEndTime = LocalTime.now();
            logger.log((nodesToSwap.size() + 1) + "th node removal ended at: " + removeEndTime + ", took " + removeStartTime.until(removeEndTime, ChronoUnit.SECONDS) + " seconds");
        }
        LocalTime endTime = LocalTime.now();
        logger.log("worstRemoval ended at: " + endTime + ", took " + startTime.until(endTime, ChronoUnit.SECONDS) + " seconds");
    }

    private float getDataValue(Data data) {
        float overallDistance = 0;
        for(Vehicle vehicle : data.getFleet()) {
            float distance = vehicle.calculateTravelDistance(data);
            overallDistance += distance;
        }
        return overallDistance;
    }
}
