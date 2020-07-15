package com.simbest.boot.wfdriver.process.operate;

import cn.hutool.core.codec.Caesar;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.wenhao.jpa.PredicateBuilder;
import com.github.wenhao.jpa.Specifications;
import com.google.common.collect.Maps;
import com.simbest.boot.base.exception.Exceptions;
import com.simbest.boot.util.json.JacksonUtils;
import com.simbest.boot.util.redis.RedisUtil;
import com.simbest.boot.wf.process.service.IWorkItemService;
import com.simbest.boot.wfdriver.api.CallFlowableProcessApi;
import com.simbest.boot.wfdriver.constants.ProcessConstants;
import com.simbest.boot.wfdriver.exceptions.FlowableDriverBusinessException;
import com.simbest.boot.wfdriver.exceptions.WorkFlowBusinessRuntimeException;
import com.simbest.boot.wfdriver.process.bussiness.service.IActBusinessStatusService;
import com.simbest.boot.wfdriver.process.listener.model.ActTaskInstModel;
import com.simbest.boot.wfdriver.process.listener.service.IActCommentModelService;
import com.simbest.boot.wfdriver.process.listener.service.IActTaskInstModelService;
import com.simbest.boot.wfdriver.util.ErrorDealUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import sun.nio.cs.ext.TIS_620;

import java.util.*;

/**
 *@ClassName WorkTaskManager
 *@Description 任务接口实现
 *@Author Administrator
 *@Date 2019/12/01 16:03
 *@Version 1.0
 **/
@Slf4j
@Service(value = "workItemManager")
public class WorkTaskManager implements IWorkItemService {

    @Autowired
    private IActBusinessStatusService actBusinessStatusService;

    @Autowired
    private CallFlowableProcessApi callFlowableProcessApi;

    @Autowired
    private IActCommentModelService actCommentModelService;

    @Autowired
    private IActTaskInstModelService actTaskInstModelService;

    /**
     * 完成指定工作项并携带流程相关数据（提交下一步）
     * @param workItemId            工作项ID
     * @param param                 流程相关数据
     * @param transactionSpan       是否启用分割事务 true:启用 false:不启用
     */
    @Override
    public long finishWorkItemWithRelativeData ( long workItemId, Map<String, Object> param, boolean transactionSpan ) {
        return 0;
    }


