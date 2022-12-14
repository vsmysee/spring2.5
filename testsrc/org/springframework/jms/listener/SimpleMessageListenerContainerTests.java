/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jms.listener;

import java.util.HashSet;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.easymock.MockControl;
import org.easymock.internal.AlwaysMatcher;

import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.StubQueue;
import org.springframework.test.AssertThrows;

/**
 * Unit tests for the {@link SimpleMessageListenerContainer} class.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public final class SimpleMessageListenerContainerTests extends AbstractMessageListenerContainerTests {

	private static final String DESTINATION_NAME = "foo";

	private static final String EXCEPTION_MESSAGE = "This.Is.It";


	private static final StubQueue QUEUE_DESTINATION = new StubQueue();


	private SimpleMessageListenerContainer container;


	protected void setUp() throws Exception {
		this.container = (SimpleMessageListenerContainer) getContainer();
	}

	protected AbstractMessageListenerContainer getContainer() {
		return new SimpleMessageListenerContainer();
	}


	public void testSessionTransactedModeReallyDoesDefaultToFalse() throws Exception {
		assertFalse("The [pubSubLocal] property of SimpleMessageListenerContainer " +
				"must default to false. Change this test (and the " +
				"attendant Javadoc) if you have changed the default.",
				container.isPubSubNoLocal());
	}

	public void testSettingConcurrentConsumersToZeroIsNotAllowed() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				container.setConcurrentConsumers(0);
				container.afterPropertiesSet();
			}
		}.runTest();
	}

	public void testSettingConcurrentConsumersToANegativeValueIsNotAllowed() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				container.setConcurrentConsumers(-198);
				container.afterPropertiesSet();
			}
		}.runTest();
	}

	public void testInitDoesNotStartTheConnectionIfAutoStartIsSetToFalse() throws Exception {
		MockControl mockMessageConsumer = MockControl.createControl(MessageConsumer.class);
		MessageConsumer messageConsumer = (MessageConsumer) mockMessageConsumer.getMock();
		messageConsumer.setMessageListener(null);
		// anon. inner class passed in, so just expect a call...
		mockMessageConsumer.setMatcher(new AlwaysMatcher());
		mockMessageConsumer.setVoidCallable();
		mockMessageConsumer.replay();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		session.createQueue(DESTINATION_NAME);
		mockSession.setReturnValue(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		session.createConsumer(QUEUE_DESTINATION, null); // no MessageSelector...
		mockSession.setReturnValue(messageConsumer);
		mockSession.replay();

		MockControl mockConnection = MockControl.createControl(Connection.class);
		Connection connection = (Connection) mockConnection.getMock();
		// session gets created in order to register MessageListener...
		connection.createSession(this.container.isSessionTransacted(), this.container.getSessionAcknowledgeMode());
		mockConnection.setReturnValue(session);
		mockConnection.replay();

		MockControl mockConnectionFactory = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) mockConnectionFactory.getMock();
		connectionFactory.createConnection();
		mockConnectionFactory.setReturnValue(connection);
		mockConnectionFactory.replay();

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);

		this.container.setMessageListener(new TestMessageListener());
		this.container.setAutoStartup(false);
		this.container.afterPropertiesSet();

		mockMessageConsumer.verify();
		mockSession.verify();
		mockConnection.verify();
		mockConnectionFactory.verify();
	}

	public void testInitStartsTheConnectionByDefault() throws Exception {
		MockControl mockMessageConsumer = MockControl.createControl(MessageConsumer.class);
		MessageConsumer messageConsumer = (MessageConsumer) mockMessageConsumer.getMock();
		messageConsumer.setMessageListener(null);
		// anon. inner class passed in, so just expect a call...
		mockMessageConsumer.setMatcher(new AlwaysMatcher());
		mockMessageConsumer.setVoidCallable();
		mockMessageConsumer.replay();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		session.createQueue(DESTINATION_NAME);
		mockSession.setReturnValue(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		session.createConsumer(QUEUE_DESTINATION, null); // no MessageSelector...
		mockSession.setReturnValue(messageConsumer);
		mockSession.replay();

		MockControl mockConnection = MockControl.createControl(Connection.class);
		Connection connection = (Connection) mockConnection.getMock();
		// session gets created in order to register MessageListener...
		connection.createSession(this.container.isSessionTransacted(), this.container.getSessionAcknowledgeMode());
		mockConnection.setReturnValue(session);
		// and the connection is start()ed after the listener is registered...
		connection.start();
		mockConnection.setVoidCallable();
		mockConnection.replay();

		MockControl mockConnectionFactory = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) mockConnectionFactory.getMock();
		connectionFactory.createConnection();
		mockConnectionFactory.setReturnValue(connection);
		mockConnectionFactory.replay();

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);

		this.container.setMessageListener(new TestMessageListener());
		this.container.afterPropertiesSet();

		mockMessageConsumer.verify();
		mockSession.verify();
		mockConnection.verify();
		mockConnectionFactory.verify();
	}

	public void testCorrectSessionExposedForSessionAwareMessageListenerInvocation() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		MockControl mockSession = MockControl.createControl(Session.class);
		final Session session = (Session) mockSession.getMock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		session.createQueue(DESTINATION_NAME);
		mockSession.setReturnValue(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		session.createConsumer(QUEUE_DESTINATION, null); // no MessageSelector...
		mockSession.setReturnValue(messageConsumer);
		// an exception is thrown, so the rollback logic is being applied here...
		session.getTransacted();
		mockSession.setReturnValue(false);
		session.getAcknowledgeMode();
		mockSession.setReturnValue(Session.AUTO_ACKNOWLEDGE);
		mockSession.replay();

		MockControl mockConnection = MockControl.createControl(Connection.class);
		Connection connection = (Connection) mockConnection.getMock();
		// session gets created in order to register MessageListener...
		connection.createSession(this.container.isSessionTransacted(), this.container.getSessionAcknowledgeMode());
		mockConnection.setReturnValue(session);
		// and the connection is start()ed after the listener is registered...
		connection.start();
		mockConnection.setVoidCallable();
		mockConnection.replay();

		MockControl mockConnectionFactory = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory connectionFactory = (ConnectionFactory) mockConnectionFactory.getMock();
		connectionFactory.createConnection();
		mockConnectionFactory.setReturnValue(connection);
		mockConnectionFactory.replay();

		final HashSet failure = new HashSet();

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(new SessionAwareMessageListener() {
			public void onMessage(Message message, Session sess) {
				try {
					// Check correct Session passed into SessionAwareMessageListener.
					assertSame(sess, session);
				}
				catch (Throwable ex) {
					failure.add("MessageListener execution failed: " + ex);
				}
			}
		});

		this.container.afterPropertiesSet();

		MockControl mockMessage = MockControl.createControl(Message.class);
		final Message message = (Message) mockMessage.getMock();
		mockMessage.replay();
		messageConsumer.sendMessage(message);

		if (!failure.isEmpty()) {
			fail(failure.iterator().next().toString());
		}

		mockMessage.verify();
		mockSession.verify();
		mockConnection.verify();
		mockConnectionFactory.verify();
	}

	public void testTaskExecutorCorrectlyInvokedWhenSpecified() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		MockControl mockSession = MockControl.createControl(Session.class);
		final Session session = (Session) mockSession.getMock();
		session.createQueue(DESTINATION_NAME);
		mockSession.setReturnValue(QUEUE_DESTINATION);
		session.createConsumer(QUEUE_DESTINATION, null); // no MessageSelector...
		mockSession.setReturnValue(messageConsumer);
		session.getTransacted();
		mockSession.setReturnValue(false);
		session.getAcknowledgeMode();
		mockSession.setReturnValue(Session.AUTO_ACKNOWLEDGE);
		mockSession.replay();

		MockControl mockConnection = MockControl.createControl(Connection.class);
		Connection connection = (Connection) mockConnection.getMock();
		connection.createSession(this.container.isSessionTransacted(), this.container.getSessionAcknowledgeMode());
		mockConnection.setReturnValue(session);
		connection.start();
		mockConnection.setVoidCallable();
		mockConnection.replay();

		MockControl mockConnectionFactory = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory connectionFactory = (ConnectionFactory) mockConnectionFactory.getMock();
		connectionFactory.createConnection();
		mockConnectionFactory.setReturnValue(connection);
		mockConnectionFactory.replay();

		final TestMessageListener listener = new TestMessageListener();

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(listener);
		this.container.setTaskExecutor(new TaskExecutor() {
			public void execute(Runnable task) {
				listener.executorInvoked = true;
				assertFalse(listener.listenerInvoked);
				task.run();
				assertTrue(listener.listenerInvoked);
			}
		});
		this.container.afterPropertiesSet();

		MockControl mockMessage = MockControl.createControl(Message.class);
		final Message message = (Message) mockMessage.getMock();
		mockMessage.replay();
		messageConsumer.sendMessage(message);

		assertTrue(listener.executorInvoked);
		assertTrue(listener.listenerInvoked);
		mockMessage.verify();
		mockSession.verify();
		mockConnection.verify();
		mockConnectionFactory.verify();
	}

	public void testRegisteredExceptionListenerIsInvokedOnException() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		session.createQueue(DESTINATION_NAME);
		mockSession.setReturnValue(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		session.createConsumer(QUEUE_DESTINATION, null); // no MessageSelector...
		mockSession.setReturnValue(messageConsumer);
		// an exception is thrown, so the rollback logic is being applied here...
		session.getTransacted();
		mockSession.setReturnValue(false);
		mockSession.replay();

		MockControl mockConnection = MockControl.createControl(Connection.class);
		Connection connection = (Connection) mockConnection.getMock();
		// session gets created in order to register MessageListener...
		connection.createSession(this.container.isSessionTransacted(), this.container.getSessionAcknowledgeMode());
		mockConnection.setReturnValue(session);
		// and the connection is start()ed after the listener is registered...
		connection.start();
		mockConnection.setVoidCallable();
		mockConnection.replay();

		MockControl mockConnectionFactory = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) mockConnectionFactory.getMock();
		connectionFactory.createConnection();
		mockConnectionFactory.setReturnValue(connection);
		mockConnectionFactory.replay();

		final JMSException theException = new JMSException(EXCEPTION_MESSAGE);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(new SessionAwareMessageListener() {
			public void onMessage(Message message, Session session) throws JMSException {
				throw theException;
			}
		});

		MockControl mockExceptionListener = MockControl.createControl(ExceptionListener.class);
		ExceptionListener exceptionListener = (ExceptionListener) mockExceptionListener.getMock();
		exceptionListener.onException(theException);
		mockExceptionListener.setVoidCallable();
		mockExceptionListener.replay();

		this.container.setExceptionListener(exceptionListener);
		this.container.afterPropertiesSet();

		// manually trigger an Exception with the above bad MessageListener...
		MockControl mockMessage = MockControl.createControl(Message.class);
		final Message message = (Message) mockMessage.getMock();
		mockMessage.replay();

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);

		mockExceptionListener.verify();
		mockMessage.verify();
		mockSession.verify();
		mockConnection.verify();
		mockConnectionFactory.verify();
	}

	public void testNoRollbackOccursIfSessionIsNotTransactedAndThatExceptionsDo_NOT_Propagate() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		session.createQueue(DESTINATION_NAME);
		mockSession.setReturnValue(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		session.createConsumer(QUEUE_DESTINATION, null); // no MessageSelector...
		mockSession.setReturnValue(messageConsumer);
		// an exception is thrown, so the rollback logic is being applied here...
		session.getTransacted();
		mockSession.setReturnValue(false);
		mockSession.replay();

		MockControl mockConnection = MockControl.createControl(Connection.class);
		Connection connection = (Connection) mockConnection.getMock();
		// session gets created in order to register MessageListener...
		connection.createSession(this.container.isSessionTransacted(), this.container.getSessionAcknowledgeMode());
		mockConnection.setReturnValue(session);
		// and the connection is start()ed after the listener is registered...
		connection.start();
		mockConnection.setVoidCallable();
		mockConnection.replay();

		MockControl mockConnectionFactory = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) mockConnectionFactory.getMock();
		connectionFactory.createConnection();
		mockConnectionFactory.setReturnValue(connection);
		mockConnectionFactory.replay();

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(new MessageListener() {
			public void onMessage(Message message) {
				throw new UnsupportedOperationException();
			}
		});
		this.container.afterPropertiesSet();

		// manually trigger an Exception with the above bad MessageListener...
		MockControl mockMessage = MockControl.createControl(Message.class);
		final Message message = (Message) mockMessage.getMock();
		mockMessage.replay();

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);

		mockMessage.verify();
		mockSession.verify();
		mockConnection.verify();
		mockConnectionFactory.verify();
	}

	public void testTransactedSessionsGetRollbackLogicAppliedAndThatExceptionsStillDo_NOT_Propagate() throws Exception {
		this.container.setSessionTransacted(true);

		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		session.createQueue(DESTINATION_NAME);
		mockSession.setReturnValue(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		session.createConsumer(QUEUE_DESTINATION, null); // no MessageSelector...
		mockSession.setReturnValue(messageConsumer);
		// an exception is thrown, so the rollback logic is being applied here...
		session.getTransacted();
		mockSession.setReturnValue(true);
		// Session is rolled back 'cos it is transacted...
		session.rollback();
		mockSession.setVoidCallable();
		mockSession.replay();

		MockControl mockConnection = MockControl.createControl(Connection.class);
		Connection connection = (Connection) mockConnection.getMock();
		// session gets created in order to register MessageListener...
		connection.createSession(this.container.isSessionTransacted(), this.container.getSessionAcknowledgeMode());
		mockConnection.setReturnValue(session);
		// and the connection is start()ed after the listener is registered...
		connection.start();
		mockConnection.setVoidCallable();
		mockConnection.replay();

		MockControl mockConnectionFactory = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) mockConnectionFactory.getMock();
		connectionFactory.createConnection();
		mockConnectionFactory.setReturnValue(connection);
		mockConnectionFactory.replay();

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(new MessageListener() {
			public void onMessage(Message message) {
				throw new UnsupportedOperationException();
			}
		});
		this.container.afterPropertiesSet();

		// manually trigger an Exception with the above bad MessageListener...
		MockControl mockMessage = MockControl.createControl(Message.class);
		final Message message = (Message) mockMessage.getMock();
		mockMessage.replay();

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);

		mockMessage.verify();
		mockSession.verify();
		mockConnection.verify();
		mockConnectionFactory.verify();
	}

	public void testDestroyClosesConsumersSessionsAndConnectionInThatOrder() throws Exception {
		MockControl mockMessageConsumer = MockControl.createControl(MessageConsumer.class);
		MessageConsumer messageConsumer = (MessageConsumer) mockMessageConsumer.getMock();
		messageConsumer.setMessageListener(null);
		// anon. inner class passed in, so just expect a call...
		mockMessageConsumer.setMatcher(new AlwaysMatcher());
		mockMessageConsumer.setVoidCallable();
		// closing down...
		messageConsumer.close();
		mockMessageConsumer.setVoidCallable();
		mockMessageConsumer.replay();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		session.createQueue(DESTINATION_NAME);
		mockSession.setReturnValue(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		session.createConsumer(QUEUE_DESTINATION, null); // no MessageSelector...
		mockSession.setReturnValue(messageConsumer);
		// closing down...
		session.close();
		mockSession.setVoidCallable();
		mockSession.replay();

		MockControl mockConnection = MockControl.createControl(Connection.class);
		Connection connection = (Connection) mockConnection.getMock();
		// session gets created in order to register MessageListener...
		connection.createSession(this.container.isSessionTransacted(), this.container.getSessionAcknowledgeMode());
		mockConnection.setReturnValue(session);
		// and the connection is start()ed after the listener is registered...
		connection.start();
		mockConnection.setVoidCallable();
		// closing down...
		connection.close();
		mockConnection.setVoidCallable();
		mockConnection.replay();

		MockControl mockConnectionFactory = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) mockConnectionFactory.getMock();
		connectionFactory.createConnection();
		mockConnectionFactory.setReturnValue(connection);
		mockConnectionFactory.replay();

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);

		this.container.setMessageListener(new TestMessageListener());
		this.container.afterPropertiesSet();
		this.container.destroy();

		mockMessageConsumer.verify();
		mockSession.verify();
		mockConnection.verify();
		mockConnectionFactory.verify();
	}


	private static class TestMessageListener implements MessageListener {

		public boolean executorInvoked = false;

		public boolean listenerInvoked = false;

		public void onMessage(Message message) {
			this.listenerInvoked = true;
		}
	}


	private static class SimpleMessageConsumer implements MessageConsumer {

		private MessageListener messageListener;

		public void sendMessage(Message message) throws JMSException {
			this.messageListener.onMessage(message);
		}

		public String getMessageSelector() throws JMSException {
			throw new UnsupportedOperationException();
		}

		public MessageListener getMessageListener() throws JMSException {
			return this.messageListener;
		}

		public void setMessageListener(MessageListener messageListener) throws JMSException {
			this.messageListener = messageListener;
		}

		public Message receive() throws JMSException {
			throw new UnsupportedOperationException();
		}

		public Message receive(long l) throws JMSException {
			throw new UnsupportedOperationException();
		}

		public Message receiveNoWait() throws JMSException {
			throw new UnsupportedOperationException();
		}

		public void close() throws JMSException {
			throw new UnsupportedOperationException();
		}
	}

}
