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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class WekaFile {
  List<String> spam_words;
  String outputPathTrain;
  String outputPathTest;
  RestClient restClient;
  Set<Integer> testIDSet;
  Set<Integer> trainIDSet;
  Map<Integer, Integer> truthMap;
  PorterStemmer stemmer = new PorterStemmer();
  StringBuilder sb;

  public WekaFile(RestClient restClient) throws IOException {
    this.restClient = restClient;
    outputPathTrain = "wekaAll.arff";

    truthMap = new HashMap<Integer, Integer>();
    testIDSet = new HashSet<Integer>();
    trainIDSet = new HashSet<Integer>();
    sb = new StringBuilder();
    cleanSpam("spam_original.txt");
    spam_words = readSpamWord("cleanSpam.txt");
    setuptableSets();
    recordScore();
  }

  private void setuptableSets() throws IOException {
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

  private void recordScore() throws FileNotFoundException {
    sb.append("@RELATION spam\n").append("\n");
    Scanner sc = new Scanner(new File("cleanSpam.txt"));
    while (sc.hasNextLine()) {
      String str = sc.nextLine();
      sb.append("@ATTRIBUTE").append(" ").append(str).append("\tNUMERIC").append("\n");
    }
    sb.append("@ATTRIBUTE").append(" ").append("class").append("\t{spam,ham}").append("\n");
    sb.append("\n");
    sb.append("@DATA").append("\n");

    //@data {1 X, 3 Y, 4 "class A"}
    for (int i = 1; i <= 75419; i++) {
    //for (int i = 1; i <= 3; i++) {
      System.out.println("write " + i + " files");
      Map<String, Integer> freqMap = getTermVector(String.valueOf(i), restClient);
      int index = 0;
      sb.append("{");
      for (String spamword : spam_words) {
        if (freqMap.keySet().contains(spamword)) {
          int score = freqMap.get(spamword);
          sb.append(index).append(" ").append(score).append(",");
        }
        index++;
      }
      String truth = truthMap.get(i) == 1 ? "spam" : "ham";
      sb.append(" ").append(spam_words.size()).append(" ").append(truth).append("}\n");
    }
    writeFile(outputPathTrain, sb);
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
      } else {
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

  private void cleanSpam(String filePath) throws IOException {
    Set<String> rst = new HashSet<>();
    try {
      Scanner sc = new Scanner(new FileInputStream(new File(filePath)));
      while (sc.hasNextLine()) {
        String next = sc.nextLine().toLowerCase().trim().replaceAll("%", "");
        rst.add(next);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    StringBuilder sb = new StringBuilder();
    for (String i : rst) {
      sb.append(i).append("\n");
    }
    FileUtils.write(new File("cleanSpam.txt"), sb.toString(), "UTF-8");
  }

  public static void main(String[] str) throws IOException {


    RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")).build();

    WekaFile wf = new WekaFile(restClient);
    restClient.close();
  }
}
