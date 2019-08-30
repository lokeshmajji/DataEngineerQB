Q)Give a general overview of Apache Spark. How is the framework structured? What are the main modules?



The cluster manager is not part of the Spark framework itself—even though Spark ships with its own, this one should not be used in production. Supported cluster managers are Mesos, Yarn, and Kybernetes.
The driver program is a Java, Scala, or Python application, which is executed on the Spark Master.
As part of the program, some Spark framework methods will be called, which themselves are executed on the worker nodes.
Each worker node might run multiple executors (as configured: normally one per available CPU core). Each of the executors will receive a task from the scheduler to be executed.
The modules of Apache Spark run directly on top of its core:


Apache Spark (Core)
Spark consists of a core framework that manages the internal representation of data, including:
	• serialization
	• memory allocation
	• caching
	• increasing resilience by storing intermediate snapshots on disk
	• automatic retries
	• data exchange (shuffling) between worker nodes
	• etc.
It also provides a bunch of methods to transform data (like map and reduce). All of these methods work on resilient distributed datasets (RDD).
Spark automatically recognizes dependencies between single steps and thereby knows which of them can be executed in parallel.
This is accomplished by building a directed acyclic graph (DAG), which also implies that transformations are not executed right away, but when action functions are called.
So basically, the methods can be divided into two types: RDD transformations and actions.

These are RDD transformations:
			§ map(func)
			§ flatMap()
			§ filter(func)
			§ mapPartitions(func)
			§ mapPartitionWithIndex()
			§ union(dataset)
			§ intersection(dataset)
			§ distinct()
			§ groupByKey()
			§ reduceByKey(func, [numTasks])
			§ sortByKey()
			§ join()
			§ coalesce()
And these are RDD actions:
			§ count()
			§ collect()
			§ take(n)
			§ top()
			§ countByValue()
			§ reduce()
			§ fold()
			§ aggregate()
			§ foreach()
	The DAG of a running job can be viewed in the Spark UI web interface. It also shows the pending jobs, the lists of tasks, and current resource usage and configuration.
	Most of the information can also be reviewed for finished (or failed) jobs if the history server is configured.
Spark SQL
	This is an abstraction of Spark’s core API. Whereas the core API works with RDD, and all transformations are defined by the developer explicitly, Spark SQL represents the RDD as so-called DataFrames. The DataFrame API is more like a DSL that looks like SQL.
	The developer can even more abstract the RDD by registering a DataFrame as a named in-memory table. This table can then be queried as one would query a table in a relational database using SQL.
Spark Streaming
	This can poll distributed logs like Apache Kafka or Amazon Kinesis (and some other messaging systems, like ActiveMQ) to process the messages in micro-batches. (Nearly) all functionality available for Spark batch jobs can also be applied on the RDD provided by Spark Streaming.
MLlib
	MLlib provides high-level algorithms that are commonly used in general data analysis (like clustering and regression) and in machine learning. It provides the functionality to define pipelines, train models and persist them, and read trained models to apply them to live data.
GraphX
	This lets you represent RDD as a graph (nodes are connected via edges) and perform some basic graph operations on it. Currently (only) three more advanced algorithms are provided: PageRank, ConnectedComponents, and TriangleCounting.


Q)You have a cluster of 10 nodes with 24 CPU cores available on each node.The following code works but might crash on large data sets, or at least will not leverage the full processing power of the cluster. Which is the problematic part and how would you adapt it?
					def calculate(sparkSession: SparkSession): Unit = {
					 val NumNodes = 10
					 val userActivityRdd: RDD[UserActivity] =
					   readUserActivityData(sparkSession)
					     .repartition(NumNodes)
					
					 val result = userActivityRdd
					   .map(e => (e.userId, 1L))
					   .reduceByKey(_ + _)
					
					 result
					   .take(1000)
					}

