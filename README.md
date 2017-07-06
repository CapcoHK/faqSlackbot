# Introduction to the faqSlackbot wiki!
Slack is an opensource messaging software which supports creating automated bots which respond to user queries. 
This slack bot helps in 
* Finding phone numbers of users
* Searching for FAQs from a database which is prepared by organisation

# Developer notes
* This project supports continuous build using [CircleCI](https://circleci.com/gh/CapcoHK/faqSlackbot)
* Please ask owner of CapcoHK for relevant permissions to access the CircleCI page
* The build system automatically builds for any new branches created
* *master* branch should be used for releasing to Production. Every build emits a jar file as an artifact which can be deployed independently to production. For example - ![Sample](https://github.com/CapcoHK/faqSlackbot/blob/master/build_artifacts.png)
