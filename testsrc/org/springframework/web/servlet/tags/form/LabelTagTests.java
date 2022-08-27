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

package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.beans.TestBean;
import org.springframework.mock.web.MockPageContext;
import org.springframework.web.servlet.tags.NestedPathTag;

/**
 * Unit tests for the {@link LabelTag} class.
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public final class LabelTagTests extends AbstractFormTagTests {

	private LabelTag tag;


	protected void onSetUp() {
		this.tag = new LabelTag() {
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPath("name");
		this.tag.setPageContext(getPageContext());
	}

	protected void extendPageContext(MockPageContext pageContext) throws JspException {
		super.extendPageContext(pageContext);

		NestedPathTag nestedPathTag = new NestedPathTag();
		nestedPathTag.setPath("spouse.");
		nestedPathTag.setPageContext(pageContext);
		nestedPathTag.doStartTag();
	}


	public void testSimpleRender() throws Exception {
		int startResult = this.tag.doStartTag();
		int endResult = this.tag.doEndTag();

		assertEquals(Tag.EVAL_BODY_INCLUDE, startResult);
		assertEquals(Tag.EVAL_PAGE, endResult);

		String output = getOutput();
		// we are using a nexted path (see extendPageContext(..)), so...
		assertContainsAttribute(output, "for", "spouse.name");
		// name attribute is not supported by <label/>
		assertAttributeNotPresent(output, "name");
		// id attribute is supported, but we don't want it
		assertAttributeNotPresent(output, "id");
		assertTrue(output.startsWith("<label "));
		assertTrue(output.endsWith("</label>"));
	}

	public void testOverrideFor() throws Exception {
		this.tag.setFor("myElement");
		int startResult = this.tag.doStartTag();
		int endResult = this.tag.doEndTag();

		assertEquals(Tag.EVAL_BODY_INCLUDE, startResult);
		assertEquals(Tag.EVAL_PAGE, endResult);

		String output = getOutput();
		assertContainsAttribute(output, "for", "myElement");
		// name attribute is not supported by <label/>
		assertAttributeNotPresent(output, "name");
		// id attribute is supported, but we don't want it
		assertAttributeNotPresent(output, "id");
		assertTrue(output.startsWith("<label "));
		assertTrue(output.endsWith("</label>"));
	}


	protected TestBean createTestBean() {
		TestBean bean = new TestBean();
		bean.setSpouse(new TestBean("Hoopy"));
		return bean;
	}

}
