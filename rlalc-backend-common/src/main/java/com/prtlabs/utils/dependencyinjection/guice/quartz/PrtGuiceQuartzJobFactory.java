package com.prtlabs.utils.dependencyinjection.guice.quartz;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Job;


/**
 * Needed when Quartz Jobs need to be "injected" by Guice
 */
public class PrtGuiceQuartzJobFactory implements JobFactory {

    @Inject
    private Injector injector;

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        return (Job)injector.getInstance(bundle.getJobDetail().getJobClass());
    }

}