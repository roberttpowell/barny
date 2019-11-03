package com.tiggit.bacaro;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.VelocityLayoutServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayoutServlet extends VelocityLayoutServlet {
	
	

	@Override
	public Template handleRequest(HttpServletRequest request, HttpServletResponse response, Context context) {

        Logger logger= LoggerFactory.getLogger(LayoutServlet.class);

		

		Template template = null;

		try {
			template = getTemplate("templates/layoutdemo.vm");

			response.setHeader("Template Returned", "Success");
		} catch (Exception e) {
			logger.error("Error while reading the template ",e);
		}

		return template;

	}

}
