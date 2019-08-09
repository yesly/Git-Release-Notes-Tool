//import static org.junit.Assert.*;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.junit.Test;

import yesly.GetGitCommits;

public class test1 {
	@Test
	public void withGitSubModules() throws RevisionSyntaxException, NoHeadException, IOException, GitAPIException {
		new GetGitCommits("C:/Users/yesly/Documents/git/Test/", "commits-report-with-git-sub-modules", true, false);
	}

	@Test
	public void notByModules() throws RevisionSyntaxException, NoHeadException, IOException, GitAPIException {
		new GetGitCommits("C:/Users/yesly/Documents/git/Test/", "commits-report-not-by-modules", false, false);
	}
	
	@Test
	public void byModules() throws RevisionSyntaxException, NoHeadException, IOException, GitAPIException {
		new GetGitCommits("C:/Users/yesly/Documents/git/Test/", "commits-report-by-modules", false, true);
	}
}
