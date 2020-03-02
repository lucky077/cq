${info.card}，你的属性变化如下
[CQ:face,id=66]生命值： ${old.health} -> ${user.health} <#if old.health gt user.health>↓<#elseif old.health lt user.health>↑</#if>
[CQ:face,id=169]攻击力： ${old.damage} -> ${user.damage} <#if old.damage gt user.damage>↓<#elseif old.damage lt user.damage>↑</#if>
[CQ:face,id=189]防御力： ${old.armor} -> ${user.armor} <#if old.armor gt user.armor>↓<#elseif old.armor lt user.armor>↑</#if>
[CQ:face,id=54]暴击率： ${old.crit} -> ${user.crit} <#if old.crit gt user.crit>↓<#elseif old.crit lt user.crit>↑</#if>
[CQ:face,id=172]闪避率： ${old.dodge} -> ${user.dodge} <#if old.dodge gt user.dodge>↓<#elseif old.dodge lt user.dodge>↑</#if>
[CQ:face,id=151]速度： ${old.speed} -> ${user.speed} <#if old.speed gt user.speed>↓<#elseif old.speed lt user.speed>↑</#if>
[CQ:face,id=144]幸运度： ${old.luck} -> ${user.luck} <#if old.luck gt user.luck>↓<#elseif old.luck lt user.luck>↑</#if>

