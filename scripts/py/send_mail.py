import smtplib
import os
from os.path import basename
from email.mime.application import MIMEApplication
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.utils import formatdate

# please note that password for the email is not stored, please read KT document for credentials


def send_mail(send_from, send_to, subject, text, server, port, attachment):
    msg = MIMEMultipart()
    msg['From']     = send_from
    msg['To']       = send_to
    msg['Date']     = formatdate(localtime=True)
    msg['Subject']  = subject

    msg.attach(MIMEText(text))

    with open(attachment, "rb") as fil:
        part = MIMEApplication(
            fil.read(),
            Name=basename(attachment)
        )
        part['Content-Disposition'] = 'attachment; filename="%s"' % basename(attachment)
        msg.attach(part)

    smtp = smtplib.SMTP(server, port)
    smtp.starttls()
    smtp.login("capco.hk.it@gmail.com", "")  # Read the KT document for the password
    smtp.sendmail(send_from, send_to, msg.as_string())
    smtp.close()

if __name__ == "__main__":
    from_address = "capco.hk.IT@gmail.com"
    to_address = "capco.hk.it@capco.com"
    subject = "Question stats this week"
    mail_content = "Hi,\nHere is a list of all unanswered questions."
    question_stats_path = "botengine/questionStats.xlsx"
    send_mail(from_address, to_address, subject, mail_content, "smtp.gmail.com", 587, question_stats_path)
    os.remove(question_stats_path)
