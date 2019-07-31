package com.bee.scheduler.core;

import com.alibaba.fastjson.JSONObject;

/**
 * @author weiwei
 */
public class TaskExecutionResult {
    private boolean success;
    private JSONObject data;

    public TaskExecutionResult(boolean success, JSONObject data) {
        this.success = success;
        this.data = data;
    }

    public static TaskExecutionResult success() {
        return success(null);
    }

    public static TaskExecutionResult success(JSONObject data) {
        return new TaskExecutionResult(true, data);
    }

    public static TaskExecutionResult fail() {
        return fail(null);
    }

    public static TaskExecutionResult fail(JSONObject data) {
        return new TaskExecutionResult(false, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }
}