#!/usr/bin/env bash

# This script is to start cmd bot

BOTSPACE=botengine

cd ~/Downloads/slack/workspace

if ps ax | grep -v grep | grep "cmdbot.py" > /dev/null
then
        echo "Command bot already running..."
else
        echo "Starting Command Bot"
        python cmdbot.py &> ~/Downloads/slack/logs/cmdbot.log &
fi

cd ~
