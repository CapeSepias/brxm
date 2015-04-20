/**
 * Copyright 2001-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.onehippo.forge.dashboard.documentwizard;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.CmsHeaderItem;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.widgets.AjaxDateTimeField;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.provider.IValueListProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewDocumentWizardPlugin extends RenderPlugin<Object> implements IHeaderContributor {

    private static final Logger log = LoggerFactory.getLogger(NewDocumentWizardPlugin.class);

    private static final String DEFAULT_LANGUAGE = "en";

    private static final ResourceReference WIZARD_CSS = new CssResourceReference(NewDocumentWizardPlugin.class, "NewDocumentWizardPlugin.css");

    private static final String PARAM_CLASSIFICATION_TYPE = "classificationType";
    private static final String PARAM_BASE_FOLDER = "baseFolder";
    private static final String PARAM_DOCUMENT_TYPE = "documentType";
    private static final String PARAM_QUERY = "query";
    private static final String PARAM_VALUELIST_PATH = "valueListPath";
    private static final String PARAM_SHORTCUT_LINK_LABEL = "shortcut-link-label";

    private static final String DEFAULT_QUERY = "new-document";
    private static final String DEFAULT_BASE_FOLDER = "/content/documents";
    private static final String DEFAULT_SERVICE_VALUELIST = "service.valuelist.default";

    private enum ClassificationType {DATE, LIST, LISTDATE}

    /**
     * This class creates a link on the dashboard. The link opens a dialog that allow the user to quickly create a
     * document. The location of the document can be configured.
     */
    public NewDocumentWizardPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AjaxLink<Object> link = new Link("link", context, config, this);
        add(link);

        final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
        Label labelComponent;

        if (localeConfig != null) {
            String labelText = localeConfig.getString(PARAM_SHORTCUT_LINK_LABEL, "Warning: label not found: " + PARAM_SHORTCUT_LINK_LABEL);
            labelComponent = new Label(PARAM_SHORTCUT_LINK_LABEL, Model.of(labelText));
        } else {
            labelComponent = new Label(PARAM_SHORTCUT_LINK_LABEL, new StringResourceModel(PARAM_SHORTCUT_LINK_LABEL, this, null));
        }

        link.add(labelComponent);

        final HippoIcon icon = HippoIcon.fromSprite("shortcut-icon", Icon.PLUS);
        link.add(icon);
    }

    /**
     * Adds the dialog css to its html header.
     */
    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(new CssReferenceHeaderItem(WIZARD_CSS, null, null, null) {
            @Override
            public Iterable<? extends HeaderItem> getDependencies() {
                return Collections.singleton(CmsHeaderItem.get());
            }
        });
    }

    private IPluginConfig getLocalizedPluginConfig(final IPluginConfig config) {
        Locale locale = getSession().getLocale();
        String localeString = getSession().getLocale().toString();
        IPluginConfig localeConfig = config.getPluginConfig(localeString);

        // just in case the locale contains others than language code, try to find it by language code again
        if (localeConfig == null && !StringUtils.equals(locale.getLanguage(), localeString)) {
            localeConfig = config.getPluginConfig(locale.getLanguage());
        }

        // if still not found, then try to find it by the default language again.
        if (localeConfig == null && !StringUtils.equals(DEFAULT_LANGUAGE, locale.getLanguage())) {
            localeConfig = config.getPluginConfig(DEFAULT_LANGUAGE);
        }

        return localeConfig;
    }

    /**
     * The link that opens a dialog window.
     */
    private class Link extends AjaxLink<Object> {

        private final IPluginContext context;
        private final IPluginConfig config;
        private final NewDocumentWizardPlugin parent;

        Link(final String id, final IPluginContext context, final IPluginConfig config, NewDocumentWizardPlugin parent) {
            super(id);
            this.context = context;
            this.config = config;
            this.parent = parent;
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            parent.getDialogService().show(new Dialog(context, config, parent));
        }
    }

    /**
     * The dialog that opens after the user has clicked the dashboard link.
     */
    protected class Dialog extends AbstractDialog<Object> {

        private static final String DIALOG_NAME_LABEL = "name-label";
        private static final String DIALOG_LIST_LABEL = "list-label";
        private static final String DIALOG_DATE_LABEL = "date-label";
        private static final String DIALOG_HOURS_LABEL = "hours-label";
        private static final String DIALOG_MINUTES_LABEL = "minutes-label";

        private final IPluginContext context;
        private final IPluginConfig config;
        private final Component parent;
        private final String documentType;
        private final String query;
        private final String baseFolder;
        private ClassificationType classificationType;

        private String documentName;
        private String list;
        private Date date;

        /**
         * @param context plugin context
         * @param config  plugin config
         * @param parent  parent component
         */
        public Dialog(final IPluginContext context, final IPluginConfig config, Component parent) {
            this.context = context;
            this.config = config;
            this.parent = parent;

            // get values from the shortcut configuration
            documentType = config.getString(PARAM_DOCUMENT_TYPE);
            if (StringUtils.isBlank(documentType)) {
                throw new IllegalArgumentException("Missing configuration parameter: " + PARAM_DOCUMENT_TYPE);
            }
            query = config.getString(PARAM_QUERY, DEFAULT_QUERY);
            baseFolder = config.getString(PARAM_BASE_FOLDER, DEFAULT_BASE_FOLDER);
            final String classification = config.getString(PARAM_CLASSIFICATION_TYPE);
            try {
                classificationType = ClassificationType.valueOf(StringUtils.upperCase(classification));
            } catch (Exception iae) {
                classificationType = ClassificationType.DATE;
            }

            feedback = new FeedbackPanel("feedback");
            replace(feedback);
            feedback.setOutputMarkupId(true);

            documentName = "";
            list = "";
            date = new Date();

            // get list value list
            IValueListProvider provider = getValueListProvider();
            ValueList categories;
            String valuelistPath = config.getString(PARAM_VALUELIST_PATH);
            try {
                categories = provider.getValueList(valuelistPath, null);
            } catch (IllegalStateException ise) {
                if (classificationType.equals(ClassificationType.LIST) || classificationType.equals(ClassificationType.LISTDATE)) {
                    log.warn("ValueList not found for parameter " + PARAM_VALUELIST_PATH + " with value " + valuelistPath);
                }
                categories = new ValueList();
            }

            // add name text field
            final Label nameLabel = getLabel(DIALOG_NAME_LABEL, config);
            add(nameLabel);
            IModel<String> nameModel = new PropertyModel<>(this, "documentName");
            TextField<String> nameField = new TextField<>("name", nameModel);
            nameField.setRequired(true);
            nameField.add(strValue -> {
                String value = strValue.getValue();
                if (!isValidName(value)) {
                    strValue.error(messageSource -> new StringResourceModel("invalid.name", this, null).getString());
                }
            });
            nameField.setLabel(new StringResourceModel(DIALOG_NAME_LABEL, this, null));
            add(nameField);

            // add list dropdown field
            Label listLabel = getLabel(DIALOG_LIST_LABEL, config);
            add(listLabel);

            final PropertyModel<Object> propModel = new PropertyModel<>(this, "list");
            final IChoiceRenderer<Object> choiceRenderer = new ListChoiceRenderer(categories);
            DropDownChoice<Object> listField = new DropDownChoice<>("list", propModel, categories, choiceRenderer);
            listField.setRequired(true);
            listField.setLabel(new StringResourceModel(DIALOG_LIST_LABEL, this, null));
            add(listField);

            if (!classificationType.equals(ClassificationType.LIST) && !classificationType.equals(ClassificationType.LISTDATE)) {
                listLabel.setVisible(false);
                listField.setVisible(false);
            }

            // add date field
            final Label dateLabel = getLabel(DIALOG_DATE_LABEL, config);
            AjaxDateTimeField dateField = new AjaxDateTimeField("date", new PropertyModel<>(this, "date"), true);
            dateField.setRequired(true);
            final StringResourceModel dateLabelModel = new StringResourceModel(DIALOG_DATE_LABEL, this, null);
            dateField.setLabel(dateLabelModel);
            ((DateTextField)dateField.get("date")).setLabel(dateLabelModel);
            ((TextField)dateField.get("hours")).setLabel(new StringResourceModel(DIALOG_HOURS_LABEL, this, null));
            ((TextField)dateField.get("minutes")).setLabel(new StringResourceModel(DIALOG_MINUTES_LABEL, this, null));
            add(dateLabel);
            add(dateField);
            if (!classificationType.equals(ClassificationType.DATE) && !classificationType.equals(ClassificationType.LISTDATE)) {
                dateLabel.setVisible(false);
                dateField.setVisible(false);
            }

            add(new IFormValidator() {

                private static final String ERROR_SNS_NODE_EXISTS = "error-sns-node-exists";
                private static final String ERROR_LOCALIZED_NAME_EXISTS = "error-localized-name-exists";
                private static final String ERROR_VALIDATION_NAMES = "error-validation-names";

                @Override
                public FormComponent<?>[] getDependentFormComponents() {
                    return new FormComponent<?>[]{nameField};
                }

                @Override
                public void validate(final Form<?> form) {
                    try {
                        // get values from components directly during validation phase
                        String list = Strings.unescapeMarkup(listField.getValue()).toString();
                        Date date = dateField.getDate();
                        HippoNode folder = getFolder(list, date, false);
                        if (folder == null) {
                            return;
                        }

                        String newNodeName = getNodeNameCodec().encode(nameField.getValue());
                        String newLocalizedName = nameField.getValue();

                        if (folder.hasNode(newNodeName)) {
                            showError(form, ERROR_SNS_NODE_EXISTS, newNodeName);
                        }
                        if (existedLocalizedName(folder, newLocalizedName)) {
                            showError(form, ERROR_LOCALIZED_NAME_EXISTS, newLocalizedName);
                        }
                    } catch (RepositoryException | RemoteException | WorkflowException e) {
                        log.error("validation error", e);
                        showError(form, ERROR_VALIDATION_NAMES);
                    }
                }

                private void showError(final Form<?> form, final String messge, final Object... arguments) {
                    form.error(new StringResourceModel(messge, Dialog.this, null, arguments).getObject());
                }

                protected boolean existedLocalizedName(final Node parentNode, final String localizedName) throws RepositoryException {
                    final NodeIterator children = parentNode.getNodes();
                    while (children.hasNext()) {
                        Node child = children.nextNode();
                        if (child.isNodeType(HippoStdNodeType.NT_FOLDER) || child.isNodeType(HippoNodeType.NT_HANDLE)) {
                            NodeTranslator nodeTranslator = new NodeTranslator(new JcrNodeModel(child));
                            String localizedChildName = nodeTranslator.getNodeName().getObject();
                            if (StringUtils.equals(localizedChildName, localizedName)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
        }

        /**
         * Get a label from the plugin config, or from the Dialog properties file.
         *
         * @param labelKey the key under which the label is stored
         * @param config   the config of the plugin
         * @return a wicket Label
         */
        private Label getLabel(final String labelKey, final IPluginConfig config) {
            final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
            if (localeConfig != null) {
                final String label = localeConfig.getString(labelKey);
                if (StringUtils.isNotBlank(label)) {
                    return new Label(labelKey, label);
                }
            }
            return new Label(labelKey, new StringResourceModel(labelKey, this, null));
        }

        /**
         * Gets the dialog title from the config.
         *
         * @return the label, or a warning if not found.
         */
        public IModel<String> getTitle() {
            final IPluginConfig localeConfig = getLocalizedPluginConfig(config);
            if (localeConfig != null) {
                String label = localeConfig.getString(PARAM_SHORTCUT_LINK_LABEL);
                if (StringUtils.isNotBlank(label)) {
                    return Model.of(label);
                }
            }
            return new StringResourceModel(PARAM_SHORTCUT_LINK_LABEL, this, null);
        }

        @Override
        public IValueMap getProperties() {
            return DialogConstants.MEDIUM;
        }

        @Override
        protected void onOk() {
            Session session = getSession().getJcrSession();
            HippoWorkspace workspace = (HippoWorkspace)session.getWorkspace();
            try {
                // get the folder node
                HippoNode folder = getFolder(list, date, true);

                WorkflowManager workflowMgr = workspace.getWorkflowManager();

                // get the folder node's workflow
                Workflow workflow = workflowMgr.getWorkflow("internal", folder);

                if (workflow instanceof FolderWorkflow) {
                    FolderWorkflow fw = (FolderWorkflow)workflow;

                    String encodedDocumentName = getNodeNameCodec().encode(documentName);
                    // create the new document
                    Map<String, String> arguments = new TreeMap<>();
                    arguments.put("name", encodedDocumentName);
                    if (classificationType.equals(ClassificationType.LIST) || classificationType.equals(ClassificationType.LISTDATE)) {
                        arguments.put("list", list);
                        log.debug("Create document using for $list: " + list);
                    }
                    if (classificationType.equals(ClassificationType.DATE) || classificationType.equals(ClassificationType.LISTDATE)) {
                        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'kk:mm:ss.SSSZZ");
                        arguments.put("date", fmt.print(date.getTime()));
                        log.debug("Create document using for $date: " + fmt.print(date.getTime()));
                    }
                    log.debug("Query used: " + query);
                    String path = fw.add(query, documentType, arguments);

                    JcrNodeModel nodeModel = new JcrNodeModel(path);
                    select(nodeModel);

                    // add the not-encoded document name as translation
                    if (!documentName.equals(encodedDocumentName)) {
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow)workflowMgr.getWorkflow("core", nodeModel.getNode());
                        if (defaultWorkflow != null) {
                            defaultWorkflow.localizeName(documentName);
                        }
                    }
                }
            } catch (RepositoryException | RemoteException | WorkflowException e) {
                log.error("Error occurred while creating new document: "
                        + e.getClass().getName() + ": " + e.getMessage());
            }
        }

        private HippoNode getFolder(String list, Date date, final boolean create) throws RepositoryException, RemoteException, WorkflowException {
            Session session = getSession().getJcrSession();
            HippoNode folder = (HippoNode)session.getItem(baseFolder);

            if (classificationType.equals(ClassificationType.LIST) || classificationType.equals(ClassificationType.LISTDATE)) {
                folder = getListFolder(folder, list, create);
            }

            if (folder == null) {
                return null;
            }

            if (classificationType.equals(ClassificationType.DATE) || classificationType.equals(ClassificationType.LISTDATE)) {
                folder = getDateFolder(folder, date, create);
            }
            return folder;
        }

        @Override
        protected void onValidate() {
            super.onValidate();
        }

        private IPluginContext getPluginContext() {
            return context;
        }

        private IValueListProvider getValueListProvider() {
            return getPluginContext().getService(DEFAULT_SERVICE_VALUELIST, IValueListProvider.class);
        }

        protected void select(JcrNodeModel nodeModel) {
            String browserId = config.getString(IBrowseService.BROWSER_ID);
            @SuppressWarnings("unchecked")
            IBrowseService<JcrNodeModel> browser = getPluginContext().getService(browserId, IBrowseService.class);
            if (browser != null) {
                browser.browse(nodeModel);
            } else {
                log.warn("no browser service found");
            }
        }

    }

    /**
     * Determine whether the a document name is valid.
     *
     * @param value the document name
     * @return whether the name is a valid JCR nodename
     */
    protected static boolean isValidName(final String value) {
        if (!value.trim().equals(value)) {
            return false;
        }
        if (".".equals(value) || "..".equals(value)) {
            return false;
        }
        return value.matches("[^\\[\\]\\|/:\\}\\{]+");
    }

    protected StringCodec getNodeNameCodec() {
        return new StringCodecFactory.UriEncoding();
    }


    /**
     * Get or create folder for classificationType.LIST.
     *
     * @param parent
     * @param list
     * @return
     * @throws java.rmi.RemoteException
     * @throws javax.jcr.RepositoryException
     * @throws org.hippoecm.repository.api.WorkflowException
     */
    protected HippoNode getListFolder(HippoNode parent, String list, boolean create) throws RemoteException, RepositoryException, WorkflowException {
        String listEncoded = getNodeNameCodec().encode(list);
        HippoNode resultParent = parent;
        if (resultParent.hasNode(listEncoded)) {
            resultParent = (HippoNode)resultParent.getNode(listEncoded);
        } else {
            if (create) {
                resultParent = createFolder(resultParent, listEncoded);
            } else {
                return null;
            }
        }
        return resultParent;
    }

    /**
     * Get or create folder(s) for classificationType.DATE.
     *
     * @param parent
     * @param date
     * @return
     * @throws java.rmi.RemoteException
     * @throws javax.jcr.RepositoryException
     * @throws org.hippoecm.repository.api.WorkflowException
     */
    protected HippoNode getDateFolder(HippoNode parent, Date date, boolean create) throws RemoteException, RepositoryException, WorkflowException {
        String year = new SimpleDateFormat("yyyy").format(date);
        HippoNode resultParent = parent;
        if (resultParent.hasNode(year)) {
            resultParent = (HippoNode)resultParent.getNode(year);
        } else {
            if (create) {
                resultParent = createFolder(resultParent, year);
            } else {
                return null;
            }
        }

        String month = new SimpleDateFormat("MM").format(date);
        if (resultParent.hasNode(month)) {
            resultParent = (HippoNode)resultParent.getNode(month);
        } else {
            if (create) {
                resultParent = createFolder(resultParent, month);
            } else {
                return null;
            }
        }

        return resultParent;
    }

    protected HippoNode createFolder(HippoNode parentNode, String name) throws RepositoryException, RemoteException, WorkflowException {
        Session session = getSession().getJcrSession();
        HippoWorkspace workspace = (HippoWorkspace)session.getWorkspace();
        WorkflowManager workflowMgr = workspace.getWorkflowManager();

        // get the folder node's workflow
        Workflow workflow = workflowMgr.getWorkflow("internal", parentNode);

        if (workflow instanceof FolderWorkflow) {
            FolderWorkflow fw = (FolderWorkflow)workflow;

            // create the new folder
            String category = "new-folder";
            NodeType[] mixinNodeTypes = parentNode.getMixinNodeTypes();
            for (NodeType mixinNodeType : mixinNodeTypes) {
                if (mixinNodeType.getName().equals("hippotranslation:translated")) {
                    category = "new-translated-folder";
                    break;
                }
            }
            fw.add(category, "hippostd:folder", name);

            HippoNode newFolder = (HippoNode)parentNode.getNode(name);

            // give the new folder the same folder types as its parent
            Property parentFolderType = parentNode.getProperty("hippostd:foldertype");
            newFolder.setProperty("hippostd:foldertype", parentFolderType.getValues());

            // try to reorder the folder
            reorderFolder(fw, parentNode);

            return newFolder;
        } else {
            throw new WorkflowException("Workflow is not an instance of FolderWorkflow");
        }

    }

    @SuppressWarnings("unused")
    protected void reorderFolder(final FolderWorkflow folderWorkflow, final HippoNode parentNode) {
        // intentional stub
    }

    private static class ListChoiceRenderer implements IChoiceRenderer<Object> {
        private final ValueList list;

        public ListChoiceRenderer(ValueList list) {
            this.list = list;
        }

        public Object getDisplayValue(Object object) {
            return list.getLabel(object);
        }

        public String getIdValue(Object object, int index) {
            return list.getKey(object);
        }

    }

}
