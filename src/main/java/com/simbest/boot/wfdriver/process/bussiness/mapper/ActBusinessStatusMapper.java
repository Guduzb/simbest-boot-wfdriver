package com.simbest.boot.wfdriver.process.bussiness.mapper;

import com.simbest.boot.base.repository.GenericRepository;
import com.simbest.boot.wfdriver.process.bussiness.model.ActBusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;


/**
 *@ClassName ActBusinessStatusMapper
 *@Description
 *@Author Administrator
 *@Date 2019/12/01 16:03
 *@Version 1.0
 **/
@Repository
public interface ActBusinessStatusMapper extends GenericRepository<ActBusinessStatus,String> {

    /**
     * 根据流程实例ID 查询业务流程操作信息
     * @param processInstID     流程实例ID
     * @return
     */
    String sql1 = "SELECT * FROM act_business_status t WHERE t.PROCESS_INST_ID = ?1 and ENABLED=1";
    @Query(value = sql1,nativeQuery = true)
    ActBusinessStatus getByProcessInst(String processInstID);

    /**
     * 根据父流程实例ID 查询最新的业务流程操作信息
     * @param parentProcessInstId       父流程实例ID
     * @return
     */
    String sql9 = "select * from act_business_status act where act.parent_proc_id=?1 and act.previous_assistant is not null";
    @Query(value = sql9,nativeQuery = true)
    List<ActBusinessStatus> getByProcessInstIdDoneNew(String parentProcessInstId);

    /**
     * 根据父流程实例ID查询业务流程操作信息
     * @param parentProcessInstId  父流程实例ID
     * @return
     */
    List<ActBusinessStatus> getActBusinessStatusByParentProcessInstId(String parentProcessInstId);

    String sql7 = "update act_business_status t set ENABLED=0 WHERE t.PROCESS_INST_ID = ?1";
    @Modifying
    @Query(value = sql7,nativeQuery = true)
    int updateActBusDataByProInsId(String processInstID);

    /**
     *  获取指定 userName 下面所有的待办数据
     * @param participant    参与者
     * @return
     */
     String sql4 = "SELECT act .*," +
                            "       task.task_Id       as taskId,"+
                            "       task.name     as taskName,"+
                            "       task.task_Definition_Key     as taskDefKey,"+
                            "       task.assignee as taskAssignee"+
                            "  FROM act_business_status act,"+
                            "       flowable_task_inst_model      task"+
                            " WHERE act.process_inst_id = task.process_Inst_Id"+
                            "   AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')"+
                            "   AND task.end_Time is null " +
                            "   AND act.enabled = 1"+
                            "   AND task.assignee = :participant"+
                            " ORDER BY task.created_Time desc";
    String sql4Count = "SELECT count(1)"+
                            "  FROM act_business_status act,"+
                            "       flowable_task_inst_model      task"+
                            " WHERE act.process_inst_id = task.process_Inst_Id"+
                            "   AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')"+
                            "   AND task.end_Time is null " +
                            "   AND act.enabled = 1"+
                            "   AND task.assignee = :participant"+
                            " ORDER BY task.created_Time desc";
    @Query(value = sql4,countQuery = sql4Count,nativeQuery = true)
    Page<Map<String,Object>> getTodoByUserPage(@Param(value = "participant") String participant, @Param(value = "dynamicWhere") String dynamicWhere, Pageable pageable);

