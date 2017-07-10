package com.bee.scheduler.admin.web;

import com.alibaba.fastjson.JSON;
import com.bee.scheduler.admin.model.*;
import com.bee.scheduler.core.Constants;
import com.bee.scheduler.admin.core.RamStore;
import com.bee.scheduler.core.job.JobComponent;
import com.bee.scheduler.admin.exception.BizzException;
import com.bee.scheduler.admin.model.TaskConfig.ScheduleTypeCalendarIntervalOptions;
import com.bee.scheduler.admin.model.TaskConfig.ScheduleTypeCronOptions;
import com.bee.scheduler.admin.model.TaskConfig.ScheduleTypeDailyTimeIntervalOptions;
import com.bee.scheduler.admin.model.TaskConfig.ScheduleTypeSimpleOptions;
import com.bee.scheduler.admin.service.TaskService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.spi.OperableTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.Calendar;
import java.util.regex.Pattern;

import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author weiwei
 */
@Controller
public class TaskController {
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TaskService taskService;

    @ResponseBody
    @GetMapping("/task/groups")
    public HttpResponseBodyWrapper taskGroups() throws Exception {
        Map<String, Object> model = new HashMap<>();
        return new HttpResponseBodyWrapper(scheduler.getTriggerGroupNames());
    }

    @ResponseBody
    @GetMapping("/task/list")
    public HttpResponseBodyWrapper task(String state, String taskName, String taskGroup, Integer page) throws Exception {
        state = StringUtils.trimToNull(state);
        taskName = StringUtils.trimToNull(taskName);
        taskGroup = StringUtils.trimToNull(taskGroup);
        page = page == null ? 1 : page;

        Pageable<Task> queryResult = taskService.queryTask(scheduler.getSchedulerName(), taskName, taskGroup, state, page);
        return new HttpResponseBodyWrapper(queryResult);
    }

    @ResponseBody
    @GetMapping("/task/trends")
    public HttpResponseBodyWrapper trends() throws Exception {
        HashMap<String, Object> data = new HashMap<>();

        String schedulerName = scheduler.getSchedulerName();
        int taskTotalCount = taskService.queryTaskCount(schedulerName, null, null, null);
        List<ExecutingTask> executingTaskList = taskService.queryExcutingTask(schedulerName);

        Pageable<ExecutedTask> taskHistoryList = taskService.queryTaskHistory(schedulerName, null, null, null, null, null, null, null, 1, 5);

        List<FiredTask> taskTrends = new ArrayList<>();

        taskTrends.addAll(executingTaskList);
        taskTrends.addAll(taskHistoryList.getResult());

        Collections.sort(taskTrends, new Comparator<FiredTask>() {
            @Override
            public int compare(FiredTask o1, FiredTask o2) {
                return o2.getFiredTime().compareTo(o1.getFiredTime());
            }
        });

        data.put("taskTotalCount", taskTotalCount);
        data.put("executingTaskCount", executingTaskList.size());
        data.put("taskTrends", taskTrends);
        return new HttpResponseBodyWrapper(data);
    }

