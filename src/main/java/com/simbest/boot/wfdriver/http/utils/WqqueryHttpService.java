package com.simbest.boot.wfdriver.http.utils;

import com.mzlion.easyokhttp.HttpClient;
import com.mzlion.easyokhttp.response.HttpResponse;
import com.simbest.boot.base.exception.Exceptions;
import com.simbest.boot.util.DateUtil;
import com.simbest.boot.util.json.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class WqqueryHttpService {
    private static final Logger log = LoggerFactory.getLogger(WqqueryHttpService.class);

    @Autowired
    private HttpConfig httpConfig;

    private  String SOURCESYSTEMID = "";
    private String SOURCESYSTEMNAME = "";
    private String token = "";
    private String wfengineHost = "";

    @PostConstruct
    public void loadConfig(){
        SOURCESYSTEMID = httpConfig.sourcesystemid;
        SOURCESYSTEMNAME = httpConfig.sourcesystemname;
        token = httpConfig.accesstoken;
        wfengineHost = httpConfig.wfengineHost;
    }

    /**
     * Json 参数调用
     * @param url 接口url
     * @param jsonParam json参数
     * @throws BadCallException
     */
    public Map<String,Object> callInterfaceJson(String url, String jsonParam)  {

        Date date = DateUtil.getCurrent();
        String SUBMITDATE = DateUtil.getTimestamp(date);
        String TIMESTAMP = String.valueOf(date.getTime()/1000);
        String ACCESSTOKEN = Md5Token.MD5(token+TIMESTAMP);


        Map m =  (Map)JacksonUtils.json2obj(jsonParam, Map.class);
        m.put("SUBMITDATE",SUBMITDATE);
        m.put("SOURCESYSTEMID",SOURCESYSTEMID);
        m.put("SOURCESYSTEMNAME",SOURCESYSTEMNAME);
        jsonParam   =JacksonUtils.obj2json(m);

        Map<String,Object> map = new HashMap<String, Object>();
        try {
            JsonResponse jsonResponse = (JsonResponse)HttpClient
                    .textBody(wfengineHost + url)
                    .json(jsonParam)
                    .header("TIMESTAMP",TIMESTAMP)
                    .header("ACCESSTOKEN",ACCESSTOKEN)
                    .charset("utf-8")
                    .asBean(JsonResponse.class);
            if (!jsonResponse.getErrcode().equals(JsonResponse.ERROR_CODE)) {
                log.error(SOURCESYSTEMID+"调用"+url+"失败！");
                map.put("state",ConstantsUtils.FAILE);
            } else if (jsonResponse.getErrcode().equals(JsonResponse.SUCCESS_CODE)) {
                log.info(SOURCESYSTEMID+"调用"+url+"成功！");
                map.put("state",ConstantsUtils.SUCCESS);
            }
            map.put("message",jsonResponse.getMessage());
            map.put("data",jsonResponse.getData());
        }catch(Exception e){
            map.put("state",ConstantsUtils.FAILE);
            map.put("message",ConstantsUtils.ERRORMES);
            log.error(Exceptions.getStackTraceAsString(e));
        }
        return map;
    }

    /**
     * 普通字符传参数调用
     * @param url 接口url
     * @param stringParam 字符传参数
     * @throws BadCallException
     */
    public Map<String,Object> callInterfaceString( String url, Map<String, String> stringParam) {
            Date date = DateUtil.getCurrent();
            String SUBMITDATE = DateUtil.getTimestamp(date);
            String TIMESTAMP = String.valueOf(date.getTime()/1000);
            String ACCESSTOKEN = Md5Token.MD5(token+TIMESTAMP);

        Map<String,Object> map = new HashMap<String,Object>();
        try {
            JsonResponse jsonResponse = (JsonResponse)HttpClient
                    .post(wfengineHost + url)
                    .param("SUBMITDATE",SUBMITDATE)
                    .param("SOURCESYSTEMID",SOURCESYSTEMID)
                    .param("SOURCESYSTEMNAME",SOURCESYSTEMNAME)
                    .param(stringParam)
                    .header("TIMESTAMP",TIMESTAMP)
                    .header("ACCESSTOKEN",ACCESSTOKEN)
                    .asBean(JsonResponse.class);
            if (jsonResponse.getErrcode().equals(JsonResponse.ERROR_CODE)) {
                log.error(SOURCESYSTEMID+"调用"+url+"失败！");
                map.put("state",ConstantsUtils.FAILE);
            } else if (jsonResponse.getErrcode().equals(JsonResponse.SUCCESS_CODE)) {
                log.info(SOURCESYSTEMID+"调用"+url+"成功！");
                map.put("state",ConstantsUtils.SUCCESS);
            }
            map.put("message",jsonResponse.getMessage());
            map.put("data",jsonResponse.getData());
        }catch(Exception e){
            map.put("state",ConstantsUtils.FAILE);
            map.put("message",ConstantsUtils.ERRORMES);
            log.error(Exceptions.getStackTraceAsString(e));
        }
        return map;
    }

    /**
     * 文件上传接口
     * @param url 接口url
     * @param stringParam 字符传参数
     * @throws BadCallException
     */
    public Map<String,Object> callInterfaceFile(String url, File file, String filename, Map<String, String> stringParam) {
        Date date = DateUtil.getCurrent();
        String SUBMITDATE = DateUtil.getTimestamp(date);
        String TIMESTAMP = String.valueOf(date.getTime()/1000);
        String ACCESSTOKEN = Md5Token.MD5(token+TIMESTAMP);

        Map<String,Object> map = new HashMap<String,Object>();
        try {
            JsonResponse jsonResponse = (JsonResponse)HttpClient
                    .post(wfengineHost + url)
                    .param("SUBMITDATE",SUBMITDATE)
                    .param("SOURCESYSTEMID",SOURCESYSTEMID)
                    .param("SOURCESYSTEMNAME",SOURCESYSTEMNAME)
                    .param(stringParam)
                    .param("filename",filename)
                    .param("file",file)
                    .header("TIMESTAMP",TIMESTAMP)
                    .header("ACCESSTOKEN",ACCESSTOKEN)
                    .asBean(JsonResponse.class);
            if (jsonResponse.getErrcode().equals(JsonResponse.ERROR_CODE)) {
                log.error(SOURCESYSTEMID+"调用"+url+"失败！");
                map.put("state",ConstantsUtils.FAILE);
            } else if (jsonResponse.getErrcode().equals(JsonResponse.SUCCESS_CODE)) {
                log.info(SOURCESYSTEMID+"调用"+url+"成功！");
                map.put("state",ConstantsUtils.SUCCESS);
            }
            map.put("message",jsonResponse.getMessage());
            map.put("data",jsonResponse.getData());
        }catch(Exception e){
            map.put("state",ConstantsUtils.FAILE);
            map.put("message",ConstantsUtils.ERRORMES);
            log.error(Exceptions.getStackTraceAsString(e));
        }
        return map;
    }

    /**
     * 普通字符传参数调用
     * @param url 接口url
     * @throws BadCallException
     */
    public HttpResponse callInterfaceOutPut(String url, String processDefinitionId, String processInstanceId) {
        Date date = DateUtil.getCurrent();
        String SUBMITDATE = DateUtil.getTimestamp(date);
        String TIMESTAMP = String.valueOf(date.getTime()/1000);
        String ACCESSTOKEN = Md5Token.MD5(token+TIMESTAMP);
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpClient.get(wfengineHost+url)
                    .header( "Content-type", MediaType.IMAGE_PNG_VALUE )
                    .header("TIMESTAMP",TIMESTAMP)
                    .header("ACCESSTOKEN",ACCESSTOKEN)
                    .queryString("SUBMITDATE",SUBMITDATE)
                    .queryString("SOURCESYSTEMID",SOURCESYSTEMID)
                    .queryString("SOURCESYSTEMNAME",SOURCESYSTEMNAME)
                    .queryString("processInstanceId",processInstanceId)
                    .queryString("processDefinitionId",processDefinitionId)
                    .queryString("tenantId",SOURCESYSTEMID)
                    .queryString("key",processDefinitionId)
                    .execute();

        }catch(Exception e){
            log.error(Exceptions.getStackTraceAsString(e));
        }
        return httpResponse;
    }
}
