#!/usr/bin/env bash

# This script is to start Phone bot engine

BOTSPACE=botengine

cd ~/Downloads/slack/workspace

echo "Starting FAQ Bot"

python faqbot.py > faqbot.log &
echo "Staring FAQ Bot Engine"
java -cp $BOTSPACE/FaqBot.jar com.capco.SlackBotMain "localhost" "54001" "$BOTSPACE/employee.xlsx" "$BOTSPACE/missingQuestions.txt" > faqbotengine.log &

cd ~
