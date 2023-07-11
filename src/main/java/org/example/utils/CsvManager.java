package org.example.utils;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.example.JavaClass;
import org.example.ModelEvaluation;
import org.example.jira_tickets.Ticket;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.example.MainClass.getProjName;

public class CsvManager {
    private CsvManager(){}
    public static void writeModelPerformances(List<ModelEvaluation> modelEvalList) throws IOException {

        FileWriter fileWriter= new FileWriter(getProjName()+"ClassifiersPerformance.csv");
        fileWriter.append("Iteration of walk forward, Classifier, Sampling, Feature selection, Cost sensitive classifier, AUC, Kappa, Precision, Recall"+"\n");

        for(ModelEvaluation modelEvaluation: modelEvalList) {
            fileWriter.append(modelEvaluation.getWalkForwardIter() + ",");
            fileWriter.append(modelEvaluation.getClassifier() + ",");
            fileWriter.append(modelEvaluation.getSampling() + ",");
            fileWriter.append(modelEvaluation.isFeatureSelection() + ",");
            fileWriter.append(modelEvaluation.isCostSensitiveClassifier()+",");
            fileWriter.append(modelEvaluation.getAUC() + ",");
            fileWriter.append(modelEvaluation.getKappa() + ",");
            fileWriter.append(modelEvaluation.getPrecision() + ",");
            fileWriter.append(modelEvaluation.getRecall() + "\n");

        }
        try{
            fileWriter.flush();
            fileWriter.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void writeCSVMetrics(List<JavaClass> classes, FileWriter fw) throws IOException {

        try {
            fw.append("Name,   Size, Authors Number, NR, Age, Public Methods, Number Of Comments, Change Set Size, Churn, Buggy" + "\n");

            for (JavaClass c : classes) {

                fw.append(c.getPath() + ",");
                fw.append(c.getSize() + ",");
                fw.append(c.getAuthNum() + ",");
                fw.append(c.getNR() + ",");
                fw.append(c.getAge() + ",");
                fw.append(c.getPublicMethodsCounter() + ",");
                fw.append(c.getCommentCounter() + ",");
                fw.append(c.getChangeSetSize() + ",");
                fw.append(c.getChurn() + ",");
                if (c.isBuggy()) {
                    fw.append("YES");
                } else {
                    fw.append("NO");
                }
                fw.append("\n");

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fw.flush();
                fw.close();

            } catch (IOException e) {
              e.printStackTrace();
            }
        }
    }

    public static void createCsvCategories(FileWriter fileWriter) {
        try {

            fileWriter.append("Index, Key, IV index, OV index, FV index");
            fileWriter.append("\n");
        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    public static void writeCsv(FileWriter fileWriter, List<Ticket> tickets) {

        try {

            createCsvCategories(fileWriter);
            for (Ticket tkt : tickets) {
                fileWriter.append(tkt.getIndex() + ",");
                fileWriter.append(tkt.getid() + ",");
                fileWriter.append(tkt.getIV() + ",");
                fileWriter.append(tkt.getOV() + ",");
                fileWriter.append(tkt.getFV() + "\n");
            }
        } catch (Exception e) {

            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }


    public static List<String> readCsvColumn(String nomeFile, int columnIndex) throws FileNotFoundException {
        FileReader fr = new FileReader(nomeFile);
        List<String> columnsval = new ArrayList<String>();
        try {
            CSVReader csvReader = new CSVReader(fr);
            fr.read();
            String[] row;
            int i = 0;
            while ((row = csvReader.readNext()) != null) {

                String colVal = row[columnIndex];

                columnsval.add(colVal);
                i++;

            }

        } catch (IOException e) {
           e.printStackTrace();
        } catch (CsvValidationException e) {
          e.printStackTrace();
        }
        return columnsval;
    }


    public static String readCsvEntry(String nomeFile, int columnIndex, int comparatorindex, String comparator) throws FileNotFoundException {
        FileReader fr = new FileReader(nomeFile);

        try {
            CSVReader csvReader = new CSVReader(fr);
            fr.read();
            String[] row;
            int i = 0;
            while ((row = csvReader.readNext()) != null) {
                if (row[comparatorindex].compareTo(comparator) == 0) {

                    return row[columnIndex];
                }

            }



        } catch (IOException e) {
          e.printStackTrace();
        } catch (CsvValidationException e) {
           e.printStackTrace();
        }
        return null;
    }
    public static String convertCSVtoarff(String path, int i, String type) throws IOException {
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(path));
        Instances data = loader.getDataSet();

        // Salva l'istanza in formato ARFF
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(getProjName()+i+type+".arff"));
        saver.writeBatch();
        return getProjName()+i+type+".arff";
    }

    public static List<String> getReleasesIndexes(String path) throws FileNotFoundException {
        List<String> releasesIndexes= new ArrayList<>();
        int numVersions=(CsvManager.readCsvColumn(path,0).size())-1;
        if(numVersions%2!=0){
            for(int i = 0; i< (numVersions-1)/2; i++){
                releasesIndexes.add(String.valueOf(i+1));
            }
        }
        else {
            for(int i = 0; i< numVersions/2; i++){
                releasesIndexes.add(String.valueOf(i+1));
            }

        }
        return releasesIndexes;

    }
}
