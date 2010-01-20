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

import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.MessagingException;
import javax.mail.BodyPart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the "mail.mime.contenttypehandler" property.
 */
public class ContentTypeCleaner {
 
    private static Session s = Session.getInstance(new Properties());

    @BeforeClass
    public static void before() {
	System.out.println("ContentTypeCleaner");
	System.setProperty("mail.mime.contenttypehandler",
	    ContentTypeCleaner.class.getName());
    }

    @Test
    public void testGarbage() throws Exception {
        MimeMessage m = createMessage();
	MimeMultipart mp = (MimeMultipart)m.getContent();
	BodyPart bp = mp.getBodyPart(0);
	assertEquals("text/plain", bp.getContentType());
	assertEquals("first part\n", bp.getContent());
    }

    @Test
    public void testValid() throws Exception {
        MimeMessage m = createMessage();
	MimeMultipart mp = (MimeMultipart)m.getContent();
	BodyPart bp = mp.getBodyPart(1);
	assertEquals("text/plain; charset=iso-8859-1", bp.getContentType());
	assertEquals("second part\n", bp.getContent());
    }

    @Test
    public void testEmpty() throws Exception {
        MimeMessage m = createMessage();
	MimeMultipart mp = (MimeMultipart)m.getContent();
	BodyPart bp = mp.getBodyPart(2);
	assertEquals("text/plain", bp.getContentType());
	assertEquals("third part\n", bp.getContent());
    }

    public static String cleanContentType(MimePart mp, String contentType) {
	if (contentType == null)
	    return null;
	if (contentType.equals("complete garbage"))
	    return "text/plain";
	return contentType;
    }

    private static MimeMessage createMessage() throws MessagingException {
        String content =
	    "Mime-Version: 1.0\n" +
	    "Subject: Example\n" +
	    "Content-Type: multipart/mixed; boundary=\"-\"\n" +
	    "\n" +
	    "preamble\n" +
	    "---\n" +
	    "Content-Type: complete garbage\n" +
	    "\n" +
	    "first part\n" +
	    "\n" +
	    "---\n" +
	    "Content-Type: text/plain; charset=iso-8859-1\n" +
	    "\n" +
	    "second part\n" +
	    "\n" +
	    "---\n" +
	    "\n" +
	    "third part\n" +
	    "\n" +
	    "-----\n";

	return new MimeMessage(s, new StringBufferInputStream(content));
    }
}
