#!/usr/bin/env bash

cd ~/Downloads/slack/workspace
python send_mail.py &> ~/Downloads/slack/logs/send_mail.log
