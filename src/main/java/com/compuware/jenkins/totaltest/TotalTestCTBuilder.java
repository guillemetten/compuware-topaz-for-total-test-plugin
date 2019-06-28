/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2019 Compuware Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.compuware.jenkins.totaltest;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

public class TotalTestCTBuilder extends Builder implements SimpleBuildStep
{
	private static final int MAX_ACCOUNTING_LEN = 52;
	
	/** Environment ID need to used during the execution */
	private final String environmentId;
	/** Folder from which tests should be executed */
	private final String folderPath;
	/** Repository server url */
	private final String serverUrl;
	/** Using Jenkins credentials plugin */
	private final String credentialsId;

	/**
	 * Recursive: true|false - if test cases should be found recursively in the folder
	 */
	private boolean recursive = DescriptorImpl.defaultRecursive;
	/** Stop if test fails or threshold is reached. Defaults to true */
	private boolean stopIfTestFailsOrThresholdReached = DescriptorImpl.defaultStopIfTestFailsOrThresholdReached;
	/**
	 * Upload to server: true|false - If results should be published to the server
	 */
	private boolean uploadToServer = DescriptorImpl.defaultUploadToServer;
	/** Halt the execution when first test case fails */
	private boolean haltAtFailure = DescriptorImpl.defaultHaltAtFailure;
	/** Code coverage treshhold */
	private int ccThreshhold = DescriptorImpl.defaultCCThreshhold;
	/** SonarQube version 5 or 6 */
	private String sonarVersion;
	/**
	 * Optional file path to a folder that contains source code of tested programs. Default is COBOL. It is only used to set the
	 * source path.
	 */
	private String sourceFolder = DescriptorImpl.defaultSourceFolder;
	private String reportFolder = DescriptorImpl.defaultReportFolder;
	private String accountInfo = DescriptorImpl.defaultAccountInfo;

	@DataBoundConstructor
	public TotalTestCTBuilder(String environmentId, String folderPath, String serverUrl, String credentialsId)
	{
		super();
		this.environmentId = environmentId;
		this.folderPath = folderPath;
		this.serverUrl = serverUrl;
		this.credentialsId = credentialsId;
	}

	/**
	 * environment where test is executed
	 * 
	 * @return <code>String</code> value of environmentId
	 */
	public String getEnvironmentId()
	{
		return environmentId;
	}

	/**
	 * environment where test is executed
	 * 
	 * @return <code>String</code> value of folder path
	 */
	public String getFolderPath()
	{
		return folderPath;
	}

	/**
	 * environment where test is executed
	 * 
	 * @return <code>String</code> value of server Url
	 */
	public String getServerUrl()
	{
		return serverUrl;
	}

	/**
	 * environment where test is executed
	 * 
	 * @return <code>String</code> value of user Id
	 */
	public String getCredentialsId()
	{
		return credentialsId;
	}

	public int getCcThreshhold()
	{
		return ccThreshhold;
	}

	public boolean getRecursive()
	{
		return recursive;
	}

	public boolean getUploadToServer()
	{
		return uploadToServer;
	}

	public boolean getHaltAtFailure()
	{
		return haltAtFailure;
	}

	public String getSonarVersion()
	{
		return sonarVersion;
	}

	public String getSourceFolder()
	{
		return sourceFolder;
	}

	public String getReportFolder()
	{
		return reportFolder;
	}

	public boolean getStopIfTestFailsOrThresholdReached()
	{
		return stopIfTestFailsOrThresholdReached;
	}

	public String getAccountInfo()
	{
		return accountInfo;
	}

	@DataBoundSetter
	public void setSourceFolder(String sourceFolder)
	{
		this.sourceFolder = sourceFolder;
	}

	@DataBoundSetter
	public void setSonarVersion(String sonarVersion)
	{
		this.sonarVersion = sonarVersion;
	}

	@DataBoundSetter
	public void setReportFolder(String reportFolder)
	{
		this.reportFolder = reportFolder;
	}

	@DataBoundSetter
	public void setCcThreshhold(int ccThreshhold)
	{
		this.ccThreshhold = ccThreshhold;
	}

	@DataBoundSetter
	public void setHaltAtFailure(boolean haltAtFailure)
	{
		this.haltAtFailure = haltAtFailure;
	}

