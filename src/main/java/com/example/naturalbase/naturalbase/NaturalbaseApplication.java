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

@RestController
@SpringBootApplication
public class NaturalbaseApplication {

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
	
	public static void main(String[] args) {
		SpringApplication.run(NaturalbaseApplication.class, args);
	}

}

