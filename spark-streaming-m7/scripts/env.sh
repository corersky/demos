#env.sh for sparkstreaming => m7 demo


#clustername below works for the sandbox, you may need to change if you are running on something else

export CLUSTER=`cat /opt/mapr/conf/mapr-clusters.conf |awk {'print $1'}`
export MYHOST=`hostname`
export SPARK_URL=spark://${MYHOST}:7077
export PORT=9999 

export BATCHSECS=3 #length of spark streaming batches/DSTREAMs
export TABLENAME=/tables/sensortable 

export OUTFILE=/mapr/${CLUSTER}/CSV/sensor.csv

export BASEDIR=/mapr/${CLUSTER}/ingest
export DEMODIR=/mapr/${CLUSTER}/demos/spark-streaming-m7
export JARFILE=${DEMODIR}/m7_streaming_import/target/scala-2.10/m7import_2.10-0.1-SNAPSHOT.jar

export SOURCE_FILE=${BASEDIR}/SensorDataV5.csv
export JAVA_BIN=`which java`
export SLEEPSECS=.25 #sleep secs for data generator to pause between sending

#determine spark version
export SPARK_REV=`ls /opt/mapr/spark`

#update path
export SHARK_BIN=/opt/mapr/shark/shark-0.9.0/bin/shark
#aliases
alias shark-beeline='${SHARK_BIN} --service beeline -u jdbc:hive2://localhost:10000 -n mapr -p mapr -d org.apache.hive.jdbc.HiveDriver'

alias step-1_start_listener='sh ${DEMODIR}/scripts/start_listener.sh'
alias step-2_start_streaming='sh ${DEMODIR}/scripts/run_m7_streaming_import.sh'
alias step-3_push_data='sh ${DEMODIR}/scripts/push_data.sh'
alias step-4_table_scan='echo "scan '\''/tables/sensortable'\'', {LIMIT => 3}" | hbase shell'
alias step-5_shark_beeline='${SHARK_BIN} --service beeline -u jdbc:hive2://localhost:10000 -n mapr -p mapr -d org.apache.hive.jdbc.HiveDriver'

alias step-YY_stop_datastream='sh ${DEMODIR}/scripts/stop_datastream.sh'

alias step-XX_restart_services='sh ${DEMODIR}/scripts/restart_services.sh'



#TODO: more aliases
# alias /opt/mapr/spark/spark-0.9.1/sbin/start-slaves.sh 
# alias /opt/mapr/shark/shark-0.9.0/bin/shark --service sharkserver2 & 

#TODO: get env.sh sourced in .bash_profile
