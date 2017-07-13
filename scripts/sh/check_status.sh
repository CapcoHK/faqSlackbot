#!/bin/sh

check_status()
{
    DESC=$1
    SERVICE=$2
    PORT_NO=$3
    if [ "$PORT_NO" = "" ]
    then
        if ps ax | grep -v grep | grep $SERVICE > /dev/null
        then
                echo "[O] $DESC running"
        else
                echo "[X] $DESC is not running"
        fi
    else
        if ps ax | grep -v grep | grep $SERVICE | grep $PORT_NO > /dev/null
        then
                echo "[O] $DESC running"
        else
                echo "[X] $DESC is not running"
        fi
    fi

}

check_status py-phonebot phonebot ""
check_status java-phonebot FaqBot.jar 54000
check_status solr-webservice solr-qa ""
check_status py-faqbot faqbot ""
check_status java-faqbot FaqBot.jar 54001
