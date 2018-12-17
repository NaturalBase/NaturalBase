package com.example.naturalbase.naturalbase;

import java.util.Map;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.naturalbase.naturalcommunicater.*;
import com.example.naturalbase.naturalp2psyncmodule.*;
import com.example.naturalbase.naturalstorage.*;

@RestController
@SpringBootApplication
public class NaturalbaseApplication {
	private static NaturalCommunicater nCommunicater;
	private static NaturalP2PSyncModule nP2pSync;
	private static NaturalStorage nStorage;
	
	private static Logger logger = LoggerFactory.getLogger(NaturalbaseApplication.class);

	@RequestMapping("/")
	public String HomePage() {
		return "This is NaturalBase!";
	}
	
	@RequestMapping(value = "/debug", method = RequestMethod.GET)
	@ResponseBody
	public String DisplayGetHttp(HttpServletRequest request) {
		String httpContent = new String();
		httpContent = "Get a Get Method\r\n";
		
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
		
		System.out.print(httpContent);
		return httpContent;
	}
	
	@RequestMapping(value = "/naturalbase", method = RequestMethod.POST, produces="application/json")
	@ResponseBody
	public String NaturalBaseRequestMain(HttpServletRequest request) {
		return nCommunicater.IncommingRequestProc(request);
	}
	
	public static void main(String[] args) {
		logger.info("Application start Init!");
		nCommunicater = NaturalCommunicater.Instance();
		nStorage = new NaturalStorage();
		nP2pSync = new NaturalP2PSyncModule(nCommunicater, nStorage);
		logger.info("Application finish Init!");
		logger.info("NaturalbaseApplication start run!");
		SpringApplication.run(NaturalbaseApplication.class, args);
	}

}

