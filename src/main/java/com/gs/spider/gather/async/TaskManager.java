package com.gs.spider.gather.async;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.gs.spider.model.async.State;
import com.gs.spider.model.async.Task;
import com.gs.spider.utils.HttpClientUtil;

/**
 * TaskManager
 *
 * @author Gao Shen
 * @version 16/2/15
 */
@Component
@Scope("prototype")
public class TaskManager {
    private final String TIMER_TASK_NAME_PREFIX = "Timer-Task-";
    private Logger LOG = LogManager.getLogger(TaskManager.class);
    private Logger TASK_LOG = LogManager.getLogger("TASK_LOG");
    private Map<String, Task> taskMap = new LinkedHashMap<>();
    @Autowired
    private HttpClientUtil httpClientUtil;

    /**
     * 获取所有Task,包括已经完成的和未完成的
     *
     * @param containsExtraInfo 是否显示额外信息
     * @return
     */
    public Collection<Task> getTasks(boolean containsExtraInfo) {
        TASK_LOG.info("获取任务列表,包含额外信息:{}", containsExtraInfo);
        // 先是三目运算符，将输入的布尔参数直接作为判断条件
        return containsExtraInfo ? taskMap.values() :
                // 当不需要额外信息时，尝试进行浅拷贝(该拷贝不涉及其引用的对象，只是对task对象本身，进行一次克隆)
                // 并且在浅拷贝的克隆体上删除 ExtraInfo
                taskMap.values().stream().map(task -> {
                    Task t = null;
                    try {
                        t = ((Task) task.clone());
                        t.setExtraInfo(null);
                        return t;
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).collect(Collectors.toList());
    }

    /**
     * 根据id获取task
     *
     * @param taskId            任务id
     * @param containsExtraInfo 是否显示额外信息
     * @return
     */
    public Task getTaskById(String taskId, boolean containsExtraInfo) {
        TASK_LOG.info("根据任务ID:{},获取任务实体", taskId);
        Task t = taskMap.get(taskId);
        if (t != null && !containsExtraInfo) {
        	try {
        		t = ((Task) t.clone());
        	} catch (CloneNotSupportedException e) {
        		e.printStackTrace();
        	}
        	t.setExtraInfo(null);
        }
        return t;
    }

    // 方法重载时，参数较少的版本往往是参数较多版本的代理人
    /**
     * 根据id获取task,显示任务详情
     *
     * @param taskId 任务id
     * @return 任务实体
     */
    public Task getTaskById(String taskId) {
        return getTaskById(taskId, true);
    }

    /**
     * 根据id获取指定task已经抓取多少
     *
     * @param taskId 带查询的任务id
     * @return 该任务已经抓取了多少
     */
    public int getTaskCount(String taskId) {
        if (taskMap.get(taskId) == null) throw new IllegalStateException("找不到当前task,taskId:" + taskId);
        return taskMap.get(taskId).getCount();
    }

    public Task initTask(String name) {
        return initTask(name, null, null);
    }

    /**
     * 初始化任务
     *
     * @param name         任务名称
     * @param callbackURL  回调地址
     * @param callbackPara 回调参数
     * @return 已初始化的任务
     */
    public Task initTask(String name, String callbackURL, String callbackPara) {
        return initTask(UUID.randomUUID().toString(), name, callbackURL, callbackPara);
    }

    /**
     * 初始化任务
     *
     * @param taskId       任务id
     * @param name         任务名称
     * @param callbackURL  回调地址
     * @param callbackPara 回调地址参数
     * @return 已初始化的任务
     */
    public Task initTask(String taskId, String name, String callbackURL, String callbackPara) {
        Task task = new Task(taskId, name, System.currentTimeMillis());
        task.addCallbackURL(callbackURL);
        task.setCallbackPara(callbackPara + "&taskId=" + taskId);
        task.setDescription("任务名称:" + name + "已初始化");
        // 所谓初始化，不仅是创建一个 task 对象，还要将它放入 taskMap
        taskMap.put(task.getTaskId(), task);
        return task;
    }

    /**
     * 初始化一个任务
     *
     * @param taskId       任务id
     * @param name         任务名称
     * @param callbackURL  回调地址列表
     * @param callbackPara 回调参数
     * @return 已经初始化的任务
     */
    public Task initTask(String taskId, String name, List<String> callbackURL, String callbackPara) {
        Task task = new Task(taskId, name, System.currentTimeMillis());
        task.setCallbackURL(callbackURL)
        	.setCallbackPara(callbackPara + "&taskId=" + taskId);
        task.setDescription("任务名称:" + name + "已初始化");
        taskMap.put(task.getTaskId(), task);
        return task;
    }

    /**
     * 开始一个任务
     *
     * @param task 待开始的任务
     */
    public void startTask(Task task) {
        task.setState(State.RUNNING);
        task.setDescription("任务ID:" + task.getTaskId() + "已标记为开始状态");
    }

    /**
     * 任务完成的数量加1
     *
     * @param task 待执行的任务
     */
    public synchronized void increaseCount(Task task) {
        task.setCount(task.getCount() + 1);
    }

    /**
     * 根据任务ID停止任务
     *
     * @param taskId 任务id
     */
    public void stopTask(String taskId) {
        //完毕更新任务状态信息
        Task task = taskMap.get(taskId);
        if (task != null){ 
        	stopTask(task);
        }
    }

    /**
     * 将任务标记为停止状态,并对任务进行回调通知
     *
     * @param task 待结束的任务实体
     */
    public void stopTask(Task task) {
        TASK_LOG.info("任务ID:{}已停止", task.getTaskId());
        try {
            //当抓取完毕的时候将访问这个回调地址,并附带上相关信息
            boolean useHttpCallback = task.getCallbackURL().size() > 0;
            if (useHttpCallback) {
                for (final String url : task.getCallbackURL()) {
                    if (StringUtils.isBlank(url) || !url.startsWith("http")) {
                        continue;
                    }
                    task.setDescription("使用HTTP进行回调");
                    TASK_LOG.info("任务ID:{},使用HTTP进行回调", task.getTaskId());
                    String callbackURLWithPara;
                    if (!url.contains("?")) {//回调地址中没有带参数
                        callbackURLWithPara = url + "?" + task.getCallbackPara();
                    } else {//回调地址中已经带有参数
                        callbackURLWithPara = url + "&" + task.getCallbackPara();
                    }
                    LOG.debug("任务线程结束,callbackURLWithPara:" + callbackURLWithPara);
                    String callBackReturnStr = httpClientUtil.get(callbackURLWithPara);
                    LOG.info("任务线程结束,callBack返回值: " + callBackReturnStr);
                    task.setDescription("HTTP回调完成,URL:%s,返回值:%s", callbackURLWithPara, callBackReturnStr);
                }
            }else{
            	LOG.info("任务线程结束,由于回调地址为空,不进行回调");
            }
        } catch (Exception e) {
            LOG.error("任务线程完毕调用回调时出错," + e.getLocalizedMessage());
            task.setDescription("任务线程完毕调用回调时出错,%s %s", e.toString(), e.getLocalizedMessage());
        }
        //完毕更新任务状态信息
        task.setState(State.STOP);
        task.setTime(System.currentTimeMillis());
        task.setDescription("任务ID:" + task.getTaskId() + "已标记为停止状态");
        TASK_LOG.info("任务ID:" + task.getTaskId() + "已标记为停止状态");
    }

    /**
     * 根据任务名称查找正在运行的任务
     *
     * @param name 任务名称
     * @return 是否存在
     */
    public boolean findRunningTaskByName(String name) {
        return taskMap.entrySet().stream()
                .filter(taskEntry ->
                        taskEntry.getValue().getName().equals(name)
                                && taskEntry.getValue().getState() == State.RUNNING
                ).count() > 0;
    }

    /**
     * 根据任务名称查找任务是否存在
     *
     * @param name 任务名称
     * @return 是否存在
     */
    public boolean findScheduledTaskByName(String name) {
        return taskMap.entrySet().stream()
                .filter(taskEntry ->
                        taskEntry.getValue().getName().equals(name)
                                // 下面这两个 与 条件，是否可以理解为，只要正常的任务一般都会有调度周期及其时间单位
                                && taskEntry.getValue().getPeriod() > 0 && taskEntry.getValue().getTimeUnit() != null
                ).count() > 0;
    }

    // 这写法有点吊，后面要重点看看
    /**
     * 根据条件查找是否有这样的任务
     *
     * @param function 条件
     * @return true 有这样的任务 false 没有这样的任务
     */
    public boolean findTaskBy(Function<Task, Boolean> function) {
        // 确保参数非 null, 如果是 null 会报出 NullPointerException
        Preconditions.checkNotNull(function);
        return taskMap.entrySet().stream()
                .filter(taskEntry ->
                        {
                            // 用 function.apply 方法判断任务是否符合用户的输入
                            Boolean b = function.apply(taskEntry.getValue());
                            return b == null ? false : b;
                        }

                ).count() > 0;
    }

    /**
     * 根据任务编号删除任务
     *
     * @param taskId 任务编号
     * @return 已删除的任务
     */
    public void deleteTask(String taskId) {
        Task task = getTaskById(taskId);
        task.getDescriptions().clear();
        task.getExtraInfo().clear();
        TASK_LOG.info("根据任务ID:{}删除任务", taskId);
        Preconditions.checkNotNull(task, "任务对象为空,taskId" + taskId);
        // 表达式不成立的时候，就会抛出 IllegalArgumentException 异常
        Preconditions.checkArgument(task.getState() != State.RUNNING && task.getState() != State.INIT, "当前任务正在运行不可删除,状态:" + task.getState());
        taskMap.remove(taskId);
    }

    // 从 taskMap 中 remove 掉处于指定状态的 task
    /**
     * 根据任务状态删除任务
     *
     * @param state 任务状态
     */
    public void deleteTasksByState(State state) {
        List<String> taskId2BeDeleted = Lists.newArrayList();
        taskMap.entrySet().stream().filter(taskEntry -> taskEntry.getValue().getState() == state).forEach(stringTaskEntry -> {
            taskId2BeDeleted.add(stringTaskEntry.getKey());
        });
        for (String id : taskId2BeDeleted) {
            taskMap.remove(id);
        }
    }

    /**
     * 统计指定状态的任务数
     *
     * @param state 指定的任务状态
     * @return 指定任务状态的任务数量
     */
    public long countByState(State state) {
        return taskMap.entrySet().stream().filter(taskEntry -> taskEntry.getValue().getState() == state).count();
    }
}

// 2019-04-29 22:56 提供针对 task 实体的一切操作，并维护一个 taskMap 用来存放一定量的 task