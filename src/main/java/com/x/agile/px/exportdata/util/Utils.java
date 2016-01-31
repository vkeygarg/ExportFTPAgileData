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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.x.agile.px.exportdata.vo.EmailVO;

public class Utils {

	public static Path getCSVFile(String fileName, Map<String, List<String>> dataMap, List<String> headerList,
			String delimiter, String eor, String eof, Logger logger) throws IOException {
		//File file = new File(fileName);
		Path path = Paths.get(fileName);
		BufferedWriter writer = null;
		try {
			logger.info("Wrting File: "+fileName);
			writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
			for (String hdrCell : headerList.subList(0, headerList.size() - 1)) {
				writer.write(hdrCell);
				writer.write(delimiter);
			}
			for (String dataCell : headerList.subList(headerList.size() - 1, headerList.size())) {
				writer.write(dataCell);
			}
			if (!StringUtils.isEmpty(eor))
				writer.write(eor);
			writer.newLine();

			Iterator<String> rowItr = dataMap.keySet().iterator();
			List<String> colList = null;
			List<String> colSubList = null;
			List<String> lastRowCell = null;
			while (rowItr.hasNext()) {
				colList = dataMap.get(rowItr.next());
				colSubList = colList.subList(0, colList.size() - 1);
				lastRowCell = colList.subList(colList.size() - 1, colList.size());
				for (String dataCell : colSubList) {
					writer.write(dataCell);
					writer.write(delimiter);
				}
				for (String dataCell : lastRowCell) {
					writer.write(dataCell);
				}
				if (!StringUtils.isEmpty(eor))
					writer.write(eor);
				writer.newLine();
			}
			if (!StringUtils.isEmpty(eof))
				writer.write(eof);
			logger.info("Wrting File Done Successfully: "+path.getFileName());
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
			throw e;
		} finally {
			if (writer != null)
				writer.close();
		}
		return path;
	}

