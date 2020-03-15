package com.example.aop;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Demo;
import com.example.annotation.CommandMapping;
import com.example.annotation.Normal;
import com.example.annotation.Times;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.model.Message;
import com.example.pojo.LastMsgCount;
import com.example.util.FtlUtil;
import com.example.util.MyUtil;
import com.sobte.cqp.jcq.entity.Member;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MemberSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.sql.Time;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.example.Variable.*;
import static com.example.Demo.*;

@Aspect
@Component
@Order(2)
public class TestAspectJ {

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public static boolean isRun = true;



    @Around(value = "@annotation(com.example.annotation.CommandMapping)")
    public Object user(ProceedingJoinPoint point) throws Throwable {


        Object[] args = point.getArgs();
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        Normal normal = method.getAnnotation(Normal.class);

        if (!isRun && normal == null){
            return null;
        }

        CommandMapping commandMapping = method.getAnnotation(CommandMapping.class);
        Times times = method.getAnnotation(Times.class);
        Message message = (Message)args[0];;
        final Long fromQQ = message.getFromQQ();

        String bankBanKey = "bankBan:"+fromQQ;
        String bankBan = stringRedisTemplate.opsForValue().get(bankBanKey);
        if (bankBan != null && normal == null){
            Long expire = stringRedisTemplate.opsForValue().getOperations().getExpire(bankBanKey);
            sendGroupMsg("还有" + (int)(Math.ceil(expire / 60.0)) + "分钟可以从监狱释放");
            return null;
        }

        User user = userMapper.selectById0(fromQQ);
        Member info = getGroupMemberInfo(user.getQq());
        if (info == null){
            return null;
        }
        user.setName(MyUtil.getCardName(info));
        message.setUser(user);


        synchronized (user){
            int tili = commandMapping.tili();
            tili = user.getTili() + tili;
            if (tili < 0){
                sendGroupMsg("体力不足无法行动，需要" + -(tili - user.getTili()) + "，而你只有" + user.getTili());
                return null;
            }

            //限定时间内只能使用限定次数的指令
            if (times != null){
                int limit = times.limit();
                long interval = times.interval();
                String key = "times:" + method.getName() + ":" + fromQQ;
                String val = stringRedisTemplate.opsForValue().get(key);
                if (val == null){
                    stringRedisTemplate.opsForValue().set(key,String.valueOf(1),interval, TimeUnit.SECONDS);
                }else {
                    if (Long.valueOf(val) >= limit){
                        Long expire = stringRedisTemplate.opsForValue().getOperations().getExpire(key);
                        sendGroupMsg(times.tip() + "\n还有" + (int)(Math.ceil(expire / 60.0)) + "分钟可用");
                        return null;
                    }else{
                        stringRedisTemplate.opsForValue().increment(key);
                    }
                }
            }

            final Object oldUser = user.clone();

            Object result = point.proceed();

            if (Objects.equals(result,-1)){
                if (times != null){
                    stringRedisTemplate.opsForValue().decrement("times:" + method.getName() + ":" + fromQQ);
                }
                return null;
            }

            if ( result != null){
                if (result instanceof String){
                    sendGroupMsg(result.toString());
                }else if (result instanceof ModelAndView){
                    ModelAndView modelAndView = ((ModelAndView) result);
                    Map<String, Object> model = modelAndView.getModel();
                    String viewName = modelAndView.getViewName();
                    String msg = FtlUtil.render(viewName, model);
                    Demo.sendGroupMsg(msg);
                }else {
                    String msg = FtlUtil.render(method.getName(), result);
                    Demo.sendGroupMsg(msg);
                }
            }

            user.setTili(tili);

            if (! user.equalsAndSetNull(oldUser)){
                userMapper.updateById0((User) oldUser);
            }

        }

        return null;
    }




}
