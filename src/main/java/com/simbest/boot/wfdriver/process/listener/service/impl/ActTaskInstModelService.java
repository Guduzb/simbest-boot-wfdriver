package com.simbest.boot.wfdriver.process.listener.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.simbest.boot.base.exception.Exceptions;
import com.simbest.boot.base.service.impl.LogicService;
import com.simbest.boot.util.DateUtil;
import com.simbest.boot.util.json.JacksonUtils;
import com.simbest.boot.util.redis.RedisUtil;
import com.simbest.boot.wfdriver.constants.ProcessConstants;
import com.simbest.boot.wfdriver.exceptions.FlowableDriverBusinessException;
import com.simbest.boot.wfdriver.process.bussiness.model.ActBusinessStatus;
import com.simbest.boot.wfdriver.process.bussiness.service.IActBusinessStatusService;
import com.simbest.boot.wfdriver.process.listener.mapper.ActTaskInstModelMapper;
import com.simbest.boot.wfdriver.process.listener.model.ActTaskInstModel;
import com.simbest.boot.wfdriver.process.listener.service.IActProcessInstModelService;
import com.simbest.boot.wfdriver.process.listener.service.IActTaskInstModelService;
import com.simbest.boot.wfdriver.process.operate.WfProcessManager;
import com.simbest.boot.wfdriver.process.operate.WorkTaskManager;
import com.simbest.boot.wfdriver.task.UserTaskSubmit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;


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
            ActBusinessStatus actBusinessStatus = actBusinessStatusService.getByProcessInst( actTaskInstModel.getProcessInstId() );
            Map<String,Object> cacheStartMapParam = RedisUtil.getBean( actTaskInstModel.getBusinessKey().concat(ProcessConstants.PROCESS_START_REDIS_SUFFIX),Map.class);
            log.warn( "回调后打印流程启动提交的候选中文名称：【{}】", JacksonUtils.obj2json( cacheStartMapParam ) );
	        String participantIdentity = actTaskInstModel.getParticipantIdentity();
	        if ( StrUtil.isEmpty( participantIdentity ) ){
	            actTaskInstModel.setParticipantIdentity( MapUtil.getStr( cacheStartMapParam,"creatorIdentity" ) );
            }
            actTaskInstModel.setEnabled(true);
	        if ( StrUtil.isEmpty( actTaskInstModel.getFromTaskId() ) ){
                actTaskInstModel.setFromTaskId( "-1" );
            }

	        if ( CollectionUtil.isNotEmpty( cacheStartMapParam ) && StrUtil.equals( actTaskInstModel.getFromTaskId(),"-1" )){
	            String[] staticNextUsers = StrUtil.split( MapUtil.getStr( cacheStartMapParam,"staticNextUser" ),"#" );
                String[] staticNextUserNames = StrUtil.split( MapUtil.getStr( cacheStartMapParam,"staticNextUserName" ),"#" );
	            for ( int i = 0,cnt = staticNextUsers.length;i < cnt;i++ ){
                    String[] staticNextUserItems = StrUtil.split( staticNextUsers[i],"," );
                    String[] staticNextUserNameItems = StrUtil.split( staticNextUserNames[i],"," );
                    for ( int k = 0,num = staticNextUserItems.length;k < num;k++ ){
                        log.warn( "启动流程回调循环输出候选人中文名称Assignee：【{}】>>>>staticNextUserItems：【{}】>>>>>【{}】",actTaskInstModel.getAssignee(),staticNextUserItems[k],StrUtil.equals( actTaskInstModel.getAssignee(),staticNextUserItems[k] ) );
                        if ( StrUtil.equals( actTaskInstModel.getAssignee(),staticNextUserItems[k] ) ){
                            actTaskInstModel.setAssigneeName( staticNextUserNameItems[k] );
                        }else{
                            actTaskInstModel.setAssigneeName( MapUtil.getStr( cacheStartMapParam,"currentUserName" ) );
                        }
                    }
                }
            }
            Map<String,Object> cacheSubmitMapParam = RedisUtil.getBean( actTaskInstModel.getProcessInstId().concat(ProcessConstants.PROCESS_SUBMIT_REDIS_SUFFIX),Map.class);
            log.warn( "回调后打印流程下一步提交的候选中文名称：【{}】", JacksonUtils.obj2json( cacheSubmitMapParam ) );
            if ( CollectionUtil.isNotEmpty( cacheSubmitMapParam ) ){
                String[] staticNextUsers = StrUtil.split( MapUtil.getStr( cacheSubmitMapParam,"staticNextUser" ),"#" );
                String[] staticNextUserName = StrUtil.split( MapUtil.getStr( cacheSubmitMapParam,"staticNextUserName" ),"#" );
                for ( int i = 0,cnt = staticNextUsers.length;i < cnt;i++ ){
                    String[] staticNextUserItems = StrUtil.split( staticNextUsers[i],"," );
                    String[] staticNextUserNameItems = StrUtil.split( staticNextUserName[i],"," );
                    for ( int k = 0,num = staticNextUserItems.length;k < num;k++ ){
                        log.warn( "流程下一步回调循环输出候选人中文名称Assignee：【{}】>>>>staticNextUserItems：【{}】>>>>>【{}】",actTaskInstModel.getAssignee(),staticNextUserItems[k],StrUtil.equals( actTaskInstModel.getAssignee(),staticNextUserItems[k] ) );
                        if ( StrUtil.equals( actTaskInstModel.getAssignee(),staticNextUserItems[k] ) ){
                            actTaskInstModel.setAssigneeName( staticNextUserNameItems[k] );
                        }
                    }
                }
            }
            actTaskInstModel.setCreator( actTaskInstModel.getAssignee() );
            actTaskInstModel.setModifier( actTaskInstModel.getAssignee() );
            actTaskInstModel = actTaskInstModelMapper.save(actTaskInstModel);
            //以下是推送统一待办
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
            actTaskInstModel.setEndTime(LocalDateTime.now());
            actTaskInstModel.setEnabled(true);
            actTaskInstModel.setModifier(actTaskInstModel.getAssignee() );
            actTaskInstModel.setModifiedTime(LocalDateTime.now());
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
    public List<ActTaskInstModel> getByProcessInstIdAndTaskDefinitionKey ( String processInstId, String taskDefinitionKey,String orgCode ) {
        try {
            List<ActTaskInstModel> actTaskInstModels = actTaskInstModelMapper.getByProcessInstIdAndTaskDefinitionKey( processInstId,taskDefinitionKey,orgCode );
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
            return actTaskInstModelMapper.queryTaskInstModelByProcessInstIdAndEnabledOrderByEndTimeAsc( processInstId,Boolean.TRUE );
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return null;
    }
}
