import os
import time
import socket
from slackclient import SlackClient
from datetime import datetime
import logging
from base64 import b64decode

# starterbot's ID as an environment variable
FAQ_BOT_ID = "U6A8B2P8D"
#os.environ.get("FAQ_BOT_ID")

# constants
FAQ_AT_BOT = "<@" + FAQ_BOT_ID + ">"
EXAMPLE_COMMAND = "do"

# instantiate Slack & Twilio clients
slack_client = SlackClient(b64decode("eG94Yi0yMTQyODMwOTEyODUtNXNVUTd0YVJVSmpHZ3pOcHRjZkZhSFJm"))
USER_CACHE = {}

def get_username(message):
    user_id = message['user']
    cached_user_name = USER_CACHE.get(user_id)
    if cached_user_name:
        return cached_user_name
    user_info_dict = slack_client.api_call("users.info",user=user_id)
    log(user_info_dict)
    real_name = user_info_dict['user']['profile']['real_name']
    user_name = user_info_dict['user']['name']
    resolved_name = real_name if len(real_name)>0 else user_name
    USER_CACHE[user_id] = resolved_name
    return resolved_name


def handle_command(command, channel, message):
    """
        Receives commands directed at the bot and determines if they
        are valid commands. If so, then acts on the commands. If not,
        returns back what it needs for clarification.
    """
    response = "None"
    try:
        log (message)
        username = get_username(message)
        log ('User: ' + username + ', Message Channel ID: ' + message['channel']  + ': ' + command)
        request_dict = {}
        request_dict['username'] = username
        request_dict['botname'] = 'faq'
        request_dict['command'] = command

        s = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
        s.connect(("localhost", 54001))
        s.send(json.dumps(request_dict))
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
    output_list = slack_rtm_output
    if output_list and len(output_list) > 0:
        log (output_list)
        for output in output_list:
            #FAQ_AT_BOT in output['text']
            if output and 'user' in output and output['user'] == FAQ_BOT_ID:
                continue
            if output and 'text' in output and ('channel' in output and output['channel'].startswith('D')):
                # return text after the @ mention, whitespace removed
                # .split(FAQ_AT_BOT)[1].strip()
                return output['text'].lower(), \
                       output['channel'], output
            elif output and 'text' in output and (FAQ_BOT_ID in output['text']):
                # return text after the @ mention, whitespace removed
                return output['text'].split(FAQ_AT_BOT)[1].strip().lower(), \
                       output['channel'], output
    return None, None, None

def log(message):
    print str(datetime.today()) + "[INFO]  " + str(message)

if __name__ == "__main__":
    READ_WEBSOCKET_DELAY = 1 # 1 second delay between reading from firehose
    if slack_client.rtm_connect():
        log ("Starter FAQ Bot connected and running!")
        log (slack_client.api_call("im.list"))
        while True:
            command, channel, message = parse_slack_output(slack_client.rtm_read())
            if command and channel and message:
                handle_command(command, channel, message)
            time.sleep(READ_WEBSOCKET_DELAY)
    else:
        log("Connection failed. Invalid Slack token or bot ID?")