    /**
     * 完成指定工作项并携带流程相关数据（提交下一步）（Activity6）
     * @param param                 流程相关数据
     */
    @Override
    public int finishWorkTaskWithRelativeData ( Map<String, Object> param) {
        int ret = 0;
        String currentUserCode = MapUtil.getStr( param,"currentUserCode" );
        String taskId = MapUtil.getStr( param, "taskId" );
        String outcome = MapUtil.getStr( param, "outcome" );
        String message = MapUtil.getStr( param, "message" );
        String processInstId = MapUtil.getStr( param, "processInstId" );
        String nextUser = MapUtil.getStr( param, "nextUser" );
        String nextUserOrgCode = MapUtil.getStr( param, "nextUserOrgCode" );
        String nextUserPostId =  MapUtil.getStr( param, "nextUserPostId" );
        String inputUserId = null;
        List<String> inputUserIds = null;
        List<String> nextUserOrgCodes = null;
        List<String> nextUserPostIds = null;
        try {
            Map<String,String> taskAddCommentMap = Maps.newHashMap();
            taskAddCommentMap.put("currentUserCode",currentUserCode);
            taskAddCommentMap.put("taskId",  taskId);
            taskAddCommentMap.put("processInstanceId", processInstId);
            taskAddCommentMap.put("comment",message);
            //保存流程审批意见
            if ( StrUtil.isNotEmpty( message ) ){   //审批意见不为空时调用流程api接口
                callFlowableProcessApi.tasksAddComment(taskAddCommentMap);
                actCommentModelService.create(currentUserCode,message,processInstId,taskId,null);
            }
            Map<String,Object> tasksCompleteMap = Maps.newHashMap();
            String[] nextUsers = StrUtil.split( nextUser,"#" );
            tasksCompleteMap.put( "outcome",outcome );
            if ( !StrUtil.isBlankIfStr( nextUsers ) && nextUsers.length == 1 ){
                String inputUserParams = MapUtil.getStr( param,"inputUserParams" );
                List<String> nextUserItems = StrUtil.splitTrim( nextUsers[0],"," );
                String participantIdentity = null;
                if ( CollectionUtil.isNotEmpty( nextUserItems ) && nextUserItems.size() > 1 ){
                    tasksCompleteMap.put( inputUserParams,nextUserItems);

                    List<Map<String,Object>> participantIdentitys = Lists.newArrayList();
                    nextUserOrgCodes =  Arrays.asList(StrUtil.split( nextUserOrgCode,"," ));
                    nextUserPostIds =  Arrays.asList(StrUtil.split( nextUserPostId,"," ));
                    for ( int i = 0,count = nextUserItems.size();i < count;i++ ){
                        String participantIdentityTmp = nextUserItems.get( i ).concat( "#" ).concat( nextUserOrgCodes.get( i ) ).concat( "#" ).concat( nextUserPostIds.get( i) );
                        Map<String,Object> map = Maps.newConcurrentMap();
                        map.put( nextUserItems.get( i ),participantIdentityTmp );
                        participantIdentitys.add( map );
                    }
                    tasksCompleteMap.put( "participantIdentitys", JacksonUtils.obj2json( participantIdentitys ) );
                }else{
                    tasksCompleteMap.put( inputUserParams,nextUsers[0]);

                    participantIdentity = nextUsers[0].concat( "#" ).concat( nextUserOrgCode ).concat( "#" ).concat( nextUserPostId );
                    tasksCompleteMap.put( "participantIdentity",participantIdentity );
                }
            }
            if ( !StrUtil.isBlankIfStr( nextUsers ) && nextUsers.length > 1 ){
                String[] inputUserParams = StrUtil.split( MapUtil.getStr( param,"inputUserParams" ),"#" );
                for(int i = 0,count = nextUsers.length;i < count;i++){
                    List<String> nextUserItems = StrUtil.splitTrim( nextUsers[i],"," );
                    tasksCompleteMap.put( inputUserParams[i], nextUserItems);
                }
                List<Map<String,Object>> participantIdentitys = Lists.newArrayList();
                nextUserOrgCodes =  Arrays.asList(StrUtil.split( nextUserOrgCode,"#" ));
                nextUserPostIds =  Arrays.asList(StrUtil.split( nextUserPostId,"#" ));
                for ( int i = 0,count = nextUsers.length;i < count;i++ ){
                    List<String> nextUserItems = StrUtil.splitTrim( nextUsers[i],"," );
                    String[] nextUserOrgCodeTmps = StrUtil.split(nextUserOrgCodes.get( i ),",");
                    String[] nextUserPostIdTmps = StrUtil.split(nextUserPostIds.get( i ),",");
                    for ( int k = 0,cnt = nextUserItems.size();k < cnt;k++ ){
                        String participantIdentityTmp = nextUserItems.get( k ).concat( "#" ).concat( nextUserOrgCodeTmps[k] ).concat( "#" ).concat( nextUserPostIdTmps[k] );
                        Map<String,Object> map = Maps.newConcurrentMap();
                        map.put( nextUserItems.get( k ),participantIdentityTmp );
                        participantIdentitys.add( map );
                    }
                }
                tasksCompleteMap.put( "participantIdentitys", JacksonUtils.obj2json( participantIdentitys ) );
            }
            tasksCompleteMap.put( "fromTaskId",taskId );
            callFlowableProcessApi.tasksComplete(taskId,tasksCompleteMap);
            Map<String,Object> nextParamMap = Dict.create()
                    .set( "processInstanceId",processInstId );
            actBusinessStatusService.updateActBusinessStatusData( nextParamMap );
            ret = 1;
        }catch ( Exception e){
            FlowableDriverBusinessException.printException( e );
            throw new WorkFlowBusinessRuntimeException("Exception Cause is submit workItem data failure,code:WF000002");
        }
        return ret;
    }

