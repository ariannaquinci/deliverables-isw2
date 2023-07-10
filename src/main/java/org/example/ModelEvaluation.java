package org.example;

public class ModelEvaluation {
    private int walkForwardIter;

    private String classifier;

    private double precision;
    private double recall;
    private double auc;
    private double kappa;
    private String sampling;
    private boolean featureSelection;
    private boolean costSensitiveClassifier;

    public boolean isCostSensitiveClassifier() {
        return costSensitiveClassifier;
    }

    public void setCostSensitiveClassifier(boolean costSensitiveClassifier) {
        this.costSensitiveClassifier = costSensitiveClassifier;
    }

    public boolean isFeatureSelection() {
        return featureSelection;
    }

    public void setFeatureSelection(boolean featureSelection) {
        this.featureSelection = featureSelection;
    }

    public String getSampling() {
        return sampling;
    }

    public void setSampling(String sampling) {
        this.sampling = sampling;
    }

    public int getWalkForwardIter() {
        return walkForwardIter;
    }

    public void setWalkForwardIter(int walkForwardIter) {
        this.walkForwardIter = walkForwardIter;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getAUC() {
        return auc;
    }

    public void setAUC(double auc) {
        this.auc = auc;
    }

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

}
