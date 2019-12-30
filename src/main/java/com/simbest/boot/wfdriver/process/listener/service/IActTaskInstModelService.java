package com.simbest.boot.wfdriver.process.listener.service;


import com.simbest.boot.base.service.ILogicService;
import com.simbest.boot.wfdriver.process.listener.model.ActTaskInstModel;

import java.util.List;

/**
 *@ClassName IActTaskInstModelService
 *@Description
 *@Author Administrator
 *@Date 2019/12/01 16:03
 *@Version 1.0
 **/
public interface IActTaskInstModelService extends ILogicService<ActTaskInstModel,String> {

	int created(ActTaskInstModel actTaskInstModel);

	int updateByTaskId(ActTaskInstModel actTaskInstModel);

    /**
     * 根据工作项ID查询工作项信息
     * @param processInstId      流程实例ID
     * @param taskID        任务ID
     * @return
     */
	ActTaskInstModel getByProcessInstIdAndTaskId(String processInstId, String taskID);

    /**
     * 根据工作项ID查询工作项信息
     * @param taskID 任务ID
     * @return
     */
    ActTaskInstModel getByTaskId(String taskID);

    /**
     * 根据流程实例ID 删除工作项信息
     * @param processInstId  流程实例ID
     * @return
     */
    int deleteByProInsId(String processInstId);

    /**
     * 根据流程实例ID，流程活动实例ID，查询流程工作项信息
     * @param processInstId     流程实例ID
     * @param taskDefinitionKey    流程活动定义ID
     * @return
     */
    List<ActTaskInstModel> getByProcessInstIdAndTaskDefinitionKey(String processInstId, String taskDefinitionKey);

    /**
     * 根据流程实例ID查询工作项信息
     * @param processInsId        流程实例ID
     * @return
     */
    List<ActTaskInstModel> queryTaskInstModelByProcessInstId(String processInsId);

}
