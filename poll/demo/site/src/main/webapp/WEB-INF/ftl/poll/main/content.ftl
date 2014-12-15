<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>

<#if pollDocument??>
    <h2>${pollDocument.title}</h2>
    Custom Parameter value: "${pollDocument.param}".
    <div id="poll">
        <#if pollDocument.poll.text??>
            <h4>${pollDocument.poll.text}</h4>
        </#if>
        <span id="noCookieSupportMessage">The poll cannot be shown because the browser does not support cookies</span>

<#-- Render Poll Form if not yet voted (voteSuccess not defined) or voting failed (voteSuccess is false) -->
        <#if !(voteSuccess??) || voteSuccess == false>
            <div id="pollDiv">
                <#if pollDocument.poll.introduction??>
                    <p id="">${pollDocument.poll.introduction}</p>
                </#if>

                <!-- The Poll -->
                <form id="form-poll" method="post" action="<@hst.actionURL />">
                    <input type="hidden" name="path" value="${path}"/>
                    <div>
                        <#list pollDocument.poll.options as curOption>
                            <div>
                                <input id="${curOption.value}" name="option" type="radio" value="${curOption.value}"
                                       <#if option?? && curOption == option>selected="true"</#if> />
                                <label for="${curOption.value}">${curOption.label}</label>
                            </div>
                        </#list>
                    </div>
                    <button class="submit" type="submit">Vote</button>
                    <#if voteSuccess??> <#-- Implies voteSuccess == "false" -->
                        <div>Sorry, processing the vote has failed</div>
                    </#if>
                </form>
            </div>
        </#if>

        <ul id="pollResults" class="poll-results-list">
            <#list pollVotes.options as curOption>
                <li>
                    <div class="poll-graph-bar">
                        <span class="poll-meter" style="width: ${curOption.votesPercentage}%"> </span>
                    </div>
                    <#if curOption.votesCount == 1>
                    ${curOption.label} - ${curOption.votesPercentage}% (${curOption.votesCount} vote)
                    <#else>
                    ${curOption.label} - ${curOption.votesPercentage}% (${curOption.votesCount} votes)
                    </#if>
                </li>
            </#list>            
            <#if pollVotes.totalVotesPercentage != 100>
	        	<li>
	        		Due to rounding the percentages don't add up to 100%
	        	</li>
	        </#if>
        </ul>

        <script  type="text/javascript">
            if (<#if voteSuccess?? && voteSuccess == true>1<#else>0</#if>) {
                hide("noCookieSupportMessage");
            } else if (checkBrowserSupportsCookies()) {
                hide("noCookieSupportMessage");
                hide("pollResults");
            } else {
                hide("pollDiv");
                hide("pollResults");
            }

            function hide(id) {
                var element = document.getElementById(id);
                element.parentNode.removeChild(element);
            }

            function checkBrowserSupportsCookies() {
                var cookieDate=new Date();
                var cookieString="testCookieSupport"+cookieDate.toUTCString();
                document.cookie="testCookieSupport="+cookieString;
                return document.cookie.length > 0;
            }
        </script>
    </div>
<#else>
    No active poll available
</#if>
