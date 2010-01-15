/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
 * Copyright 2009-2010 Jason Mehrens. All rights reserved.
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

package com.sun.mail.util.logging;

import java.lang.reflect.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.*;
import javax.mail.*;
import javax.mail.internet.*;
import junit.framework.TestCase;

/**
 * Test case for the MailHandler spec.
 * @author Jason Mehrens
 */
public class MailHandlerTest extends TestCase {

    public MailHandlerTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testIsLoggable() {
        //System.out.println("isLoggable");
        Level[] lvls = getAllLevels();
        if (lvls.length > 0) {
            LogRecord record = new LogRecord(Level.INFO, "");
            for (int i = 0; i < lvls.length; i++) {
                testLoggable(lvls[i], null);
                testLoggable(lvls[i], record);
            }
        } else {
            fail("No predefined levels.");
        }
    }

    private void testLoggable(Level lvl, LogRecord record) {
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        instance.setLevel(lvl);
        MemoryHandler mem = null;
        boolean result = false;
        boolean expect = true;
        try {
            result = instance.isLoggable(record);
            mem = new MemoryHandler(new ConsoleHandler(), 100, Level.OFF);
            mem.setErrorManager(em);
            mem.setLevel(lvl);
            expect = mem.isLoggable(record);
        } catch (RuntimeException mailEx) {
            try {
                if (mem != null) {
                    fail("MemoryHandler threw and exception: " + mailEx);
                } else {
                    mem = new MemoryHandler(new ConsoleHandler(), 100, Level.OFF);
                    mem.setErrorManager(em);
                    mem.setLevel(lvl);
                    expect = mem.isLoggable(record);
                    fail("MailHandler threw and exception: " + mailEx);
                }
            } catch (RuntimeException memEx) {
                assertEquals(memEx.getClass(), mailEx.getClass());
                result = false;
                expect = false;
            }
        }
        assertEquals(expect, result);

        instance.setLevel(Level.INFO);
        instance.setFilter(BooleanFilter.FALSE);
        instance.setAttachmentFormatters(new Formatter[]{new SimpleFormatter(), new XMLFormatter()});
        //null filter makes all records INFO and above loggable.
        instance.setAttachmentFilters(new Filter[]{BooleanFilter.FALSE, null});
        assertEquals(false, instance.isLoggable(new LogRecord(Level.FINEST, "")));
        assertEquals(true, instance.isLoggable(new LogRecord(Level.INFO, "")));
        assertEquals(true, instance.isLoggable(new LogRecord(Level.WARNING, "")));
        assertEquals(true, instance.isLoggable(new LogRecord(Level.SEVERE, "")));

        assertEquals(em.exceptions.isEmpty(), true);
    }

    public void testPublish() {
        //System.out.println("publish");

        MailHandler instance = createHandlerWithRecords();
        InternalErrorManager em = (InternalErrorManager) instance.getErrorManager();
        assertEquals(em.exceptions.isEmpty(), true);
        instance.close();

        assertEquals(1, em.exceptions.size());
        assertEquals(true, em.exceptions.get(0) instanceof MessagingException);

        //Test for valid message.
        instance = createHandlerWithRecords();
        instance.setErrorManager(new MessageErrorManager(instance) {

            protected void error(MimeMessage message, Throwable t, int code) {
                try {
                    assertTrue(null != message.getSentDate());
                    assertTrue(message.getHeader("X-Priority") == null ||
                            message.getHeader("X-Priority").length == 0);
                    message.saveChanges();
                } catch (MessagingException ME) {
                    fail(ME.toString());
                }
            }
        });

        Level[] lvls = this.getAllLevels();
        for (int i = 0; i < lvls.length; i++) {
            instance.publish(new LogRecord(lvls[i], ""));
        }

        instance.close();
    }

    private MailHandler createHandlerWithRecords() {
        Level[] lvls = getAllLevels();

        MailHandler instance = new MailHandler(lvls.length + 2);
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);
        Properties props = new Properties();
        props.put("mail.smtp.host", "bad-host-name");
        props.put("mail.host", "bad-host-name");
        instance.setMailProperties(props);
        instance.setLevel(Level.ALL);
        instance.setFilter(null);
        instance.setPushLevel(Level.OFF);
        instance.setPushFilter(null);

