package data;

public class HeuristicWeights {

    private float randomRemoveWeight;
    private float worstRemoveWeight;
    private float relatedRemoveWeight;
    private float greedyInsertWeight;
    private float regretInsertWeight;
    private int currentRemove; // 1: randomRemove; 2: worstRemove; 3: relatedRemove
    private int currentInsert; // 1: greedyInsert; 2: regretInsert

    private int timesUsedRandomRemove;
    private int timesUsedWorstRemove;
    private int timesUsedRelatedRemove;
    private int timesUsedGreedyInsert;
    private int timesUsedRegretInsert;
    private int randomRemoveScore;
    private int worstRemoveScore;
    private int RelatedRemoveScore;
    private int greedyInsertScore;
    private int regretInsertScore;

    public HeuristicWeights() {
        this.randomRemoveWeight = 1;
        this.worstRemoveWeight = 1;
        this.relatedRemoveWeight = 1;
        this.greedyInsertWeight = 1;
        this.regretInsertWeight = 1;
        this.currentRemove = -1;
        this.currentInsert = -1;
        this.timesUsedRandomRemove = 0;
        this.timesUsedWorstRemove = 0;
        this.timesUsedRelatedRemove = 0;
        this.timesUsedGreedyInsert = 0;
        this.timesUsedRegretInsert = 0;
        this.randomRemoveScore = 0;
        this.worstRemoveScore = 0;
        this.RelatedRemoveScore = 0;
        this.greedyInsertScore = 0;
        this.regretInsertScore = 0;
    }

    public void reset() {
        setRandomRemoveWeight(0);
        setWorstRemoveWeight(0);
        setRelatedRemoveWeight(0);
        setGreedyInsertWeight(0);
        setRegretInsertWeight(0);
    }

    public void setTimesUsedRegretInsert(int timesUsedRegretInsert) {
        this.timesUsedRegretInsert = timesUsedRegretInsert;
    }

    public int getRandomRemoveScore() {
        return randomRemoveScore;
    }

    public void setRandomRemoveScore(int randomRemoveScore) {
        this.randomRemoveScore = randomRemoveScore;
    }

    public int getWorstRemoveScore() {
        return worstRemoveScore;
    }

    public void setWorstRemoveScore(int worstRemoveScore) {
        this.worstRemoveScore = worstRemoveScore;
    }

    public int getRelatedRemoveScore() {
        return RelatedRemoveScore;
    }

    public void setRelatedRemoveScore(int relatedRemoveScore) {
        RelatedRemoveScore = relatedRemoveScore;
    }

    public int getGreedyInsertScore() {
        return greedyInsertScore;
    }

    public void setGreedyInsertScore(int greedyInsertScore) {
        this.greedyInsertScore = greedyInsertScore;
    }

    public int getRegretInsertScore() {
        return regretInsertScore;
    }

    public void setRegretInsertScore(int regretInsertScore) {
        this.regretInsertScore = regretInsertScore;
    }

    public int getTimesUsedRandomRemove() {
        return timesUsedRandomRemove;
    }

    public void setTimesUsedRandomRemove(int timesUsedRandomRemove) {
        this.timesUsedRandomRemove = timesUsedRandomRemove;
    }

    public int getTimesUsedWorstRemove() {
        return timesUsedWorstRemove;
    }

    public void setTimesUsedWorstRemove(int timesUsedWorstRemove) {
        this.timesUsedWorstRemove = timesUsedWorstRemove;
    }

    public int getTimesUsedRelatedRemove() {
        return timesUsedRelatedRemove;
    }

    public void setTimesUsedRelatedRemove(int timesUsedRelatedRemove) {
        this.timesUsedRelatedRemove = timesUsedRelatedRemove;
    }

    public int getTimesUsedGreedyInsert() {
        return timesUsedGreedyInsert;
    }

    public void setTimesUsedGreedyInsert(int timesUsedGreedyInsert) {
        this.timesUsedGreedyInsert = timesUsedGreedyInsert;
    }

    public int getTimesUsedRegretInsert() {
        return timesUsedRegretInsert;
    }

    public void setTimesUsedCurrentInsert(int timesUsedCurrentInsert) {
        this.timesUsedRegretInsert = timesUsedCurrentInsert;
    }

    public int getCurrentRemove() {
        return currentRemove;
    }

    public void setCurrentRemove(int currentRemove) {
        this.currentRemove = currentRemove;
    }

    public int getCurrentInsert() {
        return currentInsert;
    }

    public void setCurrentInsert(int currentInsert) {
        this.currentInsert = currentInsert;
    }

    public float sumOfRepair() {
        return getGreedyInsertWeight() + getRegretInsertWeight();
    }

    public float sumOfDestroy() {
        return getRandomRemoveWeight() + getWorstRemoveWeight() + getRelatedRemoveWeight();
    }

    public float getRandomRemoveWeight() {
        return randomRemoveWeight;
    }

    public void setRandomRemoveWeight(float randomRemoveWeight) {
        this.randomRemoveWeight = randomRemoveWeight;
    }

    public float getWorstRemoveWeight() {
        return worstRemoveWeight;
    }

    public void setWorstRemoveWeight(float worstRemoveWeight) {
        this.worstRemoveWeight = worstRemoveWeight;
    }

    public float getRelatedRemoveWeight() {
        return relatedRemoveWeight;
    }

    public void setRelatedRemoveWeight(float relatedRemoveWeight) {
        this.relatedRemoveWeight = relatedRemoveWeight;
    }

    public float getGreedyInsertWeight() {
        return greedyInsertWeight;
    }

    public void setGreedyInsertWeight(float greedyInsertWeight) {
        this.greedyInsertWeight = greedyInsertWeight;
    }

    public float getRegretInsertWeight() {
        return regretInsertWeight;
    }

    public void setRegretInsertWeight(float regretInsertWeight) {
        this.regretInsertWeight = regretInsertWeight;
    }
}
