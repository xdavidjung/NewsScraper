package edu.washington.cs.knowitall;
import edu.washington.cs.knowitall.tool.chunk.OpenNlpChunker
import edu.washington.cs.knowitall.browser.extraction.ReVerbExtraction
import edu.washington.cs.knowitall.collection.immutable.Interval

import com.codahale.jerkson.Json._

import scala.io.Source

object ConverterMain {

  def main(args: Array[String]): Unit = {

    val chunker = new OpenNlpChunker

    val newsDatum = getJson

    // for each news data, output the relevant ReVerbExtraction data
    newsDatum.foreach(news => {
      val extractions = news.newsData.extractions
      val url = news.newsData.url

      // for each extraction, build the ReVerbExtraction object and output it.
      extractions.foreach(ext => {
        val chunkTags = ext.chunkTags.split(" ")
        val posTags = ext.posTags.split(" ")
        
        // leaves offsets in format ["[a, b", "c, d", "e, f)"]
        val offsets = ext.offsets.split(") [")
        val sent = ext.sent
        
        val re = new ReVerbExtraction(
          chunker.chunk(ext.sent).toIndexedSeq,
          getInterval(ext.rArg1),
          getInterval(ext.rRel),
          getInterval(ext.rArg2),
          url)
        ReVerbExtraction.toTabDelimited(re)
      })
    })
  }

  /** Given a String that represents an open Interval i, returns i.
   * @require str must be formatted as "a, b", "[c, d", or "e, f)".
   * @return the Interval represented by str.
   */
  def getInterval(str: String): Interval = {
    val stringPair = str.tail.init.split(", ")
    val intPair = stringPair.map(intStr => {
      // handle the "[c, d" case
      if (intStr.head == "[") intStr.tail.toInt
      // handle the "e, f)" case
      else if (intStr.charAt(intStr.length - 1) == ")") intStr.init.toInt
      // handle the "a, b" case
      else intStr.toInt
    })
    Interval.open(intPair(0), intPair(1))
  }
  
  /**
    * Reads lines from standard input and returns an appropriate list
    * of News objects.
    *
    * The file must be a json file in the appropriate format.
    */
  def getJson: List[News] = {
    val jsonString = Source.stdin.getLines.mkString
    parse[List[News]](jsonString)
  }
  
  /** This class is necessary because NewsData objects are wrapped by
    * some numeric id.
    */
  case class News(newsData: NewsData)

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
}
