/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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

import java.util.Properties;
import javax.mail.Session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Test that the PropUtil methods return the correct values,
 * especially when defaults and non-String values are considered.
 */
public class PropUtilTest {
    @Test
    public void testInt() throws Exception {
	Properties props = new Properties();
	props.setProperty("test", "2");
	Session sess = Session.getInstance(props, null);
	assertEquals(PropUtil.getIntSessionProperty(sess, "test", 1), 2);
    }

    @Test
    public void testIntDef() throws Exception {
	Properties props = new Properties();
	Session sess = Session.getInstance(props, null);
	assertEquals(PropUtil.getIntSessionProperty(sess, "test", 1), 1);
    }

    @Test
    public void testIntDefProp() throws Exception {
	Properties defprops = new Properties();
	defprops.setProperty("test", "2");
	Properties props = new Properties(defprops);
	Session sess = Session.getInstance(props, null);
	assertEquals(PropUtil.getIntSessionProperty(sess, "test", 1), 2);
    }

    @Test
    public void testInteger() throws Exception {
	Properties props = new Properties();
	props.put("test", 2);
	Session sess = Session.getInstance(props, null);
	assertEquals(PropUtil.getIntSessionProperty(sess, "test", 1), 2);
    }

    @Test
    public void testBool() throws Exception {
	Properties props = new Properties();
	props.setProperty("test", "true");
	Session sess = Session.getInstance(props, null);
	assertTrue(PropUtil.getBooleanSessionProperty(sess, "test", false));
    }

    @Test
    public void testBoolDef() throws Exception {
	Properties props = new Properties();
	Session sess = Session.getInstance(props, null);
	assertTrue(PropUtil.getBooleanSessionProperty(sess, "test", true));
    }

    @Test
    public void testBoolDefProp() throws Exception {
	Properties defprops = new Properties();
	defprops.setProperty("test", "true");
	Properties props = new Properties(defprops);
	Session sess = Session.getInstance(props, null);
	assertTrue(PropUtil.getBooleanSessionProperty(sess, "test", false));
    }

    @Test
    public void testBoolean() throws Exception {
	Properties props = new Properties();
	props.put("test", true);
	Session sess = Session.getInstance(props, null);
	assertTrue(PropUtil.getBooleanSessionProperty(sess, "test", false));
    }


    // the Session variants...

    @Test
    public void testSessionInt() throws Exception {
	Properties props = new Properties();
	props.setProperty("test", "2");
	Session sess = Session.getInstance(props, null);
	assertEquals(PropUtil.getIntSessionProperty(sess, "test", 1), 2);
    }

    @Test
    public void testSessionIntDef() throws Exception {
	Properties props = new Properties();
	Session sess = Session.getInstance(props, null);
	assertEquals(PropUtil.getIntSessionProperty(sess, "test", 1), 1);
    }

    @Test
    public void testSessionIntDefProp() throws Exception {
	Properties defprops = new Properties();
	defprops.setProperty("test", "2");
	Properties props = new Properties(defprops);
	Session sess = Session.getInstance(props, null);
	assertEquals(PropUtil.getIntSessionProperty(sess, "test", 1), 2);
    }

    @Test
    public void testSessionInteger() throws Exception {
	Properties props = new Properties();
	props.put("test", 2);
	Session sess = Session.getInstance(props, null);
	assertEquals(PropUtil.getIntSessionProperty(sess, "test", 1), 2);
    }

    @Test
    public void testSessionBool() throws Exception {
	Properties props = new Properties();
	props.setProperty("test", "true");
	Session sess = Session.getInstance(props, null);
	assertTrue(PropUtil.getBooleanSessionProperty(sess, "test", false));
    }

    @Test
    public void testSessionBoolDef() throws Exception {
	Properties props = new Properties();
	Session sess = Session.getInstance(props, null);
	assertTrue(PropUtil.getBooleanSessionProperty(sess, "test", true));
    }

    @Test
    public void testSessionBoolDefProp() throws Exception {
	Properties defprops = new Properties();
	defprops.setProperty("test", "true");
	Properties props = new Properties(defprops);
	Session sess = Session.getInstance(props, null);
	assertTrue(PropUtil.getBooleanSessionProperty(sess, "test", false));
    }

    @Test
    public void testSessionBoolean() throws Exception {
	Properties props = new Properties();
	props.put("test", true);
	Session sess = Session.getInstance(props, null);
	assertTrue(PropUtil.getBooleanSessionProperty(sess, "test", false));
    }


    // the System variants...

    @Test
    public void testSystemBool() throws Exception {
	System.setProperty("test", "true");
	assertTrue(PropUtil.getBooleanSystemProperty("test", false));
    }

    @Test
    public void testSystemBoolDef() throws Exception {
	assertTrue(PropUtil.getBooleanSystemProperty("testnotset", true));
    }

    @Test
    public void testSystemBoolean() throws Exception {
	System.getProperties().put("testboolean", true);
	assertTrue(PropUtil.getBooleanSystemProperty("testboolean", false));
    }
}
