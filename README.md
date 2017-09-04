# Spam-Detector-ML

A Spam Classifier using **Machine Learning** and **ElasticSearch**.

Keywords: **java**, **sparse format**, **liblinear**, **weka**, **Linear Regression**, **Naive Bayse**

### Data
A 255 MB Corpus ([trec07p.tgz](https://plg.uwaterloo.ca/~gvcormac/treccorpus07/)) provides set of emails annotated for spam

### Clean Data

All email files are written in Multipurpose Internet Mail Extensions (MIME, [wiki page](https://en.wikipedia.org/wiki/MIME)) format. 
* extract the content using [Apache James MIME4J library](https://james.apache.org/mime4j/). 
* parse the html body using [Jsoup](https://jsoup.org/) library
* clean content with Regex, eliminate non-englinsh characters and unneccssary notations

### Upload ElasticSearch
* reformat cleaned content into Json using [Gson](https://github.com/google/gson) Library
* randomly assign 80% trainning data and 20% testing data 
* upload to ElasticSearch using its [REST api](https://www.elastic.co/guide/en/elasticsearch/reference/5.2/docs.html)

### Generate Sparse Matrix
* generate lists of spam words using two strategies. These will be the features (columns) of the data matrix.  
  1.  manuelly generate a list of spam related words,  for example : “free” , “win”, “porn”, “click here”, etc. 
  2.  use ElasticSearch to get all unigrams for entire corpus
* Generate term frequencies using ElasticSearch
* Save values into sparse format, together with a catalog file recording file docId corresponds to which line of sparse data

### Train and Test
* Train the 80% dataset using LibLinear's linear regression model，to generate a model file
* Generate prediction on the 20% dataset
* Calculate Precision of the testing set

### Results and Inprovements
Using all unigrams as features results in a average precision of 99% :+1:

However the manuel list of smaller size only had accuracy around 75%, to improve result, I used another machine learning algorithm : Naive Bayes. It outperforms other machine learning algorithms in case of spam prediction.   

[Weka](https://weka.wikispaces.com/Use+WEKA+in+your+Java+code) provides good naive bayes libaray, one only need to reformat the sparse matrix into [.ARFF](https://weka.wikispaces.com/ARFF) format to run the algorithm. The result accuracy has increased to about 80% but still not enough.


At the end, I look into the learned model provided by ALL Unigram Training set, and take the top 30 highest absolute value score from the model, indicating the most effective indicator of spam detection. Used catalog file to find the corresponding words, and added them into the short list. 

Rerun liblinear on the new list of about 50 spam key words, the average accuracy reached 96% overall and 98% for top 50 spam files :smile:


