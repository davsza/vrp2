package data;

public class HeuristicWeights {

    private float randomRemoveWeight;
    private float worstRemoveWeight;
    private float relatedRemoveWeight;
    private float greedyInsertWeight;
    private float regret_2_InsertWeight;
    private float regret_3_InsertWeight;
    private float regret_K_InsertWeight;
    private int currentRemove; // 1: randomRemove; 2: worstRemove; 3: relatedRemove
    private int currentInsert; // 1: greedyInsert; 2: regret_2_Insert; 3: regret_3_Insert; 4: regret_K_InsertWeight

    private int timesUsedRandomRemove;
    private int timesUsedWorstRemove;
    private int timesUsedRelatedRemove;
    private int timesUsedGreedyInsert;
    private int timesUsedRegret_2_Insert;
    private int timesUsedRegret_3_Insert;
    private int timesUsedRegret_K_Insert;
    private int randomRemoveScore;
    private int worstRemoveScore;
    private int RelatedRemoveScore;
    private int greedyInsertScore;
    private int regret_2_InsertScore;
    private int regret_3_InsertScore;
    private int regret_K_InsertScore;

    public HeuristicWeights() {
        this.randomRemoveWeight = 1;
        this.worstRemoveWeight = 1;
        this.relatedRemoveWeight = 1;
        this.greedyInsertWeight = 1;
        this.regret_2_InsertWeight = 1;
        this.regret_3_InsertWeight = 1;
        this.regret_K_InsertWeight = 1;
        this.currentRemove = -1;
        this.currentInsert = -1;
        this.timesUsedRandomRemove = 0;
        this.timesUsedWorstRemove = 0;
        this.timesUsedRelatedRemove = 0;
        this.timesUsedGreedyInsert = 0;
        this.timesUsedRegret_2_Insert = 0;
        this.timesUsedRegret_3_Insert = 0;
        this.timesUsedRegret_K_Insert = 0;
        this.randomRemoveScore = 0;
        this.worstRemoveScore = 0;
        this.RelatedRemoveScore = 0;
        this.greedyInsertScore = 0;
        this.regret_2_InsertScore = 0;
        this.regret_3_InsertScore = 0;
        this.regret_K_InsertScore = 0;
    }

    public void reset() {
        setRandomRemoveWeight(0);
        setWorstRemoveWeight(0);
        setRelatedRemoveWeight(0);
        setGreedyInsertWeight(0);
        setRegret_2_InsertWeight(0);
        setRegret_3_InsertWeight(0);
        setRegret_K_InsertWeight(0);
    }

    public void setTimesUsedRegret_2_Insert(int timesUsedRegret_2_Insert) {
        this.timesUsedRegret_2_Insert = timesUsedRegret_2_Insert;
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

    public int getRegret_2_InsertScore() {
        return regret_2_InsertScore;
    }

    public void setRegret_2_InsertScore(int regret_2_InsertScore) {
        this.regret_2_InsertScore = regret_2_InsertScore;
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

    public int getTimesUsedRegret_2_Insert() {
        return timesUsedRegret_2_Insert;
    }

    public void setTimesUsedCurrentInsert(int timesUsedCurrentInsert) {
        this.timesUsedRegret_2_Insert = timesUsedCurrentInsert;
    }

    public float sumOfRepair() {
        return getGreedyInsertWeight() + getRegret_2_InsertWeight() + getRegret_3_InsertWeight() + getRegret_K_InsertWeight();
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

    public float getRegret_2_InsertWeight() {
        return regret_2_InsertWeight;
    }

    public void setRegret_2_InsertWeight(float regret_2_InsertWeight) {
        this.regret_2_InsertWeight = regret_2_InsertWeight;
    }

    public float getRegret_3_InsertWeight() {
        return regret_3_InsertWeight;
    }

    public void setRegret_3_InsertWeight(float regret_3_InsertWeight) {
        this.regret_3_InsertWeight = regret_3_InsertWeight;
    }

    public float getRegret_K_InsertWeight() {
        return regret_K_InsertWeight;
    }

    public void setRegret_K_InsertWeight(float regret_K_InsertWeight) {
        this.regret_K_InsertWeight = regret_K_InsertWeight;
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

    public int getTimesUsedRegret_3_Insert() {
        return timesUsedRegret_3_Insert;
    }

    public void setTimesUsedRegret_3_Insert(int timesUsedRegret_3_Insert) {
        this.timesUsedRegret_3_Insert = timesUsedRegret_3_Insert;
    }

    public int getTimesUsedRegret_K_Insert() {
        return timesUsedRegret_K_Insert;
    }

    public void setTimesUsedRegret_K_Insert(int timesUsedRegret_K_Insert) {
        this.timesUsedRegret_K_Insert = timesUsedRegret_K_Insert;
    }

    public int getRegret_3_InsertScore() {
        return regret_3_InsertScore;
    }

    public void setRegret_3_InsertScore(int regret_3_InsertScore) {
        this.regret_3_InsertScore = regret_3_InsertScore;
    }

    public int getRegret_K_InsertScore() {
        return regret_K_InsertScore;
    }

    public void setRegret_K_InsertScore(int regret_K_InsertScore) {
        this.regret_K_InsertScore = regret_K_InsertScore;
    }
}