    /**
     *  获取指定 userName 下面涉及的所有文种  公文使用
     * @param participant    参与者
     * @return
     */
    String sq21 = "SELECT ubpd.id as BO_PROCESS_DEFID,ubpd.bo_process_def_name as BO_PROCESS_DEFNAME" +
            "  FROM us_bo_process_definition ubpd" +
            "    join us_bo_process_instance ubi" +
            "     on ubpd.id = ubi.bo_porcess_def_id" +
            "     and ubpd.enabled=1" +
            "     and ubi.enabled=1 " +
            "     and ubi.CURRENT_STATE <> '100'" +
            "    join act_business_status act" +
            "     on ubi.id = act.business_key" +
            "     and act.enabled=1 " +
            "    join flowable_task_inst_model task" +
            "     on act.process_inst_id = task.process_Inst_Id" +
            "     and task.enabled=1 " +
            "     AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')" +
            "     AND task.end_Time is null" +
            "     AND task.assignee = :participant" +
            "   group by ubpd.id,ubpd.bo_process_def_name,ubpd.global_display_order" +
            "   order by ubpd.global_display_order asc";
    @Query(value = sq21,nativeQuery = true)
    List<Map<String,Object>> getAnddocTodoByUserGroup(@Param(value = "participant") String participant, @Param(value = "dynamicWhere") String dynamicWhere);

    /**
     *  获取指定 userName 下面所有的待办数据无分页  公文使用
     * @param participant    参与者
     * @return
     */
    String sq22 = "SELECT act.id, act.business_key as businessKey, act.create_org_code as createOrgCode, act.create_org_name as createOrgName,act.create_user_name as createUserName,  " +
            "       act.current_state as currentState, to_char(act.end_time,'yyyy-MM-dd HH24:mi:ss') as endTime, act.parent_process_inst_id as parentProInstId, act.pm_inst_type as pmInstType, act.previous_assistant as previousAssistant,  " +
            "       act.previous_assistant_date as previousAssistantDate, act.previous_assistant_name as previousAssistantName , act.previous_assistant_org_code as previousAssistantOrgCode, act.previous_assistant_org_name as previousAssistantOrgName,  " +
            "       act.process_def_key as processDefKey, act.process_inst_id as processInstId, act.receipt_title as receiptTile,act.receipt_code as receiptCode,to_char(act.start_time,'yyyy-MM-dd HH24:mi:ss') as startTime,act.CREATOR_IDENTITY as creatorIdentity, " +
            "       task.task_Id as taskId,to_char(task.TASK_CREATE_TIME,'yyyy-MM-dd HH24:mi:ss') as taskCreateTime,to_char(task.END_TIME,'yyyy-MM-dd HH24:mi:ss') as taskEndTime,task.PARTICIPANT_IDENTITY as participantIdentity,task.FROM_TASK_ID as fromTaskId,task.name as taskName,task.task_Definition_Key as taskDefKey,task.assignee as taskAssignee,task.PROCESS_DEFINITION_ID as processDefintionId," +
            "       ubpd.id as boProcessDefId,ubpd.bo_process_def_name as boProcessDefName,ubpd.global_display_order as globalDisplayOrder,ubi.FLOW_DIRECTION,ubi.FORM_ID" +
            "  FROM us_bo_process_definition ubpd" +
            "    join us_bo_process_instance ubi" +
            "     on ubpd.id = ubi.bo_porcess_def_id" +
            "     and ubpd.enabled=1" +
            "     and ubi.enabled=1 " +
            "     and ubi.CURRENT_STATE <> '100'" +
            "    join act_business_status act" +
            "     on ubi.id = act.business_key" +
            "     and act.enabled=1 " +
            "    join flowable_task_inst_model task" +
            "     on act.process_inst_id = task.process_Inst_Id" +
            "     and task.enabled=1 " +
            "     AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')" +
            "     AND task.end_Time is null" +
            "     AND task.assignee = :participant" +
            " ORDER BY ubpd.global_display_order asc,task.created_Time desc";
    @Query(value = sq22,nativeQuery = true)
    List<Map<String,Object>> getAnddocTodoByUserNoPage(@Param(value = "participant") String participant, @Param(value = "dynamicWhere") String dynamicWhere);

