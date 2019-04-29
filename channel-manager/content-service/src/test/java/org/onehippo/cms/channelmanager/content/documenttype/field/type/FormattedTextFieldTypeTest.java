/*
 * Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorFactory;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({HtmlProcessorFactory.class, LocalizationUtils.class, FieldTypeUtils.class})
public class FormattedTextFieldTypeTest {

    private FormattedTextFieldType initField(final String defaultJson, final String overlayedJson, final String appendedJson) {
        return initField(defaultJson, overlayedJson, appendedJson, "formatted", "formatted");
    }

    private FormattedTextFieldType initField(final String defaultJson, final String overlayedJson, final String appendedJson,
                                             final String defaultHtmlProcessorId, final String htmlProcessorId) {

        final FormattedTextFieldType field = new FormattedTextFieldType(defaultJson, defaultHtmlProcessorId);
        final FieldTypeContext fieldContext = new MockFieldTypeContext.Builder(field)
                .jcrName("myproject:htmlfield")
                .parentContextLocale("nl")
                .build();

        expect(fieldContext.getStringConfig("maxlength")).andReturn(Optional.empty());
        expect(fieldContext.getStringConfig("ckeditor.config.overlayed.json")).andReturn(Optional.of(overlayedJson));
        expect(fieldContext.getStringConfig("ckeditor.config.appended.json")).andReturn(Optional.of(appendedJson));
        expect(fieldContext.getStringConfig("htmlprocessor.id")).andReturn(Optional.of(htmlProcessorId));

        replayAll();

        field.init(fieldContext);

        return field;
    }

    @Test
    public void getType() {
        final FormattedTextFieldType field = new FormattedTextFieldType("", "formatted");
        assertEquals(FieldType.Type.HTML, field.getType());
    }

    @Test
    public void configWithErrorsIsNull() {
        final FormattedTextFieldType field = new FormattedTextFieldType("{ this is not valid json ", "formatted");
        assertNull(field.getConfig());
    }

    @Test
    public void configContainsLanguage() {
        final FormattedTextFieldType field = initField("", "", "");
        assertEquals("nl", field.getConfig().get("language").asText());

        verifyAll();
    }

    @Test
    public void configIsCombined() {
        final FormattedTextFieldType field = initField("{ test: 1, plugins: 'a,b' }", "{ test: 2 }", "{ plugins: 'c,d' }");
        assertEquals(2, field.getConfig().get("test").asInt());
        assertEquals("a,b,c,d", field.getConfig().get("plugins").asText());

        verifyAll();
    }

    @Test
    public void customConfigIsDisabledWhenNotConfigured() {
        final FormattedTextFieldType field = initField("", "", "");
        assertEquals("", field.getConfig().get(CKEditorConfig.CUSTOM_CONFIG).asText());

        verifyAll();
    }

    @Test
    public void customConfigIsKeptWhenConfigured() {
        final FormattedTextFieldType field = initField("{ customConfig: 'myconfig.js' }", "", "");
        assertEquals("myconfig.js", field.getConfig().get(CKEditorConfig.CUSTOM_CONFIG).asText());

        verifyAll();
    }

    @Test
    public void customHtmlProcessorId() {
        mockStatic(HtmlProcessorFactory.class);
        expect(HtmlProcessorFactory.of(eq("custom-formatted")))
                .andReturn((HtmlProcessorFactory) () -> HtmlProcessorFactory.NOOP);


        initField("", "", "", "formatted", "custom-formatted");

        verifyAll();
    }
}
