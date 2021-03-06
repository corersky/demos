#!/bin/bash


if [ -f ${DEMODIR}/scripts/env.sh ]
	then
	source ${DEMODIR}/scripts/env.sh
elif [ -f ./env.sh ]
	then
	source env.sh
else
	echo "env.sh not sourced, you need to chdir to /mapr/clustername/demos/spark-streaming-m7/scripts and run this from there."
	exit 1
fi



####check if our socket is open

if ! lsof -i:${PORT}
	then
	echo "port ${PORT} isn't listening..perhaps you need to start the start_listener.sh script"
	exit 1
fi

#check if fifo exists

if [ -p ${BASEDIR}/input_pipe ]
	then
	echo "Starting datastream, hit Crtl-C to stop"
	for line in `cat ${SOURCE_FILE}| sort -k2 -k3 -t $',' `
		do
		echo ${line} | tee -a ${BASEDIR}/input_pipe 
		sleep ${SLEEPSECS}
	done
else
	echo "input_pipe ${BASEDIR}/input_pipe doesn't exist..exiting, you may need to figure out what's going on with start_listener.sh"
	exit 1
fi