    /**
     *  获取指定 userName 下面所有的待办数据  公文使用
     * @param participant    参与者
     * @return
     */
    String sql8 = "SELECT act.id, act.business_key as businessKey, act.create_org_code as createOrgCode, act.create_org_name as createOrgName,act.create_user_name as createUserName,  " +
            "       act.current_state as currentState, to_char(act.end_time,'yyyy-MM-dd HH24:mi:ss') as endTime, act.parent_process_inst_id as parentProInstId, act.pm_inst_type as pmInstType, act.previous_assistant as previousAssistant,  " +
            "       act.previous_assistant_date as previousAssistantDate, act.previous_assistant_name as previousAssistantName , act.previous_assistant_org_code as previousAssistantOrgCode, act.previous_assistant_org_name as previousAssistantOrgName,  " +
            "       act.process_def_key as processDefKey, act.process_inst_id as processInstId, act.receipt_title as receiptTile,act.receipt_code as receiptCode,to_char(act.start_time,'yyyy-MM-dd HH24:mi:ss') as startTime,act.CREATOR_IDENTITY as creatorIdentity, " +
            "       task.task_Id as taskId,to_char(task.TASK_CREATE_TIME,'yyyy-MM-dd HH24:mi:ss') as taskCreateTime,to_char(task.END_TIME,'yyyy-MM-dd HH24:mi:ss') as taskEndTime,task.PARTICIPANT_IDENTITY as participantIdentity,task.FROM_TASK_ID as fromTaskId,task.name as taskName,task.task_Definition_Key as taskDefKey," +
            "       task.assignee as taskAssignee,task.PROCESS_DEFINITION_ID as processDefintionId," +
            "       ubpd.id as boProcessDefId,ubpd.bo_process_def_name as boProcessDefName,ubpd.global_display_order as globalDisplayOrder,ubi.FLOW_DIRECTION,ubi.FORM_ID" +
            "  FROM us_bo_process_definition ubpd" +
            "    join us_bo_process_instance ubi" +
            "     on ubpd.id = ubi.bo_porcess_def_id" +
            "     and ubpd.enabled=1" +
            "     and ubi.enabled=1 " +
            "     and ubi.CURRENT_STATE <> '100'" +
            "    join act_business_status act" +
            "     on ubi.id = act.business_key" +
            "     and act.enabled=1 " +
            "    join flowable_task_inst_model task" +
            "     on act.process_inst_id = task.process_Inst_Id" +
            "     and task.enabled=1 " +
            "     AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')" +
            "     AND task.end_Time is null" +
            "     AND task.assignee = :participant" +
            " ORDER BY ubpd.global_display_order asc,task.created_Time desc";
    String sql8Count = "SELECT count(1)" +
            "  FROM us_bo_process_definition ubpd" +
            "    join us_bo_process_instance ubi" +
            "     on ubpd.id = ubi.bo_porcess_def_id" +
            "     and ubpd.enabled=1" +
            "     and ubi.enabled=1 " +
            "     and ubi.CURRENT_STATE <> '100'" +
            "    join act_business_status act" +
            "     on ubi.id = act.business_key" +
            "     and act.enabled=1 " +
            "    join flowable_task_inst_model task" +
            "     on act.process_inst_id = task.process_Inst_Id" +
            "     and task.enabled=1 " +
            "     AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')" +
            "     AND task.end_Time is null" +
            "     AND task.assignee = :participant";
    @Query(value = sql8,countQuery = sql8Count,nativeQuery = true)
    Page<Map<String,Object>> getAnddocTodoByUserPage(@Param(value = "participant") String participant, @Param(value = "dynamicWhere") String dynamicWhere, Pageable pageable);

