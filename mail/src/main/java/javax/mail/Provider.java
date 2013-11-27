/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package javax.mail;

/**
 * The Provider is a class that describes a protocol 
 * implementation.  The values typically come from the
 * javamail.providers and javamail.default.providers
 * resource files.  An application may also create and
 * register a Provider object to dynamically add support
 * for a new provider.
 *
 * @author Max Spivak
 * @author Bill Shannon
 */
public class Provider {

    /**
     * This inner class defines the Provider type.
     * Currently, STORE and TRANSPORT are the only two provider types 
     * supported.
     */

    public static class Type {
	public static final Type STORE     = new Type("STORE");
	public static final Type TRANSPORT = new Type("TRANSPORT");

	private String type;

	private Type(String type) {
	    this.type = type;
	}

	public String toString() {
	    return type;
	}
    }

    private Type type;
    private String protocol, className, vendor, version;

    /**
     * Create a new provider of the specified type for the specified
     * protocol.  The specified class implements the provider.
     *
     * @param type      Type.STORE or Type.TRANSPORT
     * @param protocol  valid protocol for the type
     * @param classname class name that implements this protocol
     * @param vendor    optional string identifying the vendor (may be null)
     * @param version   optional implementation version string (may be null)
     * @since JavaMail 1.4
     */
    public Provider(Type type, String protocol, String classname, 
	     String vendor, String version) {
	this.type = type;
	this.protocol = protocol;
	this.className = classname;
	this.vendor = vendor;
	this.version = version;
    }

    /**
     * Returns the type of this Provider.
     *
     * @return	the provider type
     */
    public Type getType() {
	return type;
    }

    /**
     * Returns the protocol supported by this Provider.
     *
     * @return	the protocol
     */
    public String getProtocol() {
	return protocol;
    }

    /**
     * Returns the name of the class that implements the protocol.
     *
     * @return	the class name
     */
    public String getClassName() {
	return className;
    }

    /**
     * Returns the name of the vendor associated with this implementation
     * or null.
     *
     * @return	the vendor
     */
    public String getVendor() {
	return vendor;
    }

    /**
     * Returns the version of this implementation or null if no version.
     *
     * @return	the version
     */
    public String getVersion() {
	return version;
    }

    /** Overrides Object.toString() */
    public String toString() {
	String s = "javax.mail.Provider[" + type + "," +
		    protocol + "," + className;

	if (vendor != null)
	    s += "," + vendor;

	if (version != null)
	    s += "," + version;

	s += "]";
	return s;
    }
}
