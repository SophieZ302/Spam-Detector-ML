import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class SpamFinder {

  public static void cleanSpam() throws IOException {
    Set<String> spamSet = new HashSet<>();
    List<String> spamList = new ArrayList<>();
    Scanner spamsc = new Scanner(new File("appendspam.txt"));
    while (spamsc.hasNextLine()) {
      String st = spamsc.nextLine();
      if (!spamSet.contains(st)) {
        spamList.add(st);
      }
      spamSet.add(st);
    }
    StringBuilder sb = new StringBuilder();
    for (String t : spamList) {
      sb.append(t).append("\n");
    }
    FileUtils.write(new File("appendspam.txt"), sb.toString(), "UTF-8");
  }

  public static class Tuple {
    int index;
    double score;
    public Tuple(int index, double score) {
      this.index = index;
      this.score = score;
    }
  }

  public static void findSpam() throws IOException {
    Map<Integer, String> spamMap = new HashMap<>();
    Scanner spamsc = new Scanner(new File("allgram.txt"));
    int j = 0;
    while (spamsc.hasNextLine()) {
      String st = spamsc.nextLine();
      j++;
      spamMap.put(j, st);
    }
    Scanner sc = new Scanner(new File("model2.txt"));
    int index = 0;
    List<Tuple> tlist = new LinkedList<>();
    while (sc.hasNextLine()) {
      String st = sc.nextLine();
      index++;
      //System.out.println(index + st);
      double score = Double.valueOf(st);
      Tuple t = new Tuple(index, score);
      tlist.add(t);
    }
    tlist.sort(new Comparator<Tuple>() {
      @Override
      public int compare(Tuple o1, Tuple o2) {
        double valA = Math.abs(o1.score);
        double valB = Math.abs(o2.score);
        return Double.compare(valB, valA);
      }
    });

    int i = 0;
    StringBuilder sb = new StringBuilder();
    for (Tuple t : tlist) {
      i++;
      String val = t.index + " " + spamMap.get(t.index) + " "+ t.score + "\n";
      System.out.print(val);
      sb.append(spamMap.get(t.index) + "\n");
    }
    FileUtils.write(new File("newspam.txt"), sb.toString(), "UTF-8");
  }

  public static void main(String[] args) throws IOException {
      //findSpam();
      cleanSpam();
  }
}