    /**
     *  获取指定 userName 已办下面涉及的所有文种  公文使用
     * @param participant    参与者
     * @return
     */
    String sq23 = "SELECT ubpd.id as BO_PROCESS_DEFID,ubpd.bo_process_def_name as BO_PROCESS_DEFNAME" +
            "  FROM us_bo_process_definition ubpd" +
            "    join us_bo_process_instance ubi" +
            "     on ubpd.id = ubi.bo_porcess_def_id" +
            "     and ubpd.enabled=1" +
            "     and ubi.enabled=1 " +
            "    join act_business_status act" +
            "     on ubi.id = act.business_key" +
            "     and act.enabled=1 " +
            "    join (select * from FLOWABLE_TASK_INST_MODEL tk where tk.end_time in(select max(t.end_time) from FLOWABLE_TASK_INST_MODEL t where t.assignee=:participant and t.enabled=1 and t.end_time is not null group by t.process_inst_id))task" +
            "     on act.process_inst_id = task.process_Inst_Id" +
            "     and task.enabled=1 " +
            "     AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')" +
            "     AND task.end_Time is not null" +
            "     AND task.assignee = :participant" +
            "   group by ubpd.id,ubpd.bo_process_def_name,ubpd.global_display_order" +
            "   order by ubpd.global_display_order asc";
    @Query(value = sq23,nativeQuery = true)
    List<Map<String,Object>> getByAreadyDoneAssistantGroup(@Param(value = "participant") String participant, @Param(value = "dynamicWhere") String dynamicWhere);


    /**
     *  获取指定 userName 已办下面所有的待办数据无分页  公文使用
     * @param participant    参与者
     * @return
     */
    String sq24 = "SELECT act.id, act.business_key as businessKey, act.create_org_code as createOrgCode, act.create_org_name as createOrgName,act.create_user_name as createUserName,  " +
            "       act.current_state as currentState, to_char(act.end_time,'yyyy-MM-dd HH24:mi:ss') as endTime, act.parent_process_inst_id as parentProInstId, act.pm_inst_type as pmInstType, act.previous_assistant as previousAssistant,  " +
            "       act.previous_assistant_date as previousAssistantDate, act.previous_assistant_name as previousAssistantName , act.previous_assistant_org_code as previousAssistantOrgCode, act.previous_assistant_org_name as previousAssistantOrgName,  " +
            "       act.process_def_key as processDefKey, act.process_inst_id as processInstId, act.receipt_title as receiptTile,act.receipt_code as receiptCode,to_char(act.start_time,'yyyy-MM-dd HH24:mi:ss') as startTime,act.CREATOR_IDENTITY as creatorIdentity, " +
            "       task.task_Id as taskId,to_char(task.TASK_CREATE_TIME,'yyyy-MM-dd HH24:mi:ss') as taskCreateTime,to_char(task.END_TIME,'yyyy-MM-dd HH24:mi:ss') as taskEndTime,task.PARTICIPANT_IDENTITY as participantIdentity,task.FROM_TASK_ID as fromTaskId,task.name as taskName," +
            "       task.task_Definition_Key as taskDefKey,task.assignee as taskAssignee,task.PROCESS_DEFINITION_ID as processDefintionId," +
            "       ubpd.id as boProcessDefId,ubpd.bo_process_def_name as boProcessDefName,ubpd.global_display_order as globalDisplayOrder,ubi.FLOW_DIRECTION,ubi.FORM_ID" +
            "  FROM us_bo_process_definition ubpd" +
            "    join us_bo_process_instance ubi" +
            "     on ubpd.id = ubi.bo_porcess_def_id" +
            "     and ubpd.enabled=1" +
            "     and ubi.enabled=1 " +
            "    join act_business_status act" +
            "     on ubi.id = act.business_key" +
            "     and act.enabled=1 " +
            "     AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')" +
            "    join (select * from FLOWABLE_TASK_INST_MODEL tk where tk.end_time in(select max(t.end_time) from FLOWABLE_TASK_INST_MODEL t where t.assignee=:participant and t.enabled=1 and t.end_time is not null group by t.process_inst_id))task" +
            "     on act.process_inst_id = task.process_Inst_Id" +
            "     and task.enabled=1 " +
            "     AND task.end_Time is not null" +
            "     AND task.assignee = :participant" +
            " ORDER BY task.end_time desc";
    @Query(value = sq24,nativeQuery = true)
    List<Map<String,Object>> getByAreadyDoneAssistantNoPage(@Param(value = "participant") String participant, @Param(value = "dynamicWhere") String dynamicWhere);


