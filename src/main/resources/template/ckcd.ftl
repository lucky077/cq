<#if list?size == 0>
${name}没有仇敌<#else>${name}的仇敌
<#list list as o>
${o_index+1}.${o.name}(${o.qq})   ${o.val}
</#list>
</#if>