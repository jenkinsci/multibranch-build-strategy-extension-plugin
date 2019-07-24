/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 igalg
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
package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import hudson.plugins.git.GitChangeLogParser;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitChangeSet.Path;
import hudson.scm.SCM;
import jenkins.branch.BranchBuildStrategy;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.plugins.git.GitSCMFileSystem;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;

public abstract class BranchBuildStrategyExtension extends BranchBuildStrategy{

		private final static int HASH_LENGTH = 40;
		private static final Logger logger = Logger.getLogger(BranchBuildStrategyExtension.class.getName());
	
	   protected SCMFileSystem   buildSCMFileSystem(SCMSource source, SCMHead head, SCMRevision currRevision,SCM scm,SCMSourceOwner owner) throws Exception{
	    	GitSCMFileSystem.Builder builder = new GitSCMFileSystem.BuilderImpl(); 
	    	if (currRevision != null && !(currRevision instanceof AbstractGitSCMSource.SCMRevisionImpl))
	             return builder.build(source, head, new AbstractGitSCMSource.SCMRevisionImpl(head, currRevision.toString().substring(0,40)));
	         else 
	             return builder.build(owner, scm, currRevision);
	    }
	    
	    
	   protected List<GitChangeSet> getGitChangeSetListFromPrevious(SCMFileSystem fileSystem,SCMHead head, SCMRevision prevRevision) throws Exception{
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        if (prevRevision != null && !(prevRevision instanceof AbstractGitSCMSource.SCMRevisionImpl))
	            fileSystem.changesSince(new AbstractGitSCMSource.SCMRevisionImpl(head,prevRevision.toString().substring(0,HASH_LENGTH)), out);
	        else 
	            fileSystem.changesSince(prevRevision, out);	        
	        GitChangeLogParser parser = new GitChangeLogParser(true);
	        return parser.parse(new ByteArrayInputStream(out.toByteArray()));
	    }
	    
	    
	   protected Set<String> collectAllAffectedFiles(List<GitChangeSet> gitChangeSetList) {
	    	Set<String> pathesSet = new HashSet<String>();
	        for (GitChangeSet gitChangeSet : gitChangeSetList) {
	        	List<Path> affectedFilesList = new ArrayList<Path>(gitChangeSet.getAffectedFiles());
	        	for (Path path : affectedFilesList) {
	        		pathesSet.add(path.getPath());
	        		logger.fine("File:" + path.getPath() +" from commit:" + gitChangeSet.getCommitId() + " Change type:" + path.getEditType().getName());
	        	}
	        }
	        return pathesSet;
	    }
	   
	   protected Set<String> collectAllComments(List<GitChangeSet> gitChangeSetList) {
	    	Set<String> comments = new HashSet<String>();
	        for (GitChangeSet gitChangeSet : gitChangeSetList) {
	        	comments.add(gitChangeSet.getComment());
	        }
	        return comments;
	    }
}
