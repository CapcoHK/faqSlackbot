#!/usr/bin/env bash

#This script is used to stop FAQ Bot

ps aux  |  grep -i FaqBot | grep -i 54001 | awk '{print $2}' | xargs kill -9
echo "Stopped java Faqbot"

pkill -9 -f faqbot
echo "Stopped java Faqbot"