package com.cts.emailsender;
//package com.cts.emailsender;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Properties;
//
//import javax.mail.Address;
//import javax.mail.Message;
//import javax.mail.MessagingException;
//import javax.mail.PasswordAuthentication;
//import javax.mail.Session;
//import javax.mail.Transport;
//import javax.mail.internet.InternetAddress;
//
//import amlabs.invoicing.MessageParser;
//
//import com.cts.emaillistener.DBManager;
//
//public class EmailSender {
//
//
//	public static void main(String[] args) {
//		
//		List<HashMap<String, String>> commLogs = DBManager.getCommLogsToBeSent();
//		for (HashMap<String, String> commLog : commLogs){
//			Message msg = buildMessage(
//					commLog.get("sendfrom"), 
//					commLog.get("replyto"), 
//					commLog.get("sendto"), 
//					commLog.get("cc"), 
//					commLog.get("bcc"), 
//					commLog.get("subject"), 
//					commLog.get("message"));
//			
//			try {
//				
//				if (msg != null){
//					Transport.send(msg);
//					DBManager.updateCommLog((String)commLog.get("commlogid"), commLog.get("subject"));
//				}
//				
//			} catch (MessagingException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//	
//	
//	private static Message buildMessage(String from, String replyto, String to, String cc, String bcc, String subject, String detail){
//
//		final Properties prop = DBManager.loadProperties("emailsender");
//		
//		Session session = null;
//		
//		if (prop.get("mail.smtp.user") == null || "".equals(prop.get("mail.smtp.user")))
//			
//			session = Session.getInstance(prop);
//		
//		else{
//
//			session = Session.getInstance(prop, new javax.mail.Authenticator() {
//				protected PasswordAuthentication getPasswordAuthentication() {
//					return new PasswordAuthentication((String)prop.getProperty("mail.smtp.user"), (String)prop.getProperty("mail.smtp.password"));
//				}
//			});
//		}
//		
//
//		try {
//			Message msg = MessageParser.createMailMessage(detail, session);
//
//			if (from != null)
//				msg.setFrom(new InternetAddress((String) from));
//			
//			if (replyto != null){
//				try{
//					msg.setReplyTo(new Address[]{new InternetAddress(replyto)});	
//				}catch(Exception e){
//					msg.setReplyTo(new Address[]{new InternetAddress(from)});
//				}
//			}
//				
//					
//			if(to !=null && !"".equals(to))
//				for(String emailAdress : to.split(","))
//					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAdress));
//
//			if(cc !=null && !"".equals(cc))
//				for(String emailAdress : cc.split(","))
//					msg.addRecipient(Message.RecipientType.CC, new InternetAddress(emailAdress));
//
//			if(bcc !=null && !"".equals(bcc))
//				for(String emailAdress : bcc.split(","))
//					msg.addRecipient(Message.RecipientType.BCC, new InternetAddress(emailAdress));
//
//			msg.setSubject(subject);
//			
//			return msg;
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//		
//		return null;
//	}
//		
// }