A)
	The repartition statement generates 10 partitions (no matter if it were more or less when they were loaded from wherever). These might become quite large on huge datasets and probably won’t fit into the allocated memory for one executor.
	Also, only one partition can be allocated per executor. This means, only 10 out of the 240 executors are used (10 nodes with 24 cores, each running one executor).
	If the number is chosen too high, the overhead of managing the partition by the scheduler adds up and decreases performance. In some cases, for very small partitions, it might even exceed the execution time itself.
	The recommended number of partitions is between two to three times the number of executors. In our case, 600 = 10 x 24 x 2.5 would be an appropriate number of partitions.
	
Q)The following code registers a user-defined function (UDF) and uses it in a query. (The general business logic is irrelevant to the question.) What’s problematic about the code such that it might tear down the whole cluster, and how can it be solved?
	(Hint: It has to do with the usage of the categoryNodesWithChildren Map variable.)
	
	def calculate(sparkSession: SparkSession): Unit = {
	 val UserIdColumnName = "userId"
	 val CategoryIdColumnName = "categoryId"
	 val NumActionsColumnName = "numActions"
	 val OtherCategoryIdColumnName = "otherCategoryId"
	 val OtherNumActionsColumnName = "otherNumActions"
	
	
	 val categoryNodesWithChildren: Map[Int, Set[Int]] =
	   Map(0 -> Set(1, 2, 3),
	     1 -> Set(4, 5),
	     2 -> Set(6, 7),
	     3 -> Set(8),
	     7 -> Set(9, 10)
	   )
	
	 sparkSession.udf.register("isChildOf", (nodeId: Int, parentNodeId: Int) =>  
	 nodeId != parentNodeId && categoryNodesWithChildren.getOrElse(nodeId, Set[Int]()).contains(parentNodeId))
	
	
	 val userCategoryActions = readUserCategoryActions(sparkSession)
	
	 val otherUserCategoryActions = userCategoryActions
	   .select(
	     col(UserIdColumnName),
	     col(CategoryIdColumnName).alias(OtherCategoryIdColumnName),
	     col(NumActionsColumnName).alias(OtherNumActionsColumnName)
	   )
	
	 val joinedUserActions = userCategoryActions
	   .join(otherUserCategoryActions, UserIdColumnName)
	   .where("!(isChildOf(categoryId,otherCategoryId) or isChildOf(otherCategoryId,categoryId))")
	   .groupBy(UserIdColumnName, CategoryIdColumnName, OtherCategoryIdColumnName)
	   .sum(OtherNumActionsColumnName)
	   .withColumnRenamed(s"sum($OtherNumActionsColumnName)", OtherNumActionsColumnName)
	
	 joinedUserActions.show()
	
	}
A)
		The line to register the UDF should be replaced with this code snippet:
		
		def calculate(sparkSession: SparkSession): Unit = {
		...
		
		
		 val categoryNodesWithChildrenBC = sparkSession.sparkContext.broadcast(categoryNodesWithChildren)
		
		 sparkSession.udf.register("isChildOf", (nodeId: Int, parentNodeId: Int) =>
		   nodeId != parentNodeId && categoryNodesWithChildrenBC.value.getOrElse(nodeId, Set[Int]()).contains(parentNodeId))
		
		...
		
		}
		The problem with the first approach is that it uses a variable from the driver application that is not available per se on the worker nodes. Spark will fetch the variable (meaning, the whole Map) from the master node each time the UDF is called. This can result in a very high load on the master and the whole cluster might become unresponsive. The adaptation of the code sends (broadcasts) a copy of the variable to each of the worker nodes where it is accessible as an org.apache.spark.broadcast object that holds the actual Map.


Q)Complete the missing SQL query to return the result as shown based on the example data:
	case class User(userId: Long, userName: String)
	case class UserActivity(userId: Long, activityTypeId: Int, timestampEpochMs: Long)
	val LoginActivityTypeId = 0
