package com.simbest.boot.wfdriver.process.bussiness.service;


import com.simbest.boot.base.service.IGenericService;
import com.simbest.boot.wfdriver.process.bussiness.model.ActBusinessStatus;
import com.simbest.boot.wfdriver.process.listener.model.ActProcessInstModel;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 *@ClassName ActBusinessStatusMapper
 *@Description
 *@Author Administrator
 *@Date 2019/12/01 16:03
 *@Version 1.0
 **/
public interface IActBusinessStatusService extends IGenericService<ActBusinessStatus,String> {


    /**
     * 流程开始 往act_business_status表中插入信息
     * @param processInstanceId   流程实例对象
     * @param startMap   相关参数
     * @return
     */

    int saveActBusinessStatusData(String processInstanceId, Map<String, Object> startMap);

    /**
     * 流程完成更新信息
     * @return
     */
	int updateActBusinessStatusData(Map<String,Object> nextParam);

    /**
     * 根据流程实例ID 查询业务流程操作信息
     * @param processInstID
     * @return
     */
	ActBusinessStatus getByProcessInst(String processInstID);


    /**
     * 流程结束更新结束时间
     */
	int updateListenerByProcess(ActProcessInstModel actProcessInstModel);

    /**
     * 根据流程实例ID删除业务流程状态数据
     * @param actBusinessStatus
     */
    void deletActBusinessStatusData(ActBusinessStatus actBusinessStatus);

    /**
     * 根据流程实例ID逻辑删除业务状态数据
     * @param processInstId
     * @return
     */
    int updateActBusDataByProInsId(String processInstId);

    /**
     * 功能描述:
     * 根据流程实例ID更新流程的状态
     * @param processInstId        流程实例ID
     * @return
     * @date 2020/2/15 23:50
     * @auther ljw
     */
    int updatePorcessStateByProInstId(String processInstId);


}
