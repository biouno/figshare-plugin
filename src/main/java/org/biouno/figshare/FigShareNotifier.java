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
package org.biouno.figshare;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.biouno.figshare.credentials.FigShareOauthCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import jenkins.model.Jenkins;

/**
 * Notifier to send artifact to figshare, such as pictures, graphs and other
 * data related files.
 *
 * @author kinow
 * @since 0.1
 */
@SuppressWarnings("unchecked")
public class FigShareNotifier extends Notifier {

	private static final Logger LOGGER = Logger.getLogger(FigShareNotifier.class.getName());

	/**
	 * Credentials ID;
	 */
	private final String credentialsId;
	/**
	 * figshare Article title that will be created.
	 */
	private final String articleTitle;
	/**
	 * figshare Article description.
	 */
	private final String articleDescription;
	/**
	 * Ant pattern to locate files.
	 */
	private final String antPattern;

	/**
	 * figshare API.
	 */
	private final FigShareClient figshare;

	@DataBoundConstructor
	public FigShareNotifier(String credentialsId, String articleTitle, String articleDescription, String antPattern) {
		this.credentialsId = credentialsId;
		this.articleTitle = articleTitle;
		this.articleDescription = articleDescription;
		this.antPattern = antPattern;

		// Get credential defined by user, using credential ID
		List<FigShareOauthCredentials> credentials = CredentialsProvider.lookupCredentials(
				FigShareOauthCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
				Collections.<DomainRequirement> emptyList());
		FigShareOauthCredentials credential = CredentialsMatchers.firstOrNull(credentials,
				CredentialsMatchers.allOf(CredentialsMatchers.withId(credentialsId)));

		if (null != credential) {
			// TBD: externalise this as an advanced option in the job
			// configuration
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.log(Level.FINE, "Initialising the figshare API");
			}
			figshare = FigShareClient.to("http://api.figshare.com/", 1, credential.getClientKey(),
					credential.getClientSecret().getPlainText(), credential.getTokenKey(),
					credential.getTokenSecret().getPlainText());
		} else {
			LOGGER.warning(String.format(
					"Could not locate credential with ID %s. figshare integration is disabled for this notifier",
					credentialsId));
			figshare = null;
		}
	}

	/**
	 * @return the credentialsId
	 */
	public String getCredentialsId() {
		return credentialsId;
	}

	/**
	 * @return the articleTitle
	 */
	public String getArticleTitle() {
		return articleTitle;
	}

	/**
	 * @return the articleDescription
	 */
	public String getArticleDescription() {
		return articleDescription;
	}

	/**
	 * @return the antPattern
	 */
	public String getAntPattern() {
		return antPattern;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		listener.getLogger().println("Looking for files to upload to figshare...");
		FilePath workspace = build.getWorkspace();
		if (null != workspace) {
			FilePath[] files = workspace.list(antPattern);
			if (null != files && files.length > 0) {
				for (FilePath file : files) {
					
				}
			} else {
				listener.getLogger().println("No files found. Skip creating an empty figshare article");
			}
		}
		return false;
	}

}