val LogoutActivityTypeId = 1
	private def readUserData(sparkSession: SparkSession): DataFrame = {
 sparkSession.createDataFrame(
   sparkSession.sparkContext.parallelize(
     Array(
       User(1, "Doe, John"),
       User(2, "Doe, Jane"),
       User(3, "X, Mr."))
   )
 )
}
	private def readUserActivityData(sparkSession: SparkSession): DataFrame = {
 sparkSession.createDataFrame(
   sparkSession.sparkContext.parallelize(
     Array(
       UserActivity(1, LoginActivityTypeId, 1514764800000L),
       UserActivity(2, LoginActivityTypeId, 1514808000000L),
       UserActivity(1, LogoutActivityTypeId, 1514829600000L),
       UserActivity(1, LoginActivityTypeId, 1514894400000L))
   )
 )
}
	def calculate(sparkSession: SparkSession): Unit = {
 val UserTableName = "user"
 val UserActivityTableName = "userActivity"
	val userDf: DataFrame = readUserData(sparkSession)
 val userActivityDf: DataFrame = readUserActivityData(sparkSession)
	userDf.createOrReplaceTempView(UserTableName)
 userActivityDf.createOrReplaceTempView(UserActivityTableName)
	val result = sparkSession
   .sql(s"SELECT ...")
	result.show()
}
	
A)
	The missing SQL should look something like this:
	 val result = sparkSession
   .sql(s"SELECT u.userName, MIN(ua.timestampEpochMs) AS firstLogin " +
     s"FROM $UserTableName u " +
     s"JOIN $UserActivityTableName ua ON u.userId=ua.userId " +
     s"WHERE ua.activityTypeId=$LoginActivityTypeId " +
     s"GROUP BY u.userName")
	(The $ notation is a Scala feature to replace expressions within a string and instead the table names might be there “hard-coded” as well. Note that you would never do this with user-supplied variables because it would open your code to injection vulnerabilities.)
	

Q)Please highlight which part of the following code will be executed on the master, and which will be run on each worker node.
	val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM")
	
	def getEventCountOnWeekdaysPerMonth(data: RDD[(LocalDateTime, Long)]): Array[(String, Long)] = {
	
	 val result = data
	   .filter(e => e._1.getDayOfWeek.getValue < DayOfWeek.SATURDAY.getValue)
	   .map(mapDateTime2Date)
	   .reduceByKey(_ + _)
	   .collect()
	
	 result
	   .map(e => (e._1.format(formatter), e._2))
	}
	
	private def mapDateTime2Date(v: (LocalDateTime, Long)): (LocalDate, Long) = {
	 (v._1.toLocalDate.withDayOfMonth(1), v._2)
	}
	
	A) The call of this function is performed by the driver application. The assignment to the result value is the definition of the DAG, including its execution, triggered by the collect() call. All parts of this (including the logic of the function mapDateTime2Date) are executed on the worker nodes.
	
	The resulting value that is stored in result is an array that is collected on the master, so the map performed on this value is run on the master.
	
	
Q) Describe the following code and what the output will be.
	case class User(userId: Long, userName: String)
	
	case class UserActivity(userId: Long, activityTypeId: Int, timestampEpochSec: Long)
	
	val LoginActivityTypeId = 0
	val LogoutActivityTypeId = 1
	
	private def readUserData(sparkSession: SparkSession): RDD[User] = {
	 sparkSession.sparkContext.parallelize(
	   Array(
	     User(1, "Doe, John"),
	     User(2, "Doe, Jane"),
	     User(3, "X, Mr."))
	 )
	}
	
	private def readUserActivityData(sparkSession: SparkSession): RDD[UserActivity] = {
	 sparkSession.sparkContext.parallelize(
	   Array(
	     UserActivity(1, LoginActivityTypeId, 1514764800L),
	     UserActivity(2, LoginActivityTypeId, 1514808000L),
	     UserActivity(1, LogoutActivityTypeId, 1514829600L),
	     UserActivity(1, LoginActivityTypeId, 1514894400L))
	 )
	}
	
	def calculate(sparkSession: SparkSession): Unit = {
	 val userRdd: RDD[(Long, User)] =
	   readUserData(sparkSession).map(e => (e.userId, e))
	 val userActivityRdd: RDD[(Long, UserActivity)] =
	   readUserActivityData(sparkSession).map(e => (e.userId, e))
	
	 val result = userRdd
	   .leftOuterJoin(userActivityRdd)
	   .filter(e => e._2._2.isDefined && e._2._2.get.activityTypeId == LoginActivityTypeId)
	   .map(e => (e._2._1.userName, e._2._2.get.timestampEpochSec))
	   .reduceByKey((a, b) => if (a < b) a else b)
	
	 result
	   .foreach(e => println(s"${e._1}: ${e._2}"))
	
	}

