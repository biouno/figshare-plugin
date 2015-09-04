/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Bruno P. Kinoshita
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.biouno.figshare.credentials;

import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.Secret;

/**
 * Implementation of {#link {@link FigShareOauthCredentials}.
 *
 * @author Bruno P. Kinoshita
 * @since 0.1
 */
@NameWith(value = FigShareCredentialsNameProvider.class, priority = 50)
public class FigShareOAuthCredentialsImpl extends BaseStandardCredentials implements FigShareOauthCredentials {

	/*
	 * Serial UID. 
	 */
	private static final long serialVersionUID = -6838004725741084526L;

	@NonNull
	private final String name;
	@NonNull
	private final String description;
	@NonNull
	private final String clientKey;
	@NonNull
	private final Secret clientSecret;
	@NonNull
	private final String tokenKey;
	@NonNull
	private final Secret tokenSecret;

	@DataBoundConstructor
	public FigShareOAuthCredentialsImpl(@CheckForNull String id,
			@NonNull @CheckForNull String name, 
			@CheckForNull String description, 
			@CheckForNull String clientKey, 
			@CheckForNull String clientSecret,
			@CheckForNull String tokenKey, 
			@CheckForNull String tokenSecret) {
		super(id, name);
		this.name = name;
		this.description = description;
		this.clientKey = clientKey;
		this.clientSecret = Secret.fromString(clientSecret);
		this.tokenKey = tokenKey;
		this.tokenSecret = Secret.fromString(tokenSecret);
	}

	@NonNull
	@Override
	public String getName() {
		return name;
	}

	@NonNull
	@Override
	public String getDescription() {
		return description;
	}

	@NonNull
	@Override
	public String getClientKey() {
		return clientKey;
	}

	@NonNull
	@Override
	public Secret getClientSecret() {
		return clientSecret;
	}

	@NonNull
	@Override
	public String getTokenKey() {
		return tokenKey;
	}

	@NonNull
	@Override
	public Secret getTokenSecret() {
		return tokenSecret;
	}

	/**
	 * figshare OAuth credentials descriptor.
	 *
	 * @author Bruno P. Kinoshita
	 * @since 0.1
	 */
	@Extension
	public static class Descriptor extends CredentialsDescriptor {

		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return "figshare OAuth Credentials";
		}

	}

}
