/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.mail.util;

import java.lang.reflect.*;
import java.security.*;

import javax.mail.internet.MimePart;

/**
 * General MIME-related utility methods.
 *
 * @author	Bill Shannon
 * @since	JavaMail 1.4.4
 */
public class MimeUtil {

    private static final Method cleanContentType;

    static {
	Method meth = null;
	try {
	    String cth = System.getProperty("mail.mime.contenttypehandler");
	    if (cth != null) {
		ClassLoader cl = getContextClassLoader();
		Class clsHandler = null;
		if (cl != null) {
		    try {
			clsHandler = Class.forName(cth, false, cl);
		    } catch (ClassNotFoundException cex) { }
		}
		if (clsHandler == null)
		    clsHandler = Class.forName(cth);
		meth = clsHandler.getMethod("cleanContentType",
				new Class[] { MimePart.class, String.class });
	    }
	} catch (Exception ex) {
	    // ignore it
	} finally {
	    cleanContentType = meth;
	}
    }

    // No one should instantiate this class.
    private MimeUtil() {
    }

    /**
     * If a Content-Type handler has been specified,
     * call it to clean up the Content-Type value.
     */
    public static String cleanContentType(MimePart mp, String contentType) {
	if (cleanContentType != null) {
	    try {
		return (String)cleanContentType.invoke(null,
					    new Object[] { mp, contentType });
	    } catch (Exception ex) {
		return contentType;
	    }
	} else
	    return contentType;
    }

    /**
     * Convenience method to get our context class loader.
     * Assert any privileges we might have and then call the
     * Thread.getContextClassLoader method.
     */
    private static ClassLoader getContextClassLoader() {
	return (ClassLoader)
		AccessController.doPrivileged(new PrivilegedAction() {
	    public Object run() {
		ClassLoader cl = null;
		try {
		    cl = Thread.currentThread().getContextClassLoader();
		} catch (SecurityException ex) { }
		return cl;
	    }
	});
    }
}
