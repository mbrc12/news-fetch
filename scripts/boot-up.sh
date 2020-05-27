#!/usr/bin/env bash

ES_PREFIX=$HOME/Software/ElasticSearch/elasticsearch-7.7.0/
KAFKA_PREFIX=$HOME/Software/Kafka/kafka_2.12-2.5.0/

echo "Starting ES ..."
$ES_PREFIX/bin/elasticsearch -d &
echo "Starting Zookeeper ..."
$KAFKA_PREFIX/bin/zookeeper-server-start.sh -daemon $KAFKA_PREFIX/config/zookeeper.properties &
sleep 10s
echo "Starting Kafka ..."
$KAFKA_PREFIX/bin/kafka-server-start.sh -daemon $KAFKA_PREFIX/config/server.properties &
sleep 10s
