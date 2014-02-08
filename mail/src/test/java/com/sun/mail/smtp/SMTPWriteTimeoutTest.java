/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import com.sun.mail.test.TestServer;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.Timeout;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test that write timeouts work.
 */
public final class SMTPWriteTimeoutTest {

    // timeout the test in case of failure
    @Rule
    public Timeout deadlockTimeout = new Timeout(5000);

    private static final int TIMEOUT = 200;	// write timeout, in millis

    @Test
    public void test() throws Exception {
        TestServer server = null;
        try {
	    SMTPHandler handler = new SMTPHandler() {
		public void readMessage() throws IOException {
		    try {
			// delay long enough to cause timeout
			Thread.sleep(5 * TIMEOUT);
		    } catch (Exception ex) { }
		    super.readMessage();
		}
	    };
            server = new TestServer(handler);
            server.start();
            Thread.sleep(1000);

            final Properties properties = new Properties();
            properties.setProperty("mail.smtp.host", "localhost");
            properties.setProperty("mail.smtp.port", "" + server.getPort());
            properties.setProperty("mail.smtp.writetimeout", "" + TIMEOUT);
            final Session session = Session.getInstance(properties);
            //session.setDebug(true);

            final Transport t = session.getTransport("smtp");
            try {
		MimeMessage msg = new MimeMessage(session);
		msg.setRecipients(Message.RecipientType.TO, "joe@example.com");
		msg.setSubject("test");
		byte[] bytes = new byte[1024*1024];
		msg.setDataHandler(
		    new DataHandler(new ByteArrayDataSource(bytes,
				    "application/octet-stream")));
                t.connect();
		t.sendMessage(msg, msg.getAllRecipients());
		fail("No exception");
	    } catch (MessagingException ex) {
		// expect an exception from sendMessage
            } finally {
                t.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (server != null) {
                server.quit();
		server.interrupt();
		// wait long enough for handler to exit
		Thread.sleep(2 * TIMEOUT);
            }
        }
    }
}
