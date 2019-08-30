Traditional RDBMS: Disk Space wastage, not scalable, not flexible

Column oriented, distributed, de-centralized

The interface node delegates the operations to the other nodes
The interface node can be a SPOF, let the application can chose any node for performing the update/read operations. This architecture is call de-centralized

Hbase : It runs on hadoop and it has SPOF which is the name node
MongoDB: It is not a columnar database
Cassandra
	• Column oriented, 
	• Distributed
	• de-centralized
	• Tunable consistency 
	• High Availability

Cassandra support secondary indexes
Data is columnar : catalog data, notification

Limitations : Not ACID Complaint
	Cassandra provides tunable consistency at the expense of availability.

By Default, Cassandra returns success when it updates data on one node and other nodes will replicate the data in the backend. This makes the data is inconsistent when an application receives old data from an unsynced nodes.

Cassandra should not be used:
	-If a single system can be managed
	- Instant read and writes (ex. Bank accounts, Product orders)
	
If Hadoop is used across the ecosystem, Hbase can be used, Heave ETL batch jobs with lower latency requirements
Cassandra can be used if real time lookups are required

##Installation
	Mkdir data
	Mkdir cassandraLogs
	
	Logback.xml
	
	Mac: Nano ~/.bash_profile
	Export CASSANDRA_HOME=
	Export Path
	Source ~/.profile
	--start the cassandra server
	Cassandra -f 
	--cql
	
	Cassandra Cluster manager CCM
	Download ccm-master
	Cd ccm-master
	Cassandra -v
	Ccm create test -v 3.6
	
	--Install Maven
	Export path = $PATH:$HOME/apache-maven-3.3.9/bin


###Cassandra Data Model
Column - Key Column, Value Column
Column family - Collection of one ore more columns, Has a primary key to uniquely identify rows in it, 
Row Key + Column 

--Column Family Attributes
	Keys_Cached : Caches the keys in memory , limit is 200,000
	Rows_Cached: Holds the row data in the memory 
	PreLoad_Row_Cache: if pre-loading is required
--Super Column Family
	It is a collection of columns, group the columsn which are likely to be quired together under the same SCF
	Read operationos are optimized
	Cassandra doesn’t index the columns in SCF
	We cannot query individual columns in SCF
	Cannot be used any more, instead use a composite key if grouping is required

	
	
 --key Space
	Collecction of 1 or more column family

 --Bloom filter 
	Bloom filters can have false positives , falsoe positibe proablbility can be between 0 and 1
	Bloom_filter_fp_chance= 0.01

###Partition Keys - Distributing data across cluster nodes

	
		




			
 
 

		
	
	
	 
	
	
	
	
	
	
	
	
	
	
	
