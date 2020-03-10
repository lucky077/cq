package com.example.service;

import com.example.Demo;
import com.example.exception.MyException;
import com.example.model.Message;
import com.example.pojo.LastMsgCount;
import com.example.pojo.MethodInvoker;
import com.example.util.MyUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.Variable.*;
import static com.sobte.cqp.jcq.event.JcqApp.CQ;

@Service
public class CoreService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    //不阻塞cq线程
    public void handleMessage(Message message){
        MyUtil.async(() ->  {
            handleMessage0(message);
        } );
    }
    private void handleMessage0(Message message){
        Long fromGroup = message.getFromGroup();
        //只处理允许的群号
        if (!allowedGroup.contains(fromGroup)){
            //return;
        }
        Long fromQQ = message.getFromQQ();


        LastMsgCount lastMsgCount = lastMsgCountMap.get(fromQQ);
        if (lastMsgCount == null){
            lastMsgCountMap.put(fromQQ,new LastMsgCount(message.getMsg()));
        }else {
            if (lastMsgCount.msg.equals(message.getMsg()) && !fromQQ.equals(3301725802l)){
                if (lastMsgCount.count.incrementAndGet() > 3){
                    String key = "times:banCount:" + fromQQ;
                    String val = stringRedisTemplate.opsForValue().get(key);
                    Integer banCount;
                    if (val == null){
                        banCount = 1;
                    }else {
                        banCount = Integer.valueOf(val);
                    }
                    CQ.setGroupBan(fromGroup,fromQQ,60 * banCount * 2 - 60);
                    stringRedisTemplate.opsForValue().set(key,String.valueOf(banCount + 1),7, TimeUnit.DAYS);
                    return;
                }
            }else {
                lastMsgCount.count.set(1);
                lastMsgCount.msg = message.getMsg();
            }
        }

        fromGroupThreadLocal.set(message.getFromGroup());

        String msg = message.getMsg();
        String[] arr = msg.split(" {1,}");//通过空格分隔，并去除所有空格
        String cmd = "";
        if (arr.length == 0 || "".equals(cmd = arr[0])){
            return;
        }
        Object[] splitParam = new Object[2];
        MethodInvoker methodInvoker = getMethodInvoker(cmd,splitParam);
        //获取指令对应的执行器，如果没有，此消息为普通消息，不处理
        if (methodInvoker == null){
            return;
        }
        Object[] args = null;
        if (arr.length > 1){
            args = new Object[arr.length + 2];
            System.arraycopy(arr,0,args,0,arr.length);
        }else {
            args = new Object[3];
        }
        args[0] = message;

        if (splitParam[1] != null){
            System.arraycopy(args,1,args,2,args.length - 2);
            args[1] = splitParam[1];
        }
        if (splitParam[0] != null){
            System.arraycopy(args,1,args,2,args.length - 2);
            args[1] = splitParam[0];
        }
        try{
            methodInvoker.invoke(args);
        }catch (MyException e){
            e.printStackTrace();
            //Demo.sendGroupMsg("[CQ:face,id=100][CQ:face,id=100][CQ:face,id=100]喵？？");
        }

    }

    private MethodInvoker getMethodInvoker(String cmd,Object[] p){
        MethodInvoker methodInvoker = commandMapping.get(cmd);
        //等值匹配
        if (methodInvoker != null){
            return methodInvoker;
        }

        //模糊匹配
        for (Map.Entry<MethodInvoker,String> entry : commandMappingByLike.entrySet()) {
            String key = entry.getValue();
            MethodInvoker method = entry.getKey();
            char left = key.charAt(0);
            char right = key.charAt(key.length() - 1);
            key = key.replace("*","");
            //不包含，匹配失败
            if ( ! cmd.contains(key)){
                continue;
            }
            if (left == '*' && cmd.endsWith(key)){
                p[0] = cmd.replace(key,"");
                return method;
            }
            if (right == '*' && cmd.startsWith(key)){
                p[0] = cmd.replace(key,"");
                return method;
            }
            if (left == '*' && right == '*'){
                String[] split = cmd.split(key);
                p[0] = split[0];
                p[1] = split[1];
                return method;
            }
        }
        return null;
    }


}
