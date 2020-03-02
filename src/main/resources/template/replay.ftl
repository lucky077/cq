<#list list as o>
${o.name1} -->> ${o.name2} 造成伤害：#{o.val}${o.dodge!}${o.crit!} ${o.name2}剩余生命值：#{o.health}<br>
</#list>
