import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

import gitReleaseNotes.GetGitCommits;

// For reference: GetGitCommits(final String pathToRepo, final String reportName, final boolean gitSubModules, final boolean byModules, final boolean allCommits);
public class ReportTests {
	
	@Test
	public void withGitSubModulesTest() throws IOException, GitAPIException {
		new GetGitCommits("C:/Users/yesly/Documents/git/Test/", "report-with-git-sub-modules", true, false, false, false);
		
		assertTrue(filesAreEqual("C:/Users/yesly/eclipse-workspace/get-git-commits/report-with-git-sub-modules.txt", "C:/Users/yesly/eclipse-workspace/get-git-commits/src/test/resources/expected-with-git-sub-modules.txt"));
	}
	
	@Test
	public void byModulesTest() throws IOException, GitAPIException  {
		new GetGitCommits("C:/Users/yesly/Documents/git/Test/", "report-by-modules", false, true, false, false);
		
		assertTrue(filesAreEqual("C:/Users/yesly/eclipse-workspace/get-git-commits/report-by-modules.txt", "C:/Users/yesly/eclipse-workspace/get-git-commits/src/test/resources/expected-by-modules.txt"));
	}
	
	@Test
	public void allCommitsTest() throws IOException, GitAPIException {
		new GetGitCommits("C:/Users/yesly/Documents/git/Test/", "report-all-commits", false, false, true, false);
		
		assertTrue(filesAreEqual("C:/Users/yesly/eclipse-workspace/get-git-commits/report-all-commits.txt", "C:/Users/yesly/eclipse-workspace/get-git-commits/src/test/resources/expected-all-commits.txt"));
	}
	
	@Test
	public void notByModulesTest() throws IOException, GitAPIException {
		new GetGitCommits("C:/Users/yesly/Documents/git/Test/", "report-not-by-modules", false, false, false, false);
		
		assertTrue(filesAreEqual("C:/Users/yesly/eclipse-workspace/get-git-commits/report-not-by-modules.txt", "C:/Users/yesly/eclipse-workspace/get-git-commits/src/test/resources/expected-not-by-modules.txt"));
	}
	
	public boolean filesAreEqual(String pathToFileq1, String pathToFileq2) throws IOException {
		BufferedReader reader1 = new BufferedReader(new FileReader(pathToFileq1));
        BufferedReader reader2 = new BufferedReader(new FileReader(pathToFileq2));
        
        String line1 = reader1.readLine();
        String line2 = reader2.readLine();
         
        while (line1 != null && line2 != null)
        {
            if(line1 == null || line2 == null) {
            	 reader1.close();
                 reader2.close();
                return false;
            } else if(!line1.equalsIgnoreCase(line2)) {
            	 reader1.close();
                 reader2.close();
                return false;
            }

            line1 = reader1.readLine();
            line2 = reader2.readLine();
        }
        reader1.close();
        reader2.close();

		return true;
	}
}
