package org.example;

import org.example.jira_tickets.Ticket;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;

import java.util.ArrayList;
import java.util.List;

public class RetrieveWekaInformations {
    private RetrieveWekaInformations() {
        //private constructor to hyde the public one
    }



    public static List<ModelEvaluation> evaluateClassifiers(int iter, Instances training, Instances testing) throws Exception{
        List<ModelEvaluation> modelEvalList= new ArrayList<>();

        try {

            if (training.classIndex() == -1) {
                training.setClassIndex(training.numAttributes() - 1);
            }
            // Carica il set di test in formato ARFF

            if (testing.classIndex() == -1) {
                testing.setClassIndex(testing.numAttributes() - 1);
            }

            // Crea i classificatori
            Classifier ibk = new IBk();
            Classifier randomForest = new RandomForest();
            Classifier naiveBayes = new NaiveBayes();
            //NO FEATURE SELECTION E NO SAMPLING
            modelEvalList.add(computeClassifiersMetrics(iter, ibk, training, testing,false, Sampling.NO_SAMPLING, false));
            modelEvalList.add(computeClassifiersMetrics(iter, randomForest, training, testing,false, Sampling.NO_SAMPLING, false));
            modelEvalList.add(computeClassifiersMetrics(iter, naiveBayes, training, testing,false, Sampling.NO_SAMPLING,false));
            //FEATURE SELECTION E NO SAMPLING
            modelEvalList.add(computeClassifiersMetrics(iter, ibk, training, testing,true, Sampling.NO_SAMPLING,false));
            modelEvalList.add(computeClassifiersMetrics(iter, randomForest, training, testing,true, Sampling.NO_SAMPLING,false));
            modelEvalList.add(computeClassifiersMetrics(iter, naiveBayes, training, testing,true, Sampling.NO_SAMPLING,false));
            //NO FEATURE SELECTION E SIMPLE OVERSAMPLING
            modelEvalList.add(computeClassifiersMetrics(iter, ibk, training, testing,false, Sampling.OVERSAMPLING,false));
            modelEvalList.add(computeClassifiersMetrics(iter, randomForest, training, testing,false, Sampling.OVERSAMPLING,false));
            modelEvalList.add(computeClassifiersMetrics(iter, naiveBayes, training, testing,false, Sampling.OVERSAMPLING,false));
            //NO FEATURE SELECTION E SMOTE
            modelEvalList.add(computeClassifiersMetrics(iter, ibk, training, testing,false, Sampling.SMOTE,false));
            modelEvalList.add(computeClassifiersMetrics(iter, randomForest, training, testing,false, Sampling.SMOTE,false));
            modelEvalList.add(computeClassifiersMetrics(iter, naiveBayes, training, testing,false, Sampling.SMOTE,false));
            //FEATURE SELECTION E SMOTE
            modelEvalList.add(computeClassifiersMetrics(iter, ibk, training, testing,true, Sampling.SMOTE,false));
            modelEvalList.add(computeClassifiersMetrics(iter, randomForest, training, testing,true, Sampling.SMOTE,false));
            modelEvalList.add(computeClassifiersMetrics(iter, naiveBayes, training, testing,true, Sampling.SMOTE,false));
            //COST SENSITIVE CLASSIFICATION
            modelEvalList.add(computeClassifiersMetrics(iter, ibk, training, testing,false, Sampling.NO_SAMPLING,true));
            modelEvalList.add(computeClassifiersMetrics(iter, randomForest, training, testing,false, Sampling.NO_SAMPLING,true));
            modelEvalList.add(computeClassifiersMetrics(iter, naiveBayes, training, testing,false, Sampling.NO_SAMPLING,true));


        } catch (Exception e) {
            e.printStackTrace();
        }
        return modelEvalList;
    }