	public static void ftpFile(File affItemsCSV, String ftpLoc, String ftpPath, String ftpUser, String ftpPassword,
			Logger log) throws IOException {
		FTPClient client = new FTPClient();
		FileInputStream fis = null;

		try {
			client.connect(ftpPath);
			client.login(ftpUser, ftpPassword);
			log.info("Connected to FTP site:" + client.getReplyString());
			String filename = affItemsCSV.getAbsolutePath();

			fis = new FileInputStream(filename);
			log.info(ftpLoc + affItemsCSV.getName());
			client.storeFile(ftpLoc + affItemsCSV.getName(), fis);
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

	public static void sendEmail(EmailVO emailObj, File[] csvFile, Properties props, Logger log) {
		// final String username = "v.garg2@gmail.com";
		// final String password = "H0sp1talJ@ipur";

		// Properties props = new Properties();
		// props.put("mail.smtp.auth", true);
		// props.put("mail.smtp.starttls.enable", true);
		// props.put("mail.smtp.host", "smtp.gmail.com");
		// props.put("mail.smtp.port", "587");

		/*
		 * Session session = Session.getInstance(props, new
		 * javax.mail.Authenticator() { protected PasswordAuthentication
		 * getPasswordAuthentication() { return new
		 * PasswordAuthentication(username, password); } });
		 */
		Session session = Session.getInstance(props, null);

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(emailObj.getFromEmail()));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailObj.getToEmail()));
			message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(emailObj.getCcEmail()));
			message.setSubject(emailObj.getSubjectEmail());
			// message.setText(props.getProperty("mail.body"));
			message.setHeader("X-Priority", emailObj.getPriorityEmail());

			BodyPart messageBodyPart = new MimeBodyPart();
			// Fill the message
			messageBodyPart.setText(emailObj.getBodyEmail());
			messageBodyPart.setContent(emailObj.getBodyEmail(), emailObj.getContentType());

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);
			// messageBodyPart = new MimeBodyPart();
			if (csvFile != null && csvFile.length > 0)
				for (File indFile : csvFile) {
					addAttachment(multipart, indFile.getAbsolutePath());
				}
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

	public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

	public static Map<String, String> loadSortedAttrMap(String propFileName) throws IOException {
		BufferedReader br = null;
		Map<String, String> propMap = new TreeMap<String, String>();
		try {
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
		} catch (IOException e) {
			throw e;
		} finally {
			if (br != null)
				br.close();
		}
		return propMap;
	}

	public static List<String> getSortedHeaderList(Map<String, String> attrPropMap) {
		List<String> sortedList = new ArrayList<String>();
		Iterator<String> itr = attrPropMap.keySet().iterator();
		String key = "";
		while (itr.hasNext()) {
			key = itr.next();
			sortedList.add(key.substring(key.indexOf("_") + 1, key.length()));
		}
		return sortedList;
	}

	public static boolean sendSFTP(File file, Properties props, Logger log) {
		boolean isFTP = false;
		String SFTPHOST = props.getProperty("SFTP_HOST_NAME");// "sftp10.successfactors.com";
		int SFTPPORT = Integer.parseInt(props.getProperty("SFTP_PORT"));// 22;
		String SFTPUSER = props.getProperty("SFTP_USER");// "biosensors-stage";
		String SFTPPASS = props.getProperty("SFTP_PASSWORD");// "afE7TWP0ExzTi";
		String SFTPWORKINGDIR = props.getProperty("SFTP_DESTINATION_LOCATION");// "/incoming/";

		com.jcraft.jsch.Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		FileInputStream fis = null;
		log.info("preparing the host information for sftp.");
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			session.setPassword(SFTPPASS);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			log.info("Host connected.");
			channel = session.openChannel("sftp");
			channel.connect();
			log.info("sftp channel opened and connected.");
			channelSftp = (ChannelSftp) channel;
			channelSftp.cd(SFTPWORKINGDIR);
			
			if (file.isDirectory()) {
				File[] fileList = file.listFiles();
				
				for (File indFile : fileList) {
					fis = new FileInputStream(indFile);
					
					channelSftp.put(fis, indFile.getName());
					fis.close();	
				}
			} else {
				fis = new FileInputStream(file);
				channelSftp.put(fis, file.getName());
				fis.close();
			}
			log.info("File transfered successfully to host.");
			isFTP = true;

		} catch (Exception ex) {
			log.error("Exception found while tranfer the response.", ex);
			EmailVO emailVO = new EmailVO();
			emailVO.setToEmail(props.getProperty("FTP_EMAIL_TO"));
			emailVO.setCcEmail(props.getProperty("FTP_EMAIL_CC"));
			emailVO.setFromEmail(props.getProperty("FTP_EMAIL_FROM"));
			emailVO.setSubjectEmail(props.getProperty("FTP_EMAIL_SUBJECT"));
			emailVO.setBodyEmail(props.getProperty("FTP_EMAIL_BODY").replace("{Exception}",
					"<li><ol>" + ex.getMessage() + "</ol></li>"));
			emailVO.setContentType(props.getProperty("FTP_EMAIL_CONTENT_TYPE"));
			emailVO.setPriorityEmail(props.getProperty("FTP_EMAIL_PRIORITY"));
			sendEmail(emailVO, file.isDirectory() ? file.listFiles() : new File[] { file }, props, log);

		} finally {
			channelSftp.exit();
			channel.disconnect();
			session.disconnect();
			if(fis!=null)
				try {
					fis.close();
				} catch (IOException e) {
					log.error(e.getMessage(),e);
				}
			log.info("Host Session disconnected.");
		}
		return isFTP;
	}

	public static void deleteTempFiles(File file, Logger log) {
		log.info("Deleting files : " + file.getAbsolutePath());
		try {
			FileUtils.cleanDirectory(file);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private static void addAttachment(Multipart multipart, String filename) throws MessagingException {
		DataSource source = new FileDataSource(filename);
		BodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName(filename);
		multipart.addBodyPart(messageBodyPart);
	}
}
