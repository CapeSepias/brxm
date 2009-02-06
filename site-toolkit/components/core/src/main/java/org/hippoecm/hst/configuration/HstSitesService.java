package org.hippoecm.hst.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSitesService extends AbstractJCRService implements HstSites, Service{

    private Logger log = LoggerFactory.getLogger(HstSites.class) ;
    private Map<String, HstSite> hstSites = new HashMap<String, HstSite>();
    
    private String sitesContentPath;
    private Map<String, HstSiteService> sites;
    
    public HstSitesService(Node node) throws ServiceException {
        super(node);
        try {
            if(node.isNodeType(Configuration.NODETYPE_HST_SITES)) {
                this.sitesContentPath = node.getPath();
                init(node);
            } 
            else {
                throw new ServiceException("Cannot instantiate a HstSites object for a node of type '"+node.getPrimaryNodeType().getName()+"' for node '"+node.getPath()+"'");
            }
        } catch (RepositoryException e) {
            throw new ServiceException("Repository Exception while creating HstSites object: " + e);
        }
    }
    
    public Service[] getChildServices() {
        return sites.values().toArray(new HstSiteService[sites.size()]);
    }
    
    private void init(Node node) throws RepositoryException {
       QueryManager qryMng = node.getSession().getWorkspace().getQueryManager();
       String query = "/jcr:root" + node.getPath()+"//element(*,"+Configuration.NODETYPE_HST_SITE+")";
       QueryResult result = qryMng.createQuery(query, "xpath").execute();
       for(NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext();) {
           Node site = nodeIt.nextNode();
           try {
               HstSite hstSite = new HstSiteService(site, this);
               hstSites.put(hstSite.getName(), hstSite);
           } catch (ServiceException e){
               log.warn("Skipping subsite: ",e.getMessage());
           }
       }
    }

    public HstSite getSite(String name) {
        return getSites().get(name);
    }

    public Map<String, HstSite> getSites() {
        return hstSites;
    }

    public String getSitesContentPath() {
        return this.sitesContentPath;
    }
 
    
}
