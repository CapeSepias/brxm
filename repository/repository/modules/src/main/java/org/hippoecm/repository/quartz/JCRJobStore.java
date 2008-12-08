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
package org.hippoecm.repository.quartz;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.core.SchedulingContext;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;

public class JCRJobStore implements JobStore {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException {
        signaler.signalSchedulingChange();
    }

    public void setUseProperties(boolean flag) {
        // FIXME
    }
    public void setClusterCheckinInterval(long interval) {
        // FIXME
    }
    public void setIsClustered(boolean flag) {
        // FIXME
    }
    public void setMisfireThreshold(long threshold) {
        // FIXME
    }


    public void schedulerStarted() throws SchedulerException {
    }

    public void shutdown() {
    }

    public boolean supportsPersistence() {
        return true;
    }

    public void storeJobAndTrigger(SchedulingContext ctxt, JobDetail newJob, Trigger newTrigger)
      throws ObjectAlreadyExistsException, JobPersistenceException {
        if (SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        try {
            Node jobNode = ((JCRSchedulingContext)ctxt).getSession().getRootNode().getNode(newJob.getName().substring(1));
            jobNode.setProperty("hipposched:data",new ByteArrayInputStream(objectToBytes(newJob)));
            jobNode.setProperty("hippo:document",(String) newJob.getJobDataMap().get("document"));
            if(!jobNode.hasNode("hipposched:triggers")) {
                jobNode.addNode("hipposched:triggers","hipposched:triggers");
            }
            String triggerRelPath = newTrigger.getName().substring(newJob.getName().length()+1);
            Node triggerNode = jobNode.getNode("hipposched:triggers").addNode(triggerRelPath, "hipposched:trigger");
            Calendar cal = Calendar.getInstance();
            cal.setTime(newTrigger.getNextFireTime());
            triggerNode.setProperty("hipposched:nextFireTime", cal);
            triggerNode.setProperty("hipposched:data", new ByteArrayInputStream(objectToBytes(newTrigger)));
            jobNode.getParent().save();
        } catch (RepositoryException ex) {
            SchedulerModule.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("Failure storing job and trigger", ex);
        }
    }

    private byte[] objectToBytes(Object o) throws RepositoryException {
        try {
            ByteArrayOutputStream store = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(store);
            ostream.writeObject(o);
            ostream.flush();
            return store.toByteArray();
        } catch (IOException ex) {
            throw new ValueFormatException(ex);
        }
    }

    public void storeJob(SchedulingContext ctxt, JobDetail newJob, boolean replaceExisting)
      throws ObjectAlreadyExistsException, JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public boolean removeJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return false;
    }

