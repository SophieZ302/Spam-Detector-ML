
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import opennlp.tools.stemmer.PorterStemmer;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


public class ManuelSpam {
  List<String> spam_words;
  String outputPathTrain;
  String outputPathTest;
  RestClient restClient;
  Set<Integer> testIDSet;
  Set<Integer> trainIDSet;
  Map<Integer, Integer> truthMap;
  PorterStemmer stemmer = new PorterStemmer();

  public ManuelSpam(RestClient restClient) throws IOException {
    this.restClient = restClient;
    spam_words = readSpamWord("personalSpam.txt");
    outputPathTrain = "sparse0_train.txt";
    outputPathTest = "sparse0_test.txt";

    //spam_words = readSpamWord("appendspam.txt");
    //spam_words = readSpamWord("spam_original.txt");
    //outputPathTrain = "sparse1_train.txt";
    //outputPathTest = "sparse1_test.txt";
    //spam_words = readSpamWord("allgram.txt");
    //outputPathTrain = "sparse2_train.txt";
    //outputPathTest = "sparse2_test.txt";


    truthMap = new HashMap<Integer, Integer>();
    testIDSet = new HashSet<Integer>();
    trainIDSet = new HashSet<Integer>();

    //printTestTrainCatalogTruthFile();
    setuptableSets();
    recordScore();
  }

  private void printTestTrainCatalogTruthFile() throws IOException {
    // file number, 0/1
    StringBuilder testCatalog = new StringBuilder();
    StringBuilder trainCatalog = new StringBuilder();
    StringBuilder truthFile = new StringBuilder();
    int testCounter = 1;
    int trainCounter = 1;
    for (int i = 1; i <= 75419; i++) {
      Response response1 = restClient.performRequest("GET",
              "trec07spam/document/" + String.valueOf(i) + "/");
      String str = EntityUtils.toString(response1.getEntity());
      JsonParser parser = new JsonParser();
      JsonObject object = parser.parse(str).getAsJsonObject();
      String label = object.getAsJsonObject("_source").get("label").getAsString();
      String split = object.getAsJsonObject("_source").get("split").getAsString();
      System.out.println(i + " " + label + " " + split);
      int truth = 0;
      if (label.equals("spam")) {
        truth = 1;
      }
      if (split.equals("train")) {
        trainCatalog.append(trainCounter++).append(" ").append(i).append(" ").append(truth).append("\n");
      } else {
        testCatalog.append(testCounter++).append(" ").append(i).append(" ").append(truth).append("\n");
      }
      truthFile.append(i).append(" ").append(truth).append("\n");
    }
    writeFile("trainCatalog.txt", trainCatalog);
    writeFile("testCatalog.txt", testCatalog);
    writeFile("truth.txt", truthFile);
  }

  private void setuptableSets() throws FileNotFoundException {
    Scanner sc = new Scanner(new File("truth.txt"));
    while (sc.hasNextLine()) {
      String line = sc.nextLine();
      String[] vals = line.split(" ");
      truthMap.put(Integer.valueOf(vals[0]), Integer.valueOf(vals[1]));
    }
    System.out.println("read all truth: " + truthMap.size());

    Scanner sc2 = new Scanner(new File("testCatalog.txt"));
    while (sc2.hasNextLine()) {
      String line = sc2.nextLine();
      String[] vals = line.split(" ");
      testIDSet.add(Integer.valueOf(vals[1]));
    }
    System.out.println("read all testID: " + testIDSet.size());

    Scanner sc3 = new Scanner(new File("trainCatalog.txt"));
    while (sc3.hasNextLine()) {
      String line = sc3.nextLine();
      String[] vals = line.split(" ");
      trainIDSet.add(Integer.valueOf(vals[1]));
    }
    System.out.println("read all trainID: " + trainIDSet.size());
  }

  private void recordScore() {
    StringBuilder sparseTrain = new StringBuilder();
    StringBuilder sparseTest = new StringBuilder();
    int testCount = 0;
    int trainCount = 0;
    for (int i = 1; i <= 75419; i++) {
      //for (int i = 1; i <= 3; i++) {
      if (trainIDSet.contains(i)) {
        trainCount++;
        if (trainCount % 50 == 0) {
          writeFile(outputPathTrain, sparseTrain);
          sparseTrain = new StringBuilder();
          System.out.println("wrote " + trainCount + "training data");
        }
        sparseTrain.append(truthMap.get(i)).append(" ");
        Map<String, Integer> freqMap = getTermVector(String.valueOf(i), restClient);
        int index = 1;
        for (String spamword : spam_words) {
          if (freqMap.keySet().contains(spamword)) {
            int score = freqMap.get(spamword);
            sparseTrain.append(index).append(":").append(score).append(" ");
          }
          index++;
        }
        sparseTrain.append("\n");

      } else {
        testCount++;
        if (testCount % 50 == 0) {
          writeFile(outputPathTest, sparseTest);
          sparseTest = new StringBuilder();
          System.out.println("wrote " + testCount + "test data");
        }
        sparseTest.append(truthMap.get(i)).append(" ");
        Map<String, Integer> freqMap = getTermVector(String.valueOf(i), restClient);
        int index = 1;
        for (String spamword : spam_words) {
          if (freqMap.keySet().contains(spamword)) {
            int score = freqMap.get(spamword);
            sparseTest.append(index).append(":").append(score).append(" ");
          }
          index++;
        }
        sparseTest.append("\n");
      }
    }
    writeFile(outputPathTrain, sparseTrain);
    writeFile(outputPathTest, sparseTest);
  }

  private void writeFile(String path, StringBuilder b) {
    try {
      FileUtils.write(new File(path), b.toString(), "UTF-8", true);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static Map<String, Integer> getTermVector(String docId, RestClient restClient) {
    Map<String, Integer> rst = new HashMap<>();

    try {
      Response response1 = restClient.performRequest("GET",
              "/trec07spam/document/" + docId + "/_termvectors");
      String response = EntityUtils.toString(response1.getEntity());
      JsonParser parser = new JsonParser();
      JsonObject object = parser.parse(response).getAsJsonObject();
      if (object.getAsJsonObject("term_vectors") != null
              && object.getAsJsonObject("term_vectors").getAsJsonObject("text") != null) {
        JsonObject textInfo = object.getAsJsonObject("term_vectors").getAsJsonObject("text").getAsJsonObject("terms");
        Type mapType = new TypeToken<Map<String, Map>>() {
        }.getType();
        Map<String, String[]> son = new Gson().fromJson(textInfo.toString(), mapType);
        for (String s : son.keySet()) {
          int frq = textInfo.getAsJsonObject(s).getAsJsonPrimitive("term_freq").getAsInt();
          rst.put(s, frq);
        }
      }else {
        System.out.println(docId + " " + response);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (NullPointerException n) {
      n.printStackTrace();
      System.out.println();
    }
    return rst;
  }

  private List<String> readSpamWord(String filePath) {
    List<String> rst = new ArrayList<>();
    try {
      Scanner sc = new Scanner(new FileInputStream(new File(filePath)));
      while (sc.hasNextLine()) {
        rst.add(stemmer.stem(sc.nextLine()));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return rst;
  }

  public static void main(String[] str) throws IOException {
    RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")).build();
    ManuelSpam ms = new ManuelSpam(restClient);
    restClient.close();
  }
}
