/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hippoecm.frontend.plugins.standards.behaviors;

import java.util.StringTokenizer;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBehaviorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserCheckPlugin extends AbstractBehavior implements IPlugin, IBehaviorService {
    private static final long serialVersionUID = 1L;

    static Logger log = LoggerFactory.getLogger(BrowserCheckPlugin.class);

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String SUPPORTED = "supported";

    IPluginConfig config;
    BrowserCheck check;

    public static class BrowserCheck implements IClusterable {
        private static final long serialVersionUID = 1L;

        Browser[] browsers;

        public BrowserCheck(String[] init) {
            browsers = new Browser[init.length];
            for (int i = 0; i < browsers.length; i++) {
                browsers[i] = new Browser(init[i]);
            }
        }

        public boolean isSupported(BrowserInfo info) {
            for (Browser browser : browsers) {
                if (browser.is(info)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Supported browsers can be configured like:
     * ie 6 <=, safari 4 <, chrome, firefox 3.5 <=, opera 
     */
    public static class Browser implements IClusterable {
        private static final long serialVersionUID = 1L;

        String agent;
        int majorVersion = -1;
        int minorVersion = -1;
        String modifier = "=";

        public Browser(String init) {
            StringTokenizer st = new StringTokenizer(init, " ");

            agent = st.nextToken();
            if (st.hasMoreTokens()) {
                String major = st.nextToken(), minor = null;
                int idx = major.indexOf(".");
                if (idx > -1) {
                    minor = major.substring(idx + 1);
                    major = major.substring(0, idx);
                }
                if (major != null) {
                    majorVersion = new Integer(major);
                }
                if (minor != null) {
                    minorVersion = new Integer(minor);
                }
            }

            if (st.hasMoreTokens()) {
                modifier = st.nextToken();
            }
        }

        public boolean is(BrowserInfo info) {
            if (!isAgent(info)) {
                return false;
            }
            return isVersion(info);
        }

        private boolean isVersion(BrowserInfo info) {
            if (majorVersion > -1 && !validVersion(majorVersion, info.getMajor())) {
                return false;
            }
            if (minorVersion > -1 && !validVersion(minorVersion, info.getMinor())) {
                return false;
            }
            return true;
        }

        private boolean validVersion(int configured, int provided) {
            if (modifier.equals("=")) {
                return configured == provided;
            } else if (modifier.equals("<")) {
                return provided < configured;
            } else if (modifier.equals("<=")) {
                return provided <= configured;
            } else if (modifier.equals(">")) {
                return provided > configured;
            } else if (modifier.equals(">=")) {
                return provided >= configured;
            }
            return false;
        }

        private boolean isAgent(BrowserInfo info) {
            if (agent.equals("ie")) {
                return info.isInternetExplorer();
            } else if (agent.equals("firefox")) {
                return info.isFirefox();
            } else if (agent.equals("safari")) {
                return info.isSafari();
            } else if (agent.equals("chrome")) {
                return info.isChrome();
            } else if (agent.equals("opera")) {
                return info.isOpera();
            }
            return false;
        }

    }

    public static interface BrowserInfo {

        boolean isOpera();

        boolean isChrome();

        boolean isSafari();

        boolean isFirefox();

        boolean isInternetExplorer();

        int getMajor();

        int getMinor();
    }

    public static class WicketBrowserInfo implements BrowserInfo {

        private static final String MSIE = "MSIE";
        private static final String CHROME = "Chrome";
        private static final String SHIRETOKO = "Shiretoko";
        private static final String FIREFOX = "Firefox";
        private WebClientInfo info;
        private int major;
        private int minor;

        public WicketBrowserInfo(WebClientInfo info) {
            this.info = info;

            major = info.getProperties().getBrowserVersionMajor();
            minor = info.getProperties().getBrowserVersionMinor();
            if (major == -1) {
                if (isFirefox()) {
                    if (info.getProperties().isBrowserMozillaFirefox()) {
                        setVersions(FIREFOX);
                    } else if (info.getUserAgent().indexOf(SHIRETOKO) > -1) {
                        setVersions(SHIRETOKO);
                    }
                } else if (isChrome()) {
                    setVersions(CHROME);
                } else if (isSafari() || isOpera()) {
                    setVersions("Version");
                } else if (isInternetExplorer()) {
                    String ua = info.getUserAgent();
                    if (ua.indexOf(MSIE) > 0) {
                        String v = ua.substring(ua.indexOf(MSIE));
                        parseMajorMinor(v.substring(5, v.indexOf(';')));
                    }
                }
            }
        }

        private void setVersions(String string) {
            String ua = info.getUserAgent();
            if (ua.indexOf(string) == -1) {
                return;
            }
            parseMajorMinor(ua.substring(ua.indexOf(string) + string.length() + 1));
        }

        private void parseMajorMinor(String parse) {
            try {
                StringTokenizer st = new StringTokenizer(parse.trim(), ". ");
                if (st.hasMoreTokens()) {
                    major = Integer.parseInt(st.nextToken());
                    if (st.hasMoreTokens()) {
                        minor = Integer.parseInt(st.nextToken());
                    }
                }
            } catch (NumberFormatException ex) {
                log.info("Could not parse " + parse + ": " + ex.getMessage());
            }
        }

        public boolean isChrome() {
            return info.getProperties().isBrowserSafari() && info.getUserAgent().indexOf(CHROME) > -1;
        }

        public boolean isFirefox() {
            return info.getProperties().isBrowserMozillaFirefox()
                    || (info.getProperties().isBrowserMozilla() && info.getUserAgent().indexOf(SHIRETOKO) > -1);
        }

        public boolean isInternetExplorer() {
            return info.getProperties().isBrowserInternetExplorer();
        }

        public boolean isOpera() {
            return info.getProperties().isBrowserOpera();
        }

        public boolean isSafari() {
            return info.getProperties().isBrowserSafari() && !isChrome();
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

    }

    public BrowserCheckPlugin(IPluginContext context, IPluginConfig config) {
        this.config = config;

        if (config.containsKey(SUPPORTED)) {
            check = new BrowserCheck(config.getStringArray(SUPPORTED));
        }
        context.registerService(this, config.getString(ID));
    }

    @Override
    public void bind(Component component) {
        super.bind(component);

        WebClientInfo info = (WebClientInfo) RequestCycle.get().getClientInfo();
        if (check != null && !check.isSupported(new WicketBrowserInfo(info))) {
            component.info(new StringResourceModel("browser.unsupported.warning", component, null).getString());
        }
    }

    public IBehavior getBehavior() {
        return this;
    }

    public String getComponentPath() {
        return config.getString(IBehaviorService.PATH);
    }

    public void start() {
    }

    public void stop() {
    }

}
