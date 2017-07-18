import smtplib
from os.path import basename
from email.mime.application import MIMEApplication
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.utils import formatdate

EMAIL = "capco.hk.it@capco.com"
PWD = ""

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
    smtp.login(EMAIL, PWD)
    smtp.sendmail(send_from, send_to, msg.as_string())
    smtp.close()

if __name__ == "__main__":
    send_mail("capco.hk.it@capco.com", "FAQ_BOT_UNANSWERED_QUESTIONS", "Unanswered questions", "Hi,\nHere is a list of all unanswered questions.",  "smtp.office365.com", 587, "botengine/missingQuestions.txt")
