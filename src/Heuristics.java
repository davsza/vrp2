import data.*;

import javax.xml.crypto.NodeSetData;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Heuristics {

    private final Random random;
    private final Solver solver;
    private final Constants CONSTANTS;

    public Heuristics(Solver solver) {
        this.random = new Random();
        this.solver = solver;
        this.CONSTANTS = new Constants();
    }

    public void repairNodes(Data data, List<Node> nodesToSwap, HeuristicWeights heuristicWeights, Logger logger, boolean printSwapInfo) {
        LocalTime startTime = LocalTime.now();
        long repairStart = System.nanoTime();
        logger.log("Repairing nodes started at: " + startTime);
        logger.log("Inserting " + nodesToSwap.size() + " nodes");

        float sumOf = heuristicWeights.sumOfRepair();
        float greedyWeight = heuristicWeights.getGreedyInsertWeight() / sumOf;
        float regret_2_Weight = heuristicWeights.getRegret_2_InsertWeight() / sumOf;
        float regret_3_Weight = heuristicWeights.getRegret_3_InsertWeight() / sumOf;
        float regret_K_Weight = heuristicWeights.getRegret_K_InsertWeight() / sumOf;
        float randomValue = random.nextFloat();
        if (randomValue < greedyWeight) {
            heuristicWeights.setCurrentInsert(1);
            logger.log("Insert method: greedyInsert");
            System.out.println("Insert method: greedyInsert");
            if(printSwapInfo) System.out.println("gi");
            greedyInsert(data, nodesToSwap, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight) {
            heuristicWeights.setCurrentInsert(2);
            logger.log("Insert method: regretInsert_2");
            System.out.println("Insert method: regretInsert_2");
            if(printSwapInfo) System.out.println("ri2");
            regretInsert(data, nodesToSwap, 2, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight + regret_3_Weight) {
            heuristicWeights.setCurrentInsert(3);
            logger.log("Insert method: regretInsert_3");
            System.out.println("Insert method: regretInsert_3");
            if(printSwapInfo) System.out.println("ri3");
            regretInsert(data, nodesToSwap, 3, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight + regret_3_Weight + regret_K_Weight) {
            heuristicWeights.setCurrentInsert(4);
            logger.log("Insert method: regretInsert_k");
            System.out.println("Insert method: regretInsert_k");
            if(printSwapInfo) System.out.println("rik");
            // TODO: for loop
            int customerNodeCount = (int) data.getNodeList().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count();
            regretInsert(data, nodesToSwap, customerNodeCount, logger);
        } else {
            System.out.println("Kurva kicsi valószínűségs");
            System.out.println("Random: " + randomValue + ", sum of: " + (greedyWeight + regret_2_Weight + regret_3_Weight + regret_K_Weight));
        }

        LocalTime endTime = LocalTime.now();
        long repairEnd = System.nanoTime();
        logger.log("Repairing nodes ended at: " + endTime + ", took " + ((repairEnd - repairStart) * 1e-9) + " seconds");

    }

    private void regretInsert(Data data, List<Node> nodesToSwap, int p, Logger logger) {

        LocalTime startTime = LocalTime.now();
        long startNanoTime = System.nanoTime();
        logger.log("regretInsert_" + (p == 2 || p == 3 ? p : "k") + " started at: " + startTime);

        float
                bestDiff,
                currentValue,
                diff,
                initialValue = solver.getDataValue(data);
        int
                indexToInsert;
        long
                totalInsertValidityCheck = 0,
                startNano,
                endNano;
        List<NodeSwap>
                nodeSwapList = new ArrayList<>();
        Node
                nodeToInsert;
        NodeSwap
                currentNodeSwap;
        Vehicle
                vehicleToInsertInto,
                penaltyVehicle = data.getPenaltyVehicle();

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle, false);
            if(!valid) {
                System.out.println("Invalid vehicle at the start of regret");
            }
        }

        for (Node nodesToInsert : nodesToSwap) {
            NodeSwap customerNodeSwap = new NodeSwap(nodesToInsert);
            boolean checkedEmptyVehicle = false;
            for (Vehicle vehicle : data.getFleet()) {
                bestDiff = Float.MAX_VALUE;
                if (vehicle.isPenaltyVehicle()) {
                    currentNodeSwap = new NodeSwap(nodesToInsert);
                    diff = 2 * data.getMaximumTravelDistance();
                    currentNodeSwap.setVehicle(vehicle);
                    currentNodeSwap.setIndex(vehicle.getRoute().size());
                    currentNodeSwap.setFoundVehicleForNodeToInsert(true);
                    currentNodeSwap.setNode(nodesToInsert);
                    currentNodeSwap.setValue(diff);
                    customerNodeSwap.getRegretNodeSwapList().add(currentNodeSwap);
                    continue;
                }
                if (checkedEmptyVehicle) continue;
                checkedEmptyVehicle = vehicle.isEmpty();
                int vehicleRouteSize = vehicle.getRoute().size();
                currentNodeSwap = new NodeSwap(nodesToInsert);
                for (int i = 1; i < vehicleRouteSize - 1; i++) {

                    float previousNodeArrivalTime = vehicle.getArrivalTimes().get(i - 1);
                    Node previousNode = vehicle.getRoute().get(i - 1);
                    Node nextNode = vehicle.getRoute().get(i);
                    float serviceTimeAtPreviousNode = previousNode.getServiceTime();
                    float travelDistance = data.getDistanceBetweenNode(previousNode, nodesToInsert);
                    float arrivalTimeAtNode = previousNodeArrivalTime + serviceTimeAtPreviousNode + travelDistance;

                    if (arrivalTimeAtNode > nodesToInsert.getTimeEnd()) {
                        break;
                    }

                    if (nodesToInsert.getTimeStart() > nextNode.getTimeEnd()) {
                        continue;
                    }

                    float distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nextNode);

                    vehicle.getRoute().add(i, nodesToInsert);

                    float distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, nodesToInsert) + data.getDistanceBetweenNode(nodesToInsert, nextNode);
                    startNano = System.nanoTime();
                    boolean valid = solver.checkForValidity(data, vehicle, false);
                    endNano = System.nanoTime();
                    totalInsertValidityCheck += (endNano - startNano);

                    if (valid) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if (diff < bestDiff) {
                            bestDiff = diff;
                            currentNodeSwap.setVehicle(vehicle);
                            currentNodeSwap.setIndex(i);
                            currentNodeSwap.setFoundVehicleForNodeToInsert(true);
                            currentNodeSwap.setNode(nodesToInsert);
                            currentNodeSwap.setValue(diff);
                        }
                    }
                    vehicle.getRoute().remove(nodesToInsert);

                }
                if (currentNodeSwap.getVehicle() != null) {
                    customerNodeSwap.getRegretNodeSwapList().add(currentNodeSwap);
                }
            }
            customerNodeSwap.sortRegretList();
            nodeSwapList.add(customerNodeSwap);
        }

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle, false);
            if(!valid) {
                System.out.println("Invalid vehicle after regret values calculated");
            }
        }

        int it = 1;
        while (nodesToSwap.size() > 0) {

            nodeSwapList.sort(new Comparator<NodeSwap>() {
                @Override
                public int compare(NodeSwap o1, NodeSwap o2) {
                    return (Float.compare(o1.getNumberOfFeasibleVehiclesToInsertInto(), o2.getNumberOfFeasibleVehiclesToInsertInto()));
                }
            });
            int leastFeasibleVehicleInsert = nodeSwapList.get(0).getNumberOfFeasibleVehiclesToInsertInto();

            NodeSwap bestNodeSwap = new NodeSwap();

            if (leastFeasibleVehicleInsert < p) {
                List<NodeSwap> feasibleNodeSwaps = nodeSwapList
                        .stream()
                        .filter(nodeSwap -> nodeSwap.getNumberOfFeasibleVehiclesToInsertInto() == leastFeasibleVehicleInsert)
                        .collect(Collectors.toList());
                float worst = 0;
                float bestDataValue = Float.MAX_VALUE;
                for (NodeSwap nodeSwap : feasibleNodeSwaps) {
                    // TODO: itt hal meg, indexbound
                    float bestValue = nodeSwap.getRegretNodeSwapList().get(0).getValue();//nodeSwap.getValues().get(0);
                    diff = nodeSwap.getRegretSum(leastFeasibleVehicleInsert);//nodeSwap.getValues().get(nodeSwap.getValues().size() - 1);
                    if (diff > worst) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = bestValue;
                    } else if (diff == worst && nodeSwap.getRegretNodeSwapList().get(0).getValue() < bestDataValue) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = bestValue;
                    }
                }
            } else {
                float worst = 0;
                float bestDataValue = Float.MAX_VALUE;
                for (NodeSwap nodeSwap : nodeSwapList) {
                    float bestValue = nodeSwap.getRegretNodeSwapList().get(0).getValue();//nodeSwap.getValues().get(0);
                    diff = nodeSwap.getRegretSum(p);//nodeSwap.getValues().get(p - 1);
                    if (diff > worst) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = bestValue;
                    } else if (diff == worst && nodeSwap.getRegretNodeSwapList().get(0).getValue() < bestDataValue) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = bestValue;
                    }
                }
            }

            // TODO: itt hal meg, nullptr
            vehicleToInsertInto = bestNodeSwap.getRegretNodeSwapList().get(0).getVehicle();
            nodeToInsert = bestNodeSwap.getRegretNodeSwapList().get(0).getNode();
            indexToInsert = bestNodeSwap.getRegretNodeSwapList().get(0).getIndex();

            if (bestNodeSwap.getRegretNodeSwapList().get(0).isFoundVehicleForNodeToInsert()) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
                solver.updateArrivalTimesForVehicle(vehicleToInsertInto, data);
            } else {
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
            }
            nodesToSwap.remove(nodeToInsert);
            nodeSwapList.remove(bestNodeSwap);

            for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
                boolean valid = solver.checkForValidity(data, vehicle, false);
                if(!valid) {
                    System.out.println("Invalid vehicle after regret insert");
                }
            }

            it++;

            initialValue += bestNodeSwap.getRegretNodeSwapList().get(0).getValue();

            for (NodeSwap nodeSwap : nodeSwapList) {

                if(vehicleToInsertInto.isPenaltyVehicle()) {
                    break;
                }

                Vehicle finalVehicleToInsertInto = vehicleToInsertInto;
                // olyan nodeswapek, ahol a vehicle megyezik
                List<NodeSwap> nodeSwapsWithSameVehicleList = nodeSwap.getRegretNodeSwapList()
                        .stream()
                        .filter(nodeSwap1 -> nodeSwap1.getVehicle().equals(finalVehicleToInsertInto))
                        .collect(Collectors.toList());

                if (nodeSwapsWithSameVehicleList.size() == 0) continue;

                NodeSwap selectedNodeSwap = nodeSwapsWithSameVehicleList.get(0);
                Node node = nodeSwap.getNode();

                boolean foundBetterValue = false;
                currentNodeSwap = null;

                /*if (selectedNodeSwap.getIndex() == indexToInsert) {*/
                    bestDiff = Float.MAX_VALUE;
                    selectedNodeSwap.setFoundVehicleForNodeToInsert(false);
                    for (int i = 1; i < vehicleToInsertInto.getRoute().size() - 1; i++) {

                        float previousNodeArrivalTime = vehicleToInsertInto.getArrivalTimes().get(i - 1);
                        Node previousNode = vehicleToInsertInto.getRoute().get(i - 1);
                        Node nextNode = vehicleToInsertInto.getRoute().get(i);
                        float serviceTimeAtPreviousNode = previousNode.getServiceTime();
                        float travelDistance = data.getDistanceBetweenNode(previousNode, node);
                        float arrivalTimeAtNode = previousNodeArrivalTime + serviceTimeAtPreviousNode + travelDistance;

                        if (arrivalTimeAtNode > node.getTimeEnd()) {
                            break;
                        }

                        if (node.getTimeStart() > nextNode.getTimeEnd()) {
                            continue;
                        }

                        float distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nextNode);

                        vehicleToInsertInto.getRoute().add(i, node);

                        float distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, node) + data.getDistanceBetweenNode(node, nextNode);

                        boolean valid = solver.checkForValidity(data, vehicleToInsertInto, false);

                        if (valid) {
                            currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                            diff = currentValue - initialValue;
                            if (diff < bestDiff) {
                                bestDiff = diff;
                                selectedNodeSwap.setIndex(i);
                                selectedNodeSwap.setFoundVehicleForNodeToInsert(true);
                                selectedNodeSwap.setValue(diff);
                            }
                        }
                        vehicleToInsertInto.getRoute().remove(node);
                    }

                    if (!selectedNodeSwap.isFoundVehicleForNodeToInsert()) {
                        nodeSwap.getRegretNodeSwapList().remove(selectedNodeSwap);
                    }

                /*} else {
                    //selected.getvalue-nál jobb e ha az i-1,i vagy i,i+1 koze inserteljuk
                    Node previousNode = vehicleToInsertInto.getRoute().get(indexToInsert - 1);
                    Node nextNode = vehicleToInsertInto.getRoute().get(indexToInsert + 1);

                    float distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nodeToInsert);

                    vehicleToInsertInto.getRoute().add(indexToInsert, node);

                    float distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, node) + data.getDistanceBetweenNode(node, nodeToInsert);

                    boolean valid = solver.checkForValidity(data, vehicleToInsertInto, false);

                    if (valid) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if (diff < selectedNodeSwap.getValue()) {
                            currentNodeSwap = new NodeSwap(node, vehicleToInsertInto, diff, indexToInsert, true);
                            foundBetterValue = true;
                        }
                    }

                    vehicleToInsertInto.getRoute().remove(indexToInsert);

                    //beszuras a beszurt node moge
                    distanceBetweenNodesToInsert = data.getDistanceBetweenNode(nodeToInsert, nextNode);

                    vehicleToInsertInto.getRoute().add(indexToInsert + 1, node);

                    distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(nodeToInsert, node) + data.getDistanceBetweenNode(node, nextNode);

                    valid = solver.checkForValidity(data, vehicleToInsertInto, false);

                    if (valid) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if ((diff < selectedNodeSwap.getValue() && currentNodeSwap == null) || (currentNodeSwap != null && diff < currentNodeSwap.getValue())) {
                            currentNodeSwap = new NodeSwap(node, vehicleToInsertInto, diff, indexToInsert + 1, true);
                            foundBetterValue = true;
                        }
                    }

                    vehicleToInsertInto.getRoute().remove(indexToInsert + 1);

                    if (foundBetterValue) {
                        selectedNodeSwap.setVehicle(currentNodeSwap.getVehicle());
                        selectedNodeSwap.setValue(currentNodeSwap.getValue());
                        selectedNodeSwap.setIndex(currentNodeSwap.getIndex());
                    } else if (selectedNodeSwap.getIndex() > indexToInsert) {
                        selectedNodeSwap.setIndex(selectedNodeSwap.getIndex() + 1);
                    }
                }*/
                nodeSwap.sortRegretList();
            }

            for(Vehicle vehicle2 : data.getFleet().stream().filter(vehicle3 -> !vehicle3.isEmpty() && !vehicle3.isPenaltyVehicle()).collect(Collectors.toList())) {
                boolean valid2 = solver.checkForValidity(data, vehicle2, false);
                if(!valid2) {
                    System.out.println("Invalid vehicle after regret values recalculated");
                }
            }

        }

        LocalTime endTime = LocalTime.now();
        long endNanoTime = System.nanoTime();
        logger.log("regretInsert ended at: " + endTime + ", took " + ((endNanoTime - startNanoTime) * 1e-9) + " seconds");
        logger.log("Validating the data took " + (totalInsertValidityCheck * 1e-9) + " seconds");

    }

    private void greedyInsert(Data data, List<Node> nodesToSwap, Logger logger) {

        LocalTime startTime = LocalTime.now();
        logger.log("greedyInsert started at: " + startTime);
        long startNanoTime = System.nanoTime();

        float
                bestDiff,
                currentValue,
                diff,
                initialValue = solver.getDataValue(data);
        int
                indexToInsert;
        long
                totalInsertValidityCheck = 0,
                endNano,
                startNano;
        List<NodeSwap>
                nodeSwapList = new ArrayList<>();
        Node
                nodeToInsert;
        NodeSwap
                currentNodeSwap = null,
                bestNodeSwap;
        Vehicle
                vehicleToInsertInto,
                penaltyVehicle = data.getPenaltyVehicle();

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle, false);
            if(!valid) {
                System.out.println("Invalid vehicle greedy elott");
            }
        }

        for (Node nodesToInsert : nodesToSwap) {
            bestDiff = Float.MAX_VALUE;
            boolean checkedEmptyVehicle = false;
            for (Vehicle vehicle : data.getFleet()) {
                //TODO:
                if (vehicle.isPenaltyVehicle()) {
                    diff = 2 * data.getMaximumTravelDistance();
                    if (diff < bestDiff) {
                        bestDiff = diff;
                        currentNodeSwap = new NodeSwap(nodesToInsert, vehicle, diff, vehicle.getRoute().size(), true);
                    }
                    continue;
                }
                if (checkedEmptyVehicle && vehicle.isEmpty()) continue;
                checkedEmptyVehicle = vehicle.isEmpty();
                for (int i = 1; i < vehicle.getRoute().size() - 1; i++) {
                    float previousNodeArrivalTime = vehicle.getArrivalTimes().get(i - 1);
                    Node previousNode = vehicle.getRoute().get(i - 1);
                    Node nextNode = vehicle.getRoute().get(i);
                    float serviceTimeAtPreviousNode = previousNode.getServiceTime();
                    float travelDistance = data.getDistanceBetweenNode(previousNode, nodesToInsert);
                    float arrivalTimeAtNode = previousNodeArrivalTime + serviceTimeAtPreviousNode + travelDistance;
                    if (arrivalTimeAtNode > nodesToInsert.getTimeEnd()) {
                        break;
                    }
                    if (nodesToInsert.getTimeStart() > nextNode.getTimeEnd()) {
                        continue;
                    }
                    float distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nextNode);
                    vehicle.getRoute().add(i, nodesToInsert);
                    float distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, nodesToInsert) + data.getDistanceBetweenNode(nodesToInsert, nextNode);
                    startNano = System.nanoTime();

                    boolean validSolution = solver.checkForValidity(data, vehicle, false);
                    endNano = System.nanoTime();
                    totalInsertValidityCheck += (endNano - startNano);
                    if (validSolution) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if (diff < bestDiff) {
                            bestDiff = diff;
                            currentNodeSwap = new NodeSwap(nodesToInsert, vehicle, diff, i, true);
                        }
                    }
                    vehicle.getRoute().remove(i);
                }
            }
            nodeSwapList.add(currentNodeSwap);
            /*
            currentNodeSwap.getVehicle().getRoute().add(currentNodeSwap.getIndex(), currentNodeSwap.getNode());
            boolean v = solver.checkForValidity(data, currentNodeSwap.getVehicle());
            if(!v) {
                System.out.println("");
            }
            //System.out.println("Current vehicle's id: " + currentNodeSwap.getVehicle().getId());
            currentNodeSwap.getVehicle().getRoute().remove(currentNodeSwap.getIndex());

             */
        }

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle, false);
            if(!valid) {
                System.out.println("Invalid vehicle greedy ertekeket kiszamolasa utan");
            }
        }

        int it = 1;
        while (nodesToSwap.size() > 0) {

            nodeSwapList.sort(new Comparator<NodeSwap>() {
                @Override
                public int compare(NodeSwap o1, NodeSwap o2) {
                    return (Float.compare(o1.getValue(), o2.getValue()));
                }
            });

            bestNodeSwap = nodeSwapList.get(0);
            vehicleToInsertInto = bestNodeSwap.getVehicle();
            nodeToInsert = bestNodeSwap.getNode();
            indexToInsert = bestNodeSwap.getIndex();

            if (bestNodeSwap.isFoundVehicleForNodeToInsert()) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
                solver.updateArrivalTimesForVehicle(vehicleToInsertInto, data);
            } else {
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
            }
            nodesToSwap.remove(nodeToInsert);
            nodeSwapList.remove(0);

            for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
                boolean valid = solver.checkForValidity(data, vehicle, true);
                if(!valid) {
                    System.out.println("Invalid vehicle greedy beszuras utan");
                }
            }

            it++;

            initialValue += bestNodeSwap.getValue();

            for (NodeSwap nodeSwap : nodeSwapList) {
                // TODO: penalty vehiclere valo beszurasnal senkit nem kell updatelni, skip
                if(vehicleToInsertInto.isPenaltyVehicle()) {
                    break;
                }
                boolean foundBetterValue = false;
                if (nodeSwap.getVehicle().equals(vehicleToInsertInto)) {

                    Node node = nodeSwap.getNode();
                    bestDiff = Float.MAX_VALUE;
                    boolean checkedEmptyVehicle = false;

                    for (Vehicle vehicle : data.getFleet()) {

                        if (vehicle.isPenaltyVehicle()) {
                            diff = 2 * data.getMaximumTravelDistance();
                            if (diff < bestDiff) {
                                bestDiff = diff;
                                currentNodeSwap = new NodeSwap(node, vehicle, diff, vehicle.getRoute().size(), true);
                            }
                            continue;
                        }

                        if (checkedEmptyVehicle && vehicle.isEmpty()) continue;
                        checkedEmptyVehicle = vehicle.isEmpty();

                        for (int i = 1; i < vehicle.getRoute().size() - 1; i++) {

                            float previousNodeArrivalTime = vehicle.getArrivalTimes().get(i - 1);
                            Node previousNode = vehicle.getRoute().get(i - 1);
                            Node nextNode = vehicle.getRoute().get(i);
                            float serviceTimeAtPreviousNode = previousNode.getServiceTime();
                            float travelDistance = data.getDistanceBetweenNode(previousNode, node);
                            float arrivalTimeAtNode = previousNodeArrivalTime + serviceTimeAtPreviousNode + travelDistance;

                            if (arrivalTimeAtNode > node.getTimeEnd()) {
                                break;
                            }

                            if (node.getTimeStart() > nextNode.getTimeEnd()) {
                                continue;
                            }

                            float distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nextNode);

                            vehicle.getRoute().add(i, node);

                            float distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, node) + data.getDistanceBetweenNode(node, nextNode);

                            boolean validSolution = solver.checkForValidity(data, vehicle, false);

                            if (validSolution) {
                                currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                                diff = currentValue - initialValue;
                                if (diff < bestDiff) {
                                    bestDiff = diff;
                                    currentNodeSwap = new NodeSwap(node, vehicle, diff, i, true);
                                }
                            }

                            vehicle.getRoute().remove(i);

                        }
                    }
                    assert currentNodeSwap != null;
                    nodeSwap.setVehicle(currentNodeSwap.getVehicle());
                    nodeSwap.setValue(currentNodeSwap.getValue());
                    nodeSwap.setIndex(currentNodeSwap.getIndex());
                    nodeSwap.setModified(true);

                } else {

                    Node previousNode = vehicleToInsertInto.getRoute().get(indexToInsert - 1);
                    Node nextNode = vehicleToInsertInto.getRoute().get(indexToInsert + 1);
                    Node node = nodeSwap.getNode();

                    //beszuras a beszurt node ele
                    float distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nodeToInsert);

                    vehicleToInsertInto.getRoute().add(indexToInsert, node);

                    float distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, node) + data.getDistanceBetweenNode(node, nodeToInsert);

                    boolean validSolution = solver.checkForValidity(data, vehicleToInsertInto, false);

                    if (validSolution) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if (diff < nodeSwap.getValue()) {
                            currentNodeSwap = new NodeSwap(node, vehicleToInsertInto, diff, indexToInsert, true);
                            foundBetterValue = true;
                        }
                    }

                    vehicleToInsertInto.getRoute().remove(indexToInsert);

                    //beszuras a beszurt node moge
                    distanceBetweenNodesToInsert = data.getDistanceBetweenNode(nodeToInsert, nextNode);

                    vehicleToInsertInto.getRoute().add(indexToInsert + 1, node);

                    distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(nodeToInsert, node) + data.getDistanceBetweenNode(node, nextNode);

                    validSolution = solver.checkForValidity(data, vehicleToInsertInto, false);

                    if (validSolution) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if ((diff < nodeSwap.getValue() && currentNodeSwap == null) || (currentNodeSwap != null && diff < currentNodeSwap.getValue())) {
                            currentNodeSwap = new NodeSwap(node, vehicleToInsertInto, diff, indexToInsert + 1, true);
                            foundBetterValue = true;
                        }
                    }

                    vehicleToInsertInto.getRoute().remove(indexToInsert + 1);

                    if (foundBetterValue) {
                        assert currentNodeSwap != null;
                        nodeSwap.setVehicle(currentNodeSwap.getVehicle());
                        nodeSwap.setValue(currentNodeSwap.getValue());
                        nodeSwap.setIndex(currentNodeSwap.getIndex());
                        nodeSwap.setModified(true);
                        currentNodeSwap = null;
                    }  /*else if (nodeSwap.getVehicle().equals(vehicleToInsertInto) && nodeSwap.getIndex() > indexToInsert) {
                        System.out.println("Elotte utana nem modosult, de az indexem nott");
                        nodeSwap.setIndex(nodeSwap.getIndex() + 1);
                    }*/
                }

            }

            for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
                boolean valid = solver.checkForValidity(data, vehicle, false);
                if(!valid) {
                    System.out.println("Invalid vehicle greedy ertekeket kiszamolasa utan");
                }
            }

        }

        LocalTime endTime = LocalTime.now();
        long endNanoTime = System.nanoTime();
        logger.log("greedyInsert ended at: " + endTime + ", took " + ((endNanoTime - startNanoTime) * 1e-9) + " seconds");
        logger.log("Validating the data took " + (totalInsertValidityCheck * 1e-9) + " seconds");

    }

    public void destroyNodes(Data data, int p, List<Node> nodesToSwap, HeuristicWeights heuristicWeights, Logger logger, boolean printSwapInfo) {
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
            if(printSwapInfo) System.out.println("wr");
            worstRemoval(data, p, nodesToSwap, CONSTANTS.getP_WORST(), logger);
        } else if (randomValue < worstWeight + randomWeight) {
            heuristicWeights.setCurrentRemove(2);
            logger.log("Destroy method: randomRemoval");
            System.out.println("Destroy method: randomRemoval");
            if(printSwapInfo) System.out.println("rr");
            randomRemoval(data, p, nodesToSwap, logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight) {
            heuristicWeights.setCurrentRemove(3);
            logger.log("Destroy method: relatedRemoval");
            System.out.println("Destroy method: relatedRemoval");
            if(printSwapInfo) System.out.println("rel");
            relatedRemoval(data, p, nodesToSwap, CONSTANTS.getPHI(), CONSTANTS.getCHI(), CONSTANTS.getPSI(), CONSTANTS.getP(), logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight + deleteWeight) {
            heuristicWeights.setCurrentRemove(4);
            logger.log("Destroy method: deleteDisposal");
            System.out.println("Destroy method: deleteDisposal");
            if(printSwapInfo) System.out.println("dd");
            deleteDisposal(data, nodesToSwap, logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight + deleteWeight + swapWeight) {
            heuristicWeights.setCurrentRemove(5);
            logger.log("Destroy method: swapDisposal");
            System.out.println("Destroy method: swapDisposal");
            if(printSwapInfo) System.out.println("sd");
            swapDisposal(data, nodesToSwap, logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight + deleteWeight + swapWeight + insertWeight) {
            heuristicWeights.setCurrentRemove(6);
            logger.log("Destroy method: insertDisposal");
            System.out.println("Destroy method: insertDisposal");
            if(printSwapInfo) System.out.println("id");
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
        long startNanoTime = System.nanoTime();

        randomRemoval(data, 1, nodesToSwap, logger);

        int randomIndex;
        List<NodeSwap> nodeSwapList;
        NodeSwap bestNodeSwap, currentNodeSwap;

        while (nodesToSwap.size() < p) {
            nodeSwapList = new ArrayList<>();
            randomIndex = nodesToSwap.size() == 0 ? 0 : random.nextInt(nodesToSwap.size());
            Node nodeToCompare = nodesToSwap.get(randomIndex);
            data.calculateVisitingTime();

            // TODO: for loop
            //List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList());
            List<Vehicle> feasibleVehicles = new ArrayList<>();
            for (Vehicle vehicle : data.getFleet()) if (!vehicle.isEmpty()) feasibleVehicles.add(vehicle);
            for (Vehicle vehicle : feasibleVehicles) {
                // TODO: for loop
                //List<Node> feasibleNodes = vehicle.getRoute().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).collect(Collectors.toList());
                List<Node> feasibleNodes = new ArrayList<>();
                for (Node node : vehicle.getRoute())
                    if (!node.isDepot() && !node.isDumpingSite()) feasibleNodes.add(node);
                for (Node node : feasibleNodes) {
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
        }

        for(Vehicle vehicle3 : data.getFleet().stream().filter(vehicle3 -> !vehicle3.isEmpty() && !vehicle3.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle3, false);
            if(!valid) {
                System.out.println("relatedremoval utanS");
                int asd = 2;
            }
        }        

        LocalTime endTime = LocalTime.now();
        long endNanoTime = System.nanoTime();
        logger.log("relatedRemoval ended at: " + endTime + ", took " + ((endNanoTime - startNanoTime) * 1e-9) + " seconds");

    }

    private void randomRemoval(Data data, int p, List<Node> nodesToSwap, Logger logger) {

        LocalTime startTime = LocalTime.now();
        logger.log("randomRemoval started at: " + startTime);
        long startNanoTime = System.nanoTime();

        boolean found;
        int numberOfFeasibleNodesToRemove, index;
        List<Node> feasibleNodesToRemove = new ArrayList<>();

        while (nodesToSwap.size() < p) {

            // TODO: for loop
            //feasibleNodesToRemove = data.getNodeList().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).collect(Collectors.toList());
            for (Node node : data.getNodeList())
                if (!node.isDepot() && !node.isDumpingSite()) feasibleNodesToRemove.add(node);
            numberOfFeasibleNodesToRemove = feasibleNodesToRemove.size();
            index = random.nextInt(numberOfFeasibleNodesToRemove);
            Node nodeToRemove = feasibleNodesToRemove.get(index);
            for (Vehicle vehicle : data.getFleet()) {
                found = false;
                for (int i = 0; i < vehicle.getRoute().size(); i++) {
                    Node node = vehicle.getRoute().get(i);
                    if ((int) node.getId() == nodeToRemove.getId()) {
                        nodesToSwap.add(node);
                        vehicle.getRoute().remove(node);
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
        }

        for(Vehicle vehicle3 : data.getFleet().stream().filter(vehicle3 -> !vehicle3.isEmpty() && !vehicle3.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle3, false);
            if(!valid) {
                System.out.println("randomremovel utanS");
                int asd = 2;
            }
        }

        LocalTime endTime = LocalTime.now();
        long endNanoTime = System.nanoTime();
        logger.log("randomRemoval ended at: " + endTime + ", took " + ((endNanoTime - startNanoTime) * 1e-9) + " seconds");

    }

    private void worstRemoval(Data data, int p, List<Node> nodesToSwap, int p_worst, Logger logger) {

        LocalTime startTime = LocalTime.now();
        logger.log("worstRemoval started at: " + startTime);
        long startNanoTime = System.nanoTime();

        float currentValue, initialValue = solver.getDataValue(data);
        int indexToRemoveFrom;
        List<NodeSwap> nodeSwapList = new ArrayList<>();
        Node nodeToRemove;
        NodeSwap bestNodeSwap, currentNodeSwap;
        Vehicle vehicleToRemoveFrom;

        //minden kocsi minden customer nodejara kiszamoljuk, hogy ha kivennenk, mennyi lenne az erteke
        for (Vehicle vehicle : data.getFleet()) {
            if (vehicle.isEmpty()) {
                continue;
            }
            for (int i = 0; i < vehicle.getRoute().size(); i++) {
                Node node = vehicle.getRoute().get(i);
                currentNodeSwap = new NodeSwap();
                if (!node.isDepot() && !node.isDumpingSite()) {
                    if(vehicle.isPenaltyVehicle()) {
                        currentValue = initialValue - 2 * data.getMaximumTravelDistance();
                        currentNodeSwap.setNode(node);
                        currentNodeSwap.setValue(currentValue);
                        currentNodeSwap.setIndex(i);
                        currentNodeSwap.setVehicle(vehicle);

                        nodeSwapList.add(currentNodeSwap);
                    } else {
                        Node previousNode = vehicle.getRoute().get(i - 1);
                        Node nextNode = vehicle.getRoute().get(i + 1);

                        float distanceBetweenNodesBeforeRemoval = data.getDistanceBetweenNode(previousNode, node) + data.getDistanceBetweenNode(node, nextNode);

                        vehicle.getRoute().remove(node);

                        float distanceBetweenNodesAfterRemoval = data.getDistanceBetweenNode(previousNode, nextNode);

                        currentValue = initialValue - distanceBetweenNodesBeforeRemoval + distanceBetweenNodesAfterRemoval;

                        currentNodeSwap.setNode(node);
                        currentNodeSwap.setValue(currentValue);
                        currentNodeSwap.setIndex(i);
                        currentNodeSwap.setVehicle(vehicle);

                        nodeSwapList.add(currentNodeSwap);

                        vehicle.getRoute().add(i, node);
                    }
                }
            }
        }

        while (nodesToSwap.size() < p) {

            nodeSwapList.sort(new Comparator<NodeSwap>() {
                @Override
                public int compare(NodeSwap o1, NodeSwap o2) {
                    return (Float.compare(o2.getValue(), o1.getValue()));
                }
            });

            double y = random.nextDouble();
            int index = (int) (Math.pow(y, p_worst) * nodeSwapList.size());

            bestNodeSwap = nodeSwapList.get(index);
            vehicleToRemoveFrom = bestNodeSwap.getVehicle();
            nodeToRemove = bestNodeSwap.getNode();
            indexToRemoveFrom = bestNodeSwap.getIndex();
            vehicleToRemoveFrom.getRoute().remove(indexToRemoveFrom);
            nodeSwapList.remove(bestNodeSwap);
            nodesToSwap.add(nodeToRemove);

            for (NodeSwap nodeSwap : nodeSwapList) {
                if (nodeSwap.getVehicle().equals(vehicleToRemoveFrom) && nodeSwap.getIndex() > indexToRemoveFrom) {
                    nodeSwap.setIndex(nodeSwap.getIndex() - 1);
                }
            }

            if(!vehicleToRemoveFrom.isPenaltyVehicle()){

                //removal a kivett node elott
                Node previousNode = vehicleToRemoveFrom.getRoute().get(indexToRemoveFrom - 1);
                if (!previousNode.isDepot() && !previousNode.isDumpingSite()) {
                    float distanceBeforeRemoval_ = data.getDistanceBetweenNode(previousNode, nodeToRemove);
                    float distanceAfterRemoval_ = data.getDistanceBetweenNode(previousNode, vehicleToRemoveFrom.getRoute().get(indexToRemoveFrom));
                    currentValue = initialValue - distanceBeforeRemoval_ + distanceAfterRemoval_;

                    currentNodeSwap = nodeSwapList.stream().filter(nodeSwap -> nodeSwap.getNode().getId() == (int) previousNode.getId()).findFirst().get();
                    if (currentValue > currentNodeSwap.getValue()) {
                        currentNodeSwap.setValue(currentValue);
                        currentNodeSwap.setVehicle(vehicleToRemoveFrom);
                        currentNodeSwap.setIndex(indexToRemoveFrom - 1);
                    }

                }

                //removal a kivett node utan
                Node nextNode = vehicleToRemoveFrom.getRoute().get(indexToRemoveFrom);
                if (!nextNode.isDepot() && !nextNode.isDumpingSite()) {
                    float distanceBeforeRemoval_ = data.getDistanceBetweenNode(nodeToRemove, nextNode);
                    float distanceAfterRemoval_ = data.getDistanceBetweenNode(vehicleToRemoveFrom.getRoute().get(indexToRemoveFrom - 1), nextNode);
                    currentValue = initialValue - distanceBeforeRemoval_ + distanceAfterRemoval_;

                    currentNodeSwap = nodeSwapList.stream().filter(nodeSwap -> nodeSwap.getNode().getId() == (int) nextNode.getId()).findFirst().get();
                    if (currentValue > currentNodeSwap.getValue()) {
                        currentNodeSwap.setValue(currentValue);
                        currentNodeSwap.setVehicle(vehicleToRemoveFrom);
                        currentNodeSwap.setIndex(indexToRemoveFrom);
                    }
                }
            }
        }

        for(Vehicle vehicle3 : data.getFleet().stream().filter(vehicle3 -> !vehicle3.isEmpty() && !vehicle3.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle3, false);
            if(!valid) {
                System.out.println("worstremoval utan");
                int asd = 2;
            }
        }

        LocalTime endTime = LocalTime.now();
        long endNanoTime = System.nanoTime();
        logger.log("worstRemoval ended at: " + endTime + ", took " + ((endNanoTime - startNanoTime) * 1e-9) + " seconds");

    }

    private void deleteDisposal(Data data, List<Node> nodesToSwap, Logger logger) {

        LocalTime startTime = LocalTime.now();
        logger.log("deleteDisposal started at: " + startTime);
        long startNanoTime = System.nanoTime();

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle, false);
            if(!valid) {
                System.out.println("deleteDisposal elott");
            }
        }

        List<NodeSwap> nodeSwapList = new ArrayList<>();
        //only those routes where are more than one dump
        // TODO: for loop
        //List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().stream().filter(Node::isDumpingSite).count() > 1).collect(Collectors.toList());
        List<Vehicle> feasibleVehicles = new ArrayList<>();
        for (Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty()).collect(Collectors.toList())) {
            int dumpingSites = 0;
            for (Node node : vehicle.getRoute()) {
                if (node.isDumpingSite()) {
                    dumpingSites++;
                }
            }
            if (dumpingSites > 1) {
                feasibleVehicles.add(vehicle);
            }
        }
        if (feasibleVehicles.size() == 0) {
            //if its 0, we can not delete that, return
            return;
        }

        //get all the dumps from all the vehicles
        for (Vehicle vehicle : feasibleVehicles) {
            for (int i = 0; i < vehicle.getRoute().size(); i++) {
                Node node = vehicle.getRoute().get(i);
                if (node.isDumpingSite()) {
                    NodeSwap nodeSwap = new NodeSwap(node, vehicle, 0, i, false);
                    nodeSwapList.add(nodeSwap);
                }
            }
        }

        //get a random disposal from the available
        int randomIndex = random.nextInt(nodeSwapList.size());
        NodeSwap nodeSwap = nodeSwapList.get(randomIndex);
        Vehicle vehicle = nodeSwap.getVehicle();
        //this is the exact dump
        Node dumpingSite = nodeSwap.getNode();
        //and the index where it is in the route
        int dumpingSiteIndex = nodeSwap.getIndex();

        // TODO: for loop
        nodeSwapList = nodeSwapList.stream().filter(nodeSwap1 -> nodeSwap1.getVehicle().equals(vehicle)).collect(Collectors.toList());

        //I might not need the sort at all, since in the loop line 1006 I put the dump from the vehicle in order, so they'll be next to
        //each other, also when I filter them
        /*
        nodeSwapList.sort(new Comparator<NodeSwap>() {
            @Override
            public int compare(NodeSwap o1, NodeSwap o2) {
                return (Float.compare(o1.getIndex(), o2.getIndex()));
            }
        });

         */

        //if the chosen dump is the last one, so the next one is the depot
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
            boolean valid = solver.checkForValidity(data, vehicle, true);
            if(!valid) {
                System.out.println("Invalid deletedisposal, last dump");
            }
        } else {
            //System.out.println("====");
            Vehicle copyVehicle = new Vehicle(vehicle);
            int maximumCapacity = vehicle.getMaximumCapacity();
            int startingIndex = nodeSwapList.indexOf(nodeSwap) == 0 ? 1 : nodeSwapList.get(nodeSwapList.indexOf(nodeSwap) - 1).getIndex() + 1;
            //System.out.println("Starting index: " + startingIndex);
            //System.out.println("End index: " + nodeSwapList.get(nodeSwapList.indexOf(nodeSwap) + 1).getIndex());
            float overallQuantity = 0;
            for (int i = startingIndex; i < nodeSwapList.get(nodeSwapList.indexOf(nodeSwap) + 1).getIndex(); i++) {
                Node node = vehicle.getRoute().get(i);
                if (!node.isDumpingSite()) {
                    overallQuantity += node.getQuantity();
                }
            }
            //System.out.println("Overall quantity: " + overallQuantity);
            vehicle.getRoute().remove(dumpingSite);
            int numberOfNodesRemoved = 0;
            while (overallQuantity > maximumCapacity) {
                Node currentNode = vehicle.getRoute().get(dumpingSiteIndex - 1);
                overallQuantity -= currentNode.getQuantity();
                //System.out.println("Removing node " + currentNode.getId());
                nodesToSwap.add(currentNode);
                vehicle.getRoute().remove(currentNode);
                numberOfNodesRemoved++;
                if (numberOfNodesRemoved % 2 == 0) {
                    dumpingSiteIndex--;
                }
            }
            boolean valid = solver.checkForValidity(data, vehicle, true);
            if(!valid) {
                System.out.println("Invalid deletedisposal, middle dump");
            }
        }

        for(Vehicle vehicle2 : data.getFleet().stream().filter(vehicle2 -> !vehicle2.isEmpty() && !vehicle2.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle2, false);
            if(!valid) {
                System.out.println("deleteDisposal utan");
            }
        }

        LocalTime endTime = LocalTime.now();
        long endNanoTime = System.nanoTime();
        logger.log("deleteDisposal ended at: " + endTime + ", took " + ((endNanoTime - startNanoTime) * 1e-9) + " seconds");

    }

    private void swapDisposal(Data data, List<Node> nodesToSwap, Logger logger) {

        LocalTime startTime = LocalTime.now();
        logger.log("swapDisposal started at: " + startTime);
        long startNanoTime = System.nanoTime();

        for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> !vehicle.isEmpty() && !vehicle.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle, false);
            if(!valid) {
                System.out.println("swapDisposal elott");
                int asd = 2;
            }
        }

        List<NodeSwap> nodeSwapList = new ArrayList<>();
        // TODO: for loop
        //int numberOfDisposalSites = (int) data.getNodeList().stream().filter(Node::isDumpingSite).count();
        int numberOfDisposalSites = 0;
        for (Node node : data.getNodeList()) if (node.isDumpingSite()) numberOfDisposalSites++;
        if (numberOfDisposalSites == 1) {
            return;
        }
        // TODO: for loop
        //List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList());
        List<Vehicle> feasibleVehicles = new ArrayList<>();
        for (Vehicle vehicle : data.getFleet()) if (!vehicle.isEmpty()) feasibleVehicles.add(vehicle);
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

        // TODO: for loop
        //List<Node> disposalSitesToSwapWith = data.getNodeList().stream().filter(disposalSite -> disposalSite.isDumpingSite() && (int)disposalSite.getId() != dumpingSite.getId()).collect(Collectors.toList());
        List<Node> disposalSitesToSwapWith = new ArrayList<>();
        for (Node disposalSite : data.getNodeList())
            if (disposalSite.isDumpingSite() && (int) disposalSite.getId() != dumpingSite.getId())
                disposalSitesToSwapWith.add(disposalSite);
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
            if (arrivalTimeAtDisposalSite <= disposalTimeEnd) {
                vehicle.getArrivalTimes().set(dumpingSiteIndex, Math.max(arrivalTimeAtDisposalSite, disposalSiteToSwapWith.getTimeStart()));
                solver.updateArrivalTimes(data);
                break;
            }
            nodesToSwap.add(previousNode);
            vehicle.getArrivalTimes().remove(dumpingSiteIndex - 1);
            vehicle.getRoute().remove(previousNode);
            dumpingSiteIndex--;
        }


        while(!solver.checkForValidity(data, vehicle, false) && dumpingSiteIndex < vehicle.getRoute().size()-2) {
            nextNode = vehicle.getRoute().get(dumpingSiteIndex+1);
            if(!nextNode.isDumpingSite() || !nextNode.isDepot()){
                nodesToSwap.add(nextNode);
                vehicle.getArrivalTimes().remove(dumpingSiteIndex+1);
                vehicle.getRoute().remove(dumpingSiteIndex+1);
            } 
        }
        solver.updateArrivalTimesForVehicle(vehicle, data);

        /* 
        for (int i = dumpingSiteIndex + 1; i < vehicle.getRoute().size() - 1; i++) {
            nextNode = vehicle.getRoute().get(i);
            serviceTime = disposalSiteToSwapWith.getServiceTime();
            travelDistance = data.getDistanceBetweenNode(disposalSiteToSwapWith, nextNode);
            arrivalTimeAtNextNode = arrivalTimeAtDisposalSite + serviceTime + travelDistance;
            if (arrivalTimeAtNextNode <= nextNode.getTimeEnd()) {
                vehicle.getArrivalTimes().set(i, Math.max(arrivalTimeAtNextNode, nextNode.getTimeStart()));
                solver.updateArrivalTimes(data);
                continue;
            }
            nodesToSwap.add(nextNode);
            vehicle.getArrivalTimes().remove(i);
            vehicle.getRoute().remove(nextNode);
            i--;
        }
        */

        List<Node> dumps = new ArrayList<>();
        int idx_lastDump = vehicle.getRoute().size()-2;
        while(vehicle.getRoute().get(idx_lastDump).isDumpingSite())
        {
            dumps.add(vehicle.getRoute().get(idx_lastDump));
            idx_lastDump--;
        }
        if(dumps.size() > 1) {
            System.out.println("hujj");
        }
        
        if(dumps.size() == 0) {
            System.out.println("what");
        }

        int numDumps = dumps.size();
        int idx_lastDepo = vehicle.getRoute().size()-1;
        Node lastdepot = vehicle.getRoute().get(idx_lastDepo);
        idx_lastDump = vehicle.getRoute().size()-2;
        int idx_firstDump = vehicle.getRoute().size()-numDumps-1;
        float leavingLastDump = vehicle.getArrivalTimes().get(idx_lastDump) + dumps.get(0).getServiceTime();
        float dumpDepotTravel = data.getDistanceBetweenNode(dumps.get(0), lastdepot);
        while(leavingLastDump + dumpDepotTravel > lastdepot.getTimeEnd()) {
            int idx_lastCustomer = vehicle.getRoute().size()-numDumps-2;
            Node lastCustomer =  vehicle.getRoute().get(idx_lastCustomer);
            nodesToSwap.add(lastCustomer);
            vehicle.getArrivalTimes().remove(idx_lastCustomer);
            vehicle.getRoute().remove(idx_lastCustomer);
            idx_lastDepo--;
            idx_firstDump--;
            idx_lastDump--;
            idx_lastCustomer--;
            lastCustomer = vehicle.getRoute().get(idx_lastCustomer);
            int idx_currDump = idx_firstDump;
            Node currDump = vehicle.getRoute().get(idx_currDump);
            float travelToCurrent = data.getDistanceBetweenNode(lastCustomer, currDump);
            float lastNodeDeparture = vehicle.getArrivalTimes().get(idx_lastCustomer) + lastCustomer.getServiceTime();
            vehicle.getArrivalTimes().set(idx_currDump, Math.max(currDump.getTimeStart(), lastNodeDeparture + travelToCurrent));
            Node prevDump = null;
            idx_currDump++;
            while(idx_currDump < idx_lastDepo) {
                prevDump = currDump;
                currDump =  vehicle.getRoute().get(idx_currDump);
                travelToCurrent = data.getDistanceBetweenNode(prevDump, currDump);
                lastNodeDeparture = vehicle.getArrivalTimes().get(idx_currDump-1) + prevDump.getServiceTime();
                vehicle.getArrivalTimes().set(idx_currDump, Math.max(currDump.getTimeStart(), lastNodeDeparture + travelToCurrent));
                idx_currDump++;
            }

            leavingLastDump = vehicle.getArrivalTimes().get(idx_lastDump) + dumps.get(0).getServiceTime();
            dumpDepotTravel = data.getDistanceBetweenNode(dumps.get(0), lastdepot);
        }

        for(Vehicle vehicle3 : data.getFleet().stream().filter(vehicle3 -> !vehicle3.isEmpty() && !vehicle3.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle3, true);
            if(!valid) {
                System.out.println("swapDisposal utanS");
                //throw new Exception();
            }
        }

        LocalTime endTime = LocalTime.now();
        long endNanoTime = System.nanoTime();
        logger.log("swapDisposal ended at: " + endTime + ", took " + ((endNanoTime - startNanoTime) * 1e-9) + " seconds");

    }

    private void insertDisposal(Data data, List<Node> nodesToSwap, Logger logger){

        LocalTime startTime = LocalTime.now();
        logger.log("inertDisposal started at: " + startTime);
        long startNanoTime = System.nanoTime();

        // TODO: for loop
        //List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList());
        List<Vehicle> feasibleVehicles = new ArrayList<>();
        for (Vehicle vehicle : data.getFleet()) if (!vehicle.isEmpty() && !vehicle.isPenaltyVehicle()) feasibleVehicles.add(vehicle);
        int randomIndex = random.nextInt(feasibleVehicles.size());
        Vehicle vehicleToInsertInto = feasibleVehicles.get(randomIndex);

        // TODO: for loop
        //List<Node> disposalSitesToSwapWith = data.getNodeList().stream().filter(disposalSite -> disposalSite.isDumpingSite() && (int)disposalSite.getId() != dumpingSiteToInsertAfter.getId()).collect(Collectors.toList());
        List<Node> disposalSitesToSwapWith = new ArrayList<>();
        for (Node disposalSite : data.getNodeList())
            if (disposalSite.isDumpingSite())
                disposalSitesToSwapWith.add(disposalSite);
        randomIndex = random.nextInt(disposalSitesToSwapWith.size());
        Node disposalSiteToInsert = disposalSitesToSwapWith.get(randomIndex);

        int index = vehicleToInsertInto.getRoute().size(); // lista merete, ezert indexbound lenne ha erre hivatkozunk de mivel beszurjuk index - 1-re a nodeot ezert beszuras utan jo lesz
        vehicleToInsertInto.getRoute().add(vehicleToInsertInto.getRoute().size() - 1, disposalSiteToInsert);
        vehicleToInsertInto.getArrivalTimes().add(index, (float) 0);
        solver.updateArrivalTimesForVehicle(vehicleToInsertInto,data);
/* 
        Node currentNode;
        vehicleToInsertInto.getArrivalTimes().add(index, (float) 0);
        float arriveTimeAtPreviousNode,
                serviceTimeAtPreviousNode,
                travelDistance;
        while (true) {
            currentNode = vehicleToInsertInto.getRoute().get(index - 1);
            arriveTimeAtPreviousNode = vehicleToInsertInto.getArrivalTimes().get(index - 1);
            serviceTimeAtPreviousNode = currentNode.getServiceTime();
            travelDistance = data.getDistanceBetweenNode(currentNode, disposalSiteToInsert);
            if (arriveTimeAtPreviousNode + serviceTimeAtPreviousNode + travelDistance <= disposalSiteToInsert.getTimeEnd()) {
                vehicleToInsertInto.getArrivalTimes().set(index, arriveTimeAtPreviousNode + serviceTimeAtPreviousNode + travelDistance);
                break;
            }
            vehicleToInsertInto.getArrivalTimes().remove(index - 1);
            nodesToSwap.add(currentNode);
            vehicleToInsertInto.getRoute().remove(currentNode);
            index--;
        }*/


        List<Node> dumps = new ArrayList<>();
        int idx_lastDump = vehicleToInsertInto.getRoute().size()-2;
        while(vehicleToInsertInto.getRoute().get(idx_lastDump).isDumpingSite())
        {
            dumps.add(vehicleToInsertInto.getRoute().get(idx_lastDump));
            idx_lastDump--;
        }        
        if(dumps.size() > 2) {
            System.out.println("hijj");
        }        

        if(dumps.size() == 0) {
            System.out.println("what");
        }

        int numDumps = dumps.size();
        int idx_lastDepo = vehicleToInsertInto.getRoute().size()-1;
        Node lastdepot = vehicleToInsertInto.getRoute().get(idx_lastDepo);
        idx_lastDump = vehicleToInsertInto.getRoute().size()-2;
        if(idx_lastDump == 0 || dumps.size() == 0){
            System.out.println('w');
        }
        int idx_firstDump = vehicleToInsertInto.getRoute().size()-numDumps-1;
        float leavingLastDump = vehicleToInsertInto.getArrivalTimes().get(idx_lastDump) + dumps.get(0).getServiceTime();
        float dumpDepotTravel = data.getDistanceBetweenNode(dumps.get(0), lastdepot);
        while(leavingLastDump + dumpDepotTravel > lastdepot.getTimeEnd()) {
            int idx_lastCustomer = vehicleToInsertInto.getRoute().size()-numDumps-2;
            Node lastCustomer =  vehicleToInsertInto.getRoute().get(idx_lastCustomer);
            nodesToSwap.add(lastCustomer);
            vehicleToInsertInto.getArrivalTimes().remove(idx_lastCustomer);
            vehicleToInsertInto.getRoute().remove(idx_lastCustomer);
            idx_lastDepo--;
            idx_lastDump--;
            idx_firstDump--;
            idx_lastCustomer--;
            lastCustomer = vehicleToInsertInto.getRoute().get(idx_lastCustomer);
            int idx_currDump = idx_firstDump;
            Node currDump = vehicleToInsertInto.getRoute().get(idx_currDump);
            float travelToCurrent = data.getDistanceBetweenNode(lastCustomer, currDump);
            float lastNodeDeparture = vehicleToInsertInto.getArrivalTimes().get(idx_lastCustomer) + lastCustomer.getServiceTime();
            vehicleToInsertInto.getArrivalTimes().set(idx_currDump, Math.max(currDump.getTimeStart(), lastNodeDeparture + travelToCurrent));
            Node prevDump = null;
            idx_currDump++;
            while(idx_currDump < idx_lastDepo) {
                prevDump = currDump;
                currDump =  vehicleToInsertInto.getRoute().get(idx_currDump);
                travelToCurrent = data.getDistanceBetweenNode(prevDump, currDump);
                lastNodeDeparture = vehicleToInsertInto.getArrivalTimes().get(idx_currDump-1) + prevDump.getServiceTime();
                vehicleToInsertInto.getArrivalTimes().set(idx_currDump, Math.max(currDump.getTimeStart(), lastNodeDeparture + travelToCurrent));
                idx_currDump++;
            }

            leavingLastDump = vehicleToInsertInto.getArrivalTimes().get(idx_lastDump) + dumps.get(0).getServiceTime();
            dumpDepotTravel = data.getDistanceBetweenNode(dumps.get(0), lastdepot);
        }

        for(Vehicle vehicle3 : data.getFleet().stream().filter(vehicle3 -> !vehicle3.isEmpty() && !vehicle3.isPenaltyVehicle()).collect(Collectors.toList())) {
            boolean valid = solver.checkForValidity(data, vehicle3, true);
            if(!valid) {
                System.out.println("insertdisposal utanS");
                //throw new Exception();
            }
        }

        LocalTime endTime = LocalTime.now();
        long endNanoTime = System.nanoTime();
        logger.log("insertDisposal ended at: " + endTime + ", took " + ((endNanoTime - startNanoTime) * 1e-9) + " seconds");

    }

}