A)
	The main method, calculate, reads two sets of data. (In the example they are provided from a constant inline data structure that is converted into a distributed dataset using parallelize.) The map applied to each of them transforms them into tuples, each consisting of a userId and the object itself. The userId is used to join the two datasets.
	
	The joined dataset is filtered by all users with all their login activities. It is then transformed into a tuple consisting of userName and the event timestamp.
	
	This is finally reduced to only the first login entry per user and written to the console.
	
	The result will be:
	
	Doe, John: 1514764800
	Doe, Jane: 1514808000

Q)The following code provides two prepared dataframes with the following structure:

DF1: userId, userName
DF2: userId, pageId, timestamp, eventType
Add the code to join the two dataframes and count the number of events per userName. It should output in the format userName; totalEventCount and only for users that have events.

def calculate(sparkSession: SparkSession): Unit = {
 val UserIdColName = "userId"
 val UserNameColName = "userName"
 val CountColName = "totalEventCount"

 val userRdd: DataFrame = readUserData(sparkSession)
 val userActivityRdd: DataFrame = readUserActivityData(sparkSession)


 val result = userRdd
   .repartition(col(UserIdColName))
   // ???????????????
   .select(col(UserNameColName))
   // ???????????????

 result.show()
}

A)
def calculate(sparkSession: SparkSession): Unit = {
 val UserIdColName = "userId"
 val UserNameColName = "userName"
 val CountColName = "totalEventCount"

 val userRdd: DataFrame = readUserData(sparkSession)
 val userActivityRdd: DataFrame = readUserActivityData(sparkSession)


 val result = userRdd
   .repartition(col(UserIdColName))
   .join(userActivityRdd, UserIdColName)
   .select(col(UserNameColName))
   .groupBy(UserNameColName)
   .count()
   .withColumnRenamed("count", CountColName)

 result.show()
}
The join expression can take different kinds of parameters. The following alternatives produce the same result:

.join(userActivityRdd,UserIdColName, "inner")
.join(userActivityRdd, Seq(UserIdColName))
.join(userActivityRdd, Seq(UserIdColName), "inner")
Passing “left” as the last parameter would return a wrong result (users without events will be included, showing an event count of 1).

Passing “right” as the last parameter would return the correct result but would be semantically misleading.

   .groupBy(UserNameColName)
This is required. Without it, the total number of rows would be counted and result would be a Long instead of a DataFrame (so the code wouldn’t even compile, since the show() method does not exist for Long.)

   .count()
This is the actual aggregation that adds a new column (count) to the DataFrame.

   .withColumnRenamed("count", CountColName)
This renames the count column to totalEventCount, as requested in the question.

Q)What are the elements the GraphX library works with, and how are they created from an RDD? Complete the following code to calculate the page ranks.
		def calculate(sparkSession: SparkSession): Unit = {
		
		 val pageRdd: RDD[(???, Page)] =
		   readPageData(sparkSession)
		     .map(e => (e.pageId, e))
		     .cache()
		 val pageReferenceRdd: RDD[???[PageReference]] = readPageReferenceData(sparkSession)
		
		 val graph = Graph(pageRdd, pageReferenceRdd)
		 val PageRankTolerance = 0.005
		 val ranks = graph.???
		
		 ranks.take(1000)
		   .foreach(println)
		}

