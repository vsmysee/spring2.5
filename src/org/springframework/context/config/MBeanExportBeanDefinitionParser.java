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

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.JdkVersion;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;context:mbean-export/&gt; element.
 *
 * <p>Registers an instance of
 * {@link org.springframework.jmx.export.annotation.AnnotationMBeanExporter}
 * within the context.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.jmx.export.annotation.AnnotationMBeanExporter
 */
class MBeanExportBeanDefinitionParser extends AbstractBeanDefinitionParser {

	private static final String MBEAN_EXPORTER_BEAN_NAME = "mbeanExporter";

	private static final String DEFAULT_DOMAIN_ATTRIBUTE = "default-domain";

	private static final String SERVER_ATTRIBUTE = "server";


	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return MBEAN_EXPORTER_BEAN_NAME;
	}

	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String beanClassName = (JdkVersion.isAtLeastJava15() ?
				"org.springframework.jmx.export.annotation.AnnotationMBeanExporter" :
				"org.springframework.jmx.export.MBeanExporter");
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(beanClassName);
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		String defaultDomain = element.getAttribute(DEFAULT_DOMAIN_ATTRIBUTE);
		if (StringUtils.hasText(defaultDomain)) {
			builder.addPropertyValue("defaultDomain", defaultDomain);
		}
		String serverBeanName = element.getAttribute(SERVER_ATTRIBUTE);
		if (StringUtils.hasText(serverBeanName)) {
			builder.addPropertyReference("server", serverBeanName);
		}
		else {
			AbstractBeanDefinition specialServer = MBeanServerBeanDefinitionParser.findServerForSpecialEnvironment();
			if (specialServer != null) {
				builder.addPropertyValue("server", specialServer);
			}
		}
		return builder.getBeanDefinition();
	}

}
