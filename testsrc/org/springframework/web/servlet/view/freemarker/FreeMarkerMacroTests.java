/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.view.freemarker;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.Configuration;
import freemarker.template.Template;
import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.servlet.view.DummyMacroRequestContext;

/**
 * @author Darren Davison
 * @author Juergen Hoeller
 * @since 25.01.2005
 */
public class FreeMarkerMacroTests extends TestCase {

	private static final String TEMPLATE_FILE = "test.ftl";


	private StaticWebApplicationContext wac;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private FreeMarkerConfigurer fc;


	public void setUp() throws Exception {
		wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());

		//final Template expectedTemplate = new Template();
		fc = new FreeMarkerConfigurer();
		fc.setPreferFileSystemAccess(false);
		fc.afterPropertiesSet();

		wac.getDefaultListableBeanFactory().registerSingleton("freeMarkerConfigurer", fc);
		wac.refresh();

		request = new MockHttpServletRequest();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		response = new MockHttpServletResponse();
	}

	public void testExposeSpringMacroHelpers() throws Exception {
		FreeMarkerView fv = new FreeMarkerView() {
			protected void processTemplate(Template template, Map model, HttpServletResponse response) {
				assertTrue(model.get(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE) instanceof RequestContext);
				RequestContext rc = (RequestContext) model.get(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE);
				BindStatus status = rc.getBindStatus("tb.name");
				assertEquals("name", status.getExpression());
				assertEquals("juergen", status.getValue());
			}
		};
		fv.setUrl(TEMPLATE_FILE);
		fv.setApplicationContext(wac);
		fv.setExposeSpringMacroHelpers(true);

		Map model = new HashMap();
		model.put("tb", new TestBean("juergen", 99));
		fv.render(model, request, response);
	}

	public void testSpringMacroRequestContextAttributeUsed() {
		final String helperTool = "wrongType";

		FreeMarkerView fv = new FreeMarkerView() {
			protected void processTemplate(Template template, Map model, HttpServletResponse response) {
				fail();
			}
		};
		fv.setUrl(TEMPLATE_FILE);
		fv.setApplicationContext(wac);
		fv.setExposeSpringMacroHelpers(true);

		Map model = new HashMap();
		model.put(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, helperTool);

		try {
			fv.render(model, request, response);
		}
		catch (Exception ex) {
			assertTrue(ex instanceof ServletException);
			assertTrue(ex.getMessage().indexOf(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE) > -1);
		}
	}

	public void testAllMacros() throws Exception {
		DummyMacroRequestContext rc = new DummyMacroRequestContext(request);
		Map msgMap = new HashMap();
		msgMap.put("hello", "Howdy");
		msgMap.put("world", "Mundo");
		rc.setMessageMap(msgMap);
		Map themeMsgMap = new HashMap();
		themeMsgMap.put("hello", "Howdy!");
		themeMsgMap.put("world", "Mundo!");
		rc.setThemeMessageMap(themeMsgMap);
		rc.setContextPath("/springtest");

		TestBean tb = new TestBean("Darren", 99);
		tb.setSpouse(new TestBean("Fred"));
		request.setAttribute("command", tb);

		HashMap names = new HashMap();
		names.put("Darren", "Darren Davison");
		names.put("John", "John Doe");
		names.put("Fred", "Fred Bloggs");
		names.put("Rob&Harrop", "Rob Harrop");

		Configuration config = fc.getConfiguration();
		Map model = new HashMap();
		model.put("command", tb);
		model.put("springMacroRequestContext", rc);
		model.put("msgArgs", new Object[] {"World"});
		model.put("nameOptionMap", names);

		FreeMarkerView view = new FreeMarkerView();
		view.setBeanName("myView");
		view.setUrl("test.ftl");
		view.setExposeSpringMacroHelpers(false);
		view.setConfiguration(config);

		view.render(model, request, response);

		// tokenize output and ignore whitespace
		String output = response.getContentAsString();
		System.out.println(output);
		String[] tokens = StringUtils.tokenizeToStringArray(output, "\t\n");

		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("NAME")) assertEquals("Darren", tokens[i + 1]);
			if (tokens[i].equals("AGE")) assertEquals("99", tokens[i + 1]);
			if (tokens[i].equals("MESSAGE")) assertEquals("Howdy Mundo", tokens[i + 1]);
			if (tokens[i].equals("DEFAULTMESSAGE")) assertEquals("hi planet", tokens[i + 1]);
			if (tokens[i].equals("MESSAGEARGS")) assertEquals("Howdy[World]", tokens[i + 1]);
			if (tokens[i].equals("MESSAGEARGSWITHDEFAULTMESSAGE")) assertEquals("Hi", tokens[i + 1]);
			if (tokens[i].equals("THEME")) assertEquals("Howdy! Mundo!", tokens[i + 1]);
			if (tokens[i].equals("DEFAULTTHEME")) assertEquals("hi! planet!", tokens[i + 1]);
			if (tokens[i].equals("THEMEARGS")) assertEquals("Howdy![World]", tokens[i + 1]);
			if (tokens[i].equals("THEMEARGSWITHDEFAULTMESSAGE")) assertEquals("Hi!", tokens[i + 1]);
			if (tokens[i].equals("URL")) assertEquals("/springtest/aftercontext.html", tokens[i + 1]);
			if (tokens[i].equals("FORM1")) assertEquals("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\"     >", tokens[i + 1]);
			if (tokens[i].equals("FORM2")) assertEquals("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" class=\"myCssClass\"    >", tokens[i + 1]);
			if (tokens[i].equals("FORM3")) assertEquals("<textarea id=\"name\" name=\"name\" >Darren</textarea>", tokens[i + 1]);
			if (tokens[i].equals("FORM4")) assertEquals("<textarea id=\"name\" name=\"name\" rows=10 cols=30>Darren</textarea>", tokens[i + 1]);
			//TODO verify remaining output (fix whitespace)
			if (tokens[i].equals("FORM9")) assertEquals("<input type=\"password\" id=\"name\" name=\"name\" value=\"\"     >", tokens[i + 1]);
			if (tokens[i].equals("FORM10")) assertEquals("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\"     >", tokens[i + 1]);
			if (tokens[i].equals("FORM11")) assertEquals("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\"     >", tokens[i + 1]);
			if (tokens[i].equals("FORM12")) assertEquals("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\"     >", tokens[i + 1]);
			if (tokens[i].equals("FORM13")) assertEquals("<input type=\"password\" id=\"name\" name=\"name\" value=\"\"     >", tokens[i + 1]);
		}
	}

}
