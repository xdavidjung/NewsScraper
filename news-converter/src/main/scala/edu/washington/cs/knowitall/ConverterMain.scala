package edu.washington.cs.knowitall;

object ConverterMain {
  
  def main(args: Array[String]): Unit = {
    // read from standard input, parse as JSON object. 
    // chunk from "sent" field to get List[ChunkedToken].

    // problem: We need offsets for the arg1/2 and REL;
    // right now we have character offsets but i think we
    // need to specify token offsets instead.  
  }

}
