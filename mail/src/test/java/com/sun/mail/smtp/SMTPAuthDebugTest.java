/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.mail.smtp;

import java.io.*;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.Transport;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.Timeout;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Test that authentication information is only included in
 * the debug output when explicitly requested by setting the
 * property "mail.debug.auth" to "true".
 *
 * XXX - should test all authentication types, but that requires
 *	 more work in the dummy test server.
 */
public final class SMTPAuthDebugTest {

    // timeout the test in case of deadlock
    @Rule
    public Timeout deadlockTimeout = new Timeout(20000);

    /**
     * Test that authentication information isn't included in the debug output.
     */
    @Test
    public void testNoAuthDefault() {
	final Properties properties = new Properties();
	assertFalse("AUTH in debug output", test(properties, "AUTH"));
    }

    @Test
    public void testNoAuth() {
	final Properties properties = new Properties();
	properties.setProperty("mail.debug.auth", "false");
	assertFalse("AUTH in debug output", test(properties, "AUTH"));
    }

    /**
     * Test that authentication information *is* included in the debug output.
     */
    @Test
    public void testAuth() {
	final Properties properties = new Properties();
	properties.setProperty("mail.debug.auth", "true");
	assertTrue("AUTH in debug output", test(properties, "AUTH"));
    }

    /**
     * Create a test server, connect to it, and collect the debug output.
     * Scan the debug output looking for "expect", return true if found.
     */
    public boolean test(Properties properties, String expect) {
	SMTPServer server = null;
	try {
	    final SMTPHandler handler = new SMTPHandler();
	    server = new SMTPServer(handler, 26425);
	    server.start();
	    Thread.sleep(1000);

	    properties.setProperty("mail.smtp.host", "localhost");
	    properties.setProperty("mail.smtp.port", "26425");
	    final Session session = Session.getInstance(properties);
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    PrintStream ps = new PrintStream(bos);
	    session.setDebugOut(ps);
	    session.setDebug(true);

	    final Transport t = session.getTransport("smtp");
	    try {
		t.connect("test", "test");
	    } catch (Exception ex) {
		System.out.println(ex);
		//ex.printStackTrace();
		fail(ex.toString());
	    } finally {
		t.close();
	    }

	    ps.close();
	    bos.close();
	    ByteArrayInputStream bis =
		new ByteArrayInputStream(bos.toByteArray());
	    BufferedReader r = new BufferedReader(
					new InputStreamReader(bis, "us-ascii"));
	    String line;
	    boolean found = false;
	    while ((line = r.readLine()) != null) {
		if (line.startsWith("DEBUG"))
		    continue;
		if (line.startsWith("*"))
		    continue;
		if (line.startsWith(expect))
		    found = true;
	    }
	    r.close();
	    return found;
	} catch (final Exception e) {
	    e.printStackTrace();
	    fail(e.getMessage());
	    return false;	// XXX - doesn't matter
	} finally {
	    if (server != null) {
		server.quit();
	    }
	}
    }
}