    /**
     * 根据流程实例id获取所有的待办
     * @param processDefId  业务流程实例id
     * @param userName      当前办理人
     * @param dynamicWhere  标题
     * @return
     */
    @Query(
            value = "SELECT act.id, act.business_key as businessKey, act.create_org_code as createOrgCode, act.create_org_name as createOrgName,act.create_user_name as createUserName,  " +
                    "                  act.current_state as currentState, to_char(act.end_time,'yyyy-MM-dd HH24:mi:ss') as endTime, act.parent_process_inst_id as parentProInstId, act.pm_inst_type as pmInstType, act.previous_assistant as previousAssistant,  " +
                    "                   act.previous_assistant_date as previousAssistantDate, act.previous_assistant_name as previousAssistantName , act.previous_assistant_org_code as previousAssistantOrgCode, act.previous_assistant_org_name as previousAssistantOrgName,   " +
                    "                   act.process_def_key as processDefKey, act.process_inst_id as processInstId, act.receipt_title as receiptTile,act.receipt_code as receiptCode,to_char(act.start_time,'yyyy-MM-dd HH24:mi:ss') as startTime,act.CREATOR_IDENTITY as creatorIdentity,  " +
                    "                   task.task_Id as taskId,to_char(task.TASK_CREATE_TIME,'yyyy-MM-dd HH24:mi:ss') as taskCreateTime,to_char(task.END_TIME,'yyyy-MM-dd HH24:mi:ss') as taskEndTime,task.PARTICIPANT_IDENTITY as participantIdentity,task.FROM_TASK_ID as fromTaskId,task.name as taskName,task.task_Definition_Key as taskDefKey, " +
                    "                   task.assignee as taskAssignee,task.PROCESS_DEFINITION_ID as processDefintionId, " +
                    "                  ubpd.id as boProcessDefId,ubpd.bo_process_def_name as boProcessDefName,ubpd.global_display_order as globalDisplayOrder,ubi.FLOW_DIRECTION,ubi.FORM_ID  " +
                    "              FROM us_bo_process_definition ubpd " +
                    "                join us_bo_process_instance ubi " +
                    "                 on ubpd.id = ubi.bo_porcess_def_id " +
                    "                 and ubpd.enabled=1 " +
                    "                 and ubi.enabled=1  " +
                    "                 and ubi.CURRENT_STATE <> '100' " +
                    "                 join act_business_status act " +
                    "                 on ubi.id = act.business_key  " +
                    "                 and act.enabled=1  " +
                    "                 join flowable_task_inst_model task " +
                    "                 on act.process_inst_id = task.process_Inst_Id " +
                    "                 and task.enabled=1  " +
                    "                 AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%') " +
                    "                 AND task.end_Time is null " +
                    "                 AND task.assignee like concat(concat('%', :userName), '%') " +
                    "                 and ubpd.id = :boProcessDefId " +
                    "                 and act.receipt_title is not null  " +
                    "              ORDER BY ubpd.global_display_order asc,task.created_Time desc",
            countQuery = "SELECT  count(1) "+
                    "              FROM us_bo_process_definition ubpd " +
                    "                join us_bo_process_instance ubi " +
                    "                 on ubpd.id = ubi.bo_porcess_def_id " +
                    "                 and ubpd.enabled=1 " +
                    "                 and ubi.enabled=1  " +
                    "                 and ubi.CURRENT_STATE <> '100' " +
                    "                 join act_business_status act " +
                    "                 on ubi.id = act.business_key  " +
                    "                 and act.enabled=1  " +
                    "                 join flowable_task_inst_model task " +
                    "                 on act.process_inst_id = task.process_Inst_Id " +
                    "                 and task.enabled=1  " +
                    "                 AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%') " +
                    "                 AND task.end_Time is null " +
                    "                 AND task.assignee like concat(concat('%', :userName), '%') " +
                    "                 and ubpd.id = :boProcessDefId " +
                    "                 and act.receipt_title is not null  " +
                    "              ORDER BY ubpd.global_display_order asc,task.created_Time desc",
            nativeQuery = true
    )
    Page<Map<String , Object>> getAllByBoProcessDefId( @Param("boProcessDefId") String boProcessDefId , @Param("userName") String userName , @Param("dynamicWhere") String dynamicWhere, Pageable pageable);

