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
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;

public class IncludeRegionBranchBuildStrategy extends RegionBranchBuildStrategy {
    
	private static final Logger logger = Logger.getLogger(IncludeRegionBranchBuildStrategy.class.getName());
    private final String includedRegions;
    
    public String getIncludedRegions() {
		return includedRegions;
	}


    @DataBoundConstructor
    public IncludeRegionBranchBuildStrategy(String includedRegions) {
        this.includedRegions = includedRegions;
    }

    

   
    /**
     * Determine if build is required by checking if any of the commit affected files is in the include regions.
     *
     * @return true if  there is at least one affected file in the include regions 
     */
    @Override
    public boolean isAutomaticBuild(SCMSource source, SCMHead head, SCMRevision currRevision, SCMRevision prevRevision) {
        try {
        	
        	 List<String> includedRegionsList = Arrays.stream(
             		includedRegions.split("\n")).map(e -> e.trim()).collect(Collectors.toList());

             logger.info(String.format("Included regions: %s", includedRegionsList.toString()));
             
             // No regions included cancel the build
             if(includedRegionsList.isEmpty())
             	return false;
        	
        	
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
            
            List<String> pathesList = new ArrayList<String>(collectAllAffectedFiles(getGitChangeSetListFromPrevious(fileSystem, head, prevRevision)));
            // If there is match for at least one file run the build
            for (String filePath : pathesList){
    			for(String includedRegion:includedRegionsList) {    				
    				if(SelectorUtils.matchPath(includedRegion, filePath)) {
    					logger.info("Matched included region:" + includedRegion + " with file path:" + filePath);
    					return true;
    				}else {
    					logger.fine("Not matched included region:" + includedRegion + " with file path:" + filePath);
    				}
    			}
            }
            
            
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
            return "Build included regions strategy";
        }
    }

}
