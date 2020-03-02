package com.example.util;

import org.springframework.util.StringUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;

public class GoogleApi {

    private static final String PATH = "/gettk.js";

    static ScriptEngine engine = null;

    private static String tkk = getTKK();

    public static String ttsspeed = "1";

    static {
        ScriptEngineManager maneger = new ScriptEngineManager();
        engine = maneger.getEngineByName("javascript");
        FileInputStream fileInputStream = null;
        Reader scriptReader = null;
        try {
            scriptReader = new InputStreamReader(GoogleApi.class.getResourceAsStream(PATH), "utf-8");
            engine.eval(scriptReader);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (scriptReader != null) {
                try {
                    scriptReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public GoogleApi() {

    }


    public static String getTKK()  {

        try {
            String result = H.get("https://translate.google.cn/").exec();
            if (!StringUtils.isEmpty(result)) {
                if (result.indexOf("tkk") > -1) {
                    String matchString = RegularUtil.findMatchString(result, "tkk:.*?',");
                    String tkk = matchString.substring(5, matchString.length() - 2);
                    return tkk;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取 tkk 出错");
        }

        return null;
    }

    public static String getTK(String word, String tkk) {
        String result = null;

        try {
            if (engine instanceof Invocable) {
                Invocable invocable = (Invocable) engine;
                result = (String) invocable.invokeFunction("tk", new Object[]{word, tkk});
            }
        } catch (Exception e) {
            throw new RuntimeException("获取 tk 出错");
        }

        return result;
    }
    public static byte[] translate(String word) {

        String tk = getTK(word, tkk);
        int len = word.length();
        try {
            word = URLEncoder.encode(word, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuffer buffer = new StringBuffer("https://translate.google.cn/translate_tts?ie=UTF-8&client=webapp&prev=input");

        buffer.append("&textlen=" + len);
        buffer.append("&tl=" + "zh-CN");
        buffer.append("&tk=" + tk);
        buffer.append("&q=" + word);
        buffer.append("&ttsspeed=" + ttsspeed);

        byte[] result = H.get(buffer.toString()).exec(byte[].class);

        if (result == null || result.length == 0){
            tkk = getTKK();
            buffer = new StringBuffer("https://translate.google.cn/translate_tts?ie=UTF-8&client=webapp&prev=input");

            buffer.append("&textlen=" + len);
            buffer.append("&tl=" + "zh-CN");
            buffer.append("&tk=" + tk);
            buffer.append("&q=" + word);
            buffer.append("&ttsspeed=" + ttsspeed);
            result = H.get(buffer.toString()).exec(byte[].class);;
            if (result == null || result.length == 0){
               return translate("GOOGLE小姐姐崩溃啦！");
            }
        }

        return result;

    }




}
