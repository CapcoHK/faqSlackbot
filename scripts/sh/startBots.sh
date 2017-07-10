#!/usr/bin/env bash

# This script is to start Phone bot engine

BOTSPACE=botengine

cd ~/Downloads/slack/workspace

echo "Starting Phone Bot"

python phonebot-ricyik.py > phonebot.log &
echo "Staring Bot Engine"
java -classpath "" -jar $BOTSPACE/javanio-all-1.0-SNAPSHOT.jar localhost 54000 $BOTSPACE/employee.xlsx  > botengine.log &

cd ~
