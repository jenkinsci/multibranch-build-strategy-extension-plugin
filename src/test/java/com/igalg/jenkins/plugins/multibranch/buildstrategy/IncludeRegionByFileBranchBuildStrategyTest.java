package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import jenkins.scm.api.SCMFileSystem;

@RunWith(MockitoJUnitRunner.class)
public class IncludeRegionByFileBranchBuildStrategyTest {

    private static final String FILE_PATH = "my_file";

    private final IncludeRegionByFileBranchBuildStrategy branchBuildStrategy = new IncludeRegionByFileBranchBuildStrategy(FILE_PATH);

    @Mock
    private SCMFileSystem fileSystem;

    @Mock
    private Set<String> expectedPatterns;

    @Test
    public void should_retrievePatternFromFile() {
        try (MockedStatic<BranchBuildStrategyHelper> mockedHelper = mockStatic(BranchBuildStrategyHelper.class)) {
            // given
            mockedHelper.when(() -> BranchBuildStrategyHelper.getPatternsFromFile(fileSystem, FILE_PATH)).thenReturn(expectedPatterns);

            // when
            Set<String> actualPatterns = branchBuildStrategy.getPatterns(fileSystem);

            // then
            assertEquals(expectedPatterns, actualPatterns);
        }
    }

}
