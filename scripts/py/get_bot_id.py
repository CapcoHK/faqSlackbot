import os
from slackclient import SlackClient
from base64 import b64decode


BOT_NAME = 'phonebot'

//Use slack token, encode it to b64 and then pass on to slack client
slack_client = SlackClient(b64decode("eG94Yi0yMTQxNTk4NDMyODItR2czNnp2RnE5dWJvUUFVczhCN0k5U05L"))


if __name__ == "__main__":
    api_call = slack_client.api_call("users.list")
    if api_call.get('ok'):
        # retrieve all users so we can find our bot
        users = api_call.get('members')
        for user in users:
            if 'name' in user and user.get('name') == BOT_NAME:
                print("Bot ID for '" + user['name'] + "' is " + user.get('id'))
    else:
        print("could not find bot user with the name " + BOT_NAME)