package com.x.agile.px.exportdata.bo;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.DataTypeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.x.agile.px.exportdata.exception.CustomException;
import com.x.agile.px.exportdata.util.Utils;
import com.x.agile.px.exportdata.vo.EmailVO;

/**
 * Description: Process Extension class holds implemented business logic
 *
 */
public class ProcessBO {

	Properties prop;
	Map<String,String> attrPropMap;
	final static Logger logger = Logger.getLogger(ProcessBO.class);
	Map<String, List<String>> errorMap = null;
	List<String> headerList = null;
	Map<String,String> attrModPropMap;
	List<String> headerModList = null;
	String DELIMITER = "|";
	String timeStamp ="";
	IAgileSession aglSession = null;
	
	


	public void init(IAgileSession session) throws IOException {
		errorMap = new HashMap<String, List<String>>();
		logger.info(System.getenv("AGILE_PROPERTIES"));
		aglSession = session;
		prop = Utils.loadPropertyFile(System.getenv("AGILE_PROPERTIES")+"\\ExportFTPAgileDataConfig.properties");
		logger.info("Main Config file loaded:"+prop.getProperty("AGL_REST_CALL_URL"));
		attrPropMap = Utils.loadSortedAttrMap(System.getenv("AGILE_PROPERTIES")+"\\ExportFTPAgileDataAttribute.properties");
		attrModPropMap = Utils.loadSortedAttrMap(System.getenv("AGILE_PROPERTIES")+"\\ExportFTPAgileDataModuleAttribute.properties");
		logger.info(attrPropMap.size());
		headerList = Utils.getSortedHeaderList(attrPropMap);
		headerModList = Utils.getSortedHeaderList(attrModPropMap);
		DELIMITER = prop.getProperty("DATA_CSV_DELIMITER");
		Calendar calobj = Calendar.getInstance();
		DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
		df.setTimeZone(TimeZone.getTimeZone(prop.getProperty("LMS_TIMEZONE")));
		timeStamp = df.format(calobj.getTime());
		logger.info("Triggered at:"+timeStamp);
	}

	public void processRequest(IChange chgObj) throws APIException, CustomException, Exception {
		ITable affTable = chgObj.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
		logger.info("# of affected items to process:" + affTable.size());
		ITwoWayIterator affItr = affTable.getReferentIterator();
		IItem affItemObj = null;
		Map<String, List<String>> itemsMap = new HashMap<String, List<String>>();
		List<String> attrList = null;
		
		Map<String, List<String>> itemsMapMod = new HashMap<String, List<String>>();
		List<String> attrListMod = null;
		String lmsID = "";
		Object lmsVal = null;
		while (affItr.hasNext()) {
			affItemObj = (IItem) affItr.next();
			lmsID=prop.getProperty(affItemObj.getAgileClass().getSuperClass().getName().toUpperCase()+".PAGE2.LMSFLAG");
			lmsVal = affItemObj.getValue(NumberUtils.isNumber(lmsID) ? Integer.parseInt(lmsID) : lmsID);
			logger.info("Affected Item: " + affItemObj.getName() + " LMS Flag:"+lmsVal);
			
			if (lmsVal != null && prop.getProperty("LMS_VAL_TO_EXTRACT_DATA").equalsIgnoreCase(lmsVal.toString())) {
				attrList = getItemAttrList(affItemObj, attrPropMap);
				itemsMap.put(affItemObj.getName(), attrList);
				logger.info("Getting Attribute details for : "+affItemObj.getName());
				//attrListMod = getItemAttrList(affItemObj, attrModPropMap);
				//itemsMapMod.put(affItemObj.getName(), attrListMod);
			}
		}

		Path csvFile = null;
		if (errorMap.isEmpty()) {
			logger.info("Generating data files for "+chgObj.getName()+" at "+timeStamp);
			if (!itemsMap.isEmpty())
				csvFile = Utils.getCSVFile(prop.getProperty("CSV_FILE_PATH")+prop.getProperty("DATA_CSV_FILE_NAME")+timeStamp,
						itemsMap, headerList, prop.getProperty("DATA_CSV_DELIMITER"), prop.getProperty("DATA_CSV_EOR"), prop.getProperty("DATA_CSV_EOF"),logger);
			if (csvFile != null){
				//Utils.ftpFile(csvFile,prop.getProperty("ftp.location"), prop.getProperty("ftp.host"), prop.getProperty("ftp.user"), prop.getProperty("ftp.password"), logger);
				//Utils.sendSFTP(csvFile, prop, logger);
			}
			if (!itemsMapMod.isEmpty())
				csvFile = Utils.getCSVFile(prop.getProperty("CSV_FILE_PATH")+prop.getProperty("DATA_MODULE_CSV_FILE_NAME")+timeStamp,
						itemsMapMod, headerModList, prop.getProperty("DATA_CSV_DELIMITER"), prop.getProperty("DATA_CSV_EOR"), prop.getProperty("DATA_CSV_EOF"), logger);
			if (csvFile != null){
				//Utils.ftpFile(csvFile,prop.getProperty("ftp.location"), prop.getProperty("ftp.host"), prop.getProperty("ftp.user"), prop.getProperty("ftp.password"), logger);
				//Utils.sendSFTP(csvFile, prop, logger);
			}
			
		} else {
			logger.info("Missing data for "+chgObj.getName()+" is: "+errorMap);
			csvFile = Utils.getCSVFile(prop.getProperty("ERROR_FILE_PATH")+prop.getProperty("ERROR_CSV_FILE_NAME").replace("{change}", chgObj.getName()),errorMap, 
					Arrays.asList("Item Number", "Attribute Name"), prop.getProperty("DATA_CSV_DELIMITER"), null, null,logger);
			if (csvFile != null) {
				EmailVO emailVO = new EmailVO();
	 			emailVO.setToEmail(prop.getProperty("ERROR_EMAIL_TO"));
	 			emailVO.setCcEmail(prop.getProperty("ERROR_EMAIL_CC"));
	 			emailVO.setFromEmail(prop.getProperty("ERROR_EMAIL_FROM"));
	 			emailVO.setSubjectEmail(prop.getProperty("ERROR_EMAIL_SUBJECT")
	 					.replace("{Change}", chgObj.getName()));
	 			emailVO.setBodyEmail(prop.getProperty("ERROR_EMAIL_BODY"));
	 			emailVO.setContentType(prop.getProperty("ERROR_EMAIL_CONTENT_TYPE"));
	 			emailVO.setPriorityEmail(prop.getProperty("ERROR_EMAIL_PRIORITY"));
				Utils.sendEmail(emailVO , new File [] {csvFile.toFile()},prop, logger);
				throw new CustomException(prop.getProperty("ERR_MSG_MISSING_DATA"));
			}
		}
	}

