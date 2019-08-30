Q1) Load XML Data in Hive Tables

    1)Download Com.ibm.spss.hive.serde2.xml.XmlSerDe jar file
       Hive> ADD JAR /home/lokesh/Downloads/hivesmlserde-1.0.0.0.jar
	2) Create table book_details (title string,author string,county string, company string,price float,year int)
	Row format serde 'com.ibm.spss.hive.serde2.xml.XmlSerDe' with serdeproperties (
	"column.xpath.title"="/book/title/text()",
	"column.xpath.author"="/book/author/text()",
	"column.xpath.country"="/book/country/text()",
	"column.xpath.company"="/book/company/text()",
	"column.xpath.price"="/book/price/text()",
	"column.xpath.year"="/book/year/text()"
	)
	Stored as  inputformat 'com.ibm.spss.hive.serde2.xml.XmlInputFormat'
	outputformat 'org.apache.hadoop.hive.ql.io.IgnoreTextOutputFormat'
	TBLPROPERTIES("xmlinput.start"="<BOOK>","xmlinput.end"="</BOOK>>");
	
	LOAD DATA LOCAL INPATH '/home/lokesh/files/books.xml' into table book_details;
	Select * from book_details;
	
Q2) Implement SCD1 in Hive 
	Updates the data, no history is maintained
	
	Select
	Case when cdc_code='update' then table3s
	When cdc_code='nochange' then table2s
	When cdc_code='new' then table3s
	When cdc_code='delete' then table2s
	End
	From (select case 
	when table2.col1=table3.col1 and table2.col2=table3.col2 then 'no change'
	When table2.col1=table3.col1 and table2.col2<>table3.col2 then 'update' 
	When table3.col1 is null then 'delete'
	When table2.col1 is null then 'new'
	End as cdc_code,
	Concat(table2.col1, ",",table2.col2) as table2s,
	Concat(table3.col1,",",table2.col2) as table3s
	From table2 full outer join table3 on table2.col1=table3.col1) as b1;

Q3) Wordcount in Hive
	Create table word_count (line string) stored as textfile;
	Load data local inpath '/home/lokesh/files/word_count' into table word_count;
	Select word, count(1) from (select explode(split(line,',') as word from word_count) w group by word;
	
Q4) Multiple tables on a single file

	Create table table16 (col1 int,col2 string,col3 string,col4 int) row format delimited fields terminated by ',' lines terminated by '\n' stored as textfile;
	Load data local inpath '/home/lokesh/files/dynamic' into table table16;
	Select * from table16;
	
	--Target table contains less columns
	Create table table17 (col1 int,col2 string,col3 string) row format delimited fields terminated by ',' lines terminated by '\n' stored as textfile;
	Load data local inpath '/home/lokesh/files/dynamic' into table table17;
	Select * from table17; // Extra data will be omitted
	
	--Target table contains more columns
	Create table table18 (col1 int,col2 string,col3 string,col4 int,col5 string,col6 int) row format delimited fields terminated by ',' lines terminated by '\n' stored as textfile;
	Load data local inpath '/home/lokesh/files/dynamic' into table table18;
	Select * from table18; //Extra columns will be NULL
	
Q5) Incremental updates to a table
	
	Ingest the data
	
		sqoop import 
		--connect jdbc:teradata://{host name}/Database=retail
		--connection-manager org.apache.sqoop.teradata.TeradataConnManager 
		--username dbc
		--password dbc 
		--table SOURCE_TBL 
		--target-dir /user/hive/base_table 
		-m 1
		
		Create external table if not exists base_tab_external (
			id string,
			Field1 string,
			Modified_date DATE)
			ROW FORMAT DELIMITED
			FIELDS TERMINATED BY ','
			STORED AS TEXTFILE
			LOCATION '/user/hive/base_table'
			
		CREATE TABLE base_table (
		        id STRING,
		        field1 STRING,
		        modified_date DATE)
		    ROW FORMAT DELIMITED
		    FIELDS TERMINATED BY ','
		    STORED AS ORC;
		
		INSERT OVERWRITE TABLE base_table SELECT * FROM base_table_external;
	
	
	sqoop import 
	--connect jdbc:teradata://{host name}/Database=retail
	--connection-manager org.apache.sqoop.teradata.TeradataConnManager
	--username dbc 
	--password dbc 
	--table SOURCE_TBL --target-dir /user/hive/incremental_table 
	-m 1
	--check-column modified_date 
	--incremental lastmodified 
	--last-value {last_import_date}
			
			OR
	
	sqoop import 
	--connect jdbc:teradata://{host name}/Database=retail
	--connection-manager org.apache.sqoop.teradata.TeradataConnManager 
	--username dbc
	--password dbc 
	--target-dir /user/hive/incremental_table 
	-m 1
	--query 'select * from SOURCE_TBL where modified_date > {last_import_date} AND $CONDITIONS’
	
	CREATE EXTERNAL TABLE incremental_table (
	        id STRING,
	        field1 STRING,
	        modified_date DATE)
	    ROW FORMAT DELIMITED
	    FIELDS TERMINATED BY ','
	    STORED AS TEXTFILE
	    location '/user/hive/incremental_table';

Reconcile or merge the data.
	
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

Compact the data

	DROP TABLE reporting_table;
	CREATE TABLE reporting_table AS
	SELECT * FROM reconcile_view;

