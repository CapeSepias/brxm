<#include "../include/imports.ftl">

<#-- @ftlvariable name="item" type="{{beansPackage}}.EventsDocument" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable?? && pageable.items?has_content>
    <#list pageable.items as item>
        <@hst.link var="link" hippobean=item />
    <article class="has-edit-button">
        <@hst.cmseditlink hippobean=item/>
        <h3><a href="${link}">${item.title}</a></h3>
        <#if item.date?? && item.date.time??>
            <p><@fmt.formatDate value=item.date.time type="both" dateStyle="medium" timeStyle="short"/></p>
        </#if>
        <#if item.enddate?? && item.endDate.time??>
            <p><@fmt.formatDate value=item.endDate.time type="both" dateStyle="medium" timeStyle="short"/></p>
        </#if>
        <p>${item.location}</p>
        <p>${item.introduction}</p>
    </article>
    </#list>
    <#if cparam.showPagination>
    <#include "../include/pagination.ftl">
    </#if>
<#-- @ftlvariable id="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
  <img src="<@hst.link path='/images/essentials/catalog-component-icons/events-list.png'/>"> Click to edit Event List
</#if>