	private List<String> getItemAttrList(IItem affItemObj, Map<String,String> attrPropMap) throws NumberFormatException, APIException {
		List<String> itemDtls = new ArrayList<String>();

		Set<String> propSet = attrPropMap.keySet();
		
		Iterator<String> propItr = propSet.iterator();
		String attrKey = null;
		
		while (propItr.hasNext()) {
			Object itemAttrAglVal = null;
			Object aglVal = null;
			attrKey = propItr.next();
			
			String [] attrProps = attrPropMap.get(attrKey).split(";");
			int propNo = 1;
			for(String attrProp : attrProps){
				switch (propNo) {
				case 1  : 
					//logger.info(attrKey + ": 1st  Token-"+attrprop);
					itemAttrAglVal = attrProp;
					break;
				case 2  :
					//logger.info(attrKey + ": 2nd  Token-"+attrprop);
					if (!StringUtils.isEmpty(attrProp)) {
						// switch (aglBaseID){
						if (attrKey.endsWith("_APP_ID")) {
							aglVal = getCellValue(affItemObj, attrProp);
							if (aglVal != null && !aglVal.toString().isEmpty()) {
								itemAttrAglVal = aglVal.toString();
							}
							try {
								itemAttrAglVal = itemAttrAglVal + "-" + Integer.parseInt(affItemObj.getRevision());
							} catch (NumberFormatException e) {
								logger.error(e.getMessage());
							}
							
						} else {
							if ("ATTACMENT_REST_URL".equalsIgnoreCase(attrProp)) {
								aglVal = getItemAttRestURL(affItemObj.getName(), affItemObj.getRevision(),
										prop.getProperty("AGL_ITEM_ATTACHMENT_NAME"),
										prop.getProperty("AGL_ITEM_ATTACHMENT_DESC"));
							} else if ("ATTACMENT_REST_URL_WITH_VARIABLE".equalsIgnoreCase(attrProp)) {
								aglVal = "docURL=" + getItemAttRestURL(affItemObj.getName(), affItemObj.getRevision(),
										prop.getProperty("AGL_ITEM_ATTACHMENT_NAME"),
										prop.getProperty("AGL_ITEM_ATTACHMENT_DESC"));
							} else if ("1014".equals(attrProp)) {
								try {
									aglVal = Integer.parseInt(affItemObj.getRevision());
								} catch (NumberFormatException e) {
									logger.error(e.getMessage());
								} catch (Exception e) {
									logger.error(e.getMessage());
								}
							} else {
								aglVal = getCellValue(affItemObj, attrProp);
							}

							if (aglVal != null && !aglVal.toString().isEmpty()) {
								itemAttrAglVal = aglVal.toString();
							}
						}

						// logger.info(attrKey + ": 2nd
						// Token-"+attrprop+"="+itemAttrAglVal);
					}
					break;
				
				case 3 : 
					//logger.info(attrKey + ": 3rd  Token-"+attrprop);
					if("Y".equalsIgnoreCase(attrProp) && (itemAttrAglVal == null || itemAttrAglVal.toString().isEmpty())){
						logMissingData(affItemObj.getName(), attrKey);
					}
					break;
				}
				propNo++;
			
			/*Object itemAttrAglVal = token.nextToken().replace(";", "");
			logger.info(attrKey + ": 1st Token-"+itemAttrAglVal);
			Object aglVal = null;
			if (token.hasMoreTokens()){
				aglBaseID = token.nextToken().replace(";", "");
				logger.info(attrKey + ": 2nd  Token-"+aglBaseID);
				if (!aglBaseID.isEmpty()) {
					// switch (aglBaseID){
					if ("ATTACMENT_REST_URL".equalsIgnoreCase(aglBaseID)) {
						aglVal = getItemAttRestURL(affItemObj.getName(), prop.getProperty("AGL_ITEM_ATTACHMENT_NAME"),
								prop.getProperty("AGL_ITEM_ATTACHMENT_DESC"));
					} else if ("ATTACMENT_REST_URL_WITH_VARIABLE".equalsIgnoreCase(aglBaseID)) {
						aglVal = "docURL="
								+ getItemAttRestURL(affItemObj.getName(), prop.getProperty("AGL_ITEM_ATTACHMENT_NAME"),
										prop.getProperty("AGL_ITEM_ATTACHMENT_DESC"));
					} else {
						aglVal = affItemObj
								.getValue(NumberUtils.isNumber(aglBaseID) ? Integer.parseInt(aglBaseID) : aglBaseID);
					}
					if(aglVal !=null && !aglVal.toString().isEmpty())
						itemAttrAglVal = aglVal;
				}
				logger.info(attrKey + ": 2nd  Token-"+aglBaseID+"="+itemAttrAglVal);
			}
			if(token.hasMoreTokens()){
				valRequired = token.nextToken().replace(";", "");
				logger.info(attrKey + ": 3rd  Token-"+valRequired);
				if("Y".equalsIgnoreCase(valRequired) && (itemAttrAglVal == null || itemAttrAglVal.toString().isEmpty())){
					logMissingData(affItemObj.getName(), attrKey);
				}
			}
			*/
			}
			itemDtls.add(itemAttrAglVal == null ? "" :itemAttrAglVal.toString());
		}
		return itemDtls;
	}

