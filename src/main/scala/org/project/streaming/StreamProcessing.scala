package org.esgi.project.streaming

import io.github.azhur.kafkaserdeplayjson.PlayJsonSupport
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.kstream.{JoinWindows, Printed, SessionWindows, TimeWindows, Windowed}
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream.{KTable, _}
import org.esgi.project.streaming.StreamProcessing.{buildProperties, builder, groupName}
import org.project.streaming.models.{KeyView, Likes, Views}
//import org.esgi.project.streaming.models.{MeanLatencyForURL, Metric, Visit, VisitWithLatency}

import java.io.InputStream
import java.time
import java.time.Duration
import java.util.Properties

object StreamProcessing  extends  PlayJsonSupport {
  import org.apache.kafka.streams.scala.ImplicitConversions._
  import org.apache.kafka.streams.scala.serialization.Serdes._
  val groupName: String ="Groupe-25IABD1"
  val applicationName = s"web-events-stream-app-$groupName"
  val viewsTopicName: String = "views"
  val likesTopicName: String = "likes"


  val props = buildProperties
  val builder: StreamsBuilder = new StreamsBuilder

  val views: KStream[String, Views] = builder.stream[String, Views](viewsTopicName)
  val likes : KStream[String,Likes]= builder.stream[String,Likes](likesTopicName)


  //views.print(Printed.toSysOut[String, Views])
  //likes.print(Printed.toSysOut[String, Likes])

  val viewsGroupById: KGroupedStream[Int,Views]=views.map((_,view)=>(view._id,view)).groupByKey

  val viewsGroupByTypeAndTitle= views.map((_, view)=> (KeyView(view._id,view.view_category,view.title),view)).groupByKey

  val viewsSinceBegining= viewsGroupByTypeAndTitle.count()(Materialized.as("viewsSinceBegining"))
  // .count().toStream.print(Printed.toSysOut[KeyView,Long])

  val viewedSince1Minute : KTable[Windowed[KeyView], Long] =
    viewsGroupByTypeAndTitle.windowedBy(TimeWindows.of(time.Duration.ofMinutes(1)).advanceBy(time.Duration.ofSeconds(30)))
      .count()(Materialized.as("viewedSince1Minute"))

  viewedSince1Minute.toStream.print(Printed.toSysOut[Windowed[KeyView],Long])

  //val viewedSince5Minute : KTable[Windowed[KeyView], Long] =
    viewsGroupByTypeAndTitle.windowedBy(TimeWindows.of(time.Duration.ofMinutes(5)).advanceBy(time.Duration.ofSeconds(30)))
      .count()(Materialized.as("viewedSince5Minute"))
      //.toStream.print(Printed.toSysOut[Windowed[KeyView],Long])

  //TODO : Calculer les Meilleurs Scores >4 et les Pires Scores <2 :
  //TODO : Utiliser une moyenne mobile depuis le début du lancement :
  //TODO : Moyenne Mobile: d1, d2 , d3 => (d1+d2),(d1+d3),(d2+d3)

  val likesGroupById = likes.map((_,like)=>(like._id,like)).groupByKey
  val bestEvals=likes.map((_,likes)=> (likes.EvalBestFilms(likes._id,likes.score),likes))
  val worstEvals=likes.map((_,likes)=> (likes.EvalWorstFilms(likes._id,likes.score),likes))
  val moyenne= likes.map((_,likes)=>(likes.score)).groupByKey.aggregate()

  def run(): KafkaStreams = {
    val streams: KafkaStreams = new KafkaStreams(builder.build(), props)
    streams.start()

    // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable() {
      override def run {
        streams.close
      }
    }))
    streams
  }

  def buildProperties: Properties = {
    import org.apache.kafka.clients.consumer.ConsumerConfig
    import org.apache.kafka.streams.StreamsConfig
    val inputStream: InputStream = getClass.getClassLoader.getResourceAsStream("kafka.properties")

    val properties = new Properties()
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationName)
    // Disable caching to print the aggregation value after each record
    properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0")
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    properties.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "-1")
    properties.load(inputStream)
    properties
  }

}
