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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.scm.SCM;
import jenkins.branch.BranchBuildStrategyDescriptor;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;

public class ExcludeByIgnoreFileBranchBuildStrategy extends BranchBuildStrategyExtension {
    
	private static final Logger logger = Logger.getLogger(ExcludeByIgnoreFileBranchBuildStrategy.class.getName());
    private final String ignorefilePath;
    
    public String getIgnorefilePath() {
		return ignorefilePath;
	}


    @DataBoundConstructor
    public ExcludeByIgnoreFileBranchBuildStrategy(String ignorefilePath) {
    	if(ignorefilePath==null || ignorefilePath.trim().length()==0)
    		ignorefilePath = ".jenkinsignore";
        this.ignorefilePath = ignorefilePath;
    }

    

   
    /**
     * Determine if build is required by checking if all of the commit affected files are in the exclude regions.
     *
     * @return false if  all affected file in the exclude regions 
     */
    @Override
    public boolean isAutomaticBuild(SCMSource source, SCMHead head, SCMRevision currRevision, SCMRevision prevRevision) {
        try {
        	
        	
        	// build SCM object
        	SCM scm = source.build(head, currRevision);
            
  
        	// Verify source owner
        	SCMSourceOwner owner = source.getOwner();
            if (owner == null) {
                logger.severe("Error verify SCM source owner");
                return true;
            }
            
            
            // Build SCM file system
            SCMFileSystem fileSystem = buildSCMFileSystem(source,head,currRevision,scm,owner);            
            if (fileSystem == null) {
                logger.severe("Error build SCM file system");
                return true;
            }
           
            SCMFile file = fileSystem.getRoot().child(ignorefilePath);
            if(file == null || !file.exists() || !file.isFile()) {
            	logger.severe("File:" + ignorefilePath + " not found");
            	return true;
            }
            
            		
            String ignoreString = fileSystem.getRoot().child(ignorefilePath).contentAsString();
            logger.info(String.format("Excluded file content: %s", ignoreString));
            
            List<String> excludedRegionsList = Arrays.stream(
            		ignoreString.split("\n")).filter(p -> !p.startsWith("#")).map(e -> e.trim()).collect(Collectors.toList());

            logger.info(String.format("Excluded regions: %s", excludedRegionsList.toString()));
            
            // No regions excluded run the build
            if(excludedRegionsList.isEmpty())
            	return true;
            
            
            
            // Collect all changes from previous build 
            List<String> pathesList = new ArrayList<String>(collectAllAffectedFiles(getGitChangeSetListFromPrevious(fileSystem, head, prevRevision)));
            // If there is no match for at least one file run the build
            for (String filePath : pathesList) {
    			boolean inExclusion = false;
        		for(String excludedRegion:excludedRegionsList) {    				
    				if(SelectorUtils.matchPath(excludedRegion, filePath)) {
    					logger.fine("Matched excluded region:" + excludedRegion + " with file path:" + filePath);
    					inExclusion = true;
    					break;
    				}else {
    					logger.fine("Not matched excluded region:" + excludedRegion + " with file path:" + filePath);
    				}
    			}
        		if(!inExclusion){
        			logger.info("File:" + filePath + " Not matched any excluded region " + excludedRegionsList + " build shoud be triggered");
        			return true;
        		}
            }
            
            logger.info("All affected files matched in excluded regions the build is canceled");
            return false;
            
        } catch (Exception e) {
            //we don't want to cancel builds on unexpected exception
        	logger.log(Level.SEVERE, "Unecpected exception", e);
            return true;
        }
        
        

    }

    @Extension
    public static class DescriptorImpl extends BranchBuildStrategyDescriptor {
        public String getDisplayName() {
            return "Cancel ci by ignore file strategy";
        }
    }
    
    
 
    

}
