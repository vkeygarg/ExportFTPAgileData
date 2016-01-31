package com.x.agile.px.exportdata.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.agile.api.APIException;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.INode;
import com.agile.px.ActionResult;
import com.agile.px.ICustomAction;
import com.x.agile.px.exportdata.bo.ProcessBO;
import com.x.agile.px.exportdata.exception.CustomException;
import com.x.agile.px.exportdata.util.Utils;

/**
 * @author 
 * Description: 
 *
 */
public class ExportSendAglDataAction implements ICustomAction {

	public ActionResult doAction(IAgileSession aglSession, INode node, IDataObject dataObj) {
		ActionResult actRes = null;
		IChange chgObj = null;
		try{
		PropertyConfigurator.configure(Utils.loadPropertyFile(System.getenv("AGILE_PROPERTIES")+"\\ExportFTPAgileDatalog4j.properties"));
		Logger logger = Logger.getLogger(ExportSendAglDataAction.class);
		try {
			chgObj = (IChange) dataObj;
			logger.info("ExportFTPAgileData Starts for Change ::"+chgObj);
			ProcessBO boObj = new ProcessBO();
			boObj.init(aglSession);
			boObj.processRequest(chgObj);
			actRes = new ActionResult(ActionResult.STRING, "Completed Successfulyy!");
			logger.info("ExportFTPAgileData Completed Successfully");
		} catch (APIException e) {
			logger.error(e.getMessage(), e);
			actRes = new ActionResult(ActionResult.EXCEPTION, e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			actRes = new ActionResult(ActionResult.EXCEPTION, e);
		} catch (CustomException e) {
			logger.error(e.getMessage(), e);
			actRes = new ActionResult(ActionResult.EXCEPTION, e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			actRes = new ActionResult(ActionResult.EXCEPTION, e);
		}}
		catch(IOException e){
			actRes = new ActionResult(ActionResult.EXCEPTION, e);
		}
		return actRes;
	}

	public static void main (String [] args){
		Logger logger = null;
		System.out.println("start");
		try {
		PropertyConfigurator.configure(Utils.loadPropertyFile(System.getenv("AGILE_PROPERTIES")+"\\ExportFTPAgileDatalog4j.properties"));
		logger  = Logger.getLogger(ExportSendAglDataAction.class);
		//	logger.info("Form Main" );
		//	ProcessBO bo = new ProcessBO();
		//	bo.init();
		//	logger.info("ExportFTPAgileData Starts for Change");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		/*List<String> rowdata = Arrays.asList("1","2");
		Map <String,List<String>> dataMap = new HashMap<String,List<String>>();
		dataMap.put("1", Arrays.asList("11","21"));
		dataMap.put("2", Arrays.asList("12","22"));
		dataMap.put("3", Arrays.asList("13","23"));
		dataMap.put("4", Arrays.asList("14","M24am"));*/
		try {
			//File file = Utils.getCSVFile("test.txt", dataMap, Arrays.asList("num 1","num 2"), ",", ";", null);
			//Utils.sendEmail(file, new Properties());
			//Utils.ftpFile(file, "/Test/","ftp.bacsexperts.com", "fileload", "2uploadfiles", logger);
			Properties prop = Utils.loadPropertyFile(System.getenv("AGILE_PROPERTIES")+"\\ExportFTPAgileDataConfig.properties");
			Path path = Paths.get(prop.getProperty("CSV_FILE_PATH"));
			if(Utils.sendSFTP(path.toFile(),prop,logger))
				Utils.deleteTempFiles(path.toFile(), logger);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("End");
		
	}
	
	
}
