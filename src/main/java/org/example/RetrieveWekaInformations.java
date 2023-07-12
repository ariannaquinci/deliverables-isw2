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

import static org.example.Sampling.OVERSAMPLING;

public class RetrieveWekaInformations {
    private RetrieveWekaInformations() {
        //private constructor to hyde the public one
    }



    public static List<ModelEvaluation> evaluateClassifiers(int iter, Instances training, Instances testing) throws Exception {
        List<ModelEvaluation> modelEvalList= new ArrayList<>();



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
            modelEvalList.add(computeClassifiersMetrics(iter, ibk, training, testing,false, OVERSAMPLING,false));
            modelEvalList.add(computeClassifiersMetrics(iter, randomForest, training, testing,false, OVERSAMPLING,false));
            modelEvalList.add(computeClassifiersMetrics(iter, naiveBayes, training, testing,false, OVERSAMPLING,false));
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


        return modelEvalList;
    }
    private static void computeClassifierMetricsNoFeatureSelection(Classifier c,ModelEvaluation modelEvaluation, Instances training, Instances testing,  Sampling sampling, Boolean costSens) throws Exception {
        modelEvaluation.setFeatureSelection(false);
        Evaluation eval;
        switch (sampling){
            case OVERSAMPLING:

                evaluateOversamplingNoFeatureSelection(modelEvaluation,training, testing,c);
                break;

            case SMOTE:
                evaluateSmoteNoFeatureSelection(modelEvaluation, training, testing,c);
                break;
            case NO_SAMPLING:
                if(costSens.booleanValue()==false){
                    c.buildClassifier(training);

                    modelEvaluation.setSampling("NONE");


                        eval = new Evaluation(testing);
                        eval.evaluateModel(c, testing);

                        setEvaluationMetrics(modelEvaluation, eval);

                }else{evaluateCostSensitive(modelEvaluation,training, testing, c);}

                break;

        }
    }
    private static void computeClassifiersMetricsWithFeatureSelection(ModelEvaluation modelEvaluation, Classifier c,Instances training, Instances testing, Sampling sampling) throws Exception {
        Evaluation eval;
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
        switch (sampling){
            case SMOTE:
                SMOTE smote = new SMOTE();
                smote.setInputFormat(filteredTraining);
                Instances oversampledDataSet = Filter.useFilter(filteredTraining, smote);

                FilteredClassifier fc = new FilteredClassifier();
                fc.setFilter(smote);
                fc.setClassifier(c);
                fc.buildClassifier(oversampledDataSet);

                modelEvaluation.setSampling("SMOTE");


                    eval.evaluateModel(fc, filteredTesting);
                    setEvaluationMetrics(modelEvaluation, eval);

                break;
            case NO_SAMPLING:
                c.buildClassifier(filteredTraining);
                eval.evaluateModel(c, filteredTesting);
                setEvaluationMetrics(modelEvaluation, eval);

                break;
            default:
                break;

        }

    }

        private static ModelEvaluation computeClassifiersMetrics(int iter, Classifier c, Instances training, Instances testing, Boolean featureSel, Sampling sampling, Boolean costSens) throws Exception {
        ModelEvaluation modelEvaluation = new ModelEvaluation();
        modelEvaluation.setWalkForwardIter(iter);
        modelEvaluation.setClassifier(getClassifierName(c));



        if (featureSel.booleanValue()==true) {
            modelEvaluation.setFeatureSelection(true);
           computeClassifiersMetricsWithFeatureSelection(modelEvaluation,c,training, testing, sampling);
        } else{
            modelEvaluation.setFeatureSelection(false);


            computeClassifierMetricsNoFeatureSelection(c, modelEvaluation,training,testing,sampling,costSens);}




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

    private static void setEvaluationMetrics(ModelEvaluation modelEvaluation, Evaluation eval) {

            modelEvaluation.setKappa(eval.kappa());
            modelEvaluation.setPrecision(eval.precision(1));
            modelEvaluation.setRecall(eval.recall(1));
            modelEvaluation.setAUC(eval.areaUnderROC(1));
            modelEvaluation.setTrueNegative(eval.numTrueNegatives(1));
            modelEvaluation.setTruePositive(eval.numTruePositives(1));
            modelEvaluation.setFalseNegative(eval.numFalseNegatives(1));
            modelEvaluation.setFalsePositive(eval.numFalsePositives(1));

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
            if(trainingSetDataSet.classAttribute().indexOfValue("YES")==1&&  !testingSetDataSet.isEmpty()){
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

            eval = new Evaluation(testing);
            eval.evaluateModel(f, testing);
            setEvaluationMetrics(modelEvaluation, eval);

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

            evalS = new Evaluation(testing);
            evalS.evaluateModel(fc, testing);
            setEvaluationMetrics(modelEvaluation, evalS);

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

            evalCS = new Evaluation(testing, costSensitiveClassifier.getCostMatrix());
            evalCS.evaluateModel(costSensitiveClassifier, testing);

            setEvaluationMetrics(modelEvaluation, evalCS);

    }

}



