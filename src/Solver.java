import data.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Solver {

    private final List<Data> dataList;
    private final Random random;
    private final List<String> hashes;
    private final Constants CONSTANTS;
    private final Heuristics heuristics;

    public Solver(List<Data> dataList) {
        this.dataList = dataList;
        this.random = new Random();
        this.hashes = new ArrayList<>();
        this.CONSTANTS = new Constants();
        this.heuristics = new Heuristics(this);
    }

    public void initGreedy(Data data, Logger logger) {
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
        float
                currentTime,
                serviceTime,
                travelTime,
                quantity;
        Node
                currentNode = data.getDepotNode(),
                dumpingSite,
                nextNode;
        Vehicle currentVehicle = data.getFleet().get(0);
        StringBuilder currentVehicleRouteStringBuilder;

        currentVehicle.getRoute().add(currentNode);
        currentVehicle.setCurrentTime((float) currentNode.getTimeStart());
        currentVehicle.getArrivalTimes().add((float) currentNode.getTimeStart());

        while (data.hasMoreUnvisitedNodes()) {
            nextNode = data.findNextNode(currentVehicle, currentNode);
            if (!nextNode.isNullNode()) {
                currentTime = currentVehicle.getCurrentTime();
                travelTime = data.getDistanceBetweenNode(currentNode, nextNode);
                serviceTime = currentNode.getServiceTime();
                quantity = nextNode.getQuantity();
                if(!currentNode.isDepot()) {
                    currentVehicle.setCurrentTime(currentTime + serviceTime + travelTime);
                } else {
                    currentVehicle.setCurrentTime(Math.max(currentTime + serviceTime + travelTime, nextNode.getTimeStart()));
                }
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

        for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() == 0 && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
            Node depotNode = data.getDepotNode();
            Node dump = data.getNearestDumpingSiteNode(vehicle, depotNode);
            vehicle.getRoute().add(depotNode);
            vehicle.getRoute().add(dump);
            vehicle.getRoute().add(depotNode);
            vehicle.getArrivalTimes().add((float) depotNode.getTimeStart());
            vehicle.getArrivalTimes().add(vehicle.getArrivalTimes().get(0) + depotNode.getServiceTime() + data.getDistanceBetweenNode(depotNode, dump));
            vehicle.getArrivalTimes().add(vehicle.getArrivalTimes().get(1) + dump.getServiceTime() + data.getDistanceBetweenNode(dump, depotNode));
        }

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = checkForValidity(data, vehicle, true);
            if(!valid) {
                System.out.println("GREEDY NEMJO HUJUJUJ");
            }
        }

        LocalTime endGreedy = LocalTime.now();
        long endGreedyNano = System.nanoTime();

        logger.log("Greedy initialization ended at " + endGreedy);
        logger.log("Greedy took " + ((endGreedyNano - startGreedyNano) * 1e-9) + " seconds.");
        logger.emptyLine();

        float travelDistance, sumTravelDistance = 0;
        int numberOfCustomers;

        for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() || vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
            travelDistance = vehicle.calculateTravelDistance(data);
            sumTravelDistance += travelDistance;
            numberOfCustomers = (int) vehicle.getRoute().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count();
            currentVehicleRouteStringBuilder = new StringBuilder("Vehicle " + vehicle.getId() + "'s service time: "
                    + travelDistance + " with " + numberOfCustomers + " customers.");
            logger.log(currentVehicleRouteStringBuilder.toString());
        }
        logger.log("Total travel distance: " + sumTravelDistance);
        logger.log("_data: " + data.getInfo());
        logger.log("_greedyDistance: " + sumTravelDistance);
        logger.log("_vehicleCountG: " + data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty()).count());
        logger.emptyLine();

        for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty()).collect(Collectors.toList())) {
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

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isPenaltyVehicle() && !vehicle.isEmpty()).collect(Collectors.toList())){
            boolean valid = checkForValidity(data, vehicle, false);
            if(!valid) {
                System.out.println("Vehicle " + vehicle.getId() + " is invalid!");
            }
        }

        logger.emptyLine();
        logger.log(CONSTANTS.getDividerString());
        //System.out.println("greedy finished");
    }

    public void ALNS(Data data, Logger logger) {

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

        HeuristicWeights heuristicWeights = new HeuristicWeights();

        data.destroyInfo();

        Data
                bestData = new Data(data),
                currentData;
        int
                customerNodeCount = (int) (data.getNodeList().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count() * 0.4),
                numberOfSteps = 1,
                numberOfNodesToSwap,
                noBetterSolutionFound = 0,
                score = 0;
        float
                bestValue = getDataValue(bestData),
                currentValue,
                delta,
                newValue,
                T = calculateInitialTemperature(data, CONSTANTS.getW());
        boolean
                printSwapInfo = false;
        List<Node>
                nodesToSwap;
        List<Float>
                valueList = new ArrayList<>();
        List<HeuristicWeights>
                heuristicWeightsList = new ArrayList<>();
        String
                hashCode;

        valueList.add(bestValue);
        heuristicWeightsList.add(heuristicWeights);

        while (numberOfSteps < 25000 && noBetterSolutionFound < 2000) {

            logger.log("Iteration " + numberOfSteps);
            iterationStart = System.nanoTime();

            //System.out.println("Iteration " + numberOfSteps);
            //System.out.println(numberOfSteps + " of " + data.getInfo() + ", not better solution found in " + noBetterSolutionFound);

            currentData = new Data(data);
            currentValue = getDataValue(currentData);
            logger.log("Current data value: " + currentValue);

            nodesToSwap = new ArrayList<>();
            numberOfNodesToSwap = 4 + (int)(Math.random() * (Math.min(((int)(customerNodeCount * 0.4) - 4), 100) + 1));

            heuristics.destroyNodes(currentData, numberOfNodesToSwap, nodesToSwap, heuristicWeights, logger, printSwapInfo);

            updateArrivalTimes(currentData);

            heuristics.repairNodes(currentData, nodesToSwap, heuristicWeights, logger, printSwapInfo);
            int num = 0;
            for(Vehicle vehicle : data.getFleet()) {
                for(Node node : vehicle.getRoute()) {
                    if(node.customerNode()) {
                        num++;
                    }
                }
            }
            if(num != data.getMatrix().length - 2) {
                System.out.println("Customer number count ERROR");
            }
            newValue = getDataValue(currentData);

            for(Vehicle vehicle : data.getFleet()) {
                if(vehicle.isPenaltyVehicle()) {
                    if(vehicle.getRoute().size() > 0) {
                        //System.out.println("Penaltyvehicle has customers on it");
                    }
                }
            }

            for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isPenaltyVehicle() && !vehicle.isEmpty()).collect(Collectors.toList())) {
                boolean valid = checkForValidity(data, vehicle, false);
                if (!valid) {
                    System.out.println("Vehicle " + vehicle.getId() + " is invalid!");
                }
            }

                //System.out.println(newValue);
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

                valueList.add(newValue);

            } else if (Math.exp(-1 * (delta) / T) > Math.random()) {

                if (!hashes.contains(hashCode)) {
                    score = CONSTANTS.getSIGMA_3();
                    hashes.add(hashCode);
                }

                data = currentData;
                updateArrivalTimes(data);
                logger.log("Solution accepted by chance");

                valueList.add(newValue);

            }

            if (newValue < bestValue) {

                noBetterSolutionFound = 0;

                score = CONSTANTS.getSIGMA_1();
                hashes.add(hashCode);

                bestValue = newValue;
                bestData = new Data(currentData);
                updateArrivalTimes(bestData);
                logger.log("New best solution found");

            } else {
                noBetterSolutionFound++;
            }

            updateHeuristicInformation(heuristicWeights, score, logger);
            //update information about performance of destroy and repair methods
            if (numberOfSteps % 100 == 0) {
                updateWeights(heuristicWeights, CONSTANTS.getR());
                heuristicWeightsList.add(new HeuristicWeights(heuristicWeights));
            }
            numberOfSteps++;
            T *= 0.995;
            iterationEnd = System.nanoTime();
            logger.log("Iteration took " + ((iterationEnd - iterationStart) * 1e-9) + " seconds");
            logger.emptyLine();
            logger.emptyLine();
        }

        for(Vehicle vehicle : data.getFleet()) {
            if(vehicle.isPenaltyVehicle()) {
                if(vehicle.getRoute().size() > 0) {
                    System.out.println("Penalty vehicle has customers on it");
                }
            }
        }

        // TODO: do something with the best found solution (bestData)

        LocalTime endALNS = LocalTime.now();
        long endALNSNano = System.nanoTime();

        logger.emptyLine();
        logger.log("ALNS ended at " + endALNS);
        logger.log("ALNS took " + ((endALNSNano - startALNSNano) * 1e-9) + " seconds.");
        logger.emptyLine();

        vehicleAndHeuristicInformation(bestData, logger, heuristicWeightsList, valueList, numberOfSteps);

        logger.emptyLine();
        logger.log(CONSTANTS.getDividerString());
    }

    public void updateArrivalTimes(Data data) {
        // TODO: update only those vehicles arrival times on which there has been change
        // TODO: for loop
        //List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList());
        List<Vehicle> feasibleVehicles = new ArrayList<>();
        for (Vehicle vehicle : data.getFleet()) if (vehicle.getRoute().size() > 3) feasibleVehicles.add(vehicle);
        for (Vehicle vehicle : feasibleVehicles) {
            vehicle.getArrivalTimes().clear();
            Node currentNode = vehicle.getRoute().get(0), previousNode;
            vehicle.getArrivalTimes().add((float) currentNode.getTimeStart());
            float arrivalTime = currentNode.getTimeStart(), serviceTime = currentNode.getServiceTime(), travelTime;

            for (int i = 1; i < vehicle.getRoute().size(); i++) {
                previousNode = currentNode;
                currentNode = vehicle.getRoute().get(i);
                travelTime = data.getDistanceBetweenNode(previousNode, currentNode);
                arrivalTime = Math.max(arrivalTime + serviceTime + travelTime, currentNode.getTimeStart());
                vehicle.getArrivalTimes().add(arrivalTime);
                serviceTime = currentNode.getServiceTime();
            }
        }
    }

    public void updateArrivalTimesForVehicle(Vehicle vehicle, Data data) {
        vehicle.getArrivalTimes().clear();
        Node currentNode = vehicle.getRoute().get(0), previousNode;
        vehicle.getArrivalTimes().add((float) currentNode.getTimeStart());
        float arrivalTime = currentNode.getTimeStart(), serviceTime = currentNode.getServiceTime(), travelTime;

        for (int i = 1; i < vehicle.getRoute().size(); i++) {
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
        float newInsertDisposalWeight = heuristicWeights.getInsertDisposalWeight() * (1 - r)
                + r * (heuristicWeights.getInsertDisposalScore() / (float) heuristicWeights.getTimesUsedInsertDisposal());
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
        heuristicWeights.setInsertDisposalWeight(newInsertDisposalWeight);
        heuristicWeights.setGreedyInsertWeight(newGreedyInsertWeight);
        heuristicWeights.setRegret_2_InsertWeight(newRegret_2_InsertWeight);
        heuristicWeights.setRegret_3_InsertWeight(newRegret_3_InsertWeight);
        heuristicWeights.setRegret_K_InsertWeight(newRegret_K_InsertWeight);
        heuristicWeights.setRandomRemovalScore(0);
        heuristicWeights.setWorstRemovalScore(0);
        heuristicWeights.setRelatedRemovalScore(0);
        heuristicWeights.setDeleteDisposalScore(0);
        heuristicWeights.setSwapDisposalScore(0);
        heuristicWeights.setInsertDisposalScore(0);
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
            case 6:
                heuristicWeights.setInsertDisposalScore(heuristicWeights.getInsertDisposalScore() + score);
                heuristicWeights.setTimesUsedInsertDisposal(heuristicWeights.getTimesUsedInsertDisposal() + 1);
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

    public boolean checkForValidity(Data data, Vehicle vehicle, boolean asd) {
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
            float serviceTimeAtPreviousNode = previousNode.getServiceTime();
            float travelTime = data.getDistanceBetweenNode(previousNode, currentNode);
            if (currentNode.isDumpingSite()) {
                vehicle.setCapacity((float) 0);
            } else if (!currentNode.isDepot()) {
                quantity = currentNode.getQuantity();
                vehicle.setCapacity(vehicle.getCapacity() + quantity);
            }

            if (vehicle.getCapacity() > vehicle.getMaximumCapacity()) {
                if(asd) System.out.println("Capacity ERROR between node " + (i - 1) + " and " + i);
                return false;
            }

            if (!data.timeWindowCheck(currentTime + serviceTimeAtPreviousNode + travelTime, currentNode)) {
                if(asd) {
                    System.out.println((currentTime + serviceTimeAtPreviousNode + travelTime) + "," + currentNode.getTimeEnd());
                    System.out.println("TW ERROR between node " + (i - 1) + " and " + i);
                }
                return false;
            }
            currentTime = Math.max(currentTime + serviceTimeAtPreviousNode + travelTime, currentNode.getTimeStart());
            vehicle.setCurrentTime(currentTime);
            previousNode = currentNode;
        }
        return true;
    }

    public float getDataValue(Data data) {
        float overallDistance = 0;
        for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty()).collect(Collectors.toList())) {
            float distance = vehicle.calculateTravelDistance(data);
            overallDistance += distance;
        }
        return overallDistance;
    }

    private void vehicleAndHeuristicInformation(Data bestData, Logger logger, List<HeuristicWeights> heuristicWeightsList, List<Float> actualValues, int numberOfSteps) {
        float travelDistance, sumTravelDistance = 0;
        int numberOfCustomers;

        StringBuilder stringBuilder;
        for (Vehicle vehicle : bestData.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3 || vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
            travelDistance = vehicle.calculateTravelDistance(bestData);
            sumTravelDistance += travelDistance;
            numberOfCustomers = (int) vehicle.getRoute().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count();
            stringBuilder = new StringBuilder("Vehicle " + vehicle.getId() + "'s service time: "
                    + travelDistance + " with " + numberOfCustomers + " customers.");
            logger.log(stringBuilder.toString());
        }
        logger.log("Total travel distance: " + sumTravelDistance);

        //System.out.println(sumTravelDistance);
        logger.log("_ALNSDistance: " + sumTravelDistance);
        logger.log("_iterations: " + numberOfSteps);
        logger.log("_vehicleCountA: " + bestData.getFleet().stream().filter(vehicle -> !vehicle.isEmpty()).count());
        stringBuilder = new StringBuilder("_values: ");
        for(Float value : actualValues) stringBuilder.append(value).append(",");
        logger.log(stringBuilder.toString());
        logger.emptyLine();

        int customerNumber = 0;
        for (Vehicle vehicle : bestData.getFleet().stream().filter(vehicle -> !vehicle.isEmpty()).collect(Collectors.toList())) {
            stringBuilder = new StringBuilder("$Vehicle " + vehicle.getId() + "'s route: ");
            for (Node node : vehicle.getRoute()) {
                String str;
                if (node.isDepot()) {
                    str = "DP0";
                } else if (node.isDumpingSite()) {
                    str = "DS" + node.getId();
                } else {
                    str = node.getId().toString();
                    customerNumber++;
                }
                stringBuilder.append(str).append(" ");
            }
            logger.log(stringBuilder.toString());
        }
        logger.log("Number of customers on all vehicles: " + customerNumber);

        logger.emptyLine();

        stringBuilder = new StringBuilder("Values in each iteration: ");
        for(Float value : actualValues) stringBuilder.append(value).append(" ");
        logger.log(stringBuilder.toString());
        logger.emptyLine();

        logger.log("Weights of each heuristic during:");
        StringBuilder
                rndR = new StringBuilder("randomRemove: "),
                wR = new StringBuilder("worstRemove: "),
                relR = new StringBuilder("relatedRemove: "),
                dD = new StringBuilder("deleteDisposal: "),
                sD = new StringBuilder("swapDisposal: "),
                iD = new StringBuilder("insertDisposal: "),
                gI = new StringBuilder("greedyInsert: "),
                r2I = new StringBuilder("regret_2_Insert: "),
                r3I = new StringBuilder("regret_3_Insert: "),
                rKI = new StringBuilder("regret_K_Insert: ");

        for(HeuristicWeights heuristicWeight : heuristicWeightsList) {
            rndR.append(heuristicWeight.getRandomRemovalWeight()).append(" ");
            wR.append(heuristicWeight.getWorstRemovalWeight()).append(" ");
            relR.append(heuristicWeight.getRelatedRemovalWeight()).append(" ");
            dD.append(heuristicWeight.getDeleteDisposalWeight()).append(" ");
            sD.append(heuristicWeight.getSwapDisposalWeight()).append(" ");
            iD.append(heuristicWeight.getInsertDisposalWeight()).append(" ");
            gI.append(heuristicWeight.getGreedyInsertWeight()).append(" ");
            r2I.append(heuristicWeight.getRegret_2_InsertWeight()).append(" ");
            r3I.append(heuristicWeight.getRegret_3_InsertWeight()).append(" ");
            rKI.append(heuristicWeight.getRegret_K_InsertWeight()).append(" ");
        }

        logger.log(rndR.toString());
        logger.log(wR.toString());
        logger.log(relR.toString());
        logger.log(dD.toString());
        logger.log(sD.toString());
        logger.log(iD.toString());
        logger.log(gI.toString());
        logger.log(r2I.toString());
        logger.log(r3I.toString());
        logger.log(rKI.toString());

    }
}
