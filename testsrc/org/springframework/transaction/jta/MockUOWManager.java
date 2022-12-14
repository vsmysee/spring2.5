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

package org.springframework.transaction.jta;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.transaction.Synchronization;

import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWActionException;
import com.ibm.wsspi.uow.UOWException;
import com.ibm.wsspi.uow.UOWManager;

/**
 * @author Juergen Hoeller
 */
public class MockUOWManager implements UOWManager {

	private int type;

	private boolean joined;

	private int timeout;

	private boolean rollbackOnly;

	private int status = UOW_STATUS_NONE;

	private final Map resources = new HashMap();

	private final List synchronizations = new LinkedList();


	public void runUnderUOW(int type, boolean join, UOWAction action) throws UOWActionException, UOWException {
		this.type = type;
		this.joined = join;
		try {
			this.status = UOW_STATUS_ACTIVE;
			action.run();
			this.status = (this.rollbackOnly ? UOW_STATUS_ROLLEDBACK : UOW_STATUS_COMMITTED);
		}
		catch (Error err) {
			this.status = UOW_STATUS_ROLLEDBACK;
			throw err;
		}
		catch (RuntimeException ex) {
			this.status = UOW_STATUS_ROLLEDBACK;
			throw ex;
		}
		catch (Exception ex) {
			this.status = UOW_STATUS_ROLLEDBACK;
			throw new UOWActionException(ex);
		}
	}

	public int getUOWType() {
		return this.type;
	}

	public boolean getJoined() {
		return this.joined;
	}

	public long getLocalUOWId() {
		return 0;
	}

	public void setUOWTimeout(int uowType, int timeout) {
		this.timeout = timeout;
	}

	public int getUOWTimeout() {
		return this.timeout;
	}

	public void setRollbackOnly() {
		this.rollbackOnly = true;
	}

	public boolean getRollbackOnly() {
		return this.rollbackOnly;
	}

	public void setUOWStatus(int status) {
		this.status = status;
	}

	public int getUOWStatus() {
		return this.status;
	}

	public void putResource(Object key, Object value) {
		this.resources.put(key, value);
	}

	public Object getResource(Object key) throws NullPointerException {
		return this.resources.get(key);
	}

	public void registerInterposedSynchronization(Synchronization sync) {
		this.synchronizations.add(sync);
	}

	public List getSynchronizations() {
		return this.synchronizations;
	}

}
