package com.tiggit.bacaro;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.apache.velocity.tools.view.VelocityLayoutServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@MultipartConfig
public class GratsServlet extends VelocityLayoutServlet {
	
	
	/*

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		// get is the same as post for now
		doPost(request, response);
		
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		SpreadSheetReader reader = new SpreadSheetReader();
		List<SheetMap> maps = reader.read();
		
		response.getOutputStream().write("OK".getBytes());
	}
	
	*/
	
	
	@Override
	public Template handleRequest(HttpServletRequest request, HttpServletResponse response, Context context) {

        Logger logger= LoggerFactory.getLogger(LayoutServlet.class);
        
		InputStream in;
		try {
			
			Part filePart = request.getPart("file");
			in = filePart.getInputStream();

			SpreadSheetReader reader = new SpreadSheetReader();
			List<StaffPayment> payments = reader.read(in);

			context.put("payments", payments);
			context.put("date", new DateTool());
			context.put("number", new NumberTool());

			Template template = null;

			try {
				template = getTemplate("templates/payments.vm");

				response.setHeader("Template Returned", "Success");
			} catch (Exception e) {
				logger.error("Error while reading the template ", e);
			}

			return template;

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			
			Template errorTemplate = null;
			
			return errorTemplate;
		} 

	}

}