    /**
     *  获取指定 userName 下面所有的已办数据
     * @param assistant     完成人
     * @return
     */
    String sql5 = "SELECT " +
                    " act.*,task.task_Id as taskId," +
                    "task.name as taskName," +
                    "task.assignee as taskAssignee " +
                    "FROM  act_business_status act, flowable_task_inst_model task "+
                    "WHERE act.process_inst_id = task.process_Inst_Id " +
                    "AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%') " +
                    "AND task.end_Time is not null " +
                    "AND task.assignee = :assistant "+
                    "AND act.enabled = 1 " +
                    "ORDER BY task.end_time desc";
    String sql5Count = "SELECT count(1)" +
                    "FROM  act_business_status act, flowable_task_inst_model task "+
                    "WHERE act.process_inst_id = task.process_Inst_Id " +
                    "AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%') " +
                    "AND task.end_Time is not null " +
                    "AND task.assignee = :assistant "+
                    "AND act.enabled = 1 ";
    @Query(value = sql5,countQuery = sql5Count,nativeQuery = true)
    Page<Map<String,Object>> getByAreadyDoneAssistantPage(@Param(value = "assistant") String assistant, @Param(value = "dynamicWhere") String dynamicWhere, Pageable pageable);


    /**
     *  获取指定 userName 下面所有的已办数据   公文使用
     * @param assistant     完成人
     * @return
     */
    String sql10 = "SELECT act.id, act.business_key as businessKey, act.create_org_code as createOrgCode, act.create_org_name as createOrgName,act.create_user_name as createUserName,  " +
            "       act.current_state as currentState, to_char(act.end_time,'yyyy-MM-dd HH24:mi:ss') as endTime, act.parent_process_inst_id as parentProInstId, act.pm_inst_type as pmInstType, act.previous_assistant as previousAssistant,  " +
            "       act.previous_assistant_date as previousAssistantDate, act.previous_assistant_name as previousAssistantName , act.previous_assistant_org_code as previousAssistantOrgCode, act.previous_assistant_org_name as previousAssistantOrgName,  " +
            "       act.process_def_key as processDefKey, act.process_inst_id as processInstId, act.receipt_title as receiptTile,act.receipt_code as receiptCode,to_char(act.start_time,'yyyy-MM-dd HH24:mi:ss') as startTime, act.CREATOR_IDENTITY as creatorIdentity," +
            "       task.task_Id as taskId,to_char(task.TASK_CREATE_TIME,'yyyy-MM-dd HH24:mi:ss') as taskCreateTime,to_char(task.END_TIME,'yyyy-MM-dd HH24:mi:ss') as taskEndTime,task.PARTICIPANT_IDENTITY as participantIdentity,task.FROM_TASK_ID as fromTaskId,task.name as taskName," +
            "       task.task_Definition_Key as taskDefKey,task.assignee as taskAssignee,task.PROCESS_DEFINITION_ID as processDefintionId,ubpd.id as boProcessDefId,ubpd.bo_process_def_name as boProcessDefName,ubpd.global_display_order as globalDisplayOrder,ubi.FLOW_DIRECTION,ubi.FORM_ID" +
            "  FROM us_bo_process_definition ubpd" +
            "    join us_bo_process_instance ubi" +
            "     on ubpd.id = ubi.bo_porcess_def_id" +
            "     and ubpd.enabled=1" +
            "     and ubi.enabled=1 " +
            "    join act_business_status act" +
            "     on ubi.id = act.business_key" +
            "     and act.enabled=1 " +
            "    join (select * from FLOWABLE_TASK_INST_MODEL tk where tk.end_time in(select max(t.end_time) from FLOWABLE_TASK_INST_MODEL t where t.assignee=:participant and t.enabled=1 and t.end_time is not null group by t.process_inst_id))task" +
            "     on act.process_inst_id = task.process_Inst_Id" +
            "     and task.enabled=1 " +
            "     AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')" +
            "     AND task.end_Time is not null" +
            "     AND task.assignee = :participant" +
            //" ORDER BY ubpd.global_display_order asc,task.created_Time desc";
            " ORDER BY task.end_time desc";
    String sql10Count = "SELECT count(1)" +
            "  FROM us_bo_process_definition ubpd" +
            "    join us_bo_process_instance ubi" +
            "     on ubpd.id = ubi.bo_porcess_def_id" +
            "     and ubpd.enabled=1" +
            "     and ubi.enabled=1 " +
            "    join act_business_status act" +
            "     on ubi.id = act.business_key" +
            "     and act.enabled=1 " +
            "    join (select * from FLOWABLE_TASK_INST_MODEL tk where tk.end_time in(select max(t.end_time) from FLOWABLE_TASK_INST_MODEL t where t.assignee=:participant and t.enabled=1 and t.end_time is not null group by t.process_inst_id))task" +
            "     on act.process_inst_id = task.process_Inst_Id" +
            "     and task.enabled=1 " +
            "     AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')" +
            "     AND task.end_Time is not null" +
            "     AND task.assignee = :participant";
    @Query(value = sql10,countQuery = sql10Count,nativeQuery = true)
    Page<Map<String,Object>> getAnddocDoneAssistantPage(@Param(value = "participant") String participant, @Param(value = "dynamicWhere") String dynamicWhere, Pageable pageable);

