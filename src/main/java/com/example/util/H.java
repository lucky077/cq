package com.example.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.example.Variable.deleteFiles;

/**
 * 网络请求工具类
 */
public class H {

    private static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(1000).setConnectionRequestTimeout(1000)
            .setSocketTimeout(3000).build();

    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36";

    private static final List<Cookie> COOKIES = new CopyOnWriteArrayList<>();

    private Class<? extends HttpRequestBase> httpType;

    private String uri;

    private String contentType;

    private Map<String,String> headers;

    private  List<Cookie> cookies;

    private Object body;

    private Integer connectTimeout,connectionRequestTimeout,socketTimeout;

    private HttpHost proxyHost;

    public HttpHost getProxyHost() {
        return proxyHost;
    }

    private H(){};
    /**
     * 可能抛出 UnknownHostException(uri错误，本地网络异常)、ConnectTimeoutException(连接超时)、SocketTimeoutException(响应超时)
     * @param returnType 只支持 String.class/byte[].class
     * @param responseStatus Integer对象不能以 -128 ~ 127 之间的常量初始化，否则会破坏Integer缓存。建议直接new Integer(n)
     * @param <T>
     * @return
     */
    public <T> T exec(Class<T> returnType,Integer responseStatus){
        CookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG).setDefaultCookieStore(cookieStore).build();
        HttpRequestBase httpRequest = null;
        try {
            httpRequest = httpType.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        };

        RequestConfig.Builder custom = RequestConfig.custom();
        custom .setConnectTimeout(connectTimeout != null ? connectTimeout : DEFAULT_REQUEST_CONFIG.getConnectTimeout())
                .setConnectionRequestTimeout(connectionRequestTimeout != null ? connectionRequestTimeout : DEFAULT_REQUEST_CONFIG.getConnectionRequestTimeout())
                .setSocketTimeout(socketTimeout != null ? socketTimeout : DEFAULT_REQUEST_CONFIG.getSocketTimeout());
        if (proxyHost != null){
            custom.setProxy(proxyHost);
        }
        RequestConfig requestConfig = custom.build();

        httpRequest.setConfig(requestConfig != null ? requestConfig : DEFAULT_REQUEST_CONFIG);
        URI uri = URI.create(this.uri);
        httpRequest.setURI(uri);
        httpRequest.addHeader("Accept","*/*");
        //httpRequest.addHeader("Origin", uri.getHost());
        //httpRequest.addHeader("Referer", uri.getHost());
        httpRequest.addHeader("User-Agent",DEFAULT_USER_AGENT);
        httpRequest.addHeader("Connection","keep-alive");

        if (contentType != null){
            httpRequest.addHeader("Content-type", contentType);
        }
        if (headers != null){
            HttpRequestBase finalHttpRequest = httpRequest;
            headers.entrySet().forEach((e) -> {
                finalHttpRequest.addHeader(e.getKey(),e.getValue());
            });
        }
        if(cookies != null){
            if (!cookies.isEmpty()){
                httpRequest.addHeader("Cookie",cookieToString(cookies));
            }
        }else {
            if(!COOKIES.isEmpty()){
                httpRequest.addHeader("Cookie",cookieToString(COOKIES));
            }
        }

        if (httpRequest instanceof  HttpPost){
            HttpPost httpPost = (HttpPost) httpRequest;
            HttpEntity httpEntity = null;
            if (body instanceof List){
                try {
                    httpEntity  = new UrlEncodedFormEntity((List)body);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }else if(body instanceof String){
                httpEntity = new StringEntity(body.toString(), "utf-8");
            }
            if (httpEntity != null)
            httpPost.setEntity(httpEntity);
        }
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpRequest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Object result = null;
        HttpEntity entity = httpResponse.getEntity();
        if (cookies != null){
            cookieFlush(cookies,cookieStore.getCookies());
        }else {
            cookieFlush(COOKIES,cookieStore.getCookies());
        }
        try {
            if (returnType == String.class){
                result = EntityUtils.toString(entity,"utf-8");
            }else if (returnType == byte[].class){
                result = EntityUtils.toByteArray(entity);
            }
            EntityUtils.consume(entity);

            if (httpResponse != null){
                if (responseStatus != null){
                    setIntegerValue(responseStatus,httpResponse.getStatusLine().getStatusCode());
                }
                httpResponse.close();
            }
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (T)result;
    }

    /**
     * 单位 毫秒
     * @param connectTimeout 请求超时
     * @param connectionRequestTimeout 从连接池获取连接超时
     * @param socketTimeout 响应超时
     * @return this
     */
    public H timeout(int connectTimeout,int connectionRequestTimeout,int socketTimeout) {
        this.connectTimeout = connectTimeout;
        this.connectionRequestTimeout = connectionRequestTimeout;
        this.socketTimeout = socketTimeout;
        return this;
    }

    public H proxy(HttpHost proxyHost){
        this.proxyHost = proxyHost;
        return this;
    }

    public H contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public H cookies(List<Cookie> cookies) {
        this.cookies = cookies;
        return this;
    }

    public H body(Object body) {
        this.body = body;
        return this;
    }

    public H headers(Map<String,String> headers){
        this.headers = headers;
        return this;
    }

    public static H get(String uri){
        H h = new H();
        h.httpType = HttpGet.class;
        h.uri = uri;
        return h;
    }
    public static H post(String uri){
        H h = new H();
        h.httpType = HttpPost.class;
        h.uri = uri;
        return h;
    }
    public <T> T exec(Class<T> returnType){
        return exec(returnType,null);
    }
    public String exec(Integer responseStatus){
        return exec(String.class,responseStatus);
    }
    public String exec(){
        return exec(String.class,null);
    }

    /**
     * 更改Integer值
     * @param integer Integer对象
     * @param val int值
     */
    private static void setIntegerValue(Integer integer,int val){
        Field valueFiled = null;
        try {
            valueFiled = integer.getClass().getDeclaredField("value");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        valueFiled.setAccessible(true);
        try {
            valueFiled.set(integer,val);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * 添加cookie，如果name相同则覆盖
     * @param old 原cookie
     * @param target 需要添加的cookie
     */
    private static void cookieFlush(List<Cookie> old, List<Cookie> target){
        flag:for (Cookie cookie : target) {
            for (int i = 0;i<old.size();i++){
                if(old.get(i).getName().equals(cookie.getName())){
                    old.set(i,cookie);
                    continue flag; //已覆盖，不需要添加cookie，直接跳出
                }
            }
            old.add(cookie);
        }
    }
    public static List<Cookie>  cookieFromString(String cookieStr){

        String[] cookiesStr = cookieStr.split("; ");
        List<Cookie> cookies = new ArrayList<>();
        for (String c : cookiesStr) {
            String[] nameValue = c.split("=");
            String name = nameValue[0];
            String value = "";
            if (nameValue.length == 2){
                value = nameValue[1];
            }
            BasicClientCookie cookie = new BasicClientCookie(name,value);
            cookies.add(cookie);
        }
        return cookies;
    }

    private static String cookieToString(List<Cookie> list){
        StringBuilder sb = new StringBuilder();
        for (Cookie cookie:list){
            sb.append(cookie.getName()+"="+cookie.getValue()+";");;
        }
        return sb.toString();
    }
    public static String writeFile(String path,byte[] bytes,String suffix){
        String fileName = UUID.randomUUID().toString() + suffix;
        try {
            FileOutputStream os = new FileOutputStream(path + "/" + fileName);
            os.write(bytes);
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        deleteFiles.add(new File(path + "/" +fileName));
        return fileName;

    }
}
