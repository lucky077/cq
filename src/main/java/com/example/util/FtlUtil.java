package com.example.util;

import com.example.Demo;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.ui.freemarker.SpringTemplateLoader;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FtlUtil {

    private static Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);
    static{

        DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader();
        //String path = Demo.class.getClassLoader().getResource("template").getPath();
        try {
            cfg.setTemplateLoader(new SpringTemplateLoader(defaultResourceLoader,"template/"));
            //cfg.setDirectoryForTemplateLoading(new File(path));
        } catch (Exception e) {
            try {
                cfg.setDirectoryForTemplateLoading(new File("C:\\Users\\Administrator\\Desktop\\酷Q Pro\\app\\com.sobte.cqp.com.sobte.cqp.jcq\\app\\template"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
        cfg.setDefaultEncoding("UTF-8");
    }

    public static String render(String templateName,Object data){
        Template template = null;
        try {
            template = cfg.getTemplate(templateName + ".ftl");
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Writer out = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));

        try {
            template.process(data, out);
        } catch (TemplateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String result = new String(byteArrayOutputStream.toByteArray());
        return result;
    }

}
