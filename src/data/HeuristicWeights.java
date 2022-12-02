package data;

public class HeuristicWeights {

    private float randomRemovalWeight;
    private float worstRemovalWeight;
    private float relatedRemovalWeight;
    private float deleteDisposalWeight;
    private float swapDisposalWeight;
    private float insertDisposalWeight;
    private float greedyInsertWeight;
    private float regret_2_InsertWeight;
    private float regret_3_InsertWeight;
    private float regret_K_InsertWeight;
    private int currentRemove; // 1: randomRemove; 2: worstRemove; 3: relatedRemove; 4: deleteDisposal; 5: swapDisposal; 6: insertDisposal
    private int currentInsert; // 1: greedyInsert; 2: regret_2_Insert; 3: regret_3_Insert; 4: regret_K_InsertWeight

    private int timesUsedRandomRemove;
    private int timesUsedWorstRemove;
    private int timesUsedRelatedRemove;
    private int timesUsedDeleteDisposal;
    private int timesUsedSwapDisposal;
    private int timesUsedInsertDisposal;
    private int timesUsedGreedyInsert;
    private int timesUsedRegret_2_Insert;
    private int timesUsedRegret_3_Insert;
    private int timesUsedRegret_K_Insert;
    private int randomRemovalScore;
    private int worstRemovalScore;
    private int relatedRemovalScore;
    private int deleteDisposalScore;
    private int swapDisposalScore;
    private int insertDisposalScore;
    private int greedyInsertScore;
    private int regret_2_InsertScore;
    private int regret_3_InsertScore;
    private int regret_K_InsertScore;

    public HeuristicWeights() {
        this.randomRemovalWeight = 1;
        this.worstRemovalWeight = 1;
        this.relatedRemovalWeight = 1;
        this.deleteDisposalWeight = 1;
        this.swapDisposalWeight = 1;
        this.insertDisposalWeight = 1;
        this.greedyInsertWeight = 1;
        this.regret_2_InsertWeight = 1;
        this.regret_3_InsertWeight = 1;
        this.regret_K_InsertWeight = 1;
        this.currentRemove = -1;
        this.currentInsert = -1;
        this.timesUsedRandomRemove = 0;
        this.timesUsedWorstRemove = 0;
        this.timesUsedRelatedRemove = 0;
        this.timesUsedDeleteDisposal = 0;
        this.timesUsedSwapDisposal = 0;
        this.timesUsedInsertDisposal = 0;
        this.timesUsedGreedyInsert = 0;
        this.timesUsedRegret_2_Insert = 0;
        this.timesUsedRegret_3_Insert = 0;
        this.timesUsedRegret_K_Insert = 0;
        this.randomRemovalScore = 0;
        this.worstRemovalScore = 0;
        this.relatedRemovalScore = 0;
        this.deleteDisposalScore = 0;
        this.swapDisposalScore = 0;
        this.insertDisposalScore = 0;
        this.greedyInsertScore = 0;
        this.regret_2_InsertScore = 0;
        this.regret_3_InsertScore = 0;
        this.regret_K_InsertScore = 0;
    }

    public HeuristicWeights(HeuristicWeights heuristicWeights) {
        this.randomRemovalWeight = heuristicWeights.getRandomRemovalWeight();
        this.worstRemovalWeight = heuristicWeights.getWorstRemovalWeight();
        this.relatedRemovalWeight = heuristicWeights.getRelatedRemovalWeight();
        this.deleteDisposalWeight = heuristicWeights.getDeleteDisposalWeight();
        this.swapDisposalWeight = heuristicWeights.getSwapDisposalWeight();
        this.insertDisposalWeight = heuristicWeights.getInsertDisposalWeight();
        this.greedyInsertWeight = heuristicWeights.getGreedyInsertWeight();
        this.regret_2_InsertWeight = heuristicWeights.getRegret_2_InsertWeight();
        this.regret_3_InsertWeight = heuristicWeights.getRegret_3_InsertWeight();
        this.regret_K_InsertWeight = heuristicWeights.getRegret_K_InsertWeight();
        this.currentRemove = heuristicWeights.getCurrentRemove();
        this.currentInsert = heuristicWeights.getCurrentInsert();
        this.timesUsedRandomRemove = heuristicWeights.getTimesUsedRandomRemove();
        this.timesUsedWorstRemove = heuristicWeights.getTimesUsedWorstRemove();
        this.timesUsedRelatedRemove = heuristicWeights.getTimesUsedRelatedRemove();
        this.timesUsedDeleteDisposal = heuristicWeights.getTimesUsedDeleteDisposal();
        this.timesUsedSwapDisposal = heuristicWeights.getTimesUsedSwapDisposal();
        this.timesUsedInsertDisposal = heuristicWeights.getTimesUsedInsertDisposal();
        this.timesUsedGreedyInsert = heuristicWeights.getTimesUsedGreedyInsert();
        this.timesUsedRegret_2_Insert = heuristicWeights.getTimesUsedRegret_2_Insert();
        this.timesUsedRegret_3_Insert = heuristicWeights.getTimesUsedRegret_3_Insert();
        this.timesUsedRegret_K_Insert = heuristicWeights.getTimesUsedRegret_K_Insert();
        this.randomRemovalScore = heuristicWeights.getRandomRemovalScore();
        this.worstRemovalScore = heuristicWeights.getWorstRemovalScore();
        this.relatedRemovalScore = heuristicWeights.getRelatedRemovalScore();
        this.deleteDisposalScore = heuristicWeights.getDeleteDisposalScore();
        this.swapDisposalScore = heuristicWeights.getSwapDisposalScore();
        this.insertDisposalScore = heuristicWeights.getInsertDisposalScore();
        this.greedyInsertScore = heuristicWeights.getGreedyInsertScore();
        this.regret_2_InsertScore = heuristicWeights.getRegret_2_InsertScore();
        this.regret_3_InsertScore = heuristicWeights.getRegret_3_InsertScore();
        this.regret_K_InsertScore = heuristicWeights.getRegret_K_InsertScore();
    }

