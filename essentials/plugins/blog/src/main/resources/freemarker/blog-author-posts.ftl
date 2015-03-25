<#include "../../include/imports.ftl">

<#-- @ftlvariable name="item" type="{{beansPackage}}.Blogpost" -->
<#-- @ftlvariable name="author" type="{{beansPackage}}.Author" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->

<@hst.setBundle basename="essentials.blog"/>
<#if pageable??>
<div class="panel panel-default">
    <div class="panel-heading">
        <h3 class="panel-title"><@fmt.message key="blog.moreby"/>&nbsp;${author.fullName?html}</h3>
    </div>
    <#if pagebale?? && pageable.total > 0>
        <div class="panel-body">
            <#list pageable.items as item>
                <@hst.link hippobean=item var="link"/>
                <p><a href="${link}">${item.title?html}</a></p>
            </#list>
        </div>
    <#else>
        <div class="panel-body">
            <p><fmt:message key="blog.notfound"/></p>
        </div>
    </#if>
</div>
<#-- @ftlvariable id="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/blogposts-by-author.png'/>"> Click to edit Blogposts by Author
</#if>
