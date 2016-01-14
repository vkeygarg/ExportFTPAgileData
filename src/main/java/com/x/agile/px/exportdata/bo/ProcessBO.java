package com.x.agile.px.exportdata.bo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.x.agile.px.exportdata.exception.CustomException;
import com.x.agile.px.exportdata.util.Utils;

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
	
	


	public void init() throws IOException {
		errorMap = new HashMap<String, List<String>>();
		logger.info(System.getenv("AGILE_PROPERTIES"));
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

				attrListMod = getItemAttrList(affItemObj, attrModPropMap);
				itemsMapMod.put(affItemObj.getName(), attrListMod);
			}
		}

		File csvFile = null;
		if (errorMap.isEmpty()) {
			if (!itemsMap.isEmpty())
				csvFile = Utils.getCSVFile(prop.getProperty("CSV_FILE_PATH")+prop.getProperty("DATA_CSV_FILE_NAME")+timeStamp,
						itemsMap, headerList, prop.getProperty("DATA_CSV_DELIMITER"), prop.getProperty("DATA_CSV_EOR"), prop.getProperty("DATA_CSV_EOF"));
			if (csvFile != null){
				//Utils.ftpFile(csvFile,prop.getProperty("ftp.location"), prop.getProperty("ftp.host"), prop.getProperty("ftp.user"), prop.getProperty("ftp.password"), logger);
				Utils.sendSFTP(csvFile, prop, logger);
			}
			if (!itemsMapMod.isEmpty())
				csvFile = Utils.getCSVFile(prop.getProperty("CSV_FILE_PATH")+prop.getProperty("DATA_MODULE_CSV_FILE_NAME")+timeStamp,
						itemsMapMod, headerModList, prop.getProperty("DATA_CSV_DELIMITER"), prop.getProperty("DATA_CSV_EOR"), prop.getProperty("DATA_CSV_EOF"));
			if (csvFile != null){
				//Utils.ftpFile(csvFile,prop.getProperty("ftp.location"), prop.getProperty("ftp.host"), prop.getProperty("ftp.user"), prop.getProperty("ftp.password"), logger);
				Utils.sendSFTP(csvFile, prop, logger);
			}
			
		} else {
			logger.info("Missing data is: "+errorMap);
			csvFile = Utils.getCSVFile("./"+prop.getProperty("ERROR_CSV_FILE_NAME").replace("{change}", chgObj.getName()),errorMap, 
					Arrays.asList("Item Number", "Attribute Name"), prop.getProperty("DATA_CSV_DELIMITER"), null, null);
			if (csvFile != null) {
				Utils.sendEmail(csvFile,prop, logger);
				throw new CustomException(prop.getProperty("ERR_MSG_MISSING_DATA"));
			}
		}
	}

	private List<String> getItemAttrList(IItem affItemObj, Map<String,String> attrProp) throws NumberFormatException, APIException {
		List<String> itemDtls = new ArrayList<String>();

		Set<String> propSet = attrProp.keySet();
		
		Iterator<String> propItr = propSet.iterator();
		String attrKey = null;
		
		while (propItr.hasNext()) {
			Object itemAttrAglVal = null;
			Object aglVal = null;
			attrKey = propItr.next();
			String [] attrProps = attrProp.get(attrKey).split(";");
			int propNo = 1;
			for(String attrprop : attrProps){
				switch (propNo) {
				case 1  : 
					logger.info(attrKey + ": 1st  Token-"+attrprop);
					itemAttrAglVal = attrprop;
					break;
				case 2  :
					logger.info(attrKey + ": 2nd  Token-"+attrprop);
					if (!StringUtils.isEmpty(attrprop)) {
						// switch (aglBaseID){
						if ("ATTACMENT_REST_URL".equalsIgnoreCase(attrprop)) {
							aglVal = getItemAttRestURL(affItemObj.getName(), prop.getProperty("AGL_ITEM_ATTACHMENT_NAME"),
									prop.getProperty("AGL_ITEM_ATTACHMENT_DESC"));
						} else if ("ATTACMENT_REST_URL_WITH_VARIABLE".equalsIgnoreCase(attrprop)) {
							aglVal = "docURL="
									+ getItemAttRestURL(affItemObj.getName(), prop.getProperty("AGL_ITEM_ATTACHMENT_NAME"),
											prop.getProperty("AGL_ITEM_ATTACHMENT_DESC"));
						} else {
							aglVal = affItemObj
									.getValue(NumberUtils.isNumber(attrprop) ? Integer.parseInt(attrprop) : attrprop);
						}
						if(aglVal !=null && !aglVal.toString().isEmpty())
							itemAttrAglVal = aglVal;
					}
					logger.info(attrKey + ": 2nd  Token-"+attrprop+"="+itemAttrAglVal);
					break;
				
				case 3 : 
					logger.info(attrKey + ": 3rd  Token-"+attrprop);
					if("Y".equalsIgnoreCase(attrprop) && (itemAttrAglVal == null || itemAttrAglVal.toString().isEmpty())){
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

	private String getItemAttRestURL(String itemName, String fileName, String fileDesc) {
		StringBuilder uri = new StringBuilder(prop.getProperty("AGL_REST_CALL_URL"));

		try {
			uri.append("?itemName=" + URLEncoder.encode(itemName, "UTF-8"));

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
	
	public void printAttr() {

		BufferedReader br = null;
		Map<String, String> propMap = new TreeMap<String, String>();
		try {
			br = new BufferedReader(
					new FileReader("D:/Agile/Agile934/integration/sdk/extensions/attributeConfig.properties"));
			String line = br.readLine();
			while (line != null && !line.isEmpty()) {
				String key = line.split("=")[0];
				String val = line.split("=")[1];
				propMap.put(key, val);
				line = br.readLine();
			}
		} catch (Exception e) {
		}
		System.out.println(propMap);

	}
	
	public static void main(String [] args){
		ProcessBO ob = new ProcessBO();
		try {
			ob.init();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ob.printAttr();
	}

}
