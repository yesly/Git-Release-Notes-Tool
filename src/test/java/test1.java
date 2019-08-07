//import static org.junit.Assert.*;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.junit.Test;

import yesly.GetGitCommits;

public class test1 {

	@Test
	public void test() throws RevisionSyntaxException, NoHeadException, IOException, GitAPIException {
		GetGitCommits c = new GetGitCommits("C:/Users/yesly/Documents/git/Test/");
		c.buildReport();
	}
}
