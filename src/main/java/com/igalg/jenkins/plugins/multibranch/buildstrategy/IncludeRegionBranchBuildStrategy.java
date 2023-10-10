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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tools.ant.types.selectors.SelectorUtils;

abstract class IncludeRegionBranchBuildStrategy extends AbstractBranchBuildStrategy {

    private static final Logger LOGGER = Logger.getLogger(IncludeRegionBranchBuildStrategy.class.getName());

    protected IncludeRegionBranchBuildStrategy() {
        super(Strategy.INCLUDED);
    }

    /**
     * Determine if build is required by checking if any of the commit affected files is in the include regions.
     *
     * @return {@code true} if at least one file matches in the include regions
     */
    @Override
    boolean shouldRunBuild(Set<String> patterns, Set<String> paths) {
        for (String path : paths) {
            for (String pattern : patterns) {
                if (SelectorUtils.matchPath(pattern, path)) {
                    LOGGER.log(Level.INFO, () -> "Matched included region: " + pattern + " with file path: " + path);
                    return true; // If at least one file matches for, run the build
                } else {
                    LOGGER.log(Level.FINE, () -> "Not matched included region: " + pattern + " with file path: " + path);
                }
            }
        }

        LOGGER.log(Level.INFO, () -> "No matching any included regions, skipping build");

        return false;
    }

}
