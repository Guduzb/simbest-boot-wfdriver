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
            String[] outcomes = StrUtil.split( outcome,"#" );
            String[] nextUsers = StrUtil.split( nextUser,"#" );
            if ( !StrUtil.isBlankIfStr( outcomes ) && outcomes.length > 1 ){  //多任务
                String participantIdentity = null;
                if ( !StrUtil.isBlankIfStr( outcomes ) && nextUsers.length == 1 ){
                    for(int i = 0,count = outcomes.length;i < count;i++){
                        tasksCompleteMap.put( "outcome_" + i,outcomes[i] );
                        tasksCompleteMap.put( "inputUserId_" + i, nextUsers[0]);
                    }
                    participantIdentity = nextUsers[0].concat( "#" ).concat( nextUserOrgCode ).concat( "#" ).concat( nextUserPostId );
                    tasksCompleteMap.put( "participantIdentity",participantIdentity );
                }
                if ( !StrUtil.isBlankIfStr( outcomes ) && nextUsers.length > 1 ){
                    for(int i = 0,count = outcomes.length;i < count;i++){
                        tasksCompleteMap.put( "outcome_" + i,outcomes[i] );

                        List<String> nextUserItems = StrUtil.splitTrim( nextUsers[i],"," );
                        if ( CollectionUtil.isNotEmpty( nextUserItems ) && nextUserItems.size() > 1 ){
                            tasksCompleteMap.put( "inputUserIds_" + i, nextUserItems);
                        }else{
                            tasksCompleteMap.put( "inputUserId_" + i, nextUsers[i]);
                        }
                    }
                    List<Map<String,Object>> participantIdentitys = Lists.newArrayList();
                    nextUserOrgCodes =  Arrays.asList(StrUtil.split( nextUserOrgCode,"#" ));
                    nextUserPostIds =  Arrays.asList(StrUtil.split( nextUserPostId,"#" ));
                    for ( int i = 0,count = nextUsers.length;i < count;i++ ){
                        String participantIdentityTmp = nextUsers[i].concat( "#" ).concat( nextUserOrgCodes.get( i ) ).concat( "#" ).concat( nextUserPostIds.get( i ) );
                        Map<String,Object> map = Maps.newConcurrentMap();
                        map.put( nextUsers[i],participantIdentityTmp );
                        participantIdentitys.add( map );
                    }
                    tasksCompleteMap.put( "participantIdentitys", JacksonUtils.obj2json( participantIdentitys ) );
                }
            } else {  //单任务，单人单任务，多人单任务
                tasksCompleteMap.put( "outcome",outcomes[0] );
                String participantIdentity = null;
                if ( !StrUtil.isBlankIfStr( outcomes ) && nextUsers.length == 1 ){
                    tasksCompleteMap.put( "inputUserId", nextUsers[0]);
                    participantIdentity = nextUsers[0].concat( "#" ).concat( nextUserOrgCode ).concat( "#" ).concat( nextUserPostId );
                    tasksCompleteMap.put( "participantIdentity",participantIdentity );
                }
                /*多人会签，建议流程图中collection使用变量inputUserIds，迭代使用inputUserId和其他保持一直*/
                if ( !StrUtil.isBlankIfStr( outcomes ) && nextUsers.length > 1 ){
                    tasksCompleteMap.put( "inputUserIds", Arrays.asList(nextUsers));
                    List<Map<String,Object>> participantIdentitys = Lists.newArrayList();
                    nextUserOrgCodes =  Arrays.asList(StrUtil.split( nextUserOrgCode,"#" ));
                    nextUserPostIds =  Arrays.asList(StrUtil.split( nextUserPostId,"#" ));
                    for ( int i = 0,count = nextUsers.length;i < count;i++ ){
                        String participantIdentityTmp = nextUsers[i].concat( "#" ).concat( nextUserOrgCodes.get( i ) ).concat( "#" ).concat( nextUserPostIds.get( i ) );
                        Map<String,Object> map = Maps.newConcurrentMap();
                        map.put( nextUsers[i],participantIdentityTmp );
                        participantIdentitys.add( map );
                    }
                    tasksCompleteMap.put( "participantIdentitys", JacksonUtils.obj2json( participantIdentitys ) );
                }
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
            List<ActTaskInstModel> actTaskInstModels = actTaskInstModelService.getByProcessInstIdAndTaskDefinitionKey( processInstId,taskDefKey );
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

}
