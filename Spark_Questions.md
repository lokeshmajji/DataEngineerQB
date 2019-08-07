# Spark Questions

## Spark and Scala Versions ?
    Spark 2.3.3 - Scala 2.11.x - Hadoop 2.7 - Java 8
    Spark 1.6.0 - Scala 2.10.x - Hadoop 2.3 - Java 7+

## Hive Versions ?
    2017 - 2.3.0 - only supports Hadoop 2.x.y
    2016 - 2.0.0 - Supports Hadoop 1.x.y, 2.x.y
    2015 - 1.2.1 - Supports Hadoop 1.x.y, 2.x.y
    
## What is a RDD?
    DD is an immutable distributed collection of elements of your data, partitioned across nodes in your cluster that can be operated in parallel with a low-level API that offers transformations and actions.
## When you use RDD?
    you want low-level transformation and actions and control on your dataset;
    your data is unstructured, such as media streams or streams of text;
    you want to manipulate your data with functional programming constructs than domain specific expressions;
## What is a DataFrame?
    a DataFrame is an immutable distributed collection of data. Unlike an RDD, data is organized into named columns, like a table in a relational database. Designed to make large data sets processing even easier, DataFrame allows developers to impose a structure onto a distributed collection of data, allowing higher-level abstraction;
    DataFrame = DataSet[Row]
    
## What is a Dataset?
    * Starting in Spark 2.0, Dataset takes on two distinct APIs characteristics: a strongly-typed API and an untyped API, as shown in the table below. Conceptually, consider DataFrame as an alias for a collection of generic objects Dataset[Row], where a Row is a generic untyped JVM object. Dataset, by contrast, is a collection of strongly-typed JVM objects, dictated by a case class you define in Scala or a class in Java. DataSet[T] // T can be a Case Class in Scala or a class in Java
    
    * First, because DataFrame and Dataset APIs are built on top of the Spark SQL engine, it uses Catalyst to generate an optimized logical and physical query plan. Across R, Java, Scala, or Python DataFrame/Dataset APIs, all relation type queries undergo the same code optimizer, providing the space and speed efficiency. Whereas the Dataset[T] typed API is optimized for data engineering tasks, the untyped Dataset[Row] (an alias of DataFrame) is even faster and suitable for interactive analysis.
    
    * Syntax and Analysis errors can be found compile time
    * Code:
      {"device_id": 198164, "device_name": "sensor-pad-198164owomcJZ", "ip": "80.55.20.25", "cca2": "PL", "cca3": "POL", "cn": "Poland", "latitude": 53.080000, "longitude": 18.620000, "scale": "Celsius", "temp": 21, "humidity": 65, "battery_level": 8, "c02_level": 1408, "lcd": "red", "timestamp" :1458081226051}
    
        case class DeviceIoTData (battery_level: Long, c02_level: Long, cca2: String, cca3: String, cn: String, device_id: Long, device_name: String, humidity: Long, ip: String, latitude: Double, lcd: String, longitude: Double, scale:String, temp: Long, timestamp: Long)
    
        val ds = spark.read.json(“/databricks-public-datasets/data/iot/iot_devices.json”).as[DeviceIoTData]
        

## Create DataFrames

    import org.apache.spark.sql.SparkSession
    val spark = SparkSession
        .builder()
        .appName("Spark SQL basic example")
        .config("spark.some.config.option", "some-value")
        .getOrCreate()
    // For implicit conversions like converting RDDs to DataFrames
    import spark.implicits._

    val df = spark.read.json("examples/src/main/resources/people.json")
    df.show()
    df.printSchema()
    df.select("name").show()
    df.select($"name", $"age" + 1).show()
    df.filter($"age" > 21).show()
    df.groupBy("age").count().show()

    // Register the DataFrame as a SQL temporary view
    df.createOrReplaceTempView("people")

    val sqlDF = spark.sql("SELECT * FROM people")
    sqlDF.show()

    // Register the DataFrame as a global temporary view
    df.createGlobalTempView("people")

    // Global temporary view is tied to a system preserved database `global_temp`
    spark.sql("SELECT * FROM global_temp.people").show()


## Creating Datasets
    case class Person(name: String, age: Long)
    // Encoders are created for case classes
    val caseClassDS = Seq(Person("Andy", 32)).toDS()
    caseClassDS.show()
    
    // Encoders for most common types are automatically provided by importing spark.implicits._
    val primitiveDS = Seq(1, 2, 3).toDS()
    primitiveDS.map(_ + 1).collect()

    // DataFrames can be converted to a Dataset by providing a class. Mapping will be done by name
    val path = "examples/src/main/resources/people.json"
    val peopleDS = spark.read.json(path).as[Person]
    peopleDS.show()


