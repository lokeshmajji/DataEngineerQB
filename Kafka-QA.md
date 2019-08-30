
Why Kafka?

 Create tightly coupled and redundant code for passing the data among different applications.

Compare other Message Brokers

Kafka 	RabbitMQ
Dumb broker / Smart Consumer	Smart broker / Dumb consumer
Streaming support	Not on its own
Dependent on Zookeeper	Not dependent
Supports few languages	Support many languages
100,000 messages per second	20,000 messages per second
Perfect fit for Big data ecosystem


A topic can have multiple producers and multiple consumers

Producers, Consumers doesn't have worry about complexity as it is handled by Kafka

Message - K,V Pair
Zookeeper - Helps in coordination, electing leaders, store meta data , topic, replicas

Partitions : Queue or a log, 1 Partition cannot span across nodes
Topics can have multiple partitions on multiple nodes, Multiple partitions can reside on a single node
Consumer group : Partitions can be assigned to unique consumers in a consumer group, Parallelization
The data inside a topic in a partition is retained by default : 7 days






Messages are ordered in a partition, they will be appended to the end of the partitions. Offset keeps the track of the messages
Consumers responsibility to keep track of the offsets, they are maintained in internal topic in kafka

0.8.1 - Zookeeper recorded offsets
Later versions - kafka internal topic









																
																
																
																

Resiliency : 3 
Leader will get the message 
In Sync Replica
A message is available for a consumer to read from a partition only after the message is written or in other words committed to the in sync replicas
Replica.lag.time.max.ms = 10000 (10 Sec)

Stuck follower : Follower did not issue a fetch request to the leader in the past 10 seconds
Slow follower : Latest message in the follower replica is older than 10 seconds compared to the leader






Unclean.leader.election.enable=true
0.11.0 - Disabled by default and if it enabled, data loss is inevitable
Min.insync.replicas 
Alerts set for ISR List for all critical topics


## ZOOKEEPER SETUP ##
 
--Download Zookeeper - 3 Node Zoo keeper ensemble/cluster
wget http://apache.claz.org/zookeeper/stable/zookeeper-3.4.10.tar.gz

--untar
tar -xzf zookeeper-3.4.10.tar.gz

--Configure zoo.cfg - Contains data dir, server details, ports: 2181
vi /home/ubuntu/zookeeper-3.4.10/conf

--Create Zookeeper directory on all the nodes
sudo mkdir /var/zookeeper
sudo chown ubuntu:ubuntu /var/zookeeper

--Create myid  : add the number to the file , for example, 0,1,2 in the nodes respectively in the myid file
vi /var/zookeeper/myid

--Start Zookeeper : on all the nodes
bin/zkServer.sh start


## KAFKA SETUP ##

	--untar
	tar -xzf confluent-oss-3.3.0-2.11.tar.gz
	
	--Configuration
		//In server.properties
		broker.id should be set to unique values in the cluster
		Zookeeper.connect : list the zookeper server names
		vi /home/ubuntu/confluent-3.3.0/etc/kafka/server.properties
	
		// In zookeeper.properties
		dataDir=/var/ZooKeeper
		clientPort=2181
		vi /home/ubuntu/confluent-3.3.0/etc/kafka/zookeeper.properties
	
	--Start Kafka
	nohup bin/kafka-server-start etc/kafka/server.properties > /dev/null 2>&1 &
	
	--Stop Kafka
	bin/kafka-server-stop

## Create & Describe Topic ##

bin/kafka-topics --create --zookeeper localhost:2181 --partitions 1 --replication-factor 3 --topic hirw-console-topic

/bin/kafka-topics.sh --list --zookeeper localhost:2181

bin/kafka-topics --describe --zookeeper localhost:2181 --topic hirw-console-topic

## Start Producer & Consumer ##

bin/kafka-console-producer --broker-list ip-172-31-37-234.ec2.internal:9092,ip-172-31-42-160.ec2.internal:9092,ip-172-31-45-199.ec2.internal:9092 --topic hirw-console-topic

bin/kafka-console-consumer --bootstrap-server ip-172-31-37-234.ec2.internal:9092,ip-172-31-42-160.ec2.internal:9092,ip-172-31-45-199.ec2.internal:9092 --topic hirw-console-topic

bin/kafka-console-consumer --bootstrap-server ip-172-31-37-234.ec2.internal:9092,ip-172-31-42-160.ec2.internal:9092,ip-172-31-45-199.ec2.internal:9092 --topic hirw-console-topic --from-beginning

## Consumer Group ##

bin/kafka-console-consumer --bootstrap-server ip-172-31-37-234.ec2.internal:9092,ip-172-31-42-160.ec2.internal:9092,ip-172-31-45-199.ec2.internal:9092 --topic hirw-console-topic --from-beginning --consumer-property group.id=console-consumer-group

Kafka knows that a conumesr has consumed the messages in a topic , it won't let another consumer from a group to consume the same message


## Increase Partitions ##

bin/kafka-topics --alter --zookeeper localhost:2181 --topic hirw-console-topic --partitions 3

bin/kafka-topics --describe --zookeeper localhost:2181 --topic hirw-console-topic



## Check parition assignment & Offsets ##

bin/kafka-run-class kafka.admin.ConsumerGroupCommand --bootstrap-server ip-172-31-37-234.ec2.internal:9092,ip-172-31-42-160.ec2.internal:9092,ip-172-31-45-199.ec2.internal:9092 --describe --group console-consumer-group




Streaming Meetup with Kafka



--Create Topic

	bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 3 --partitions 3 --topic hirw-meetup-topic

--Start producer

	java -cp KafkaMeetup-1.0.jar:/home/ubuntu/confluent-3.3.0/share/java/kafka/*:/home/ubuntu/confluent-3.3.0/share/java/confluent-common/*:/home/ubuntu/confluent-3.3.0/share/java/schema-registry/*:/home/ubuntu/meetup_libs/* com.hirw.kafka.KafkaMeetupProducerAvro

--Start consumer

	java -cp KafkaMeetup-1.0.jar:/home/ubuntu/confluent-3.3.0/share/java/kafka/*:/home/ubuntu/confluent-3.3.0/share/java/confluent-common/*:/home/ubuntu/confluent-3.3.0/share/java/schema-registry/*:/home/ubuntu/meetup_libs/* com.hirw.kafka.KafkaMeetupConsoleConsumer

--Describe parition

	bin/kafka-topics --describe --zookeeper localhost:2181 --topic hirw-meetup-topic

--Start Kafka connect

	
	
	
	bin/connect-standalone etc/schema-registry/connect-avro-standalone.properties etc/kafka-connect-hdfs/meetup-hdfs.properties

--Hive against RSVP data
	
	SET hive.mapred.supports.subdirectories=TRUE;
	SET mapred.input.dir.recursive=TRUE;  --helps hive to process the files recursively in a folder

	CREATE EXTERNAL TABLE meetup_avro
	ROW FORMAT
	SERDE 'org.apache.hadoop.hive.serde2.avro.AvroSerDe'
	STORED AS
	INPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'
	OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'
	LOCATION '/user/ubuntu/topics/hirw-meetup-topic'
	TBLPROPERTIES ('avro.schema.url'='hdfs:///user/ubuntu/meetup.avsc');

	select count(1) from meetup_avro;
	
	select get_json_object(rsvp, '$.group.group_name') as group_name, count(1) from meetup_avro
	group by get_json_object(rsvp, '$.group.group_name');