    /**
     * 获取指定 userName 下面所有的创建的工单数据
     * @param creator     创建人
     * @return
     */
    String sql6 = " SELECT DISTINCT act_business_status.*"+
            " FROM act_business_status"+
            " where act_business_status.receipt_title like concat(concat('%',:dynamicWhere),'%')"+
            " and act_business_status.enabled = 1"+
            " and act_business_status.create_User_Id = :creator"+
            " order by act_business_status.start_Time desc";
    String sql6Count = " SELECT count(1)"+
            " FROM act_business_status"+
            " where act_business_status.RECEIPT_TITLE like concat(concat('%',:dynamicWhere),'%')"+
            " and act_business_status.enabled = 1"+
            " and act_business_status.create_User_Id = :creator"+
            " order by act_business_status.start_Time desc";
    @Query(value = sql6,countQuery = sql6Count,nativeQuery = true)
    Page<Map<String,Object>> getMyCreateDataPage(@Param(value = "creator") String creator, @Param(value = "dynamicWhere") String dynamicWhere, Pageable pageable);

    /**
     * 根据流程实例ID更新流程状态
     */
    String sq20 = "update act_business_status t set CURRENT_STATE=7 WHERE ENABLED=1 and t.END_TIME=sysdate and t.PROCESS_INST_ID = :#{#actBusinessStatus.processInstId}";
    @Modifying
    @Query(value = sq20,nativeQuery = true)
    int updatePorcessStateByProInstId(ActBusinessStatus actBusinessStatus);

    /**
     * 功能描述: 更新根据ID更新主单据信息
     *
     * @param
     * @return
     * @date 2020/2/20 11:51
     * @auther ljw
     */
    String sq25 = "update us_bo_process_instance t set CURRENT_STATE=:currentState WHERE ENABLED=1 and t.id = :id";
    @Modifying
    @Query(value = sq25,nativeQuery = true)
    int updateBoProcessInstById(@Param(value = "id")String id,@Param(value = "currentState")String currentState);

