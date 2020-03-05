package com.example.util;

import com.example.annotation.CommandMapping;
import com.example.entity.User;
import com.example.exception.MyException;
import com.example.pojo.MethodInvoker;
import com.sobte.cqp.jcq.entity.Member;
import javafx.scene.input.DataFormat;
import lombok.experimental.UtilityClass;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.Variable.*;

@UtilityClass
public class MyUtil {

    private ExecutorService async = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);
    private ExecutorService sync = Executors.newFixedThreadPool(1);

    private static SimpleDateFormat yymmdd = new SimpleDateFormat("YY-dd-mm");

    public void async(Runnable r){
        async.execute(r);
    }

    public void sync(Runnable r){
        sync.execute(r);
    }

    public boolean equalsAndSetNull(Object obj1,Object obj2,String... exclude){
        List<String> excludeList = Arrays.asList(exclude);
        boolean result = true;
        try {
            Class<?> clazz = obj1.getClass();
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (excludeList.contains(declaredField.getName())){
                    continue;
                }
                declaredField.setAccessible(true);
                Object o1 = declaredField.get(obj1);
                Object o2 = declaredField.get(obj2);
                if (o1.equals(o2)){
                    declaredField.set(obj2,null);
                }else{
                    declaredField.set(obj2,o1);
                    result = false;
                }


            }
        }catch (Exception e){
            System.out.println(obj1);
            System.out.println(obj2);
            throw new MyException(e);
        }
        return result;
    }

    public boolean userListContains(List<User> users,User user){
        for (User user1 : users) {
            if (user.getQq().equals(user1.getQq())){
                return true;
            }
        }
        return false;
    }


    public String getChildMenu(String cd){
        StringBuilder sb = new StringBuilder();

        Collection<MethodInvoker> values = commandMapping.values();
        Set<Method> methodSet = new HashSet<>();
        int i = 0;
        for (MethodInvoker q : values) {
            if (methodSet.contains(q.getMethod())){
                continue;
            }
            CommandMapping annotation = AnnotationUtils.findAnnotation(q.getMethod(), CommandMapping.class);

            String[] menu = annotation.menu();
            for (String s : menu) {
                if (cd.equals(s)){
                    methodSet.add(q.getMethod());
                    if (i++ > 0){
                        sb.append("\n");
                    }
                    sb.append(i)
                            .append(". ").
                                append(arrToString(annotation.value()));
                    if(!annotation.notes().equals(""))
                    sb.append("（" + annotation.notes() + "）");
                }
            }
        }

        return sb.toString();
    }

    private String arrToString(String[] arr){
        if (arr.length == 1){
            return arr[0];
        }
        String str = arr[0];
        for (int i = 1; i < arr.length; i++) {
            str += "/"+arr[i];

        }
        return str;
    }

    public byte[] getHead(Long qq){
        return getHead(qq,false);
    }
    public byte[] getHead(Long qq,boolean s){

        int size = 100;
        if (s){
            size = 640;
        }
       return H.get("http://q1.qlogo.cn/g?b=qq&nk="+qq+"&s=" + size).exec(byte[].class);
    }

    public String getCardName(Member member){

        if (member == null){
            return "已退群人员";
        }

        String name = null;
        name = member.getCard();

        if (StringUtils.isEmpty(name)){
            name = member.getNick();
        }

        return name;
    }

}
