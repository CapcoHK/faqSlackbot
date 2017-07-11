#!/usr/bin/env bash

# This script is to start Phone bot engine

BOTSPACE=botengine

cd ~/Downloads/slack/workspace

echo "Starting Phone Bot"

python phonebot-ricyik.py &> ~/Downloads/slack/logs/phonebot.log &
echo "Staring Bot Engine"
java -classpath "" -jar $BOTSPACE/FaqBot.jar localhost 54000 $BOTSPACE/employee.xlsx null.log &> ~/Downloads/slack/logs/phonebotengine.log &

cd ~
