@ECHO OFF

SETLOCAL

set CS_CLASSPATH=lib\mail.jar;lib\Opta.jar;lib\commons-io-2.4.jar;lib\commons-codec-1.4.jar;.

java -cp %CS_CLASSPATH% com.cts.emailsender.EmailSender
