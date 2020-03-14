[CQ:face,id=192][CQ:face,id=192][CQ:face,id=192]
<#if list?size == 0>商店已经售空了<#else>符卡商店：
<#list list as o>
${o.number}.${o.itemName} 价格：${o.price}
</#list>输入购买 + 编号</#if>