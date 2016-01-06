package com.x.agile.px.exportdata.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

public class Utils {

	public static File getCSVFile(String fileName, Map<String, List<String>> dataMap, List<String> headerList,
			String delimiter,String eor,String eof) throws IOException {
		File file = new File(fileName);
		Path path = Paths.get(file.getAbsolutePath());
		BufferedWriter writer = null;
	    try {
	    	writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
	    	for (String hdrCell : headerList.subList(0, headerList.size()-1)) {
				writer.write(hdrCell);
				writer.write(delimiter);
			}
	    	for (String dataCell : headerList.subList(headerList.size()-1, headerList.size())) {
				writer.write(dataCell);
			}
	    	if(!StringUtils.isEmpty(eor))
				writer.write(eor);
			writer.newLine();
			
			Iterator<String> rowItr = dataMap.keySet().iterator();
			List<String> colList = null;
			List<String> colSubList = null;
			List<String> lastRowCell = null;
			while (rowItr.hasNext()) {
				colList = dataMap.get(rowItr.next());
				colSubList = colList.subList(0, colList.size()-1);
				lastRowCell  = colList.subList(colList.size()-1, colList.size());
				for (String dataCell : colSubList) {
					writer.write(dataCell);
					writer.write(delimiter);
				}
				for (String dataCell : lastRowCell) {
					writer.write(dataCell);
				}
				if(!StringUtils.isEmpty(eor))
					writer.write(eor);
				writer.newLine();
			}
			if(!StringUtils.isEmpty(eof))
				writer.write(eof);
	    }
		catch (IOException e) {
			throw e;
		} 
	    finally{
	    	if(writer!=null)
	    	writer.close();
	    }
		return file;
	}

	public static void ftpFile(File affItemsCSV,String ftpPath, String ftpUser, String ftpPassword,Logger log) throws IOException {
		FTPClient client = new FTPClient();
		FileInputStream fis = null;

		try {
		    client.connect(ftpPath);
		    client.login(ftpUser, ftpPassword);
		    log.info("Connected to FTP site:"+client.getReplyString());
		    String filename = affItemsCSV.getAbsolutePath();
		    
		    fis = new FileInputStream(filename);
		    client.storeFile(affItemsCSV.getName(), fis);
		    log.info("File Sent");
		    client.logout();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		    throw e;
		} finally {
		    try {
		        if (fis != null) {
		            fis.close();
		        }
		        client.disconnect();
		    } catch (IOException e) {
		        throw e;
		    }
		}
	}

	public static void sendEmail(File csvFile, Properties props, Logger log) {
		    //final String username = "v.garg2@gmail.com";
		    //final String password = "H0sp1talJ@ipur";

		    //Properties props = new Properties();
		   // props.put("mail.smtp.auth", true);
		    //props.put("mail.smtp.starttls.enable", true);
		   // props.put("mail.smtp.host", "smtp.gmail.com");
		   // props.put("mail.smtp.port", "587");

		   /* Session session = Session.getInstance(props,
		            new javax.mail.Authenticator() {
		                protected PasswordAuthentication getPasswordAuthentication() {
		                    return new PasswordAuthentication(username, password);
		                }
		            });*/
		Session session = Session.getInstance(props, null);

		    try {

		        Message message = new MimeMessage(session);
		        message.setFrom(new InternetAddress(props.getProperty("mail.from")));
		        message.setRecipients(Message.RecipientType.TO,
		                InternetAddress.parse(props.getProperty("mail.to")));
		        message.setSubject(props.getProperty("mail.subject"));
		        //message.setText(props.getProperty("mail.body"));
		        

		        BodyPart messageBodyPart = new MimeBodyPart();
		        // Fill the message
		        messageBodyPart.setText(props.getProperty("mail.body"));
		        messageBodyPart.setContent(props.getProperty("mail.body"), "text/html");
		        
		        MimeBodyPart attBodyPart = new MimeBodyPart();

		        Multipart multipart = new MimeMultipart();
		        multipart.addBodyPart(messageBodyPart);
		       // messageBodyPart = new MimeBodyPart();
		        String file = csvFile.getAbsolutePath();
		        String fileName = csvFile.getName();
		        DataSource source = new FileDataSource(file);
		        attBodyPart.setDataHandler(new DataHandler(source));
		        attBodyPart.setFileName(fileName);
		        multipart.addBodyPart(attBodyPart);

		        message.setContent(multipart);

		        log.info("Sending Email");

		        Transport.send(message);

		        log.info("Mail Sent");

		    } catch (MessagingException e) {
		        e.printStackTrace();
		    }
		  }

	

	public static Properties loadPropertyFile(String propFileName) throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		FileInputStream file = null;
		try {
			file = new FileInputStream(propFileName);
			prop.load(file);
		} catch (IOException e) {
			throw new FileNotFoundException("Configuration file '" + propFileName + "' not found in the classpath");
		} finally {
			if (file != null)
				file.close();
		}
		return prop;
	}
	
	public static
	<T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
	  List<T> list = new ArrayList<T>(c);
	  java.util.Collections.sort(list);
	  return list;
	}

	public static Map<String, String> loadSortedAttrMap(String propFileName) throws IOException {
		BufferedReader br = null;
		Map<String,String> propMap = new TreeMap<String, String>();
		try{
		br = new BufferedReader(new FileReader(propFileName));
			String line = br.readLine();
			while (line != null && !line.isEmpty()) {
				if (!line.startsWith("#")) {
					String key = line.split("=")[0];
					String val = line.split("=")[1];
					propMap.put(key, val);
				}
				line = br.readLine();
			}
		}catch(IOException e){
			throw e;
		}
		finally{
			if(br!=null)
				br.close();
		}
			return propMap;
	}

	public static List<String> getSortedHeaderList(Map<String, String> attrPropMap) {
		List<String> sortedList = new ArrayList<String>();
		Iterator<String> itr = attrPropMap.keySet().iterator();
		String key = "";
		while(itr.hasNext()){
			key = itr.next();
			sortedList.add(key.substring(key.indexOf("_")+1, key.length()));
		}
		return sortedList;
	}
}
