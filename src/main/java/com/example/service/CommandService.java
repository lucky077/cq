package com.example.service;



import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Demo;
import com.example.annotation.CommandMapping;
import com.example.annotation.Normal;
import com.example.annotation.Times;
import com.example.aop.TestAspectJ;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.model.Message;
import com.example.util.H;
import com.example.util.LuckUtil;
import com.example.util.MyUtil;
import com.sobte.cqp.jcq.entity.Member;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.example.Demo.*;
import static com.example.Variable.*;
import static com.example.util.LuckUtil.*;

@CommandMapping
public class CommandService {

    private Integer limit = 5566;

    @Resource
    private UserMapper userMapper;

    @CommandMapping("菜单")
    public Object cd(Message message){
        String cd = MyUtil.getChildMenu("cd");
        return cd;
    }
    @CommandMapping(value = {"互动"},menu = {"cd"})
    public Object hd(Message message){
        String cd = MyUtil.getChildMenu("hd");
        return cd;
    }
    //@CommandMapping(value = {"商店"},menu = {"cd"})
    public Object sd(Message message){
       String cd = "www.aslucky.club/robot/item/list?sale=1";
        return cd;
    }
    @CommandMapping(value = {"赌博*"},menu = {"hd"})
    @Times(limit = 5,tip = "每天只能赌5次噢")
    public Object db(Message message,Integer limit){
        User user = message.getUser();

        if (limit == null || limit < 1){
            limit = 100;
        }

        if (user.getMoney() < limit){
            sendGroupMsg("你赌不起这么大的,找别人借点钱再赌吧喵~");
            return -1;
        }
        Double luck = 50.0;
        double offset = ((luck - 50.0)) / 100;
        offset = 2 * offset;

        int add = LuckUtil.randInt(-limit, (int)(limit * (1 + offset)));
        if (add == 0) add = limit * 12;
        user.setMoney(add + user.getMoney());
        if (add > 0){
            return "你赢得" + add + "金币！";
        }else {
            return "你失去了" + -add + "金币！";
        }
    }
    @CommandMapping(value = {"财富排名*","财富排行*"},menu = {"cd"})
    public Object cfpm(Message message,Integer limit){

        if (limit == null || limit > 10){
            limit = 10;
        }

        List<User> users = userMapper.selectList(new QueryWrapper<User>().eq("group_id",message.getFromGroup()).gt("qq",9999).orderByDesc("money").select("qq", "money").last("limit " + limit));

        users.forEach(user -> {
            Member info = getGroupMemberInfo(user.getQq());
            user.setName(MyUtil.getCardName(info));
        });
        Map data = new HashMap(){{
            put("list",users);
        }};

        return data;
    }

    @CommandMapping(value = {"真实财富排名*","真实财富排行*"},menu = {"cd"})
    public Object zscfpm(Message message,Integer limit){

        if (limit == null || limit > 10){
            limit = 10;
        }

        List<User> users = userMapper.selectListzs(message.getFromGroup(),limit);

        users.forEach(user -> {
            user.setMoney(user.getMoney() + user.getBankMoney());
            Member info = getGroupMemberInfo(user.getQq());
            user.setName(MyUtil.getCardName(info));
        });
        List<User> finalUsers = users;
        Map data = new HashMap(){{
            put("list", finalUsers);
        }};

        return new ModelAndView("cfpm",data);
    }



    @CommandMapping(value = {"随机老婆*"},menu = {"cd"})
    @Times(limit = 3,interval = 180)
    @Normal
    public Object sjlp(Message message,Integer start,Integer limit){
        if (start == null || start < 1 ||start > this.limit){
            start = 1;
            limit = this.limit;
        }else {
            if (limit == null){
                limit = 1;
            }else {
                limit -= start;
                if (limit < 0 || limit + start > this.limit){
                    start = 1;
                    limit = this.limit;
                }else {
                    limit++;
                }
            }
        }

        ThreadLocalRandom r = ThreadLocalRandom.current();
        int i = r.nextInt(limit) + start;
        String image = CC.image(i + ".jpg");
        return "老婆" + i + "号" + image;
    }

    @CommandMapping(value = {"百科*"},menu = {"cd"})
    @Times(limit = 3,interval = 180)
    @Normal
    public Object bk(Message message,String word){
        if (StringUtils.isEmpty(word)){
            word = "百度百科";
        }
        String html;
        try {
            html = H.get("https://baike.baidu.com/item/"+word).exec();
        }catch (Exception e){
            return -1;
        }
        Document document = Jsoup.parse(html);
        Element element = document.selectFirst(".lemma-summary");
        if (element == null){
            String face = CC.face(21);
            return "找不到这个词条喔"+face;
        }
        String text = element.text();
        text = text.replaceAll("\\[[0-9]\\]","").replace("  ","");

        Element pic = document.selectFirst(".summary-pic");
        if (pic == null){
            return text;
        }
        String imgUrl = pic.selectFirst("img").attr("src");
        byte[] img = H.get(imgUrl).exec(byte[].class);
        String fileName = H.writeFile(imageCachePath, img, ".jpg");
        String imgCq = CC.image("cache/" + fileName);
        return imgCq + text;
    }

    @CommandMapping(value = {"签到"},notes = "免费抽奖一次",menu = {"cd"})
    @Normal
    public Object qd(Message message){
        Long fromQQ = message.getFromQQ();
        User user = message.getUser();

        List<User> users = userMapper.selectList(new QueryWrapper<User>()
                .eq("group_id",message.getFromGroup())
                .apply( "DATE_FORMAT(check_date,'%Y-%m-%d') = CURDATE()")
        );
        if (MyUtil.userListContains(users,user)){
            return user.getName() + ",你今天已经签到过了，不能重复签到！";
        }
        int size = users.size() + 1 ;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(user.getCheckDate());
        calendar.add(Calendar.DATE,1);
        LocalDate localDate = LocalDate.ofYearDay(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR));
        LocalDate now = LocalDate.now();

        if (now.equals(localDate)){
            user.setCheckDay(user.getCheckDay() + 1);
        }else {
            user.setCheckDay(0);
        }
        int addMoney = (user.getCheckDay() + 1) * 10;
        if (addMoney > 288){
            addMoney = 288;
        }
        user.setCheckDate(new Date()).setMoney(user.getMoney() + addMoney);

        byte[] head = MyUtil.getHead(fromQQ);
        //String path = rootPath +"/data/image";
        String fileName = H.writeFile(imageCachePath,head,".jpg");
        String imgCq = CC.image("cache/" + fileName);
        Map map = new HashMap();
        map.put("head",imgCq);
        map.put("jinbi",addMoney);
        map.put("user",user);
        map.put("paiming",size);
        map.put("draw",draw(user));
        return map;
    }

    public String draw(User user){

        int add;
        if (trueOrFalse(5.0)){
            add = randInt(500,1500);
        }else {
            add = randInt(10,100);
        }
        user.setMoney(user.getMoney() + add);

        return "抽奖得到" + add +"金币！";
    }

    @CommandMapping(value = {"机器人*"})
    @Normal
    public Object jqr(Message message,String cmd){
       if (!message.getUser().getQq().equals(471129493L)){
           return -1;
       }
       if ("开启".equals(cmd)){
           TestAspectJ.isRun = true;
       }else if ("关闭".equals(cmd)){
           TestAspectJ.isRun = false;
       }

        return "ok";
    }


}