    /**
     * 功能描述: 根据流程实例ID更新更新标题信息
     *
     * @param
     * @return
     * @date 2020/2/20 11:51
     * @auther ljw
     */
    String sq26 = "update act_business_status t set RECEIPT_TITLE=?2 WHERE ENABLED=1 and t.PROCESS_INST_ID = ?1";
    @Modifying
    @Query(value = sq26,nativeQuery = true)
    int updateTitleByProcessInstById(String processInstId,String title);

    /**
     * 查询所有工单信息  公文使用
     * @param participant    参与者
     * @return
     */
    String sql27 = "SELECT act.id, act.business_key as businessKey, act.create_org_code as createOrgCode, act.create_org_name as createOrgName,act.create_user_name as createUserName,  " +
            "       act.current_state as currentState, to_char(act.end_time,'yyyy-MM-dd HH24:mi:ss') as endTime, act.parent_process_inst_id as parentProInstId, act.pm_inst_type as pmInstType, act.previous_assistant as previousAssistant,  " +
            "       act.previous_assistant_date as previousAssistantDate, act.previous_assistant_name as previousAssistantName , act.previous_assistant_org_code as previousAssistantOrgCode, act.previous_assistant_org_name as previousAssistantOrgName,  " +
            "       act.process_def_key as processDefKey, act.process_inst_id as processInstId, act.receipt_title as receiptTile,act.receipt_code as receiptCode,to_char(act.start_time,'yyyy-MM-dd HH24:mi:ss') as startTime,act.CREATOR_IDENTITY as creatorIdentity, " +
            "       task.task_Id as taskId,to_char(task.TASK_CREATE_TIME,'yyyy-MM-dd HH24:mi:ss') as taskCreateTime,to_char(task.END_TIME,'yyyy-MM-dd HH24:mi:ss') as taskEndTime,task.PARTICIPANT_IDENTITY as participantIdentity,task.FROM_TASK_ID as fromTaskId,task.name as taskName,task.task_Definition_Key as taskDefKey," +
            "       task.assignee as taskAssignee,task.PROCESS_DEFINITION_ID as processDefintionId," +
            "       ubpd.id as boProcessDefId,ubpd.bo_process_def_name as boProcessDefName,ubpd.global_display_order as globalDisplayOrder,ubi.FLOW_DIRECTION,ubi.FORM_ID" +
            "  FROM us_bo_process_definition ubpd" +
            "    join us_bo_process_instance ubi" +
            "     on ubpd.id = ubi.bo_porcess_def_id" +
            "     and ubpd.enabled=1" +
            "     and ubi.enabled=1 " +
            "    join act_business_status act" +
            "     on ubi.id = act.business_key" +
            "     and act.enabled=1 " +
            "     AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')" +
            "     and act.current_state=:currentState" +
            "    join flowable_task_inst_model task" +
            "     on act.process_inst_id = task.process_Inst_Id" +
            "     and task.enabled=1 " +
            "     AND task.assignee = :participant" +
            " ORDER BY ubpd.global_display_order asc,task.created_Time desc";
    String sql27Count = "SELECT count(1)" +
            "  FROM us_bo_process_definition ubpd" +
            "    join us_bo_process_instance ubi" +
            "     on ubpd.id = ubi.bo_porcess_def_id" +
            "     and ubpd.enabled=1" +
            "     and ubi.enabled=1 " +
            "    join act_business_status act" +
            "     on ubi.id = act.business_key" +
            "     and act.enabled=1 " +
            "     AND act.receipt_title LIKE concat(concat('%', :dynamicWhere), '%')" +
            "     and act.current_state=:currentState" +
            "    join flowable_task_inst_model task" +
            "     on act.process_inst_id = task.process_Inst_Id" +
            "     and task.enabled=1 " +
            "     AND task.assignee = :participant";
    @Query(value = sql27,countQuery = sql27Count,nativeQuery = true)
    Page<Map<String,Object>> getAnddocAllByUserPage(@Param(value = "participant") String participant, @Param(value = "dynamicWhere") String dynamicWhere,@Param(value = "currentState") String currentState, Pageable pageable);
}
