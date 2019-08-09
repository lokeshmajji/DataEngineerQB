# Oozie

1) Sample job.properties
            nameNode=hdfs://host:8020
            jobTracker=host:8050
            queueName=default
            examplesRoot=examples
            oozie.use.system.libpath=true
            oozie.wf.application.path=${nameNode}/user/${user.name}/${examplesRoot}/apps/pyspark
            master=yarn-cluster
            oozie.action.sharelib.for.spark=spark2

2) Sample worflow.xml
            <workflow-app xmlns=’uri:oozie:workflow:0.5' name=’SparkPythonPi’>
                <start to=’spark-node’ />
                <action name=’spark-node’>
                    <spark xmlns=”uri:oozie:spark-action:0.1">
                        <job-tracker>${jobTracker}</job-tracker>
                        <name-node>${nameNode}</name-node>
                        <master>${master}</master>
                        <name>Python-Spark-Pi</name>
                        <jar>pi.py</jar>
                    </spark>
                    <ok to=”end” />
                    <error to=”fail” />
                </action>
                <kill name=”fail”>
                        <message>Workflow failed, error message [${wf:errorMessage(wf:lastErrorNode())}]</message>
                </kill>
                <end name=’end’ />
            </workflow-app>