    private static ModelEvaluation computeClassifiersMetrics(int iter, Classifier c, Instances training, Instances testing, Boolean featureSel, Sampling sampling, Boolean costSens) throws Exception {
        ModelEvaluation modelEvaluation = new ModelEvaluation();
        modelEvaluation.setWalkForwardIter(iter);
        modelEvaluation.setClassifier(getClassifierName(c));
        modelEvaluation.setFeatureSelection(true);
        Evaluation eval;

        if (featureSel ==true) {
            AttributeSelection filter = new AttributeSelection();
            CfsSubsetEval featSelEval = new CfsSubsetEval();
            GreedyStepwise backSearch = new GreedyStepwise();
            backSearch.setSearchBackwards(true);

            filter.setEvaluator(featSelEval);
            filter.setSearch(backSearch);
            filter.setInputFormat(training);

            Instances filteredTraining = Filter.useFilter(training, filter);
            Instances filteredTesting = Filter.useFilter(testing, filter);
            eval = new Evaluation(filteredTesting);
            if (sampling == Sampling.SMOTE) {
                SMOTE smote = new SMOTE();
                smote.setInputFormat(filteredTraining);
                Instances oversampledDataSet = Filter.useFilter(filteredTraining, smote);

                FilteredClassifier fc = new FilteredClassifier();
                fc.setFilter(smote);
                fc.setClassifier(c);
                fc.buildClassifier(oversampledDataSet);

                modelEvaluation.setSampling("SMOTE");

                try {
                    eval.evaluateModel(fc, filteredTesting);
                    setEvaluationMetrics(modelEvaluation, eval, testing);
                }catch (IndexOutOfBoundsException e){
                    setNaNMetrics(modelEvaluation);
                }

            } else if (sampling == Sampling.NO_SAMPLING) {
                c.buildClassifier(filteredTraining);
                try {
                    eval.evaluateModel(c, filteredTesting);
                    setEvaluationMetrics(modelEvaluation, eval, testing);
                }catch(IndexOutOfBoundsException e){
                    setNaNMetrics(modelEvaluation);
                }

            }
        } else{
            modelEvaluation.setFeatureSelection(false);
            if (sampling == Sampling.OVERSAMPLING) {
                evaluateOversamplingNoFeatureSelection(modelEvaluation,training, testing,c);

            } else if (sampling == Sampling.SMOTE) {
                    evaluateSmoteNoFeatureSelection(modelEvaluation, training, testing,c);

                } else if (costSens) {

                        evaluateCostSensitive(modelEvaluation,training, testing, c);
                } else {
                    c.buildClassifier(training);

                    modelEvaluation.setSampling("NONE");

                    try {
                        eval = new Evaluation(testing);
                        eval.evaluateModel(c, testing);

                        setEvaluationMetrics(modelEvaluation, eval, testing);
                    }catch (IndexOutOfBoundsException e){
                        setNaNMetrics(modelEvaluation);
                    }
                }
            }


        return modelEvaluation;
    }

    private static String getClassifierName(Classifier c) {
        if (c instanceof IBk) {
            return "IBk";
        } else if (c instanceof RandomForest) {
            return "RandomForest";
        } else {
            return "NaiveBayes";
        }
    }

    private static void setEvaluationMetrics(ModelEvaluation modelEvaluation, Evaluation eval, Instances testing) {
        try {
            modelEvaluation.setKappa(eval.kappa());
            modelEvaluation.setPrecision(eval.precision(testing.classAttribute().indexOfValue("YES")));
            modelEvaluation.setRecall(eval.recall(testing.classAttribute().indexOfValue("YES")));
            modelEvaluation.setAUC(eval.areaUnderROC(testing.classAttribute().indexOfValue("YES")));
        }catch(Exception e){
            setNaNMetrics(modelEvaluation);
        }
    }

    private static void setNaNMetrics(ModelEvaluation modelEvaluation) {
        modelEvaluation.setKappa(Double.NaN);
        modelEvaluation.setPrecision(Double.NaN);
        modelEvaluation.setRecall(Double.NaN);
        modelEvaluation.setAUC(Double.NaN);
    }

