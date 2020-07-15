package com.simbest.boot.wfdriver.process.bussiness.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.simbest.boot.base.service.impl.GenericService;
import com.simbest.boot.security.IOrg;
import com.simbest.boot.security.IUser;
import com.simbest.boot.util.redis.RedisUtil;
import com.simbest.boot.util.security.SecurityUtils;
import com.simbest.boot.wf.process.service.IProcessInstanceService;
import com.simbest.boot.wf.unitfytodo.IProcessTodoDataService;
import com.simbest.boot.wfdriver.constants.AppConstants;
import com.simbest.boot.wfdriver.enums.BoProcessInstStateEnum;
import com.simbest.boot.wfdriver.enums.ProcessSateEnum;
import com.simbest.boot.wfdriver.exceptions.FlowableDriverBusinessException;
import com.simbest.boot.wfdriver.process.bussiness.mapper.ActBusinessStatusMapper;
import com.simbest.boot.wfdriver.process.bussiness.model.ActBusinessStatus;
import com.simbest.boot.wfdriver.process.bussiness.service.IActBusinessStatusService;
import com.simbest.boot.wfdriver.process.listener.model.ActProcessInstModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *@ClassName ActBusinessStatusMapper
 *@Description
 *@Author Administrator
 *@Date 2019/12/01 16:03
 *@Version 1.0
 **/
@Slf4j
@Service
public class ActBusinessStatusService extends GenericService<ActBusinessStatus,String> implements IActBusinessStatusService,IProcessTodoDataService {

    private ActBusinessStatusMapper actBusinessStatusMapper;

    @Autowired
    private IProcessInstanceService processInstanceService;

    @Autowired
    public ActBusinessStatusService(ActBusinessStatusMapper mapper ) {
        super(mapper);
        this.actBusinessStatusMapper = mapper;
    }


    /**
     * 流程开始 往act_business_status表中插入信息
     * @param processInstanceId   流程实例对象ID
     * @param startMap   相关参数
     * @return
     */
    @Transactional (propagation= Propagation.REQUIRES_NEW)
    @Override
	public int saveActBusinessStatusData(String processInstanceId, Map<String, Object> startMap ) {
        int ret = 0;
        try{
            ActBusinessStatus actBusinessStatus = new ActBusinessStatus();
            String currentUserCode = MapUtil.getStr(startMap,"currentUserCode" );
            String receipTitle = MapUtil.getStr(startMap, "receipTitle" );
            Boolean iscg = MapUtil.getBool(startMap, "iscg" );
            String processDefKey = MapUtil.getStr(startMap, "idValue" );
            String orgCode = MapUtil.getStr(startMap, "orgCode" );
            String postId = MapUtil.getStr(startMap, "postId" );
            String businessKey = MapUtil.getStr( startMap, "businessKey" );
            String receiptCode = MapUtil.getStr( startMap, "receiptCode" );
            IUser user = SecurityUtils.getCurrentUser();
            Optional<IOrg> orgOptional = ( Optional<IOrg> ) user.getAuthOrgs().stream().
                                    filter( org -> org.getOrgCode().equals( orgCode ) ).findFirst();
            String createOrgName = "";
            if ( orgOptional.isPresent() ){//如果存在
                createOrgName = orgOptional.get().getOrgName();
            }
            actBusinessStatus.setCreateUserId( currentUserCode );
            actBusinessStatus.setCreateUserName( user.getTruename() );
            actBusinessStatus.setPreviousAssistant(currentUserCode);
            actBusinessStatus.setPreviousAssistantName(user.getTruename());
            actBusinessStatus.setCreateUserId( currentUserCode );
            actBusinessStatus.setCreateOrgCode( orgCode );
            actBusinessStatus.setCreateOrgName( createOrgName );
            actBusinessStatus.setEnabled(true);
            actBusinessStatus.setRemoved(false);
            actBusinessStatus.setReceiptTitle(receipTitle);
            actBusinessStatus.setReceiptCode(receiptCode);
            actBusinessStatus.setIscg(iscg);
            actBusinessStatus.setBusinessKey(businessKey);
            actBusinessStatus.setProcessInstId(processInstanceId);
            actBusinessStatus.setStartTime(LocalDateTime.now());
            actBusinessStatus.setProcessDefKey( processDefKey );
            actBusinessStatus.setCurrentState( ProcessSateEnum.RUNNING.getNum() );
            actBusinessStatus.setPmInstType( StrUtil.sub( receiptCode,0,1 ) );
            actBusinessStatus.setCreatorIdentity( currentUserCode.concat( "#" ).concat( orgCode ).concat( "#" ).concat( postId ) );
            actBusinessStatus = actBusinessStatusMapper.saveAndFlush(actBusinessStatus);
            if ( actBusinessStatus != null ){
                ret = 1;
            }
        }catch(Exception e){
            FlowableDriverBusinessException.printException( e );
        }
		return ret;
	}

