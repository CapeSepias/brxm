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
package org.hippoecm.frontend.plugins.xinha;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.plugin.IPluginContext;

class XinhaEditorBehavior extends AbstractHeaderContributor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected static final String SINGLE_QUOTE = "'";

    private IPluginContext context;
    
    private static final String BINARIES_PREFIX = "/binaries";
    //    private Set<XinhaPlugin.Configuration> configurations;

    Pattern numbers = Pattern.compile("\\d*");

    XinhaEditorBehavior(IPluginContext context) {
        this.context = context;
    }

    private String serialize2JS(String value) {
        if (value == null)
            return "null";
        else if (value.equalsIgnoreCase("true"))
            return "true";
        else if (value.equalsIgnoreCase("false"))
            return "false";
        else if (numbers.matcher(value).matches())
            return value;

        return SINGLE_QUOTE + value.replaceAll(SINGLE_QUOTE, "\\\\" + SINGLE_QUOTE) + SINGLE_QUOTE;
    }

    @Override
    public final IHeaderContributor[] getHeaderContributors() {
        return null;
    }

    IHeaderContributor[] getHeaderContributorsPartly() {

        final Page page = context.getService(Home.class.getName(), Home.class);
        final List<XinhaPlugin.Configuration> configurations = context.getServices(XinhaPlugin.Configuration.class
                .getName(), XinhaPlugin.Configuration.class);

        return new IHeaderContributor[] {

        new IHeaderContributor() {
            private static final long serialVersionUID = 1L;

            public void renderHead(IHeaderResponse response) {
                String xinhaEditorUrl = RequestCycle.get().getRequest().getRelativePathPrefixToContextRoot()
                        + "xinha/xinha/";

                StringBuffer sb = new StringBuffer();
                sb.append("_editor_url = '" + xinhaEditorUrl + "';\n");
                sb.append("_editor_lang = '" + page.getLocale().getLanguage() + "';\n");
                for (XinhaPlugin.Configuration config : configurations) {
                    if (config.getSkin() != null) {
                        sb.append("_editor_skin = '" + config.getSkin() + "';\n");
                        break;
                    }
                }
                for (XinhaPlugin.Configuration config : configurations) {
                    if(config.getJcrNodePath() != null) {
                        System.out.println(config.getJcrNodePath());
                        sb.append("_editor_jcrnode_url = '"+BINARIES_PREFIX + config.getJcrNodePath() + "';\n");
                        break;
                    }
                }
                
                response.renderJavascript(sb, null);
            }
        },

        HeaderContributor.forJavaScript("xinha/xinha/XinhaLoader.js"),

        new IHeaderContributor() {
            private static final long serialVersionUID = 1L;

            public void renderHead(IHeaderResponse response) {
                StringBuffer sb = new StringBuffer();

                //Declare/reset Xinha global variables
                sb.append("if( typeof xinha_editors == \"undefined\" ) { xinha_editors = []; }\n");
                sb.append("xinha_init    = null;\n");
                sb.append("xinha_config  = null;\n");
                sb.append("xinha_plugins = null;\n");

                //Create Xinha initialize function
                sb.append("xinha_init = xinha_init ? xinha_init : function()\n");
                sb.append("{\n");

                //Declare Xinha editors
                sb.append("  var new_editors = [];\n");
                for (XinhaPlugin.Configuration config : configurations) {
                    sb.append("  if(xinha_editors.").append(config.getName()).append(" == undefined) {\n");
                    sb.append("    new_editors.push('").append(config.getName()).append("');\n");
                    sb.append("  }\n");
                }

                //Declare and load Xinha plugins
                Set<XinhaPlugin.PluginConfiguration> plugins = new HashSet<XinhaPlugin.PluginConfiguration>();
                for (XinhaPlugin.Configuration config : configurations) {
                    for (XinhaPlugin.PluginConfiguration pluginConfig : config.getPluginConfigurations()) {
                        plugins.add(pluginConfig);
                    }
                }
                sb.append("  xinha_plugins = xinha_plugins ? xinha_plugins :\n");
                appendAsJSArray(sb, plugins);
                sb.append(";\n");
                sb.append("  if(!Xinha.loadPlugins(xinha_plugins, xinha_init)) return;\n");

                //Create global Xinha configuration
                sb.append("  xinha_config = xinha_config ? xinha_config() : new Xinha.Config();\n");

                //collect all stylesheets and add to global Xinha configuration
                boolean hasCss = false;
                for (XinhaPlugin.Configuration config : configurations) {
                    if (config.getStyleSheets() != null && config.getStyleSheets().size() > 0) {
                        if (!hasCss) {
                            sb.append("  xinha_config.pageStyleSheets = [");
                            hasCss = true;
                        }
                        for (String css : config.getStyleSheets()) {
                            //respect absolute urls
                            if (!css.startsWith("/") && !css.startsWith("http://"))
                                sb.append(" _editor_url + ");
                            sb.append("'" + css + "'");
                            sb.append(",");
                        }
                    }
                }
                if (hasCss) {
                    sb.deleteCharAt(sb.length() - 1);//remove last comma
                    sb.append("];\n");
                }

                //Instantiate Xinha editors and override editor specific (plugin)configuration values
                sb.append("  new_editors   = Xinha.makeEditors(new_editors, xinha_config);\n");
                for (XinhaPlugin.Configuration config : configurations) {
                    sb.append("  if(new_editors.").append(config.getName()).append(" != undefined) {\n");
                    sb.append("    new_editors.").append(config.getName()).append(".registerPlugins(");
                    appendAsJSArray(sb, config.getPluginConfigurations());
                    sb.append(");\n");

                    String prefix = "    new_editors." + config.getName() + ".config.";
                    appendProperties(sb, prefix, config.getProperties());

                    if (config.getToolbarItems() != null && config.getToolbarItems().size() > 0) {
                        sb.append(prefix).append("toolbar = [[");
                        for (Iterator<String> it = config.getToolbarItems().iterator(); it.hasNext();) {
                            sb.append(serialize2JS(it.next()));
                            if (it.hasNext())
                                sb.append(",");
                        }
                        sb.append("]];\n");
                    }
                    for (XinhaPlugin.PluginConfiguration pluginConfig : config.getPluginConfigurations()) {
                        appendProperties(sb, prefix + pluginConfig.getName() + ".", pluginConfig.getProperties());
                    }
                    sb.append("  }\n");
                }

                //Start editors
                sb.append("  Xinha.startEditors(new_editors);\n");

                //Update xinha_editors
                for (XinhaPlugin.Configuration config : configurations) {
                    sb.append("  if(new_editors.").append(config.getName()).append(" != undefined) {\n");
                    sb.append("    xinha_editors.").append(config.getName()).append(" = new_editors.").append(
                            config.getName()).append(";\n");
                    sb.append("  }\n");
                }

                // end xinha_init
                sb.append("};\n");

                //add Xinha initialize function to window.onLoad event
                //sb.append("Xinha._addEvent(window,'load', xinha_init);\n");
                response.renderJavascript(sb, null);
            }

            private void appendAsJSArray(StringBuffer sb, Set<? extends XinhaPlugin.BaseConfiguration> configs) {
                sb.append("  [\n");
                for (Iterator<? extends XinhaPlugin.BaseConfiguration> iter = configs.iterator(); iter.hasNext();) {
                    sb.append("    ");
                    sb.append(serialize2JS(iter.next().getName()));
                    if (iter.hasNext())
                        sb.append(",");
                    sb.append("\n");
                }
                sb.append("  ]");
            }

            private void appendProperties(StringBuffer sb, String prefix, Map<String, String> properties) {
                for (String key : properties.keySet()) {
                    sb.append(prefix);
                    sb.append(key).append(" = ").append(serialize2JS(properties.get(key)));
                    sb.append(";\n");
                }
            }
        },

        new IHeaderContributor() {
            private static final long serialVersionUID = 1L;

            public void renderHead(IHeaderResponse response) {
                response.renderOnLoadJavascript("xinha_init();");
            }
        }

        };
    }
}
