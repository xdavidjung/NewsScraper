package edu.washington.cs.knowitall.newsconverter;

import edu.washington.cs.knowitall.browser.extraction.ReVerbExtraction
import edu.washington.cs.knowitall.collection.immutable.Interval
import edu.washington.cs.knowitall.tool.chunk.ChunkedToken
import edu.washington.cs.knowitall.tool.tokenize.OpenNlpTokenizer

import net.liftweb.json._

import scala.io.Source

import java.io.File

object ConverterMain {
  
  val tokenizerLocal = new java.lang.ThreadLocal[OpenNlpTokenizer]() { override def initialValue = new OpenNlpTokenizer }
  def tokenizer = tokenizerLocal.get
  
  def main(args: Array[String]): Unit = {
    
    parser.parse(args, Config()) map { config =>
      // if the user specifies a file use it, otherwise assume the 
      // data comes from standard input
      val newsDatum = if (config.file != None) readJson(config.file.get) 
                      else getJson
      
      // for each news data, look at the extractions
      val extrs = newsDatum.foreach(newsData => {
        val extractions = newsData.extractions
        val url = newsData.url
        
        // for each extraction, build the ReVerbExtraction object and output it.
        extractions.foreach(ext => {
          val chunkTags = ext.chunkTags.split(" ").iterator
          val posTags = ext.posTags.split(" ").iterator
          val sent = tokenizer.tokenize(ext.sent).map(_.string)
          val sentIterator = sent.iterator
          val offsets = ext.offsets.split("\\) \\[").map(getOffset).iterator
          
          val chunkedTokens = new Array[ChunkedToken](sent.length)
          for (n <- 0 to (sent.length - 1)) {
            chunkedTokens(n) = new ChunkedToken(
                  chunkTags.next, 
                  posTags.next, 
                  sentIterator.next, 
                  offsets.next)
          }
          
          val indChunkedTokens = chunkedTokens.toIndexedSeq
          val re = new ReVerbExtraction(
            indChunkedTokens,
            getInterval(ext.rArg1),
            getInterval(ext.rRel),
            getInterval(ext.rArg2),
            url)
          println(ReVerbExtraction.serializeToString(re))
        })
      })
    } getOrElse {
      // arguments are bad, usage message will have been displayed
      // don't think it's possible to get here though.
    }
  }
  
  /** for parsing json */
  implicit val formats = DefaultFormats
  
  /** ReVerb extractions. */
  case class Extraction(
    sent: String,
    arg1: String,
    rArg1: String,
    relation: String,
    rRel: String,
    arg2: String,
    rArg2: String,
    chunkTags: String,
    posTags: String,
    offsets: String,
    confidence: String)
  
  /** Contains information about the article as a whole. */
  case class NewsData(
    title: String,
    date: String,
    imgAlt: String,
    imgTitle: String,
    content: String,
    category: String,
    subCategory: String,
    url: String,
    source: String,
    imgUrl: String,
    extractions: List[Extraction])
  
  // defines how command line args are parsed.
  case class Config(file: Option[File] = None)
  
  // scopt parser
  private val parser = new scopt.immutable.OptionParser[Config]("news-converter", "1.0") { 
    def options = Seq(
      argOpt("<input-file>", "json file with news data to read") { 
        (path: String, c: Config) => c.copy(file = Some(new File(path)))
      }
    )
  }
  
  /** Given a String that represents an open Interval i, returns i.
   * @require str must be of the form "[a, b)".
   * @return the Interval object represented by str.
   */
  def getInterval(str: String): Interval = {
    val stringPair = str.tail.init.split(", ")

    stringPair(0) = stringPair(0)
    stringPair(1) = stringPair(1)
    val intPair = stringPair.map(intStr => intStr.toInt)
    intPair(0)
    
    Interval.open(intPair(0), intPair(1))
  }
  
  /** Given a String that represents an open Interval [a, b), returns a.
   * @require str must be formatted as "a, b", "[c, d", or "e, f)".
   * @return the first parameter of the Interval represented by str.
   */
  def getOffset(str: String): Int = {
    val stringPair = str.split(", ")
    
    if (stringPair(0).head == '[') stringPair(0).tail.toInt
    else stringPair(0).toInt
  }
  
  /**
    * Reads lines from standard input and returns an appropriate list
    * of News objects.
    *
    * The input must be the lines of a json file in the appropriate format.
    */
  def getJson: List[NewsData] = {
    val source = Source.fromInputStream(System.in, "UTF-8")
    val jsonString = source.getLines.mkString
    
    source.close()
    val json = parse(jsonString)
    json.extract[List[NewsData]]
  }
  
  /**
   * Reads lines from stdin and returns an appropriate list of News
   * objects. 
   * 
   * The lines from stdin must form a json file in the appropriate format. 
   */
  def readJson(file: File): List[NewsData] = {
    val source = Source.fromFile(file, "UTF-8")
    val jsonString = source.getLines.mkString
    source.close()
    
    val json = parse(jsonString)
    json.extract[List[NewsData]]
  }
}
