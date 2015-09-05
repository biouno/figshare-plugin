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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.biouno.figshare.credentials.FigShareOauthCredentials;
import org.biouno.figshare.v1.model.Article;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.security.Roles;

/**
 * Notifier to send artifact to figshare, such as pictures, graphs and other
 * data related files.
 *
 * @author Bruno P. Kinoshita
 * @since 0.1
 */
@SuppressWarnings("unchecked")
public class FigShareNotifier extends Notifier {

    /*
     * Logger.
     * 
     * @see https://wiki.jenkins-ci.org/display/JENKINS/Logging
     */
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
     * Credential used.
     */
    private final FigShareOauthCredentials credential;

    /**
     * Constructor called from a Jelly view. The parameters are given by a user.
     *
     * @param credentialsId figshare credential ID, selected out of a combo box
     * @param articleTitle figshare article title
     * @param articleDescription figshare article description
     * @param antPattern an ant-like pattern (e.g. **\/*.png)
     */
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

        this.credential = credential;
        if (null == credential) {
            LOGGER.warning(String.format(
                    "Could not locate credential with ID %s. figshare integration is disabled for this notifier",
                    credentialsId));
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
        // TBD: if users ask to fail the build when figshare is not executed,
        // let's make
        // that a feature. Not right now.
        if (null != credential) {
            FilePath workspace = build.getWorkspace();
            if (null != workspace) {
                FilePath[] files = workspace.list(antPattern);
                if (null != files && files.length > 0) {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Creating FileCallable...");
                    }
                    FigShareCallable callable = new FigShareCallable(antPattern, articleTitle, articleDescription,
                            credential, listener.getLogger());
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Calling FileCallable...");
                    }
                    try {
                        workspace.act(callable);
                    } catch (RuntimeException re) {
                        LOGGER.log(Level.WARNING, "Error executing figshare: " + re.getMessage(), re);
                        throw new AbortException("Error executing figshare: " + re.getMessage());
                    }
                } else {
                    listener.getLogger().println("No files found. Skip creating an empty figshare article.");
                }
            } else {
                listener.getLogger().println("Missing workspace. Skip creating an empty figshare article.");
            }
        } else {
            listener.getLogger().println("No credentials found. Skipping figshare post build step.");
        }
        return Boolean.TRUE;
    }

    /**
     * {#link FileCallable} used to execute upload in the slave with the files.
     * 
     * @author Bruno P. Kinoshita
     * @since 0.1
     */
    private static final class FigShareCallable implements FileCallable<Void> {

        private static final String FIGSHARE_ARTICLE_DEFAULT_TYPE = "dataset";

        /*
         * Serial UID.
         */
        private static final long serialVersionUID = 5511693287716237552L;

        private final String includes;
        private final String title;
        private final String description;

        /*
         * From build listener.
         */
        private final PrintStream ps;
        private final FigShareOauthCredentials credential;

        /**
         * Internal only constructor.
         *
         * @param includes ant include pattern
         * @param title article title
         * @param description article description
         * @param credential credential
         * @param ps job output
         */
        FigShareCallable(String includes, String title, String description, FigShareOauthCredentials credential,
                PrintStream ps) {
            this.includes = includes;
            this.title = title;
            this.description = description;
            this.credential = credential;
            this.ps = ps;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jenkinsci.remoting.RoleSensitive#checkRoles(org.jenkinsci.
         * remoting.RoleChecker)
         */
        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
            // OK to execute it anywhere
            checker.check(this, Arrays.asList(Roles.MASTER, Roles.SLAVE));
        }

        /*
         * (non-Javadoc)
         * 
         * @see hudson.FilePath.FileCallable#invoke(java.io.File,
         * hudson.remoting.VirtualChannel)
         */
        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            FileSet fs = Util.createFileSet(f, includes, null);
            fs.setDefaultexcludes(/* defaultExcludes */ true);
            DirectoryScanner ds = fs.getDirectoryScanner(new Project());
            String[] files = ds.getIncludedFiles();
            if (null != files && files.length > 0) {
                // TBD: externalise this as an advanced option in the job
                // configuration
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Initialising the figshare API");
                }
                final FigShareClient figshare = FigShareClient.to("http://api.figshare.com/", 1,
                        credential.getClientKey(), credential.getClientSecret().getPlainText(),
                        credential.getTokenKey(), credential.getTokenSecret().getPlainText());
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, String.format("Creating article %s, description: %s", title, description));
                }
                Article article = figshare.createArticle(title, description, FIGSHARE_ARTICLE_DEFAULT_TYPE);
                ps.println(String.format("Article %d created!", article.getArticleId()));
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Uploading files...");
                }
                for (final String file : files) {
                    final File fileToUpload = new File(f, file);
                    org.biouno.figshare.v1.model.File uploaded = figshare.uploadFile(article.getArticleId(),
                            fileToUpload);
                    ps.println(String.format("File %s/%s uploaded as %s to article %d", uploaded.getName(),
                            uploaded.getSize(), uploaded.getMimeType(), article.getArticleId()));
                }
            } else {
                ps.println(String.format("No files found for pattern %s", includes));
            }
            return null;
        }

    }

    /**
     * Notifier descriptor.
     * 
     * @author Bruno P. Kinoshita
     * @since 0.1
     */
    @Extension
    public final static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        /**
         * Used internally only.
         */
        public DescriptorImpl() {
            super(FigShareNotifier.class);
            load();
        }

        /*
         * (non-Javadoc)
         * 
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         */
        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            // applicable to all job type
            return Boolean.TRUE;
        }

        /*
         * (non-Javadoc)
         * 
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return "Publish artefacts to figshare";
        }

        /**
         * Used by the UI to fill a combo box with the credentials available to
         * be used in the job.
         *
         * @param context context
         * @param remoteBase remove base parameter
         * @return a model for a combo box
         */
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context,
                @QueryParameter String remoteBase) {
            if (context == null || !context.hasPermission(Item.CONFIGURE)) {
                return new StandardListBoxModel();
            }

            List<DomainRequirement> domainRequirements = new ArrayList<DomainRequirement>();
            return new StandardListBoxModel().withEmptySelection().withMatching(
                    CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(FigShareOauthCredentials.class)),
                    CredentialsProvider.lookupCredentials(StandardCredentials.class, context, ACL.SYSTEM,
                            domainRequirements));
        }

    }
}