    /**
     * 功能描述:  根据环节配置的属性进行流转下一步
     *
     * @param
     * @return
     * @date 2020/2/29 11:43
     * @auther ljw
     */
    @Override
    public int finshTaskWithComplete(Map<String,Object> nextParam){
        int ret = 0;
        Map<String,Object> cacheSubmitMapParam = CollectionUtil.newHashMap();
        String flowDirection = MapUtil.getStr( nextParam,"flowDirection" );
        String currentUserCode = MapUtil.getStr( nextParam,"currentUserCode" );
        String taskId = MapUtil.getStr( nextParam, "taskId" );
        String outcome = MapUtil.getStr( nextParam, "outcome" );
        String message = MapUtil.getStr( nextParam, "message" );
        String processInstId = MapUtil.getStr( nextParam, "processInstId" );
        String nextUser = MapUtil.getStr( nextParam, "nextUser" );
        String nextUserName = MapUtil.getStr( nextParam, "nextUserName" );
        String nextUserOrgCode = MapUtil.getStr( nextParam, "nextUserOrgCode" );
        String nextUserPostId =  MapUtil.getStr( nextParam, "nextUserPostId" );
        String processDefinitionId = MapUtil.getStr( nextParam,"processDefinitionId" );
        String nextActivityParam = MapUtil.getStr( nextParam,"taskDefinitionKey" );   //每一个 defid,defname,oen/multi,
        String receipTitle = MapUtil.getStr(nextParam, "receipTitle" );
        Boolean isSign = MapUtil.getBool( nextParam,"isSign" );
        Boolean isFinallySign = MapUtil.getBool( nextParam,"isFinallySign" );
        String sourceTaskDefinitionKey = MapUtil.getStr( nextParam,"sourceTaskDefinitionId" );
        cacheSubmitMapParam.put( "staticNextUserName",nextUserName );
        cacheSubmitMapParam.put( "staticNextUser",nextUser );
        StringBuilder countUserKye = new StringBuilder();
        countUserKye.append( flowDirection ).append( "_" ).append( processInstId ).append( "_" ).append( nextUser );
        RedisUtil.setBean( countUserKye.toString(),1 );
        RedisUtil.setBean( processInstId.concat( ProcessConstants.PROCESS_SUBMIT_REDIS_SUFFIX ),cacheSubmitMapParam );
        log.warn( "正常打印打印流程下一步提交的候选中文名称：【{}】",JacksonUtils.obj2json( cacheSubmitMapParam ) );
        List<String> nextUserOrgCodes;
        List<String> nextUserPostIds;
        Map<String,Object> resMap = MapUtil.newHashMap();
        try {
            nextUserOrgCodes =  Arrays.asList(StrUtil.split( nextUserOrgCode,"#" ));
            nextUserPostIds =  Arrays.asList(StrUtil.split( nextUserPostId,"#" ));
            //保存流程审批意见
            Map<String,String> taskAddCommentMap = Maps.newHashMap();
            taskAddCommentMap.put("currentUserCode",currentUserCode);
            taskAddCommentMap.put("taskId",  taskId);
            taskAddCommentMap.put("processInstanceId", processInstId);
            taskAddCommentMap.put("comment",message);
            if ( StrUtil.isNotEmpty( message ) ){   //审批意见不为空时调用流程api接口
                resMap = callFlowableProcessApi.tasksAddComment(taskAddCommentMap);
                if ( ErrorDealUtil.getErrorCode( resMap) < 0  ){
                    return ret;
                }
                actCommentModelService.create(currentUserCode,message,processInstId,taskId,null);
            }

            Map<String,Object> tasksCompleteMap = Maps.newHashMap();
            List<Map<String,Object>> participantIdentitys = CollectionUtil.newArrayList();
            String[] nextUsers = StrUtil.split( nextUser,"#" );
            String[] inputUserParams = StrUtil.split( MapUtil.getStr( nextParam,"inputUserParams" ),"#" );
            String[] outcomes = StrUtil.split( outcome,"#" );
            List<String> nextActivityParams = StrUtil.splitTrim( nextActivityParam, '#' );
            Boolean taskFlag  = Boolean.TRUE;

            for ( int i = 0,cnt = nextActivityParams.size();i < cnt;i++ ){
                List<String> nextActivityParamItems = StrUtil.split( nextActivityParams.get( i ), ',' );
                if ( isSign ){
                    tasksCompleteMap.clear( );
                    tasksCompleteMap.put( "outcome", outcome );
                    tasksCompleteMap.put( "fromTaskId", taskId );
                    tasksCompleteMap.put( "tenantId", "anddoc" );
                    tasksCompleteMap.put( "processDefinitionId", processDefinitionId );
                    tasksCompleteMap.put( inputUserParams[ 0 ], nextUsers[ 0 ] );
                    String participantIdentity = nextUsers[ 0 ].concat( "#" ).concat( nextUserOrgCode ).concat( "#" ).concat( nextUserPostId );
                    tasksCompleteMap.put( "participantIdentity", participantIdentity );
                    if ( isFinallySign ){
                        resMap = callFlowableProcessApi.tasksComplete( taskId, tasksCompleteMap );
                    }else{
                        callFlowableProcessApi.finshTask( taskId );
                    }
                }else {
                    if ( StrUtil.equals( nextActivityParamItems.get( 2 ), "end" ) ) {     //结束环节
                        tasksCompleteMap.clear( );
                        tasksCompleteMap.put( "outcome", outcomes[i] );
                        tasksCompleteMap.put( "fromTaskId", taskId );
                        tasksCompleteMap.put( "tenantId", "anddoc" );
                        tasksCompleteMap.put( "processDefinitionId", processDefinitionId );
                        resMap = callFlowableProcessApi.tasksComplete( taskId, tasksCompleteMap );
                    }
                    if ( StrUtil.equals( nextActivityParamItems.get( 2 ), "one" ) ) {     //单人单任务
                        tasksCompleteMap.clear( );
                        tasksCompleteMap.put( "outcome", outcomes[i] );
                        tasksCompleteMap.put( inputUserParams[ i ], nextUsers[ i ] );
                        String participantIdentity = nextUsers[ i ].concat( "#" ).concat( nextUserOrgCode ).concat( "#" ).concat( nextUserPostId );
                        tasksCompleteMap.put( "participantIdentity", participantIdentity );
                        tasksCompleteMap.put( "fromTaskId", taskId );
                        tasksCompleteMap.put( "tenantId", "anddoc" );
                        tasksCompleteMap.put( "processDefinitionId", processDefinitionId );
                        resMap = callFlowableProcessApi.tasksComplete( taskId, tasksCompleteMap );
                    }
                    if ( StrUtil.equals( nextActivityParamItems.get( 2 ), "multi" ) ) {     //多人单任务
                        tasksCompleteMap.clear( );
                        //先创建多实例的task
                        tasksCompleteMap.put( "fromTaskId", taskId );
                        tasksCompleteMap.put( "tenantId", "anddoc" );
                        tasksCompleteMap.put( "processDefinitionId", processDefinitionId );

                        List<String> nextUserItems = StrUtil.splitTrim( nextUsers[ i ], "," );
                        String[] nextUserOrgCodeTmps = StrUtil.split( nextUserOrgCodes.get( i ), "," );
                        String[] nextUserPostIdTmps = StrUtil.split( nextUserPostIds.get( i ), "," );
                        for ( int k = 0, cnt1 = nextUserItems.size( ); k < cnt1; k++ ) {
                            String participantIdentityTmp = nextUserItems.get( k ).concat( "#" ).concat( nextUserOrgCodeTmps[ k ] ).concat( "#" ).concat( nextUserPostIdTmps[ k ] );
                            Map<String, Object> map = Maps.newConcurrentMap( );
                            map.put( nextUserItems.get( k ), participantIdentityTmp );
                            participantIdentitys.add( map );
                        }
                        tasksCompleteMap.put( "participantIdentitys", JacksonUtils.obj2json( participantIdentitys ) );
                        callFlowableProcessApi.createTaskEntityImpls( sourceTaskDefinitionKey,nextUserItems, nextActivityParamItems.get( 1 ), nextActivityParamItems.get( 0 ), processInstId, processDefinitionId,"anddoc",tasksCompleteMap );

                        //再完成当前task
                        if ( taskFlag && !StrUtil.containsIgnoreCase( nextActivityParam,"one" )) {
                            taskFlag = Boolean.FALSE;
                            callFlowableProcessApi.finshTask( taskId );
                        }
                    }
                    if ( StrUtil.equals( nextActivityParamItems.get( 2 ), "addTask" ) ) {     //多人单任务
                        tasksCompleteMap.clear( );
                        //先创建多实例的task
                        tasksCompleteMap.put( "fromTaskId", taskId );
                        tasksCompleteMap.put( "tenantId", "anddoc" );
                        tasksCompleteMap.put( "processDefinitionId", processDefinitionId );

                        List<String> nextUserItems = StrUtil.splitTrim( nextUsers[ i ], "," );
                        String[] nextUserOrgCodeTmps = StrUtil.split( nextUserOrgCodes.get( i ), "," );
                        String[] nextUserPostIdTmps = StrUtil.split( nextUserPostIds.get( i ), "," );
                        for ( int k = 0, cnt1 = nextUserItems.size( ); k < cnt1; k++ ) {
                            String participantIdentityTmp = nextUserItems.get( k ).concat( "#" ).concat( nextUserOrgCodeTmps[ k ] ).concat( "#" ).concat( nextUserPostIdTmps[ k ] );
                            Map<String, Object> map = Maps.newConcurrentMap( );
                            map.put( nextUserItems.get( k ), participantIdentityTmp );
                            participantIdentitys.add( map );
                        }
                        tasksCompleteMap.put( "participantIdentitys", JacksonUtils.obj2json( participantIdentitys ) );
                        callFlowableProcessApi.createTaskEntityImpls( sourceTaskDefinitionKey,nextUserItems, nextActivityParamItems.get( 1 ), nextActivityParamItems.get( 0 ), processInstId, processDefinitionId,"anddoc",tasksCompleteMap );
                    }
                    if ( StrUtil.equals( nextActivityParamItems.get( 2 ), "sign" ) ) {    //流程会签
                        tasksCompleteMap.put( "fromTaskId", taskId );
                        tasksCompleteMap.put( "tenantId", "anddoc" );
                        tasksCompleteMap.put( "processDefinitionId", processDefinitionId );
                        tasksCompleteMap.put( "outcome", outcomes[i] );

                        List<String> nextUserItems = StrUtil.splitTrim( nextUsers[ i ], "," );
                        tasksCompleteMap.put( inputUserParams[i], nextUserItems);
                        String[] nextUserOrgCodeTmps = StrUtil.split( nextUserOrgCodes.get( i ), "," );
                        String[] nextUserPostIdTmps = StrUtil.split( nextUserPostIds.get( i ), "," );
                        for ( int k = 0, cnt1 = nextUserItems.size( ); k < cnt1; k++ ) {
                            String participantIdentityTmp = nextUserItems.get( k ).concat( "#" ).concat( nextUserOrgCodeTmps[ k ] ).concat( "#" ).concat( nextUserPostIdTmps[ k ] );
                            Map<String, Object> map = Maps.newConcurrentMap( );
                            map.put( nextUserItems.get( k ), participantIdentityTmp );
                            participantIdentitys.add( map );
                        }
                        tasksCompleteMap.put( "participantIdentitys", JacksonUtils.obj2json( participantIdentitys ) );
                        resMap = callFlowableProcessApi.tasksComplete( taskId, tasksCompleteMap );
                    }
                    if ( StrUtil.equals( nextActivityParamItems.get( 2 ), "finish" ) ) {    //结束当前任务，不流程下一步
                        tasksCompleteMap.put( "fromTaskId", taskId );
                        tasksCompleteMap.put( "tenantId", "anddoc" );
                        tasksCompleteMap.put( "processDefinitionId", processDefinitionId );

                        callFlowableProcessApi.finshTask( taskId );
                    }
                }
            }
            if ( ErrorDealUtil.getErrorCode( resMap) < 0  ){
                return ret;
            }
            Map<String,Object> nextParamMap = Dict.create()
                    .set( "processInstanceId",processInstId )
                    .set( "receipTitle",receipTitle );
            actBusinessStatusService.updateActBusinessStatusData( nextParamMap );
            ret = 1;
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
            throw new WorkFlowBusinessRuntimeException("Exception Cause is submit workItem data failure,code:WF000002");
        }
        return ret;
    }

