#!/usr/bin/env bash

#This script is used to stop phonebot

ps aux  |  grep -i FaqBot | grep -i 54000 | awk '{print $2}' | xargs kill -9
echo "Stopped java Phonebot"

pkill -9 -f phonebot
echo "Stopped py Phonebot"