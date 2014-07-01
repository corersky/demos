#env.sh for sparkstreaming => m7 demo


#clustername below works for the sandbox, you may need to change if you are running on something else




export PORT=REPLACEPORT 
export USERNAME="REPLACEUSER"
export SHARK_HOST=REPLACESHARKHOST

export BATCHSECS=3 #length of spark streaming batches/DSTREAMs



export CLUSTER=`cat /opt/mapr/conf/mapr-clusters.conf |awk {'print $1'}`
#export NODELIST=`clush -a 'hostname -f' | awk {'print $2'}`

export MYHOST=`hostname -f`
export SPARK_URL=spark://REPLACEURL:7077


export LABDIR=/mapr/${CLUSTER}/user/${USERNAME}/spark

export TABLENAME="sensortable"
export TABLEPATH=${LABDIR}/tables/${TABLENAME}

export OUTFILE=${LABDIR}/output/output.csv
export D3_OUTPUT=${LABDIR}/output/d3.out.json

export BASEDIR=${LABDIR}/plumbing

export JARFILE=${LABDIR}/m7_streaming_import/target/scala-2.10/m7import_2.10-0.1-SNAPSHOT.jar

export SOURCE_FILE=${LABDIR}/data/SensorDataV5.csv

export JAVA_BIN=`which java`
export SLEEPSECS=.1 #sleep secs for data generator to pause between sending


#update path
export SHARK_BIN=/opt/mapr/shark/shark-0.9.0/bin/shark
#aliases
# alias shark-beeline='${SHARK_BIN} --service beeline -u jdbc:hive2://localhost:10000 -n mapr -p mapr -d org.apache.hive.jdbc.HiveDriver'

# alias step-1_start_listener='sh ${DEMODIR}/scripts/start_listener.sh'
# alias step-2_start_streaming='sh ${DEMODIR}/scripts/run_m7_streaming_import.sh'
# alias step-3_push_data='sh ${DEMODIR}/scripts/push_data.sh'
# alias step-4_table_scan='echo "scan '\''/tables/sensortable'\'', {LIMIT => 3}" | hbase shell'
# alias step-5_shark_beeline='${SHARK_BIN} --service beeline -u jdbc:hive2://localhost:10000 -n mapr -p mapr -d org.apache.hive.jdbc.HiveDriver'

# alias step-YY_stop_datastream='sh ${DEMODIR}/scripts/stop_datastream.sh'

# alias step-XX_restart_services='sh ${DEMODIR}/scripts/restart_services.sh'



#TODO: more aliases
# alias /opt/mapr/spark/spark-0.9.1/sbin/start-slaves.sh 
# alias /opt/mapr/shark/shark-0.9.0/bin/shark --service sharkserver2 & 

#TODO: get env.sh sourced in .bash_profile