    /**
     * 提交流程审批意见   (bps使用)
     * @param taskId            工作项ID
     * @param approvalMsg           审批意见信息
     */
    @Override
    public int submitApprovalMsg ( long taskId, String approvalMsg ) {
        return 0;
    }

    /**
     * 改派工作项  BPS使用接口
     * @param workItemId            工作项ID
     * @param reassignUsers         改派的人
     * @return
     */
    @Override
    public int reassignWorkItem ( long workItemId, List<Object> reassignUsers ) {
        return 0;
    }

    /**
     * 改派规工作项 activity使用接口
     * @param map
     * @return
     */
    @Override
    public int reassignWorkByActivity(Map<String, Object> map) {
        int i = 0 ;
        try {
            //分发新的环节
            i = this.sendOtherProcess(map);
            //结束当前工作项
            this.endTask(map);
            //删除当前环节的流程明细信息
            List<String> taskIds = Lists.newArrayList();
            taskIds.add(MapUtil.getStr(map , "taskId"));
            boolean ret = actTaskInstModelService.deleteAllByTaskIds(taskIds);
            i = ret ? 1 : 0 ;
        }catch (Exception e) {
            Exceptions.printException(e);
        }
        return i;
    }

    @Override
    public Object getByProInstIdAAndAInstId ( Long processInstID, String activityDefID ) {
        return null;
    }