    /**
     * 任务提交下一步更新流程状态信息
     * @return
     */
    @Transactional (propagation= Propagation.REQUIRES_NEW)
	@Override
	public int updateActBusinessStatusData( Map<String,Object> nextParam ) {
		int ret = 0;
        try{
            String processInstanceId = MapUtil.getStr( nextParam,"processInstanceId" );
            String receipTitle = MapUtil.getStr( nextParam,"receipTitle" );
            IUser user = SecurityUtils.getCurrentUser();
            ActBusinessStatus actBusinessStatus = getByProcessInst(processInstanceId);
            actBusinessStatus.setPreviousAssistant(user.getUsername());
            actBusinessStatus.setPreviousAssistantDate(LocalDateTime.now());
            actBusinessStatus.setPreviousAssistantName(user.getTruename());
            actBusinessStatus.setPreviousAssistantOrgCode( user.getBelongOrgCode() );
            actBusinessStatus.setPreviousAssistantOrgName( user.getBelongOrgName() );
            actBusinessStatus.setReceiptTitle( receipTitle );
            actBusinessStatus = actBusinessStatusMapper.saveAndFlush(actBusinessStatus);
            if ( actBusinessStatus != null ){
                ret = 1;
            }
        }catch(Exception e){
            FlowableDriverBusinessException.printException( e );
        }
		return ret;
	}

    /**
     * 根据流程实例ID查询流程业务状态信息
     * @param processInstID         流程实例ID
     * @return
     */
    @Override
    public ActBusinessStatus getByProcessInst ( String processInstID ) {
        return actBusinessStatusMapper.getByProcessInst( processInstID );
    }

