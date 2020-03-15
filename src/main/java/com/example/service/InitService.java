package com.example.service;

import com.example.Demo;
import com.example.Variable;
import com.example.annotation.CommandMapping;
import com.example.config.ScheduConfig;
import com.example.model.Message;
import com.example.pojo.MethodInvoker;
import com.example.util.MyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import static com.example.Variable.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Service
public class InitService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ScheduConfig scheduConfig;


    @PostConstruct
    public void postConstruct(){
        CoreService coreService = applicationContext.getBean(CoreService.class);
        Demo.setCoreService(coreService);
        Demo.setApplicationContext(applicationContext);
        allowedGroup.add(492155062l);
        allowedGroup.add(86266257l);

        initCommandMapping();

        MyUtil.async(() -> {
            MyUtil.sleep(5000);
            System.out.println("读取银行商店更新时间");
            scheduConfig.bank();
        });

    }
    @PreDestroy
    public void preDestroy(){
        System.out.println("exit success");
    }

    private void initCommandMapping() {
        Class<CommandService> commandServiceClass = CommandService.class;
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(CommandMapping.class);
        System.out.println("初始化完成");
        Demo.CQ.logInfo("初始化完成","初始化完成");
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = bean.getClass();
            Method[] methods = beanClass.getMethods();
            for (Method method : methods) {
                CommandMapping annotation = AnnotationUtils.findAnnotation(method,CommandMapping.class);
                if (annotation == null) {
                    continue;
                }

                String[] cmdStrs = annotation.value();
                for (String cmdStr : cmdStrs) {
                    if (cmdStr.contains("*")){
                        commandMappingByLike.put(new MethodInvoker(method, bean),cmdStr);
                        cmdStr = cmdStr.replace("*","");
                        commandMapping.put(cmdStr, new MethodInvoker(method, bean));
                    }
                    else{
                        commandMapping.put(cmdStr, new MethodInvoker(method, bean));
                    }
                }

            }

        }


    }

    private void initCommandMappingOrder(){

    }
}
