package com.bee.scheduler.context;

import com.bee.scheduler.context.listener.*;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author weiwei.
 */
public class CustomizedQuartzSchedulerFactoryBean extends SchedulerFactoryBean implements EnvironmentAware {
    private static final long CLUSTER_CHECKIN_INTERVAL = 5000;
    private static final long MISFIRE_THRESHOLD = 5000;
    private static final long BATCH_TRIGGER_ACQUISITION_FIRE_AHEAD_TIME_WINDOW = 5000;
    private String instanceId;
    private boolean clusterMode = false;
    private int threadPoolSize = 10;
    private List<TaskListenerSupport> taskListenerList = new ArrayList<>();
    private Environment environment;

    public CustomizedQuartzSchedulerFactoryBean(String name, String instanceId, DataSource dataSource) {
        this.instanceId = instanceId;
        this.setDataSource(dataSource);
        this.setSchedulerName(name);
        this.setDataSource(dataSource);
//        this.setAutoStartup(false);
    }

    public CustomizedQuartzSchedulerFactoryBean(String name, DataSource dataSource) {
        this(name, null, dataSource);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Properties quartzProperties = new Properties();
        //common props
        quartzProperties.setProperty("org.quartz.jobStore.tablePrefix", "BS_");
        quartzProperties.setProperty("org.quartz.jobStore.useProperties", "true");
        quartzProperties.setProperty("org.quartz.jobStore.misfireThreshold", String.valueOf(MISFIRE_THRESHOLD));
        quartzProperties.setProperty("org.quartz.threadPool.threadCount", String.valueOf(threadPoolSize));
        quartzProperties.setProperty("org.quartz.jobStore.acquireTriggersWithinLock", "true");
        quartzProperties.setProperty("org.quartz.scheduler.batchTriggerAcquisitionMaxCount", String.valueOf(threadPoolSize));
        quartzProperties.setProperty("org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow", String.valueOf(BATCH_TRIGGER_ACQUISITION_FIRE_AHEAD_TIME_WINDOW));
        quartzProperties.setProperty("org.quartz.plugin.JobInterruptMonitorPlugin.class", "org.quartz.plugins.interrupt.JobInterruptMonitorPlugin");
        quartzProperties.setProperty("org.quartz.plugin.JobInterruptMonitorPlugin.defaultMaxRunTime", "300000");
        //switch data source delegate
        String dsp = environment.getProperty("spring.datasource.platform");
        if ("mysql".equals(dsp)) {
            quartzProperties.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        } else if ("postgresql".equals(dsp)) {
            quartzProperties.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        }
        //how to generate instance id
        if (this.instanceId == null) {
            quartzProperties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        } else {
            quartzProperties.setProperty("org.quartz.scheduler.instanceId", instanceId);
        }
        //is cluster mode enabled
        if (clusterMode) {
            quartzProperties.setProperty("org.quartz.jobStore.isClustered", "true");
            quartzProperties.setProperty("org.quartz.jobStore.clusterCheckinInterval", String.valueOf(CLUSTER_CHECKIN_INTERVAL));
        }
        this.setQuartzProperties(quartzProperties);
        super.afterPropertiesSet();
    }

    public void addListener(TaskListenerSupport listener) {
        this.taskListenerList.add(listener);
    }

    @Override
    protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName) throws SchedulerException {
        Scheduler scheduler = super.createScheduler(schedulerFactory, schedulerName);
        taskListenerList.add(new BindingTaskLoggerOnThreadLocalListener());
        taskListenerList.add(new VetoDangerousTaskListener());
        taskListenerList.add(new TaskLinkageHandleListener());
        taskListenerList.add(new TaskHistoryListener());
        ListenerManager listenerManager = scheduler.getListenerManager();
        for (TaskListenerSupport listener : taskListenerList) {
            listenerManager.addJobListener(listener);
            listenerManager.addTriggerListener(listener);
        }
        return scheduler;
    }

    public boolean isClusterMode() {
        return clusterMode;
    }

    public void setClusterMode(boolean clusterMode) {
        this.clusterMode = clusterMode;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
