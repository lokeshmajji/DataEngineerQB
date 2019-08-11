# DataEngineerQB

* Hive Questions
* Spark Questions

## Scrum
![image](https://user-images.githubusercontent.com/26711011/62640573-7a57e100-b907-11e9-8add-0638830b665f.png)

## Project : Real estate management


This project has been aimed at effecitve utilization of space in one of the major office of Takeda. The space utilization team wanted to build meaningful insights with the data available on different sources. Based on the employees, space managent team wanted to save some space. There are 4 major sources IWMS,Reservation System,Badge Swipe, PeopleSoft, the data is ingested from these sources using Sqoop and scrapping data from web urls using python scripts. RAW Tables are created using the, The data then be transformed and stored in data lakes (snappy compressed) and using spark-scala processing the data as per business need and the aggregated data is made available for visualization in Tableau.

* I was Involved in data mapping, file format conversions, file compressions
* Performing incremental loads on tables to gather incremental data
* writing spark-scala code to join different sources , write aggregate logic
* Using Hive/Spark SQL write queries:
    * Overall Average Space Utilization
    * Pattern of Attendance Trend over Time
    * Space Utilization based on Department / Band on a Particular Week/Month
    * How business units utilizing the space
    * Utilization by Space Type/Count
    * Meeting room booking vs attendance
* Store the data in different formats requested
* Make the data available for visualization in Tableau
* This as one of the source for decision making 
* Later on this project has been applied to different locations and handled by a different team

## Project
Real World Evidence promises to quantiy improvments to health outocmes and treatments, but this data must be available at scale. High storage and processing costs, challenges with merging stucutre and usntructed data and an over-reliance on infomratics resources  for analysis has slowed the evaloultion of RWS. With Hadoop, RWE groups are combining key data sources, including claims, presricptions, electronic media  records, HIE and social media. With Big data anlyastic in the pharmacetureal industry, analysst are unlocking real insighen and delivering advacnes insighst via cost -efficetive and failiar tools.




## Learninjg
https://www.pluralsight.com/courses/hive-complex-analytical-queries
