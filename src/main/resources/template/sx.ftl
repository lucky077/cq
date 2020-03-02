${name}
${head}
[CQ:face,id=192]财富：${user.money}
[CQ:face,id=168]体力：${user.tili}
[CQ:face,id=190]荣誉：${user.honor}
[CQ:face,id=54]符卡MAX：<#if item??>${item.name}【${item.level}】<#else>无</#if>
<#if tail??>
${tail}
</#if>