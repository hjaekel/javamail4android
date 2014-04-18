/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package com.sun.mail.imap;

import java.util.Date;
import javax.mail.Message;
import javax.mail.search.SearchTerm;

/**
 * Find messages that are younger than a given interval (in seconds).
 * Relies on the server implementing the WITHIN search extension
 * (<A HREF="http://www.ietf.org/rfc/rfc5032.txt">RFC 5032</A>).
 *
 * @since	JavaMail 1.5.1
 * @author	Bill Shannon
 */
public final class YoungerTerm extends SearchTerm {

    private int interval;

    private static final long serialVersionUID = 1592714210688163496L;

    /**
     * Constructor.
     *
     * @param interval	number of seconds younger
     */
    public YoungerTerm(int interval) {
	this.interval = interval;
    }

    /**
     * Return the interval.
     *
     * @return	the interval
     */
    public int getInterval() {
	return interval;
    }

    /**
     * The match method.
     *
     * @param msg	the date comparator is applied to this Message's
     *			received date
     * @return		true if the comparison succeeds, otherwise false
     */
    public boolean match(Message msg) {
	Date d;

	try {
	    d = msg.getReceivedDate();
	} catch (Exception e) {
	    return false;
	}

	if (d == null)
	    return false;

	return d.getTime() >=
		    System.currentTimeMillis() - ((long)interval * 1000);
    }

    /**
     * Equality comparison.
     */
    public boolean equals(Object obj) {
	if (!(obj instanceof YoungerTerm))
	    return false;
	return interval == ((YoungerTerm)obj).interval;
    }

    /**
     * Compute a hashCode for this object.
     */
    public int hashCode() {
	return interval;
    }
}
