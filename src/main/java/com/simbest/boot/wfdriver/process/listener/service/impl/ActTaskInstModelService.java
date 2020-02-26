package com.simbest.boot.wfdriver.process.listener.service.impl;

import cn.hutool.core.util.StrUtil;
import com.simbest.boot.base.exception.Exceptions;
import com.simbest.boot.base.service.impl.LogicService;
import com.simbest.boot.util.DateUtil;
import com.simbest.boot.wfdriver.exceptions.FlowableDriverBusinessException;
import com.simbest.boot.wfdriver.process.bussiness.model.ActBusinessStatus;
import com.simbest.boot.wfdriver.process.bussiness.service.IActBusinessStatusService;
import com.simbest.boot.wfdriver.process.listener.mapper.ActTaskInstModelMapper;
import com.simbest.boot.wfdriver.process.listener.model.ActTaskInstModel;
import com.simbest.boot.wfdriver.process.listener.service.IActProcessInstModelService;
import com.simbest.boot.wfdriver.process.listener.service.IActTaskInstModelService;
import com.simbest.boot.wfdriver.process.operate.WfProcessManager;
import com.simbest.boot.wfdriver.task.UserTaskSubmit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;


/**
 * <strong>Title : ActTaskInstModelService</strong><br>
 * <strong>Description : 流程工作项业务操作</strong><br>
 * <strong>Create on : 2018/5/29</strong><br>
 * <strong>Modify on : 2018/5/29</strong><br>
 * <strong>Copyright (C) Ltd.</strong><br>
 *
 * @author LJW lijianwu@simbest.com.cn
 * @version <strong>V1.0.0</strong><br>
 *          <strong>修改历史:</strong><br>
 *          修改人 修改日期 修改描述<br>
 *          -------------------------------------------<br>
 */
@Slf4j
@Service(value="actTaskInstModelService")
public class ActTaskInstModelService extends LogicService<ActTaskInstModel,String> implements IActTaskInstModelService {

	private ActTaskInstModelMapper actTaskInstModelMapper;

    @Autowired
    private UserTaskSubmit userTaskSubmit;

    @Autowired
    private IActBusinessStatusService actBusinessStatusService;

    @Autowired
    private IActProcessInstModelService actProcessInstModelService;

	@Autowired
    public ActTaskInstModelService ( ActTaskInstModelMapper mapper ) {
	    super(mapper);
        this.actTaskInstModelMapper = mapper;
    }

    @Transactional
	@Override
	public int created(ActTaskInstModel actTaskInstModel) {
	    int ret = 0;
	    try {
	        String participantIdentity = actTaskInstModel.getParticipantIdentity();
	        if ( StrUtil.isEmpty( participantIdentity ) ){
	            actTaskInstModel.setParticipantIdentity( WfProcessManager.creatorIdentity );
            }
            actTaskInstModel.setEnabled(true);
	        if ( StrUtil.isEmpty( actTaskInstModel.getFromTaskId() ) ){
                actTaskInstModel.setFromTaskId( "-1" );
            }
            actTaskInstModel.setCreator( actTaskInstModel.getAssignee() );
            actTaskInstModel.setModifier( actTaskInstModel.getAssignee() );
            //wrapCreateInfo( actTaskInstModel );
            actTaskInstModel = actTaskInstModelMapper.save(actTaskInstModel);
            //以下是推送统一待办
            //ActBusinessStatus actBusinessStatus = actBusinessStatusService.getByProcessInst( actTaskInstModel.getProcessInstId() );
            //userTaskSubmit.submitTodoOpen( actBusinessStatus,actTaskInstModel, actTaskInstModel.getAssignee());
            ret = 1;
        }catch (Exception e){
            ret = 0;
            FlowableDriverBusinessException.printException( e );
        }
		return ret;
	}

	@Override
	public int updateByTaskId(ActTaskInstModel actTaskInstModel) {
        int ret = 0;
        try {
            actTaskInstModel.setEndTime( LocalDateTime.now());
            actTaskInstModel.setEnabled(true);
            actTaskInstModel.setModifier(actTaskInstModel.getAssignee() );
            actTaskInstModel.setModifiedTime(LocalDateTime.now());
            //wrapUpdateInfo( actTaskInstModel );
            actTaskInstModelMapper.updateByTaskId(actTaskInstModel);
            //以下是推送统一待办
            //ActBusinessStatus actBusinessStatus = actBusinessStatusService.getByProcessInst( actTaskInstModel.getProcessInstId() );
            //userTaskSubmit.submitTodoClose( actBusinessStatus,actTaskInstModel, actTaskInstModel.getAssignee());
            ret = 1;
        }catch (Exception e){
            ret = 0;
            FlowableDriverBusinessException.printException( e );
        }
        return ret;
	}


    /**
     * 根据工作项ID查询工作项信息
     * @param processInstId     流程实例ID
     * @param taskID        工作项ID
     * @return
     */
	@Override
	public ActTaskInstModel getByProcessInstIdAndTaskId(String processInstId, String taskID) {
	    try {
            return actTaskInstModelMapper.getByProcessInstIdAndTaskId(processInstId,taskID);
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return null;
	}

    /**
     * 根据工作项ID查询工作项信息
     * @param taskID  工作项ID
     * @return
     */
    @Override
    public ActTaskInstModel getByTaskId ( String taskID ) {
        try {
            return actTaskInstModelMapper.getByTaskId(taskID);
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return null;
    }

    /**
     * 根据流程实例ID 删除工作项信息
     * @param processInstId   流程实例ID
     * @return
     */
	@Override
	public int deleteByProInsId(String processInstId) {
	    int ret = 0;
	    try {
            ret = actTaskInstModelMapper.deleteByProcessInst(processInstId);
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return ret;
	}


    /**
     * 根据流程实例ID，流程活动实例ID，查询流程工作项信息
     * @param processInstId     流程实例ID
     * @param taskDefinitionKey    流程活动定义ID
     * @return
     */
    @Override
    public List<ActTaskInstModel> getByProcessInstIdAndTaskDefinitionKey ( String processInstId, String taskDefinitionKey ) {
        try {
            List<ActTaskInstModel> actTaskInstModels = actTaskInstModelMapper.getByProcessInstIdAndTaskDefinitionKey( processInstId,taskDefinitionKey );
            return actTaskInstModels;
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return null;
    }

    /**
     * 根据流程实例ID查询工作项信息
     * @param processInstId        流程实例ID
     * @return
     */
    @Override
    public List<ActTaskInstModel> queryTaskInstModelByProcessInstId ( String processInstId ) {
        try {
            return actTaskInstModelMapper.queryTaskInstModelByProcessInstIdAndEnabled( processInstId,Boolean.TRUE );
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return null;
    }

}