    /**
     * 流程结束更新结束时间
     * @return
     */
    @Transactional (propagation= Propagation.REQUIRES_NEW)
	@Override
	public int updateListenerByProcess(ActProcessInstModel actProcessInstModel) {
        int ret = 0;
        try{
            ActBusinessStatus o = getByProcessInst(actProcessInstModel.getProcessInstanceId());
            o.setEndTime( LocalDateTime.now());
            Duration duration = Duration.between( o.getEndTime(),o.getStartTime() );
            o.setDuration(duration.toNanos());
            o.setCurrentState( BoProcessInstStateEnum.PROCESS_INST_STATE_END.getValue() );
            //actBusinessStatusMapper.updateBoProcessInstById( o.getBusinessKey(), BoProcessInstStateEnum.PROCESS_INST_STATE_END.getValue() );
            o = actBusinessStatusMapper.saveAndFlush(o);
            if ( o != null ){
                RedisUtil.delete( AppConstants.APP_CODE.concat( "_act" ) );
                ret = 1;
            }
        }catch(Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return ret;
	}

    /**
     * 根据流程实例ID删除业务流程状态数据
     * @param actBusinessStatus
     */
    @Override
    public void deletActBusinessStatusData ( ActBusinessStatus actBusinessStatus ) {
        try {
            actBusinessStatusMapper.delete( actBusinessStatus );
        }catch ( Exception e ){
            FlowableDriverBusinessException.printException( e );
        }
    }

    /**
     * 根据流程实例ID逻辑删除业务状态数据
     * @param processInstID
     * @return
     */
    @Override
    public int updateActBusDataByProInsId ( String processInstID ) {
        int ret  = 0;
        try {
            actBusinessStatusMapper.updateActBusDataByProInsId( processInstID );
            ret = 1;
        }catch ( Exception e ){
            FlowableDriverBusinessException.printException( e );
        }
        return ret;
    }

    /**
     * 功能描述:
     * 根据流程实例ID更新流程的状态
     * @param processInstId        流程实例ID
     * @return
     * @date 2020/2/15 23:50
     * @auther ljw
     */
    @Override
    public int updatePorcessStateByProInstId ( String processInstId ) {
        try {
            ActBusinessStatus actBusinessStatus = new ActBusinessStatus();
            actBusinessStatus.setProcessInstId( processInstId );
            actBusinessStatusMapper.updatePorcessStateByProInstId( actBusinessStatus );
        }catch (Exception e){
            FlowableDriverBusinessException.printException( e );
        }
        return 0;
    }

    /**
     * 功能描述: 根据流程实例ID更新更新标题信息
     *
     * @param
     * @return
     * @date 2020/2/20 11:51
     * @auther ljw
     */
    @Override
    public int updateTitleByProcessInstById ( Map<? extends Object, ? extends Object> paramMap ) {
        int ret = 0;
        try {
            String processInstId = MapUtil.getStr( paramMap,"processInstId" );
            String title = MapUtil.getStr( paramMap,"title" );
            ret = actBusinessStatusMapper.updateTitleByProcessInstById( processInstId,title );
        }catch (Exception e){
            ret = -1;
            FlowableDriverBusinessException.printException( e );
        }
        return ret;
    }

    /**
     * 分页获取所有待办数据
     * @param todoParam   查询待办参数
     * @return
     */
    @Override
    public Page<Map<String, Object>> getAllTodoPage ( Map<?, ?> todoParam, Pageable pageable ) {
        return null;
    }

    @Override
    public Page<Map<String, Object>> getMyCreateAllTodoPage(Map<?, ?> map, Pageable pageable) {
        return null;
    }

    @Override
    public Page<Map<String, Object>> getMyApplyPage(Map<?, ?> map, Pageable pageable) {
        return null;
    }

    @Override
    public List<Map<String, Object>> getAllTodoNoPage(Map<?, ?> map) {
        return null;
    }

    /**
     * 获取指定 userName 下面所有的待办数据
     * @param todoUserParam        查询待办参数
     * @return
     */
    @Override
    public Page<?> getTodoByUserNamePage ( Map<?, ?> todoUserParam, Pageable pageable ) {
        return null;
    }

    @Override
    public List<?> getTodoByUserNameNoPage(Map<?, ?> map) {
        return null;
    }

    /**
     * 获取指定 userName 下面所有的待办办数据(返回为Map类型)
     * @param doneUserParam         查询已办参数
     * @return
     */
    @Override
    public Page<Map<String, Object>> getTodoByUserIdPageMap ( Map<?, ?> doneUserParam, Pageable pageable ) {
        Page<Map<String, Object>> pages = null;
        String participant = (String) doneUserParam.get( "participant" );
        String dynamicWhere = (String) doneUserParam.get( "title" );
        StringBuilder inWhere = new StringBuilder();
        try {
            if ( StringUtils.isEmpty( dynamicWhere )){
                dynamicWhere = "";
            }
            pages = actBusinessStatusMapper.getAnddocTodoByUserPage(participant,dynamicWhere,pageable);
        }catch ( Exception e ){
            FlowableDriverBusinessException.printException( e );
        }
        return pages;
    }

    /**
     * 功能描述:获取指定 userName 下面所有的待办数据无分页
     *
     * @param 
     * @return 
     * @date 2020/2/19 0:00
     * @auther ljw
     */
    @Override
    public List<Map<String, Object>> getTodoByUserIdNoPageMap ( Map<?, ?> doneUserParam ) {
        List<Map<String, Object>> todoBoProcessDefIdList = CollectionUtil.newArrayList();
        String participant = (String) doneUserParam.get( "participant" );
        String dynamicWhere = (String) doneUserParam.get( "title" );
        StringBuilder inWhere = new StringBuilder();
        try {
            if ( StringUtils.isEmpty( dynamicWhere )){
                dynamicWhere = "";
            }
            todoBoProcessDefIdList = actBusinessStatusMapper.getAnddocTodoByUserNoPage(participant,dynamicWhere);
        }catch ( Exception e ){
            FlowableDriverBusinessException.printException( e );
        }
        return todoBoProcessDefIdList;
    }

    /**
     * 功能描述:获取指定 userName 下面涉及的所有文种   公文使用
     *
     * @param
     * @return
     * @date 2020/2/19 0:01
     * @auther ljw
     */
    @Override
    public List<Map<String, Object>> getTodoByUserIdGroupMap ( Map<?, ?> doneUserParam ) {
        List<Map<String, Object>> todoList = CollectionUtil.newArrayList();
        String participant = (String) doneUserParam.get( "participant" );
        String dynamicWhere = (String) doneUserParam.get( "title" );
        StringBuilder inWhere = new StringBuilder();
        try {
            if ( StringUtils.isEmpty( dynamicWhere )){
                dynamicWhere = "";
            }
            todoList = actBusinessStatusMapper.getAnddocTodoByUserGroup(participant,dynamicWhere);
        }catch ( Exception e ){
            FlowableDriverBusinessException.printException( e );
        }
        return todoList;
    }

    /**
     * 获取指定 userName 下面所有的已办数据
     * @param doneUserParam         查询已办参数
     * @return
     */
    @Override
    public Page<?> getAreadyDoneByUserIdPage ( Map<?, ?> doneUserParam, Pageable pageable ) {
        return null;
    }

    /**
     * 获取指定 userName 下面所有的已办数据 存在子流程
     * @param doneUserParam         查询已办参数
     * @return
     */
    @Override
    public Page<?> getAreadyDoneByUserIdSubFlowPage ( Map<?, ?> doneUserParam, Pageable pageable ) {
        return null;
    }

    @Override
    public List<?> getAreadyDoneByUserIdSubFlowNoPage(Map<?, ?> map) {
        return null;
    }

    /**
     * 获取指定 userName 下面所有的已办数据
     * @param doneUserParam         查询已办参数
     * @return
     */
    @Override
    public Page<Map<String, Object>> getAreadyDoneByUserIdPageMap ( Map<?, ?> doneUserParam, Pageable pageable ) {
        Page<Map<String, Object>> pages = null;
        String assistant = (String) doneUserParam.get( "assistant" );
        String dynamicWhere = (String) doneUserParam.get( "title" );
        StringBuilder inWhere = new StringBuilder();
        try {
            if ( StringUtils.isEmpty( dynamicWhere )){
                dynamicWhere = "";
            }
            //pages = actBusinessStatusMapper.getByAreadyDoneAssistantPage( assistant,dynamicWhere,pageable );
            pages = actBusinessStatusMapper.getAnddocDoneAssistantPage( assistant,dynamicWhere,pageable );
        }catch ( Exception e ){
            FlowableDriverBusinessException.printException( e );
        }
        return pages;
    }

    /**
     * 功能描述:获取指定 userName 已办下面涉及的所有文种  公文使用
     *
     * @param 
     * @return 
     * @date 2020/2/19 0:01
     * @auther ljw
     */
    @Override
    public List<Map<String, Object>> getAreadyDoneByUserIdNoPageMap ( Map<?, ?> doneUserParam ) {
        List<Map<String, Object>> doneBoProcessDefIdList = CollectionUtil.newArrayList();
        String participant = (String) doneUserParam.get( "participant" );
        String dynamicWhere = (String) doneUserParam.get( "title" );
        StringBuilder inWhere = new StringBuilder();
        try {
            if ( StringUtils.isEmpty( dynamicWhere )){
                dynamicWhere = "";
            }
            doneBoProcessDefIdList = actBusinessStatusMapper.getByAreadyDoneAssistantNoPage(participant,dynamicWhere);
        }catch ( Exception e ){
            FlowableDriverBusinessException.printException( e );
        }
        return doneBoProcessDefIdList;
    }

    /**
     * 功能描述:获取指定 userName 已办下面所有的待办数据无分页  公文使用
     *
     * @param 
     * @return 
     * @date 2020/2/19 0:01
     * @auther ljw
     */
    @Override
    public List<Map<String, Object>> getAreadyDoneByUserIdGroupMap ( Map<?, ?> doneUserParam ) {
        List<Map<String, Object>> doneList = CollectionUtil.newArrayList();
        String participant = (String) doneUserParam.get( "participant" );
        String dynamicWhere = (String) doneUserParam.get( "title" );
        StringBuilder inWhere = new StringBuilder();
        try {
            if ( StringUtils.isEmpty( dynamicWhere )){
                dynamicWhere = "";
            }
            doneList = actBusinessStatusMapper.getByAreadyDoneAssistantGroup(participant,dynamicWhere);
        }catch ( Exception e ){
            FlowableDriverBusinessException.printException( e );
        }
        return doneList;
    }

    /**
     * 获取指定 userName 下面所有的创建的工单数据
     * @param userParam         userId
     * @return
     */
    @Override
    public Page<?> getMyCreateDataPage ( Map<?, ?> userParam, Pageable pageable ) {
        Page<Map<String, Object>> pages = null;
        String creator = (String) userParam.get( "creator" );
        String dynamicWhere = (String) userParam.get( "title" );
        StringBuilder inWhere = new StringBuilder();
        try {
            if ( StringUtils.isEmpty( dynamicWhere )){
                dynamicWhere = "";
            }
            pages = actBusinessStatusMapper.getMyCreateDataPage( creator,dynamicWhere,pageable );
        }catch ( Exception e ){
            FlowableDriverBusinessException.printException( e );
        }
        return pages;
    }

    /**
     * 获取指定userName下面待阅数据
     * @param paramMap          查询待阅参数
     * @param pageable          分页参数
     * @return
     */
    @Override
    public Page<?> getMyTodoReadByUserNamePage ( Map<?, ?> paramMap, Pageable pageable ) {
        return null;
    }

    /**
     * 获取指定userName下面待阅数据
     * @param paramMap          查询待阅参数
     * @param pageable          分页参数
     * @return
     */
    @Override
    public Page<Map<String, Object>> getMyTodoReadByUserNamePageMap ( Map<?, ?> paramMap, Pageable pageable ) {
        return null;
    }

    /**
     * 获取指定userName下面已阅数据
     * @param paramMap          查询已阅参数
     * @param pageable          分页参数
     * @return
     */
    @Override
    public Page<?> getMyAreadyReadByUserNamePage ( Map<?, ?> paramMap, Pageable pageable ) {
        return null;
    }

    /**
     * 获取指定userName下面已阅数据
     * @param paramMap          查询已阅参数
     * @param pageable          分页参数
     * @return
     */
    @Override
    public Page<Map<String, Object>> getMyAreadyReadByUserNamePageMap ( Map<?, ?> paramMap, Pageable pageable ) {
        return null;
    }

    /**
     * 查询业务系统中所有正在办理的工单数据   综合流程管理平台调用接口
     * @param paramMap          查询参数
     * @param pageable          分页参数
     * @return
     */
    @Override
    public Page<Map<String, Object>> getAllTodoByManagerPage ( Map<?, ?> paramMap, Pageable pageable ) {
        Page<Map<String, Object>> pages = null;
        String participant = MapUtil.getStr( paramMap,"participant" );
        String currentState = MapUtil.getStr( paramMap,"currentState" );
        String dynamicWhere = MapUtil.getStr( paramMap,"title" );
        StringBuilder inWhere = new StringBuilder();
        try {
            if ( StringUtils.isEmpty( dynamicWhere )){
                dynamicWhere = "";
            }
            if ( StrUtil.isEmpty( currentState ) ){
                currentState = "2";
            }
            pages = actBusinessStatusMapper.getAnddocAllByUserPage( participant,dynamicWhere,currentState,pageable );
        }catch ( Exception e ){
            FlowableDriverBusinessException.printException( e );
        }
        return pages;
    }

    /**
     * 根据主单据中的pmInstId查询流程业务操作数据
     * @param pmInsId       主单据中的pmInstId
     * @return
     */
    @Override
    public Object queryActBusinessStatusByPmInstId ( String pmInsId ) throws Exception {
        return null;
    }

    /**
     * 根据流程实例ID查询流程业务操作数据，可以会出现多条数据挂载一个businessKey（目前网络割接在用）
     * @param processInstId    流程实例ID
     * @return
     */
    @Override
    public List<?> queryActBusinessStatusByProInsId ( Long processInstId ) throws Exception {
        return null;
    }

    @Override
    public List<Map<String, Object>> getAllTodoByManagerNoPage() throws Exception {
        return null;
    }
}
