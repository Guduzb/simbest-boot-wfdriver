package com.simbest.boot.wfdriver.process.operate;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.github.wenhao.jpa.Specifications;
import com.google.common.collect.Maps;
import com.simbest.boot.util.json.JacksonUtils;
import com.simbest.boot.wf.process.service.IWorkItemService;
import com.simbest.boot.wfdriver.api.CallFlowableProcessApi;
import com.simbest.boot.wfdriver.exceptions.FlowableDriverBusinessException;
import com.simbest.boot.wfdriver.exceptions.WorkFlowBusinessRuntimeException;
import com.simbest.boot.wfdriver.process.bussiness.service.IActBusinessStatusService;
import com.simbest.boot.wfdriver.process.listener.model.ActTaskInstModel;
import com.simbest.boot.wfdriver.process.listener.service.IActCommentModelService;
import com.simbest.boot.wfdriver.process.listener.service.IActTaskInstModelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    public static Map<String,Object> cacheSubmitMapParam = CollectionUtil.newHashMap();

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
            actBusinessStatusService.updateActBusinessStatusData( processInstId,currentUserCode );
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
        Boolean isSign = MapUtil.getBool( nextParam,"isSign" );
        Boolean isFinallySign = MapUtil.getBool( nextParam,"isFinallySign" );
        cacheSubmitMapParam.put( "staticNextUserName",nextUserName );
        cacheSubmitMapParam.put( "staticNextUser",nextUser );
        log.warn( "正常打印打印流程下一步提交的候选中文名称：【{}】",JacksonUtils.obj2json( cacheSubmitMapParam ) );
        List<String> nextUserOrgCodes = null;
        List<String> nextUserPostIds = null;
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
                callFlowableProcessApi.tasksAddComment(taskAddCommentMap);
                actCommentModelService.create(currentUserCode,message,processInstId,taskId,null);
            }

            Map<String,Object> tasksCompleteMap = Maps.newHashMap();
            List<Map<String,Object>> participantIdentitys = Lists.newArrayList();
            String[] nextUsers = StrUtil.split( nextUser,"#" );
            String[] inputUserParams = StrUtil.split( MapUtil.getStr( nextParam,"inputUserParams" ),"#" );
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
                        callFlowableProcessApi.tasksComplete( taskId, tasksCompleteMap );
                    }else{
                        callFlowableProcessApi.finshTask( taskId );
                    }
                }else {
                    if ( StrUtil.equals( nextActivityParamItems.get( 2 ), "end" ) ) {     //结束环节
                        tasksCompleteMap.clear( );
                        tasksCompleteMap.put( "outcome", outcome );
                        tasksCompleteMap.put( "fromTaskId", taskId );
                        tasksCompleteMap.put( "tenantId", "anddoc" );
                        tasksCompleteMap.put( "processDefinitionId", processDefinitionId );
                        callFlowableProcessApi.tasksComplete( taskId, tasksCompleteMap );
                    }
                    if ( StrUtil.equals( nextActivityParamItems.get( 2 ), "one" ) ) {     //单人单任务
                        tasksCompleteMap.clear( );
                        tasksCompleteMap.put( "outcome", outcome );
                        tasksCompleteMap.put( inputUserParams[ 0 ], nextUsers[ 0 ] );
                        String participantIdentity = nextUsers[ 0 ].concat( "#" ).concat( nextUserOrgCode ).concat( "#" ).concat( nextUserPostId );
                        tasksCompleteMap.put( "participantIdentity", participantIdentity );
                        tasksCompleteMap.put( "fromTaskId", taskId );
                        tasksCompleteMap.put( "tenantId", "anddoc" );
                        tasksCompleteMap.put( "processDefinitionId", processDefinitionId );
                        callFlowableProcessApi.tasksComplete( taskId, tasksCompleteMap );
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
                        callFlowableProcessApi.createTaskEntityImpls( nextUserItems, nextActivityParamItems.get( 1 ), nextActivityParamItems.get( 0 ), processInstId, processDefinitionId, tasksCompleteMap );

                        //再完成当前task
                        if ( taskFlag ) {
                            taskFlag = Boolean.FALSE;
                            callFlowableProcessApi.finshTask( taskId );
                        }
                    }
                }
            }
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
    public List<Map<String, Object>> queryWorkITtemDataMap ( Map<String, Object> paramMap ) {
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

    /**
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

    public static void main ( String[] args ) {
        List<String> nextActivityParams = StrUtil.splitTrim( ",,one", '#' );
        //下面会把空 空串去掉
        List<String> nextActivityParamItems = StrUtil.splitTrim( nextActivityParams.get( 0 ), ',' );
        System.out.println( nextActivityParamItems.size() );
    }
}
