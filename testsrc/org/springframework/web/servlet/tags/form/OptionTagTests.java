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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.beans.Colour;
import org.springframework.beans.TestBean;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.mock.web.MockBodyContent;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.AssertThrows;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.support.BindStatus;

/**
 * Unit tests for the {@link OptionTag} class.
 *
 * @author Rob Harrop
 * @author Rick Evans
 */
public class OptionTagTests extends AbstractHtmlElementTagTests {

	private static final String ARRAY_SOURCE = "abc,123,def";

	private static final String[] ARRAY = StringUtils.commaDelimitedListToStringArray(ARRAY_SOURCE);


	private OptionTag tag;


	protected void onSetUp() {
		this.tag = new OptionTag() {
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setParent(new SelectTag());
		this.tag.setPageContext(getPageContext());
	}


	public void testCanBeDisabledEvenWhenSelected() throws Exception {
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.name", false));
		this.tag.setValue("bar");
		this.tag.setLabel("Bar");
		this.tag.setDisabled("true");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "bar");
		assertContainsAttribute(output, "disabled", "disabled");
		assertBlockTagContains(output, "Bar");
	}

	public void testRenderNotSelected() throws Exception {
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.name", false));
		this.tag.setValue("bar");
		this.tag.setLabel("Bar");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "bar");
		assertBlockTagContains(output, "Bar");
	}

	public void testRenderSelected() throws Exception {
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.name", false));
		this.tag.setValue("foo");
		this.tag.setLabel("Foo");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "foo");
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, "Foo");
	}

	public void testWithNoLabel() throws Exception {
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.name", false));
		this.tag.setValue("bar");
		this.tag.setCssClass("myClass");
		this.tag.setOnclick("CLICK");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "bar");
		assertContainsAttribute(output, "class", "myClass");
		assertContainsAttribute(output, "onclick", "CLICK");
		assertBlockTagContains(output, "bar");
	}

	public void testWithoutContext() throws Exception {
		this.tag.setParent(null);
		this.tag.setValue("foo");
		this.tag.setLabel("Foo");
		new AssertThrows(IllegalStateException.class,
				"Must not be able to use <option> tag without exposed context.") {
			public void test() throws Exception {
				tag.doStartTag();
			}
		}.runTest();
	}

	public void testWithEnum() throws Exception {
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.favouriteColour", false));

		String value = Colour.GREEN.getCode().toString();
		String label = Colour.GREEN.getLabel();

		this.tag.setValue(value);
		this.tag.setLabel(label);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", value);
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, label);
	}

	public void testWithEnumNotSelected() throws Exception {
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.favouriteColour", false));

		String value = Colour.BLUE.getCode().toString();
		String label = Colour.BLUE.getLabel();

		this.tag.setValue(value);
		this.tag.setLabel(label);

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", value);
		assertAttributeNotPresent(output, "selected");
		assertBlockTagContains(output, label);
	}

	public void testWithPropertyEditor() throws Exception {
		BindStatus bindStatus = new BindStatus(getRequestContext(), "testBean.stringArray", false) {
			public PropertyEditor getEditor() {
				return new StringArrayPropertyEditor();
			}
		};
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		this.tag.setValue(ARRAY_SOURCE);
		this.tag.setLabel("someArray");

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", ARRAY_SOURCE);
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, "someArray");

	}

	public void testWithPropertyEditorStringComparison() throws Exception {
		final PropertyEditor testBeanEditor = new TestBeanPropertyEditor();
		testBeanEditor.setValue(new TestBean("Sally"));
		BindStatus bindStatus = new BindStatus(getRequestContext(), "testBean.spouse", false) {
			public PropertyEditor getEditor() {
				return testBeanEditor;
			}
		};
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		this.tag.setValue("Sally");

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "Sally");
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, "Sally");
	}

	public void testWithCustomObjectSelected() throws Exception {
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.someNumber", false));
		this.tag.setValue("${myNumber}");
		this.tag.setLabel("GBP ${myNumber}");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "12.34");
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, "GBP 12.34");
	}

	public void testWithCustomObjectNotSelected() throws Exception {
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), "testBean.someNumber", false));
		this.tag.setValue("${myOtherNumber}");
		this.tag.setLabel("GBP ${myOtherNumber}");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "12.35");
		assertAttributeNotPresent(output, "selected");
		assertBlockTagContains(output, "GBP 12.35");
	}

	public void testWithCustomObjectAndEditorSelected() throws Exception {
		final PropertyEditor floatEditor = new SimpleFloatEditor();
		floatEditor.setValue(new Float("12.34"));
		BindStatus bindStatus = new BindStatus(getRequestContext(), "testBean.someNumber", false) {
			public PropertyEditor getEditor() {
				return floatEditor;
			}
		};
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		this.tag.setValue("${myNumber}");
		this.tag.setLabel("${myNumber}");

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, "12.34f");
	}

	public void testWithCustomObjectAndEditorNotSelected() throws Exception {
		final PropertyEditor floatEditor = new SimpleFloatEditor();
		BindStatus bindStatus = new BindStatus(getRequestContext(), "testBean.someNumber", false) {
			public PropertyEditor getEditor() {
				return floatEditor;
			}
		};
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		this.tag.setValue("${myOtherNumber}");
		this.tag.setLabel("${myOtherNumber}");

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertAttributeNotPresent(output, "selected");
		assertBlockTagContains(output, "12.35f");
	}

	public void testAsBodyTag() throws Exception {
		BindStatus bindStatus = new BindStatus(getRequestContext(), "testBean.name", false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		String bodyContent = "some content";

		this.tag.setValue("foo");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		this.tag.setBodyContent(new MockBodyContent(bodyContent, getWriter()));
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "selected", "selected");
		assertBlockTagContains(output, bodyContent);
	}

	public void testAsBodyTagSelected() throws Exception {
		BindStatus bindStatus = new BindStatus(getRequestContext(), "testBean.name", false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		String bodyContent = "some content";

		this.tag.setValue("Rob Harrop");
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		this.tag.setBodyContent(new MockBodyContent(bodyContent, getWriter()));
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertBlockTagContains(output, bodyContent);
	}

	public void testAsBodyTagCollapsed() throws Exception {
		BindStatus bindStatus = new BindStatus(getRequestContext(), "testBean.name", false);
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		String bodyContent = "some content";

		this.tag.setValue(bodyContent);
		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		this.tag.setBodyContent(new MockBodyContent(bodyContent, getWriter()));
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getOutput();
		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", bodyContent);
		assertBlockTagContains(output, bodyContent);
	}

	public void testAsBodyTagWithEditor() throws Exception {
		BindStatus bindStatus = new BindStatus(getRequestContext(), "testBean.stringArray", false) {
			public PropertyEditor getEditor() {
				return new RulesVariantEditor();
			}
		};
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);

		RulesVariant rulesVariant = new RulesVariant("someRules", "someVariant");
		getPageContext().getRequest().setAttribute("rule", rulesVariant);

		this.tag.setValue("${rule}");

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);

		assertEquals(rulesVariant, getPageContext().getAttribute("value"));
		assertEquals(rulesVariant.toId(), getPageContext().getAttribute("displayValue"));

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);
	}

	public void testMultiBind() throws Exception {
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(new TestBean(), "testBean");
		result.getPropertyAccessor().registerCustomEditor(TestBean.class, "friends", new FriendEditor());
		exposeBindingResult(result);

		BindStatus bindStatus = new BindStatus(getRequestContext(), "testBean.friends", false);

		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, bindStatus);
		this.tag.setValue(new TestBean("foo"));
		this.tag.doStartTag();
		this.tag.doEndTag();

		assertEquals(getOutput(), "<option value=\"foo\">foo</option>");
	}

	public void testOptionTagNotNestedWithinSelectTag() throws Exception {
		new AssertThrows(IllegalStateException.class,
				"Must throw an IllegalStateException when not nested within a <select/> tag.") {
			public void test() throws Exception {
				tag.setParent(null);
				tag.setValue("foo");
				tag.doStartTag();
			}
		}.runTest();
	}


	private void assertOptionTagOpened(String output) {
		assertTrue(output.startsWith("<option"));
	}

	private void assertOptionTagClosed(String output) {
		assertTrue(output.endsWith("</option>"));
	}

	protected void extendRequest(MockHttpServletRequest request) {
		TestBean bean = new TestBean();
		bean.setName("foo");
		bean.setFavouriteColour(Colour.GREEN);
		bean.setStringArray(ARRAY);
		bean.setSpouse(new TestBean("Sally"));
		bean.setSomeNumber(new Float("12.34"));

		List friends = new ArrayList();
		friends.add(new TestBean("bar"));
		friends.add(new TestBean("penc"));
		bean.setFriends(friends);

		request.setAttribute("testBean", bean);
		request.setAttribute("myNumber", new Float(12.34));
		request.setAttribute("myOtherNumber", new Float(12.35));
	}


	private static class TestBeanPropertyEditor extends PropertyEditorSupport {

		public void setAsText(String text) throws IllegalArgumentException {
			setValue(new TestBean(text + "k", 123));
		}

		public String getAsText() {
			return ((TestBean) getValue()).getName();
		}
	}


	public static class RulesVariant implements Serializable {

		private String rules;

		private String variant;

		public RulesVariant(String rules, String variant) {
			this.setRules(rules);
			this.setVariant(variant);
		}

		private void setRules(String rules) {
			this.rules = rules;
		}

		public String getRules() {
			return rules;
		}

		private void setVariant(String variant) {
			this.variant = variant;
		}

		public String getVariant() {
			return variant;
		}

		public String toId() {
			if (this.variant != null) {
				return this.rules + "-" + this.variant;
			} else {
				return rules;
			}
		}

		public static RulesVariant fromId(String id) {
			String[] s = id.split("-", 2);
			String rules = s[0];
			String variant = s.length > 1 ? s[1] : null;
			return new RulesVariant(rules, variant);
		}

		public boolean equals(Object obj) {
			if (obj instanceof RulesVariant) {
				RulesVariant other = (RulesVariant) obj;
				return this.toId().equals(other.toId());
			}
			return false;
		}

		public int hashCode() {
			return this.toId().hashCode();
		}
	}


	public class RulesVariantEditor extends PropertyEditorSupport {

		public void setAsText(String text) throws IllegalArgumentException {
			setValue(RulesVariant.fromId(text));
		}

		public String getAsText() {
			RulesVariant rulesVariant = (RulesVariant) getValue();
			return rulesVariant.toId();
		}
	}


	private static class FriendEditor extends PropertyEditorSupport {

		public void setAsText(String text) throws IllegalArgumentException {
			setValue(new TestBean(text));
		}


		public String getAsText() {
			return ((TestBean) getValue()).getName();
		}
	}

}
