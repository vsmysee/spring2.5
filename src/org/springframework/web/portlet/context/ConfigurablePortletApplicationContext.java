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

package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * Interface to be implemented by configurable portlet application contexts.
 * Supported by {@link org.springframework.web.portlet.FrameworkPortlet}.
 *
 * <p>Note: The setters of this interface need to be called before an
 * invocation of the {@link #refresh} method inherited from
 * {@link org.springframework.context.ConfigurableApplicationContext}.
 * They do not cause an initialization of the context on their own.
 *
 * @author Juergen Hoeller
 * @author William G. Thompson, Jr.
 * @author John A. Lewis
 * @since 2.0
 * @see #refresh
 * @see org.springframework.web.context.ContextLoader#createWebApplicationContext
 * @see org.springframework.web.portlet.FrameworkPortlet#createPortletApplicationContext
 * @see org.springframework.web.context.ConfigurableWebApplicationContext
 */
public interface ConfigurablePortletApplicationContext
		extends WebApplicationContext, ConfigurableApplicationContext {

	/**
	 * Any number of these characters are considered delimiters between
	 * multiple context paths in a single String value.
	 * @see org.springframework.web.portlet.FrameworkPortlet#setContextConfigLocation
	 */
	String CONFIG_LOCATION_DELIMITERS = ",; \t\n";


	/**
	 * Set the PortletContext for this portlet application context.
	 * <p>Does not cause an initialization of the context: refresh needs to be
	 * called after the setting of all configuration properties.
	 * @see #refresh()
	 */
	void setPortletContext(PortletContext portletContext);

	/**
	 * Return the standard Portlet API PortletContext for this application.
	 */
	PortletContext getPortletContext();

	/**
	 * Set the PortletConfig for this portlet application context.
	 * @see #refresh()
	 */
	void setPortletConfig(PortletConfig portletConfig);

	/**
	 * Return the PortletConfig for this portlet application context, if any.
	 */
	PortletConfig getPortletConfig();

	/**
	 * Set the namespace for this portlet application context,
	 * to be used for building a default context config location.
	 */
	void setNamespace(String namespace);

	/**
	 * Return the namespace for this web application context, if any.
	 */
	String getNamespace();

	/**
	 * Set the config locations for this portlet application context.
	 * If not set, the implementation is supposed to use a default for the
	 * given namespace.
	 */
	void setConfigLocations(String[] configLocations);

	/**
	 * Return the config locations for this web application context,
	 * or <code>null</code> if none specified.
	 */
	String[] getConfigLocations();

}
