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

package org.springframework.jms.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.beans.BeansException;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.parsing.EmptyReaderEventListener;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jca.endpoint.GenericMessageEndpointManager;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Christian Dupuis
 */
public class JmsNamespaceHandlerTests extends TestCase {

	private static final String DEFAULT_CONNECTION_FACTORY = "connectionFactory";

	private static final String EXPLICIT_CONNECTION_FACTORY = "testConnectionFactory";

	private ToolingTestApplicationContext context;


	protected void setUp() throws Exception {
		this.context = new ToolingTestApplicationContext("jmsNamespaceHandlerTests.xml", getClass());
	}

	protected void tearDown() throws Exception {
		this.context.close();
	}

	public void testBeansCreated() {
		Map containers = context.getBeansOfType(DefaultMessageListenerContainer.class);
		assertEquals("Context should contain 3 JMS listener containers", 3, containers.size());

		containers = context.getBeansOfType(GenericMessageEndpointManager.class);
		assertEquals("Context should contain 3 JCA endpoint containers", 3, containers.size());
	}

	public void testContainerConfiguration() throws Exception {
		Map containers = context.getBeansOfType(DefaultMessageListenerContainer.class);
		ConnectionFactory defaultConnectionFactory = (ConnectionFactory) context.getBean(DEFAULT_CONNECTION_FACTORY);
		ConnectionFactory explicitConnectionFactory = (ConnectionFactory) context.getBean(EXPLICIT_CONNECTION_FACTORY);

		int defaultConnectionFactoryCount = 0;
		int explicitConnectionFactoryCount = 0;

		Iterator iter = containers.values().iterator();
		while (iter.hasNext()) {
			DefaultMessageListenerContainer container = (DefaultMessageListenerContainer) iter.next();
			if (container.getConnectionFactory().equals(defaultConnectionFactory)) {
				defaultConnectionFactoryCount++;
			}
			else if (container.getConnectionFactory().equals(explicitConnectionFactory)) {
				explicitConnectionFactoryCount++;
			}
		}

		assertEquals("1 container should have the default connectionFactory", 1, defaultConnectionFactoryCount);
		assertEquals("2 containers should have the explicit connectionFactory", 2, explicitConnectionFactoryCount);
	}

	public void testListeners() throws Exception {
		TestBean testBean1 = (TestBean) context.getBean("testBean1");
		TestBean testBean2 = (TestBean) context.getBean("testBean2");
		TestMessageListener testBean3 = (TestMessageListener) context.getBean("testBean3");

		assertNull(testBean1.getName());
		assertNull(testBean2.getName());
		assertNull(testBean3.message);

		MockControl control1 = MockControl.createControl(TextMessage.class);
		TextMessage message1 = (TextMessage) control1.getMock();
		control1.expectAndReturn(message1.getText(), "Test1");
		control1.replay();

		MessageListener listener1 = getListener("listener1");
		listener1.onMessage(message1);
		assertEquals("Test1", testBean1.getName());
		control1.verify();

		MockControl control2 = MockControl.createControl(TextMessage.class);
		TextMessage message2 = (TextMessage) control2.getMock();
		control2.expectAndReturn(message2.getText(), "Test2");
		control2.replay();

		MessageListener listener2 = getListener("listener2");
		listener2.onMessage(message2);
		assertEquals("Test2", testBean2.getName());
		control2.verify();

		MockControl control3 = MockControl.createControl(TextMessage.class);
		TextMessage message3 = (TextMessage) control3.getMock();
		control3.expectAndReturn(message3.getText(), "Test3");
		control3.replay();

		MessageListener listener3 = getListener(DefaultMessageListenerContainer.class.getName() + "#0");
		listener3.onMessage(message3);
		assertSame(message3, testBean3.message);
		control3.verify();
	}
	
