/** note: this only appears to work if you start your netcat listener prior to launching, eg:
nc -lk 9999.  But, to make things more useful, do something like this:
1) mkfifo /mapr/cluster/input
2) tail -f /mapr/cluster/input | nc -lk 9999
3) start this spark app to connect to the port
4) echo/cat stuff into /mapr/cluster/input, eg: `for i in SensorDataV5.csv |sort -k2 -k3 -t $',' `;do echo $i > in;sleep .01;done;
This should allow the connection to stay open, and be able to shove whatever you want into the FIFO and have it show up on the spark side.
*/

/*the idea here is that you'll have a netcat stream of CSV data coming in, and we need to:
1. insert/append the row into M7-tables
2. write/append the CSV to disk, for now just using loopback NFS.
*/

/*TODO:
- decide how to chunk output files up..eg: every minute? hour? day? # of rows?
- nice-to-have: figure out dstream 'windows' and spit something out to web/D3 somehow....
- Take out any hardcoded paths.
- When saving RDD to disk, may be useful to output with the 'dateTime' squashed field instead of the date,time fields (for tableau)
- use KAFKA or another messaging system as the input (per Ted)
*/




package org.apache.spark.streaming.m7import

import java.io._
import scala.io.Source
import scala.util.Random
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.IntParam
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.{HBaseAdmin,HTable,Put,Get}
import org.apache.hadoop.hbase.util.Bytes
import com.google.common.io.Files
import java.nio.charset.Charset
//for json conversion:
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

/** probably better to package the logging function up as a separate class, but for now this is fine */

import org.apache.spark.Logging
import org.apache.log4j.{Level, Logger}



/** Utility functions for Spark Streaming examples. */
object StreamingExamples extends Logging {

  /** Set reasonable logging levels for streaming if the user has not configured log4j. */
  def setStreamingLogLevels() {
    val log4jInitialized = Logger.getRootLogger.getAllAppenders.hasMoreElements
    if (!log4jInitialized) {
      // We first log something to initialize Spark's default logging, then we override the
      // logging level.
      logInfo("Setting log level to [WARN] for streaming example." +
        " To override add a custom log4j.properties to the classpath.")
      Logger.getRootLogger.setLevel(Level.WARN)
    }
    Logger.getRootLogger.setLevel(Level.WARN)
  }
}


/** now to the actual work..borrowed from spark streaming examples */

