<#if list?size == 0>${userName}没有符卡<#else>${userName}的符卡：
<#list list as o>
[${o.type}]${o.name}【${o.level}】X${o.count}
</#list>
[CQ:face,id=144][CQ:face,id=144][CQ:face,id=144]
</#if>