	public void testComponentRegistration() {
		assertTrue("Parser should have registered a component named 'listener1'", context.containsComponentDefinition("listener1"));
		assertTrue("Parser should have registered a component named 'listener2'", context.containsComponentDefinition("listener2"));
		assertTrue("Parser should have registered a component named 'listener3'", context.containsComponentDefinition("listener3"));
		assertTrue("Parser should have registered a component named '" + DefaultMessageListenerContainer.class.getName() + "#0'", 
			context.containsComponentDefinition(DefaultMessageListenerContainer.class.getName() + "#0"));
		assertTrue("Parser should have registered a component named '" + JmsMessageEndpointManager.class.getName() + "#0'", 
			context.containsComponentDefinition(JmsMessageEndpointManager.class.getName() + "#0"));
	}
	
	public void testSourceExtraction() {
		Iterator iterator = context.getRegisteredComponents();
		while (iterator.hasNext()) {
			ComponentDefinition compDef = (ComponentDefinition) iterator.next();
			if (compDef instanceof CompositeComponentDefinition) {
				assertNotNull("CompositeComponentDefinition '" + compDef.getName()+ "' has no source attachment", ((CompositeComponentDefinition) compDef).getSource());
			}
			validateComponentDefinition(compDef);
		}
	}

	private void validateComponentDefinition(ComponentDefinition compDef) {
		BeanDefinition[] beanDefs = compDef.getBeanDefinitions();
		for (int i = 0; i < beanDefs.length; i++) {
			if (beanDefs[i] instanceof AbstractBeanDefinition) {
				assertNotNull("AbstractBeanDefinition has no source attachment", ((AbstractBeanDefinition) beanDefs[i]).getSource());
			}
		}
	}

	private MessageListener getListener(String containerBeanName) {
		DefaultMessageListenerContainer container =
				(DefaultMessageListenerContainer) this.context.getBean(containerBeanName);
		return (MessageListener) container.getMessageListener();
	}


	public static class TestMessageListener implements MessageListener {

		public Message message;

		public void onMessage(Message message) {
			this.message = message;
		}
	}
	

	/**
	 * Internal extension that registers a {@link ReaderEventListener} to store
	 * registered {@link ComponentDefinition}s.
	 */
	private static class ToolingTestApplicationContext extends ClassPathXmlApplicationContext {
		
		private static final Set REGISTERED_COMPONENTS = new HashSet();
		
		public ToolingTestApplicationContext(String path, Class clazz)
				throws BeansException {
			super(path, clazz);
		}

		protected void initBeanDefinitionReader(
				XmlBeanDefinitionReader beanDefinitionReader) {
			beanDefinitionReader.setEventListener(new StoringReaderEventListener(REGISTERED_COMPONENTS));
			beanDefinitionReader.setSourceExtractor(new PassThroughSourceExtractor());
		}
		
		public boolean containsComponentDefinition(String name) {
			Iterator iterator = REGISTERED_COMPONENTS.iterator();
			while (iterator.hasNext()) {
				ComponentDefinition cd = (ComponentDefinition) iterator.next();
				if (cd instanceof CompositeComponentDefinition) {
					ComponentDefinition[] innerCds = ((CompositeComponentDefinition) cd)
							.getNestedComponents();
					for (int i = 0; i < innerCds.length; i++) {
						if (innerCds[i].getName().equals(name)) {
							return true;
						}
					}
				}
				else {
					if (cd.getName().equals(name)) {
						return true;
					}
				}
			}
			return false;
		}
		
		public Iterator getRegisteredComponents() {
			return REGISTERED_COMPONENTS.iterator();
		}
	}
	

	private static class StoringReaderEventListener extends EmptyReaderEventListener {
		
		protected Set registeredComponents = null;
		
		public StoringReaderEventListener(Set registeredComponents) {
			this.registeredComponents = registeredComponents;
			this.registeredComponents.clear();
		}
		
		public void componentRegistered(ComponentDefinition componentDefinition) {
			this.registeredComponents.add(componentDefinition);
		}
	}
}
