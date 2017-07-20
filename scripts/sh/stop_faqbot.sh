#!/usr/bin/env bash

#This script is used to stop FAQ Bot

pkill -9 -f faqbot
ps aux  |  grep -i FaqBot | grep -i 54001 | awk '{print $2}' | xargs kill -9
