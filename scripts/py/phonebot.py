import os
import time
import socket
from slackclient import SlackClient
from datetime import datetime
import logging
from base64 import b64decode

# starterbot's ID as an environment variable
BOT_ID = "U6A4PQT8A"

# constants
AT_BOT = "<@" + BOT_ID + ">"
EXAMPLE_COMMAND = "do"

# instantiate Slack & Twilio clients
slack_client = SlackClient(b64decode("eG94Yi0yMTQxNTk4NDMyODItR2czNnp2RnE5dWJvUUFVczhCN0k5U05L"))


def handle_command(command, channel, message):
    """
        Receives commands directed at the bot and determines if they
        are valid commands. If so, then acts on the commands. If not,
        returns back what it needs for clarification.
    """
    try:
        log (message)
        username = message['user']
        log ('User: ' + username + ', Message Channel ID: ' + message['channel']  + ': ' + command)

        s = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
        s.connect(("localhost", 54000))
        s.send(username + " phone " + command)
        buf = s.recv(2000)
        if len(buf) > 0:
            response = buf
    except IOError:
        response = "Unable to connect to backend server.Please contact Capco Admin"
    except Exception as e:
        logging.exception("Caught exception")
        response = "Looks like somethings is wrong with input. Can you please type in alpahabets only"

    slack_client.api_call("chat.postMessage", channel=channel,
                          text=response, as_user=True)





def parse_slack_output(slack_rtm_output):
    """
        The Slack Real Time Messaging API is an events firehose.
        this parsing function returns None unless a message is
        directed at the Bot, based on its ID.
    """
    if len(slack_rtm_output) > 0:
        log (slack_rtm_output)
    output_list = slack_rtm_output
    if output_list and len(output_list) > 0:
        for output in output_list:
            #AT_BOT in output['text']
            if output and 'user' in output and output['user'] == BOT_ID:
                continue
            if output and 'text' in output and ('channel' in output and output['channel'].startswith('D')):
                # return text after the @ mention, whitespace removed
                # .split(AT_BOT)[1].strip()
                return output['text'].lower(), \
                       output['channel'], output
            elif output and 'text' in output and (BOT_ID in output['text']):
                # return text after the @ mention, whitespace removed
                return output['text'].split(AT_BOT)[1].strip().lower(), \
                       output['channel'], output
    return None, None, None

def log(message):
    print str(datetime.today()) + "[INFO]  " + str(message)

if __name__ == "__main__":
    READ_WEBSOCKET_DELAY = 1 # 1 second delay between reading from firehose
    if slack_client.rtm_connect():
        log("StarterBot connected and running!")
        log (slack_client.api_call("im.list"))
        while True:
            command, channel, message = parse_slack_output(slack_client.rtm_read())
            if command and channel and message:
                handle_command(command, channel, message)
            time.sleep(READ_WEBSOCKET_DELAY)
    else:
        log("Connection failed. Invalid Slack token or bot ID?")