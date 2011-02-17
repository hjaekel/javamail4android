/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.mail.mbox;

import java.io.*;
import java.util.*;

/**
 * A support class that contains the state and logic needed when
 * loading messages from a folder.
 */
final class MessageLoader {
    private final TempFile temp;
    private FileInputStream fis;
    private AppendStream fos;
    private int pos, len;
    private long off;
    private MboxFolder.MessageMetadata md;
    private byte[] buf;
    // the length of the longest header we'll need to look at
    private static final int LINELEN = "Content-Length: XXXXXXXXXX".length();
    private char[] line;

    public MessageLoader(TempFile temp) {
	this.temp = temp;
    }

    /**
     * Load messages from the given file descriptor, starting at the
     * specified offset, adding the MessageMetadata to the list. <p>
     *
     * The data is assumed to be in UNIX mbox format, with newlines
     * only as the line terminators.
     */
    public int load(FileDescriptor fd, long offset, List msgs)
				throws IOException {
	// XXX - could allocate and deallocate buffers here
	int loaded = 0;
	try {
	    fis = new FileInputStream(fd);
	    if (fis.skip(offset) != offset)
		throw new EOFException("Failed to skip to offset " + offset);
	    this.off = offset;
	    pos = len = 0;
	    line = new char[LINELEN];
	    buf = new byte[64 * 1024];
	    fos = temp.getAppendStream();
	    int n;
	    // keep loading messages as long as we have headers
	    while ((n = skipHeader()) >= 0) {
		long start;
		if (n == 0) {
		    // didn't find a Content-Length, skip the body
		    start = skipBody();
		    if (start < 0) {
			md.end = -1;
			msgs.add(md);
			loaded++;
			break;
		    }
		} else {
		    // skip over the body
		    skip(n);
		    int b;
		    start = off;
		    // skip any blank lines after the body
		    while ((b = get()) >= 0) {
			if (b != '\n')
			    break;
		    }
		}
		md.end = start;
		msgs.add(md);
		loaded++;
	    }
	} finally {
	    try {
		fis.close();
	    } catch (IOException ex) {
		// ignore
	    }
	    try {
		fos.close();
	    } catch (IOException ex) {
		// ignore
	    }
	    line = null;
	    buf = null;
	}
	return loaded;
    }

    /**
     * Skip over the message header, returning the content length
     * of the body, or 0 if no Content-Length header was seen.
     * Update the MessageMetadata based on the headers seen.
     * return -1 on EOF.
     */
    private int skipHeader()  throws IOException {
	int clen = 0;
	boolean bol = true;
	int lpos = -1;
	int b;
	md = new MboxFolder.MessageMetadata();
	md.recent = true;
	while ((b = get()) >= 0) {
	    if (bol) {
		if (b == '\n')
		    break;
		lpos = 0;
	    }
	    if (b == '\n') {
		bol = true;
		// newline at end of line, was the line one of the headers
		// we're looking for?
		if (lpos > 7) {
		    // XXX - make this more efficient?
		    String s = new String(line, 0, lpos);
		    // fast check for Content-Length header
		    if (line[7] == '-' && isPrefix(s, "Content-Length:")) {
			s = s.substring(15).trim();
			try {
			    clen = Integer.parseInt(s);
			} catch (NumberFormatException ex) {
			    // ignore it
			}
		    // fast check for Status header
		    } else if ((line[1] == 't' || line[1] == 'T') &&
				isPrefix(s, "Status:")) {
			if (s.indexOf('O') >= 0)
			    md.recent = false;
		    // fast check for X-Status header
		    } else if ((line[3] == 't' || line[3] == 'T') &&
				isPrefix(s, "X-Status:")) {
			if (s.indexOf('D') >= 0)
			    md.deleted = true;
		    // fast check for X-Dt-Delete-Time header
		    } else if (line[4] == '-' &&
				isPrefix(s, "X-Dt-Delete-Time:")) {
			md.deleted = true;
		    // fast check for X-IMAP header
		    } else if (line[5] == 'P' && s.startsWith("X-IMAP:")) {
			md.imap = true;
		    }
		}
	    } else {
		// accumlate data in line buffer
		bol = false;
		if (lpos < 0)	// ignoring this line
		    continue;
		if (lpos == 0 && (b == ' ' || b == '\t'))
		    lpos = -1;	// ignore continuation lines
		else if (lpos < line.length)
		    line[lpos++] = (char)b;
	    }
	}
	if (b < 0)
	    return -1;
	else
	    return clen;
    }

    /**
     * Does "s" start with "pre", ignoring case?
     */
    private static final boolean isPrefix(String s, String pre) {
	return s.regionMatches(true, 0, pre, 0, pre.length());
    }

    /**
     * Skip over the body of the message looking for a line that starts
     * with "From ".  If found, return the offset of the beginning of
     * that line.  Return -1 on EOF.
     */
    private long skipBody() throws IOException {
	boolean bol = true;
	int lpos = -1;
	long loff = off;
	int b;
	while ((b = get()) >= 0) {
	    if (bol) {
		lpos = 0;
		loff = off - 1;
	    }
	    if (b == '\n') {
		bol = true;
		if (lpos >= 5) {	// have enough data to test?
		    if (line[0] == 'F' && line[1] == 'r' && line[2] == 'o' &&
			line[3] == 'm' && line[4] == ' ')
			return loff;
		}
	    } else {
		bol = false;
		if (lpos < 0)
		    continue;
		if (lpos == 0 && b != 'F')
		    lpos = -1;		// ignore lines that don't start with F
		else if (lpos < 5)	// only need first 5 chars to test
		    line[lpos++] = (char)b;
	    }
	}
	return -1;
    }

    /**
     * Skip "n" bytes, returning how much we were able to skip.
     */
    private final int skip(int n) throws IOException {
	int n0 = n;
	if (pos + n < len) {
	    pos += n;	// can do it all within this buffer
	    off += n;
	} else {
	    do {
		n -= (len - pos);	// skip rest of this buffer
		off += (len - pos);
		fill();
		if (len <= 0)	// ran out of data
		    return n0 - n;
	    } while (n > len);
	    pos += n;
	    off += n;
	}
	return n0;
    }

    /**
     * Return the next byte.
     */
    private final int get() throws IOException {
	if (pos >= len)
	    fill();
	if (pos >= len)
	    return -1;
	else {
	    off++;
	    return buf[pos++] & 0xff;
	}
    }

    /**
     * Fill our buffer with more data.
     * Every buffer we read is also written to the temp file.
     */
    private final void fill() throws IOException {
	len = fis.read(buf);
	pos = 0;
	if (len > 0)
	    fos.write(buf, 0, len);
    }
}
