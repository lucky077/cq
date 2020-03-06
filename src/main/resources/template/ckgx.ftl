<#if list?size == 0>
${name}没有${word}<#else>${name}的${word}
<#list list as o>
${o_index+1}.${o.name}(${o.qq})   ${o.val}
</#list>
</#if>