    @ResponseBody
    @PostMapping("/task/new")
    public void newTask(@RequestBody TaskConfig taskConfig, HttpServletRequest request) throws Exception {
        taskConfig.setName(StringUtils.trimToEmpty(taskConfig.getName()));
        taskConfig.setGroup(StringUtils.trimToEmpty(taskConfig.getGroup()));

        if (StringUtils.isEmpty(taskConfig.getName())) {
            throw new BizzException(BizzException.error_code_invalid_params, "请输入任务名称");
        } else {
            if (!Pattern.matches("^[A-Za-z0-9_]+$", taskConfig.getName())) {
                throw new BizzException(BizzException.error_code_invalid_params, "任务名称只允许使用字母、数字和下划线，请检查");
            }
        }
        if (StringUtils.isEmpty(taskConfig.getGroup())) {
            throw new BizzException(BizzException.error_code_invalid_params, "请输入任务所属组");
        } else {
            if (!Pattern.matches("^[A-Za-z0-9_]+$", taskConfig.getGroup())) {
                throw new BizzException(BizzException.error_code_invalid_params, "任务所属组只允许使用字母、数字和下划线，请检查");
            }
        }
        if (StringUtils.isEmpty(taskConfig.getJobComponent())) {
            throw new BizzException(BizzException.error_code_invalid_params, "请选择任务组件");
        }
        if (StringUtils.isNotEmpty(taskConfig.getParams())) {
            try {
                JSON.parseObject(taskConfig.getParams());
            } catch (Exception e) {
                throw new BizzException(BizzException.error_code_invalid_params, "任务参数输入有误，必须是JSON格式");
            }
        }
        if (StringUtils.isNotEmpty(taskConfig.getLinkageRule())) {
            try {
                JSON.parseArray(taskConfig.getLinkageRule());
            } catch (Exception e) {
                throw new BizzException(BizzException.error_code_invalid_params, "联动任务规则输入有误，必须是JSON格式");
            }
        }
        if (Constants.TASK_GROUP_Manual.equalsIgnoreCase(taskConfig.getGroup()) || Constants.TASK_GROUP_Tmp.equalsIgnoreCase(taskConfig.getGroup()) || Constants.TASK_GROUP_Linkage.equalsIgnoreCase(taskConfig.getGroup())) {
            throw new BizzException(BizzException.error_code_invalid_params, "任务所属组不允许使用\"tmp\"、\"manual\"、\"linkage\"");
        }


        Class<? extends JobComponent> jobComponentClass = RamStore.jobs.get(taskConfig.getJobComponent()).getClass();
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(Constants.JOB_DATA_KEY_TASK_PARAM, taskConfig.getParams());
        dataMap.put(Constants.JOB_DATA_KEY_TASK_LINKAGE_RULE, taskConfig.getLinkageRule());
        JobDetail jobDetail = JobBuilder.newJob(jobComponentClass).withIdentity(taskConfig.getName(), taskConfig.getGroup()).build();

        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(taskConfig.getName(), taskConfig.getGroup()).usingJobData(dataMap).withDescription(taskConfig.getDescription());

        if (taskConfig.getStartAtType() == 1) {
            triggerBuilder.startNow();
        } else {
            triggerBuilder.startAt(taskConfig.getStartAt());
        }
        if (taskConfig.getEndAtType() != 1) {
            triggerBuilder.endAt(taskConfig.getEndAt());
        }


        if (taskConfig.getScheduleType() == 1) {
            ScheduleTypeSimpleOptions scheduleOptions = taskConfig.getScheduleTypeSimpleOptions();

            SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
            scheduleBuilder.withIntervalInMilliseconds(scheduleOptions.getInterval())
                    .withRepeatCount(scheduleOptions.getRepeatType() == 1 ? -1 : scheduleOptions.getRepeatCount());

            if (scheduleOptions.getMisfireHandlingType() == 1) {
                scheduleBuilder.withMisfireHandlingInstructionFireNow();
            } else if (scheduleOptions.getMisfireHandlingType() == 2) {
                scheduleBuilder.withMisfireHandlingInstructionNextWithExistingCount();
            } else if (scheduleOptions.getMisfireHandlingType() == 3) {
                scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            } else if (scheduleOptions.getMisfireHandlingType() == 4) {
                scheduleBuilder.withMisfireHandlingInstructionNextWithRemainingCount();
            } else if (scheduleOptions.getMisfireHandlingType() == 5) {
                scheduleBuilder.withMisfireHandlingInstructionNowWithExistingCount();
            } else if (scheduleOptions.getMisfireHandlingType() == 6) {
                scheduleBuilder.withMisfireHandlingInstructionNowWithRemainingCount();
            }

            SimpleTrigger trigger = triggerBuilder.withSchedule(scheduleBuilder).build();

            scheduler.scheduleJob(jobDetail, trigger);
        } else if (taskConfig.getScheduleType() == 2) {
            ScheduleTypeCalendarIntervalOptions scheduleOptions = taskConfig.getScheduleTypeCalendarIntervalOptions();

            CalendarIntervalScheduleBuilder scheduleBuilder = CalendarIntervalScheduleBuilder.calendarIntervalSchedule();
            scheduleBuilder.withInterval(scheduleOptions.getInterval(), scheduleOptions.getIntervalUnit());

            if (scheduleOptions.getMisfireHandlingType() == 1) {
                scheduleBuilder.withMisfireHandlingInstructionDoNothing();
            } else if (scheduleOptions.getMisfireHandlingType() == 2) {
                scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
            } else if (scheduleOptions.getMisfireHandlingType() == 3) {
                scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            }


            CalendarIntervalTrigger trigger = triggerBuilder.withSchedule(scheduleBuilder).build();

            scheduler.scheduleJob(jobDetail, trigger);
        } else if (taskConfig.getScheduleType() == 3) {
            ScheduleTypeDailyTimeIntervalOptions scheduleOptions = taskConfig.getScheduleTypeDailyTimeIntervalOptions();

            DailyTimeIntervalScheduleBuilder scheduleBuilder = DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule();
            scheduleBuilder.withInterval(scheduleOptions.getInterval(), scheduleOptions.getIntervalUnit());

            if (scheduleOptions.getStartTimeOfDay() != null) {
                scheduleBuilder.startingDailyAt(scheduleOptions.getStartTimeOfDay());
            }
            if (scheduleOptions.getEndTimeOfDay() != null) {
                scheduleBuilder.endingDailyAt(scheduleOptions.getEndTimeOfDay());
            }
            if (ArrayUtils.isNotEmpty(scheduleOptions.getDaysOfWeek())) {
                scheduleBuilder.onDaysOfTheWeek(scheduleOptions.getDaysOfWeek());
            }

            if (scheduleOptions.getMisfireHandlingType() == 1) {
                scheduleBuilder.withMisfireHandlingInstructionDoNothing();
            } else if (scheduleOptions.getMisfireHandlingType() == 2) {
                scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
            } else if (scheduleOptions.getMisfireHandlingType() == 3) {
                scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            }

            DailyTimeIntervalTrigger trigger = triggerBuilder.withSchedule(scheduleBuilder).build();

            scheduler.scheduleJob(jobDetail, trigger);
        } else if (taskConfig.getScheduleType() == 4) {
            ScheduleTypeCronOptions scheduleOptions = taskConfig.getScheduleTypeCronOptions();

            if (!CronExpression.isValidExpression(scheduleOptions.getCron())) {
                throw new BizzException(BizzException.error_code_invalid_params, "Cron表达式输入有误");
            }

            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleOptions.getCron());

            if (scheduleOptions.getMisfireHandlingType() == 1) {
                scheduleBuilder.withMisfireHandlingInstructionDoNothing();
            } else if (scheduleOptions.getMisfireHandlingType() == 2) {
                scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
            } else if (scheduleOptions.getMisfireHandlingType() == 3) {
                scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            }

            CronTrigger trigger = triggerBuilder.withSchedule(scheduleBuilder).build();

            scheduler.scheduleJob(jobDetail, trigger);
        }
    }

    @ResponseBody
    @GetMapping("/task/detail")
    public HttpResponseBodyWrapper detail(String name, String group) throws Exception {
        Trigger abstractTrigger = scheduler.getTrigger(new TriggerKey(name, group));
        JobDetail jobDetail = scheduler.getJobDetail(abstractTrigger.getJobKey());

        TaskConfig taskConfig = new TaskConfig();
        taskConfig.setName(abstractTrigger.getKey().getName());
        taskConfig.setGroup(abstractTrigger.getKey().getGroup());
        taskConfig.setStartAtType(abstractTrigger.getStartTime() == null ? 1 : 2);
        taskConfig.setStartAt(abstractTrigger.getStartTime());
        taskConfig.setEndAtType(abstractTrigger.getEndTime() == null ? 1 : 2);
        taskConfig.setEndAt(abstractTrigger.getEndTime());
        taskConfig.setJobComponent(jobDetail.getJobClass().getName());
        taskConfig.setParams(abstractTrigger.getJobDataMap().getString(Constants.JOB_DATA_KEY_TASK_PARAM));
        taskConfig.setDescription(abstractTrigger.getDescription());
        taskConfig.setLinkageRule(abstractTrigger.getJobDataMap().getString(Constants.JOB_DATA_KEY_TASK_LINKAGE_RULE));


        if (abstractTrigger instanceof SimpleTrigger) {
            SimpleTrigger trigger = (SimpleTrigger) abstractTrigger;

            taskConfig.setScheduleType(1);

            ScheduleTypeSimpleOptions scheduleOptions = taskConfig.getScheduleTypeSimpleOptions();
            scheduleOptions.setInterval(trigger.getRepeatInterval());
            scheduleOptions.setRepeatType(trigger.getRepeatCount() == -1 ? 1 : 2);
            scheduleOptions.setRepeatCount(trigger.getRepeatCount());
            scheduleOptions.setMisfireHandlingType(trigger.getMisfireInstruction());
        } else if (abstractTrigger instanceof CalendarIntervalTrigger) {
            CalendarIntervalTrigger trigger = (CalendarIntervalTrigger) abstractTrigger;

            taskConfig.setScheduleType(2);

            ScheduleTypeCalendarIntervalOptions scheduleOptions = taskConfig.getScheduleTypeCalendarIntervalOptions();
            scheduleOptions.setInterval(trigger.getRepeatInterval());
            scheduleOptions.setIntervalUnit(trigger.getRepeatIntervalUnit());
            scheduleOptions.setMisfireHandlingType(trigger.getMisfireInstruction());
        } else if (abstractTrigger instanceof DailyTimeIntervalTrigger) {
            DailyTimeIntervalTrigger trigger = (DailyTimeIntervalTrigger) abstractTrigger;

            taskConfig.setScheduleType(3);

            ScheduleTypeDailyTimeIntervalOptions scheduleOptions = taskConfig.getScheduleTypeDailyTimeIntervalOptions();
            scheduleOptions.setStartTimeOfDay(trigger.getStartTimeOfDay());
            scheduleOptions.setEndTimeOfDay(trigger.getEndTimeOfDay());
            scheduleOptions.setDaysOfWeek(trigger.getDaysOfWeek().toArray(new Integer[trigger.getDaysOfWeek().size()]));
            scheduleOptions.setInterval(trigger.getRepeatInterval());
            scheduleOptions.setIntervalUnit(trigger.getRepeatIntervalUnit());
            scheduleOptions.setMisfireHandlingType(trigger.getMisfireInstruction());
        } else if (abstractTrigger instanceof CronTrigger) {
            CronTrigger trigger = (CronTrigger) abstractTrigger;

            taskConfig.setScheduleType(4);

            ScheduleTypeCronOptions scheduleOptions = taskConfig.getScheduleTypeCronOptions();
            scheduleOptions.setCron(trigger.getCronExpression());
            scheduleOptions.setMisfireHandlingType(trigger.getMisfireInstruction());
        }

        return new HttpResponseBodyWrapper(taskConfig);
    }

    @ResponseBody
    @PostMapping("/task/edit")
    public void edit(@RequestBody TaskConfig taskConfig) throws Exception {
        if (StringUtils.isNotEmpty(taskConfig.getParams())) {
            try {
                JSON.parseObject(taskConfig.getParams());
            } catch (Exception e) {
                throw new BizzException(BizzException.error_code_invalid_params, "任务参数输入有误，必须是JSON格式");
            }
        }
        if (StringUtils.isNotEmpty(taskConfig.getLinkageRule())) {
            try {
                JSON.parseArray(taskConfig.getLinkageRule());
            } catch (Exception e) {
                throw new BizzException(BizzException.error_code_invalid_params, "联动任务规则输入有误，必须是JSON格式");
            }
        }

        JobDataMap dataMap = new JobDataMap();
        dataMap.put(Constants.JOB_DATA_KEY_TASK_PARAM, taskConfig.getParams());
        dataMap.put(Constants.JOB_DATA_KEY_TASK_LINKAGE_RULE, taskConfig.getLinkageRule());

        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(taskConfig.getName(), taskConfig.getGroup()).usingJobData(dataMap).withDescription(taskConfig.getDescription());

        if (taskConfig.getStartAtType() == 1) {
            triggerBuilder.startNow();
        } else {
            triggerBuilder.startAt(taskConfig.getStartAt());
        }
        if (taskConfig.getEndAtType() != 1) {
            triggerBuilder.endAt(taskConfig.getEndAt());
        }


        if (taskConfig.getScheduleType() == 1) {
            ScheduleTypeSimpleOptions scheduleOptions = taskConfig.getScheduleTypeSimpleOptions();

            SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
            scheduleBuilder.withIntervalInMilliseconds(scheduleOptions.getInterval())
                    .withRepeatCount(scheduleOptions.getRepeatType() == 1 ? -1 : scheduleOptions.getRepeatCount());

            if (scheduleOptions.getMisfireHandlingType() == 1) {
                scheduleBuilder.withMisfireHandlingInstructionFireNow();
            } else if (scheduleOptions.getMisfireHandlingType() == 2) {
                scheduleBuilder.withMisfireHandlingInstructionNextWithExistingCount();
            } else if (scheduleOptions.getMisfireHandlingType() == 3) {
                scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            } else if (scheduleOptions.getMisfireHandlingType() == 4) {
                scheduleBuilder.withMisfireHandlingInstructionNextWithRemainingCount();
            } else if (scheduleOptions.getMisfireHandlingType() == 5) {
                scheduleBuilder.withMisfireHandlingInstructionNowWithExistingCount();
            } else if (scheduleOptions.getMisfireHandlingType() == 6) {
                scheduleBuilder.withMisfireHandlingInstructionNowWithRemainingCount();
            }

            SimpleTrigger trigger = triggerBuilder.withSchedule(scheduleBuilder).build();

            scheduler.rescheduleJob(trigger.getKey(), trigger);
        } else if (taskConfig.getScheduleType() == 2) {
            ScheduleTypeCalendarIntervalOptions scheduleOptions = taskConfig.getScheduleTypeCalendarIntervalOptions();

            CalendarIntervalScheduleBuilder scheduleBuilder = CalendarIntervalScheduleBuilder.calendarIntervalSchedule();
            scheduleBuilder.withInterval(scheduleOptions.getInterval(), scheduleOptions.getIntervalUnit());

            if (scheduleOptions.getMisfireHandlingType() == 1) {
                scheduleBuilder.withMisfireHandlingInstructionDoNothing();
            } else if (scheduleOptions.getMisfireHandlingType() == 2) {
                scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
            } else if (scheduleOptions.getMisfireHandlingType() == 3) {
                scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            }


            CalendarIntervalTrigger trigger = triggerBuilder.withSchedule(scheduleBuilder).build();

            scheduler.rescheduleJob(trigger.getKey(), trigger);
        } else if (taskConfig.getScheduleType() == 3) {
            ScheduleTypeDailyTimeIntervalOptions scheduleOptions = taskConfig.getScheduleTypeDailyTimeIntervalOptions();

            DailyTimeIntervalScheduleBuilder scheduleBuilder = DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule();
            scheduleBuilder.withInterval(scheduleOptions.getInterval(), scheduleOptions.getIntervalUnit());

            if (scheduleOptions.getStartTimeOfDay() != null) {
                scheduleBuilder.startingDailyAt(scheduleOptions.getStartTimeOfDay());
            }
            if (scheduleOptions.getEndTimeOfDay() != null) {
                scheduleBuilder.endingDailyAt(scheduleOptions.getEndTimeOfDay());
            }
            if (ArrayUtils.isNotEmpty(scheduleOptions.getDaysOfWeek())) {
                scheduleBuilder.onDaysOfTheWeek(scheduleOptions.getDaysOfWeek());
            }

            if (scheduleOptions.getMisfireHandlingType() == 1) {
                scheduleBuilder.withMisfireHandlingInstructionDoNothing();
            } else if (scheduleOptions.getMisfireHandlingType() == 2) {
                scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
            } else if (scheduleOptions.getMisfireHandlingType() == 3) {
                scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            }

            DailyTimeIntervalTrigger trigger = triggerBuilder.withSchedule(scheduleBuilder).build();

            scheduler.rescheduleJob(trigger.getKey(), trigger);
        } else if (taskConfig.getScheduleType() == 4) {
            ScheduleTypeCronOptions scheduleOptions = taskConfig.getScheduleTypeCronOptions();

            if (!CronExpression.isValidExpression(scheduleOptions.getCron())) {
                throw new BizzException(BizzException.error_code_invalid_params, "Cron表达式输入有误");
            }

            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleOptions.getCron());

            if (scheduleOptions.getMisfireHandlingType() == 1) {
                scheduleBuilder.withMisfireHandlingInstructionDoNothing();
            } else if (scheduleOptions.getMisfireHandlingType() == 2) {
                scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
            } else if (scheduleOptions.getMisfireHandlingType() == 3) {
                scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            }

            CronTrigger trigger = triggerBuilder.withSchedule(scheduleBuilder).build();

            scheduler.rescheduleJob(trigger.getKey(), trigger);
        }


    }

    @ResponseBody
    @PostMapping("/task/delete")
    public void delete(String name, String group) throws Exception {
        scheduler.unscheduleJob(new TriggerKey(name, group));
    }

    @ResponseBody
    @PostMapping("/task/pause")
    public void pause(String name, String group) throws Exception {
        scheduler.pauseTrigger(new TriggerKey(name, group));
    }

    @ResponseBody
    @PostMapping("/task/resume")
    public void resume(String name, String group) throws Exception {
        scheduler.resumeTrigger(new TriggerKey(name, group));
    }

    @ResponseBody
    @PostMapping("/task/execute")
    public void execute(String name, String group) throws Exception {
        JobKey jobKey = new JobKey(name, group);
        Trigger trigger = scheduler.getTrigger(new TriggerKey(name, group));
        JobDataMap jobDataMap = trigger.getJobDataMap();

        String randomTriggerName = "MT_" + Long.toString(RandomUtils.nextLong(), 30 + (int) (System.currentTimeMillis() % 7));
        OperableTrigger operableTrigger = (OperableTrigger) newTrigger().withIdentity(randomTriggerName, Constants.TASK_GROUP_Manual).forJob(jobKey).build();
        if (jobDataMap != null) {
            operableTrigger.setJobDataMap(jobDataMap);
        }

        scheduler.scheduleJob(operableTrigger);
    }

    @ResponseBody
    @PostMapping("/task/tmp")
    public void quickTask(@RequestBody TmpTaskConfig quickTaskConfig) throws Exception {

        quickTaskConfig.setName(StringUtils.trimToEmpty(quickTaskConfig.getName()));

        if (StringUtils.isEmpty(quickTaskConfig.getName())) {
            throw new BizzException(BizzException.error_code_invalid_params, "请输入任务名称");
        } else {
            if (!Pattern.matches("^[A-Za-z0-9_]+$", quickTaskConfig.getName())) {
                throw new BizzException(BizzException.error_code_invalid_params, "任务名称只允许使用字母、数字和下划线，请检查");
            }
        }

        if (StringUtils.isEmpty(quickTaskConfig.getJobComponent())) {
            throw new BizzException(BizzException.error_code_invalid_params, "请选择任务组件");
        }
        if (StringUtils.isNotEmpty(quickTaskConfig.getParams())) {
            try {
                JSON.parseObject(quickTaskConfig.getParams());
            } catch (Exception e) {
                throw new BizzException(BizzException.error_code_invalid_params, "任务参数输入有误，必须是JSON格式");
            }
        }

        String name = quickTaskConfig.getName();
        String group = Constants.TASK_GROUP_Tmp;

        Class<? extends JobComponent> jobComponentClass = RamStore.jobs.get(quickTaskConfig.getJobComponent()).getClass();
        JobDetail jobDetail = JobBuilder.newJob(jobComponentClass).withIdentity(name, group).build();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(Constants.JOB_DATA_KEY_TASK_PARAM, quickTaskConfig.getParams());
//        jobDataMap.put(Constants.JOB_DATA_KEY_TASK_LINKAGE_RULE, quickTaskConfig.getLinkageRule());

        OperableTrigger operableTrigger = (OperableTrigger) newTrigger().withIdentity(name, group).usingJobData(jobDataMap).build();

        if (quickTaskConfig.getEnableStartDelay() && quickTaskConfig.getStartDelay() != null) {
            Calendar startTime = Calendar.getInstance();
            startTime.add(Calendar.MILLISECOND, quickTaskConfig.getStartDelay());
            operableTrigger.setStartTime(startTime.getTime());
        }
        scheduler.scheduleJob(jobDetail, operableTrigger);
    }
}
