package com.example.pojo;

import com.example.Demo;
import com.example.exception.MyException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodInvoker {

    private Method method;

    private Object target;

    public void invoke(Object... args){
        try {
            int length = args.length;
            Class[] parameters = method.getParameterTypes();
            Object[] params = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Class p = parameters[i];
                Object param = null;
                //此参数不为空
                if (length > i && (param = args[i]) != null){
                    if (p == param.getClass() || p == Object.class){
                        params[i] = param;
                    } else if(Number.class.isAssignableFrom(p) && param.getClass() == String.class){
                        if (((String) param).startsWith("[CQ:at")){
                            params[i] = Demo.CC.getAt((String)param);
                            continue;
                        }
                        Method valueOf = p.getMethod("valueOf", String.class);
                        params[i] = valueOf.invoke(null,param);
                    }
                }else {
                    params[i] = null;
                }
            }
            try{
                method.invoke(target,params);
            }catch (Exception e){
                //Demo.CQ.logError("业务内部错误",e.getMessage());
                Demo.CQ.logError("业务内部错误",e.toString());
                //Demo.CQ.logError("业务内部错误", Arrays.asList(e.getStackTrace()).toString());
                e.printStackTrace();
            }

        } catch (Exception e) {
            Demo.CQ.logError("参数错误","");
            throw new MyException(e);
        }

    }

}