## Inferring the Schema Using Reflection
    // For implicit conversions from RDDs to DataFrames
        import spark.implicits._
    // Create an RDD of Person objects from a text file, convert it to a Dataframe
        val peopleDF = spark.sparkContext.textFile("examples/src/main/resources/people.txt").map(_.split(","))
                .map(attributes => Person(attributes(0), attributes(1).trim.toInt))
                .toDF()
        
        peopleDF.createOrReplaceTempView("people")
        val teenagersDF = spark.sql("SELECT name, age FROM people WHERE age BETWEEN 13 AND 19")

    // The columns of a row in the result can be accessed by field index
            teenagersDF.map(teenager => "Name: " + teenager(0)).show()
    // or by field name
            teenagersDF.map(teenager => "Name: " + teenager.getAs[String]("name")).show()
    
    // No pre-defined encoders for Dataset[Map[K,V]], define explicitly
    implicit val mapEncoder = org.apache.spark.sql.Encoders.kryo[Map[String, Any]]
    // Primitive types and case classes can be also defined as
    // implicit val stringIntMapEncoder: Encoder[Map[String, Any]] = ExpressionEncoder()

    // row.getValuesMap[T] retrieves multiple columns at once into a Map[String, T]
        teenagersDF.map(teenager => teenager.getValuesMap[Any](List("name", "age"))).collect()
    // Array(Map("name" -> "Justin", "age" -> 19))

## Programmatically Specifying the Schema

import org.apache.spark.sql.types._
val peopleRDD = spark.sparkContext.textFile("examples/src/main/resources/people.txt")
val schemaString = "name age"
val fields = schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, nullable = true))
val schema = StructType(fields)
val rowRDD = peopleRDD.map(_.split(",")).map(attributes => Row(attributes(0), attributes(1).trim))
val peopleDF = spark.createDataFrame(rowRDD, schema)
peopleDF.createOrReplaceTempView("people")
val results = spark.sql("SELECT name FROM people")
results.map(attributes => "Name: " + attributes(0)).show()

## Read/Write Different Files in Spark

Parquet:
    spark.read.parquet("/data/people.parquet")
    df.write.parquet("/output/people/parquet")
    peopleDF.select("name", "age").write.format("parquet").save("namesAndAges.parquet")
    
JSON: 
    val peopleDF = spark.read.format("json").load("examples/src/main/resources/people.json")
CSV :
    val peopleDFCsv = spark.read.format("csv").option("sep", ",").option("inferSchema", "true").option("header", "true").load("examples/src/main/resources/people.csv")

Direct Files:
    val sqlDF = spark.sql("SELECT * FROM parquet.`examples/src/main/resources/users.parquet`")

Save Modes:
    SaveMode.ErrorIfExists, SaveMode.Append,SaveMode.OverWrite,SaveMode.Ignore

Bucketing, Sorting and Partitioning:
    peopleDF.write.bucketBy(42, "name").sortBy("age").saveAsTable("people_bucketed")
    usersDF.write.partitionBy("favorite_color").format("parquet").save("namesPartByColor.parquet")
Hive:
    val spark = SparkSession.builder().appName("Spark Hive Example").config("spark.sql.warehouse.dir", warehouseLocation).enableHiveSupport().getOrCreate()
    import spark.implicits._
    import spark.sql    
    sql("CREATE TABLE IF NOT EXISTS src (key INT, value STRING) USING hive")
    sql("LOAD DATA LOCAL INPATH 'examples/src/main/resources/kv1.txt' INTO TABLE src")
    // Queries are expressed in HiveQL
    sql("SELECT * FROM src").show()

    spark.sqlContext.setConf("hive.exec.dynamic.partition", "true")
    spark.sqlContext.setConf("hive.exec.dynamic.partition.mode", "nonstrict")
    df.write.partitionBy("key").format("hive").saveAsTable("hive_part_tbl")
    
    sql("SELECT * FROM hive_part_tbl").show()

 JDBC:
     bin/spark-shell --driver-class-path postgresql-9.4.1207.jar --jars postgresql-9.4.1207.jar
     
     val jdbcDF = spark.read.format("jdbc")
        .option("url", "jdbc:postgresql:dbserver")
        .option("dbtable", "schema.tablename")
        .option("user", "username")
        .option("password", "password")
        .load()
        
     val connectionProperties = new Properties()
     connectionProperties.put("user", "username")
     connectionProperties.put("password", "password")
     
     val jdbcDF2 = spark.read.jdbc("jdbc:postgresql:dbserver", "schema.tablename", connectionProperties)
     
     // Specifying the custom data types of the read schema
        connectionProperties.put("customSchema", "id DECIMAL(38, 0), name STRING")
        val jdbcDF3 = spark.read.jdbc("jdbc:postgresql:dbserver", "schema.tablename", connectionProperties)
    
    // Saving data to a JDBC source
        jdbcDF.write.format("jdbc")
        .option("url", "jdbc:postgresql:dbserver")
        .option("dbtable", "schema.tablename")
        .option("user", "username")
        .option("password", "password")
        .save()
        jdbcDF2.write.jdbc("jdbc:postgresql:dbserver", "schema.tablename", connectionProperties)
    // Specifying create table column data types on write
        jdbcDF.write
            .option("createTableColumnTypes", "name CHAR(64), comments VARCHAR(1024)")
            .jdbc("jdbc:postgresql:dbserver", "schema.tablename", connectionProperties)