Purge the data

	hadoop fs –rm –r /user/hive/incremental_table/*
	
	DROP TABLE base_table;
	CREATE TABLE base_table (
	        id STRING,
	        field1 STRING,
	        modified_date DATE)
	    ROW FORMAT DELIMITED
	    FIELDS TERMINATED BY ','
	    STORED AS ORC;
	
	INSERT OVERWRITE TABLE base_table SELECT * FROM reporting_table;
	
Q6)Order By Sort By Cluster By

	Order by uses a single reducer :
		Select * from table6 order by col2;
	Sort by uses multiple reducers - sorting is done within a reducers , doesn't guarantee the full sorting 
		Select * from table6 sort by col2;
	Distribute by : ensures specific values are distributed over reducers
		Select * from table6 distribute by col2;
		Select * from table6 distributed by col2 sort by col2;
	Cluster By = Distribute by Sort By

Q7) Explode and Lateral View
		Select author_name, lv_col_book_name from table2 lateral view explode(book_names) lv_book_names as lv_col_book_name
		
Q8) Rank(),Dense_Rank,Row_Number()
	
		Select col1,col2,rank() over(order by col2 desc),dense_rank() over(order by col2 desc),row_number() over (order by col2 desc)  from table1;
		
		Select col1,col2,rank() over(partition by col1 order by col2 desc),dense_rank() over(partition by col1 order by col2 desc),row_number() over (partition by col1 order by col2 desc)  from table1;
Q9) Partitioning , Static Partitioning, Dynamic Partitioning

	//Data is known, manual 
	Create table if not exists part_dept(deptno int, empname string, sal int) partition by (deptname string) row format delimited fields terminated by ',' lines terminated by '\n' stored as textfile;
	
	Insert into table part_dept partition(deptname='HR') select co1,col3,col4 from dept where col2='HR'
	
	Load data local inpath '/home/lokesh/files/acctdata' into table part_dept partition(deptname = 'ACT')
	
	
	//data is uknown, automatic, slow, unique values
	Hive> set hive.exec.dynamic.partition=true;
	Hive> set hive.exec.dynamic.partition.mode=nonstrict;
	
	Create table if not exists part_dept_dynamic(deptno int, empname string, sal int) partition by (deptname string) row format delimited fields terminated by ',' lines terminated by '\n' stored as textfile;
	Insert into table part_dept_dynamic partition(deptname) select co1,col3,col4,col2 from dept;
	
	
Q10)  MSCK Repair
	Show partitions part_dept1;
	Alter table part_dept1 drop partition(deptname='HR');
	Show partitions part_dept1;
	Alter table part_dept1 add partition(deptname='DEV');
	Load data local inpath '/home/lokesh/files/dev' into table part_dept partition(deptname = 'DEV')
	
	Hadoop fs -ls /user/hive/warehouse/part_dept1
	Hadoop fs -mkdir /user/hive/warehouse/part_dept1/deptname=Finance
	
	Alter table part_dept1 add partition(deptname='Finance')
	Show partitions part_dept1;
	
	Hadoop fs -mkdir /user/hive/warehouse/part_dept1/deptname=Production
	Msck repair table part_dept1;
	Show partitions part_dept1;
	
Q11)Bucketing
	All the same column values of a bucketed columns will go into a same file
	Hashing Algorithm
	
	Set hive.enforce.bucketing=true;
	Set hive.exec.dynamic.partition.mode=nonstrict;

	Create table if not exists dept_buck(deptno int,empname string,sal int,location string) partition by (deptname string) clustered by (location) into 4 buckets row format delimited fields terminated by ',' lines terminated by ',' stored as textfile;
	
	Insert into table dept_buck partition(deptname) select col1,col3,col4,col5,col2 from dept_with_Loc;
	
Q12) Map Side Join
	
	If the table are small enough join can be done at the map side 
	
	Select /*+ MAPJOIN */ emp.col1,emp.col2,dept.col1,dept.col2,dept.col3 from emp join dept on emp.col6 = dept.col1;
	
	Hive> SET hive.auto.convert.join=true;
	Hive> SET hive.mapjoin.smalltable.filesize=250000000;
	
	Full outer map join - will not work in mapside and will result in using the reducer
	Only left and right outer joins
	
Q13) UDF
	Import org.apache.hadoop.hive.ql.exec.UDF;
	Import org.apache.hadoop.io.Text;
	
	Public final class custom_udf extends UDF {
		Public Text evaluate(final Text s) {
			If ( s == null) {
				Return null;
			}
			Else
			{
				Return new Text(s.toString().toUpperCase());
			}
		}
	}
	
	Hive> ADD JAR /home/lokesh/custom_udf.jar;
	Hive> create temporary function f2 as 'com.hive1.custom_udf';
	Hive>select col1,f2(col2) from table20;
	
Q14) Skipping Headers and Footers
	TBLPROPERTIES("skip.header.line.count"="3");
	TBLPROPERTIES("skip.footer.line.count"="3");
	
Q15)Execute hive queries from Bash Shell
	Hive -e 'select * from emp;'
	Hive -e 'select * from emp;select * from emp where empid=6;'
	
	Script.hql
	Hive> Source /home/lokesh/script.hql
	Hive -f /home/lokesh/script.hql

Q16) HIVECONF and HIVEVAR
	--Hive var sets the value globally whereas hiveconf sets locally
	
	Set deptno=40;
	Set deptno;
	Set hiveconf:d1=20;
	Set d1;
	Select * from empt_tab where col6={hiveconf:deptno};
	Set hivevar:d2=40;
	Select * from emp_tab where col6={d2};
	Select * from emp_tab where col6={hivevar:d2};
	
Q17) Compression
	Hive> set mapred.compress.map.output;
	Hive> set mapred.map.output.compression.codec;
	Hive> set mapred.map.output.compression.codec=gzip;
	Hive> set mapred.map.output.compression.codec=snappy;
	Hive> set mapred.output.compress=true;
	Hive> set mapred.output.compression.codec=snappy;
	Hive> set mapred.output.compression.codec=lzo;
	
Q18) HIVERC File 
	//Executed while launching hive
	//Sets configuration for the hive session

	
	
	
	
	
	
	
