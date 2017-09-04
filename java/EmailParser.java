import com.google.gson.Gson;

import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.stemmer.Stemmer;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.message.DefaultBodyDescriptorBuilder;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptorBuilder;
import org.apache.james.mime4j.stream.MimeConfig;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import tech.blueglacier.email.Attachment;
import tech.blueglacier.email.Email;
import tech.blueglacier.parser.CustomContentHandler;

public class EmailParser {
  //[documentId -> spam/ham]
  Map<String, String> truthTabel;
  Random r;
  static PorterStemmer stemmer = new PorterStemmer();

  public EmailParser(RestClient restClient) {
    truthTabel = new HashMap<String, String>();
    setUpTruth();
    System.out.println("build truth table: " + truthTabel.size());
    r = new Random();
    readEmail(restClient);
  }

  private void setUpTruth() {
    try {
      Scanner sc = new Scanner(new FileInputStream(new File("/Users/sophie/Desktop/trec07p/full/index")));
      while (sc.hasNextLine()) {
        String s = sc.nextLine().replaceAll(" ","");
        String[] st = s.split("\\.\\./data/inmail.");
        truthTabel.put(st[1], st[0]);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void readEmail(RestClient restClient) {
    StringBuilder catalog = new StringBuilder();
    String pathName = "/Users/sophie/Desktop/trec07p/data/inmail.";
    for (int i = 1; i <= 75419; i++) {
    //for (int i = 1; i <= 5; i++) {
    //for (int i = 51564; i <= 51564; i++) {
      String split = randomSplit();
      String label = truthTabel.get(String.valueOf(i));
      String content = "";
      try {
        ContentHandler contentHandler = new CustomContentHandler();
        MimeConfig mime4jParserConfig = MimeConfig.DEFAULT;
        BodyDescriptorBuilder bodyDescriptorBuilder = new DefaultBodyDescriptorBuilder();
        MimeStreamParser mime4jParser = new MimeStreamParser(mime4jParserConfig, DecodeMonitor.SILENT, bodyDescriptorBuilder);
        mime4jParser.setContentDecoding(true);
        mime4jParser.setContentHandler(contentHandler);
        InputStream mailIn = new FileInputStream(pathName + i);
        mime4jParser.parse(mailIn);
        Email email = ((CustomContentHandler) contentHandler).getEmail();
        content = emailExtract(email);
        //System.out.println(content);
        upload2ES(String.valueOf(i), label, content, split, restClient);

      } catch (IOException e) {
        upload2ES(String.valueOf(i), label, content, split, restClient);
        catalog.append("\nfile: ").append(i).append(" ").append(e);
        e.printStackTrace();
      } catch (MimeException m) {
        upload2ES(String.valueOf(i), label, content, split, restClient);
        catalog.append("\nfile: ").append(i).append(" ").append(m);
        m.printStackTrace();
      } catch (UnsupportedCharsetException c) {
        upload2ES(String.valueOf(i), label, content, split, restClient);
        catalog.append("\nfile: ").append(i).append(" ").append(c);
        c.printStackTrace();
      } catch (IllegalCharsetNameException il) {
        upload2ES(String.valueOf(i), label, content, split, restClient);
        catalog.append("\nfile: ").append(i).append(" ").append(il);
        il.printStackTrace();
      } catch (Exception ex) {
        upload2ES(String.valueOf(i), label, content, split, restClient);
        catalog.append("\nfile: ").append(i).append(" ").append(ex);
        ex.printStackTrace();
      }
      try {
        FileUtils.write(new File("catalog.txt"), catalog.toString());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static String emailExtract(Email email) {
    StringBuilder sb = new StringBuilder();
    List<Attachment> attachments = email.getAttachments();
    Attachment calendar = email.getCalendarBody();
    Attachment htmlBody = email.getHTMLEmailBody();
    Attachment plainText = email.getPlainTextEmailBody();

    //email.getHeader().getField()

    String subject = email.getEmailSubject();
    sb.append(subject).append("\n");
    if (htmlBody != null) {
      String charset = htmlBody.getBd().getCharset();
      try {
        Document doc = Jsoup.parse(htmlBody.getIs(), charset, "");
        sb.append(doc.body().text()).append("\n");
      }catch (IOException e) {
        e.printStackTrace();
        try {
          Document doc = Jsoup.parse(htmlBody.getIs(), "UTF-8", "");
          sb.append(doc.body().text()).append("\n");
        } catch (IOException e2) {
          e2.printStackTrace();
        }
      }
    }
    if (plainText != null) {
      Scanner sc2 = new Scanner(plainText.getIs());
      while (sc2.hasNextLine()) {
        sb.append(sc2.nextLine()).append("\n");
      }
    }
    String rst = sb.toString().replaceAll("[^A-Za-z0-9$% ]", " ").toLowerCase().trim();
    rst = rst.replaceAll("$", " \\$ ");

    String[] wordList = rst.split(" ");
    StringBuilder frst = new StringBuilder();
    for (String st : wordList)  {
      if (!st.equals(" ")) {
        String s = stemmer.stem(st);
        frst.append(s).append(" ");
      }
    }
    return frst.toString();
  }

  private static String htmlParser(String html) {
    Document doc = Jsoup.parse(html);
    return doc.body().text();
  }

  public static class Index {
    public String file_name;
    public String label;
    public String split;
    public String text;

    public Index(String file_name, String label, String split, String text) {
      this.file_name = file_name;
      this.label = label;
      this.split = split;
      this.text = text;
    }
  }

  private String randomSplit() {
    String[] split = {"train", "test"};
    float chance = r.nextFloat();
    if (chance <= 0.20f) {
      return split[1];
    }
    return split[0];
  }

  public static void upload2ES(String documentID, String label, String text, String split, RestClient restClient) {
    System.out.println(documentID + " " + label + split);
    Index index = new Index(documentID, label, split, text);
    Gson gson = new Gson();
    String json = gson.toJson(index);
    HttpEntity entity = new NStringEntity(
            json, ContentType.APPLICATION_JSON);
    try {
      Response indexResponse = restClient.performRequest(
              "PUT",
              "/trec07spam/document/" + documentID,
              Collections.<String, String>emptyMap(),
              entity);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void testReadEmail(String html) {
    StringBuilder catalog = new StringBuilder();
    String pathName = "/Users/sophie/Desktop/trec07p/data/inmail.";
    //for (int i = 1; i <= 75419; i++) {
      for (int i = 65408; i <= 65408; i++) {
      try {
        ContentHandler contentHandler = new CustomContentHandler();
        MimeConfig mime4jParserConfig = MimeConfig.DEFAULT;
        BodyDescriptorBuilder bodyDescriptorBuilder = new DefaultBodyDescriptorBuilder();
        MimeStreamParser mime4jParser = new MimeStreamParser(mime4jParserConfig, DecodeMonitor.SILENT, bodyDescriptorBuilder);
        mime4jParser.setContentDecoding(true);
        mime4jParser.setContentHandler(contentHandler);
        InputStream mailIn = new FileInputStream(pathName + i);
        mime4jParser.parse(mailIn);
        Email email = ((CustomContentHandler) contentHandler).getEmail();

        StringBuilder sb = new StringBuilder();

        Attachment htmlBody = email.getHTMLEmailBody();
        Attachment plainText = email.getPlainTextEmailBody();

        String subject = email.getEmailSubject();
        sb.append(subject).append("\n");
        if (plainText != null) {
          Scanner sc2 = new Scanner(plainText.getIs());
          while (sc2.hasNextLine()) {
            sb.append(sc2.nextLine()).append("\n");
          }
        }


        if (htmlBody != null) {
          String charset = htmlBody.getBd().getCharset();
          Document doc = Jsoup.parse(htmlBody.getIs(), charset, "");
          sb.append( doc.body().text());
        }

        String rst = sb.toString().replaceAll("[^A-Za-z0-9$%\n ]", " ");
        System.out.println(rst);

      } catch (IOException e) {
        catalog.append("\nfile: ").append(i).append(" ").append(e);
        e.printStackTrace();
      } catch (MimeException m) {
        catalog.append("\nfile: ").append(i).append(" ").append(m);
        m.printStackTrace();
      } catch (UnsupportedCharsetException c) {
        catalog.append("\nfile: ").append(i).append(" ").append(c);
        c.printStackTrace();
      } catch (IllegalCharsetNameException il) {
        catalog.append("\nfile: ").append(i).append(" ").append(il);
        il.printStackTrace();
      } catch (Exception ex) {
        catalog.append("\nfile: ").append(i).append(" ").append(ex);
        ex.printStackTrace();
      }

      System.out.println(catalog.toString());
    }
  }


  public static void main(String[] str) throws IOException, MimeException {
    RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")).build();
    EmailParser ep = new EmailParser(restClient);
    restClient.close();
  }
}
