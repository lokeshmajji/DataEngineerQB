# Hive Questions

## What is partitioning
        Partitioning – Apache Hive organizes tables into partitions for grouping same type of data together based on a column or partition key. 
        Each table in the hive can have one or more partition keys to identify a particular partition.
            Using partition we can make it faster to do queries on slices of the data.

        CREATE TABLE table_name (column1 data_type, column2 data_type) PARTITIONED BY (partition1 data_type, partition2 data_type,….);
        
        create table p_patient1(patient_id int, patient_name string, gender string, total_amount int) partitioned by ( drug string);
        insert overwrite table p_patient1 partition(drug='metacin') select patient_id, patient_name, gender, total_amount from patient where drug='metacin';

        SET hive.exec.dynamic.partition=true;
        SET hive.exec.dynamic.partition.mode=nonstrict;
        
        create table dynamic_partition_patient (patient_id int,patient_name string, gender string, total_amount int) partitioned by (drug string);
        insert into table dynamic_partition_patient PARTITION(drug) select * from patient1;

## What is Bucketing
        In Hive Tables or partition are subdivided into buckets based on the hash function of a column in the table to give extra structure to the data that may be used for more efficient queries.

        CREATE TABLE table_name PARTITIONED BY (partition1 data_type, partition2 data_type,….) CLUSTERED BY (column_name1, column_name2, …) SORTED BY (column_name [ASC|DESC], …)] INTO num_buckets BUCKETS;

        create table patient(patient_id int, patient_name string, drug string, gender string, total_amount int) row format delimited fields terminated by ',' stored as textfile;
        load data local inpath '/home/geouser/Documents/patient' into table patient;
        
        set hive.enforce.bucketing =true;
        create table bucket_patient(patient_id int, patient_name string, drug string,gender string, total_amount int) clustered by (drug) into 4 buckets;
        insert overwrite table bucket_patient select * from patient;
        select * from bucket_patient TABLESAMPLE(BUCKET 1 OUT OF 4 ON drug);
        select * from bucket_patient TABLESAMPLE(10 percent);


## What is the difference between external table and managed table?
In case of managed table, If one drops a managed table, the metadata information along with the table data is deleted from the Hive warehouse directory.
On the contrary, in case of an external table, Hive just deletes the metadata information regarding the table and leaves the table data present in HDFS untouched. 

## How will you consume this CSV file into the Hive warehouse using built SerDe?
        id first_name last_name email gender ip_address
        1 Hugh Jackman hughjackman@cam.ac.uk Male 136.90.241.52
        2 David Lawrence dlawrence1@gmail.com Male 101.177.15.130        
        
        CREATE EXTERNAL TABLE sample(id int, first_name string,last_name string, email string,gender string, ip_address string) 
            ROW FORMAT SERDE ‘org.apache.hadoop.hive.serde2.OpenCSVSerde’ 
            STORED AS TEXTFILE LOCATION ‘/temp’;

        SELECT first_name FROM sample WHERE gender = ‘male’;

##  Suppose, I have a lot of small CSV files present in /input directory in HDFS and I want to create a single Hive table corresponding to these files. The data in these files are in the format: {id, name, e-mail, country}. 
        Now, as we know, Hadoop performance degrades when we use lots of small files.So, how will you solve this problem where we want to create a single Hive table for lots of small files without degrading the performance of the system?

        CREATE TABLE temp_table (id INT, name STRING, e-mail STRING, country STRING) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE;
        LOAD DATA INPATH ‘/input’ INTO TABLE temp_table;
        
        CREATE TABLE sample_seqfile (id INT, name STRING, e-mail STRING, country STRING) ROW FORMAT DELIMITED STORED AS SEQUENCEFILE;
        INSERT OVERWRITE TABLE sample SELECT * FROM temp_table;


