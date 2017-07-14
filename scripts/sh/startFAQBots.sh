#!/usr/bin/env bash

# This script is to start FAQ bot engine

BOTSPACE=botengine

cd ~/Downloads/slack/workspace

echo "Starting FAQ Bot"

python faqbot.py &> ~/Downloads/slack/logs/faqbot.log &
echo "Staring FAQ Bot Engine"
java -cp $BOTSPACE/FaqBot.jar com.capco.SlackBotMain "localhost" "54001" "$BOTSPACE/employee.xlsx" "$BOTSPACE/missingQuestions.txt" "~/Downloads/slack/taming-text/book/apache-solr/solr-qa/conf/stopwords.txt" &> ~/Downloads/slack/logs/faqbotengine.log &

cd ~
