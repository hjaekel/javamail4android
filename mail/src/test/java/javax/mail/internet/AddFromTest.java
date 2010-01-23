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

package javax.mail.internet;

import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;

import org.junit.*;
import static org.junit.Assert.assertEquals;

/**
 * Test the MimeMultipart.addFrom method, for bug 5057742.
 */
public class AddFromTest {
 
    private static final Session s = Session.getInstance(new Properties());
    private static final String ADDR = "a@example.com";
    private static final InternetAddress iaddr;
    private static final InternetAddress[] addresses;
    static {
	InternetAddress ia = null;
	try {
	    ia = new InternetAddress(ADDR);
	} catch (AddressException ex) {
	    // can't happen
	} finally {
	    iaddr = ia;
	}
	addresses = new InternetAddress[] { iaddr };
    }

    @Test
    public void testNoFrom() throws Exception {
        MimeMessage m = new MimeMessage(s);
	m.addFrom(addresses);
	assertEquals("Number of From headers", 1, m.getHeader("From").length);
	assertEquals("From header", ADDR, m.getHeader("From", ","));
    }

    @Test
    public void testOneFrom() throws Exception {
        MimeMessage m = new MimeMessage(s);
	m.setFrom(iaddr);
	m.addFrom(addresses);
	assertEquals("Number of From headers", 1, m.getHeader("From").length);
	assertEquals("From header", ADDR + ", " + ADDR,
	    m.getHeader("From", ","));
    }
}
