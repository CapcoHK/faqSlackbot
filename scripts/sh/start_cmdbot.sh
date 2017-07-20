#!/usr/bin/env bash

# This script is to start cmd bot

BOTSPACE=botengine

cd ~/Downloads/slack/workspace

echo "Starting Command Bot"

python cmdbot.py &> ~/Downloads/slack/logs/cmdbot.log &
cd ~
