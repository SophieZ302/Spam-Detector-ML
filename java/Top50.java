import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Top50 {
  //read out1, out2, make 2tuple, id-score, sort, print
  private class Tuple{
    int index;
    double score;
    public Tuple(int index, double score) {
      this.index = index;
      this.score = score;
    }
  }
  public void top50All() throws IOException{
    //truth map
    Map<Integer, Integer> truthMap = new HashMap<>();
    Scanner sc0 = new Scanner(new File("truth.txt"));
    while(sc0.hasNextLine()) {
      String[] vals = sc0.nextLine().split(" ");
      truthMap.put(Integer.valueOf(vals[0]), Integer.valueOf(vals[1]));
    }
    //test Catalog, map of ith line => fileIndex
    Map<Integer, Integer> catalog = new HashMap<>();
    Scanner sc1 = new Scanner(new File("testCatalog.txt"));
    while(sc1.hasNextLine()) {
      String[] vals = sc1.nextLine().split(" ");
      catalog.put(Integer.valueOf(vals[0]), Integer.valueOf(vals[1]));
    }

    //index-score
    List<Tuple> scoreList = new LinkedList<>();
    Scanner sc = new Scanner(new File("out2"));
    int lineCount = 0;
    sc.nextLine();
    while (sc.hasNextLine()) {
      lineCount++;
      String line = sc.nextLine();
      String[] vals = line.split(" ");
      Double score = Double.valueOf(vals[2]);
      int fileIndex = catalog.get(lineCount);
      scoreList.add(new Tuple(fileIndex, score));
    }
    scoreList.sort(new Comparator<Tuple>() {
      @Override
      public int compare(Tuple o1, Tuple o2) {
        return Double.compare(o2.score, o1.score);
      }
    });
    StringBuilder sb = new StringBuilder();
    int correctCount = 0;
    for(int i = 0; i < 50; i++) {
      int fileIndex = scoreList.get(i).index;
      sb.append(fileIndex).append("\n");
      if (truthMap.get(fileIndex) == 1) {
        correctCount++;
      } else {
        System.out.println(fileIndex);
      }
    }
    System.out.println("Accuracy top50 all unigram: " + (double) correctCount/ (double) 50);
    sb.append("Accuracy: ").append((double) correctCount/ (double) 50).append("\n");
    FileUtils.write(new File("top/top50all.txt"), sb.toString(), "UTF-8");
  }
  public void top50Small() throws IOException {
    //truth map
    Map<Integer, Integer> truthMap = new HashMap<>();
    Scanner sc0 = new Scanner(new File("truth.txt"));
    while(sc0.hasNextLine()) {
      String[] vals = sc0.nextLine().split(" ");
      truthMap.put(Integer.valueOf(vals[0]), Integer.valueOf(vals[1]));
    }
    //test Catalog, map of ith line => fileIndex
    Map<Integer, Integer> catalog = new HashMap<>();
    Scanner sc1 = new Scanner(new File("testCatalog.txt"));
    while(sc1.hasNextLine()) {
      String[] vals = sc1.nextLine().split(" ");
      catalog.put(Integer.valueOf(vals[0]), Integer.valueOf(vals[1]));
    }

    //index-score top 50
    List<Tuple> scoreList = new LinkedList<>();
    Scanner sc = new Scanner(new File("out1"));
    int lineCount = 0;
    sc.nextLine();
    while (sc.hasNextLine()) {
      lineCount++;
      String line = sc.nextLine();
      String[] vals = line.split(" ");
      Double score = Double.valueOf(vals[2]);
      int fileIndex = catalog.get(lineCount);
      scoreList.add(new Tuple(fileIndex, score));
    }
    scoreList.sort(new Comparator<Tuple>() {
      @Override
      public int compare(Tuple o1, Tuple o2) {
        return Double.compare(o2.score, o1.score);
      }
    });
    StringBuilder sb = new StringBuilder();
    int correctCount = 0;
    for(int i = 0; i < 50; i++) {
      int fileIndex = scoreList.get(i).index;
      sb.append(fileIndex).append("\n");
      if (truthMap.get(fileIndex) == 1) {
        correctCount++;
      }
    }
    System.out.println("Accuracy top50 small: " + (double) correctCount/ (double) 50);
    sb.append("Accuracy: ").append((double) correctCount/ (double) 50).append("\n");
    FileUtils.write(new File("top/top50small.txt"), sb.toString(), "UTF-8");
  }
  // read truth.txt into map <Integer, Integer>, spam = 1, ham = 0

  public static void main(String[] str) throws IOException {
    Top50 tp = new Top50();
    tp.top50Small();
    tp.top50All();
  }
}