    public float sumOfRepair() {
        return getGreedyInsertWeight() + getRegret_2_InsertWeight() + getRegret_3_InsertWeight() + getRegret_K_InsertWeight();
    }

    public float sumOfDestroy() {
        return getRandomRemovalWeight() + getWorstRemovalWeight() + getRelatedRemovalWeight() + getDeleteDisposalWeight() + getSwapDisposalWeight() + getInsertDisposalWeight();
    }

    public float getRandomRemovalWeight() {
        return randomRemovalWeight;
    }

    public void setRandomRemovalWeight(float randomRemovalWeight) {
        this.randomRemovalWeight = randomRemovalWeight;
    }

    public float getWorstRemovalWeight() {
        return worstRemovalWeight;
    }

    public void setWorstRemovalWeight(float worstRemovalWeight) {
        this.worstRemovalWeight = worstRemovalWeight;
    }

    public float getRelatedRemovalWeight() {
        return relatedRemovalWeight;
    }

    public void setRelatedRemovalWeight(float relatedRemovalWeight) {
        this.relatedRemovalWeight = relatedRemovalWeight;
    }

    public float getDeleteDisposalWeight() {
        return deleteDisposalWeight;
    }

    public void setDeleteDisposalWeight(float deleteDisposalWeight) {
        this.deleteDisposalWeight = deleteDisposalWeight;
    }

    public float getSwapDisposalWeight() {
        return swapDisposalWeight;
    }

    public void setSwapDisposalWeight(float swapDisposalWeight) {
        this.swapDisposalWeight = swapDisposalWeight;
    }

    public float getInsertDisposalWeight() {
        return insertDisposalWeight;
    }

    public void setInsertDisposalWeight(float insertDisposalWeight) {
        this.insertDisposalWeight = insertDisposalWeight;
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

    public int getTimesUsedDeleteDisposal() {
        return timesUsedDeleteDisposal;
    }

    public void setTimesUsedDeleteDisposal(int timesUsedDeleteDisposal) {
        this.timesUsedDeleteDisposal = timesUsedDeleteDisposal;
    }

    public int getTimesUsedSwapDisposal() {
        return timesUsedSwapDisposal;
    }

    public void setTimesUsedSwapDisposal(int timesUsedSwapDisposal) {
        this.timesUsedSwapDisposal = timesUsedSwapDisposal;
    }

    public int getTimesUsedInsertDisposal() {
        return timesUsedInsertDisposal;
    }

    public void setTimesUsedInsertDisposal(int timesUsedInsertDisposal) {
        this.timesUsedInsertDisposal = timesUsedInsertDisposal;
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

    public void setTimesUsedRegret_2_Insert(int timesUsedRegret_2_Insert) {
        this.timesUsedRegret_2_Insert = timesUsedRegret_2_Insert;
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

    public int getRandomRemovalScore() {
        return randomRemovalScore;
    }

    public void setRandomRemovalScore(int randomRemovalScore) {
        this.randomRemovalScore = randomRemovalScore;
    }

    public int getWorstRemovalScore() {
        return worstRemovalScore;
    }

    public void setWorstRemovalScore(int worstRemovalScore) {
        this.worstRemovalScore = worstRemovalScore;
    }

    public int getRelatedRemovalScore() {
        return relatedRemovalScore;
    }

    public void setRelatedRemovalScore(int relatedRemovalScore) {
        this.relatedRemovalScore = relatedRemovalScore;
    }

    public int getDeleteDisposalScore() {
        return deleteDisposalScore;
    }

    public void setDeleteDisposalScore(int deleteDisposalScore) {
        this.deleteDisposalScore = deleteDisposalScore;
    }

    public int getSwapDisposalScore() {
        return swapDisposalScore;
    }

    public void setSwapDisposalScore(int swapDisposalScore) {
        this.swapDisposalScore = swapDisposalScore;
    }

    public int getInsertDisposalScore() {
        return insertDisposalScore;
    }

    public void setInsertDisposalScore(int insertDisposalScore) {
        this.insertDisposalScore = insertDisposalScore;
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
