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
package org.hippoecm.hst.core.sitemenu;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class EditableMenuItemImpl extends AbstractMenuItem implements EditableMenuItem {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EditableMenuItemImpl.class);
    
    private EditableMenu editableMenu;
    private List<EditableMenuItem> childMenuItems = new ArrayList<EditableMenuItem>();;
    private EditableMenuItem parentItem;
 
    /*
     * For classes extending this EditableMenuItemImpl. These classes need to have some init method
     * where the instance variables are set
     */ 
    
    public EditableMenuItemImpl(EditableMenuItem parentItem){
        if(parentItem == null) {
            log.error("Cannot add a Editable menu item when parent item is null");
            return;
        }
        this.parentItem = parentItem;
        this.editableMenu = parentItem.getEditableMenu();
    }
    
    public EditableMenuItemImpl(EditableMenu editableMenu, EditableMenuItem parentItem , HstSiteMenuItem siteMenuItem) {
       
       this.editableMenu = editableMenu;
       this.parentItem = parentItem;
       this.name = siteMenuItem.getName();
       this.depth = siteMenuItem.getDepth();
       this.repositoryBased = siteMenuItem.isRepositoryBased();
       this.properties = siteMenuItem.getProperties();
       this.hstLink = siteMenuItem.getHstLink();
       this.externalLink = siteMenuItem.getExternalLink();
       this.expanded = siteMenuItem.isExpanded();
       this.selected = siteMenuItem.isSelected();
       
       for(HstSiteMenuItem childMenuItem : siteMenuItem.getChildMenuItems()) {
           childMenuItems.add(new EditableMenuItemImpl(this.editableMenu, this, childMenuItem));
       }
    }

    public List<EditableMenuItem> getChildMenuItems() {
        return this.childMenuItems;
    }
    
    public void addChildMenuItem(EditableMenuItem childMenuItem) {
        this.childMenuItems.add(childMenuItem);
        if(childMenuItem.isSelected() || childMenuItem.isExpanded()) {
            this.setExpanded(true);
        }
        if(childMenuItem.isSelected()) {
            this.editableMenu.setSelectedMenuItem(childMenuItem);
        }
    }

    public EditableMenu getEditableMenu() {
        return this.editableMenu;
    }
    
    public EditableMenuItem getParentItem() {
        return this.parentItem;
    }
    

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if(this.parentItem != null) {
            this.parentItem.setExpanded(expanded);
        }
    }
    
}
