#!/usr/bin/python
import json
import pprint
import elasticsearch
import os
import time
from datetime import datetime, timedelta

path = "/mapr/REPLACE_HOST/user/mapr/elasticsearch/data/"

def submit_data(data, timestamp):
        #create json object based on string passed
        j = json.loads(data)
        print "*" * 20

        es = {}
        es["id"] = str(j["id"]).replace("'","").replace("\"", "")
        es["username"] = j["user"]["screen_name"]
        es["tweet"] = j["text"]
        es["timestamp"] = timestamp
        es_j = json.dumps(es)

        #submit to elasticsearch
        es_api = elasticsearch.Elasticsearch()
        es_api.index(
                index="user8_nfl",
                doc_type="tweets",
                id=es["id"],
                body=es
                )

        #print output to console
        pprint.pprint(es)
        print "*" * 20
        return True


ofolder = os.listdir(path)


for filename in ofolder:
    contents = open(path + filename, "r").read()
    timestamp = datetime.fromtimestamp(os.path.getmtime(path + filename)).isoformat()	
    submit_data(contents, timestamp)