    public static List<ModelEvaluation> evaluateWalkForward(List<String> releases, List<Ticket> validTickets) throws Exception {
        List<ModelEvaluation> modelEvalList=new ArrayList<>();
        ConverterUtils.DataSource trainingSet;
        ConverterUtils.DataSource testingSet;
        for(int i=0; i<releases.size(); i++){


            trainingSet = WalkForward.buildTrainingSet(validTickets, Integer.parseInt(releases.get(i)));

            testingSet=WalkForward.buildTestingSet(validTickets, Integer.parseInt(releases.get(i)));
            Instances trainingSetDataSet= new Instances(trainingSet.getDataSet());
            Instances testingSetDataSet= new Instances(testingSet.getDataSet());
            int numAttr = trainingSetDataSet.numAttributes();

            testingSetDataSet.setClassIndex(numAttr - 1);
            trainingSetDataSet.setClassIndex(numAttr - 1);
            if(trainingSetDataSet.classAttribute().indexOfValue("YES")==1&&trainingSetDataSet.classAttribute().indexOfValue("NO")==0 &&
                    testingSetDataSet.classAttribute().indexOfValue("NO")==0){
                //se nel training set ho anche istanze buggy allora posso fare iterazione walk forward e il testing set non deve essere vuoto
                modelEvalList.addAll(evaluateClassifiers(Integer.valueOf(releases.get(i)), trainingSetDataSet,testingSetDataSet));
            }
        }
        return modelEvalList;
    }
    private static  void evaluateOversamplingNoFeatureSelection(ModelEvaluation modelEvaluation, Instances training, Instances testing, Classifier c) throws Exception {
        Resample resampleFilter = new Resample();
        resampleFilter.setInputFormat(training);
        resampleFilter.setNoReplacement(true);
        Instances oversampledData = Filter.useFilter(training, resampleFilter);

        FilteredClassifier f = new FilteredClassifier();
        f.setFilter(resampleFilter);
        f.setClassifier(c);
        f.buildClassifier(oversampledData);


        modelEvaluation.setSampling("SIMPLE OVERSAMPLING");


        Evaluation eval ;
        try {
            eval = new Evaluation(testing);
            eval.evaluateModel(f, testing);
            setEvaluationMetrics(modelEvaluation, eval, testing);
        }catch(IndexOutOfBoundsException e){
            setNaNMetrics(modelEvaluation);
        }
    }
    private static void evaluateSmoteNoFeatureSelection(ModelEvaluation modelEvaluation, Instances training, Instances testing, Classifier c) throws Exception {
        SMOTE smote = new SMOTE();
        smote.setInputFormat(training);
        Instances oversampledDataSet = Filter.useFilter(training, smote);

        FilteredClassifier fc = new FilteredClassifier();
        fc.setFilter(smote);
        fc.setClassifier(c);
        fc.buildClassifier(oversampledDataSet);

        modelEvaluation.setFeatureSelection(false);
        modelEvaluation.setSampling("SMOTE");
        modelEvaluation.setClassifier(getClassifierName(c));

        Evaluation evalS;

        try {
            evalS = new Evaluation(testing);
            evalS.evaluateModel(fc, testing);
            setEvaluationMetrics(modelEvaluation, evalS, testing);

        }catch(IndexOutOfBoundsException e){
            setNaNMetrics(modelEvaluation);
        }
    }
    private static void evaluateCostSensitive(ModelEvaluation modelEvaluation, Instances training, Instances testing, Classifier c) throws Exception {
        CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
        costSensitiveClassifier.setClassifier(c);
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 1, 0.0);
        costMatrix.setCell(0, 1, 1.0); // Costo FP
        costMatrix.setCell(1, 0, 10.0); //costo FN

        costSensitiveClassifier.setCostMatrix(costMatrix);
        costSensitiveClassifier.buildClassifier(training);


        modelEvaluation.setCostSensitiveClassifier(true);
        modelEvaluation.setSampling("NONE");

        Evaluation evalCS;
        try {
            evalCS = new Evaluation(testing, costSensitiveClassifier.getCostMatrix());
            evalCS.evaluateModel(costSensitiveClassifier, testing);

            setEvaluationMetrics(modelEvaluation, evalCS, testing);
        } catch (Exception e) {
            setNaNMetrics(modelEvaluation);
        }
    }

}



