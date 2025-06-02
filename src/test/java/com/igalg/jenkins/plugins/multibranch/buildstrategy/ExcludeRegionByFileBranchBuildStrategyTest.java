package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import jenkins.scm.api.SCMFileSystem;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExcludeRegionByFileBranchBuildStrategyTest {

    private static final String FILE_PATH = "my_file";

    private final ExcludeRegionByFileBranchBuildStrategy branchBuildStrategy = new ExcludeRegionByFileBranchBuildStrategy(FILE_PATH);

    @Mock
    private SCMFileSystem fileSystem;

    @Mock
    private Set<String> expectedPatterns;

    @Test
    void should_retrievePatternFromFile() {
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