	private Object getCellValue(IItem affItemObj, String attrProp) {
		Object aglVal = null;
		SimpleDateFormat revDateformat = new SimpleDateFormat(prop.getProperty("LMS_DATE_FORMAT"));
		revDateformat.setTimeZone(TimeZone.getTimeZone(prop.getProperty("LMS_TIMEZONE")));
		try{
		ICell cell = affItemObj
				.getCell(NumberUtils.isNumber(attrProp) ? Integer.parseInt(attrProp) : attrProp);
		if (cell != null) {
			if (cell.getDataType() == DataTypeConstants.TYPE_DATE) {
				try {
					aglVal = revDateformat.format(cell.getValue()).toUpperCase();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					aglVal = cell.getValue();
				}
			} else {
				aglVal = cell.getValue();
			}
		}
		}catch(APIException e){
			logger.error(e.getMessage(),e);
		}
		return aglVal ;
	}

	private String getItemAttRestURL(String itemName,String rev, String fileName, String fileDesc) {
		StringBuilder uri = new StringBuilder(prop.getProperty("AGL_REST_CALL_URL"));

		try {
			uri.append("?itemName=" + URLEncoder.encode(itemName, "UTF-8"));
			if (!StringUtils.isEmpty(rev))
				uri.append("&itemRev=" + URLEncoder.encode(rev, "UTF-8"));
			if (!StringUtils.isEmpty(fileName))
				uri.append("&fileName=" + URLEncoder.encode(fileName, "UTF-8"));
			if (!StringUtils.isEmpty(fileDesc))
				uri.append("&fileDesc=" + URLEncoder.encode(fileDesc, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
		logger.info("Encoded URL for Attachment:" + uri.toString());
		return uri.toString();
	}

	private void logMissingData(String itemName, String attrKey) {
		String keyVal = attrKey.substring(attrKey.indexOf("_")+1, attrKey.length());
		if (errorMap.containsKey(itemName)) {
			errorMap.get(itemName).add(keyVal);
		} else {
			List<String> colList = new ArrayList<String>();
			colList.add(itemName);
			colList.add(keyVal);
			errorMap.put(itemName, colList);
		}
	}

}