    /**
     * 根据流程实例ID查询工作项信息  流程跟踪   (BPS和Activity6共用)
     * @param processInsId        流程实例ID
     * @return
     */
    @Override
    public List<?> queryWorkTtemDataByProInsId ( long processInsId ) {
        return actTaskInstModelService.queryTaskInstModelByProcessInstId(processInsId+"");
    }

    /**
     * 根据流程实例ID查询工作项信息  流程跟踪   (Flowable共用)
     * @param processInsId        流程实例ID
     * @return
     */
    @Override
    public List<?> queryWorkTtemDataByProInsId ( Object processInsId ) {
        List<ActTaskInstModel> actTaskInstModels = actTaskInstModelService.queryTaskInstModelByProcessInstId((String)processInsId);
        return actTaskInstModels;
    }

    @Override
    public List<?> queryWorkItems ( long processInstId, long workItemId ) {
        return null;
    }

    @Override
    public List<Map<String, Object>> queryWorkITtemDataMap ( Map<String, Object> paramMap ) {return null;}

    /**
     * 根据流程实例ID查询工作项信息  流程跟踪
     * @param paramMap      参数
     * @return
     */
    @Override
    public Object queryTaskDataMap ( Map<String, Object> paramMap ) {
        try {
            String processInstId = MapUtil.getStr( paramMap,"processInstId" );
            String taskId = MapUtil.getStr( paramMap,"taskId" );
            ActTaskInstModel actTaskInstModel = actTaskInstModelService.getByProcessInstIdAndTaskId(processInstId,taskId);
            return actTaskInstModel;
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return null;
    }

    /**
     * 根据流程实例ID查询工作项信息 存在子流程  流程跟踪
     * @param parentProcessInsId        父流程实例ID
     * @return
     */
    @Override
    public List<?> queryWorkTtemDataByProInsIdSubFlow ( long parentProcessInsId ) {
        return null;
    }

    /**
     * 根据流程实例ID 删除工作项信息
     * @param processInstID  流程实例ID
     * @return
     */
    @Override
    public int deleteByProInsId ( Long processInstID ) {
        return 0;
    }

    /**
     * 根据流程实例ID和环节定义ID更新工作项状态信息
     * @param processInstId             流程实例ID
     * @param activityDefId             环节定义ID
     * @return
     */
    @Override
    public int updateEnabledByProInsIdAndActivityDefId ( Long processInstId, String activityDefId ) {
        return 0;
    }

    @Override
    public void finishWorkItem ( long workItemID, boolean transactionSpan ) {
    }

    @Override
    public Object getWorkItemByWorkItemId ( Long processInsId, Long workItemId ) {
        return null;
    }

    @Override
    public Object getWorkItemByWorkItemId ( Long workItemId ) {
        return null;
    }

    @Override
    public List<?> queryAllByCurrentStateAndeAndEnabledNative ( ) {
        return null;
    }

    @Override
    public Object updateWorkItemInfo ( Map<String, Object> workItem ) {
        return null;
    }

    /**
     * 根据当前操作人查询工作任务  （Activity6）
     * @param queryParam        流程任务查询
     * @return
     */
    @Override
    public List<?> queryPorcessWorkTask ( Map<String, Object> queryParam ) {
        try {
            String processInstId = MapUtil.getStr( queryParam,"processInstId" );
            String taskDefKey = MapUtil.getStr( queryParam,"value" );
            String orgCode = MapUtil.getStr( queryParam,"orgCode" );
            List<ActTaskInstModel> actTaskInstModels = actTaskInstModelService.getByProcessInstIdAndTaskDefinitionKey( processInstId,taskDefKey,orgCode );
            return actTaskInstModels;
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return null;
    }

    /**f
     * 根据工作任务ID结束流程    （Activity6）
     * @param processParam      参数
     */
    @Override
    public void endProcess ( Map<String, Object> processParam ) {
    }

    @Override
    public int updateOptMsgByProInsIdWorkItemId ( Long processInstId, Long workItemId ) {
        return 0;
    }

    /**
     * 领取任务  （Activity6）
     * @param taskId        任务ID
     * @param userId        领取人
     * @return
     */
    @Override
    public int claim ( String taskId, String userId ) {
        int ret = 0;
        return ret;
    }

    @Override
    public void endTask(Map<String , Object> processParam) {
        try {
            String taskId = MapUtil.getStr(processParam, "taskId");
            callFlowableProcessApi.finshTask( taskId );
        } catch (Exception e ) {
            Exceptions.printException(e);
        }
    }

    /**
     * 补发 ： 仅仅用于收文流程
     * @param map
     * @return
     */
    @Override
    public Map<String , Object> reissueProcess(Map<String, Object> map) {
        Map<String, Object> resultMap = Maps.newHashMap();
        try {
            //根据流程id和环节名称获取父节点环节id
            String activityDefId = MapUtil.getStr(map, "sourceTaskDefinitionKey");
            String processInstId = MapUtil.getStr(map, "processInstId");
            Specification<ActTaskInstModel> build = Specifications.<ActTaskInstModel>and()
                    .eq("processInstId", processInstId)
                    .eq("enabled", Boolean.TRUE)
                    .eq("taskDefinitionKey", activityDefId)
                    .build();
            Sort sort = Sort.by(Sort.Direction.ASC, "createdTime");
            List<ActTaskInstModel> list = actTaskInstModelService.findAllNoPage(build , sort);
            if (CollectionUtil.isNotEmpty(list)) {
                ActTaskInstModel model = list.get(0);
                map.put("taskId" , model.getTaskId());
                this.sendOtherProcess(map);
                resultMap.put("taskId" , model.getTaskId());
            }
        }catch (Exception e ) {
            Exceptions.printException(e);
        }
        return map ;
    }

    /**
     * 回滚：回滚该环节及其以下所有环节至该环节的上一环节 （activity使用）
     * @param paramMap
     * @return
     */
    @Override
    public Map<String, Object> processGoBack (Map<String, Object> paramMap) {
        Map<String, Object> map = Maps.newHashMap();
        try {
            String processInstId = MapUtil.getStr(paramMap, "processInstId");
            String taskId = MapUtil.getStr(paramMap, "taskId");
            String flowDirection = MapUtil.getStr(paramMap, "flowDirection");
            Specification<ActTaskInstModel> build = Specifications.<ActTaskInstModel>and()
                    .eq("processInstId", processInstId)
                    .eq("enabled", Boolean.TRUE)
                    .build();
            Sort sort = Sort.by(Sort.Direction.ASC, "createdTime");
            List<ActTaskInstModel> list = actTaskInstModelService.findAllNoPage(build , sort);
            if (CollectionUtil.isNotEmpty(list)) {
                Map<String , ActTaskInstModel> actTaskInstModelMap = Maps.newHashMap();
                list.stream().forEach(actTaskInstModel -> {
                    actTaskInstModelMap.put(actTaskInstModel.getTaskId() , actTaskInstModel);
                });
                ActTaskInstModel model = actTaskInstModelMap.get(taskId);
                if (ObjectUtil.isNotEmpty(model)) {
                    ActTaskInstModel formTaskModel = actTaskInstModelMap.get(model.getFromTaskId());
                    if (ObjectUtil.isNotEmpty(formTaskModel)) {
                        //拼接需要回滚的环节的taskId
                        List<String> deleteTaskIds = Lists.newArrayList();
                        String deleteFormTaskIdStr = formTaskModel.getTaskId();
                        list.stream().forEach(actTaskInstModel -> {
                            if (deleteFormTaskIdStr.indexOf(actTaskInstModel.getFromTaskId()) > -1) {
                                deleteFormTaskIdStr.concat(",").concat(actTaskInstModel.getTaskId());
                                deleteTaskIds.add(actTaskInstModel.getTaskId());
                            }
                        });
                        //删除回滚的环节
                        actTaskInstModelService.deleteAllByTaskIds(deleteTaskIds);
                        //封装流转参数
                        String[] participantIdentity = formTaskModel.getParticipantIdentity().split("#");
                        Map<String , Object> sendMap = Dict.create()
                                .set("taskId", formTaskModel.getFromTaskId())
                                .set("nextUsers", participantIdentity[0])
                                .set("nextUserNames", formTaskModel.getAssigneeName())
                                .set("nextUserOrgCodes", participantIdentity[1])
                                .set("nextUserPostIds", participantIdentity[2])
                                .set("sourceTaskDefinitionId", formTaskModel.getTaskDefinitionKey())
                                .set("processDefinitionId", formTaskModel.getProcessDefinitionId())
                                .set("processInstId", processInstId)
                                .set("nextActivityDefName", formTaskModel.getName())
                                .set("nextActivityDefId", formTaskModel.getTaskDefinitionKey())
                                .set("flowDirection", flowDirection);
                        int i = this.sendOtherProcess(sendMap);
                        if (i > 0) {
                            map.put("deleteTaskIds" , deleteTaskIds);
                        }
                    } else {
                        throw new Exception("获取上一环节信息失败！");
                    }
                } else {
                    throw new Exception("查询流程环节信息失败！");
                }
            } else {
                throw new Exception("查询流程信息失败！");
            }
        } catch (Exception e ) {
            Exceptions.printException(e);
        }
        return map;
    }

    /**
     * 功能描述:查询正在运行中的任务实例
     *
     * @param
     * @return
     * @date 2020/4/3 11:58
     * @auther Administrator
     */
    @Override
    public List<?> queryRunningTaskInstModelByProcessInstId ( Map<String, Object> processParamMap ) {
        try {
            String processInstId = MapUtil.getStr( processParamMap,"processInstId" );
            List<ActTaskInstModel> actTaskInstModels = actTaskInstModelService.queryRunningTaskInstModelByProcessInstId( processInstId );
            return actTaskInstModels;
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return null;
    }

    private int sendOtherProcess(Map<String , Object> map) {
        int i = 0 ;
        try {
            Map<String, Object> tasksCompleteMap = Maps.newHashMap();
            String taskId = MapUtil.getStr(map, "taskId");
            String nextUsers = MapUtil.getStr(map, "nextUsers");
            String nextUserNames = MapUtil.getStr(map, "nextUserNames");
            String nextUserOrgCodes = MapUtil.getStr(map, "nextUserOrgCodes");
            String nextUserPostIds = MapUtil.getStr(map, "nextUserPostIds");
            String sourceTaskDefinitionKey = MapUtil.getStr( map,"sourceTaskDefinitionId" );
            String processDefinitionId = MapUtil.getStr(map, "processDefinitionId");
            String processInstId = MapUtil.getStr(map, "processInstId");
            String nextActivityDefName = MapUtil.getStr(map, "nextActivityDefName");
            String nextActivityDefId = MapUtil.getStr(map, "nextActivityDefId");

            Map<String,Object> cacheSubmitMapParam = CollectionUtil.newHashMap();
            String flowDirection = MapUtil.getStr( map,"flowDirection" );
            cacheSubmitMapParam.put( "staticNextUserName",nextUserNames );
            cacheSubmitMapParam.put( "staticNextUser",nextUsers);
            StringBuilder countUserKye = new StringBuilder();
            countUserKye.append( flowDirection ).append( "_" ).append( processInstId ).append( "_" ).append( nextUsers );
            RedisUtil.setBean( countUserKye.toString(),1 );
            RedisUtil.setBean( processInstId.concat( ProcessConstants.PROCESS_SUBMIT_REDIS_SUFFIX ),cacheSubmitMapParam );

            tasksCompleteMap.put( "fromTaskId", taskId );
            tasksCompleteMap.put( "tenantId", "anddoc" );
            tasksCompleteMap.put( "processDefinitionId", processDefinitionId );
            List<String> nextUserItems = StrUtil.splitTrim( nextUsers, "," );
            String[] nextUserOrgCodeTmps = StrUtil.split( nextUserOrgCodes, "," );
            String[] nextUserPostIdTmps = StrUtil.split( nextUserPostIds, "," );
            List<Map<String,Object>> participantIdentitys = CollectionUtil.newArrayList();
            for ( int k = 0, cnt1 = nextUserItems.size( ); k < cnt1; k++ ) {
                String participantIdentityTmp = nextUserItems.get( k ).concat( "#" ).concat( nextUserOrgCodeTmps[ k ] ).concat( "#" ).concat( nextUserPostIdTmps[ k ] );
                Map<String, Object> userMap = Maps.newConcurrentMap();
                map.put( nextUserItems.get( k ), participantIdentityTmp );
                participantIdentitys.add( userMap );
            }
            tasksCompleteMap.put( "participantIdentitys", JacksonUtils.obj2json( participantIdentitys ) );
            //进行分发
            callFlowableProcessApi.createTaskEntityImpls( sourceTaskDefinitionKey,nextUserItems, nextActivityDefName, nextActivityDefId, processInstId, processDefinitionId,"anddoc",tasksCompleteMap );
            i = 1 ;
        } catch (Exception e ) {
            Exceptions.printException(e);
        }
        return i;
    }

    public static void main ( String[] args ) {
        List<String> nextActivityParams = StrUtil.splitTrim( ",,one", '#' );
        //下面会把空 空串去掉
        List<String> nextActivityParamItems = StrUtil.splitTrim( nextActivityParams.get( 0 ), ',' );
        System.out.println( nextActivityParamItems.size() );
    }
}
