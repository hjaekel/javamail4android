/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.mail.smtp;

import java.io.*;
import java.util.*;
import javax.security.sasl.*;
import javax.security.auth.callback.*;
import javax.mail.MessagingException;

import com.sun.mail.util.*;

/**
 * This class contains a single method that does authentication using
 * SASL.  This is in a separate class so that it can be compiled with
 * J2SE 1.5.  Eventually it should be merged into SMTPTransport.java.
 */

public class SMTPSaslAuthenticator implements SaslAuthenticator {

    private SMTPTransport pr;
    private String name;
    private Properties props;
    private boolean debug;
    private PrintStream out;
    private String host;

    public SMTPSaslAuthenticator(SMTPTransport pr, String name,
		Properties props, boolean debug, PrintStream out, String host) {
	this.pr = pr;
	this.name = name;
	this.props = props;
	this.debug = debug;
	this.out = out;
	this.host = host;
    }

    public boolean authenticate(String[] mechs, final String realm,
				final String authzid, final String u,
				final String p) throws MessagingException {

	boolean done = false;
	if (debug) {
	    out.print("DEBUG SMTP SASL: Mechanisms:");
	    for (int i = 0; i < mechs.length; i++)
		out.print(" " + mechs[i]);
	    out.println();
	}

	SaslClient sc;
	CallbackHandler cbh = new CallbackHandler() {
	    public void handle(Callback[] callbacks) {
		if (debug)
		    out.println("DEBUG SMTP SASL: callback length: " +
							callbacks.length);
		for (int i = 0; i < callbacks.length; i++) {
		    if (debug)
			out.println("DEBUG SMTP SASL: callback " + i + ": " +
							callbacks[i]);
		    if (callbacks[i] instanceof NameCallback) {
			NameCallback ncb = (NameCallback)callbacks[i];
			ncb.setName(u);
		    } else if (callbacks[i] instanceof PasswordCallback) {
			PasswordCallback pcb = (PasswordCallback)callbacks[i];
			pcb.setPassword(p.toCharArray());
		    } else if (callbacks[i] instanceof RealmCallback) {
			RealmCallback rcb = (RealmCallback)callbacks[i];
			rcb.setText(realm != null ?
				    realm : rcb.getDefaultText());
		    } else if (callbacks[i] instanceof RealmChoiceCallback) {
			RealmChoiceCallback rcb =
			    (RealmChoiceCallback)callbacks[i];
			if (realm == null)
			    rcb.setSelectedIndex(rcb.getDefaultChoice());
			else {
			    // need to find specified realm in list
			    String[] choices = rcb.getChoices();
			    for (int k = 0; k < choices.length; k++) {
				if (choices[k].equals(realm)) {
				    rcb.setSelectedIndex(k);
				    break;
				}
			    }
			}
		    }
		}
	    }
	};

	try {
	    sc = Sasl.createSaslClient(mechs, authzid, name, host,
					(Map)props, cbh);
	} catch (SaslException sex) {
	    if (debug)
		out.println("DEBUG SMTP SASL: Failed to create SASL client: " +
								sex);
	    return false;
	}
	if (sc == null) {
	    if (debug)
		out.println("DEBUG SMTP SASL: No SASL support");
	    return false;
	}
	if (debug)
	    out.println("DEBUG SMTP SASL: SASL client " +
						sc.getMechanismName());

	int resp;
	try {
	    String mech = sc.getMechanismName();
	    String ir = null;
	    if (sc.hasInitialResponse()) {
		byte[] ba = sc.evaluateChallenge(new byte[0]);
		ba = BASE64EncoderStream.encode(ba);
		ir = ASCIIUtility.toString(ba, 0, ba.length);
	    }
	    if (ir != null)
		resp = pr.simpleCommand("AUTH " + mech + " " + ir);
	    else
		resp = pr.simpleCommand("AUTH " + mech);

	    /*
	     * A 530 response indicates that the server wants us to
	     * issue a STARTTLS command first.  Do that and try again.
	     */
	    if (resp == 530) {
		pr.startTLS();
		if (ir != null)
		    resp = pr.simpleCommand("AUTH " + mech + " " + ir);
		else
		    resp = pr.simpleCommand("AUTH " + mech);
	    }

	    if (resp == 235)
		return true;	// success already!

	    if (resp != 334)
		return false;
	} catch (Exception ex) {
	    if (debug)
		out.println("DEBUG SMTP SASL: AUTHENTICATE Exception: " + ex);
	    return false;
	}

	while (!done) { // loop till we are done
	    try {
	    	if (resp == 334) {
		    byte[] ba = null;
		    if (!sc.isComplete()) {
			ba = ASCIIUtility.getBytes(responseText(pr));
			if (ba.length > 0)
			    ba = BASE64DecoderStream.decode(ba);
			if (debug)
			    out.println("DEBUG SMTP SASL: challenge: " +
				ASCIIUtility.toString(ba, 0, ba.length) + " :");
			ba = sc.evaluateChallenge(ba);
		    }
		    if (ba == null) {
			if (debug)
			    out.println("DEBUG SMTP SASL: no response");
			resp = pr.simpleCommand("*");
		    } else {
			if (debug)
			    out.println("DEBUG SMTP SASL: response: " +
				ASCIIUtility.toString(ba, 0, ba.length) + " :");
			ba = BASE64EncoderStream.encode(ba);
			resp = pr.simpleCommand(ba);
		    }
		} else
		    done = true;
	    } catch (Exception ioex) {
		if (debug)
		    ioex.printStackTrace();
		done = true;
		// XXX - ultimately return true???
	    }
	}

	if (sc.isComplete() /*&& res.status == SUCCESS*/) {
	    String qop = (String)sc.getNegotiatedProperty(Sasl.QOP);
	    if (qop != null && (qop.equalsIgnoreCase("auth-int") ||
				qop.equalsIgnoreCase("auth-conf"))) {
		// XXX - NOT SUPPORTED!!!
		if (debug)
		    out.println("DEBUG SMTP SASL: " +
			"Mechanism requires integrity or confidentiality");
		return false;
	    }
	}

	return true;
    }

    private static final String responseText(SMTPTransport pr) {
	String resp = pr.getLastServerResponse().trim();
	if (resp.length() > 4)
	    return resp.substring(4);
	else
	    return "";
    }
}
