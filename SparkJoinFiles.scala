import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._

object SparkJoinFiles extends App {
  System.setProperty("hadoop.home.dir","C:\\Users\\LokeshMajji\\Desktop\\Loki\\Downloads\\Hadoop_WinUtils")
  val spark = new SparkSession.Builder().master("local").appName("Spark Emp").getOrCreate()
  import spark.implicits._

  spark.sparkContext.setLogLevel("ERROR")
  val empdf = spark.read.format("csv")
    .option("inferSchema","true")
    .option("header","true")
    .option("sep",",")
    .load("C:\\Users\\LokeshMajji\\Desktop\\Loki\\Spark2\\employee.txt")

  val companydf = spark.read.format("csv")
    .option("inferSchema","true")
    .option("header","true")
    .option("sep",",")
    .load("C:\\Users\\LokeshMajji\\Desktop\\Loki\\Spark2\\company.txt")

  println("employee data")
  empdf.show()
  println("company data")
  companydf.show()

  //Using Spark SQL
  empdf.createOrReplaceTempView("employee")
  companydf.createOrReplaceTempView("company")
  val resultsql = spark.sql("select c.companyname,count(e.empid) as emp_count from employee e join company c on e.companyid=c.companyid where e.state='Minnesota' and e.salary > 50000 group by c.companyname")
  println("Show Results using Spark SQL")
  resultsql.show()

  //using Spark DF
  val joineddf = empdf.join(companydf,empdf.col("companyid") === companydf.col("companyid"), "inner")
  val resultdf = joineddf.filter("state='Minnesota' and salary > 50000").groupBy("companyname").agg(count("empid"))
  println("Show Results using Spark DF")
  resultdf.show()

  //Save as Paruet
  //resultsql.write.format("parquet").mode(SaveMode.Append).save("C:\\Users\\LokeshMajji\\Desktop\\Loki\\Spark2\\out\\parquet")
  //resultsql.write.format("csv").mode(SaveMode.Append).save("C:\\Users\\LokeshMajji\\Desktop\\Loki\\Spark2\\out\\csv")
  resultsql.write.format("csv").option("sep","\t").mode(SaveMode.Overwrite).save("C:\\Users\\LokeshMajji\\Desktop\\Loki\\Spark2\\out\\tsv")

}
