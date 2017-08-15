#!/bin/sh

check_status()
{
    SERVICE=$1
    PORT_NO=$2
    if [ "$PORT_NO" = "" ]
    then
        if ps ax | grep -v grep | grep $SERVICE > /dev/null
        then
                return 0
        else
                return 1
        fi
    else
        if ps ax | grep -v grep | grep $SERVICE | grep $PORT_NO > /dev/null
        then
                return 0
        else
                return 1
        fi
    fi

}

RUNNING=""
STOPPED=""

if check_status cmdbot ""; then RUNNING+='[O] py-cmdbot\n'; else STOPPED+='[X] py-cmdbot\n'; fi
if check_status phonebot ""; then RUNNING+='[O] py-phonebot\n'; else STOPPED+='[X] py-phonebot\n'; fi
if check_status FaqBot.jar 54000; then RUNNING+='[O] java-phonebot\n'; else STOPPED+='[X] java-phonebot\n'; fi
if check_status faqbot ""; then RUNNING+='[O] py-faqbot\n'; else STOPPED+='[X] py-faqbot\n'; fi
if check_status FaqBot.jar 54001; then RUNNING+='[O] java-faqbot\n'; else STOPPED+='[X] java-faqbot\n'; fi
if check_status start.jar ""; then RUNNING+='[O] solr-webservice\n'; else STOPPED+='[X] solr-webservice\n'; fi

if [ ! -z "$RUNNING" -a "$RUNNING" != " " ]; then
        echo -e 'RUNNING : \n'$RUNNING
fi

if [ ! -z "$STOPPED" -a "$STOPPED" != " " ]; then
        echo -e '\nSTOPPED : \n'$STOPPED
fi