	@DataBoundSetter
	public void setRecursive(boolean recursive)
	{
		this.recursive = recursive;
	}

	@DataBoundSetter
	public void setUploadToServer(boolean uploadToServer)
	{
		this.uploadToServer = uploadToServer;
	}

	@DataBoundSetter
	public void setStopIfTestFailsOrThresholdReached(boolean stopIfTestFailsOrThresholdReached)
	{
		this.stopIfTestFailsOrThresholdReached = stopIfTestFailsOrThresholdReached;
	}

	@DataBoundSetter
	public void setAccountInfo(String accountInfo)
	{
		this.accountInfo = accountInfo;
	}

	/*
	 * (non-Javadoc)
	 * @see jenkins.tasks.SimpleBuildStep#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener)
	 */
	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException
	{
		listener.getLogger().println("Running " + Messages.displayName() + "\n");

		try
		{
			validateParameters(launcher, listener, build.getParent());

			TotalTestCTRunner runner = new TotalTestCTRunner(this);
			boolean success = runner.run(build, launcher, workspace, listener);
			if (success == false)
			{
				listener.error("Test failure");
				throw new AbortException("Test failure");
			}
			else
			{
				listener.getLogger().println("Test Success...");
			}

		}
		catch (Exception e)
		{
			listener.getLogger().println(e.getMessage());
			throw new AbortException();
		}
	}

	/**
	 * Validates the configuration parameters.
	 * 
	 * @param launcher
	 *            An instance of <code>Launcher</code> for launching the plugin.
	 * @param listener
	 *            An instance of <code>TaskListener</code> for the build listener.
	 * @param project
	 *            An instance of <code>Item</code> for the Jenkins project.
	 */
	public void validateParameters(final Launcher launcher, final TaskListener listener, final Item project)
	{
		if (!getEnvironmentId().isEmpty())
		{
			listener.getLogger().println("environmentId = " + environmentId);
		}
		else
		{
			throw new IllegalArgumentException(
					"Missing parameter Environment Id - please get the environment ID from the repository server");
		}

		if (!getServerUrl().isEmpty())
		{
			listener.getLogger().println("serverUrl = " + serverUrl);
		}
		else
		{
			throw new IllegalArgumentException(
					"Missing parameter CES server URL - please use the Compuware configuration tool to configure");
		}

		if (!getCredentialsId().isEmpty())
		{

			if (TotalTestRunnerUtils.getLoginInformation(project, getCredentialsId()) != null)
			{
				listener.getLogger().println("Credentials entered...");
			}
			else
			{
				throw new IllegalArgumentException(
						"Credential ID entered is not valid - enter valid ID from Jenkins Credentials plugin");
			}
		}
		else
		{
			throw new IllegalArgumentException("Missing Credentials ID - configure plugin correctly");
		}
		
		if (!getAccountInfo().isEmpty() && getAccountInfo().length() > MAX_ACCOUNTING_LEN)
		{
			throw new IllegalArgumentException("Entered accounting information is greater than 52 characters.");
		}

		listener.getLogger().println("ccThreshhold = " + ccThreshhold);
	}

