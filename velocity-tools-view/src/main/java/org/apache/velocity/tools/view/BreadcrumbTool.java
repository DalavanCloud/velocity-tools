package org.apache.velocity.tools.view;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.velocity.tools.Scope;
import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.config.ValidScope;
import org.apache.velocity.tools.generic.LocaleConfig;
import org.apache.velocity.tools.generic.ValueParser;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Helper tool to display a navigation breadcrumb to the end user.</p>
 * <p>It relies on the asumption that your URI hierarchy corresponds to the navigation hierarchy you want to present
 * to your users, each level having its landing index page. Please note that this asumption is considered a *good practice*.</p>
 * <p>Each intermediate path element becomes a navigation element, and there is an additional ending navigation element
 * when the page is not an index page.</p>
 * <p>For instance, if the URI is `/colors/red/nuances.html`, the provided path elements will be:</p>
 * <ul>
 *     <li>'colors', pointing towards '/colors/index.html'</li>
 *     <li>'red', pointing towards '/colors/red/index.html'</li>
 *     <li>'nuances', pointing towards '/colors/red/nuances.html'</li>
 * </ul>
 * <p>The filename extension (`html` in the above example) is inferred from found URIs.</p>
 * <p>By default, the displayed name of a navigation element is the name of its corresponding path element with all
 * '<code>_</code>' replaced by spaces, transformed to lowercase.</p>
 * <p>You can customize this displayed name and the URL of every path element using tools properties:</p>
 * <pre><code>
 *     <tool key="breadcrumb" colors.name="My Colors" colors.url="/colors/all.vhtml"/>
 * </code></pre>
 * <p>where '<code>colors</code>' refers to the <code>/colors/</code> path element. The name and destination of the
 * root path element can be changed with '<code>home.name</code>' and '<code>home.url</code>'.</p>
 * <p>Inside a template, you would either render directly the default resulting HTML fragment with:</p>
 * <pre><code>$breadcrumb</code></pre>
 * <p>which would produce simething like:</p>
 * <pre><code>&lt;a href="/index.vhtml"&gt;home&lt;/a&gt;&amp;nbsp;&amp;gt;&amp;nbsp;&lt;a href="/colors/all.vhtml"&gt;My Colors&lt;/a&gt;&amp;nbsp;&amp;gt;&amp;nbsp;nuances</code></pre>
 * <p>or you would iterate through navigation elements for a better control of the output with:</p>
 * <pre><code>
 * <div id="breadcrumb">
 * #foreach($element in $breadcrumb)
 *  #if(!$foreach.first)
 *   <span class="divider">&gt;</span>
 *  #end
 *  #if(!$foreach.last)<a href="$element.url">#end
 *   $element.name
 *  #if(!$foreach.last)</a>#end
 * #end
 *</div></code></pre>
 * <p>And of course, you would supply the necessary CSS, for instance:</p>
 * <pre><code>
 * #breadcrumb { font-size: smaller; }
 * #breadcrumb a { color: #4183c4; text-decoration: none; }
 * #breadcrumb .divider { color: rgba(0, 0, 0, 0.4); vertical-align: baseline; margin: 0 0.2rem; }
 * </code></pre>
 *
 * @version $$
 * @author Claude Brisson
 * @since 3.1
 *
 */

@DefaultKey("breadcrumb")
@ValidScope(Scope.REQUEST)
public class BreadcrumbTool extends LocaleConfig implements Iterable<BreadcrumbTool.NavigationElement>
{
    /**
     * Navigation elements for the current URI
     */
    protected List<NavigationElement> navigationElements = null;

    /**
     * Class representing a navigation element
     */
    public static class NavigationElement
    {
        /**
         * destination URL
         */
        private String url;

        /**
         * Displayed name
         */
        private String name;

        /**
         * Constructor
         * @param url destination URL
         * @param name displayed name
         */
        public NavigationElement(String url, String name)
        {
            this.url = url;
            if (name.endsWith(".vhtml")) name = name.substring(0, name.indexOf(".vhtml"));
            this.name = name;
        }

        /**
         * Destination URL getter
         * @return destination URL
         */
        public String getUrl() { return url; }

        /**
         * Displayed name getter
         * @return displayed name
         */
        public String getName() { return name; }

        /**
         * Displayed name setter
         * @param name displayed name
         */
        public void setName(String name)
        {
            this.name = name;
        }

        /**
         * Destination URL setter
         * @param url destination url
         */
        public void setUrl(String url)
        {
            this.url = url;
        }
    }

    /**
     * Current request setter
     * @param request
     */
    public void setRequest(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        // infer extension
        String ext = getExtension(uri);
        String index = "index." + ext;
        if ("/".equals(uri)) uri = "/" + index;
        String elements[] = uri.split("/");
        navigationElements = new ArrayList<NavigationElement>();
        StringBuilder builder = new StringBuilder();
        for (String elem : elements)
        {
            builder.append(elem);
            String currentPath = builder.toString();
            if (index.equals(elem)) continue;
            if (!elem.endsWith('.' + ext)) currentPath = currentPath + '/' + index;
            String name = builder.length() == 0 ? "home" : elem.replace('_', ' ').toLowerCase(getLocale());
            navigationElements.add(new NavigationElement(currentPath, name));
            builder.append('/');
        }
    }

    /**
     * Configuration
     */
    @Override
    protected void configure(ValueParser config)
    {
        if (navigationElements == null)
        {
            getLog().warn("cannot build breadcrumb: no provided request");
            return;
        }
        for (NavigationElement elem : navigationElements)
        {
            Object obj = config.get(elem.getName());
            if (obj != null && obj instanceof ValueParser)
            {
                ValueParser values = (ValueParser) obj;
                elem.setName((String) values.getOrDefault("name", elem.getName()));
                elem.setUrl((String) values.getOrDefault("url", elem.getUrl()));
            }
        }
    }

    /**
     * Navigation elements iteration
     * @return Iterator over navigation elements
     */
    @Override
    public Iterator<NavigationElement> iterator()
    {
        return navigationElements.iterator();
    }

    /**
     * Utility method to return URI file extension
     * @param filename
     * @return
     */
    protected static String getExtension(String filename)
    {
        if (filename == null) {
            return null;
        }
        int extensionPos = filename.lastIndexOf('.');
        int lastSeparator = filename.lastIndexOf('/');
        int index = lastSeparator > extensionPos ? -1 : extensionPos;
        if (index == -1)
        {
            return "";
        }
        else
        {
            return filename.substring(index + 1);
        }
    }

    /**
     * Default concatenation of navigation elements, separated by '>'
     * @return string representation
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        int len = navigationElements.size();
        for (int i = 0; i < len; ++i)
        {
            NavigationElement elem = navigationElements.get(i);
            if (i > 0)
            {
                builder.append("&nbsp;&gt;&nbsp;");
            }
            if (i < len - 1)
            {
                builder.append("<a href=\"").append(elem.getUrl()).append("\">");
            }
            builder.append(elem.getName());
            if (i < len - 1)
            {
                builder.append("</a>");
            }
        }
        return builder.toString();
    }
}
