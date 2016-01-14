package com.x.agile.px.exportdata.action;

import java.io.FileNotFoundException;
import java.io.IOException;

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
			boObj.init();
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

	/*public static void main (String [] args){
		
		System.out.println("start");
		try {
		PropertyConfigurator.configure(Utils.loadPropertyFile(System.getenv("AGILE_PROPERTIES")+"\\ExportFTPAgileDatalog4j.properties"));
		Logger logger = Logger.getLogger(ExportSendAglDataAction.class);
			logger.info("Form Main" );
			ProcessBO bo = new ProcessBO();
			bo.init();
			logger.info("ExportFTPAgileData Starts for Change");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		
		System.out.println("End");
		
		List<String> rowdata = Arrays.asList("Vikram","Garg");
		Map <String,List<String>> dataMap = new HashMap<String,List<String>>();
		dataMap.put("1", Arrays.asList("Vikram","Garg"));
		dataMap.put("2", Arrays.asList("Anchal","Jain"));
		dataMap.put("3", Arrays.asList("Aashni","G"));
		dataMap.put("4", Arrays.asList("Shivi","Mam"));
		try {
			File file = Utils.getCSVFile("FamilyName.txt", dataMap, Arrays.asList("First Name","Last Name"), ",", ";", null);
			//Utils.sendEmail(file, new Properties());
			//Utils.ftpFile(file, "/Test/","ftp.bacsexperts.com", "fileload", "2uploadfiles", logger);
			
			Utils.sendSFTP(file,Utils.loadPropertyFile(System.getenv("AGILE_PROPERTIES")+"\\ExportFTPAgileDataConfig.properties"),logger);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}*/
	
	
}
