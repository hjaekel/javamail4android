/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2013-2014 Jason Mehrens. All rights reserved.
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
package com.sun.mail.util.logging;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * A plain text formatter that can produce fixed width output. By default this
 * formatter will produce output no greater than 160 characters wide. Only
 * specified fields support an {@link #toAlternate(java.lang.String) alternate}
 * fixed width format.
 * <p>
 * The LogManager properties are:
 * <ul>
 * <li>&lt;formatter-name&gt;.format - the {@link java.util.Formatter
 *     format} string used to transform the output. The format string can be used to
 * fix the output size. (defaults to "%7$#.160s%n")</li>
 * </ul>
 *
 * @author Jason Mehrens
 * @since JavaMail 1.5.2
 */
public class CompactFormatter extends java.util.logging.Formatter {

    /**
     * Holds the java.util.Formatter pattern.
     */
    private final String fmt;

    /**
     * Creates an instance with a default format pattern.
     */
    public CompactFormatter() {
        String p = getClass().getName();
        this.fmt = initFormat(p);
    }

    /**
     * Creates an instance with the given format pattern.
     *
     * @param format the {@link java.util.Formatter pattern}. The arguments are
     * described in the {@link #format(java.util.logging.LogRecord) format}
     * method.
     */
    public CompactFormatter(final String format) {
        String p = getClass().getName();
        this.fmt = format == null ? initFormat(p) : format;
    }

    /**
     * Format the given log record and returns the formatted string.
     *
     * <ol start='0'>
     * <li>{@code format} - the {@link java.util.Formatter
     *     java.util.Formatter} format string specified in the
     * &lt;formatter-name&gt;.format property or the format that was given when
     * this formatter was created.</li>
     * <li>{@code date} - a {@link Date} object representing
     * {@linkplain LogRecord#getMillis event time} of the log record.</li>
     * <li>{@code source} - a string representing the caller, if available;
     * otherwise, the logger's name.</li>
     * <li>{@code logger} - the logger's name.</li>
     * <li>{@code level} - the
     * {@linkplain java.util.logging.Level#getLocalizedName log level}.</li>
     * <li>{@code message} - the formatted log message returned from the
     * {@link #formatMessage(LogRecord)} method.</li>
     * <li>{@code thrown} - a string representing the
     * {@linkplain LogRecord#getThrown throwable} associated with the log record
     * and a relevant stack trace element if available. Otherwise, an empty
     * string is used.</li>
     * <li>{@code message|thrown} The message and the thrown properties joined
     * as one parameter. This parameter supports
     * {@link #toAlternate(java.lang.String) alternate} form.</li>
     * <li>{@code thrown|message} The thrown and message properties joined as
     * one parameter. This parameter supports
     * {@link #toAlternate(java.lang.String) alternate} form.</li>
     * </ol>
     *
     * @param record to format.
     * @return the formatted record.
     * @throws NullPointerException if the given record is null.
     */
    @Override
    public String format(final LogRecord record) {
        //LogRecord is mutable so define local vars.
        ResourceBundle rb = record.getResourceBundle();
        Locale l = rb == null ? null : rb.getLocale();

        String msg = formatMessage(record);
        String thrown = formatThrown(record);
        Object[] params = {
            new Date(record.getMillis()),
            formatSource(record),
            formatLoggerName(record),
            formatLevel(record),
            msg,
            thrown,
            new Alternate(msg, thrown),
            new Alternate(thrown, msg)};

        return String.format(l, fmt, params);
    }

    /**
     * Formats message for the log record. This method removes any fully
     * qualified throwable class names from the message.
     *
     * @param record the log record.
     * @return the formatted message string.
     */
    @Override
    public String formatMessage(final LogRecord record) {
        String msg = super.formatMessage(record);
        msg = replaceClassName(msg, record.getThrown());
        msg = replaceClassName(msg, record.getParameters());
        return msg;
    }

    /**
     * Formats the message from the thrown property of the log record. This
     * method removes any fully qualified throwable class names from the message
     * cause chain.
     *
     * @param t the throwable to format.
     * @return the formatted message string from the throwable.
     */
    public String formatMessage(final Throwable t) {
        return t != null ? replaceClassName(apply(t).getMessage(), t) : "";
    }

    /**
     * Formats the level property of the given log record.
     *
     * @param record the record.
     * @return the formatted logger name.
     * @throws NullPointerException if the given record is null.
     */
    public String formatLevel(final LogRecord record) {
        return record.getLevel().getLocalizedName();
    }

    /**
     * Formats the source from the given log record.
     *
     * @param record the record.
     * @return the formatted source of the log record.
     * @throws NullPointerException if the given record is null.
     */
    public String formatSource(final LogRecord record) {
        String source = record.getSourceClassName();
        if (source != null) {
            if (record.getSourceMethodName() != null) {
                source = simpleClassName(source) + " "
                        + record.getSourceMethodName();
            } else {
                source = simpleClassName(source);
            }
        } else {
            source = simpleClassName(record.getLoggerName());
        }
        return source;
    }

    /**
     * Formats the logger name property of the given log record.
     *
     * @param record the record.
     * @return the formatted logger name.
     * @throws NullPointerException if the given record is null.
     */
    public String formatLoggerName(final LogRecord record) {
        return simpleClassName(record.getLoggerName());
    }

    /**
     * Formats the thrown property of a LogRecord. The returned string will
     * contain a throwable message with a back trace.
     *
     * @param record the record.
     * @return empty string if nothing was thrown or formatted string.
     * @throws NullPointerException if the given record is null.
     * @see #apply(java.lang.Throwable)
     * @see #formatBackTrace(java.util.logging.LogRecord)
     */
    public String formatThrown(final LogRecord record) {
        String msg;
        final Throwable t = record.getThrown();
        if (t != null) {
            final Throwable root = apply(t);
            if (root != null) {
                msg = formatMessage(t);
                String site = formatBackTrace(record);
                msg = root.getClass().getSimpleName() + ": " + msg
                        + (isNullOrSpaces(site) ? "" : ' ' + site);
            } else {
                msg = "";
            }
        } else {
            msg = "";
        }
        return msg;
    }

    /**
     * Formats the back trace for the given log record.
     *
     * @param record the log record to format.
     * @return the formatted back trace.
     * @throws NullPointerException if the given record is null.
     * @see #apply(java.lang.Throwable)
     * @see #formatThrown(java.util.logging.LogRecord)
     * @see #ignore(java.lang.StackTraceElement)
     */
    public String formatBackTrace(LogRecord record) {
        String site = "";
        final Throwable t = record.getThrown();
        if (t != null) {
            final Throwable root = apply(t);
            if (root != null) {
                site = findAndFormat(root.getStackTrace());
                if (isNullOrSpaces(site)) {
                    int limit = 0;
                    for (Throwable c = t; c != null; c = c.getCause()) {
                        site = findAndFormat(c.getStackTrace());
                        if (!isNullOrSpaces(site)) {
                            break;
                        }

                        //Deal with excessive cause chains
                        //and cyclic throwables.
                        if (++limit == (1 << 16)) {
                            break; //Give up.
                        }
                    }
                }
            }
        }
        return site;
    }

    /**
     * Finds and formats the first stack frame of interest.
     *
     * @param trace the fill stack to examine.
     * @return a String that best describes the call site.
     * @throws NullPointerException if stack trace element array is null.
     */
    private String findAndFormat(StackTraceElement[] trace) {
        String site = "";
        for (StackTraceElement s : trace) {
            if (!ignore(s)) {
                site = formatStackTraceElement(s);
                break;
            }
        }

        //Check if all code was compiled with no debugging info.
        if (isNullOrSpaces(site)) {
            for (StackTraceElement s : trace) {
                if (!defaultIgnore(s)) {
                    site = formatStackTraceElement(s);
                    break;
                }
            }
        }
        return site;
    }

    /**
     * Formats a stack trace element into a simple call site.
     *
     * @param s the stack trace element to format.
     * @return the formatted stack trace element.
     * @throws NullPointerException if stack trace element is null.
     * @see #formatThrown(java.util.logging.LogRecord)
     */
    private String formatStackTraceElement(final StackTraceElement s) {
        String v = simpleClassName(s.getClassName());
        String result;
        if (v != null) {
            result = s.toString().replace(s.getClassName(), v);
        } else {
            result = s.toString();
        }

        //If the class name contains the simple file name then remove file name.
        v = simpleFileName(s.getFileName());
        if (v != null && result.startsWith(v)) {
            result = result.replace(s.getFileName(), "");
        }
        return result;
    }

    /**
     * Chooses a single throwable from the cause chain that will be formatted.
     * This implementation chooses the throwable that best describes the chain.
     * Subclasses can override this method to choose an alternate throwable for
     * formatting.
     *
     * @param t the throwable from the log record.
     * @return the throwable or null.
     * @see #formatThrown(java.util.logging.LogRecord)
     */
    protected Throwable apply(final Throwable t) {
        return SeverityComparator.getInstance().apply(t);
    }

    /**
     * Determines if a stack frame should be ignored as the cause of an error.
     *
     * @param s the stack trace element.
     * @return true if this frame should be ignored.
     * @see #formatThrown(java.util.logging.LogRecord)
     */
    protected boolean ignore(StackTraceElement s) {
        return isUnknown(s) || defaultIgnore(s);
    }

    /**
     * Defines the alternate format. This implementation removes all control
     * characters from the given string.
     *
     * @param s any string or null.
     * @return null if the argument was null otherwise, an alternate string.
     */
    protected String toAlternate(final String s) {
        return s != null ? s.replaceAll("[\\x00-\\x1F\\x7F]+", "") : null;
    }

    /**
     * Determines if a stack frame should be ignored as the cause of an error.
     * This does not check for unknown line numbers because code can be compiled
     * without debugging info.
     *
     * @param s the stack trace element.
     * @return true if this frame should be ignored.
     */
    private boolean defaultIgnore(StackTraceElement s) {
        return isSynthetic(s) || isStaticUtility(s) || isReflection(s);
    }

    /**
     * Determines if a stack frame is for a static utility class.
     *
     * @param s the stack trace element.
     * @return true if this frame should be ignored.
     */
    private boolean isStaticUtility(final StackTraceElement s) {
        try {
            return LogManagerProperties.isStaticUtilityClass(s.getClassName());
        } catch (RuntimeException ignore) {
        } catch (Exception ignore) {
        } catch (LinkageError ignore) {
        }
        return (!s.getClassName().endsWith("es")
                && s.getClassName().endsWith("s"))
                || s.getClassName().contains("Util");
    }

    /**
     * Determines if a stack trace element is for a synthetic method.
     *
     * @param s the stack trace element.
     * @return true if synthetic.
     * @throws NullPointerException if stack trace element is null.
     */
    private boolean isSynthetic(final StackTraceElement s) {
        return s.getMethodName().indexOf('$') > -1;
    }

    /**
     * Determines if a stack trace element has an unknown line number or a
     * native line number.
     *
     * @param s the stack trace element.
     * @return true if the line number is unknown.
     * @throws NullPointerException if stack trace element is null.
     */
    private boolean isUnknown(final StackTraceElement s) {
        return s.getLineNumber() < 0;
    }

    /**
     * Determines if a stack trace element represents a reflection frame.
     *
     * @param s the stack trace element.
     * @return true if the line number is unknown.
     * @throws NullPointerException if stack trace element is null.
     */
    private boolean isReflection(final StackTraceElement s) {
        try {
            return LogManagerProperties.isReflectionClass(s.getClassName());
        } catch (RuntimeException ignore) {
        } catch (Exception ignore) {
        } catch (LinkageError ignore) {
        }
        return s.getClassName().startsWith("java.lang.reflect.")
                || s.getClassName().startsWith("sun.reflect.");
    }

    /**
     * Creates the format pattern for this formatter.
     *
     * @param p the class name prefix.
     * @return the java.util.Formatter format string.
     * @throws NullPointerException if the given class name is null.
     */
    private String initFormat(final String p) {
        LogManager m = LogManagerProperties.getLogManager();
        String v = m.getProperty(p.concat(".format"));
        if (isNullOrSpaces(v)) {
            v = "%7$#.160s%n"; //160 chars split between message and thrown.
        }
        return v;
    }

    /**
     * Searches the given message for all instances fully qualified class name
     * with simple class name based off of the types contained in the given
     * parameter array.
     *
     * @param msg the message.
     * @param t the throwable cause chain to search or null.
     * @return the modified message string.
     */
    private static String replaceClassName(String msg, Throwable t) {
        if (!isNullOrSpaces(msg)) {
            int limit = 0;
            for (Throwable c = t; c != null; c = c.getCause()) {
                final Class<?> k = c.getClass();
                msg = msg.replace(k.getName(), k.getSimpleName());

                //Deal with excessive cause chains and cyclic throwables.
                if (++limit == (1 << 16)) {
                    break; //Give up.
                }
            }
        }
        return msg;
    }

    /**
     * Searches the given message for all instances fully qualified class name
     * with simple class name based off of the types contained in the given
     * parameter array.
     *
     * @param msg the message or null.
     * @param p the parameter array or null.
     * @return the modified message string.
     */
    private static String replaceClassName(String msg, Object[] p) {
        if (!isNullOrSpaces(msg) && p != null) {
            for (Object o : p) {
                if (o != null) {
                    final Class<?> k = o.getClass();
                    msg = msg.replace(k.getName(), k.getSimpleName());
                }
            }
        }
        return msg;
    }

    /**
     * Converts a fully qualified class name to a simple class name.
     *
     * @param name the fully qualified class name or null.
     * @return the simple class name or null.
     */
    private static String simpleClassName(String name) {
        if (name != null) {
            final int index = name.lastIndexOf('.');
            name = index > -1 ? name.substring(index + 1) : name;
        }
        return name;
    }

    /**
     * Converts a file name with an extension to a file name without an
     * extension.
     *
     * @param name the full file name or null.
     * @return the simple file name or null.
     */
    private static String simpleFileName(String name) {
        if (name != null) {
            final int index = name.lastIndexOf('.');
            name = index > -1 ? name.substring(0, index) : name;
        }
        return name;
    }

    /**
     * Determines is the given string is null or spaces.
     *
     * @param s the string or null.
     * @return true if null or spaces.
     */
    private static boolean isNullOrSpaces(final String s) {
        return s == null || s.trim().length() == 0;
    }

    /**
     * Used to format two arguments as fixed length message.
     */
    private class Alternate implements java.util.Formattable {

        /**
         * The left side of the output.
         */
        private final String left;
        /**
         * The right side of the output.
         */
        private final String right;

        /**
         * Creates an alternate output.
         *
         * @param left the left side or null.
         * @param right the right side or null.
         */
        Alternate(final String left, final String right) {
            this.left = String.valueOf(left);
            this.right = String.valueOf(right);
        }

        public void formatTo(java.util.Formatter formatter, int flags,
                int width, int precision) {

            String l = left;
            String r = right;
            if ((flags & java.util.FormattableFlags.UPPERCASE)
                    == java.util.FormattableFlags.UPPERCASE) {
                l = l.toUpperCase(formatter.locale());
                r = r.toUpperCase(formatter.locale());
            }

            if ((flags & java.util.FormattableFlags.ALTERNATE)
                    == java.util.FormattableFlags.ALTERNATE) {
                l = toAlternate(l);
                r = toAlternate(r);
            }

            if (precision <= 0) {
                precision = Integer.MAX_VALUE;
            }

            int fence = Math.min(l.length(), precision);
            if (fence > (precision >> 1)) {
                fence = Math.max(fence - r.length(), fence >> 1);
            }

            if (fence > 0) {
                if (fence > l.length()
                        && Character.isHighSurrogate(l.charAt(fence - 1))) {
                    --fence;
                }
                l = l.substring(0, fence);
            }
            r = r.substring(0, Math.min(precision - fence, r.length()));

            if (width > 0) {
                final int half = width >> 1;
                if (l.length() < half) {
                    l = pad(flags, l, half);
                }

                if (r.length() < half) {
                    r = pad(flags, r, half);
                }
            }

            Object[] empty = Collections.emptySet().toArray();
            formatter.format(l, empty);
            if (l.length() != 0 && r.length() != 0) {
                formatter.format("|", empty);
            }
            formatter.format(r, empty);
        }

        /**
         * Pad the given input string.
         *
         * @param flags the formatter flags.
         * @param s the string to pad.
         * @param length the final string length.
         * @return the padded string.
         */
        private String pad(int flags, String s, int length) {
            final int padding = length - s.length();
            final StringBuilder b = new StringBuilder(length);
            if ((flags & java.util.FormattableFlags.LEFT_JUSTIFY)
                    == java.util.FormattableFlags.LEFT_JUSTIFY) {
                for (int i = 0; i < padding; ++i) {
                    b.append('\u0020');
                }
                b.append(s);
            } else {
                b.append(s);
                for (int i = 0; i < padding; ++i) {
                    b.append('\u0020');
                }
            }
            return b.toString();
        }
    }
}
