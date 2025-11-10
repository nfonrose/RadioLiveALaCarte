package com.prtlabs.utils.dependencyinjection.hk2.quartz;

import jakarta.inject.Inject;
import org.glassfish.hk2.api.ServiceLocator;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * Needed when Quartz Jobs need to be "injected" by HK2
 */
public class PrtHK2QuartzJobFactory implements JobFactory {

    @Inject
    private ServiceLocator serviceLocator;

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        return (Job)serviceLocator.getService(bundle.getJobDetail().getJobClass());
    }
}