A)
	def calculate(sparkSession: SparkSession): Unit = {
	
	 val pageRdd: RDD[(VertexId, Page)] =
	   readPageData(sparkSession)
	     .map(e => (e.pageId, e))
	     .cache()
	 val pageReferenceRdd: RDD[Edge[PageReference]] = readPageReferenceData(sparkSession)
	
	 val graph = Graph(pageRdd, pageReferenceRdd)
	 val PageRankTollerance = 0.005
	 val ranks = graph.pageRank(PageRankTollerance).vertices
	
	 ranks.take(1000)
	   .foreach(println)
	}
	A graph consists of Vertex objects and Edge objects that are passed to the Graph object as RDDs of type RDD[VertexId, VT] and RDD[Edge[ET]] (where VT and ET are any user-defined types that should be associated with a given Vertex or Edge). The constructor of the Edge type is Edge[ET](srcId: VertexId, dstId: VertexId, attr: ET). The type VertexId is basically an alias for Long.

Q)Compare Spark Streaming to Kafka Streams and Flink. Highlight the differences and advantages of each technology, and for which use cases each of the stream processing frameworks works best.

The following table gives an overview of some characteristics of each of the frameworks. There is not always a clear answer of when to use which framework. Especially since it’s often in unimportant details where they differ.
But it’s important to understand:
	• That Kafka Streams is just a library (no additional infrastructure component, but it has the responsibility to deploy and scale the streaming application).
	• That Flink is currently the most superior/feature-rich framework when it comes to low-latency stream processing (which is important when streams are used as the core communication between services in real-time).
	• That Spark’s main benefit is the whole existing eco-system including the MLlib/GraphX abstractions and that parts of the code can be reused for both batch- and stream-processing functionality.
		Flink	Kafka Streams	Spark Streaming
	Deployment	A framework that also takes care of deployment in a cluster	A library that can be included in any Java program. Deployment depends how the Java application is deployed.	A framework that also takes care of deployment in a cluster
	Life cycle	Stream processing logic is run as a job in the Flink cluster	Stream processing logic is run as part of a "standard" Java application	Stream processing logic is run as a job in the Spark cluster
	Responsibility	Dedicated infrastructure team	Application developer	Dedicated infrastructure team
	Coordination	Flink master (JobManager), part of the streaming program	Leverages the Kafka cluster for coordination, load balancing, and fault-tolerance	Spark Master
	Source of continuous data	Kafka, File Systems, other message queues	Kafka only	Common streaming platforms like Kafka, Flume, Kinesis, etc.
	Sink for results	Any storage where an implementation using the Flink Sink API is available	Kafka or any other storage where a Kafka Sink is implemented using the Kafka Connect API	File and Kafka as a predefined sink, any other destination using the forEach-sink (manual implementation)
	Bounded and unbounded data streams	Unbounded and bounded	Unbounded	Unbounded (bounded using Spark Batch jobs)
	Semantical guarantees	Exactly once for internal Flink state; end-to-end exactly once with selected sources and sinks (e.g., Kafka to Flink to HDFS); at least once when Kafka is used as a sink	Exactly once, end-to-end with Kafka	Exactly once
	Stream processing approach	Single record	Single record	Micro-batches
	State management	Key-value storage, transparent to the developer	No, must be implemented manually	No, stateless by nature
	Feature set	Rich feature set, including event time (opposed to processing time), sliding windows, and watermarks	Simple features set to aggregate in tumbling windows	Wide feature set but lacking some of the more advanced features that Flink offers
	Low latency	Yes	Yes	No
	Example of when to choose as stream processor	Setup of a new event-driven architecture that needs advanced stream-processing features and has low-latency requirements	JVM application that should consume an existing Kafka event stream	Add stream processing when Spark is already used for batch processing and low latency is not mandatory
	

Q). How Array and List can be differentiated in Scala?
	The Array is a mutable data structure that is sequential in nature while Lists are immutable data structures that are recursive in nature.
	Size of array is predefined while lists change its size based on operational requirements. In other words, Lists are variable in size while the array is fixed size data structure.

