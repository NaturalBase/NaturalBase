package com.example.naturalbase.naturalbase;

import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.naturalbase.naturalcommunicater.*;
import com.example.naturalbase.naturalp2psyncmodule.*;
import com.example.naturalbase.naturalstorage.*;
import com.example.naturalbase.common.*;

@RestController
@SpringBootApplication
public class NaturalbaseApplication {
	private static NaturalCommunicater nCommunicater;
	private static NaturalP2PSyncModule nP2pSync;
	private static NaturalStorage nStorage;
	private static StructureLog nStructureLog;
	
	private static Logger logger = LoggerFactory.getLogger(NaturalbaseApplication.class);

	@RequestMapping("/")
	public String HomePage() {
		nStructureLog.structureLogTest();
		return "This is NaturalBase!";
	}
	
	@RequestMapping(value = "/debug")
	@ResponseBody
	public String DisplayGetHttp(HttpServletRequest request) {
		String httpContent = new String();
		httpContent = "Get a Request. Method:" + request.getMethod() + "\r\n";
		
		Map<String, String[]> requestMsg = request.getParameterMap();
		Enumeration<String> requestHeader = request.getHeaderNames();
		
		httpContent += "--------------Header--------------\r\n";
		while(requestHeader.hasMoreElements()) {
			String headerKey = requestHeader.nextElement().toString();
			httpContent += "[" + headerKey + "] = " + request.getHeader(headerKey) + "\r\n"; 
		}
		
		httpContent += "--------------Parameter--------------\r\n";
		for (String key : requestMsg.keySet()) {
			httpContent += "[" + key + "]:\r\n";
			for (int i=0; i<requestMsg.get(key).length; i++) {
				httpContent += "[" + i + "]:" + requestMsg.get(key)[i].toString() + "\r\n";
			}
		}
		
		try {
			InputStream inStream = request.getInputStream();
			byte[] inBuffer = new byte[request.getContentLength()];
			inStream.read(inBuffer);
			httpContent += "--------------Body--------------\r\n";
			httpContent += new String(inBuffer);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.print(httpContent);
		return httpContent;
	}
	
	@RequestMapping(value = "/naturalbase", method = RequestMethod.POST, produces="application/json")
	@ResponseBody
	public ResponseEntity<Object> NaturalBaseRequestMain(HttpServletRequest request) {
		return nCommunicater.IncommingRequestProc(request);
	}
	
	public static void main(String[] args) {
		logger.info("Application start Init!");
		nCommunicater = NaturalCommunicater.Instance();
		nStorage = new NaturalStorage();
		nP2pSync = new NaturalP2PSyncModule(nCommunicater, nStorage);
		nStructureLog = new StructureLog();
		logger.info("Application finish Init!");
		
		Date d = new Date();
		logger.info("NaturalbaseApplication start run! Date:" + d.toString());
		SpringApplication.run(NaturalbaseApplication.class, args);
	}

}

