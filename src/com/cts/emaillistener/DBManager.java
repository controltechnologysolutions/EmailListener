package com.cts.emaillistener;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;


public class DBManager {

	private static Connection con = null;

	public static Properties loadProperties(String propName){

		Properties prop = new Properties();
		
		try{
		    FileInputStream fis = new FileInputStream(propName+".properties");
			prop.load(fis);
			fis.close();
		}catch(Exception e){

			try{
				FileInputStream fis = new FileInputStream("F:/Github/EmailListener/resources/" + propName + ".properties");
			    prop.load(fis);
				fis.close();
			}catch(Exception e1){
				e1.printStackTrace();
			}
		}
		
		return prop;
	}


	private static Connection getListenerConnection(){
		return getConnection("listener");
	}

	private static Connection getSenderConnection(){
		return getConnection("emailsender");
	}

	private static Connection getConnection(String prop){

		if (con != null) return con;

		Properties properties = loadProperties(prop);

		try{
			Driver driver = (Driver)Class.forName(properties.getProperty("mxe.db.driver", "sun.jdbc.odbc.JdbcOdbcDriver")).newInstance();
			DriverManager.registerDriver(driver);
			String dbUrl = properties.getProperty("mxe.db.url", "");
			String user = properties.getProperty("mxe.db.user", "maximo");
			String password = properties.getProperty("mxe.db.password", "maximo");
			con = DriverManager.getConnection(dbUrl, user, password);
			con.setAutoCommit(false);

		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("");
		}
		return con;
	}

	public static void updateCommunicationLog(String html, String from, String subject, Date oldestMessage, boolean updateDetailField){

		String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(oldestMessage);

		html = html.replace("'", "''");

		String query = "update commlog set message='"+html+"' where inbound=1 and subject = '"+subject+"' and sendfrom = '"+from+"' and createdate> '"+date+"'; ";

		if (updateDetailField)
			query += "update longdescription set ldtext = '"+html+"' where ldownercol='DESCRIPTION' and ldownertable='TICKET' and "
					+ "ldkey=(select ownerid from commlog where inbound=1 and subject = '"+subject+"' and sendfrom = '"+from+"' and createdate> '"+date+"'); ";

		try{

			Connection conn = DBManager.getListenerConnection();
			conn.setAutoCommit(true);

			Statement statement = conn.createStatement();
			statement.executeUpdate(query);

		}catch(Exception e){
			e.printStackTrace();
		}		
	}

	public static List<HashMap<String, String>> getCommLogsToBeSent(){

		Connection conn = DBManager.getSenderConnection();
		String query = "select * from commlog where subject like '[[]send%'";
		List<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		
		try {

			Statement stmt = conn.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);

			ResultSet rs = stmt.executeQuery(query);
			
			while (rs.next()){
				
				HashMap<String, String> record = new HashMap<String, String>();
				
				record.put("commlogid", String.valueOf(rs.getLong("commlogid")));
				record.put("sendfrom", getString(rs,"sendfrom"));
				record.put("replyto", getString(rs,"replyto"));
				record.put("sendto", getString(rs,"sendto"));
				record.put("subject", getString(rs,"subject").replace("[send]", ""));
				record.put("cc", getString(rs,"cc"));
				record.put("bcc", getString(rs,"bcc"));
				record.put("message", getString(rs,"message").replace("<!-- RICH TEXT -->", ""));
				
				list.add(record);
			}

		      rs.close();
		      stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}

	private static String getString(ResultSet rs, String field) throws SQLException{
		
		try{
			
			if (rs.getString(field) == null)
				return "";
			else
				return rs.getString(field);
		}catch(Exception e){
			e.printStackTrace();
			return "";
		}
	}
	
	public static void updateCommLog(String commLogId, String subject){

		String query = "update commlog set subject='"+subject+"' where commlogid="+commLogId+"; ";

		try{

			Connection conn = DBManager.getSenderConnection();
			conn.setAutoCommit(true);

			Statement statement = conn.createStatement();
			statement.executeUpdate(query);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