    public JobDetail retrieveJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        try {
            Session session = getSession(ctxt);
            Node jobNode = session.getRootNode().getNode(jobName.substring(1));
            if(jobNode != null) {
                Object o = new ObjectInputStream(jobNode.getProperty("hipposched:data").getStream()).readObject();
                JobDetail job = (JobDetail) o;
                job.setName(jobNode.getPath());
                return job;
            }
        } catch(RepositoryException ex) {
            SchedulerModule.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("error while retrieving job", ex);
        } catch(IOException ex) {
            SchedulerModule.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("data format while retrieving job", ex);
        } catch(ClassNotFoundException ex) {
            SchedulerModule.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("cannot recreate job", ex);
        }
        return null;
    }

    public void storeTrigger(SchedulingContext ctxt, Trigger newTrigger, boolean replaceExisting)
      throws ObjectAlreadyExistsException, JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public boolean removeTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return false;
    }

    public boolean replaceTrigger(SchedulingContext ctxt, String triggerName, String groupName, Trigger newTrigger)
      throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return false;
    }

    public Trigger retrieveTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return null;
    }

    public void storeCalendar(SchedulingContext ctxt, String name, org.quartz.Calendar calendar, boolean replaceExisting,
                              boolean updateTriggers) throws ObjectAlreadyExistsException, JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public boolean removeCalendar(SchedulingContext ctxt, String calName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return false;
    }

    public org.quartz.Calendar retrieveCalendar(SchedulingContext ctxt, String calName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return null;
    }

    public int getNumberOfJobs(SchedulingContext ctxt) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return 0;
    }

    public int getNumberOfTriggers(SchedulingContext ctxt) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return 0;
    }

    public int getNumberOfCalendars(SchedulingContext ctxt) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return 0;
    }

    public String[] getJobNames(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return new String[0];
    }

    public String[] getTriggerNames(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return new String[0];
    }

    public String[] getJobGroupNames(SchedulingContext ctxt) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return new String[0];
    }

    public String[] getTriggerGroupNames(SchedulingContext ctxt) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return new String[0];
    }

    public String[] getCalendarNames(SchedulingContext ctxt) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return new String[0];
    }

    public Trigger[] getTriggersForJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return new Trigger[0];
    }

    public int getTriggerState(SchedulingContext ctxt, String triggerName, String triggerGroup) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return 0;
    }

    public void pauseTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public void pauseTriggerGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public void pauseJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public void pauseJobGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public void resumeTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public void resumeTriggerGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public Set getPausedTriggerGroups(SchedulingContext ctxt) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        return new HashSet();
    }

    public void resumeJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public void resumeJobGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public void pauseAll(SchedulingContext ctxt) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public void resumeAll(SchedulingContext ctxt) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public Trigger acquireNextTrigger(SchedulingContext ctxt, long noLaterThan) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("acquireNextTrigger({})",noLaterThan);
        }
        synchronized(this) { // FIXME
            try {
                Session session = getSession(ctxt);
                QueryManager qMgr = session.getWorkspace().getQueryManager();
                Query query = qMgr.createQuery("SELECT * FROM hipposched:trigger ORDER BY hipposched:nextFireTime", Query.SQL);
                QueryResult result = query.execute();
                Node triggerNode = null;
                for(NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
                    triggerNode = iter.nextNode();
                    if(triggerNode != null && triggerNode.hasProperty("hipposched:nextFireTime")) {
                        break;
                    } else {
                        triggerNode = null;
                    }
                }
                if(triggerNode != null) {
                    if(triggerNode.isNodeType("mix:versionable") && !triggerNode.isCheckedOut()) {
                        triggerNode.checkout();
                    }
                    Object o = new ObjectInputStream(triggerNode.getProperty("hipposched:data").getStream()).readObject();
                    Trigger trigger = (Trigger) o;
                    trigger.setName(triggerNode.getPath());
                    trigger.setJobName(triggerNode.getParent().getParent().getPath());
                    triggerNode.getProperty("hipposched:nextFireTime").remove();
                    triggerNode.save();
                    return (Trigger) o;
                }
            } catch(RepositoryException ex) {
                SchedulerModule.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new JobPersistenceException("error acquiring next trigger", ex);
            } catch(IOException ex) {
                SchedulerModule.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new JobPersistenceException("data format exception while acquiring next trigger", ex);
            } catch(ClassNotFoundException ex) {
                SchedulerModule.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new JobPersistenceException("cannot recreate trigger", ex);
            }
        }
        return null;
    }

    public void releaseAcquiredTrigger(SchedulingContext ctxt, Trigger trigger) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
    }

    public TriggerFiredBundle triggerFired(SchedulingContext ctxt, Trigger trigger) throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        try {
            Session session = getSession(ctxt);
            Node triggerNode = session.getRootNode().getNode(trigger.getName().substring(1));
            if(triggerNode.isNodeType("mix:versionable") && !triggerNode.isCheckedOut()) {
                triggerNode.checkout();
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(trigger.getNextFireTime());
            triggerNode.setProperty("hipposched:nextFireTime", cal);
            triggerNode.save();
            return new TriggerFiredBundle(retrieveJob(ctxt, trigger.getJobName(), trigger.getJobGroup()),
                                          trigger, null, false,
                                          trigger.getPreviousFireTime(), trigger.getPreviousFireTime(),
                                          trigger.getPreviousFireTime(), trigger.getNextFireTime());
        } catch(RepositoryException ex) {
            SchedulerModule.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("error while marking trigger fired", ex);
        }
    }

    public void triggeredJobComplete(SchedulingContext ctxt, Trigger trigger, JobDetail jobDetail, int triggerInstCode)
      throws JobPersistenceException {
        if(SchedulerModule.log.isDebugEnabled()) {
            SchedulerModule.log.trace("trace");
        }
        try {
            Session session = getSession(ctxt);
            String jobName = jobDetail.getName();
            Node jobNode = session.getRootNode().getNode(jobName.substring(1));
            if(jobNode != null) {
                Node handle = jobNode.getParent();
                if(handle.isNodeType("mix:versionable") && !handle.isCheckedOut()) {
                    handle.checkout();
                }
                jobNode.remove();
                handle.save();
            }
        } catch(PathNotFoundException ex) {
            // deliberate ignore
        } catch(RepositoryException ex) {
            SchedulerModule.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("error while marking job completed", ex);
        }
    }

    private Session getSession(SchedulingContext ctxt) throws RepositoryException {
        Session session;
        if(ctxt instanceof JCRSchedulingContext) {
            session = ((JCRSchedulingContext)ctxt).getSession();
        } else {
            session = SchedulerModule.session;
        }
        session.refresh(false);
        return session;
    }
}
