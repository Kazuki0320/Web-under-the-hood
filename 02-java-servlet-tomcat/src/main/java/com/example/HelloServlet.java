package com.example;

import java.io.PrintWriter;
import java.io.IOException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HelloServlet extends HttpServlet {
	@Override
	protected void doGet(
		HttpServletRequest req,
		HttpServletResponse res
	) throws IOException {
		res.setContentType("text/plain; charset=UTF-8");
		PrintWriter out = res.getWriter();
		out.println("Hello from Servlet on Tomcat!");
	}
}
