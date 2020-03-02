package com.example.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LuckUtil {

    private static final int TIMES = 10000;

    public static boolean trueOrFalse(double probability){
        ThreadLocalRandom rdm = ThreadLocalRandom.current();
        return rdm.nextInt(100 * TIMES) < probability * TIMES;
    }

    public static int getOne(List<Double> probabilitys){
        ThreadLocalRandom rdm = ThreadLocalRandom.current();
        int num = rdm.nextInt(100 * TIMES);

        int t = 0;

        for (int i = 0;i < probabilitys.size();i++){
            if (num <probabilitys.get(i) * TIMES + t){
                return i;
            }
            t += (int)(probabilitys.get(i) * TIMES);
        }

        return  -1;
    }

    public static int randInt(int start,int limit){
       return ThreadLocalRandom.current().nextInt(start,limit + 1);
    }


    /**
     * 计算一个概率 经过幸运值之后
     * @param probability 概率
     * @param luck 玩家幸运值
     * @return
     */
    public static double f(double probability,int luck){
        if (luck > 99){
            double v = (luck - 100) / 100.0;
            BigDecimal b1 = BigDecimal.valueOf(1.0 + v);
            BigDecimal multiply = BigDecimal.valueOf(probability).multiply(b1);
            return multiply.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        int i = 100 - luck;
        double v = i / (100.0 + i);
        BigDecimal multiply = BigDecimal.valueOf(1).subtract(BigDecimal.valueOf(v)).multiply(BigDecimal.valueOf(probability));
        return multiply.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

    }





}
