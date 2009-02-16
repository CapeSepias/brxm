package org.hippoecm.hst.configuration;

import java.util.Iterator;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.SiteMapMatcher;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapMatcher.MatchResult;

public class ConfigurationViewUtilities {

    public static final String SMALL_INDENT = "  ";
    public static final String INDENT = "\t";
    
    public static final void view(StringBuffer buf, HstSites sites) {
        view(buf, "", sites); 
    }
    
    public static final void view(StringBuffer buf, String indent,  HstSites sites) {
        buf.append("\n\n***** HstSites ("+sites.hashCode()+") *******");
        buf.append("\n").append(indent).append("-Content path: ").append(sites.getSitesContentPath());
        buf.append("\n").append(indent).append("+Sites: ");
        for(Iterator<HstSite> it = sites.getSites().values().iterator(); it.hasNext();) {
            view(buf,indent+SMALL_INDENT, it.next());
        }
        buf.append("\n\n***** End HstSites *******");
    }

    
    public static final void view(StringBuffer buf, HstSite site) {
        view(buf, "", site);
    }
    
    public static final void view(StringBuffer buf, String indent, HstSite site) {
        if(site == null) {
            buf.append("\n").append(indent).append("+HstSite: null");
            return;
        }
        buf.append("\n").append(indent).append("+Site: ").append(site.getName()).append(" (").append(site.hashCode()).append(")");
        indent = indent + SMALL_INDENT;
        buf.append("\n").append(indent).append("-Content Path: ").append(site.getContentPath());
       
        view(buf, indent+SMALL_INDENT, site.getSiteMap()) ;
        
        view(buf, indent+SMALL_INDENT, site.getComponentsConfiguration()) ;
    }
    

    public static final void view(StringBuffer buf,  HstSiteMap hstSiteMap) {
        view(buf, "", hstSiteMap);
    }
    
    public static final void view(StringBuffer buf, String indent,  HstSiteMap hstSiteMap) {
        if(hstSiteMap == null) {
            buf.append("\n").append(indent).append("+HstSiteMap: null");
            return;
        }
        buf.append("\n").append(indent).append("+HstSiteMap: (").append(hstSiteMap.hashCode()).append(")");
        for(Iterator<HstSiteMapItem> it = hstSiteMap.getSiteMapItems().iterator(); it.hasNext();) {
            view(buf, indent+SMALL_INDENT, it.next());
        }
    }
    
    public static final void view(StringBuffer buf,  HstSiteMapItem hstSiteMapItem) {
        view(buf, "", hstSiteMapItem);
    }
    
    public static final void view(StringBuffer buf, String indent,  HstSiteMapItem hstSiteMapItem) {
        if(hstSiteMapItem == null) {
            buf.append("\n").append(indent).append("+HstSiteMapItem: null");
            return;
        }
        buf.append("\n").append(indent).append("+HstSiteMapItem: (").append(hstSiteMapItem.hashCode()).append(")");
        String newLine = "\n" + indent + SMALL_INDENT + "-";
        buf.append(newLine).append("id = ").append(hstSiteMapItem.getId());
        buf.append(newLine).append("value = ").append(hstSiteMapItem.getValue());
        buf.append(newLine).append("path = ").append(hstSiteMapItem.getPath());
        buf.append(newLine).append("relativecontentpath = ").append(hstSiteMapItem.getRelativeContentPath());
        buf.append(newLine).append("componentconfigurationid = ").append(hstSiteMapItem.getComponentConfigurationId());
        buf.append(newLine).append("iswildcard = ").append(hstSiteMapItem.isWildCard());
        for(Iterator<HstSiteMapItem> it = hstSiteMapItem.getChildren().iterator(); it.hasNext();) {
            view(buf, indent+SMALL_INDENT, it.next());
        }
    }
    
    public static final void view(StringBuffer buf,  HstComponentsConfiguration hstComponentsConfiguration) {
        view(buf, "", hstComponentsConfiguration);
    }
    
    public static final void view(StringBuffer buf, String indent,  HstComponentsConfiguration hstComponentsConfiguration) {
        if(hstComponentsConfiguration == null) {
            buf.append("\n").append(indent).append("+HstComponentsConfiguration: null");
            return;
        }
        buf.append("\n").append(indent).append("+HstComponentsConfiguration: (").append(hstComponentsConfiguration.hashCode()).append(")");
        
        for(Iterator<HstComponentConfiguration> it = hstComponentsConfiguration.getComponentConfigurations().values().iterator(); it.hasNext();) {
            view(buf, indent+SMALL_INDENT, it.next());
        }
        
    }
    
    public static final void view(StringBuffer buf,  HstComponentConfiguration hstComponentConfiguration) {
        view(buf, "", hstComponentConfiguration);
    }
    
    public static final void view(StringBuffer buf, String indent,  HstComponentConfiguration hstComponentConfiguration) {
        if(hstComponentConfiguration == null) {
            buf.append("\n").append(indent).append("+HstComponentConfiguration: null");
            return;
        }
        buf.append("\n").append(indent).append("+HstComponentConfiguration: (").append(hstComponentConfiguration.hashCode()).append(")");
        String newLine = "\n" + indent + SMALL_INDENT + "-";
        buf.append(newLine).append("id = ").append(hstComponentConfiguration.getId());
        buf.append(newLine).append("referencename = ").append(hstComponentConfiguration.getReferenceName());
        buf.append(newLine).append("componentclassname = ").append(hstComponentConfiguration.getComponentClassName());
        buf.append(newLine).append("renderpath = ").append(hstComponentConfiguration.getRenderPath());
        buf.append(newLine).append("compontentcontentbasepath = ").append(hstComponentConfiguration.getComponentContentBasePath());
        buf.append(newLine).append("contextrelativepath = ").append(hstComponentConfiguration.getContextRelativePath());
        for(Iterator<HstComponentConfiguration> it = hstComponentConfiguration.getChildren().values().iterator(); it.hasNext();) {
            view(buf, indent+SMALL_INDENT, it.next());
        }
    }
    
    public static final void view(StringBuffer buf, MatchResult matchResult) {
       view(buf, "", matchResult);
    }
    
    public static final void view(StringBuffer buf,String indent, MatchResult matchResult) {
        buf.append("\n\n***** SiteMapMatcher ("+matchResult.hashCode()+") *******");
        indent = indent + SMALL_INDENT;
        
        buf.append("\n").append(indent).append("-Remainder = ").append(matchResult.getRemainder());
        view(buf,indent,matchResult.getSiteMapItem());
        view(buf,indent,matchResult.getCompontentConfiguration());
        
        buf.append("\n\n***** End SiteMapMatcher *******");
    }

}
