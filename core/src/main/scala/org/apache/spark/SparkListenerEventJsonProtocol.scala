package org.apache.spark

import org.apache.spark.scheduler.SparkListenerEvent
import org.apache.spark.util.JsonProtocol
import org.json4s.jackson.JsonMethods
import org.json4s.string2JsonInput

object SparkListenerEventJsonProtocol {
  def parse(input: String): SparkListenerEvent = {
    JsonProtocol.sparkEventFromJson(JsonMethods.parse(input))
  }
}
