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

package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;


/**
 * Data-binding aware JSP tag for rendering a hidden HTML '<code>input</code>' field
 * containing the databound value.
 *
 * <p>Example (binding to 'name' property of form backing object):
 * <pre class="code>
 * &lt;form:hidden path=&quot;name&quot;/&gt;
 * </pre>
 * 
 * @author Rob Harrop
 * @since 2.0
 */
public class HiddenInputTag extends AbstractDataBoundFormElementTag {

	/**
	 * The CSS class to use when the field bound to a particular tag has errors.
	 */
	private String cssErrorClass;

	/**
	 * Writes the HTML '<code>input</code>' tag to the supplied {@link TagWriter} including the
	 * databound value.
	 * @see #writeDefaultAttributes(TagWriter)
	 * @see #getBoundValue()
	 */
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("input");
		writeDefaultAttributes(tagWriter);
		tagWriter.writeAttribute("type", "hidden");
		tagWriter.writeAttribute("value", getDisplayString(getBoundValue(), getPropertyEditor()));
		tagWriter.endTag();
		return EVAL_PAGE;
	}

	/**
	 * The CSS class to use when the field bound to a particular tag has errors.
	 * May be a runtime expression.
	 */
	public String getCssErrorClass() {
		return cssErrorClass;
	}

	/**
	 * The CSS class to use when the field bound to a particular tag has errors.
	 * May be a runtime expression.
	 */
	public void setCssErrorClass(String cssErrorClass) {
		this.cssErrorClass = cssErrorClass;
	}
}