        for (int i = 0; i < lvls.length; i++) {
            instance.publish(new LogRecord(lvls[i], ""));
        }
        return instance;
    }

    public void testBadFormatters() {
        MailHandler instance = new MailHandler();
        instance.setLevel(Level.ALL);
        Properties props = new Properties();
        props.put("mail.smtp.host", "bad-host-name");
        props.put("mail.host", "bad-host-name");
        instance.setMailProperties(props);

        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);
        instance.setComparator(new ThrowComparator());
        instance.setFormatter(new ThrowFormatter());
        instance.setSubject(new ThrowFormatter());
        instance.setAttachmentFormatters(new Formatter[]{new ThrowFormatter()});
        instance.setAttachmentNames(new Formatter[]{new ThrowFormatter()});

        LogRecord record = new LogRecord(Level.INFO, "");
        instance.publish(record);
        instance.close();

        assertEquals(true, !em.exceptions.isEmpty());
    }

    public void testBadFilters() {
        LogRecord record = new LogRecord(Level.INFO, "");
        ConsoleHandler console = new ConsoleHandler();
        console.setFilter(new ThrowFilter());
        MailHandler instance = null;
        try {
            boolean expect = console.isLoggable(record);
            instance = new MailHandler();
            instance.setLevel(Level.ALL);
            instance.setFilter(new ThrowFilter());
            boolean result = instance.isLoggable(record);
            assertEquals(expect, result);
        } catch (RuntimeException expectEx) {
            if (instance == null) {
                try {
                    instance = new MailHandler();
                    instance.setLevel(Level.ALL);
                    instance.setFilter(new ThrowFilter());
                    instance.isLoggable(record);
                    fail("Doesn't match the console handler.");
                } catch (RuntimeException resultEx) {
                    assertEquals(expectEx.getClass(), resultEx.getClass());
                }
            } else {
                fail("Doesn't match the console handler.");
            }
        }
        instance.setFilter(null);


        Properties props = new Properties();
        props.put("mail.smtp.host", "bad-host-name");
        props.put("mail.host", "bad-host-name");
        instance.setMailProperties(props);

        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        instance.setAttachmentFormatters(new Formatter[]{new SimpleFormatter()});
        instance.setAttachmentFilters(new Filter[]{new ThrowFilter()});
        instance.setAttachmentNames(new String[]{"test.txt"});

        instance.publish(record);
        instance.close();

        assertEquals(true, !em.exceptions.isEmpty());
    }

    public void testPushInsidePush() {
        //System.out.println("PushInsidePush");
        Level[] lvls = getAllLevels();

        MailHandler instance = new MailHandler(lvls.length + 2);
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);
        Properties props = new Properties();
        props.put("mail.smtp.host", "bad-host-name");
        props.put("mail.host", "bad-host-name");
        instance.setMailProperties(props);
        instance.setLevel(Level.ALL);
        instance.setFilter(null);
        instance.setPushLevel(Level.OFF);
        instance.setPushFilter(null);

        instance.setFormatter(new SimpleFormatter() {

            public String getHead(Handler h) {
                try {
                    h.flush();
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getHead(h);
            }

            public String getTail(Handler h) {
                final Filter filter = h.getFilter();
                try {
                    h.setFilter(filter);
                } catch (Throwable T) {
                    fail(T.toString());
                }

                final Level lvl = h.getLevel();
                try {
                    h.setLevel(lvl);
                } catch (Throwable T) {
                    fail(T.toString());
                }

                final String enc = h.getEncoding();
                try {
                    h.setEncoding(enc);
                } catch (Throwable T) {
                    fail(T.toString());
                }


                try {
                    h.setFormatter(new SimpleFormatter());
                } catch (Throwable T) {
                    fail(T.toString());
                }

                try {
                    h.close();
                    assertEquals(h.getLevel(), Level.OFF);
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getTail(h);
            }
        });


        Formatter push = new SimpleFormatter() {

            public String getHead(Handler h) {
                try {
                    ((MailHandler) h).push();
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getHead(h);
            }

            public String getTail(Handler h) {
                try {
                    ((MailHandler) h).push();
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getTail(h);
            }
        };

        Formatter atFor = new SimpleFormatter() {

            public String getHead(Handler h) {
                MailHandler mh = (MailHandler) h;
                Formatter[] f = mh.getAttachmentFormatters();
                try {
                    mh.setAttachmentFormatters(f);
                    fail("Mutable formatter.");
                } catch (IllegalStateException pass) {
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getHead(h);
            }

            public String getTail(Handler h) {
                getHead(h);
                return super.getTail(h);
            }
        };

        Formatter atName = new SimpleFormatter() {

            public String getHead(Handler h) {
                MailHandler mh = (MailHandler) h;
                Formatter[] f = mh.getAttachmentNames();
                try {
                    mh.setAttachmentNames(f);
                    fail("Mutable formatter.");
                } catch (IllegalStateException pass) {
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getHead(h);
            }

            public String getTail(Handler h) {
                getHead(h);
                return super.getTail(h);
            }
        };

        Formatter atFilter = new SimpleFormatter() {

            public String getHead(Handler h) {
                MailHandler mh = (MailHandler) h;
                Filter[] f = mh.getAttachmentFilters();
                try {
                    mh.setAttachmentFilters(f);
                    fail("Mutable filters.");
                } catch (IllegalStateException pass) {
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getHead(h);
            }

            public String getTail(Handler h) {
                getHead(h);
                return super.getTail(h);
            }
        };

        Formatter nameComp = new Formatter() {

            public String getHead(Handler h) {
                MailHandler mh = (MailHandler) h;
                Comparator c = mh.getComparator();
                try {
                    mh.setComparator(c);
                    fail("Mutable comparator.");
                } catch (IllegalStateException pass) {
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getHead(h);
            }

            public String format(LogRecord r) {
                return "";
            }

            public String getTail(Handler h) {
                getHead(h);
                return "name.txt";
            }
        };

        Formatter nameMail = new Formatter() {

            public String getHead(Handler h) {
                MailHandler mh = (MailHandler) h;
                Properties props = mh.getMailProperties();
                try {
                    mh.setMailProperties(props);
                    fail("Mutable props.");
                } catch (IllegalStateException pass) {
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getHead(h);
            }

            public String format(LogRecord r) {
                return "";
            }

            public String getTail(Handler h) {
                getHead(h);
                return "name.txt";
            }
        };

        Formatter nameSub = new Formatter() {

            public String getHead(Handler h) {
                MailHandler mh = (MailHandler) h;
                Formatter f = mh.getSubject();
                try {
                    mh.setSubject(f);
                    fail("Mutable subject.");
                } catch (IllegalStateException pass) {
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getHead(h);
            }

            public String format(LogRecord r) {
                return "";
            }

            public String getTail(Handler h) {
                getHead(h);
                return "name.txt";
            }
        };

        Formatter nameAuth = new Formatter() {

            public String getHead(Handler h) {
                MailHandler mh = (MailHandler) h;
                Authenticator a = mh.getAuthenticator();
                try {
                    mh.setAuthenticator(a);
                    fail("Mutable Authenticator.");
                } catch (IllegalStateException pass) {
                } catch (Throwable T) {
                    fail(T.toString());
                }
                return super.getHead(h);
            }

            public String format(LogRecord r) {
                return "";
            }

            public String getTail(Handler h) {
                getHead(h);
                return "name.txt";
            }
        };

        instance.setAttachmentFormatters(new Formatter[]{push, atFor, atName, atFilter});
        instance.setAttachmentNames(new Formatter[]{nameComp, nameMail, nameSub, nameAuth});

        for (int i = 0; i < lvls.length; i++) {
            instance.publish(new LogRecord(lvls[i], ""));
        }
        instance.flush();

        for (int i = 0; i < em.exceptions.size(); i++) {
            assertEquals(false, em.exceptions.get(i) instanceof RuntimeException);
        }
    }

    public void testPush() {
        //System.out.println("push");
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);
        instance.push();
        assertEquals(true, em.exceptions.isEmpty());

        instance = createHandlerWithRecords();
        em = (InternalErrorManager) instance.getErrorManager();
        instance.push();

        assertEquals(1, em.exceptions.size());
        assertEquals(true, em.exceptions.get(0) instanceof MessagingException);

        //Test for valid message.
        instance = createHandlerWithRecords();
        instance.setErrorManager(new PushErrorManager(instance));
        instance.push();
        instance.close();
    }

    public void testFlush() {
        //System.out.println("flush");
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);
        instance.flush();

        assertEquals(true, em.exceptions.isEmpty());

        instance = createHandlerWithRecords();
        em = (InternalErrorManager) instance.getErrorManager();
        instance.flush();

        assertEquals(1, em.exceptions.size());
        assertEquals(true, em.exceptions.get(0) instanceof MessagingException);

        //Test for valid message.
        instance = createHandlerWithRecords();
        instance.setErrorManager(new MessageErrorManager(instance) {

            protected void error(MimeMessage message, Throwable t, int code) {
                try {
                    assertTrue(null != message.getSentDate());
                    assertTrue(message.getHeader("X-Priority") == null ||
                            message.getHeader("X-Priority").length == 0);
                    message.saveChanges();
                } catch (MessagingException ME) {
                    fail(ME.toString());
                }
            }
        });
        instance.flush();
        instance.close();
    }

    public void testClose() {
        //System.out.println("close");
        LogRecord record = new LogRecord(Level.INFO, "");
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);
        int capacity = instance.getCapacity();

        assertNotNull(instance.getLevel());

        instance.setLevel(Level.ALL);
        assertEquals(true, instance.isLoggable(record));

        instance.close();

        assertEquals(false, instance.isLoggable(record));
        assertEquals(Level.OFF, instance.getLevel());

        instance.setLevel(Level.ALL);
        assertEquals(Level.OFF, instance.getLevel());

        assertEquals(capacity, instance.getCapacity());
        assertEquals(true, em.exceptions.isEmpty());

        instance = createHandlerWithRecords();
        em = (InternalErrorManager) instance.getErrorManager();
        instance.close();

        assertEquals(1, em.exceptions.size());
        assertEquals(true, em.exceptions.get(0) instanceof MessagingException);

        //Test for valid message.
        instance = createHandlerWithRecords();
        instance.setErrorManager(new MessageErrorManager(instance) {

            protected void error(MimeMessage message, Throwable t, int code) {
                try {
                    assertTrue(null != message.getSentDate());
                    assertTrue(message.getHeader("X-Priority") == null ||
                            message.getHeader("X-Priority").length == 0);
                    message.saveChanges();
                } catch (MessagingException ME) {
                    fail(ME.toString());
                }
            }
        });
        instance.close();
    }

    public void testLevel() {
        //System.out.println("Level");
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        assertNotNull(instance.getLevel());

        try {
            instance.setLevel(null);
            fail("Null level was allowed");
        } catch (NullPointerException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        Level[] lvls = getAllLevels();
        for (int i = 0; i < lvls.length; i++) {
            instance.setLevel(lvls[i]);
            assertEquals(instance.getLevel(), lvls[i]);
        }

        instance.close();
        for (int i = 0; i < lvls.length; i++) {
            instance.setLevel(lvls[i]);
            assertEquals(Level.OFF, instance.getLevel());
        }
        assertEquals(true, em.exceptions.isEmpty());
    }

    public void testPushLevel() {
        //System.out.println("PushLevel");
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        assertNotNull(instance.getPushLevel());

        try {
            instance.setPushLevel(null);
            fail("Null level was allowed");
        } catch (NullPointerException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        Level[] lvls = getAllLevels();
        for (int i = 0; i < lvls.length; i++) {
            instance.setPushLevel(lvls[i]);
            assertEquals(instance.getPushLevel(), lvls[i]);
        }

        instance.close();
        for (int i = 0; i < lvls.length; i++) {
            instance.setPushLevel(lvls[i]);
            assertEquals(instance.getPushLevel(), lvls[i]);
        }
        assertEquals(true, em.exceptions.isEmpty());
    }

    public void testPushFilter() {
        //System.out.println("PushFilter");
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        instance.setPushFilter(BooleanFilter.TRUE);
        assertEquals(BooleanFilter.TRUE, instance.getPushFilter());

        assertEquals(true, em.exceptions.isEmpty());

        instance = createHandlerWithRecords();
        instance.setErrorManager(new PushErrorManager(instance));
        instance.setPushFilter(BooleanFilter.TRUE);
        instance.setLevel(Level.ALL);
        instance.setPushLevel(Level.WARNING);
        instance.publish(new LogRecord(Level.SEVERE, ""));
        instance.close();
    }

    public void testComparator() {
        //System.out.println("Comparator");
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        Comparator uselessComparator = new UselessComparator();
        Comparator result = instance.getComparator();
        assertEquals(false, uselessComparator.equals(result));

        instance.setComparator(uselessComparator);
        result = instance.getComparator();

        assertEquals(true, uselessComparator.equals(result));

        assertEquals(true, em.exceptions.isEmpty());
    }

    public void testCapacity() {
        //System.out.println("Capacity");

        try {
            new MailHandler(-1);
            fail("Negative capacity was allowed.");
        } catch (IllegalArgumentException pass) {
        } catch (RuntimeException RE) {
            fail(RE.toString());
        }

        try {
            new MailHandler(0);
            fail("Zero capacity was allowed.");
        } catch (IllegalArgumentException pass) {
        } catch (RuntimeException RE) {
            fail(RE.toString());
        }

        try {
            new MailHandler(1);
        } catch (RuntimeException RE) {
            fail(RE.toString());
        }


        final int expResult = 20;
        MailHandler instance = new MailHandler(20);
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        int result = instance.getCapacity();
        assertEquals(expResult, result);
        instance.close();

        result = instance.getCapacity();
        assertEquals(expResult, result);
        assertEquals(true, em.exceptions.isEmpty());
    }

    public void testAuthenticator() {
        //System.out.println("Authenticator");

        Authenticator auth = new EmptyAuthenticator();

        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        try {
            instance.setAuthenticator(instance.getAuthenticator());
        } catch (RuntimeException RE) {
            fail(RE.toString());
        }

        try {
            instance.setAuthenticator(auth);
            assertEquals(auth, instance.getAuthenticator());
        } catch (RuntimeException RE) {
            fail(RE.toString());
        }

        assertEquals(true, em.exceptions.isEmpty());

        instance = createHandlerWithRecords();
        instance.setAuthenticator(new ThrowAuthenticator());
        em = (InternalErrorManager) instance.getErrorManager();
        instance.close();

        assertEquals(1, em.exceptions.size());
        assertEquals(true, em.exceptions.get(0) instanceof MessagingException);
    }

    public void testMailProperties() {
        //System.out.println("MailProperties");
        Properties props = new Properties();
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        assertNotNull(instance.getMailProperties());

        try {
            instance.setMailProperties(null);
            fail("Null was allowed.");
        } catch (NullPointerException pass) {
        } catch (RuntimeException RE) {
            fail(RE.toString());
        }

        instance.setMailProperties(props);
        Properties stored = instance.getMailProperties();

        assertNotNull(stored);
        assertEquals(false, props == stored);

        assertEquals(true, em.exceptions.isEmpty());
        instance.close();

        final String p = MailHandler.class.getName();
        instance = createHandlerWithRecords();
        props = instance.getMailProperties();
        em = new InternalErrorManager();
        instance.setErrorManager(em);
        props.setProperty(p.concat(".mail.from"), ";:;'");
        props.setProperty(p.concat(".mail.to"), ";:;'");
        props.setProperty(p.concat(".mail.sender"), ";:;'");
        props.setProperty(p.concat(".mail.cc"), ";:;'");
        props.setProperty(p.concat(".mail.bcc"), ";:;'");
        props.setProperty(p.concat(".mail.reply.to"), ";:;'");
        instance.setMailProperties(props);
        instance.close();
        assertEquals(false, em.exceptions.isEmpty());
    }

    public void testAttachmentFilters() {
        //System.out.println("AttachmentFilters");
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        Filter[] result = instance.getAttachmentFilters();
        assertNotNull(result);
        assertEquals(result.length, instance.getAttachmentFormatters().length);


        assertEquals(false, instance.getAttachmentFilters() == result);

        if (instance.getAttachmentFormatters().length != 0) {
            instance.setAttachmentFormatters(new Formatter[0]);
        }

        try {
            instance.setAttachmentFilters(null);
            fail("Null allowed.");
        } catch (NullPointerException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        try {
            instance.setAttachmentFilters(new Filter[0]);
        } catch (RuntimeException re) {
            fail(re.toString());
        }


        try {
            assertEquals(0, instance.getAttachmentFormatters().length);

            instance.setAttachmentFilters(new Filter[]{BooleanFilter.TRUE});
            fail("Filter to formatter mismatch.");
        } catch (IndexOutOfBoundsException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        instance.setAttachmentFormatters(new Formatter[]{new SimpleFormatter(), new XMLFormatter()});

        try {
            assertEquals(2, instance.getAttachmentFormatters().length);

            instance.setAttachmentFilters(new Filter[]{BooleanFilter.TRUE});
            fail("Filter to formatter mismatch.");
        } catch (IndexOutOfBoundsException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        try {
            assertEquals(2, instance.getAttachmentFormatters().length);
            Filter[] filters = new Filter[]{BooleanFilter.TRUE, BooleanFilter.TRUE};
            assertEquals(instance.getAttachmentFormatters().length, filters.length);
            instance.setAttachmentFilters(filters);
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        try {
            assertEquals(2, instance.getAttachmentFormatters().length);
            Filter[] filters = new Filter[]{null, null};
            assertEquals(instance.getAttachmentFormatters().length, filters.length);
            instance.setAttachmentFilters(filters);
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        try {
            assertEquals(2, instance.getAttachmentFormatters().length);
            instance.setAttachmentFilters(new Filter[0]);
            fail("Filter to formatter mismatch.");
        } catch (IndexOutOfBoundsException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        try {
            assertEquals(instance.getAttachmentFormatters().length, 2);
            Filter[] filters = new Filter[]{null, null};
            instance.setAttachmentFilters(filters);
            filters[0] = BooleanFilter.TRUE;
            assertEquals(filters[0], filters[0]);
            assertEquals(filters[0].equals(instance.getAttachmentFilters()[0]), false);
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        assertEquals(em.exceptions.isEmpty(), true);
    }

    public void testAttachmentFormatters() {
        //System.out.println("AttachmentFormatters");
        MailHandler instance = new MailHandler();

        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        Formatter[] result = instance.getAttachmentFormatters();
        assertNotNull(result);
        assertEquals(result == instance.getAttachmentFormatters(), false);

        assertEquals(result.length, instance.getAttachmentFilters().length);
        assertEquals(result.length, instance.getAttachmentNames().length);

        result = new Formatter[]{new SimpleFormatter(), new XMLFormatter()};
        instance.setAttachmentFormatters(result);

        assertEquals(result.length, instance.getAttachmentFilters().length);
        assertEquals(result.length, instance.getAttachmentNames().length);

        result[0] = new XMLFormatter();
        result[1] = new SimpleFormatter();
        assertEquals(result[1].getClass(), instance.getAttachmentFormatters()[0].getClass());
        assertEquals(result[0].getClass(), instance.getAttachmentFormatters()[1].getClass());

        try {
            instance.setAttachmentFormatters(null);
            fail("Null was allowed.");
        } catch (NullPointerException NPE) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        result[0] = null;
        try {
            instance.setAttachmentFormatters(result);
            fail("Null index was allowed.");
        } catch (NullPointerException NPE) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }


        result = new Formatter[0];
        try {
            instance.setAttachmentFormatters(result);
            assertEquals(result.length, instance.getAttachmentFilters().length);
            assertEquals(result.length, instance.getAttachmentNames().length);
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        assertEquals(em.exceptions.isEmpty(), true);
    }

    public void testAttachmentNames_StringArr() {
        //System.out.println("AttachmentNames");
        Formatter[] names = null;
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        names = instance.getAttachmentNames();
        assertNotNull(names);

        try {
            instance.setAttachmentNames((String[]) null);
            fail("Null was allowed.");
        } catch (RuntimeException re) {
            assertEquals(NullPointerException.class, re.getClass());
        }

        if (instance.getAttachmentFormatters().length > 0) {
            instance.setAttachmentFormatters(new Formatter[0]);
        }

        try {
            instance.setAttachmentNames(new String[0]);
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        try {
            instance.setAttachmentNames(new String[1]);
            fail("Mismatch with attachment formatters.");
        } catch (NullPointerException pass) {
        } catch (IndexOutOfBoundsException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        instance.setAttachmentFormatters(new Formatter[]{new SimpleFormatter(), new XMLFormatter()});
        try {
            instance.setAttachmentNames(new String[2]);
            fail("Null index was allowed.");
        } catch (NullPointerException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        Formatter[] formatters = instance.getAttachmentFormatters();
        names = instance.getAttachmentNames();

        assertEquals(names[0].toString(), String.valueOf(formatters[0]));
        assertEquals(names[1].toString(), String.valueOf(formatters[1]));

        String[] stringNames = new String[]{"error.txt", "error.xml"};
        instance.setAttachmentNames(stringNames);
        assertEquals(stringNames[0], instance.getAttachmentNames()[0].toString());
        assertEquals(stringNames[1], instance.getAttachmentNames()[1].toString());

        stringNames[0] = "info.txt";
        assertEquals(stringNames[0].equals(instance.getAttachmentNames()[0].toString()), false);

        try {
            instance.setAttachmentNames(new String[0]);
            fail("Names mismatch formatters.");
        } catch (IndexOutOfBoundsException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        assertEquals(true, em.exceptions.isEmpty());
    }

    public void testAttachmentNames_FormatterArr() {
        //System.out.println("AttachmentNames");
        Formatter[] formatters = null;
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        assertNotNull(instance.getAttachmentNames());

        try {
            instance.setAttachmentNames((Formatter[]) null);
            fail("Null was allowed.");
        } catch (NullPointerException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        if (instance.getAttachmentFormatters().length > 0) {
            instance.setAttachmentFormatters(new Formatter[0]);
        }

        try {
            instance.setAttachmentNames(new Formatter[2]);
            fail("formatter mismatch.");
        } catch (NullPointerException pass) {
        } catch (IndexOutOfBoundsException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        instance.setAttachmentFormatters(
                new Formatter[]{new SimpleFormatter(), new XMLFormatter()});

        assertEquals(instance.getAttachmentFormatters().length, instance.getAttachmentNames().length);

        formatters = new Formatter[]{new SimpleFormatter(), new XMLFormatter()};
        instance.setAttachmentNames(formatters);
        formatters[0] = new XMLFormatter();
        assertEquals(formatters[0].equals(instance.getAttachmentNames()[0]), false);

        assertEquals(em.exceptions.isEmpty(), true);
    }

    public void testSubject_String() {
        //System.out.println("Subject");
        String subject = "Test subject.";
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        assertNotNull(instance.getSubject());

        try {
            instance.setSubject((String) null);
            fail("Null subject was allowed.");
        } catch (NullPointerException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        instance.setSubject(subject);
        assertEquals(subject, instance.getSubject().toString());

        assertEquals(em.exceptions.isEmpty(), true);
    }

    public void testSubject_Formatter() {
        //System.out.println("Subject");
        Formatter format = new SimpleFormatter();
        MailHandler instance = new MailHandler();
        InternalErrorManager em = new InternalErrorManager();
        instance.setErrorManager(em);

        assertNotNull(instance.getSubject());

        try {
            instance.setSubject((Formatter) null);
            fail("Null subject was allowed.");
        } catch (NullPointerException pass) {
        } catch (RuntimeException re) {
            fail(re.toString());
        }

        instance.setSubject(format);
        assertEquals(format, instance.getSubject());


        assertEquals(true, em.exceptions.isEmpty());
    }

    public void testReportError() {
        //System.out.println("reportError");
        MailHandler instance = new MailHandler();
        instance.setErrorManager(new ErrorManager() {

            public void error(String msg, Exception ex, int code) {
                assertNull(msg);
            }
        });

        instance.reportError(null, null, ErrorManager.GENERIC_FAILURE);



        instance.setErrorManager(new ErrorManager() {

            public void error(String msg, Exception ex, int code) {
                assertEquals(msg.indexOf(Level.SEVERE.getName()), 0);
            }
        });

        instance.reportError("simple message.", null, ErrorManager.GENERIC_FAILURE);



        //Test for valid message.
        instance = createHandlerWithRecords();
        instance.setErrorManager(new MessageErrorManager(instance) {

            protected void error(MimeMessage message, Throwable t, int code) {
                try {
                    assertTrue(message.getHeader("X-Mailer")[0].startsWith(MailHandler.class.getName()));
                    assertTrue(null != message.getSentDate());
                    message.saveChanges();
                } catch (MessagingException ME) {
                    fail(ME.toString());
                }
            }
        });
        instance.close();
    }

    public void testSecurityManager() {
        class LogSecurityManager extends SecurityManager {

            boolean secure = false;

            public void checkPermission(java.security.Permission perm) {
                if (secure) {
                    super.checkPermission(perm);
                }
            }

            public void checkPermission(java.security.Permission perm, Object context) {
                if (secure) {
                    super.checkPermission(perm, context);
                }
            }
        }

        final LogSecurityManager manager = new LogSecurityManager();
        System.setSecurityManager(manager);

        manager.secure = false;
        MailHandler h = new MailHandler();
        manager.secure = true;

        try {
            h.setAttachmentFilters(new Filter[]{new ThrowFilter()});
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setAttachmentFormatters(new Formatter[]{new ThrowFormatter()});
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setAttachmentNames(new String[]{"error.txt"});
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setAttachmentNames(new Formatter[]{new ThrowFormatter()});
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setAuthenticator(null);
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setComparator(null);
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setLevel(Level.ALL);
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setMailProperties(new Properties());
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setPushFilter(null);
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setPushLevel(Level.OFF);
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setSubject(new ThrowFormatter());
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setSubject("test");
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.getAuthenticator();
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.getMailProperties();
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.close();
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            h.setLevel(Level.ALL);
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            new MailHandler();
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            new MailHandler(100);
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }

        try {
            new MailHandler(new Properties());
            fail("Missing secure check.");
        } catch (SecurityException pass) {
        } catch (Exception fail) {
            fail(fail.toString());
        }
        manager.secure = false;
        System.setSecurityManager(null);
    }

    /**
     * Test must run last.
     */
    public void testZInit() {
        assertNull(System.getProperty("java.util.logging.config.class"));

        final String key = "java.util.logging.config.file";
        assertNull(System.getProperty(key));
        String tmp = System.getProperty("java.io.tmpdir");
        if (tmp == null) {
            tmp = System.getProperty("user.home");
        }

        File dir = new File(tmp);
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        try {
            File cfg = File.createTempFile("mailhandler_test", ".properties", dir);
            cfg.deleteOnExit();
            System.setProperty(key, cfg.getAbsolutePath());
            try {
                initGoodTest(cfg);
                initBadTest(cfg);
            } finally {
                boolean v;
                v = cfg.delete();
                assertTrue(v);

                v = cfg.createNewFile();
                assertTrue(v);

                System.getProperties().remove(key);
                LogManager.getLogManager().readConfiguration();
            }
        } catch (IOException IOE) {
            IOE.printStackTrace();
            assertTrue(false);
        }
    }

    private void initGoodTest(File cfg) throws IOException {
        final String p = MailHandler.class.getName();
        Properties props = new Properties();
        FileOutputStream out = new FileOutputStream(cfg);
        try {
            props.put(p.concat(".errorManager"), InternalErrorManager.class.getName());
            props.put(p.concat(".capacity"), "10");
            props.put(p.concat(".level"), "ALL");
            props.put(p.concat(".formatter"), XMLFormatter.class.getName());
            props.put(p.concat(".filter"), ThrowFilter.class.getName());
            props.put(p.concat(".authenticator"), EmptyAuthenticator.class.getName());
            props.put(p.concat(".pushLevel"), "WARNING");
            props.put(p.concat(".pushFilter"), ThrowFilter.class.getName());
            props.put(p.concat(".comparator"), ThrowComparator.class.getName());
            props.put(p.concat(".encoding"), "UTF-8");

            props.put(p.concat(".attachment.filters"),
                    "null, " + ThrowFilter.class.getName() + ", " +
                    ThrowFilter.class.getName());

            props.put(p.concat(".attachment.formatters"),
                    SimpleFormatter.class.getName() + ", " +
                    XMLFormatter.class.getName() + ", " +
                    SimpleFormatter.class.getName());

            props.put(p.concat(".attachment.names"), "msg.txt, " + SimpleFormatter.class.getName() + ", error.txt");

            props.store(out, "Mail handler test file.");
        } finally {
            out.close();
        }

        LogManager.getLogManager().readConfiguration();
        MailHandler h = new MailHandler();
        assertEquals(10, h.getCapacity());
        assertEquals(Level.ALL, h.getLevel());
        assertEquals(ThrowFilter.class, h.getFilter().getClass());
        assertEquals(XMLFormatter.class, h.getFormatter().getClass());
        assertEquals(Level.WARNING, h.getPushLevel());
        assertEquals(ThrowFilter.class, h.getPushFilter().getClass());
        assertEquals("UTF-8", h.getEncoding());
        assertEquals(EmptyAuthenticator.class, h.getAuthenticator().getClass());
        assertEquals(3, h.getAttachmentFormatters().length);
        assertTrue(null != h.getAttachmentFormatters()[0]);
        assertTrue(null != h.getAttachmentFormatters()[1]);
        assertTrue(null != h.getAttachmentFormatters()[2]);
        assertEquals(3, h.getAttachmentFilters().length);
        assertEquals(null, h.getAttachmentFilters()[0]);
        assertEquals(ThrowFilter.class, h.getAttachmentFilters()[1].getClass());
        assertEquals(ThrowFilter.class, h.getAttachmentFilters()[2].getClass());
        assertEquals(3, h.getAttachmentNames().length);
        assertTrue(null != h.getAttachmentNames()[0]);
        assertTrue(null != h.getAttachmentNames()[1]);
        assertTrue(null != h.getAttachmentNames()[2]);

        InternalErrorManager em = (InternalErrorManager) h.getErrorManager();
        assertTrue(em.exceptions.isEmpty());

        for (int i = 0; i < em.exceptions.size(); i++) {
            System.out.println(em.exceptions.get(i));
        }

        h.close();
        assertEquals(em.exceptions.isEmpty(), true);

        props.remove(p.concat(".attachment.filters"));
        LogManager.getLogManager().readConfiguration();

        h = new MailHandler();
        em = (InternalErrorManager) h.getErrorManager();
        assertTrue(em.exceptions.isEmpty());
        assertEquals(3, h.getAttachmentFormatters().length);
        h.close();

        props.remove(p.concat(".attachment.names"));
        LogManager.getLogManager().readConfiguration();

        h = new MailHandler();
        em = (InternalErrorManager) h.getErrorManager();
        assertTrue(em.exceptions.isEmpty());
        assertEquals(h.getAttachmentFormatters().length, 3);
        h.close();
    }

    private void initBadTest(File cfg) throws IOException {
        final PrintStream err = System.err;
        ByteArrayOutputStream oldErrors = new ByteArrayOutputStream();

        final String p = MailHandler.class.getName();
        Properties props = new Properties();
        FileOutputStream out = new FileOutputStream(cfg);
        try {
            props.put(p.concat(".errorManager"), "InvalidErrorManager");
            props.put(p.concat(".capacity"), "-10");
            props.put(p.concat(".level"), "BAD");
            props.put(p.concat(".formatter"), "InvalidFormatter");
            props.put(p.concat(".filter"), "InvalidFilter");
            props.put(p.concat(".authenticator"), ThrowAuthenticator.class.getName());
            props.put(p.concat(".pushLevel"), "PUSHBAD");
            props.put(p.concat(".pushFilter"), "InvalidPushFilter");
            props.put(p.concat(".comparator"), "InvalidComparator");
            props.put(p.concat(".encoding"), "MailHandler-ENC");
            props.put(p.concat(".attachment.filters"), "null, " +
                    "InvalidAttachFilter1, " + ThrowFilter.class.getName());

            props.put(p.concat(".attachment.formatters"),
                    "InvalidAttachFormatter0, " +
                    ThrowComparator.class.getName() + ", " +
                    XMLFormatter.class.getName());

            props.put(p.concat(".attachment.names"), "msg.txt, " +
                    ThrowComparator.class.getName() + ", " + XMLFormatter.class.getName());
            props.store(out, "Mail handler test file.");
        } finally {
            out.close();
        }

        MailHandler h = null;
        oldErrors.reset();
        System.setErr(new PrintStream(oldErrors));
        try {
            /**
             * Bad level value for property: com.sun.mail.util.logging.MailHandler.level
             * The LogManager.setLevelsOnExistingLoggers triggers an error.
             * This code swallows that error message.
             */
            LogManager.getLogManager().readConfiguration();
            System.err.flush();
            String result = oldErrors.toString().trim();
            oldErrors.reset();
            if (result.length() > 0) {
                final String expect = "Bad level value for property: " + p + ".level";
                assertEquals(expect, result);
            }

            /**
             * The default error manager writes to System.err.
             * Since this test is trying to install an invalid ErrorManager
             * we can only capture the error by capturing System.err.
             */
            h = new MailHandler();
            System.err.flush();
            result = oldErrors.toString().trim();
            int index = result.indexOf(ErrorManager.class.getName() + ": " +
                    ErrorManager.OPEN_FAILURE + ": " + Level.SEVERE.getName() +
                    ": InvalidErrorManager");
            assertTrue(index > -1);
            assertTrue(result.indexOf("java.lang.ClassNotFoundException: InvalidErrorManager") > index);
            oldErrors.reset();
        } finally {
            System.setErr(err);
        }

        assertEquals(ErrorManager.class, h.getErrorManager().getClass());
        assertTrue(h.getCapacity() != 10);
        assertTrue(h.getCapacity() != -10);
        assertEquals(Level.WARNING, h.getLevel());
        assertEquals(null, h.getFilter());
        assertEquals(SimpleFormatter.class, h.getFormatter().getClass());
        assertEquals(Level.OFF, h.getPushLevel());
        assertEquals(null, h.getPushFilter());
        assertEquals(null, h.getEncoding());
        assertEquals(ThrowAuthenticator.class, h.getAuthenticator().getClass());
        assertEquals(3, h.getAttachmentFormatters().length);
        assertTrue(null != h.getAttachmentFormatters()[0]);
        assertTrue(null != h.getAttachmentFormatters()[1]);
        assertTrue(null != h.getAttachmentFormatters()[2]);
        assertEquals(3, h.getAttachmentFilters().length);
        assertTrue(null == h.getAttachmentFilters()[0]);
        assertTrue(null == h.getAttachmentFilters()[1]);
        assertTrue(null != h.getAttachmentFilters()[2]);
        assertEquals(ThrowFilter.class, h.getAttachmentFilters()[2].getClass());
        assertEquals(3, h.getAttachmentNames().length);
        assertTrue(null != h.getAttachmentNames()[0]);
        assertTrue(null != h.getAttachmentNames()[1]);
        assertTrue(null != h.getAttachmentNames()[2]);
        assertEquals(XMLFormatter.class, h.getAttachmentNames()[2].getClass());
        h.close();
    }

    private Level[] getAllLevels() {
        Field[] fields = Level.class.getFields();
        List a = new ArrayList(fields.length);
        for (int i = 0; i < fields.length; i++) {
            if (Modifier.isStatic(fields[i].getModifiers()) &&
                    Level.class.isAssignableFrom(fields[i].getType())) {
                try {
                    a.add(fields[i].get(null));
                } catch (IllegalArgumentException ex) {
                    fail(ex.toString());
                } catch (IllegalAccessException ex) {
                    fail(ex.toString());
                }
            }
        }
        return (Level[]) a.toArray(new Level[a.size()]);
    }

    private static abstract class MessageErrorManager extends InternalErrorManager {

        private final MailHandler h;

        protected MessageErrorManager(final MailHandler h) {
            if (h == null) {
                throw new NullPointerException();
            }
            this.h = h;
        }

        public final void error(String msg, Exception ex, int code) {
            super.error(msg, ex, code);
            MimeMessage message = null;
            try {
                byte[] b = msg.getBytes();
                assertTrue(b.length > 0);

                ByteArrayInputStream in = new ByteArrayInputStream(b);
                Session session = Session.getInstance(h.getMailProperties());
                message = new MimeMessage(session, in);
                error(message, ex, code);
            } catch (RuntimeException RE) {
                fail(RE.toString());
            } catch (MessagingException ME) {
                fail(ME.toString());
            }
        }

        protected abstract void error(MimeMessage msg, Throwable t, int code);
    }

    private static final class PushErrorManager extends MessageErrorManager {

        PushErrorManager(MailHandler h) {
            super(h);
        }

        protected void error(MimeMessage message, Throwable t, int code) {
            try {
                assertTrue(null != message.getSentDate());
                assertNotNull(message.getHeader("X-Priority"));
                assertEquals("2", message.getHeader("X-Priority")[0]);
                message.saveChanges();
            } catch (RuntimeException RE) {
                fail(RE.toString());
            } catch (MessagingException ME) {
                fail(ME.toString());
            }
        }
    }

    public static class ThrowFilter implements Filter {

        public boolean isLoggable(LogRecord record) {
            throw new RuntimeException(record.toString());
        }
    }

    public static final class ThrowComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            throw new RuntimeException();
        }
    }

    public static final class ThrowFormatter extends Formatter {

        public String format(LogRecord record) {
            throw new RuntimeException("format");
        }

        public String getHead(Handler h) {
            throw new RuntimeException("head");
        }

        public String getTail(Handler h) {
            throw new RuntimeException("head");
        }
    }

    public static class UselessComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            return o1.toString().compareTo(o2.toString());
        }
    };

    public static final class BooleanFilter implements Filter {

        static final BooleanFilter TRUE = new BooleanFilter(true);
        static final BooleanFilter FALSE = new BooleanFilter(false);
        private final boolean value;

        public BooleanFilter() {
            this(false);
        }

        private BooleanFilter(boolean v) {
            this.value = v;
        }

        public boolean isLoggable(LogRecord r) {
            return value;
        }
    }

    public static class InternalErrorManager extends ErrorManager {

        final List exceptions = new ArrayList();

        public void error(String msg, Exception ex, int code) {
            exceptions.add(ex);
        }
    }

    public static final class ThrowAuthenticator extends javax.mail.Authenticator {

        protected PasswordAuthentication getPasswordAuthentication() {
            throw new RuntimeException();
        }
    }

    public static final class EmptyAuthenticator extends javax.mail.Authenticator {

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication("", "");
        }
    }
}
