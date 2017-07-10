import os
import time
import socket
from slackclient import SlackClient


# starterbot's ID as an environment variable
BOT_ID = "U561PE27Q"

# constants
AT_BOT = "<@" + BOT_ID + ">"
EXAMPLE_COMMAND = "do"

# instantiate Slack & Twilio clients
#slack_client = SlackClient(os.environ.get('SLACK_BOT_TOKEN'))
slack_client = SlackClient("xoxb-176057478262-TkcO4eizHQrbAdvlNgB1HYwM")


def handle_command(command, channel, message):
    """
        Receives commands directed at the bot and determines if they
        are valid commands. If so, then acts on the commands. If not,
        returns back what it needs for clarification.
    """
    try:
        log (message)
        username = message['subtitle']
        log ('User: ' + message['user'] + ', Title: ' +  username + ', Message Channel ID: ' + message['channel']  + ': ' + command)

        s.connect(("localhost", 54000))
        s.send("phone " + command)
        buf = s.recv(2000)
        if len(buf) > 0:
            response = buf
    except IOError:
        response = "Unable to connect to backend server.Please contact Capco Admin"
    except :
        response = "Looks like somethings is wrong with input. Can you please type in alpahabets only"
    slack_client.api_call("chat.postMessage", channel=channel,
                          text=response, as_user=True)





def parse_slack_output(slack_rtm_output):
    """
        The Slack Real Time Messaging API is an events firehose.
        this parsing function returns None unless a message is
        directed at the Bot, based on its ID.
    """
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
    print datetime.today() + ":" + message

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