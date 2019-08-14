package gitReleaseNotes;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;

//For reference: GetGitCommits(final String pathToRepo, final String reportName, final boolean gitSubModules, final boolean byModules, final boolean allCommits)
public class Demo {
	public static void main(String[] args) throws IOException, GitAPIException {
		new GetGitCommits("C:/Users/yesly/Documents/git/electron/", "electron-release-notes", true, false, false, false);
	}
}