	@Symbol("totaltest")
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
	{
		public static final String defaultFolderPath = "";
		public static final int defaultCCThreshhold = 0;
		public static final String defaultSourceFolder = "COBOL";
		public static final String defaultReportFolder = "TTTReport";
		public static final Boolean defaultRecursive = true;
		public static final Boolean defaultStopIfTestFailsOrThresholdReached = true;
		public static final Boolean defaultUploadToServer = false;
		public static final Boolean defaultHaltAtFailure = false;
		public static final String defaultAccountInfo = "";

		/**
		 * Validates for the 'CcThreshhold' field
		 * 
		 * @param value
		 * 		The code coverage threshold.
		 * @return validation message
		 */
		public FormValidation doCheckCcThreshhold(@QueryParameter String value)
		{
			if (value.length() == 0)
			{
				return FormValidation.error(Messages.errors_missingCcThreshhold());
			}

			try
			{
				int iValue = Integer.parseInt(value);

				if (iValue < 0 || iValue > 100)
				{
					return FormValidation.error(Messages.errors_missingCcThreshhold());
				}
			}
			catch (NumberFormatException e)
			{
				return FormValidation.error(Messages.errors_missingCcThreshhold());
			}

			return FormValidation.ok();
		}

		/**
		 * Validates for the 'EnvironmentId' field
		 * 
		 * @param value
		 * 		The environment id.
		 * @return validation message
		 */
		public FormValidation doCheckEnvironmentId(@QueryParameter String value)
		{

			if (value == null || value.isEmpty() || value.trim().length() == 0)
			{
				return FormValidation.error(Messages.errors_missingEnvironmentId());
			}

			return FormValidation.ok();
		}

		/**
		 * Validates for the 'CES server URL' field
		 * 
		 * @param value
		 * 		The CES server URL.
		 * @return validation message
		 */
		public FormValidation doCheckServerUrl(@QueryParameter String value)
		{
			if (value == null || value.isEmpty() || value.trim().length() == 0)
			{
				return FormValidation.error(Messages.errors_missingServerUrl());
			}

			return FormValidation.ok();
		}

		/**
		 * Validates for the 'Login Credential' field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "fileExtension" field
		 * @return validation message
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter final String value)
		{
			if (value == null || value.isEmpty() || value.trim().length() == 0)
			{
				return FormValidation.error(Messages.checkLoginCredentialError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validates for the 'reportFolder' field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "fileExtension" field
		 * @return validation message
		 */
		public FormValidation doCheckReportFolder(@QueryParameter final String value)
		{
			if (value == null || value.isEmpty() || value.trim().length() == 0)
			{
				return FormValidation.error(Messages.errors_missingReportFolder());
			}

			File theFolder = new File(value);
			if (theFolder.isFile())
			{
				return FormValidation.error(Messages.errors_wrongReportFolder());
			}

			return FormValidation.ok();
		}

		/**
		 * Validates for the 'folderPath' field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "fileExtension" field
		 * @return validation message
		 */
		public FormValidation doCheckFolderPath(@QueryParameter final String value)
		{
			if (value != null && value.trim().length() > 0)
			{
				File theFolder = new File(value);
				if (theFolder.isFile())
				{
					return FormValidation.error(Messages.errors_missingFolderPath());
				}
			}

			return FormValidation.ok();
		}
		
		public FormValidation doCheckAccountInfo(@QueryParameter final String value)
		{
			if (value != null && value.trim().length() > 0)
			{
				if (value.length() > 52)
				{
					return FormValidation.error(Messages.errors_invalidAccountingLength());
				}
			}
			return FormValidation.ok();
		}

		/**
		 * Fills in the Login Credential selection box with applicable Jenkins credentials
		 * 
		 * @param context
		 *            Jenkins context.
		 * @param credentialsId
		 *            The credendtial id for the user.
		 * @param project
		 *            The Jenkins project.
		 * 
		 * @return credential selections
		 * 
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Jenkins context,
				@QueryParameter final String credentialsId, @AncestorInPath final Item project)
		{
			List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
					StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
					Collections.<DomainRequirement> emptyList());

			StandardListBoxModel model = new StandardListBoxModel();

			model.add(new Option("", "", false));

			for (StandardUsernamePasswordCredentials c : creds)
			{
				boolean isSelected = false;

				if (credentialsId != null)
				{
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername() + (description != null ? " (" + description + ")" : ""), c.getId(),
						isSelected));
			}

			return model;
		}

		/**
		 * Fills in the CES server URL selection box with applicable Jenkins credentials
		 * 
		 * @param serverUrl
		 *            The serverUrl id for the user.
		 * 
		 * @return serverUrl selections
		 * 
		 */
		public ListBoxModel doFillServerUrlItems(@QueryParameter String serverUrl)
		{

			ListBoxModel model = new ListBoxModel();
			model.add(new Option("", "", false));
			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
			if (globalConfig != null)
			{
				HostConnection[] hostConnections = globalConfig.getHostConnections();

				for (HostConnection connection : hostConnections)
				{
					String cesServerURL = connection.getCesUrl();
					if (cesServerURL != null && !cesServerURL.isEmpty())
					{
						boolean isSelected = false;
						if (serverUrl != null)
						{
							isSelected = serverUrl.equalsIgnoreCase(cesServerURL);
						}

						model.add(new Option(cesServerURL, cesServerURL, isSelected));
					}
				}
			}

			return model;
		}

		/*
		 * (non-Javadoc)
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass)
		{
			return true;
		}

		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName()
		{
			return Messages.displayName();
		}
	}
}