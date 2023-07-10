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

            if(featureSel ){ //solo feature sel
                CfsSubsetEval featSelEval = new CfsSubsetEval();
                GreedyStepwise backSearch = new GreedyStepwise();
                backSearch.setSearchBackwards(true);

                AttributeSelection filter = new AttributeSelection();
                filter.setEvaluator(featSelEval);
                filter.setSearch(backSearch);
                filter.setInputFormat(training);

                Instances filteredTraining = Filter.useFilter(training, filter);
                Instances filteredTesting = Filter.useFilter(testing, filter);
                if(sampling==Sampling.NO_SAMPLING){
                        int numAttrFiltered = filteredTraining.numAttributes();
                        filteredTraining.setClassIndex(numAttrFiltered - 1);
                        filteredTesting.setClassIndex(numAttrFiltered-1);

                        c.buildClassifier(filteredTraining);
                        Evaluation evalF = new Evaluation(filteredTesting);

                        ModelEvaluation modelEvaluationF= new ModelEvaluation();
                        if(c instanceof IBk){
                            modelEvaluationF.setClassifier("IBk");
                        }
                        else if(c instanceof RandomForest){
                            modelEvaluationF.setClassifier("RandomForest");
                        }
                        else{
                            modelEvaluationF.setClassifier("NaiveBayes");
                        }

                        modelEvaluationF.setSampling("NONE");
                        modelEvaluationF.setFeatureSelection(true);
                        modelEvaluationF.setWalkForwardIter(iter);
                        try{
                            evalF.evaluateModel(c, filteredTesting);
                            modelEvaluationF.setKappa(evalF.kappa());

                            modelEvaluationF.setPrecision(evalF.precision(testing.classAttribute().indexOfValue("YES")));
                            modelEvaluationF.setRecall(evalF.recall(testing.classAttribute().indexOfValue("YES")));
                            modelEvaluationF.setAUC(evalF.areaUnderROC(testing.classAttribute().indexOfValue("YES")));
                        }catch(ArrayIndexOutOfBoundsException e){
                            modelEvaluationF.setPrecision(Double.NaN);
                            modelEvaluationF.setRecall(Double.NaN);
                            modelEvaluationF.setAUC(Double.NaN);
                            modelEvaluationF.setKappa(Double.NaN);
                        }
                    return modelEvaluationF;
                }
                else if (sampling==Sampling.SMOTE) {        //feature sel e smote
                    SMOTE smote = new SMOTE();
                    FilteredClassifier fc = new FilteredClassifier();
                    smote.setInputFormat(filteredTraining);
                    Instances oversampledDataSet = Filter.useFilter(filteredTraining, smote);
                    fc.setFilter(smote);

                    fc.setClassifier(c);
                    fc.buildClassifier(oversampledDataSet);
                    Evaluation evalSamF = new Evaluation(filteredTesting);

                    ModelEvaluation modelEvaluationSamF = new ModelEvaluation();
                    modelEvaluationSamF.setFeatureSelection(true);
                    if(c instanceof IBk){
                        modelEvaluationSamF.setClassifier("IBk");}
                    else if(c instanceof RandomForest){
                        modelEvaluationSamF.setClassifier("RandomForest");
                    }
                    else{
                        modelEvaluationSamF.setClassifier("NaiveBayes");
                    }

                    modelEvaluationSamF.setSampling("SMOTE");
                    modelEvaluationSamF.setWalkForwardIter(iter);
                    try {
                        evalSamF.evaluateModel(fc, filteredTesting);
                        modelEvaluationSamF.setKappa(evalSamF.kappa());
                        modelEvaluationSamF.setPrecision(evalSamF.precision(testing.classAttribute().indexOfValue("YES")));
                        modelEvaluationSamF.setRecall(evalSamF.recall(testing.classAttribute().indexOfValue("YES")));
                        modelEvaluationSamF.setAUC(evalSamF.areaUnderROC(testing.classAttribute().indexOfValue("YES")));
                    }catch(ArrayIndexOutOfBoundsException e){
                        modelEvaluationSamF.setPrecision(Double.NaN);
                        modelEvaluationSamF.setRecall(Double.NaN);
                        modelEvaluationSamF.setAUC(Double.NaN);
                        modelEvaluationSamF.setKappa(Double.NaN);
                    }
                    return modelEvaluationSamF;
                }


            }
            else if(sampling==Sampling.OVERSAMPLING){ //solo oversmpling

                Resample resampleFilter = new Resample();
                resampleFilter.setInputFormat(training);
                resampleFilter.setNoReplacement(true);
                Instances oversampledData = Filter.useFilter(training, resampleFilter);

                FilteredClassifier f = new FilteredClassifier();



                f.setFilter(resampleFilter);

                f.setClassifier(c);
                f.buildClassifier(oversampledData);
                Evaluation evalO = new Evaluation(testing);

                ModelEvaluation modelEvaluationO= new ModelEvaluation();
                modelEvaluationO.setFeatureSelection(false);
                if(c instanceof IBk){
                    modelEvaluationO.setClassifier("IBk");
                }
                else if(c instanceof RandomForest){
                    modelEvaluationO.setClassifier("RandomForest");
                }
                else{
                    modelEvaluationO.setClassifier("NaiveBayes");
                }

                modelEvaluationO.setSampling("SIMPLE OVERSAMPLING");
                modelEvaluationO.setWalkForwardIter(iter);
                try {
                    evalO.evaluateModel(f, testing);
                    modelEvaluationO.setKappa(evalO.kappa());
                    modelEvaluationO.setPrecision(evalO.precision(testing.classAttribute().indexOfValue("YES")));
                    modelEvaluationO.setRecall(evalO.recall(testing.classAttribute().indexOfValue("YES")));
                    modelEvaluationO.setAUC(evalO.areaUnderROC(testing.classAttribute().indexOfValue("YES")));
                }catch(ArrayIndexOutOfBoundsException e){
                    modelEvaluationO.setPrecision(Double.NaN);
                    modelEvaluationO.setKappa(Double.NaN);
                    modelEvaluationO.setRecall(Double.NaN);
                    modelEvaluationO.setAUC(Double.NaN);
                }
                return modelEvaluationO;
            }

            else if(sampling==Sampling.SMOTE){ //solo SMOTE
                SMOTE smote= new SMOTE();
                smote.setInputFormat(training);
                Instances oversampledDataSet= Filter.useFilter(training,smote);
                FilteredClassifier fc = new FilteredClassifier();
                fc.setFilter(smote);
                fc.setClassifier(c);
                fc.buildClassifier(oversampledDataSet);
                Evaluation evalS = new Evaluation(testing);

                ModelEvaluation modelEvaluationS= new ModelEvaluation();
                modelEvaluationS.setFeatureSelection(false);
                if(c instanceof IBk){
                    modelEvaluationS.setClassifier("IBk");
                }
                else if(c instanceof RandomForest){
                    modelEvaluationS.setClassifier("RandomForest");
                }
                else{
                    modelEvaluationS.setClassifier("NaiveBayes");
                }
                modelEvaluationS.setSampling("SMOTE");
                modelEvaluationS.setWalkForwardIter(iter);
                try{
                    evalS.evaluateModel(fc, testing);
                    modelEvaluationS.setKappa(evalS.kappa());
                    modelEvaluationS.setPrecision(evalS.precision(testing.classAttribute().indexOfValue("YES")));
                    modelEvaluationS.setRecall(evalS.recall(testing.classAttribute().indexOfValue("YES")));
                    modelEvaluationS.setAUC(evalS.areaUnderROC(testing.classAttribute().indexOfValue("YES")));
                }catch(ArrayIndexOutOfBoundsException e){
                    modelEvaluationS.setKappa(Double.NaN);
                    modelEvaluationS.setPrecision(Double.NaN);
                    modelEvaluationS.setRecall(Double.NaN);
                    modelEvaluationS.setAUC(Double.NaN);
                }
                return modelEvaluationS;

            }
            //cost sensitive classifier
            else if(costSens){
                CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
                costSensitiveClassifier.setClassifier(c);
                CostMatrix costMatrix = new CostMatrix(2);
                costMatrix.setCell(0, 0, 0.0);
                costMatrix.setCell(1, 1, 0.0);
                if(testing.classAttribute().indexOfValue("YES")==1){
                    costMatrix.setCell(0, 1, 1.0); // Costo FP
                    costMatrix.setCell(1, 0, 10.0); //costo FN
                     }
                else{
                    costMatrix.setCell(1, 0, 1.0); // Costo FP
                    costMatrix.setCell(0, 1, 10.0); //costo FN
                }
                costSensitiveClassifier.setCostMatrix(costMatrix);

                costSensitiveClassifier.buildClassifier(training);


                ModelEvaluation modelEvaluationCS= new ModelEvaluation();
                modelEvaluationCS.setFeatureSelection(false);
                modelEvaluationCS.setCostSensitiveClassifier(true);
                if(c instanceof IBk){
                    modelEvaluationCS.setClassifier("IBk");
                }
                else if(c instanceof RandomForest){
                    modelEvaluationCS.setClassifier("RandomForest");
                }
                else{
                    modelEvaluationCS.setClassifier("NaiveBayes");
                }

                modelEvaluationCS.setSampling("NONE");
                modelEvaluationCS.setWalkForwardIter(iter);
                try{
                    Evaluation evalCS = new Evaluation(testing, costSensitiveClassifier.getCostMatrix());
                    evalCS.evaluateModel(costSensitiveClassifier, testing);
                    modelEvaluationCS.setKappa(evalCS.kappa());
                    modelEvaluationCS.setPrecision(evalCS.precision(testing.classAttribute().indexOfValue("YES")));
                    modelEvaluationCS.setRecall(evalCS.recall(testing.classAttribute().indexOfValue("YES")));
                    modelEvaluationCS.setAUC(evalCS.areaUnderROC(testing.classAttribute().indexOfValue("YES")));
                }catch(ArrayIndexOutOfBoundsException e){
                    modelEvaluationCS.setKappa(Double.NaN);
                    modelEvaluationCS.setPrecision(Double.NaN);
                    modelEvaluationCS.setRecall(Double.NaN);
                    modelEvaluationCS.setAUC(Double.NaN);
                }catch (Exception e){   //nel caso in cui nel testing set non ho valori positivi si solleva eccezione, che gestisco qui
                    modelEvaluationCS.setKappa(Double.NaN);
                    modelEvaluationCS.setPrecision(Double.NaN);
                    modelEvaluationCS.setRecall(Double.NaN);
                    modelEvaluationCS.setAUC(Double.NaN);
                }
                return modelEvaluationCS;

            }
            //nè feature sel nè oversampling

                c.buildClassifier(training);
                Evaluation evalIBk = new Evaluation(testing);

                ModelEvaluation modelEvaluation1= new ModelEvaluation();
                if(c instanceof IBk){
                    modelEvaluation1.setClassifier("IBk");
                }
                else if(c instanceof RandomForest){
                    modelEvaluation1.setClassifier("RandomForest");
                }
                else{
                    modelEvaluation1.setClassifier("NaiveBayes");
                }
                modelEvaluation1.setSampling("NONE");
                modelEvaluation1.setFeatureSelection(false);

                modelEvaluation1.setWalkForwardIter(iter);
                try{
                    evalIBk.evaluateModel(c, testing);
                    modelEvaluation1.setKappa(evalIBk.kappa());
                    modelEvaluation1.setPrecision(evalIBk.precision(testing.classAttribute().indexOfValue("YES")));
                    modelEvaluation1.setRecall(evalIBk.recall(testing.classAttribute().indexOfValue("YES")));
                    modelEvaluation1.setAUC(evalIBk.areaUnderROC(testing.classAttribute().indexOfValue("YES")));
                }catch(ArrayIndexOutOfBoundsException e){
                    modelEvaluation1.setKappa(Double.NaN);
                    modelEvaluation1.setPrecision(Double.NaN);
                    modelEvaluation1.setRecall(Double.NaN);
                    modelEvaluation1.setAUC(Double.NaN);
                }
                return modelEvaluation1;




        }
        public static List<ModelEvaluation> evaluateWalkForward(List<String> releases, List<Ticket> validTickets) throws Exception {
            List<ModelEvaluation> modelEvalList=new ArrayList<>();
            ConverterUtils.DataSource trainingSet;
            ConverterUtils.DataSource testingSet;
            for(int i=0; i<releases.size(); i++){


                trainingSet = WalkForward.buildTRSET(validTickets, Integer.parseInt(releases.get(i)));

                testingSet=WalkForward.buildTS(validTickets, Integer.parseInt(releases.get(i)));
                Instances trainingSetDataSet= new Instances(trainingSet.getDataSet());
                Instances testingSetDataSet= new Instances(testingSet.getDataSet());
                int numAttr = trainingSetDataSet.numAttributes();

                testingSetDataSet.setClassIndex(numAttr - 1);
                trainingSetDataSet.setClassIndex(numAttr - 1);
                if(trainingSetDataSet.classAttribute().indexOfValue("YES")==1&&trainingSetDataSet.classAttribute().indexOfValue("NO")==0 &&
                        (testingSetDataSet.classAttribute().indexOfValue("NO")==0||testingSetDataSet.classAttribute().indexOfValue("YES")==0)){
                    //se nel training set ho anche istanze buggy allora posso fare iterazione walk forward e il testing set non deve essere vuoto
                    modelEvalList.addAll(evaluateClassifiers(Integer.valueOf(releases.get(i)), trainingSetDataSet,testingSetDataSet));
                }
            }
            return modelEvalList;
        }

    }




