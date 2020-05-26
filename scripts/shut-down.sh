#!/usr/bin/env bash

ps -ef | grep -v "grep" | grep $USER | grep "elasticsearch" | awk -F' ' '/^.*$/{print $2}' | xargs -r -l kill -9
ps -ef | grep -v "grep" | grep $USER | grep "zookeeper" | awk -F' ' '/^.*$/{print $2}' | xargs -r -l kill -9
ps -ef | grep -v "grep" | grep $USER | grep "kafka" | awk -F' ' '/^.*$/{print $2}' | xargs -r -l kill -9