## Transfer data to HDFS
        hdfs dfs -put /home/cloudera/Desktop/Loki/employees/employees.json /user/cloudera/employees/
        hdfs dfs -put /home/cloudera/Desktop/Loki/employees/salaries.json /user/cloudera/employees/
        
        val sal = spark.read.json("/user/cloudera/employees/salaries.json")
        val sal = sqlContext.read.json("/user/cloudera/employees/salaries.json")
        val rdd = sal.map(x => x(0) + "," + x(1) + "," + x(2) + "," + x(3))
        rdd.saveAsTextFile("/user/cloudera/out/emp/sal_text")

## Create Partitioned Table on Salary Range
        create external table emp_sal(emp_no bigint,from_date string,salary bigint,to_date string) row format delimited fields terminated by ',' lines terminated by '\n' stored as textfile LOCATION '/user/cloudera/out/emp/sal';

        select * from emp_sal limit 10;

        create table emp_part(emp_no bigint,from_date string,salary bigint,to_date string) partitioned by (salaryrange string) row format delimited fields terminated by ',' lines terminated by '\n' stored as textfile;

        set hive.exec.dynamic.partition=true;
        set hive.exec.dynamic.partition.mode=nonstrict;

        insert into table emp_part partition(salaryrange) select emp_no,from_date,salary,to_date,case
        when salary between 10000 and 25000 then '10000-25000'
        when salary between 25001 and 50000 then  '25001-50000'
        when salary between 50001 and 75000 then  '50001-75000'
        when salary between 75001 and 100000 then '75001-100000'
        else '1000000+' 
        end as salaryrange from emp_sal;

        select * from emp_part limit 10;



## Write Hive Query Output to Directory
                INSERT OVERWRITE LOCAL DIRECTORY '/home/hadoop/yourTableDir'
                ROW FORMAT DELIMITED
                FILEDS TERMINATED BY '\t'
                STORED AS TEXTFILE
                SELECT * FROM employees where salary > 50000

                INSERT OVERWRITE DIRECTORY '/home/hadoop/yourTableDir'
                ROW FORMAT DELIMITED
                FILEDS TERMINATED BY '\t'
                STORED AS TEXTFILE
                SELECT * FROM employees where salary > 50000


## Incremental Load in Hive
                CREATE VIEW reconcile_view AS
                SELECT t1.* FROM
                (SELECT * FROM base_table
                        UNION ALL
                SELECT * from incremental_table) t1
                JOIN
                (SELECT id, max(modified_date) max_modified FROM
                        (SELECT * FROM base_table
                                UNION ALL
                        SELECT * from incremental_table)
                GROUP BY id) t2
                ON t1.id = t2.id AND t1.modified_date = t2.max_modified;

## Hive Tuning Techniques
* Execution Engine : Use Tez instead of MR
* Usage of suitable file format : ORC or Parquet
* By partitioning : Dividing data into folder
* Use of bucketing : Dividing data into manageable parts
* Use of vectorization : Vectorized query execution improves performance of operations like scans, aggregations, filters and joins, by performing them in batches of 1024 rows at once instead of single row each time.

        set hive.vectorized.execution = true;
        set hive.vectorized.execution.enabled = true;

* Cost based optimization : Hive, CBO, performs further optimizations based on query cost, resulting in potentially different decisions: how to order joins, which type of join to perform, degree of parallelism and others.
        set hive.cbo.enable=true;
        set hive.compute.query.using.stats=true;
        set hive.stats.fetch.column.stats=true;
        set hive.stats.fetch.partition.stats=true;
        
        Then, prepare the data for CBO by running Hive’s “analyze” command to collect various statistics on the tables for which we want to use CBO.
        ANALYZE TABLE test1 COMPUTE STATISTICS;

* Use of indexing :
        The major advantage of using indexing is; whenever we perform a query on a table that has an index, there is no need for the query to scan all the rows in the table. Further, it checks the index first and then goes to the particular column and performs the operation.
* execute.parallerl = true