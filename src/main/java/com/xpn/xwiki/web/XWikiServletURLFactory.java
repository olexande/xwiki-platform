/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 25 mai 2004
 * Time: 12:23:58
 */
package com.xpn.xwiki.web;

import java.net.MalformedURLException;
import java.net.URL;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;

public class XWikiServletURLFactory extends XWikiDefaultURLFactory {
    protected URL serverURL;
    protected String servletPath;
    protected String actionPath;

    public XWikiServletURLFactory() {
    }

    // Used by tests
    public XWikiServletURLFactory(URL serverURL, String servletPath, String actionPath) {
        this.serverURL = serverURL;
        this.servletPath = servletPath;
        this.actionPath = actionPath;
    }

    // Used by tests
    public XWikiServletURLFactory(XWikiContext context) {
        init(context);
    }

    public void init(XWikiContext context) {
        URL url = context.getURL();
        String path = url.getPath();
        String servletpath = context.getRequest().getServletPath();

        servletPath = (context.getWiki()==null) ? "" : context.getWiki().Param("xwiki.servletpath", "");
        if (servletPath.equals("")) {
            try {
                servletPath = ((XWikiServletContext)context.getEngineContext()).getServletContext().getServletContextName() + "/";
            } catch (Exception e) {
                servletPath = path.substring(0, path.indexOf('/', 1) + 1);
            }
        }

        actionPath = context.getWiki().Param("xwiki.actionpath", "");
        if (actionPath.equals("")) {
            if (servletpath.startsWith ("/bin")) {
                actionPath = "bin/";
            }
            else if (context.getRequest().getServletPath().startsWith ("/testbin")) {
                context.setDatabase("xwikitest");
                context.setOriginalDatabase("xwikitest");
                actionPath = "testbin/";
            } else {
                actionPath = context.getWiki().Param("xwiki.defaultactionpath", "xwiki/");;
            }
        }

        try {
            serverURL = new URL(url, "/");
        } catch (MalformedURLException e) {
            // This can't happen
        }
    }

    public String getServletPath() {
        return servletPath;
    }

    private URL getServerURL(XWikiContext context) throws MalformedURLException {
        return getServerURL(context.getDatabase(), context);
    }

    private URL getServerURL(String xwikidb, XWikiContext context) throws MalformedURLException {
        if (context.getRequest()!=null){      // necessary to the tests
            final String host = context.getRequest().getHeader("x-forwarded-host"); // apache modproxy host
            if (host!=null) {            	
            	int comaind = host.indexOf(',');
            	final String host1 = comaind>0 ? host.substring(0, comaind) : host;
            	if (!host1.equals(""))
            		return new URL("http://"+host1);
            }
        }
        if (xwikidb==null)
            return serverURL;

        if (xwikidb.equals(context.getOriginalDatabase()))
            return serverURL;
        else {
            return context.getWiki().getServerURL(xwikidb, context);
        }
    }

    public URL createURL(String web, String name, String action, boolean redirect, XWikiContext context) {
        return createURL(web, name, action, context);
    }

    public URL createURL(String web, String name, String action, String querystring, String anchor,
                         String xwikidb, XWikiContext context) {
        StringBuffer newpath = new StringBuffer(servletPath);
        newpath.append(actionPath);
        addAction(newpath, action);
        addSpace(newpath, web, action, context);
        addName(newpath, name, action, context);

        if ((querystring!=null)&&(!querystring.equals(""))) {
            newpath.append("?");
            newpath.append(querystring);
            // newpath.append(querystring.replaceAll("&","&amp;"));
        }

        if ((anchor!=null)&&(!anchor.equals(""))) {
            newpath.append("#");
            newpath.append(anchor);
        }

        try {
            return new URL(getServerURL(xwikidb, context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    private void addAction(StringBuffer newpath, String action) {
        if (!action.equals("view")) {
            newpath.append(action);
            newpath.append("/");
        }
    }

    private void addSpace(StringBuffer newpath, String web, String action, XWikiContext context) {
        boolean useDefaultWeb = context.getWiki().useDefaultWeb(context);
        if (useDefaultWeb) {
           String defaultWeb = context.getWiki().getDefaultWeb(context);
           useDefaultWeb = (web.equals(defaultWeb))&&(action.equals("view"));
        }
        if (!useDefaultWeb) {
            newpath.append(encode(web, context));
            newpath.append("/");
        }
    }

    private void addName(StringBuffer newpath, String name, String action, XWikiContext context) {
        XWiki xwiki = context.getWiki();
        if ((xwiki.useDefaultAction(context))
                ||(!name.equals(xwiki.getDefaultPage(context))||(!action.equals("view")))) {
            newpath.append(encode(name, context));
        }
    }

    private void addFileName(StringBuffer newpath, String filename, XWikiContext context) {
        newpath.append("/");
        newpath.append(encode(filename, context));
    }

    private String encode(String name, XWikiContext context) {
        return Utils.encode(name, context);
    }

    public URL createExternalURL(String web, String name, String action, String querystring, String anchor, String xwikidb, XWikiContext context) {
        return this.createURL(web, name, action, querystring, anchor, xwikidb, context);
    }

    public URL createSkinURL(String filename, String skin, XWikiContext context) {
        StringBuffer newpath = new StringBuffer(servletPath);
        newpath.append("skins/");
        newpath.append(skin);
        addFileName(newpath, filename, context);
        try {
            return new URL(getServerURL(context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    public URL createSkinURL(String filename, String web, String name, String xwikidb, XWikiContext context) {
        StringBuffer newpath = new StringBuffer(servletPath);
        newpath.append(actionPath);
        addAction(newpath, "skin");
        addSpace(newpath, web, "skin", context);
        addName(newpath, name, "skin", context);
        addFileName(newpath, filename, context);
        try {
            return new URL(getServerURL(xwikidb, context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }


    public URL createTemplateURL(String filename, XWikiContext context) {
        StringBuffer newpath = new StringBuffer(servletPath);
        newpath.append("templates");
        addFileName(newpath, filename, context);
        try {
            return new URL(getServerURL(context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    public URL createAttachmentURL(String filename, String web, String name, String action, String xwikidb, XWikiContext context) {
        StringBuffer newpath = new StringBuffer(servletPath);
        newpath.append(actionPath);
        addAction(newpath, action);
        addSpace(newpath, web, action, context);
        addName(newpath, name, action, context);
        addFileName(newpath, filename, context);
        try {
            return new URL(getServerURL(xwikidb, context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }


    public String getURL(URL url, XWikiContext context) {
        try {
            if (url==null)
                return "";

            String surl = url.toString();
            if (!surl.startsWith(serverURL.toString()))
                return surl;
            else {
                StringBuffer sbuf = new StringBuffer(url.getPath());
                String querystring = url.getQuery();
                if ((querystring!=null)&&(!querystring.equals(""))) {
                    sbuf.append("?");
                    sbuf.append(querystring);
                    // sbuf.append(querystring.replaceAll("&","&amp;"));
                }

                String anchor = url.getRef();
                if ((anchor!=null)&&(!anchor.equals(""))) {
                    sbuf.append("#");
                    sbuf.append(anchor);
                }
                return sbuf.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public URL getRequestURL(XWikiContext context) {    	
    	final URL url = super.getRequestURL(context);
		try {
			final URL servurl = getServerURL(context); // if use apache mod_proxy we needed to know external host address
			return new URL(url.getProtocol(), servurl.getHost(), servurl.getPort(), url.getFile());
		} catch (MalformedURLException e) {
			// This should not happen
			e.printStackTrace();
			return url;
		}
    }
}
