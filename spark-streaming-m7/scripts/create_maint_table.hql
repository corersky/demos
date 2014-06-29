CREATE EXTERNAL TABLE USERNAME_maint_table

(resourceid STRING, eventDate STRING,
technician STRING, description STRING)

ROW FORMAT DELIMITED FIELDS TERMINATED BY ","


STORED AS TEXTFILE LOCATION "/mapr/CLUSTER/user/USERNAME/spark/data/SENSORMAINT.csv";
