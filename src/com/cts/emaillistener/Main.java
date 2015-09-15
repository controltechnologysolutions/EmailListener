package com.cts.emaillistener;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.sun.mail.util.BASE64DecoderStream;

public class Main {

	String html = "";
	LinkedHashMap<String, String> images = new LinkedHashMap<String, String>();
	static Properties properties = new Properties();

	public static void main(String[] args) {

		properties = DBManager.loadProperties("listener");
		new Main().readEmails();
	}


	private void readEmails(){

		Folder folder = null;
		Store store = null; 
		Session session = null;

		try{
			//add timeout so that connection may not hang
			Properties prop = System.getProperties();
			prop.put("mail.store.protocol","pop3");
			//			prop.put("mail.transport.protocol","smtp");
			prop.put("mail.smtp.host", properties.get("mail.smtp.host"));
			prop.put("mail.smtp.port",properties.get("mail.smtp.port"));

			prop.put("mail.pop3.connectiontimeout","300000");//5 minutes 
			prop.put("mail.pop3.timeout","300000");//5 minutes
			prop.put("mail.imap.connectiontimeout","300000");//5 minutes
			prop.put("mail.imap.timeout","300000");//5 minutes
			prop.put("mail.smtp.connectiontimeout","300000");//5 minutes
			prop.put("mail.smtp.timeout","300000");//5 minutes

			session = Session.getDefaultInstance(prop, null);

			store = session.getStore();
			store.connect((String)properties.get("mail.smtp.host"),(String)properties.get("mail.smtp.user"),(String)properties.get("mail.smtp.password"));
			folder = store.getFolder((String)properties.get("mail.smtp.folder"));

			int minutesAgo = Integer.parseInt((String)properties.get("mail.smtp.oldestmessage"));
			
			// Open the Folder
			folder.open(Folder.READ_WRITE);

			int totalMessages = folder.getMessageCount();

			for(int x=totalMessages;x>=1;--x){
				
				try{
					Message m = folder.getMessage(x);
					
					Date oldestMessage = new Date(System.currentTimeMillis()-1000*60*minutesAgo);
					
					// don't process older messages
					if (m.getSentDate().getTime() < oldestMessage.getTime()) break;
					
					processMessage(m, oldestMessage);
					
				}catch(Exception ee){
					ee.printStackTrace();
				}				
			}
			//close the folder
			if(folder.isOpen())
				folder.close(true);
			if(store != null)
				store.close();

		}catch(Exception e){
			e.printStackTrace();
		}

	}

	public void handleMultipart(Multipart multipart) throws MessagingException, IOException {
		for (int i=0, n=multipart.getCount(); i<n; i++) {
			handlePart(multipart.getBodyPart(i));
		}
	}

	public void handlePart(Part part) throws MessagingException, IOException {

		String disposition = part.getDisposition();
		String contentType = part.getContentType();

		if (contentType.contains("multipart")){
			handleMultipart((Multipart) part.getContent());
			return;
		}

		if (disposition == null) { // When just body

//			System.out.println("PART:::::\n"+part.getContent().toString());
			
			// Check if html or plain text content
			if (part.getContentType().contains("html")) 
				html = part.getContent()+"<!-- RICH TEXT -->";
			else if (part.getContentType().contains("text") && html.isEmpty()) 
				html = "<pre>"+part.getContent()+"</pre><!-- RICH TEXT -->";

		} else if (disposition.equalsIgnoreCase(Part.ATTACHMENT)) {



		} else if (disposition.equalsIgnoreCase(Part.INLINE)) {

			getBase64Image(part);

		}
	}

	public void getBase64Image(Part part) throws IOException, MessagingException{

		if (part.getContent() instanceof BASE64DecoderStream){

			BASE64DecoderStream base64DecoderStream = (BASE64DecoderStream) part.getContent();
			byte[] byteArray = IOUtils.toByteArray(base64DecoderStream);
			byte[] encodeBase64 = Base64.encodeBase64(byteArray);

			String imageCID = null;

			try{

				imageCID = part.getHeader("Content-ID")[0];

				String imageContentBase64 = new String(encodeBase64, "UTF-8");
				String imageContentType = part.getContentType();

				imageCID = imageCID.replace("<", "").replace(">", "");
				imageContentType = imageContentType.split(";")[0];

				images.put(
						"cid:"+imageCID,
						"data:"+imageContentType+";base64,"+imageContentBase64);

			}catch(Exception e3){
				e3.printStackTrace();
			}
		}
	}
	
	public void processMessage(Message m, Date oldestMessage) throws IOException, MessagingException{
		
		html = "";
		images = new LinkedHashMap<String, String>();
		String subject = m.getSubject();
		
		Object content = m.getContent();
		if (content instanceof Multipart){
			handleMultipart((Multipart)content);
		}else{
			handlePart(m);
		}

		for (String img : images.keySet()){
			html = html.replace(img, images.get(img));
		}

		String from = m.getFrom()[0].toString();
		from = from.substring(from.indexOf("<")+1, from.indexOf(">"));

		//if it is a response or reply (= contains ##), don't update the detail field
		// it only updates the detail field if it is the first e-mail (which opens the request)
		boolean updateDetailField = !subject.contains("##");
			
		DBManager.updateCommunicationLog(html, from, subject, oldestMessage, updateDetailField);
		
		System.out.println("E-mail '"+subject+"' successfully processed!");
	
	}

}