package org.onehippo.cms7.channelmanager;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlElement;

public class ExtTest extends AbstractJavascriptTest {

    @Test
    public void runBasicExtTest() throws Exception {
        setUp("extjs-test.html");

        // TODO: replace by retry loop
        Thread.sleep(1000);

//        System.out.print(page.asXml());

        final HtmlElement result = page.getElementById("result");
        assertNotNull(result);
        assertTrue(result.getTextContent().contains("pass"));
    }

    @Test
    public void runPageEditorTest() throws Exception {
        setUp("pageeditor-test.html");

        // TODO: replace by retry loop
        Thread.sleep(1000);

        // System.out.print(page.asXml());

        final HtmlElement result = page.getElementById("result");
        assertNotNull(result);

        assertTrue(result.getTextContent().contains("pass"));

        final HtmlElement instance = page.getElementById("Hippo.ChannelManager.TemplateComposer.Instance");
        assertNotNull(instance);
    }

}
