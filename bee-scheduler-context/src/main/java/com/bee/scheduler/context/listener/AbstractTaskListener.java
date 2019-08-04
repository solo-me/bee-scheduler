package com.bee.scheduler.context.listener;

import com.bee.scheduler.context.TaskExecutionContextUtil;
import com.bee.scheduler.core.TaskExecutionContext;
import com.bee.scheduler.core.TaskExecutionResult;
import org.quartz.*;

/**
 * @author weiwei
 */
public abstract class AbstractTaskListener implements JobListener, TriggerListener {
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        taskToBeExecuted(TaskExecutionContextUtil.convert(context), context.getScheduler());
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        taskExecutionVetoed(TaskExecutionContextUtil.convert(context), context.getScheduler());
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        taskWasExecuted(TaskExecutionContextUtil.convert(context), TaskExecutionContextUtil.getTaskExecutionResult(context), context.getScheduler(), jobException);
    }


    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        taskTriggerFired(TaskExecutionContextUtil.convert(context), context.getScheduler());
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return vetoTaskExecution(TaskExecutionContextUtil.convert(context), context.getScheduler());
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        taskTriggerMisfired(trigger);
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {
        taskTriggerComplete(TaskExecutionContextUtil.convert(context), context.getScheduler(), triggerInstructionCode);
    }

    protected void taskToBeExecuted(TaskExecutionContext context, Scheduler scheduler) {

    }

    protected void taskExecutionVetoed(TaskExecutionContext context, Scheduler scheduler) {

    }

    protected void taskWasExecuted(TaskExecutionContext context, TaskExecutionResult result, Scheduler scheduler, JobExecutionException jobException) {

    }

    protected void taskTriggerFired(TaskExecutionContext context, Scheduler scheduler) {

    }

    protected boolean vetoTaskExecution(TaskExecutionContext context, Scheduler scheduler) {
        return false;
    }

    protected void taskTriggerMisfired(Trigger trigger) {

    }

    protected void taskTriggerComplete(TaskExecutionContext context, Scheduler scheduler, Trigger.CompletedExecutionInstruction triggerInstructionCode) {

    }

}
