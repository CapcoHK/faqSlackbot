#!/bin/sh

check_status()
{
    SERVICE=$1
    if ps ax | grep -v grep | grep $SERVICE > /dev/null
    then
        echo "[O] $SERVICE running"
    else
        echo "[X] $SERVICE is not running"
    fi
}

check_status phonebot
check_status javanio-all
check_status solr-qa
check_status faqbot
check_status SlackBotMain