object m7import {
  def main(args: Array[String]) {
    if (args.length < 6) {
      System.err.println("Usage: m7import <master> <hostname> <port> <batchsecs> </path/to/tablename> </path/to/outputFile> </path/to/d3.json>\n" +
        "In local mode, <master> should be 'local[n]' with n > 1")
      System.exit(1)
    }

  
    StreamingExamples.setStreamingLogLevels()

     val Array(master, host, IntParam(port), IntParam(batchsecs), tablename, outputPath, d3Input) = args

     //time to write an output file..we should probably split it into multiples but for now we'll just stream to a single file.

    val outputFile = new File(outputPath)
    if (outputFile.exists()) {
      outputFile.delete()
      } 

    //force spark to look for an open port to use for this context
    System.setProperty("spark.ui.port", "5050")
    System.setProperty("spark.cores.max", "2")
    // Create the context with a X second batch size, where X is the arg you supplied as 'batchsecs'.

    val ssc = new StreamingContext(master, "M7import", Seconds(batchsecs),
      System.getenv("SPARK_HOME"), StreamingContext.jarOfClass(this.getClass))

    //instantiate m7/hbase connection ahead of time.

    val conf = HBaseConfiguration.create()
    
    val admin = new HBaseAdmin(conf)

    if(!admin.isTableAvailable(tablename )) {
        println("Table doesn't exist..quitting")
        System.exit(1)
        //admin.createTable(tableDesc)
      }

      val table = new HTable(conf, tablename)


    

    // Create a NetworkInputDStream on target ip:port and count the
    // words in input stream of \n delimited text (eg. generated by 'nc')
    val records = ssc.socketTextStream(host, port.toInt, StorageLevel.MEMORY_ONLY_SER)





    //not needed for this excercise.  split each line into fields, delimited by ,
    //val words = records.flatMap(_.split(","))


    //basically, foreach rdd inside the Dstream, perform a 'collect' on the RDD, which creates an array, 
    // and run a foreach on the elements within the array.  Maybe there's a more 'sparky' way of doing this..so sue me.
    records.foreach(rdd => {
      val rddarray = rdd.collect
        if(rddarray.length > 0) {
          var linecount = 0
          for(line <- rddarray) {
            linecount += 1
             //time to split this row into words, from scala-cookbook, the .trim removes leading/trailing
             //spaces from the values.
            val Array(resID, date, time, hz, disp, flo, sedPPM, psi, chlPPM) = line.split(",").map(_.trim)
            //since tableau is lame about datefields, need to combine date+time
            val dateTime = date + " " + time
            // Time to create a compositekey
            val compositeKey = resID + "_" + dateTime

            //now we need some code to shove into m7..

            //println(s"Writing this to m7: $resID,$date,$time,$hz,$disp,$flo,$sedPPM,$psi,$chlPPM")
            
            //OLD: generate a random number to use for a key
            //val myKey=Random.nextInt(Integer.MAX_VALUE)

            val tblPut = new Put(Bytes.toBytes(compositeKey))
            //build our tblPut object with multiple columns.
            // TODO: probably better done w/ a loop, but that's for another day.

            tblPut.add(Bytes.toBytes("cf1"),Bytes.toBytes("resID"),Bytes.toBytes(resID))
            tblPut.add(Bytes.toBytes("cf1"),Bytes.toBytes("date"),Bytes.toBytes(dateTime))
            tblPut.add(Bytes.toBytes("cf1"),Bytes.toBytes("hz"),Bytes.toBytes(hz))
            tblPut.add(Bytes.toBytes("cf1"),Bytes.toBytes("disp"),Bytes.toBytes(disp))
            tblPut.add(Bytes.toBytes("cf1"),Bytes.toBytes("flo"),Bytes.toBytes(flo))
            tblPut.add(Bytes.toBytes("cf1"),Bytes.toBytes("sedPPM"),Bytes.toBytes(sedPPM))
            tblPut.add(Bytes.toBytes("cf1"),Bytes.toBytes("psi"),Bytes.toBytes(psi))
            tblPut.add(Bytes.toBytes("cf1"),Bytes.toBytes("chlPPM"),Bytes.toBytes(chlPPM))

    
            table.put(tblPut)
            Files.append(resID + "," + dateTime + "," + hz + "," + disp + "," + flo + "," + sedPPM + "," + psi + "," + chlPPM + "\n", outputFile, Charset.defaultCharset())


          
          }
          /*now that each of the rows are in m7 , lets dump the entire RDD to disk.
          NOTE: we're not writing this with our 'datetime' column...
          note: this is sort of annoying..it saves to a new folder with a 'part-0000' file..just like a MR job
          */
          
          /*val csvDir = Random.nextInt(Integer.MAX_VALUE)
          rdd.saveAsTextFile("/mapr/shark/CSV/" + csvDir )
          */
          println("dumped " + linecount + " rows to table " + tablename + " and wrote them to " + outputPath)

          
         
          //needless println
          //println("record array length: " + rddarray.length + " first row: " + rddarray(0))
        }
    })

//lets also try and save the whole dstream.  this is even uglier than saving each RDD..because it even saves 
//EMPTY dstreams!  Probbaly should insert a check here..
   // records.saveAsTextFiles("/mapr/shark/CSV/")

       //sliding window testing, this makes a new dstream containing the last 60 seconds of data..every 15 seconds
    val slidingWindow = records.window(Seconds(60), Seconds(15))

        //basically, foreach rdd inside the Dstream, perform a 'collect' on the RDD, which creates an array, 
    // and run a foreach on the elements within the array.  Maybe there's a more 'sparky' way of doing this..so sue me.
    slidingWindow.foreach(rdd => {
      //this only works because there's only one RDD per dstream in this caseprintln("window RDD")
      val d3File = new File(d3Input)
      if (d3File.exists()) {
        d3File.delete()
      }

      val rddarray = rdd.collect
        if(rddarray.length > 0) {
          for(line <- rddarray) {
             //time to split this row into words, from scala-cookbook, the .trim removes leading/trailing
             //spaces from the values.
            val Array(resID, date, time, hz, disp, flo, sedPPM, psi, chlPPM) = line.split(",").map(_.trim)
            //since tableau is lame about datefields, need to combine date+time
            val dateTime = date + " " + time

              val json = 
              ("PumpID" -> resID) ~
              ("date" -> date) ~
              ("time" -> time) ~
              ("HZ" -> hz) ~
              ("Displacement" -> disp) ~
              ("Flow" -> flo) ~
              ("SedimentPPM" -> sedPPM) ~
              ("PSI" -> psi) ~
              ("ChlorinepPM" -> chlPPM)
            Files.append(pretty(render(json)) + "\n", d3File, Charset.defaultCharset())


          
          }
        }
    })


    //what happens when we spit this to the screen?
    //slidingWindow.print()

            //   val json = 
            //   ("PumpID" -> resID) ~
            //   ("date" -> date) ~
            //   ("time" -> time) ~
            //   ("HZ" -> hz) ~
            //   ("Displacement" -> disp) ~
            //   ("Flow" -> flo) ~
            //   ("SedimentPPM" -> sedPPM) ~
            //   ("PSI" -> psi) ~
            //   ("ChlorinepPM" -> chlPPM)
            // println(pretty(render(json)))

    // print lines to console
    //records.print()
    ssc.start()             // Start the computation
    ssc.awaitTermination()  // Wait for the computation to

  }
}