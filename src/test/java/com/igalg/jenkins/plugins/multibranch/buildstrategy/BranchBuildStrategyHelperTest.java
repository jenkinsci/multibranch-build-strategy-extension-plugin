package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import static com.igalg.jenkins.plugins.multibranch.buildstrategy.BranchBuildStrategyHelper.getPatternsFromFile;
import static com.igalg.jenkins.plugins.multibranch.buildstrategy.BranchBuildStrategyHelper.toPatterns;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchBuildStrategyHelperTest {

    @Mock
    private SCMFileSystem fileSystem;

    @Mock
    private SCMFile root;

    @Mock
    private SCMFile child;

    @Test
    void should_return_setOfPatternsFromFile() throws IOException, InterruptedException {
        // given
        String filePath = "my_file";

        given(fileSystem.getRoot()).willReturn(root);
        given(root.child(filePath)).willReturn(child);
        given(child.exists()).willReturn(true);
        given(child.isFile()).willReturn(true);
        given(child.contentAsString()).willReturn("/foo/bar/**");

        // when
        Set<String> patterns = getPatternsFromFile(fileSystem, filePath);

        // then
        assertEquals(1, patterns.size());
        assertEquals("foo/bar/**", patterns.iterator().next());
    }

    @Test
    void should_return_empty_when_fileDoesNotExist() throws IOException, InterruptedException {
        // given
        String filePath= "my_file";

        given(fileSystem.getRoot()).willReturn(root);
        given(root.child(filePath)).willReturn(child);
        given(child.exists()).willReturn(false);

        // when
        Set<String> patterns = getPatternsFromFile(fileSystem, filePath);

        // then
        assertTrue(patterns.isEmpty());
        then(child).should(never()).contentAsString();
    }

    @Test
    void should_return_empty_when_fileNotAFile() throws IOException, InterruptedException {
        // given
        String filePath = "my_file";

        given(fileSystem.getRoot()).willReturn(root);
        given(root.child(filePath)).willReturn(child);
        given(child.exists()).willReturn(true);
        given(child.isFile()).willReturn(false);

        // when
        Set<String> patterns = getPatternsFromFile(fileSystem, filePath);

        // then
        assertTrue(patterns.isEmpty());
        then(child).should(never()).contentAsString();
    }

    @Test
    void should_return_empty_when_ExceptionOccurred() throws IOException, InterruptedException {
        // given
        String filePath = "my_file";

        given(fileSystem.getRoot()).willReturn(root);
        given(root.child(filePath)).willReturn(child);
        given(child.exists()).willReturn(true);
        given(child.isFile()).willReturn(true);
        given(child.contentAsString()).willThrow(IOException.class);

        // when
        Set<String> patterns = getPatternsFromFile(fileSystem, filePath);

        // then
        assertTrue(patterns.isEmpty());
    }

    @Test
    void should_returnEmpty_when_valueIsNull() {
        // when
        Set<String> patterns = toPatterns(null);

        // then
        assertTrue(patterns.isEmpty());
    }

    @Test
    void should_returnEmpty_when_valueIsEmpty() {
        // when
        Set<String> patterns = toPatterns("");

        // then
        assertTrue(patterns.isEmpty());
    }

    @Test
    void should_returnCleanedUpPatterns() {
        // given
        String value = """
            # some comments\
            
            \
            
            path/foo/**\
            
            /path/bar/**\
            
            """;

        // when
        Set<String> patterns = toPatterns(value);

        // then
        assertEquals(2, patterns.size());
        assertTrue(patterns.contains("path/foo/**"));
        assertTrue(patterns.contains("path/bar/**"));
    }
}
