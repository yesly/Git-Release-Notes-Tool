package yesly;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

public class GetGitCommits {

	public static void main(String[] args) throws IOException, RevisionSyntaxException, NoHeadException, GitAPIException {
		BasicConfigurator.configure();
		Repository repo = new FileRepository("C:/Users/yesly/Documents/git/Test/.git");
		Git git = new Git(repo);
		
				
		ObjectId headCommitId = git.getRepository().resolve( "HEAD" );
		List<Ref> list = git.tagList().call();
		ObjectId taggedCommit = list.get(2).getObjectId();
		//Iterable<RevCommit> commits = git.log().addRange(headCommitId, taggedCommit).call();
		
	    Iterable<RevCommit> commits = git.log().all().call();
	    for (RevCommit commit : commits) {
	    	ObjectId treeId = commit.getTree();
	    	TreeWalk treeWalk = new TreeWalk(repo);
	    	treeWalk.reset(treeId);
	    	while (treeWalk.next()) { // all files contained in this revision
	    	    String path = treeWalk.getPathString();
	    	    System.out.println(path);
	    	}
	    	treeWalk.close();
	    	
	    	System.out.println(new Date(commit.getCommitTime() * 1000L));
	    	
	    	System.out.println(commit.getFullMessage());
	    }
	    System.out.println(list.get(2).getName());	    
	    System.out.println(list.get(2).getObjectId());
	    System.out.println(list.get(2).getPeeledObjectId());
	    
	    git.close();
	}
}