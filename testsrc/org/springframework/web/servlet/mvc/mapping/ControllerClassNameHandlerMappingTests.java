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

package org.springframework.web.servlet.mvc.mapping;

import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class ControllerClassNameHandlerMappingTests extends TestCase {

	public static final String LOCATION = "/org/springframework/web/servlet/mvc/mapping/mapping.xml";

	private XmlWebApplicationContext wac;

	private HandlerMapping hm;

	private HandlerMapping hm2;

	private HandlerMapping hm3;

	private HandlerMapping hm4;

	public void setUp() throws Exception {
		MockServletContext sc = new MockServletContext("");
		this.wac = new XmlWebApplicationContext();
		this.wac.setServletContext(sc);
		this.wac.setConfigLocations(new String[] {LOCATION});
		this.wac.refresh();
		this.hm = (HandlerMapping) this.wac.getBean("mapping");
		this.hm2 = (HandlerMapping) this.wac.getBean("mapping2");
		this.hm3 = (HandlerMapping) this.wac.getBean("mapping3");
		this.hm4 = (HandlerMapping) this.wac.getBean("mapping4");
	}

	public void testIndexUri() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("index"), chain.getHandler());
	}

	public void testMapSimpleUri() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/welcome");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

	public void testWithContextPath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/welcome");
		request.setContextPath("/myapp");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

	public void testWithMultiActionControllerMapping() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/user");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("admin"), chain.getHandler());

		request = new MockHttpServletRequest("GET", "/admin/product");
		chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("admin"), chain.getHandler());
	}

	public void testWithoutControllerSuffix() throws Exception {
	  MockHttpServletRequest request = new MockHttpServletRequest("GET", "/buyform");
		HandlerExecutionChain chain = this.hm.getHandler(request);
		assertEquals(this.wac.getBean("buy"), chain.getHandler());
	}

	public void testWithBasePackage() throws Exception {
	  MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/mvc/mapping/welcome");
		HandlerExecutionChain chain = this.hm2.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

	public void testWithBasePackageAndCaseSensitive() throws Exception {
	  MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/mvc/mapping/buyForm");
		HandlerExecutionChain chain = this.hm2.getHandler(request);
		assertEquals(this.wac.getBean("buy"), chain.getHandler());
	}

	public void testWithFullBasePackage() throws Exception {
	  MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/welcome");
		HandlerExecutionChain chain = this.hm3.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

	public void testWithRootAsBasePackage() throws Exception {
	  MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myapp/org/springframework/web/servlet/mvc/mapping/welcome");
		HandlerExecutionChain chain = this.hm4.getHandler(request);
		assertEquals(this.wac.getBean("welcome"), chain.getHandler());
	}

}
