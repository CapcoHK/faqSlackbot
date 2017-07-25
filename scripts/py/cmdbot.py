import os
from slackclient import SlackClient
from datetime import datetime
import logging
from base64 import b64decode
import subprocess
import requests
import os.path
import re
import time

current_milli_time = lambda: str(int(round(time.time() * 1000)))

# starterbot's ID as an environment variable
FAQ_BOT_ID = "U6B6MMJMS"

# constants
FAQ_AT_BOT = "<@%s>" % FAQ_BOT_ID
EXAMPLE_COMMAND = "do"

# instantiate Slack & Twilio clients
TOK = b64decode("eG94Yi0yMTUyMjU3MzI3NDAtMG96MDk3Skk5b2hsZFh0SXBIeGRPQWQ3")
slack_client = SlackClient(TOK)
ENABLED_COMMANDS = ['crontab', 'sh', 'cat', 'find', 'grep', 'ps', 'date', 'ls', 'cd', 'pwd', 'df', 'du']
USER_CACHE = {}
SUPPORTED_FILES = {
                    "Slack_FAQ.xlsx" : os.path.expanduser('~') + "/Downloads/slack/taming-text/faqsetup",
                    "employee.xlsx"  : os.path.expanduser('~') + "/Downloads/slack/workspace/botengine"
                  }

# the token T2R5R0DJT is the team-id and statys constant
FILE_DOWNLOAD_URL = "https://files.slack.com/files-pri/T2R5R0DJT-%s/download/%s"

def get_username(message):
    user_id = message['user']
    cached_user_name = USER_CACHE.get(user_id)
    if cached_user_name:
        return cached_user_name
    user_info_dict = slack_client.api_call("users.info", user=user_id)
    log(user_info_dict)
    real_name = user_info_dict['user']['profile']['real_name']
    user_name = user_info_dict['user']['name']
    resolved_name = real_name if len(real_name) > 0 else user_name
    USER_CACHE[user_id] = resolved_name
    return resolved_name


def preprocess(command):
    command = command.replace('~', os.path.expanduser('~'))
    if command.endswith('.sh') and not ' ' in command:
        command = "sh %s" % command
    return command


def get_file_info(command):
    """
    When user uploads a file, slack returns a display URL which doesn't point to the actual file, instead it has
    a java script so that the file can be displayed in a nice formatted manner.
    This method gets the display URL, extracts out the file_id from that and then concatenates it with the actual file
    url (see FILE_DOWNLOAD_URL)
    :param command: The whole message which was received by slack bot
    :return: dictionary containing two keys - file_name and file_url
    """
    retval = None
    match = re.search("(?P<url>https?://[^\s]+)", command)
    if match:
        matched_url = match.group("url")
        retval = {}
        file_id = matched_url.split("|")[0].split('/')[-2]
        retval['file_name'] = matched_url.split("|")[1][:-1]
        retval['file_url'] = FILE_DOWNLOAD_URL % (file_id, retval['file_name'].lower())
    return retval


def download_file(file_name, url):
    local_filename = current_milli_time() + "_" + file_name
    # NOTE the stream=True parameter
    r = requests.get(url, headers={'Authorization': 'Bearer %s' % TOK})
    with open(local_filename, 'wb') as f:
        for chunk in r.iter_content(chunk_size=1024):
            if chunk: # filter out keep-alive new chunks
                f.write(chunk)
    log("Downloaded file %s from slack" % local_filename)
    return os.path.join(os.getcwd(), local_filename)


def move_file(command):
    file_info = get_file_info(command)
    if not file_info:
        return "ERROR : Unable to parse the file url returned by cmdbot"

    file_name = file_info['file_name']
    file_url = file_info['file_url']

    if file_name not in SUPPORTED_FILES:
        return "The file you uploaded %s is not recognized by cmdbot, only these files are supported : %s" % (file_name, ", ".join(SUPPORTED_FILES.keys()))

    existing_file_renamed = False
    backup_file_path = None
    existing_file_path = os.path.join(SUPPORTED_FILES[file_name], file_name)
    try:
        downloaded_file = download_file(file_name, file_url)
        if os.path.isfile(existing_file_path):
            log("Found existing file %s"%(existing_file_path))
            backup_file_path = existing_file_path + "_" + current_milli_time()
            log("Backing up existing file as %s" % backup_file_path)
            os.rename(existing_file_path, backup_file_path)
            existing_file_renamed = True
        os.rename(downloaded_file, existing_file_path)
        log("Moving %s to %s"%(downloaded_file, existing_file_path))
        return "%s was successfully moved to %s" % (file_name, SUPPORTED_FILES[file_name])
    except Exception as e:
        logging.exception("Caught exception")
        if existing_file_renamed:
            log("Restoring backup file %s to %s" % (backup_file_path, existing_file_path))
            os.rename(backup_file_path, existing_file_path)
        return "Unable to process the uploaded file, will revert to previous version of the file"


def handle_command(command, channel, message):
    """
        Receives commands directed at the bot and determines if they
        are valid commands. If so, then acts on the commands. If not,
        returns back what it needs for clarification.
    """
    response = None
    try:
        log(message)
        username = get_username(message)
        log('User: %s, Message Channel ID: %s : %s' % (username, message['channel'], command))
        command = preprocess(command)
        log('Preprocessed command : %s' % command)
        split_cmd = command.split(' ')
        actual_cmd = split_cmd[0]
        if actual_cmd not in ENABLED_COMMANDS:
            if r"uploaded a file:" in command:
                response = move_file(command)
            else:
                response = "Cmdbot supports following commands (type \"command --help\" to learn more) \n%s" % (", ".join(ENABLED_COMMANDS))
        elif actual_cmd == 'cd':
            os.chdir(split_cmd[1])
            response = subprocess.check_output('pwd')
        else:
            response = subprocess.check_output(split_cmd, stderr=subprocess.STDOUT)
            if len(response) == 0:
                response = "Command executed successfully without result"
    except subprocess.CalledProcessError as e:
        response = e.output
    except Exception as e:
        logging.exception("Caught exception")
        response = "Looks like something is wrong with input. %s" % e

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
                return output['text'], \
                       output['channel'], output
            elif output and 'text' in output and (FAQ_BOT_ID in output['text']):
                # return text after the @ mention, whitespace removed
                return output['text'].split(FAQ_AT_BOT)[1].strip(), \
                       output['channel'], output
    return None, None, None


def log(message):
    print "%s [INFO]  %s" % (datetime.today(), message)

if __name__ == "__main__":
    os.chdir("..")  # one level up
    READ_WEBSOCKET_DELAY = 1  # 1 second delay between reading from firehose
    if slack_client.rtm_connect():
        log("Starter FAQ Bot connected and running!")
        log(slack_client.api_call("im.list"))
        while True:
            command, channel, message = parse_slack_output(slack_client.rtm_read())
            if command and channel and message:
                handle_command(command, channel, message)
            time.sleep(READ_WEBSOCKET_DELAY)
    else:
        log("Connection failed. Invalid Slack token or bot ID?")
