import data.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Solver {

    private final List<Data> dataList;
    private HeuristicWeights heuristicWeights;
    private final Random random;
    private final List<String> hashes;
    private final Constants CONSTANTS;

    public Solver(List<Data> dataList) {
        this.dataList = dataList;
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
        for (Vehicle vehicle : data.getFleet()) vehicle.initVehicle();
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
        currentVehicle.setCurrentTime((float) currentNode.getTimeStart());
        currentVehicle.getArrivalTimes().add((float) currentNode.getTimeStart());

        while (data.hasMoreUnvisitedNodes()) {
            // TODO: greedy ne várhasson
            nextNode = data.findNextNode(currentVehicle, currentNode);
            if (!nextNode.isNullNode()) {
                currentTime = currentVehicle.getCurrentTime();
                travelTime = data.getDistanceBetweenNode(currentNode, nextNode);
                serviceTime = currentNode.getServiceTime();
                quantity = nextNode.getQuantity();
                currentVehicle.setCurrentTime(currentTime + serviceTime + travelTime);
                currentVehicle.setCapacity(currentVehicle.getCapacity() + quantity);

                currentNode = nextNode;
                currentNode.setVisitedAt(currentVehicle.getCurrentTime());
                currentVehicle.getArrivalTimes().add(currentNode.getVisitedAt());
                currentNode.setVisited(true);
                currentVehicle.getRoute().add(currentNode);
            } else {
                if (currentNode.isDumpingSite()) {
                    Node depot = data.getDepotNode();
                    currentTime = currentVehicle.getCurrentTime();
                    travelTime = data.getDistanceBetweenNode(currentNode, depot);
                    serviceTime = currentNode.getServiceTime();
                    currentVehicle.setCurrentTime(currentTime + serviceTime + travelTime);
                    currentVehicle.getArrivalTimes().add(currentTime + serviceTime + travelTime);

                    currentNode = depot;
                    currentVehicle.getRoute().add(currentNode);
                    currentVehicle = data.getFleet().get(currentVehicle.getId() + 1);

                    currentVehicle.getRoute().add(data.getDepotNode());
                    currentVehicle.setCurrentTime((float) data.getDepotNode().getTimeStart());
                    currentVehicle.getArrivalTimes().add((float) data.getDepotNode().getTimeStart());
                    continue;
                }
                dumpingSite = data.getNearestDumpingSiteNode(currentVehicle, currentNode);
                currentTime = currentVehicle.getCurrentTime();
                travelTime = data.getDistanceBetweenNode(currentNode, dumpingSite);
                serviceTime = currentNode.getServiceTime();
                currentVehicle.setCurrentTime(currentTime + serviceTime + travelTime);
                currentVehicle.getArrivalTimes().add(currentTime + serviceTime + travelTime);
                currentVehicle.setCapacity((float) 0);

                currentNode = dumpingSite;
                currentVehicle.getRoute().add(currentNode);
            }
        }

        dumpingSite = data.getNearestDumpingSiteNode(currentVehicle, currentNode);
        currentVehicle.getArrivalTimes().add(currentVehicle.getArrivalTimes().get(currentVehicle.getArrivalTimes().size() - 1) + currentNode.getServiceTime() + data.getDistanceBetweenNode(currentNode, dumpingSite));
        currentVehicle.getArrivalTimes().add(currentVehicle.getArrivalTimes().get(currentVehicle.getArrivalTimes().size() - 1) + dumpingSite.getServiceTime() + data.getDistanceBetweenNode(dumpingSite, data.getDepotNode()));
        currentVehicle.getRoute().add(dumpingSite);
        currentVehicle.getRoute().add(data.getDepotNode());

        for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() == 0).collect(Collectors.toList())) {
            Node depotNode = data.getDepotNode();
            Node dump = data.getNearestDumpingSiteNode(vehicle, depotNode);
            vehicle.getRoute().add(depotNode);
            vehicle.getRoute().add(dump);
            vehicle.getRoute().add(depotNode);
            vehicle.getArrivalTimes().add((float)depotNode.getTimeStart());
            vehicle.getArrivalTimes().add(vehicle.getArrivalTimes().get(0) + depotNode.getServiceTime() + data.getDistanceBetweenNode(depotNode, dump));
            vehicle.getArrivalTimes().add(vehicle.getArrivalTimes().get(1) + dump.getServiceTime() + data.getDistanceBetweenNode(dump, depotNode));
        }

        LocalTime endGreedy = LocalTime.now();
        long endGreedyNano = System.nanoTime();

        logger.log("Greedy initialization ended at " + endGreedy);
        logger.log("Greedy took " + ((endGreedyNano - startGreedyNano) * 1e-9) + " seconds.");
        logger.emptyLine();

        float travelDistance, sumTravelDistance = 0;
        int numberOfCustomers;

        for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList())) {
            travelDistance = vehicle.calculateTravelDistance(data);
            sumTravelDistance += travelDistance;
            numberOfCustomers = (int) vehicle.getRoute().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count();
            currentVehicleRouteStringBuilder = new StringBuilder("Vehicle " + vehicle.getId() + "'s service time: "
                    + travelDistance + " with " + numberOfCustomers + " customers.");
            logger.log(currentVehicleRouteStringBuilder.toString());
        }
        logger.log("Total travel distance: " + sumTravelDistance);
        logger.emptyLine();

        for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList())) {
            currentVehicleRouteStringBuilder = new StringBuilder("Vehicle " + vehicle.getId() + "'s route: ");
            for (Node node : vehicle.getRoute()) {
                String str;
                if (node.isDepot()) {
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
        long iterationStart;
        long iterationEnd;

        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        logger.log(CONSTANTS.getDividerString());
        logger.emptyLine();
        logger.log("Solving " + data.getInfo() + " with greedy at " + date + " " + time);
        logger.emptyLine();
        logger.log("ALNS initialization started at " + startALNS);

        logger.emptyLine();
        logger.emptyLine();

        heuristicWeights = new HeuristicWeights();

        data.destroyInfo();
        Data bestData = new Data(data);
        Data currentData;
        float T = calculateInitialTemperature(data, CONSTANTS.getW()), bestValue = getDataValue(bestData), currentValue, newValue, delta;
        List<Node> nodesToSwap;
        int numberOfSteps = 1, score = 0, numberOfNodesToSwap, upperLimit;
        String hashCode;
        StringBuilder currentVehicleRouteStringBuilder;
        while (numberOfSteps < 10) {
            logger.log("Iteration " + numberOfSteps);
            iterationStart = System.nanoTime();
            System.out.println(numberOfSteps + " of " + data.getInfo());
            currentData = new Data(data);
            currentValue = getDataValue(currentData);
            logger.log("Current data value: " + currentValue);
            nodesToSwap = new ArrayList<>();
            upperLimit = Math.min(100, (int) (currentData.getNodeList().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count() * 0.4));
            numberOfNodesToSwap = random.nextInt(upperLimit - 4) + 4;
            System.out.println("nodes to swap: " + numberOfNodesToSwap);
            logger.log("Number of nodes to swap: " + numberOfNodesToSwap);
            destroyNodes(currentData, numberOfNodesToSwap, nodesToSwap, logger);
            updateArrivalTimes(currentData);
            repairNodes(currentData, nodesToSwap, logger);
            newValue = getDataValue(currentData);
            logger.log("New data value: " + newValue);
            delta = newValue - currentValue;
            hashCode = currentData.dataToHash();

            if (delta < 0) {

                if (newValue >= bestValue) {
                    if (!hashes.contains(hashCode)) {
                        score = CONSTANTS.getSIGMA_2();
                        hashes.add(hashCode);
                    }
                }

                data = currentData;
                updateArrivalTimes(data);
                logger.log("Solution accepted by default");

            } else if (Math.exp(-1 * (delta) / T) > Math.random()) {

                if (!hashes.contains(hashCode)) {
                    score = CONSTANTS.getSIGMA_3();
                    hashes.add(hashCode);
                }

                data = currentData;
                updateArrivalTimes(data);
                logger.log("Solution accepted by chance");

            }

            if (newValue < bestValue) {

                if (!hashes.contains(hashCode)) {
                    score = CONSTANTS.getSIGMA_1();
                    hashes.add(hashCode);
                }

                bestValue = newValue;
                bestData = new Data(currentData);
                updateArrivalTimes(bestData);
                logger.log("New best solution found");

            }

            updateHeuristicInformation(heuristicWeights, score, logger);
            //update information about performance of destroy and repair methods
            if (numberOfSteps % 100 == 0) {
                updateWeights(heuristicWeights, CONSTANTS.getR());
            }
            numberOfSteps++;
            T *= 0.995;
            iterationEnd = System.nanoTime();
            logger.log("Iteration took " + ((iterationEnd - iterationStart) * 1e-9) + " seconds");
            logger.emptyLine();
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

        for (Vehicle vehicle : bestData.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList())) {
            travelDistance = vehicle.calculateTravelDistance(bestData);
            sumTravelDistance += travelDistance;
            numberOfCustomers = (int) vehicle.getRoute().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count();
            currentVehicleRouteStringBuilder = new StringBuilder("Vehicle " + vehicle.getId() + "'s service time: "
                    + travelDistance + " with " + numberOfCustomers + " customers.");
            logger.log(currentVehicleRouteStringBuilder.toString());
        }
        logger.log("Total travel distance: " + sumTravelDistance);
        logger.emptyLine();

        for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList())) {
            currentVehicleRouteStringBuilder = new StringBuilder("Vehicle " + vehicle.getId() + "'s route: ");
            for (Node node : vehicle.getRoute()) {
                String str;
                if (node.isDepot()) {
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

    private void updateArrivalTimes(Data data) {
        List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList());
        for(Vehicle vehicle : feasibleVehicles) {
            vehicle.getArrivalTimes().clear();
            Node currentNode = vehicle.getRoute().get(0), previousNode;
            vehicle.getArrivalTimes().add((float)currentNode.getTimeStart());
            float arrivalTime = currentNode.getTimeStart(), serviceTime = currentNode.getServiceTime(), travelTime;

            for(int i = 1; i < vehicle.getRoute().size(); i++) {
                previousNode = currentNode;
                currentNode = vehicle.getRoute().get(i);
                travelTime = data.getDistanceBetweenNode(previousNode, currentNode);
                arrivalTime = Math.max(arrivalTime + serviceTime + travelTime, currentNode.getTimeStart());
                vehicle.getArrivalTimes().add(arrivalTime);
                serviceTime = currentNode.getServiceTime();
            }
        }
    }

    private void updateArrivalTimesForVehicle(Vehicle vehicle, Data data) {
        vehicle.getArrivalTimes().clear();
        Node currentNode = vehicle.getRoute().get(0), previousNode;
        vehicle.getArrivalTimes().add((float)currentNode.getTimeStart());
        float arrivalTime = currentNode.getTimeStart(), serviceTime = currentNode.getServiceTime(), travelTime;

        for(int i = 1; i < vehicle.getRoute().size(); i++) {
            previousNode = currentNode;
            currentNode = vehicle.getRoute().get(i);
            travelTime = data.getDistanceBetweenNode(previousNode, currentNode);
            arrivalTime = Math.max(arrivalTime + serviceTime + travelTime, currentNode.getTimeStart());
            vehicle.getArrivalTimes().add(arrivalTime);
            serviceTime = currentNode.getServiceTime();
        }
    }

    private float calculateInitialTemperature(Data data, float W) {
        float initialValue = getDataValue(data);
        return (float) (-1 * (W * initialValue) / Math.log(0.5));
    }

    private void updateWeights(HeuristicWeights heuristicWeights, float r) {
        float newRandomRemoveWeight = heuristicWeights.getRandomRemovalWeight() * (1 - r)
                + r * (heuristicWeights.getRandomRemovalScore() / (float) heuristicWeights.getTimesUsedRandomRemove());
        float newWorstRemoveWeight = heuristicWeights.getWorstRemovalWeight() * (1 - r)
                + r * (heuristicWeights.getWorstRemovalScore() / (float) heuristicWeights.getTimesUsedWorstRemove());
        float newRelatedRemoveWeight = heuristicWeights.getRelatedRemovalWeight() * (1 - r)
                + r * (heuristicWeights.getRelatedRemovalScore() / (float) heuristicWeights.getTimesUsedRelatedRemove());
        float newDeleteDisposalWeight = heuristicWeights.getDeleteDisposalWeight() * (1 - r)
                + r * (heuristicWeights.getDeleteDisposalScore() / (float) heuristicWeights.getTimesUsedDeleteDisposal());
        float newSwapDisposalWeight = heuristicWeights.getSwapDisposalWeight() * (1 - r)
                + r * (heuristicWeights.getSwapDisposalScore() / (float) heuristicWeights.getTimesUsedSwapDisposal());
        float newGreedyInsertWeight = heuristicWeights.getGreedyInsertWeight() * (1 - r)
                + r * (heuristicWeights.getGreedyInsertScore() / (float) heuristicWeights.getTimesUsedGreedyInsert());
        float newRegret_2_InsertWeight = heuristicWeights.getRegret_2_InsertWeight() * (1 - r)
                + r * (heuristicWeights.getRegret_2_InsertScore() / (float) heuristicWeights.getTimesUsedRegret_2_Insert());
        float newRegret_3_InsertWeight = heuristicWeights.getRegret_3_InsertWeight() * (1 - r)
                + r * (heuristicWeights.getRegret_3_InsertScore() / (float) heuristicWeights.getTimesUsedRegret_3_Insert());
        float newRegret_K_InsertWeight = heuristicWeights.getRegret_K_InsertWeight() * (1 - r)
                + r * (heuristicWeights.getRegret_K_InsertScore() / (float) heuristicWeights.getTimesUsedRegret_K_Insert());
        heuristicWeights.setRandomRemovalWeight(newRandomRemoveWeight);
        heuristicWeights.setWorstRemovalWeight(newWorstRemoveWeight);
        heuristicWeights.setRelatedRemovalWeight(newRelatedRemoveWeight);
        heuristicWeights.setDeleteDisposalWeight(newDeleteDisposalWeight);
        heuristicWeights.setSwapDisposalWeight(newSwapDisposalWeight);
        heuristicWeights.setGreedyInsertWeight(newGreedyInsertWeight);
        heuristicWeights.setRegret_2_InsertWeight(newRegret_2_InsertWeight);
        heuristicWeights.setRegret_3_InsertWeight(newRegret_3_InsertWeight);
        heuristicWeights.setRegret_K_InsertWeight(newRegret_K_InsertWeight);
        heuristicWeights.setRandomRemovalScore(0);
        heuristicWeights.setWorstRemovalScore(0);
        heuristicWeights.setRelatedRemovalScore(0);
        heuristicWeights.setDeleteDisposalScore(0);
        heuristicWeights.setSwapDisposalScore(0);
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

        switch (destroyHeuristic) {
            case 1:
                heuristicWeights.setWorstRemovalScore(heuristicWeights.getWorstRemovalScore() + score);
                heuristicWeights.setTimesUsedWorstRemove(heuristicWeights.getTimesUsedWorstRemove() + 1);
                break;
            case 2:
                heuristicWeights.setRandomRemovalScore(heuristicWeights.getRandomRemovalScore() + score);
                heuristicWeights.setTimesUsedRandomRemove(heuristicWeights.getTimesUsedRandomRemove() + 1);
                break;
            case 3:
                heuristicWeights.setRelatedRemovalScore(heuristicWeights.getRelatedRemovalScore() + score);
                heuristicWeights.setTimesUsedRelatedRemove(heuristicWeights.getTimesUsedRelatedRemove() + 1);
                break;
            case 4:
                heuristicWeights.setDeleteDisposalScore(heuristicWeights.getDeleteDisposalScore() + score);
                heuristicWeights.setTimesUsedDeleteDisposal(heuristicWeights.getTimesUsedDeleteDisposal() + 1);
                break;
            case 5:
                heuristicWeights.setSwapDisposalScore(heuristicWeights.getSwapDisposalScore() + score);
                heuristicWeights.setTimesUsedSwapDisposal(heuristicWeights.getTimesUsedSwapDisposal() + 1);
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
        long repairStart = System.nanoTime();
        logger.log("Repairing nodes started at: " + startTime);
        logger.log("Inserting " + nodesToSwap.size() + " nodes");

        float sumOf = heuristicWeights.sumOfRepair();
        float greedyWeight = heuristicWeights.getGreedyInsertWeight() / sumOf;
        float regret_2_Weight = heuristicWeights.getRegret_2_InsertWeight() / sumOf;
        float regret_3_Weight = heuristicWeights.getRegret_3_InsertWeight() / sumOf;
        float regret_K_Weight = heuristicWeights.getRegret_K_InsertWeight() / sumOf;
        double randomValue = random.nextDouble();
        if (randomValue < greedyWeight) {
            heuristicWeights.setCurrentInsert(1);
            logger.log("Insert method: greedyInsert");
            System.out.println("Insert method: greedyInsert");
            greedyInsert(data, nodesToSwap, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight) {
            heuristicWeights.setCurrentInsert(2);
            logger.log("Insert method: regretInsert_2");
            System.out.println("Insert method: regretInsert_2");
            regretInsert(data, nodesToSwap, 2, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight + regret_3_Weight) {
            heuristicWeights.setCurrentInsert(3);
            logger.log("Insert method: regretInsert_3");
            System.out.println("Insert method: regretInsert_3");
            regretInsert(data, nodesToSwap, 3, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight + regret_3_Weight + regret_K_Weight) {
            heuristicWeights.setCurrentInsert(4);
            logger.log("Insert method: regretInsert_k");
            System.out.println("Insert method: regretInsert_k");
            int customerNodeCount = (int) data.getNodeList().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count();
            regretInsert(data, nodesToSwap, customerNodeCount, logger);
        }

        LocalTime endTime = LocalTime.now();
        long repairEnd = System.nanoTime();
        logger.log("Repairing nodes ended at: " + endTime + ", took " + ((repairEnd - repairStart) * 1e-9) + " seconds");
    }

    private void regretInsert(Data data, List<Node> nodesToSwap, int p, Logger logger) {

        LocalTime startTime = LocalTime.now();
        long startNanoTime = System.nanoTime();
        logger.log("regretInsert_" + (p == 2 || p == 3 ? p : "k") + " started at: " + startTime);

        List<NodeSwap> nodeSwapList;
        float bestDiff, currentValue, diff, initialValue;
        Vehicle vehicleToInsertInto = null, penaltyVehicle = data.getPenaltyVehicle();
        int indexToInsert = 0;
        NodeSwap currentNodeSwap;
        Node nodeToInsert;
        long totalNanoSecondsForValidityCheckInCurrentIteration = 0, startNano, endNano;
        long totalInsertValidityCheck = 0;
        while (nodesToSwap.size() > 0) {

            LocalTime insertStartTime = LocalTime.now();
            long insertStartNanoTime = System.nanoTime();
            logger.log((nodesToSwap.size() + 1) + "node to insert left, started at: " + insertStartTime);

            initialValue = getDataValue(data);
            nodeSwapList = new ArrayList<>();
            List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> !vehicle.isPenaltyVehicle()).collect(Collectors.toList());
            for (Node nodesToInsert : nodesToSwap) {
                totalNanoSecondsForValidityCheckInCurrentIteration = 0;
                bestDiff = Float.MAX_VALUE;
                currentNodeSwap = new NodeSwap(nodesToInsert);
                boolean checkedEmptyVehicle = false;
                for (Vehicle vehicle : feasibleVehicles) {
                    if(checkedEmptyVehicle) break;
                    checkedEmptyVehicle = vehicle.getRoute().size() == 3;
                    int vehicleRouteSize = vehicle.getRoute().size();
                    for (int i = 1; i < vehicleRouteSize - 1; i++) {
                        // TODO: if(wrong){continue;}
                        float previousNodeArrivalTime = vehicle.getArrivalTimes().get(i - 1);
                        Node previousNode = vehicle.getRoute().get(i - 1);
                        Node nextNode = vehicle.getRoute().get(i + 1);
                        float serviceTimeAtPreviousNode = previousNode.getServiceTime();
                        float travelDistance = data.getDistanceBetweenNode(previousNode, nodesToInsert);
                        float arrivalTimeAtNode = previousNodeArrivalTime + serviceTimeAtPreviousNode + travelDistance;

                        if(arrivalTimeAtNode > nodesToInsert.getTimeEnd()) {
                            break;
                        }

                        if(nodesToInsert.getTimeStart() > nextNode.getTimeEnd()) {
                            continue;
                        }

                        List<Node> copiedRoute = vehicle.copyRoute();
                        vehicle.getRoute().add(i, nodesToInsert);

                        startNano = System.nanoTime();
                        boolean valid = checkForValidity(data, vehicle);
                        endNano = System.nanoTime();

                        totalNanoSecondsForValidityCheckInCurrentIteration += (endNano - startNano);

                        if (valid) {
                            currentValue = getDataValue(data);
                            currentNodeSwap.getValues().add(currentValue);
                            currentNodeSwap.getVehicleSet().add(vehicle);
                            diff = currentValue - initialValue;
                            if (diff < bestDiff) {
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

            totalInsertValidityCheck += totalNanoSecondsForValidityCheckInCurrentIteration;

            nodeSwapList.sort(new Comparator<NodeSwap>() {
                @Override
                public int compare(NodeSwap o1, NodeSwap o2) {
                    return (Float.compare(o1.getVehicleSet().size(), o2.getVehicleSet().size()));
                }
            });
            int leastFeasibleVehicleInsert = nodeSwapList.get(0).getVehicleSet().size();

            NodeSwap bestNodeSwap = new NodeSwap();

            if (leastFeasibleVehicleInsert < p) {
                List<NodeSwap> feasibleNodeSwaps = nodeSwapList
                        .stream()
                        .filter(e -> e.getVehicleSet().size() == leastFeasibleVehicleInsert)
                        .collect(Collectors.toList());
                float worst = 0;
                float bestDataValue = Float.MAX_VALUE;
                for (NodeSwap nodeSwap : feasibleNodeSwaps) {
                    /*
                    nodeSwap.getValues().sort(new Comparator<Float>() {
                        @Override
                        public int compare(Float o1, Float o2) {
                            return (Float.compare(o1, o2));
                        }
                    });
                    */
                    float bestValue = Collections.min(nodeSwap.getValues());//nodeSwap.getValues().get(0);
                    float worstValue = Collections.max(nodeSwap.getValues());//nodeSwap.getValues().get(nodeSwap.getValues().size() - 1);
                    diff = worstValue - bestValue;
                    if (diff > worst) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = nodeSwap.getValue();
                    } else if (diff == worst && nodeSwap.getValues().get(0) < bestDataValue) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = nodeSwap.getValue();
                    }
                }
            } else {
                float worst = 0;
                float bestDataValue = Float.MAX_VALUE;
                for (NodeSwap nodeSwap : nodeSwapList) {
                    /*
                    nodeSwap.getValues().sort(new Comparator<Float>() {
                        @Override
                        public int compare(Float o1, Float o2) {
                            return (Float.compare(o1, o2));
                        }
                    });
                     */
                    float bestValue = Collections.min(nodeSwap.getValues());//nodeSwap.getValues().get(0);
                    float worstValue = Collections.max(nodeSwap.getValues());//nodeSwap.getValues().get(p - 1);
                    diff = worstValue - bestValue;
                    if (diff > worst) {
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

            if (bestNodeSwap.isFoundVehicleForNodeToInsert()) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
                updateArrivalTimesForVehicle(vehicleToInsertInto, data);
            } else {
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
            }
            nodesToSwap.remove(nodeToInsert);

            LocalTime insertEndTime = LocalTime.now();
            long insertEndNanoTime = System.nanoTime();
            logger.log("Node insert ended at: " + insertEndTime + ", took " + ((insertEndNanoTime - insertStartNanoTime) * 1e-9) + " seconds, " +
                    "validating data took " + (totalNanoSecondsForValidityCheckInCurrentIteration * 1e-9) + " seconds");

        }
        LocalTime endTime = LocalTime.now();
        long endNanoTime = System.nanoTime();
        logger.log("regretInsert ended at: " + endTime + ", took " + ((endNanoTime - startNanoTime) * 1e-9) + " seconds");
        logger.log("Validating the data took " + (totalInsertValidityCheck * 1e-9) + " seconds");
    }

    private void greedyInsert(Data data, List<Node> nodesToSwap, Logger logger) {

        LocalTime startTime = LocalTime.now();
        long startNanoTime = System.nanoTime();
        logger.log("greedyInsert started at: " + startTime);

        List<NodeSwap> nodeSwapList;
        float bestDiff, currentValue, diff, initialValue;
        Vehicle vehicleToInsertInto = null, penaltyVehicle = data.getPenaltyVehicle();
        int indexToInsert = 0;
        boolean foundVehicleForInsert = false;
        NodeSwap currentNodeSwap;
        Node nodeToInsert;
        long totalNanoSecondsForValidityCheckInCurrentIteration = 0, startNano, endNano;
        long totalInsertValidityCheck = 0;
        while (nodesToSwap.size() > 0) {

            LocalTime insertStartTime = LocalTime.now();
            long insertStartNanoTime = System.nanoTime();
            logger.log((nodesToSwap.size() + 1) + "node to insert left, started at: " + insertStartTime);

            nodeSwapList = new ArrayList<>();
            initialValue = getDataValue(data);
            List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> !vehicle.isPenaltyVehicle()).collect(Collectors.toList());
            for (Node nodesToInsert : nodesToSwap) {
                totalNanoSecondsForValidityCheckInCurrentIteration = 0;
                bestDiff = Float.MAX_VALUE;
                boolean checkedEmptyVehicle = false;
                for (Vehicle vehicle : feasibleVehicles) {
                    if(checkedEmptyVehicle) break;
                    checkedEmptyVehicle = vehicle.getRoute().size() == 3;
                    for (int i = 1; i < vehicle.getRoute().size() - 1; i++) {

                        float previousNodeArrivalTime = vehicle.getArrivalTimes().get(i - 1);
                        Node previousNode = vehicle.getRoute().get(i - 1);
                        Node nextNode = vehicle.getRoute().get(i + 1);
                        float serviceTimeAtPreviousNode = previousNode.getServiceTime();
                        float travelDistance = data.getDistanceBetweenNode(previousNode, nodesToInsert);
                        float arrivalTimeAtNode = previousNodeArrivalTime + serviceTimeAtPreviousNode + travelDistance;

                        if(arrivalTimeAtNode > nodesToInsert.getTimeEnd()) {
                            break;
                        }

                        if(nodesToInsert.getTimeStart() > nextNode.getTimeEnd()) {
                            continue;
                        }

                        List<Node> copiedRoute = vehicle.copyRoute();
                        vehicle.getRoute().add(i, nodesToInsert);

                        startNano = System.nanoTime();
                        boolean validSolution = checkForValidity(data, vehicle);
                        endNano = System.nanoTime();

                        totalNanoSecondsForValidityCheckInCurrentIteration += (endNano - startNano);

                        if (validSolution) {
                            currentValue = getDataValue(data);
                            diff = currentValue - initialValue;
                            if (diff < bestDiff) {
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

            totalInsertValidityCheck += totalNanoSecondsForValidityCheckInCurrentIteration;

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

            if (bestNodeSwap.isFoundVehicleForNodeToInsert()) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
                updateArrivalTimesForVehicle(vehicleToInsertInto, data);
            } else {
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
            }
            nodesToSwap.remove(nodeToInsert);

            LocalTime insertEndTime = LocalTime.now();
            long insertEndNanoTime = System.nanoTime();
            logger.log("Node insert ended at: " + insertEndTime + ", took " + ((insertEndNanoTime - insertStartNanoTime) * 1e-9) + " seconds, " +
                    "validating data took " + (totalNanoSecondsForValidityCheckInCurrentIteration * 1e-9) + " seconds");
        }

        LocalTime endTime = LocalTime.now();
        long endNanoTime = System.nanoTime();
        logger.log("greedyInsert ended at: " + endTime + ", took " + ((endNanoTime - startNanoTime) * 1e-9) + " seconds");
        logger.log("Validating the data took " + (totalInsertValidityCheck * 1e-9) + " seconds");
    }

    private boolean checkForValidity(Data data, Vehicle vehicle) {
        List<Node> route = vehicle.getRoute();

        if (route.size() > vehicle.getMaximumNumberOfStopsToVisit()) {
            return false;
        }

        Node currentNode = route.get(0), previousNode;
        float currentTime = currentNode.getTimeStart(), quantity;
        vehicle.setCurrentTime(currentTime);
        vehicle.setCapacity((float) 0);
        previousNode = currentNode;
        for (int i = 1; i < route.size(); i++) {
            currentNode = route.get(i);
            if (currentNode.isDumpingSite()) {
                vehicle.setCapacity((float) 0);
            } else if (!currentNode.isDepot()) {
                quantity = currentNode.getQuantity();
                vehicle.setCapacity(vehicle.getCapacity() + quantity);
            }

            if (vehicle.getCapacity() > vehicle.getMaximumCapacity()) {
                return false;
            }

            float serviceTimeAtPreviousNode = previousNode.getServiceTime();
            float travelDistance = data.getDistanceBetweenNode(previousNode, currentNode);

            if (!data.timeWindowCheck(currentTime + serviceTimeAtPreviousNode + travelDistance, currentNode)) {
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
        long destroyStart = System.nanoTime();
        logger.log("Destroying nodes started at: " + startTime);
        logger.log("Removing " + p + " nodes");

        float sumOf = heuristicWeights.sumOfDestroy();
        float worstWeight = heuristicWeights.getWorstRemovalWeight() / sumOf;
        float randomWeight = heuristicWeights.getRandomRemovalWeight() / sumOf;
        float relatedWeight = heuristicWeights.getRelatedRemovalWeight() / sumOf;
        float deleteWeight = heuristicWeights.getDeleteDisposalWeight() / sumOf;
        float swapWeight = heuristicWeights.getSwapDisposalWeight() / sumOf;
        float insertWeight = heuristicWeights.getInsertDisposalWeight() / sumOf;
        double randomValue = random.nextDouble();

        if (randomValue < worstWeight) {
            heuristicWeights.setCurrentRemove(1);
            logger.log("Destroy method: worstRemoval");
            System.out.println("Destroy method: worstRemoval");
            worstRemoval(data, p, nodesToSwap, CONSTANTS.getP_WORST(), logger);
        } else if (randomValue < worstWeight + randomWeight) {
            heuristicWeights.setCurrentRemove(2);
            logger.log("Destroy method: randomRemoval");
            System.out.println("Destroy method: randomRemoval");
            randomRemoval(data, p, nodesToSwap, logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight) {
            heuristicWeights.setCurrentRemove(3);
            logger.log("Destroy method: relatedRemoval");
            System.out.println("Destroy method: relatedRemoval");
            relatedRemoval(data, p, nodesToSwap, CONSTANTS.getPHI(), CONSTANTS.getCHI(), CONSTANTS.getPSI(), CONSTANTS.getP(), logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight + deleteWeight) {
            heuristicWeights.setCurrentRemove(4);
            logger.log("Destroy method: deleteDisposal");
            System.out.println("Destroy method: deleteDisposal");
            deleteDisposal(data, nodesToSwap, logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight + deleteWeight + swapWeight) {
            heuristicWeights.setCurrentRemove(5);
            logger.log("Destroy method: swapDisposal");
            System.out.println("Destroy method: swapDisposal");
            swapDisposal(data, nodesToSwap, logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight + deleteWeight + swapWeight + insertWeight) {
            heuristicWeights.setCurrentRemove(5);
            logger.log("Destroy method: insertDisposal");
            System.out.println("Destroy method: insertDisposal");
            insertDisposal(data, nodesToSwap, logger);
        }

        LocalTime endTime = LocalTime.now();
        long destroyEnd = System.nanoTime();
        logger.log("Destroying nodes ended at: " + endTime + ", took " + ((destroyEnd - destroyStart) * 1e-9) + " seconds");

    }


    private void relatedRemoval(Data data, int p, List<Node> nodesToSwap,
                                float phi, float chi, float psi, int P, Logger logger) {
        LocalTime startTime = LocalTime.now();
        logger.log("relatedRemoval started at: " + startTime);
        randomRemoval(data, 1, nodesToSwap, logger);
        int randomIndex;
        List<NodeSwap> nodeSwapList;
        NodeSwap bestNodeSwap, currentNodeSwap;
        while (nodesToSwap.size() < p) {
            LocalTime removeStartTime = LocalTime.now();
            logger.log((nodesToSwap.size() + 1) + "th node removal started at: " + removeStartTime);
            nodeSwapList = new ArrayList<>();
            randomIndex = nodesToSwap.size() == 0 ? 0 : random.nextInt(nodesToSwap.size());
            Node nodeToCompare = nodesToSwap.get(randomIndex);
            data.calculateVisitingTime();
            for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList())) {
                for (Node node : vehicle.getRoute().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).collect(Collectors.toList())) {
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
            int index = (int) (Math.pow(y, P) * nodeSwapList.size());

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
            for (Vehicle vehicle : data.getFleet()) {
                found = false;
                for (int i = 0; i < vehicle.getRoute().size(); i++) {
                    Node node = vehicle.getRoute().get(i);
                    if ((int)node.getId() == nodeToRemove.getId()) {
                        nodesToSwap.add(node);
                        vehicle.getRoute().remove(node);
                        found = true;
                        break;
                    }
                }
                if (found) break;
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
            for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList())) {
                for (int i = 0; i < vehicle.getRoute().size(); i++) {
                    Node node = vehicle.getRoute().get(i);
                    NodeSwap currentNodeSwap = new NodeSwap();
                    if (!node.isDepot() && !node.isDumpingSite()) {
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
            int index = (int) (Math.pow(y, p_worst) * nodeSwapList.size());

            bestNodeSwap = nodeSwapList.get(index);
            bestNodeSwap.getVehicle().getRoute().remove(bestNodeSwap.getIndex());
            nodesToSwap.add(bestNodeSwap.getNode());

            LocalTime removeEndTime = LocalTime.now();
            logger.log((nodesToSwap.size() + 1) + "th node removal ended at: " + removeEndTime + ", took " + removeStartTime.until(removeEndTime, ChronoUnit.SECONDS) + " seconds");
        }
        LocalTime endTime = LocalTime.now();
        logger.log("worstRemoval ended at: " + endTime + ", took " + startTime.until(endTime, ChronoUnit.SECONDS) + " seconds");
    }

    private void deleteDisposal(Data data, List<Node> nodesToSwap, Logger logger) {
        LocalTime startTime = LocalTime.now();
        logger.log("deleteDisposal started at: " + startTime);

        List<NodeSwap> nodeSwapList = new ArrayList<>();
        List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().stream().filter(Node::isDumpingSite).count() > 1).collect(Collectors.toList());
        if (feasibleVehicles.size() == 0) {
            return;
        }
        for (Vehicle vehicle : feasibleVehicles) {
            for (int i = 0; i < vehicle.getRoute().size(); i++) {
                Node node = vehicle.getRoute().get(i);
                if (node.isDumpingSite()) {
                    NodeSwap nodeSwap = new NodeSwap(node, vehicle, 0, i, false);
                    nodeSwapList.add(nodeSwap);
                }
            }
        }

        int randomIndex = random.nextInt(nodeSwapList.size());
        NodeSwap nodeSwap = nodeSwapList.get(randomIndex);
        Vehicle vehicle = nodeSwap.getVehicle();
        Node dumpingSite = nodeSwap.getNode();
        int dumpingSiteIndex = nodeSwap.getIndex();

        nodeSwapList.stream().filter(nodeSwap1 -> nodeSwap1.getVehicle() == vehicle).collect(Collectors.toList());

        nodeSwapList.sort(new Comparator<NodeSwap>() {
            @Override
            public int compare(NodeSwap o1, NodeSwap o2) {
                return (Float.compare(o1.getIndex(), o2.getIndex()));
            }
        });

        if (vehicle.getRoute().get(dumpingSiteIndex + 1).isDepot()) {
            Node currentNode = dumpingSite;
            vehicle.getRoute().remove(currentNode);
            currentNode = vehicle.getRoute().get(dumpingSiteIndex - 1);
            while (!currentNode.isDumpingSite()) {
                nodesToSwap.add(currentNode);
                vehicle.getRoute().remove(currentNode);
                dumpingSiteIndex--;
                currentNode = vehicle.getRoute().get(dumpingSiteIndex - 1);
            }
        } else {
            int maximumCapacity = vehicle.getMaximumCapacity();
            int startingIndex = nodeSwapList.indexOf(nodeSwap) == 0 ? 1 : nodeSwapList.get(nodeSwapList.indexOf(nodeSwap) - 1).getIndex();
            float overallQuantity = 0;
            for (int i = startingIndex; i < nodeSwapList.get(randomIndex + 1).getIndex(); i++) {
                if(i == vehicle.getRoute().size()) {
                    System.out.println("");
                }
                Node node = vehicle.getRoute().get(i);
                if (!node.isDumpingSite()) {
                    overallQuantity += node.getQuantity();
                }
            }
            vehicle.getRoute().remove(dumpingSite);
            int numberOfNodesRemoved = 0;
            while (overallQuantity > maximumCapacity) {
                Node currentNode = vehicle.getRoute().get(dumpingSiteIndex - 1);
                overallQuantity -= currentNode.getQuantity();
                nodesToSwap.add(currentNode);
                vehicle.getRoute().remove(currentNode);
                numberOfNodesRemoved++;
                if (numberOfNodesRemoved % 2 == 0) {
                    dumpingSiteIndex--;
                }
            }
        }

        LocalTime endTime = LocalTime.now();
        logger.log("worstRemoval ended at: " + endTime + ", took " + startTime.until(endTime, ChronoUnit.SECONDS) + " seconds");
    }

    private void swapDisposal(Data data, List<Node> nodesToSwap, Logger logger) {
        List<NodeSwap> nodeSwapList = new ArrayList<>();
        List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList());
        for (Vehicle vehicle : feasibleVehicles) {
            for (int i = 0; i < vehicle.getRoute().size(); i++) {
                Node node = vehicle.getRoute().get(i);
                if (node.isDumpingSite()) {
                    NodeSwap nodeSwap = new NodeSwap(node, vehicle, 0, i, false);
                    nodeSwapList.add(nodeSwap);
                }
            }
        }

        int randomIndex = random.nextInt(nodeSwapList.size());
        NodeSwap nodeSwap = nodeSwapList.get(randomIndex);
        Vehicle vehicle = nodeSwap.getVehicle();
        Node dumpingSite = nodeSwap.getNode();
        int dumpingSiteIndex = nodeSwap.getIndex();

        List<Node> disposalSitesToSwapWith = data.getNodeList().stream().filter(disposalSite -> disposalSite.isDumpingSite() && (int)disposalSite.getId() != dumpingSite.getId()).collect(Collectors.toList());
        randomIndex = random.nextInt(disposalSitesToSwapWith.size());
        Node disposalSiteToSwapWith = disposalSitesToSwapWith.get(randomIndex);

        vehicle.getRoute().set(dumpingSiteIndex, disposalSiteToSwapWith);
        float disposalTimeEnd = disposalSiteToSwapWith.getTimeEnd();
        float arrivalTimeAtPreviousNode, arrivalTimeAtNextNode, travelDistance, serviceTime, arrivalTimeAtDisposalSite;
        Node previousNode, nextNode;

        while (true) {
            previousNode = vehicle.getRoute().get(dumpingSiteIndex - 1);
            arrivalTimeAtPreviousNode = vehicle.getArrivalTimes().get(dumpingSiteIndex - 1);
            serviceTime = previousNode.getServiceTime();
            travelDistance = data.getDistanceBetweenNode(previousNode, disposalSiteToSwapWith);
            arrivalTimeAtDisposalSite = arrivalTimeAtPreviousNode + serviceTime + travelDistance;
            if(arrivalTimeAtDisposalSite <= disposalTimeEnd) {
                vehicle.getArrivalTimes().set(dumpingSiteIndex, Math.max(arrivalTimeAtDisposalSite, disposalSiteToSwapWith.getTimeStart()));
                updateArrivalTimes(data);
                break;
            }
            nodesToSwap.add(previousNode);
            vehicle.getArrivalTimes().remove(dumpingSiteIndex - 1);
            vehicle.getRoute().remove(previousNode);
            dumpingSiteIndex--;
        }
        for(int i = dumpingSiteIndex + 1; i < vehicle.getRoute().size() - 1; i++) {
            nextNode = vehicle.getRoute().get(i);
            serviceTime = disposalSiteToSwapWith.getServiceTime();
            travelDistance = data.getDistanceBetweenNode(disposalSiteToSwapWith, nextNode);
            arrivalTimeAtNextNode = arrivalTimeAtDisposalSite + serviceTime + travelDistance;
            if(arrivalTimeAtNextNode <= nextNode.getTimeEnd()) {
                vehicle.getArrivalTimes().set(i, Math.max(arrivalTimeAtNextNode, nextNode.getTimeStart()));
                updateArrivalTimes(data);
                continue;
            }
            nodesToSwap.add(nextNode);
            vehicle.getArrivalTimes().remove(i);
            vehicle.getRoute().remove(nextNode);
            i--;
        }
    }

    private void insertDisposal(Data data, List<Node> nodesToSwap, Logger logger) {
        List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList());
        int randomIndex = random.nextInt(feasibleVehicles.size());
        Vehicle vehicleToInsertInto = feasibleVehicles.get(randomIndex);
        List<Node> disposalSites = vehicleToInsertInto.getRoute().stream().filter(Node::isDumpingSite).collect(Collectors.toList());
        Node dumpingSiteToInsertAfter = disposalSites.get(disposalSites.size() - 1);

        List<Node> disposalSitesToSwapWith = data.getNodeList().stream().filter(disposalSite -> disposalSite.isDumpingSite() && (int)disposalSite.getId() != dumpingSiteToInsertAfter.getId()).collect(Collectors.toList());
        randomIndex = random.nextInt(disposalSitesToSwapWith.size());
        Node disposalSiteToInsert = disposalSitesToSwapWith.get(randomIndex);

        int index = vehicleToInsertInto.getRoute().size(); // lista merete, ezert indexbound lenne ha erre hivatkozunk de mivel beszurjuk index - 1-re a nodeot ezert beszuras utan jo lesz
        vehicleToInsertInto.getRoute().add(vehicleToInsertInto.getRoute().size() - 1, disposalSiteToInsert);

        Node currentNode;
        vehicleToInsertInto.getArrivalTimes().add(index, (float)0);
        float arriveTimeAtPreviousNode,
                serviceTimeAtPreviousNode,
                travelDistance;
        while (true) {
            currentNode = vehicleToInsertInto.getRoute().get(index - 1);
            arriveTimeAtPreviousNode = vehicleToInsertInto.getArrivalTimes().get(index - 1);
            serviceTimeAtPreviousNode = currentNode.getServiceTime();
            travelDistance = data.getDistanceBetweenNode(currentNode, disposalSiteToInsert);
            if(arriveTimeAtPreviousNode + serviceTimeAtPreviousNode + travelDistance <= disposalSiteToInsert.getTimeEnd()) {
                vehicleToInsertInto.getArrivalTimes().set(index, arriveTimeAtPreviousNode + serviceTimeAtPreviousNode + travelDistance);
                break;
            }
            vehicleToInsertInto.getArrivalTimes().remove(index - 1);
            nodesToSwap.add(currentNode);
            vehicleToInsertInto.getRoute().remove(currentNode);
            index--;
        }
    }

    private float getDataValue(Data data) {
        float overallDistance = 0;
        for (Vehicle vehicle : data.getFleet()) {
            float distance = vehicle.calculateTravelDistance(data);
            overallDistance += distance;
        }
        return overallDistance;
    }
}
