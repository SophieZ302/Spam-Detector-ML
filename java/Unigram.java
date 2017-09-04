import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Unigram {

  public static Set<String> getAll(RestClient restClient) {
    StringBuilder sb = new StringBuilder();
    Set<String> rst = new HashSet<>();
    for (int i = 1; i <= 75419; i++) {
      if (i%100 == 0) {
        System.out.println("read file " + i);
      }
    //for (int i = 1; i < 3; i++) {
      try {
        String docId = String.valueOf(i);
        Response response1 = restClient.performRequest("GET",
                "/trec07spam/document/" + docId + "/_termvectors");
        String response = EntityUtils.toString(response1.getEntity());
        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(response).getAsJsonObject();
        if (object.getAsJsonObject("term_vectors") != null) {
          JsonObject textInfo = object.getAsJsonObject("term_vectors").getAsJsonObject("text").getAsJsonObject("terms");
          Type mapType = new TypeToken<Map<String, Map>>() {
          }.getType();
          Map<String, String[]> son = new Gson().fromJson(textInfo.toString(), mapType);
          rst.addAll(son.keySet());
        } else {
          System.out.println(docId + " " + response);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } catch (NullPointerException n) {
        n.printStackTrace();
        System.out.println();
      }
    }

    System.out.println("writing file");
    for (String s :rst) {
      sb.append(s).append("\n");
    }
    try{
      FileUtils.write(new File("allgram.txt"), sb.toString(), "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return rst;
  }

  public static void main(String[] str) throws IOException {
    RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")).build();
    Set<String> all = Unigram.getAll(restClient);
    System.out.println(all.size());
    restClient.close();
  }
}
