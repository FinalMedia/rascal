/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:

 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
*******************************************************************************/
package org.rascalmpl.uri;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;

import org.rascalmpl.uri.IURIInputStreamResolver;

public class HttpsURIResolver implements IURIInputStreamResolver {

	public InputStream getInputStream(URI uri) throws IOException {
		return new BufferedInputStream(uri.toURL().openStream());
	}

	public String scheme() {
		return "https";
	}

	public boolean exists(URI uri) {
		try {
			uri.toURL().openConnection();
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	public boolean isDirectory(URI uri) {
		return false;
	}

	public boolean isFile(URI uri) {
		return exists(uri);
	}

	public long lastModified(URI uri) {
		try {
			return uri.toURL().openConnection().getLastModified();
		}
		catch (IOException e) {
			return 0L;
		}
	}

	public String[] listEntries(URI uri) {
		String [] ls = {};
		return ls;
	}

	public String absolutePath(URI uri) {
		return uri.getPath();
	}

	public boolean supportsHost() {
		return true;
	}

	@Override
	public Charset getCharset(URI uri) throws IOException {
		try {
			String encoding = uri.toURL().openConnection().getContentEncoding();
			if (encoding != null && Charset.isSupported(encoding)) {
				return Charset.forName(encoding);
			}
			return null;
		}
		catch (IOException e) {
			return null;
		}
	}
}
